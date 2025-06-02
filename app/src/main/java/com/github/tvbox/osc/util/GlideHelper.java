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
                    .addInterceptor(new BrotliInterceptor());

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

        android.util.Log.d("GlideHelper", "加载图片: " + processedUrl);

        Glide.with(imageView.getContext())
                .load(processedUrl)
                .placeholder(R.drawable.img_loading_placeholder)
                .error(R.drawable.img_loading_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .dontAnimate() // 禁用动画以提高性能
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        android.util.Log.w("GlideHelper", "图片加载失败: " + processedUrl + ", 错误: " + (e != null ? e.getMessage() : "未知"));
                        return false; // 返回false让Glide显示error图片
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        android.util.Log.d("GlideHelper", "图片加载成功: " + processedUrl);
                        return false; // 返回false让Glide正常显示图片
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

    /**
     * 清理Glide相关资源，防止内存泄漏
     * 建议在Application的onTerminate()中调用
     */
    public static void cleanup() {
        try {
            // 清理OkHttpClient
            if (okHttpClient != null) {
                // 关闭连接池
                okHttpClient.connectionPool().evictAll();
                // 强制关闭调度器
                try {
                    okHttpClient.dispatcher().executorService().shutdownNow();
                    if (!okHttpClient.dispatcher().executorService().awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                        LOG.w("GlideHelper", "Glide OkHttpClient调度器未能在2秒内关闭");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOG.w("GlideHelper", "等待Glide OkHttpClient调度器关闭时被中断");
                }
                okHttpClient = null;
            }

            // 尝试清理Glide的内部线程池
            try {
                Context appContext = App.getInstance();
                if (appContext != null) {
                    // 清理Glide实例
                    Glide glide = Glide.get(appContext);

                    // 使用反射清理GlideExecutor
                    try {
                        java.lang.reflect.Field engineField = Glide.class.getDeclaredField("engine");
                        engineField.setAccessible(true);
                        Object engine = engineField.get(glide);

                        if (engine != null) {
                            // 清理Engine中的线程池
                            java.lang.reflect.Field sourceExecutorField = engine.getClass().getDeclaredField("sourceExecutor");
                            sourceExecutorField.setAccessible(true);
                            Object sourceExecutor = sourceExecutorField.get(engine);

                            if (sourceExecutor != null && sourceExecutor instanceof java.util.concurrent.ExecutorService) {
                                ((java.util.concurrent.ExecutorService) sourceExecutor).shutdownNow();
                                LOG.i("GlideHelper", "Glide sourceExecutor已关闭");
                            }

                            java.lang.reflect.Field diskCacheExecutorField = engine.getClass().getDeclaredField("diskCacheExecutor");
                            diskCacheExecutorField.setAccessible(true);
                            Object diskCacheExecutor = diskCacheExecutorField.get(engine);

                            if (diskCacheExecutor != null && diskCacheExecutor instanceof java.util.concurrent.ExecutorService) {
                                ((java.util.concurrent.ExecutorService) diskCacheExecutor).shutdownNow();
                                LOG.i("GlideHelper", "Glide diskCacheExecutor已关闭");
                            }
                        }
                    } catch (Exception e) {
                        LOG.w("GlideHelper", "清理Glide内部线程池时发生错误: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                LOG.w("GlideHelper", "清理Glide实例时发生错误: " + e.getMessage());
            }

            LOG.i("GlideHelper", "Glide资源清理完成");
        } catch (Exception e) {
            LOG.e("GlideHelper", "清理Glide资源时发生错误: " + e.getMessage());
        }
    }

    /**
     * 处理图片URL的特殊情况
     * @param url 原始图片URL
     * @return 处理后的图片URL
     */
    private static String processImageUrl(String url) {
        if (android.text.TextUtils.isEmpty(url)) {
            return url;
        }

        try {
            // 去除URL前后的空白字符
            url = url.trim();

            // 处理中文域名的图片URL
            if (url.contains("饭太硬")) {
                android.util.Log.d("GlideHelper", "检测到饭太硬域名图片URL: " + url);
                // 使用UrlUtil处理中文域名
                url = UrlUtil.convertToPunycode(url);
                android.util.Log.d("GlideHelper", "转换后的图片URL: " + url);
            }

            // 处理相对路径的图片URL
            if (url.startsWith("//")) {
                url = "https:" + url;
                android.util.Log.d("GlideHelper", "补充协议后的图片URL: " + url);
            } else if (url.startsWith("/") && !url.startsWith("//")) {
                // 相对路径，需要根据当前数据源的域名补充
                android.util.Log.d("GlideHelper", "检测到相对路径图片URL: " + url);
            }

            // 检查URL是否有效
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                android.util.Log.w("GlideHelper", "无效的图片URL协议: " + url);
                return url; // 返回原始URL，让Glide处理
            }

            return url;
        } catch (Exception e) {
            android.util.Log.e("GlideHelper", "处理图片URL时发生错误: " + e.getMessage());
            return url; // 发生错误时返回原始URL
        }
    }
}
