package com.github.tvbox.osc.util;

import android.content.Context;
import android.widget.ImageView;

/**
 * 图片加载测试助手
 * 用于测试和验证图片加载功能
 */
public class ImageTestHelper {
    private static final String TAG = "ImageTestHelper";

    /**
     * 测试常见的图片URL
     */
    public static void testCommonImageUrls(Context context, ImageView imageView) {
        String[] testUrls = {
            // 豆瓣图片
            "https://img1.doubanio.com/view/photo/s_ratio_poster/public/p2561716440.jpg",
            // TMDB图片
            "https://image.tmdb.org/t/p/w500/8UlWHLMpgZm9bx6QYh0NFoq67TZ.jpg",
            // 通用测试图片
            "https://via.placeholder.com/300x400/FF0000/FFFFFF?text=Test",
            // 相对协议URL
            "//img1.doubanio.com/view/photo/s_ratio_poster/public/p2561716440.jpg"
        };

        LOG.i(TAG, "开始测试图片URL");
        
        for (int i = 0; i < testUrls.length; i++) {
            final String url = testUrls[i];
            final int index = i;
            
            LOG.i(TAG, "测试URL " + (index + 1) + ": " + url);
            
            // 延迟加载，避免同时请求过多
            ThreadPoolManager.executeMainDelayed(() -> {
                try {
                    GlideHelper.loadImage(imageView, url);
                    LOG.i(TAG, "已发送加载请求: " + url);
                } catch (Exception e) {
                    LOG.e(TAG, "加载图片时发生错误: " + e.getMessage());
                }
            }, index * 3000); // 每3秒测试一个URL
        }
    }

    /**
     * 测试URL处理功能
     */
    public static void testUrlProcessing() {
        String[] testUrls = {
            "https://img1.doubanio.com/test.jpg",
            "//img1.doubanio.com/test.jpg",
            "http://饭太硬.com/test.jpg",
            "proxy://example.com/test.jpg",
            "",
            null
        };

        LOG.i(TAG, "开始测试URL处理");
        
        for (String url : testUrls) {
            try {
                String processed = DefaultConfig.checkReplaceProxy(url);
                LOG.i(TAG, "原始URL: " + url + " -> 处理后: " + processed);
            } catch (Exception e) {
                LOG.e(TAG, "处理URL时发生错误: " + url + ", 错误: " + e.getMessage());
            }
        }
    }

    /**
     * 测试订阅源中的图片URL
     */
    public static void testSubscriptionImageUrls(Context context, ImageView imageView) {
        // 这些是从实际订阅源中提取的图片URL示例
        String[] subscriptionUrls = {
            "https://pic.rmb.bdstatic.com/bjh/240328/8c5c8f8f8c5c8f8f8c5c8f8f.jpg",
            "https://img.alicdn.com/imgextra/i1/2208857268603/O1CN01.jpg",
            "https://p0.meituan.net/movie/test.jpg"
        };

        LOG.i(TAG, "开始测试订阅源图片URL");
        
        for (int i = 0; i < subscriptionUrls.length; i++) {
            final String url = subscriptionUrls[i];
            final int index = i;
            
            ThreadPoolManager.executeMainDelayed(() -> {
                LOG.i(TAG, "测试订阅源图片: " + url);
                GlideHelper.loadImage(imageView, url);
            }, index * 2000);
        }
    }
}
