package com.fongmi.android.tv.utils;

import android.text.TextUtils;

import com.fongmi.android.tv.Setting;
import com.github.catvod.utils.Logger;
import com.thegrizzlylabs.sardineandroid.Sardine;
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine;

/**
 * WebDAV 配置管理 — 负责加载、校验和重新加载同步配置。
 *
 * 从 WebDAVSyncManager 拆分而来，职责单一化。
 */
public class WebDAVConfig {

    // 同步模式：ACCOUNT（账号模式）或 CODE（同步码模式）
    public enum SyncMode {
        ACCOUNT,  // 使用WebDAV账号
        CODE      // 使用同步码（无需账号）
    }

    private Sardine sardine;
    private String baseUrl;
    private String username;
    private String password;
    private String syncCode;
    private SyncMode syncMode = SyncMode.ACCOUNT;

    public WebDAVConfig() {
        load();
    }

    /** 加载配置 */
    public void load() {
        String modeStr = Setting.getWebDAVSyncMode();
        if ("CODE".equals(modeStr)) {
            syncMode = SyncMode.CODE;
            syncCode = Setting.getWebDAVSyncCode();
            baseUrl = getPublicStorageUrl();
            username = null;
            password = null;
        } else {
            syncMode = SyncMode.ACCOUNT;
            baseUrl = Setting.getWebDAVUrl();
            username = Setting.getWebDAVUsername();
            password = Setting.getWebDAVPassword();
        }

        if (syncMode == SyncMode.ACCOUNT) {
            if (!TextUtils.isEmpty(baseUrl) && !TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
                initSardine();
            } else {
                sardine = null;
            }
        } else {
            if (!TextUtils.isEmpty(syncCode) && !TextUtils.isEmpty(baseUrl)) {
                initSardine();
            } else {
                sardine = null;
            }
        }
    }

    private void initSardine() {
        try {
            sardine = new OkHttpSardine();
            if (syncMode == SyncMode.ACCOUNT) {
                sardine.setCredentials(username, password);
            }
            Logger.d("WebDAV: " + syncMode + " 模式配置已加载");
        } catch (Exception e) {
            Logger.e("WebDAV: 初始化失败: " + e.getMessage());
            sardine = null;
        }
    }

    /** 配置是否有效 */
    public boolean isConfigured() {
        if (syncMode == SyncMode.CODE) {
            return sardine != null && !TextUtils.isEmpty(baseUrl) && !TextUtils.isEmpty(syncCode);
        } else {
            return sardine != null && !TextUtils.isEmpty(baseUrl) && !TextUtils.isEmpty(username) && !TextUtils.isEmpty(password);
        }
    }

    // ==================== 访问器 ====================

    public Sardine getSardine() { return sardine; }
    public String getBaseUrl() { return baseUrl; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getSyncCode() { return syncCode; }
    public SyncMode getSyncMode() { return syncMode; }
    public boolean isCodeMode() { return syncMode == SyncMode.CODE; }

    /** 获取文件完整 URL */
    public String getFileUrl(String filename) {
        String url = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        return url + filename;
    }

    /** 确保目录存在 */
    public void ensureDirectory() throws Exception {
        if (!isCodeMode() && !TextUtils.isEmpty(baseUrl)) {
            String dirUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
            try {
                if (!sardine.exists(dirUrl)) {
                    sardine.createDirectory(dirUrl);
                    Logger.d("WebDAV: 创建目录: " + dirUrl);
                }
            } catch (Exception e) {
                Logger.w("WebDAV: 创建目录失败: " + e.getMessage());
                throw e;
            }
        }
    }

    // ==================== 工具方法 ====================

    /** 生成 8 位随机同步码 */
    public static String generateSyncCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        java.util.Random random = new java.util.Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    /** 获取公开存储 URL（同步码模式使用） */
    private String getPublicStorageUrl() {
        String gistBaseUrl = Setting.getWebDAVPublicUrl();
        if (TextUtils.isEmpty(gistBaseUrl)) return null;
        if (!TextUtils.isEmpty(syncCode)) {
            String url = gistBaseUrl.endsWith("/") ? gistBaseUrl : gistBaseUrl + "/";
            return url + syncCode + "/";
        }
        return gistBaseUrl;
    }

    // ==================== 测试连接 ====================

    /** 测试结果 */
    public static class TestResult {
        public final boolean success;
        public final String message;
        public TestResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    /** 测试连接 */
    public TestResult testConnection() {
        if (!isConfigured()) {
            return new TestResult(false, "WebDAV未配置，请检查URL、用户名和密码");
        }
        try {
            String testUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
            sardine.list(testUrl);
            return new TestResult(true, "连接成功！");
        } catch (java.io.IOException e) {
            return new TestResult(false, formatError(e.getMessage()));
        } catch (Exception e) {
            return new TestResult(false, "连接失败：" + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    private String formatError(String errorMsg) {
        if (errorMsg == null) return "未知错误";
        if (errorMsg.contains("401") || errorMsg.contains("Unauthorized"))
            return "认证失败：用户名或密码错误。\n提示：坚果云需要使用应用密码，不是登录密码";
        if (errorMsg.contains("403") || errorMsg.contains("Forbidden"))
            return "访问被拒绝：账号可能没有WebDAV权限";
        if (errorMsg.contains("404") || errorMsg.contains("Not Found"))
            return "URL不存在：请检查WebDAV服务器地址是否正确";
        if (errorMsg.contains("SSL") || errorMsg.contains("Certificate"))
            return "SSL证书错误：请检查服务器证书是否有效";
        if (errorMsg.contains("timeout") || errorMsg.contains("Timeout"))
            return "连接超时：请检查网络连接或服务器地址";
        if (errorMsg.contains("UnknownHost") || errorMsg.contains("unreachable"))
            return "无法连接到服务器：请检查网络连接和服务器地址";
        return "连接失败：" + errorMsg;
    }
}