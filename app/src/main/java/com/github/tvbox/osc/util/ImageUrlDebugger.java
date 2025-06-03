package com.github.tvbox.osc.util;

import android.text.TextUtils;
import android.util.Log;
import com.github.tvbox.osc.server.ControlManager;

/**
 * 图片URL调试工具
 * 专门用于调试"ok杰克"等视频源的图片URL处理问题
 */
public class ImageUrlDebugger {
    private static final String TAG = "ImageUrlDebugger";

    /**
     * 调试图片URL处理全流程
     * @param originalUrl 原始图片URL
     * @param sourceName 视频源名称
     * @param videoName 视频名称
     */
    public static void debugImageUrl(String originalUrl, String sourceName, String videoName) {
        Log.d(TAG, "=== 开始调试图片URL ===");
        Log.d(TAG, "视频源: " + sourceName);
        Log.d(TAG, "视频名称: " + videoName);
        Log.d(TAG, "原始URL: " + originalUrl);

        if (TextUtils.isEmpty(originalUrl)) {
            Log.w(TAG, "原始URL为空");
            return;
        }

        // 步骤1: 检查原始URL格式
        analyzeUrlFormat(originalUrl);

        // 步骤2: 检查proxy://处理
        String afterProxy = DefaultConfig.checkReplaceProxy(originalUrl);
        Log.d(TAG, "proxy处理后: " + afterProxy);

        // 步骤3: 检查图片URL处理
        String afterProcess = DefaultConfig.processImageUrl(afterProxy);
        Log.d(TAG, "图片处理后: " + afterProcess);

        // 步骤4: 检查是否包含无效地址
        checkInvalidAddress(afterProcess);

        Log.d(TAG, "=== 图片URL调试结束 ===");
    }

    /**
     * 分析URL格式
     */
    private static void analyzeUrlFormat(String url) {
        Log.d(TAG, "--- URL格式分析 ---");

        if (url.startsWith("http://")) {
            Log.d(TAG, "协议: HTTP");
        } else if (url.startsWith("https://")) {
            Log.d(TAG, "协议: HTTPS");
        } else if (url.startsWith("proxy://")) {
            Log.d(TAG, "协议: PROXY");
        } else if (url.startsWith("//")) {
            Log.d(TAG, "协议: 相对协议");
        } else {
            Log.d(TAG, "协议: 未知或相对路径");
        }

        // 检查是否包含域名
        if (url.contains("://")) {
            String[] parts = url.split("://");
            if (parts.length > 1) {
                String hostPart = parts[1].split("/")[0];
                Log.d(TAG, "域名: " + hostPart);

                // 检查是否是IP地址
                if (hostPart.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                    Log.d(TAG, "类型: IP地址");
                    if (hostPart.equals("0.0.0.0")) {
                        Log.w(TAG, "警告: 检测到无效IP地址 0.0.0.0");
                    }
                } else {
                    Log.d(TAG, "类型: 域名");
                }
            }
        }
    }

    /**
     * 检查无效地址
     */
    private static void checkInvalidAddress(String url) {
        Log.d(TAG, "--- 无效地址检查 ---");

        if (TextUtils.isEmpty(url)) {
            Log.w(TAG, "URL为空");
            return;
        }

        if (url.contains("0.0.0.0")) {
            Log.e(TAG, "发现无效地址: 0.0.0.0");
            Log.e(TAG, "完整URL: " + url);

            // 尝试分析0.0.0.0的来源
            if (url.startsWith("http://0.0.0.0")) {
                Log.e(TAG, "可能原因: proxy://协议处理时获取本地地址失败");
            }
        }

        if (url.contains("127.0.0.1")) {
            Log.i(TAG, "检测到本地地址: 127.0.0.1");
        }

        if (url.contains("localhost")) {
            Log.i(TAG, "检测到本地地址: localhost");
        }
    }

    /**
     * 检查ControlManager地址获取
     */
    public static void debugControlManagerAddress() {
        Log.d(TAG, "=== ControlManager地址调试 ===");

        try {
            String address = ControlManager.get().getAddress(true);
            Log.d(TAG, "ControlManager地址: " + address);

            if (address.contains("0.0.0.0")) {
                Log.e(TAG, "ControlManager返回无效地址: " + address);
                Log.e(TAG, "这可能是proxy://协议处理失败的原因");
            }
        } catch (Exception e) {
            Log.e(TAG, "获取ControlManager地址失败: " + e.getMessage());
        }

        Log.d(TAG, "=== ControlManager地址调试结束 ===");
    }

    /**
     * 为ok杰克源专门的调试方法
     */
    public static void debugOkJackImages() {
        Log.d(TAG, "=== ok杰克图片URL专项调试 ===");

        // 模拟一些可能的ok杰克图片URL格式
        String[] testUrls = {
            "proxy://example.com/pic.jpg",
            "http://ok321.top/pic.jpg",
            "https://img.ok321.top/pic.jpg",
            "//img.ok321.top/pic.jpg",
            "/pic.jpg"
        };

        for (String testUrl : testUrls) {
            Log.d(TAG, "测试URL: " + testUrl);
            debugImageUrl(testUrl, "ok杰克", "测试视频");
            Log.d(TAG, "---");
        }

        Log.d(TAG, "=== ok杰克图片URL专项调试结束 ===");
    }
}
