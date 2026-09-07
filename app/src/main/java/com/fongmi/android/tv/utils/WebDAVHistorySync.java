package com.fongmi.android.tv.utils;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.RefreshEvent;
import com.github.catvod.utils.Logger;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * WebDAV 观看记录同步 — 负责上传、下载和智能合并历史记录。
 *
 * 从 WebDAVSyncManager 拆分而来，职责单一化。
 */
public class WebDAVHistorySync {

    private static final String HISTORY_FILE = "xmbox_history.json";

    private final WebDAVConfig config;

    public WebDAVHistorySync(WebDAVConfig config) {
        this.config = config;
    }

    /** 上传观看记录 */
    public boolean upload() {
        if (!config.isConfigured()) {
            Logger.e("WebDAV: 未配置，无法上传观看记录");
            return false;
        }

        try {
            List<History> historyList = AppDatabase.get().getHistoryDao().findAllRecent(0);
            if (historyList == null) historyList = new ArrayList<>();

            // 修复编码问题
            for (History h : historyList) {
                String fixedKey = fixHistoryKey(h.getKey());
                if (!h.getKey().equals(fixedKey)) h.setKey(fixedKey);
                String fixedName = fixEncodingIfNeeded(h.getVodName());
                if (!h.getVodName().equals(fixedName)) h.setVodName(fixedName);
            }

            String json = App.gson().toJson(historyList);
            if (TextUtils.isEmpty(json)) json = "[]";

            config.ensureDirectory();

            config.getSardine().put(config.getFileUrl(HISTORY_FILE), json.getBytes("UTF-8"));

            if (config.getSardine().exists(config.getFileUrl(HISTORY_FILE))) {
                Logger.d("WebDAV: 观看记录上传成功，共 " + historyList.size() + " 条");
                return true;
            } else {
                Logger.e("WebDAV: 上传后文件不存在，可能上传失败");
                return false;
            }
        } catch (Exception e) {
            Logger.e("WebDAV: 观看记录上传失败: " + e.getMessage());
            return false;
        }
    }

    /** 下载观看记录并与本地合并 */
    public boolean downloadAndMerge() {
        if (!config.isConfigured()) {
            Logger.e("WebDAV: 未配置，无法下载观看记录");
            return false;
        }

        try {
            String fileUrl = config.getFileUrl(HISTORY_FILE);
            if (!config.getSardine().exists(fileUrl)) {
                Logger.w("WebDAV: 观看记录文件不存在，跳过下载");
                return false;
            }

            // 下载远程数据
            String json = downloadFile(fileUrl);
            if (TextUtils.isEmpty(json)) return true;

            Type listType = new TypeToken<List<History>>(){}.getType();
            List<History> remoteList = App.gson().fromJson(json, listType);
            if (remoteList == null) return false;

            List<History> localList = AppDatabase.get().getHistoryDao().findAllRecent(0);
            Logger.d("WebDAV: 本地 " + localList.size() + " 条，远程 " + remoteList.size() + " 条");

            // 修复远程记录
            long historyTimeLimit = System.currentTimeMillis() - com.fongmi.android.tv.Constant.HISTORY_TIME;
            for (History remote : remoteList) {
                if (remote == null) continue;
                remote.setKey(fixHistoryKey(remote.getKey()));
                remote.setVodName(fixEncodingIfNeeded(remote.getVodName()));
                // 远程记录超过60天则重置时间戳，避免被过滤
                if (remote.getCreateTime() < historyTimeLimit) {
                    remote.setCreateTime(System.currentTimeMillis());
                }
            }

            // 构建本地映射
            Map<String, History> localMap = new java.util.HashMap<>();
            for (History local : localList) {
                if (local != null && local.getKey() != null) {
                    local.setKey(fixHistoryKey(local.getKey()));
                    localMap.put(local.getKey(), local);
                }
            }

            // 合并：插入新记录 + 更新较新的记录
            List<History> toInsert = new ArrayList<>();
            List<History> toUpdate = new ArrayList<>();

            for (History remote : remoteList) {
                if (remote == null || TextUtils.isEmpty(remote.getKey())) continue;
                History local = localMap.get(remote.getKey());

                if (local == null) {
                    toInsert.add(remote);
                } else if (shouldUpdate(local, remote)) {
                    toUpdate.add(remote);
                }
            }

            if (!toInsert.isEmpty()) {
                AppDatabase.get().getHistoryDao().insert(toInsert);
                Logger.d("WebDAV: 新增 " + toInsert.size() + " 条记录");
            }
            if (!toUpdate.isEmpty()) {
                AppDatabase.get().getHistoryDao().update(toUpdate);
                Logger.d("WebDAV: 更新 " + toUpdate.size() + " 条记录");
            }

            // 触发 UI 刷新
            App.post(() -> RefreshEvent.history());
            Logger.d("WebDAV: 观看记录合并完成");
            return true;
        } catch (Exception e) {
            Logger.e("WebDAV: 观看记录下载失败: " + e.getMessage());
            return false;
        }
    }

    /** 判断远程记录是否应覆盖本地记录 */
    private boolean shouldUpdate(History local, History remote) {
        long remotePos = remote.getPosition();
        long localPos = local.getPosition();
        long remoteTime = remote.getCreateTime();
        long localTime = local.getCreateTime();

        // 远程时间更新 → 覆盖
        if (remoteTime > localTime) return true;
        // 时间相近，比较播放进度
        if (Math.abs(remoteTime - localTime) <= 1000) {
            return remotePos >= 0 && remotePos > localPos;
        }
        // 本地时间更新但远程进度显著领先(>1分钟)
        return remotePos >= 0 && localPos >= 0 && remotePos > localPos + 60000;
    }

    /** 下载文件内容 */
    private String downloadFile(String fileUrl) throws Exception {
        InputStream is = config.getSardine().get(fileUrl);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        is.close();
        String json = baos.toString("UTF-8");
        baos.close();
        return json;
    }

    // ==================== 编码修复 ====================

    /** 修复 History key 中的站点名称编码 */
    private String fixHistoryKey(String key) {
        if (key == null || key.isEmpty()) return key;
        try {
            String symbol = AppDatabase.SYMBOL;
            String[] parts = key.split(java.util.regex.Pattern.quote(symbol));
            if (parts.length >= 3) {
                String fixedSiteKey = fixEncodingIfNeeded(parts[0]);
                if (!parts[0].equals(fixedSiteKey)) {
                    StringBuilder newKey = new StringBuilder(fixedSiteKey);
                    for (int i = 1; i < parts.length; i++) {
                        newKey.append(symbol).append(parts[i]);
                    }
                    return newKey.toString();
                }
            }
        } catch (Exception e) {
            Logger.e("WebDAV: 修复 History key 失败: " + e.getMessage());
        }
        return key;
    }

    /** 修复字符串编码（UTF-8 被误解析为 ISO-8859-1 的场景） */
    private String fixEncodingIfNeeded(String str) {
        if (str == null || str.isEmpty()) return str;
        try {
            boolean needsFix = false;
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                if (c == '\uFFFD' || (c >= 0x80 && c < 0xA0)) {
                    needsFix = true;
                    break;
                }
            }
            if (needsFix) {
                byte[] bytes = str.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
                return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            Logger.e("WebDAV: 编码修复失败: " + e.getMessage());
        }
        return str;
    }
}