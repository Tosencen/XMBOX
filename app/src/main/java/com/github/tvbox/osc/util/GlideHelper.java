package com.github.tvbox.osc.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.xmbox.app.R;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.util.urlhttp.BrotliInterceptor;
import com.github.tvbox.osc.util.OkHttpSafetyUtil;
import com.github.tvbox.osc.util.LOG;
import com.orhanobut.hawk.Hawk;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Glide图片加载工具类
 * 替代Picasso实现更高效的图片加载
 */
@GlideModule
public class GlideHelper extends AppGlideModule {
    private static final int TIMEOUT = 10000; // 10秒超时
    private static OkHttpClient okHttpClient;

    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        // 设置内存缓存大小为默认的4倍
        MemorySizeCalculator calculator = new MemorySizeCalculator.Builder(context)
                .setMemoryCacheScreens(4) // 默认是2，增加到4来缓存更多图片
                .build();
        builder.setMemoryCache(new LruResourceCache((int) (calculator.getMemoryCacheSize() * 2.0)));

        // 设置默认图片质量为RGB_565，减少内存占用
        builder.setDefaultRequestOptions(
                new RequestOptions()
                        .format(DecodeFormat.PREFER_RGB_565)
                        .diskCacheStrategy(DiskCacheStrategy.ALL) // 启用磁盘缓存
                        .disallowHardwareConfig() // 某些设备上硬件加速可能导致问题
        );
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        // 使用OkHttp作为网络请求库
        registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(getOkHttpClient()));
    }

    private static OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                    .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                    .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                    .retryOnConnectionFailure(true)
                    .addInterceptor(new BrotliInterceptor())
                    .addInterceptor(new ImageHeaderInterceptor()); // 添加图片请求头拦截器

            // 使用与OkGoHelper相同的SSL设置
            try {
                OkGoHelper.setOkHttpSsl(builder);
            } catch (Throwable th) {
                th.printStackTrace();
            }

            // 使用安全工具类设置DNS，确保不会传入null值
            // 在Android 15及以上版本，OkHttp不再接受null作为DNS参数
            OkHttpSafetyUtil.ensureSafeDns(builder, OkGoHelper.dnsOverHttps);

            // 配置自定义调度器以限制并发请求数
            okhttp3.Dispatcher dispatcher = new okhttp3.Dispatcher();
            dispatcher.setMaxRequestsPerHost(8); // 每个主机最多8个并发请求
            dispatcher.setMaxRequests(64);       // 总共最多64个并发请求
            builder.dispatcher(dispatcher);

            okHttpClient = builder.build();
        }
        return okHttpClient;
    }

    /**
     * 加载图片到ImageView
     * @param imageView 目标ImageView
     * @param url 图片URL
     */
    public static void loadImage(ImageView imageView, String url) {
        if (imageView == null) return;

        // 处理图片URL
        String processedUrl = processImageUrl(url);

        if (android.text.TextUtils.isEmpty(processedUrl)) {
            imageView.setImageResource(R.drawable.img_loading_placeholder);
            return;
        }

        // 添加调试日志
        LOG.d("GlideHelper", "原始URL: " + url);
        LOG.d("GlideHelper", "处理后URL: " + processedUrl);

        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.img_loading_placeholder)
                .error(R.drawable.img_loading_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .timeout(10000); // 10秒超时

        Glide.with(imageView.getContext())
                .load(processedUrl)
                .apply(options)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        LOG.w("GlideHelper", "图片加载失败: " + processedUrl + ", 错误: " + (e != null ? e.getMessage() : "未知"));
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        LOG.d("GlideHelper", "图片加载成功: " + processedUrl);
                        return false;
                    }
                })
                .into(imageView);
    }

    /**
     * 加载图片到ImageView，带尺寸限制
     * @param imageView 目标ImageView
     * @param url 图片URL
     * @param width 目标宽度
     * @param height 目标高度
     */
    public static void loadImage(ImageView imageView, String url, int width, int height) {
        if (imageView == null) return;

        // 处理图片URL
        String processedUrl = processImageUrl(url);

        Glide.with(imageView.getContext())
                .load(processedUrl)
                .placeholder(R.drawable.img_loading_placeholder)
                .error(R.drawable.img_loading_placeholder)
                .override(width, height)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .dontAnimate() // 禁用动画以提高性能
                .into(imageView);
    }

    /**
     * 直接加载图片到ImageView，不处理URL（用于已经处理过的URL）
     * @param imageView 目标ImageView
     * @param url 已处理的图片URL
     * @param width 目标宽度
     * @param height 目标高度
     */
    public static void loadImageDirect(ImageView imageView, String url, int width, int height) {
        if (imageView == null) return;

        if (android.text.TextUtils.isEmpty(url)) {
            imageView.setImageResource(R.drawable.img_loading_placeholder);
            return;
        }

        // 添加调试日志
        LOG.d("GlideHelper", "直接加载图片: " + url);

        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.img_loading_placeholder)
                .error(R.drawable.img_loading_placeholder)
                .override(width, height)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .dontAnimate() // 禁用动画以提高性能
                .timeout(10000); // 10秒超时

        Glide.with(imageView.getContext())
                .load(url)
                .apply(options)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        LOG.w("GlideHelper", "图片加载失败: " + url + ", 错误: " + (e != null ? e.getMessage() : "未知"));
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        LOG.d("GlideHelper", "图片加载成功: " + url);
                        return false;
                    }
                })
                .into(imageView);
    }

    /**
     * 加载圆角图片到ImageView
     * @param imageView 目标ImageView
     * @param url 图片URL
     * @param radius 圆角半径（像素）
     */
    public static void loadRoundedImage(ImageView imageView, String url, int radius) {
        if (imageView == null) return;

        // 处理图片URL
        String processedUrl = processImageUrl(url);

        Glide.with(imageView.getContext())
                .load(processedUrl)
                .placeholder(R.drawable.img_loading_placeholder)
                .error(R.drawable.img_loading_placeholder)
                .transform(new CenterCrop(), new RoundedCorners(radius))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .dontAnimate() // 禁用动画以提高性能
                .into(imageView);
    }

    /**
     * 加载圆角图片到ImageView，带尺寸限制
     * @param imageView 目标ImageView
     * @param url 图片URL
     * @param width 目标宽度
     * @param height 目标高度
     * @param radius 圆角半径（像素）
     */
    public static void loadRoundedImage(ImageView imageView, String url, int width, int height, int radius) {
        if (imageView == null) return;

        // 处理图片URL
        String processedUrl = processImageUrl(url);

        Glide.with(imageView.getContext())
                .load(processedUrl)
                .placeholder(R.drawable.img_loading_placeholder)
                .error(R.drawable.img_loading_placeholder)
                .override(width, height)
                .transform(new CenterCrop(), new RoundedCorners(radius))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .dontAnimate() // 禁用动画以提高性能
                .priority(com.bumptech.glide.Priority.LOW) // 使用低优先级
                .into(imageView);
    }

    /**
     * 预加载图片到内存缓存
     * @param context 上下文
     * @param url 图片URL
     */
    public static void preloadImage(Context context, String url) {
        // 处理图片URL
        String processedUrl = processImageUrl(url);

        Glide.with(context)
                .load(processedUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(300, 400) // 预加载时使用较小的尺寸以提高性能
                .priority(com.bumptech.glide.Priority.LOW) // 使用低优先级
                .preload();
    }

    /**
     * 清除内存缓存
     * @param context 上下文
     */
    public static void clearMemoryCache(Context context) {
        Glide.get(context).clearMemory();
    }

    /**
     * 清除磁盘缓存（需要在后台线程中调用）
     * @param context 上下文
     */
    public static void clearDiskCache(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Glide.get(context).clearDiskCache();
            }
        }).start();
    }

    // 防止重复清理的标志
    private static volatile boolean isCleanedUp = false;
    private static final Object cleanupLock = new Object();

    /**
     * 清理Glide相关资源，防止内存泄漏
     * 建议在Application的onTerminate()中调用
     */
    public static void cleanup() {
        synchronized (cleanupLock) {
            if (isCleanedUp) {
                LOG.d("GlideHelper", "Glide资源已经清理过，跳过重复清理");
                return;
            }

            try {
                LOG.i("GlideHelper", "开始清理Glide资源");

                // 清理OkHttpClient
                if (okHttpClient != null) {
                    try {
                        // 关闭连接池
                        okHttpClient.connectionPool().evictAll();

                        // 关闭调度器
                        if (okHttpClient.dispatcher() != null && okHttpClient.dispatcher().executorService() != null) {
                            okHttpClient.dispatcher().executorService().shutdownNow();
                            try {
                                if (!okHttpClient.dispatcher().executorService().awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                                    LOG.w("GlideHelper", "OkHttpClient调度器未能在1秒内关闭");
                                }
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                LOG.w("GlideHelper", "等待OkHttpClient调度器关闭时被中断");
                            }
                        }

                        okHttpClient = null;
                        LOG.i("GlideHelper", "OkHttpClient已清理");
                    } catch (Exception e) {
                        LOG.w("GlideHelper", "清理OkHttpClient时发生错误: " + e.getMessage());
                    }
                }

                // 清理Glide缓存（安全方式）
                try {
                    Context appContext = App.getInstance();
                    if (appContext != null) {
                        // 清理内存缓存（主线程）
                        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
                            Glide.get(appContext).clearMemory();
                            LOG.i("GlideHelper", "Glide内存缓存已清理");
                        }

                        // 清理磁盘缓存（后台线程）
                        new Thread(() -> {
                            try {
                                Glide.get(appContext).clearDiskCache();
                                LOG.i("GlideHelper", "Glide磁盘缓存已清理");
                            } catch (Exception e) {
                                LOG.w("GlideHelper", "清理Glide磁盘缓存失败: " + e.getMessage());
                            }
                        }).start();
                    }
                } catch (Exception e) {
                    LOG.w("GlideHelper", "清理Glide缓存时发生错误: " + e.getMessage());
                }

                isCleanedUp = true;
                LOG.i("GlideHelper", "Glide资源清理完成");
            } catch (Exception e) {
                LOG.e("GlideHelper", "清理Glide资源时发生错误: " + e.getMessage());
            }
        }
    }

    /**
     * 重置清理状态（仅用于测试）
     */
    public static void resetCleanupState() {
        synchronized (cleanupLock) {
            isCleanedUp = false;
        }
    }

    /**
     * 处理图片URL，简化处理逻辑，参考TVBoxOS-Mobile
     * @param url 原始图片URL
     * @return 处理后的图片URL
     */
    private static String processImageUrl(String url) {
        if (android.text.TextUtils.isEmpty(url)) {
            return url;
        }

        // 直接使用DefaultConfig的处理方法，保持一致性
        return DefaultConfig.processImageUrl(url);
    }

    /**
     * 图片请求头拦截器
     * 为图片请求添加必要的请求头，解决防盗链问题
     */
    private static class ImageHeaderInterceptor implements okhttp3.Interceptor {
        @Override
        public okhttp3.Response intercept(Chain chain) throws java.io.IOException {
            okhttp3.Request originalRequest = chain.request();

            // 为图片请求添加通用请求头
            okhttp3.Request.Builder requestBuilder = originalRequest.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
                    .header("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Cache-Control", "no-cache")
                    .header("Pragma", "no-cache");

            // 根据域名添加特定的Referer
            String url = originalRequest.url().toString();
            String host = originalRequest.url().host();

            // 为常见的图片服务器添加Referer
            if (host.contains("douban") || host.contains("tmdb") || host.contains("imdb")) {
                requestBuilder.header("Referer", "https://" + host + "/");
            } else if (host.contains("ok") || host.contains("杰克") || host.contains("okjack")) {
                // 为ok杰克相关的图片服务器添加特殊处理
                requestBuilder.header("Referer", "https://" + host + "/");
                requestBuilder.header("Origin", "https://" + host);
            } else if (url.contains("pic") || url.contains("img") || url.contains("image")) {
                // 对于包含图片关键词的URL，添加通用Referer
                requestBuilder.header("Referer", "https://" + host + "/");
            }

            return chain.proceed(requestBuilder.build());
        }
    }
}
