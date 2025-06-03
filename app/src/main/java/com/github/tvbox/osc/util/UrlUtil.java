package com.github.tvbox.osc.util;

import android.text.TextUtils;
import android.util.Log;

import java.net.IDN;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * URL处理工具类，用于处理中文域名等特殊URL
 */
public class UrlUtil {
    private static final String TAG = "UrlUtil";

    // 特殊URL映射表，用于处理一些特殊的URL
    private static final Map<String, String> SPECIAL_URL_MAP = new HashMap<>();
    static {
        // 添加已知的特殊URL映射
        SPECIAL_URL_MAP.put("http://www.饭太硬.com/tv", "http://饭太硬.top/tv");
        SPECIAL_URL_MAP.put("http://饭太硬.top/tv/live.txt", "http://饭太硬.top/tv/live.txt");
        // 可以添加更多特殊URL映射
    }

    /**
     * 处理URL，包括特殊URL映射和Punycode转换
     *
     * @param url 原始URL
     * @return 处理后的URL
     */
    public static String processUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }

        // 对于直播源URL，可能需要特殊处理
        if (url.contains("/live.txt") || url.contains(".m3u") || url.contains(".m3u8") || url.contains(".ts")) {
            Log.d(TAG, "直播源URL: " + url);
            // 1. 检查是否是特殊URL
            if (SPECIAL_URL_MAP.containsKey(url)) {
                String mappedUrl = SPECIAL_URL_MAP.get(url);
                Log.d(TAG, "Special URL mapped: " + url + " -> " + mappedUrl);
                url = mappedUrl;
            }

            // 2. 进行Punycode转换
            String punycodeUrl = convertToPunycode(url);
            Log.d(TAG, "直播源URL转换: " + url + " -> " + punycodeUrl);
            return punycodeUrl;
        }

        // 1. 检查是否是特殊URL
        if (SPECIAL_URL_MAP.containsKey(url)) {
            String mappedUrl = SPECIAL_URL_MAP.get(url);
            Log.d(TAG, "Special URL mapped: " + url + " -> " + mappedUrl);
            url = mappedUrl;
        }

        // 2. 进行Punycode转换
        return convertToPunycode(url);
    }

    /**
     * 将URL中的中文域名转换为Punycode格式
     * 例如：http://www.饭太硬.com/tv -> http://www.xn--i2s25uzvs.com/tv
     *
     * @param url 原始URL
     * @return 转换后的URL
     */
    public static String convertToPunycode(String url) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }

        try {
            URL originalUrl = new URL(url);
            String host = originalUrl.getHost();

            // 检查是否包含非ASCII字符
            if (!host.equals(host.replaceAll("[^\\x00-\\x7F]", ""))) {
                String punycodeHost = IDN.toASCII(host);
                Log.d(TAG, "Converting domain: " + host + " -> " + punycodeHost);

                // 重建URL
                String protocol = originalUrl.getProtocol();
                int port = originalUrl.getPort();
                String path = originalUrl.getPath();
                String query = originalUrl.getQuery();
                String ref = originalUrl.getRef();

                StringBuilder newUrl = new StringBuilder();
                newUrl.append(protocol).append("://").append(punycodeHost);

                if (port != -1) {
                    newUrl.append(":").append(port);
                }

                if (path != null) {
                    newUrl.append(path);
                }

                if (query != null) {
                    newUrl.append("?").append(query);
                }

                if (ref != null) {
                    newUrl.append("#").append(ref);
                }

                return newUrl.toString();
            }
            return url;
        } catch (MalformedURLException e) {
            Log.e(TAG, "Error converting URL to Punycode: " + e.getMessage());
            return url;
        }
    }
}
