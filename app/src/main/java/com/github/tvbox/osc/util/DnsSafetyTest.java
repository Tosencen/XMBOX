package com.github.tvbox.osc.util;

import android.util.Log;

import okhttp3.Dns;
import okhttp3.OkHttpClient;

/**
 * DNS安全测试工具类
 * 用于验证DNS安全修复是否有效
 */
public class DnsSafetyTest {
    private static final String TAG = "DnsSafetyTest";

    /**
     * 测试DNS安全工具类
     * @return 测试是否通过
     */
    public static boolean testDnsSafety() {
        try {
            // 测试1: 传入null DNS
            OkHttpClient.Builder builder1 = new OkHttpClient.Builder();
            OkHttpSafetyUtil.ensureSafeDns(builder1, null);
            OkHttpClient client1 = builder1.build();
            Log.d(TAG, "Test 1 passed: Successfully built OkHttpClient with null DNS");

            // 测试2: 传入系统DNS
            OkHttpClient.Builder builder2 = new OkHttpClient.Builder();
            OkHttpSafetyUtil.ensureSafeDns(builder2, Dns.SYSTEM);
            OkHttpClient client2 = builder2.build();
            Log.d(TAG, "Test 2 passed: Successfully built OkHttpClient with system DNS");

            // 测试3: 获取安全DNS
            Dns safeDns = OkHttpSafetyUtil.getSafeDns(null);
            if (safeDns == null) {
                Log.e(TAG, "Test 3 failed: getSafeDns returned null");
                return false;
            }
            Log.d(TAG, "Test 3 passed: getSafeDns returned non-null DNS");

            // 测试4: Spider.safeDns()
            Dns spiderDns = com.github.catvod.crawler.Spider.safeDns();
            if (spiderDns == null) {
                Log.e(TAG, "Test 4 failed: Spider.safeDns() returned null");
                return false;
            }
            Log.d(TAG, "Test 4 passed: Spider.safeDns() returned non-null DNS");

            // 测试5: OkHttp.dns()
            Dns okHttpDns = com.github.catvod.net.OkHttp.dns();
            if (okHttpDns == null) {
                Log.e(TAG, "Test 5 failed: OkHttp.dns() returned null");
                return false;
            }
            Log.d(TAG, "Test 5 passed: OkHttp.dns() returned non-null DNS");

            Log.d(TAG, "All DNS safety tests passed!");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "DNS safety test failed with exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
