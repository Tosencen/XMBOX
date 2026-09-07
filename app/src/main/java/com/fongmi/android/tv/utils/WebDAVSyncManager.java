package com.fongmi.android.tv.utils;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.bean.Backup;
import com.github.catvod.utils.Logger;
import com.github.catvod.utils.Prefers;
import com.google.gson.Gson;

import java.io.InputStream;
import java.util.Map;

/**
 * WebDAV同步管理器 — 统筹观看记录、设置、完整备份的同步。
 *
 * 配置管理委托给 {@link WebDAVConfig}，历史记录同步委托给 {@link WebDAVHistorySync}。
 * 职责：编排同步流程、设置同步、备份同步。
 */
public class WebDAVSyncManager {

    private static final String SETTINGS_FILE = "xmbox_settings.json";
    private static final String BACKUP_FILE = "xmbox_backup.json";

    private static WebDAVSyncManager instance;
    private final WebDAVConfig config;
    private final WebDAVHistorySync historySync;
    private volatile boolean isSyncing = false;

    public static WebDAVSyncManager get() {
        if (instance == null) {
            instance = new WebDAVSyncManager();
        }
        return instance;
    }

    private WebDAVSyncManager() {
        this.config = new WebDAVConfig();
        this.historySync = new WebDAVHistorySync(config);
    }

    // ==================== 配置 ====================

    public WebDAVConfig getConfig() { return config; }

    public boolean isConfigured() { return config.isConfigured(); }

    public void reloadConfig() { config.load(); }

    // ==================== 历史记录同步 ====================

    public boolean uploadHistory() { return historySync.upload(); }

    public boolean downloadHistory() { return historySync.downloadAndMerge(); }

    public boolean syncHistory(boolean async) {
        return syncWithLock(async, () -> {
            uploadHistory();
            downloadHistory();
        });
    }

    public boolean syncHistory() { return syncHistory(true); }

    // ==================== 设置同步 ====================

    public boolean uploadSettings() {
        if (!config.isConfigured()) return false;
        try {
            Map<String, ?> allPrefs = Prefers.getPrefers().getAll();
            String json = App.gson().toJson(allPrefs);
            config.ensureDirectory();
            config.getSardine().put(config.getFileUrl(SETTINGS_FILE), json.getBytes("UTF-8"));
            Logger.d("WebDAV: 设置上传成功");
            return true;
        } catch (Exception e) {
            Logger.e("WebDAV: 设置上传失败: " + e.getMessage());
            return false;
        }
    }

    public boolean downloadSettings() {
        if (!config.isConfigured()) return false;
        try {
            String fileUrl = config.getFileUrl(SETTINGS_FILE);
            if (!config.getSardine().exists(fileUrl)) return false;

            InputStream is = config.getSardine().get(fileUrl);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();

            String json = new String(buffer, "UTF-8");
            Map<String, Object> settings = App.gson().fromJson(json, Map.class);
            if (settings != null) {
                for (Map.Entry<String, Object> entry : settings.entrySet()) {
                    if (!shouldSkipSetting(entry.getKey())) {
                        Prefers.put(entry.getKey(), entry.getValue());
                    }
                }
                Logger.d("WebDAV: 设置下载成功");
                return true;
            }
            return false;
        } catch (Exception e) {
            Logger.e("WebDAV: 设置下载失败: " + e.getMessage());
            return false;
        }
    }

    public boolean syncSettings(boolean async) {
        return syncSimple(async, () -> {
            uploadSettings();
            downloadSettings();
        });
    }

    public boolean syncSettings() { return syncSettings(true); }

    // ==================== 完整备份同步 ====================

    public boolean uploadBackup() {
        if (!config.isConfigured()) return false;
        try {
            Backup backup = Backup.create();
            String json = backup.toString();
            config.ensureDirectory();
            config.getSardine().put(config.getFileUrl(BACKUP_FILE), json.getBytes("UTF-8"));
            Logger.d("WebDAV: 完整备份上传成功");
            return true;
        } catch (Exception e) {
            Logger.e("WebDAV: 完整备份上传失败: " + e.getMessage());
            return false;
        }
    }

    public boolean downloadBackup() {
        if (!config.isConfigured()) return false;
        try {
            String fileUrl = config.getFileUrl(BACKUP_FILE);
            if (!config.getSardine().exists(fileUrl)) return false;

            InputStream is = config.getSardine().get(fileUrl);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();

            String json = new String(buffer, "UTF-8");
            Backup backup = Backup.objectFrom(json);
            if (!backup.getConfig().isEmpty()) {
                backup.restore();
                Logger.d("WebDAV: 完整备份下载并恢复成功");
                return true;
            }
            return false;
        } catch (Exception e) {
            Logger.e("WebDAV: 完整备份下载失败: " + e.getMessage());
            return false;
        }
    }

    // ==================== 全量同步 ====================

    public boolean syncAll(boolean async) {
        return syncWithLock(async, () -> {
            uploadHistory();
            downloadHistory();
            syncSettings(false);
        });
    }

    public boolean syncAll() { return syncAll(true); }

    // ==================== 内部工具 ====================

    private boolean shouldSkipSetting(String key) {
        return key.startsWith("webdav_") || "device_uuid".equals(key) || "device_name".equals(key);
    }

    private boolean syncWithLock(boolean async, Runnable task) {
        if (!config.isConfigured()) return false;
        if (isSyncing) {
            Logger.w("WebDAV: 同步正在进行中，跳过本次请求");
            return false;
        }
        Runnable wrapped = () -> {
            try {
                isSyncing = true;
                task.run();
            } finally {
                isSyncing = false;
            }
        };
        if (async) App.execute(wrapped);
        else wrapped.run();
        return true;
    }

    private boolean syncSimple(boolean async, Runnable task) {
        if (!config.isConfigured()) return false;
        if (async) App.execute(task);
        else task.run();
        return true;
    }
}