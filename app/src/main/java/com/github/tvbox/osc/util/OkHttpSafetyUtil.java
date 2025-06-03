package com.github.tvbox.osc.util;

import okhttp3.Dns;
import okhttp3.OkHttpClient;

/**
 * OkHttp安全工具类
 * 用于确保所有OkHttp客户端实例都使用安全的DNS设置
 * 避免在Android 15及以上版本中出现空指针异常
 */
public class OkHttpSafetyUtil {

    /**
     * 确保OkHttpClient.Builder使用安全的DNS设置
     * 如果传入的DNS为null，则使用系统DNS
     * 
     * @param builder OkHttpClient.Builder实例
     * @param dns DNS实例，可以为null
     * @return 设置了安全DNS的builder实例
     */
    public static OkHttpClient.Builder ensureSafeDns(OkHttpClient.Builder builder, Dns dns) {
        // 如果DNS为null，使用系统DNS
        builder.dns(dns != null ? dns : Dns.SYSTEM);
        return builder;
    }

    /**
     * 获取安全的DNS实例
     * 如果传入的DNS为null，则返回系统DNS
     * 
     * @param dns DNS实例，可以为null
     * @return 非null的DNS实例
     */
    public static Dns getSafeDns(Dns dns) {
        return dns != null ? dns : Dns.SYSTEM;
    }
}
