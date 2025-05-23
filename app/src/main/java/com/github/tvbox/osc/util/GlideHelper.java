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
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.util.urlhttp.BrotliInterceptor;
import com.github.tvbox.osc.util.OkHttpSafetyUtil;
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

        Glide.with(imageView.getContext())
                .load(url)
                .placeholder(R.drawable.img_loading_placeholder)
                .error(R.drawable.img_loading_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .dontAnimate() // 禁用动画以提高性能
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

        Glide.with(imageView.getContext())
                .load(url)
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

        Glide.with(imageView.getContext())
                .load(url)
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

        Glide.with(imageView.getContext())
                .load(url)
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
        Glide.with(context)
                .load(url)
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
}
