package com.github.tvbox.osc.util;

import android.content.Context;
import android.util.Log;
import android.widget.ImageView;

/**
 * 图片加载测试工具类
 * 用于测试和调试图片加载问题
 */
public class ImageLoadTest {
    private static final String TAG = "ImageLoadTest";

    /**
     * 测试图片URL处理
     * @param url 原始URL
     * @return 处理后的URL
     */
    public static String testProcessImageUrl(String url) {
        Log.d(TAG, "原始URL: " + url);
        String processedUrl = DefaultConfig.processImageUrl(url);
        Log.d(TAG, "处理后URL: " + processedUrl);
        return processedUrl;
    }

    /**
     * 测试图片加载
     * @param imageView 目标ImageView
     * @param url 图片URL
     */
    public static void testLoadImage(ImageView imageView, String url) {
        Log.d(TAG, "开始测试图片加载: " + url);
        
        // 测试URL处理
        String processedUrl = testProcessImageUrl(url);
        
        // 使用GlideHelper加载图片
        GlideHelper.loadImage(imageView, processedUrl);
        
        Log.d(TAG, "图片加载请求已发送");
    }

    /**
     * 测试常见的图片URL格式
     * @param context 上下文
     */
    public static void testCommonImageUrls(Context context) {
        String[] testUrls = {
            "https://example.com/image.jpg",
            "//example.com/image.jpg",
            "http://饭太硬.com/image.jpg",
            "https://img.example.com/poster/123.jpg",
            "proxy://example.com/image.jpg",
            "",
            null
        };

        for (String url : testUrls) {
            Log.d(TAG, "=== 测试URL: " + url + " ===");
            testProcessImageUrl(url);
        }
    }
}
