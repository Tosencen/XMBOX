package com.github.tvbox.osc.util;

import com.orhanobut.hawk.Hawk;

/**
 * 应用重启配置管理
 * 管理应用自动重启相关的配置参数
 */
public class AppRestartConfig {
    private static final String TAG = "AppRestartConfig";

    // 配置键名（使用HawkConfig中定义的键）
    public static final String KEY_AUTO_RESTART_ENABLED = HawkConfig.AUTO_RESTART_ENABLED;
    public static final String KEY_RESTART_THRESHOLD_HOURS = HawkConfig.RESTART_THRESHOLD_HOURS;
    public static final String KEY_SHOW_RESTART_TOAST = HawkConfig.SHOW_RESTART_TOAST;
    public static final String KEY_CLEANUP_ON_RESTART = HawkConfig.CLEANUP_ON_RESTART;

    // 默认值
    public static final boolean DEFAULT_AUTO_RESTART_ENABLED = true;
    public static final int DEFAULT_RESTART_THRESHOLD_HOURS = 3;
    public static final boolean DEFAULT_SHOW_RESTART_TOAST = true;
    public static final boolean DEFAULT_CLEANUP_ON_RESTART = true;

    /**
     * 初始化默认配置
     */
    public static void initDefaults() {
        if (!Hawk.contains(KEY_AUTO_RESTART_ENABLED)) {
            Hawk.put(KEY_AUTO_RESTART_ENABLED, DEFAULT_AUTO_RESTART_ENABLED);
        }
        if (!Hawk.contains(KEY_RESTART_THRESHOLD_HOURS)) {
            Hawk.put(KEY_RESTART_THRESHOLD_HOURS, DEFAULT_RESTART_THRESHOLD_HOURS);
        }
        if (!Hawk.contains(KEY_SHOW_RESTART_TOAST)) {
            Hawk.put(KEY_SHOW_RESTART_TOAST, DEFAULT_SHOW_RESTART_TOAST);
        }
        if (!Hawk.contains(KEY_CLEANUP_ON_RESTART)) {
            Hawk.put(KEY_CLEANUP_ON_RESTART, DEFAULT_CLEANUP_ON_RESTART);
        }

        LOG.i(TAG, "应用重启配置已初始化");
    }

    /**
     * 是否启用自动重启
     */
    public static boolean isAutoRestartEnabled() {
        return Hawk.get(KEY_AUTO_RESTART_ENABLED, DEFAULT_AUTO_RESTART_ENABLED);
    }

    /**
     * 设置是否启用自动重启
     */
    public static void setAutoRestartEnabled(boolean enabled) {
        Hawk.put(KEY_AUTO_RESTART_ENABLED, enabled);
        LOG.i(TAG, "自动重启设置已更新: " + enabled);
    }

    /**
     * 获取重启阈值（小时）
     */
    public static int getRestartThresholdHours() {
        return Hawk.get(KEY_RESTART_THRESHOLD_HOURS, DEFAULT_RESTART_THRESHOLD_HOURS);
    }

    /**
     * 设置重启阈值（小时）
     */
    public static void setRestartThresholdHours(int hours) {
        if (hours < 1) {
            hours = 1; // 最少1小时
        } else if (hours > 24) {
            hours = 24; // 最多24小时
        }

        Hawk.put(KEY_RESTART_THRESHOLD_HOURS, hours);
        LOG.i(TAG, "重启阈值已更新: " + hours + "小时");
    }

    /**
     * 获取重启阈值（毫秒）
     */
    public static long getRestartThresholdMillis() {
        return getRestartThresholdHours() * 60 * 60 * 1000L;
    }

    /**
     * 是否显示重启提示
     */
    public static boolean isShowRestartToast() {
        return Hawk.get(KEY_SHOW_RESTART_TOAST, DEFAULT_SHOW_RESTART_TOAST);
    }

    /**
     * 设置是否显示重启提示
     */
    public static void setShowRestartToast(boolean show) {
        Hawk.put(KEY_SHOW_RESTART_TOAST, show);
        LOG.i(TAG, "重启提示设置已更新: " + show);
    }

    /**
     * 是否在重启时清理数据
     */
    public static boolean isCleanupOnRestart() {
        return Hawk.get(KEY_CLEANUP_ON_RESTART, DEFAULT_CLEANUP_ON_RESTART);
    }

    /**
     * 设置是否在重启时清理数据
     */
    public static void setCleanupOnRestart(boolean cleanup) {
        Hawk.put(KEY_CLEANUP_ON_RESTART, cleanup);
        LOG.i(TAG, "重启清理设置已更新: " + cleanup);
    }

    /**
     * 获取配置摘要
     */
    public static String getConfigSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("应用重启配置:\n");
        summary.append("- 自动重启: ").append(isAutoRestartEnabled() ? "启用" : "禁用").append("\n");
        summary.append("- 重启阈值: ").append(getRestartThresholdHours()).append("小时\n");
        summary.append("- 显示提示: ").append(isShowRestartToast() ? "是" : "否").append("\n");
        summary.append("- 重启清理: ").append(isCleanupOnRestart() ? "是" : "否");
        return summary.toString();
    }

    /**
     * 重置为默认配置
     */
    public static void resetToDefaults() {
        Hawk.put(KEY_AUTO_RESTART_ENABLED, DEFAULT_AUTO_RESTART_ENABLED);
        Hawk.put(KEY_RESTART_THRESHOLD_HOURS, DEFAULT_RESTART_THRESHOLD_HOURS);
        Hawk.put(KEY_SHOW_RESTART_TOAST, DEFAULT_SHOW_RESTART_TOAST);
        Hawk.put(KEY_CLEANUP_ON_RESTART, DEFAULT_CLEANUP_ON_RESTART);

        LOG.i(TAG, "应用重启配置已重置为默认值");
    }

    /**
     * 验证配置的有效性
     */
    public static boolean validateConfig() {
        try {
            int thresholdHours = getRestartThresholdHours();
            if (thresholdHours < 1 || thresholdHours > 24) {
                LOG.w(TAG, "重启阈值无效: " + thresholdHours + "小时，重置为默认值");
                setRestartThresholdHours(DEFAULT_RESTART_THRESHOLD_HOURS);
                return false;
            }

            LOG.d(TAG, "配置验证通过");
            return true;
        } catch (Exception e) {
            LOG.e(TAG, "配置验证失败: " + e.getMessage());
            resetToDefaults();
            return false;
        }
    }

    /**
     * 导出配置
     */
    public static String exportConfig() {
        try {
            StringBuilder config = new StringBuilder();
            config.append("auto_restart_enabled=").append(isAutoRestartEnabled()).append("\n");
            config.append("restart_threshold_hours=").append(getRestartThresholdHours()).append("\n");
            config.append("show_restart_toast=").append(isShowRestartToast()).append("\n");
            config.append("cleanup_on_restart=").append(isCleanupOnRestart()).append("\n");

            return config.toString();
        } catch (Exception e) {
            LOG.e(TAG, "导出配置失败: " + e.getMessage());
            return "";
        }
    }

    /**
     * 导入配置
     */
    public static boolean importConfig(String configString) {
        try {
            if (configString == null || configString.trim().isEmpty()) {
                return false;
            }

            String[] lines = configString.split("\n");
            for (String line : lines) {
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    switch (key) {
                        case "auto_restart_enabled":
                            setAutoRestartEnabled(Boolean.parseBoolean(value));
                            break;
                        case "restart_threshold_hours":
                            setRestartThresholdHours(Integer.parseInt(value));
                            break;
                        case "show_restart_toast":
                            setShowRestartToast(Boolean.parseBoolean(value));
                            break;
                        case "cleanup_on_restart":
                            setCleanupOnRestart(Boolean.parseBoolean(value));
                            break;
                    }
                }
            }

            // 验证导入的配置
            validateConfig();

            LOG.i(TAG, "配置导入成功");
            return true;
        } catch (Exception e) {
            LOG.e(TAG, "导入配置失败: " + e.getMessage());
            return false;
        }
    }
}
