package com.fongmi.android.tv;

import android.content.Intent;
import android.provider.Settings;

import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.utils.SecurePrefs;
import com.github.catvod.utils.Prefers;

/**
 * 应用设置管理 — 集中管理所有 SharedPreferences 读写操作。
 *
 * 所有公共方法签名保持与之前完全一致，仅内部实现做了统一化重构。
 * 每个设置项由一个键常量 + 泛型 helper 方法组成，消除重复的 Prefers 调用模板代码。
 */
public class Setting {

    // ==================== 键常量 ====================

    private static final String KEY_DOH = "doh";
    private static final String KEY_PROXY = "proxy";
    private static final String KEY_KEYWORD = "keyword";
    private static final String KEY_HOT = "hot";
    private static final String KEY_UA = "ua";
    private static final String KEY_WALL = "wall";
    private static final String KEY_RESET = "reset";
    private static final String KEY_DECODE = "decode";
    private static final String KEY_PLAYER_ENGINE = "player_engine";
    private static final String KEY_RENDER = "render";
    private static final String KEY_QUALITY = "quality";
    private static final String KEY_SIZE = "size";
    private static final String KEY_VIEW_TYPE = "viewType";
    private static final String KEY_SCALE = "scale";
    private static final String KEY_SCALE_LIVE = "scale_live";
    private static final String KEY_BUFFER = "buffer";
    private static final String KEY_BACKGROUND = "background";
    private static final String KEY_SITE_MODE = "site_mode";
    private static final String KEY_SYNC_MODE = "sync_mode";
    private static final String KEY_SYNC_INTERVAL = "sync_interval";
    private static final String KEY_INCOGNITO = "incognito";
    private static final String KEY_BOOT_LIVE = "boot_live";
    private static final String KEY_INVERT = "invert";
    private static final String KEY_ACROSS = "across";
    private static final String KEY_CHANGE = "change";
    private static final String KEY_UPDATE = "update";
    private static final String KEY_AUTO_UPDATE_CHECK = "auto_update_check";
    private static final String KEY_USE_CN_MIRROR = "use_cn_mirror";
    private static final String KEY_CAPTION = "caption";
    private static final String KEY_TUNNEL = "tunnel";
    private static final String KEY_AUDIO_PREFER = "audio_prefer";
    private static final String KEY_PREFER_AAC = "prefer_aac";
    private static final String KEY_DANMAKU_LOAD = "danmaku_load";
    private static final String KEY_DANMAKU_SIZE = "danmaku_size";
    private static final String KEY_DANMAKU_SHOW = "danmaku_show";
    private static final String KEY_ZHUYIN = "zhuyin";
    private static final String KEY_SPEED = "speed";
    private static final String KEY_SUBTITLE_TEXT_SIZE = "subtitle_text_size";
    private static final String KEY_SUBTITLE_POSITION = "subtitle_position";
    private static final String KEY_PRIVACY_AGREED = "privacy_agreed_v1";
    private static final String KEY_LIVE_TAB_VISIBLE = "live_tab_visible";
    private static final String KEY_HISTORY_VISIBLE = "history_visible";
    private static final String KEY_AI_AD_BLOCK = "ai_ad_block";
    private static final String KEY_AUTO_SYNC = "auto_sync";
    private static final String KEY_SYNC_ENABLED = "sync_enabled";
    private static final String KEY_WEBDAV_URL = "webdav_url";
    private static final String KEY_WEBDAV_USERNAME = "webdav_username";
    private static final String KEY_WEBDAV_PASSWORD = "webdav_password";
    private static final String KEY_WEBDAV_SYNC_MODE = "webdav_sync_mode";
    private static final String KEY_WEBDAV_SYNC_CODE = "webdav_sync_code";
    private static final String KEY_WEBDAV_PUBLIC_URL = "webdav_public_url";
    private static final String KEY_WEBDAV_AUTO_SYNC = "webdav_auto_sync";
    private static final String KEY_WEBDAV_SYNC_INTERVAL = "webdav_sync_interval";

    // ==================== 泛型 Helper 方法 ====================

    private static String getStr(String key) { return Prefers.getString(key); }
    private static String getStr(String key, String def) { return Prefers.getString(key, def); }
    private static void putStr(String key, String val) { Prefers.put(key, val); }

    private static int getInt(String key) { return Prefers.getInt(key); }
    private static int getInt(String key, int def) { return Prefers.getInt(key, def); }
    private static void putInt(String key, int val) { Prefers.put(key, val); }

    private static boolean getBool(String key) { return Prefers.getBoolean(key); }
    private static boolean getBool(String key, boolean def) { return Prefers.getBoolean(key, def); }
    private static void putBool(String key, boolean val) { Prefers.put(key, val); }

    private static float getFlt(String key) { return Prefers.getFloat(key); }
    private static float getFlt(String key, float def) { return Prefers.getFloat(key, def); }
    private static void putFlt(String key, float val) { Prefers.put(key, val); }

    // ==================== 网络 & 代理 ====================

    public static String getDoh() { return getStr(KEY_DOH); }
    public static void putDoh(String doh) { putStr(KEY_DOH, doh); }

    public static String getProxy() { return getStr(KEY_PROXY); }
    public static void putProxy(String proxy) { putStr(KEY_PROXY, proxy); }

    public static String getKeyword() { return getStr(KEY_KEYWORD); }
    public static void putKeyword(String keyword) { putStr(KEY_KEYWORD, keyword); }

    public static String getHot() { return getStr(KEY_HOT); }
    public static void putHot(String hot) { putStr(KEY_HOT, hot); }

    public static String getUa() { return getStr(KEY_UA); }
    public static void putUa(String ua) { putStr(KEY_UA, ua); }

    public static boolean getUseCnMirror() { return getBool(KEY_USE_CN_MIRROR, false); }
    public static void putUseCnMirror(boolean useCnMirror) { putBool(KEY_USE_CN_MIRROR, useCnMirror); }

    // ==================== 显示 & 布局 ====================

    public static int getWall() { return getInt(KEY_WALL, 6); }
    public static void putWall(int wall) { putInt(KEY_WALL, wall); }

    public static int getReset() { return getInt(KEY_RESET, 0); }
    public static void putReset(int reset) { putInt(KEY_RESET, reset); }

    public static int getQuality() { return getInt(KEY_QUALITY, 2); }
    public static void putQuality(int quality) { putInt(KEY_QUALITY, quality); }

    public static int getSize() { return getInt(KEY_SIZE, 2); }
    public static void putSize(int size) { putInt(KEY_SIZE, size); }

    public static int getViewType(int viewType) { return getInt(KEY_VIEW_TYPE, viewType); }
    public static void putViewType(int viewType) { putInt(KEY_VIEW_TYPE, viewType); }

    public static int getScale() { return getInt(KEY_SCALE); }
    public static void putScale(int scale) { putInt(KEY_SCALE, scale); }

    public static int getLiveScale() { return getInt(KEY_SCALE_LIVE, getScale()); }
    public static void putLiveScale(int scale) { putInt(KEY_SCALE_LIVE, scale); }

    public static int getBackground() { return getInt(KEY_BACKGROUND, 0); }
    public static void putBackground(int background) { putInt(KEY_BACKGROUND, background); }

    public static boolean isBackgroundOff() { return getBackground() == 0; }
    public static boolean isBackgroundOn() { return getBackground() == 1; }

    // ==================== 播放器 ====================

    public static int getDecode() { return getInt(KEY_DECODE, Players.AUTO); }
    public static void putDecode(int decode) { putInt(KEY_DECODE, decode); }

    public static int getPlayerEngine() { return getInt(KEY_PLAYER_ENGINE, Players.ENGINE_EXO); }
    public static void putPlayerEngine(int engine) { putInt(KEY_PLAYER_ENGINE, engine); }

    public static int getRender() { return getInt(KEY_RENDER, 0); }
    public static void putRender(int render) { putInt(KEY_RENDER, render); }

    public static int getBuffer() { return Math.min(Math.max(getInt(KEY_BUFFER), 1), 10); }
    public static void putBuffer(int buffer) { putInt(KEY_BUFFER, buffer); }

    public static int getSiteMode() { return getInt(KEY_SITE_MODE); }
    public static void putSiteMode(int mode) { putInt(KEY_SITE_MODE, mode); }

    public static int getSyncMode() { return getInt(KEY_SYNC_MODE); }
    public static void putSyncMode(int mode) { putInt(KEY_SYNC_MODE, mode); }

    public static int getSyncInterval() { return getInt(KEY_SYNC_INTERVAL, 30); }
    public static void putSyncInterval(int minutes) { putInt(KEY_SYNC_INTERVAL, minutes); }

    // ==================== 播放器开关 ====================

    public static boolean isCaption() { return getBool(KEY_CAPTION); }
    public static void putCaption(boolean caption) { putBool(KEY_CAPTION, caption); }

    public static boolean isTunnel() { return getBool(KEY_TUNNEL); }
    public static void putTunnel(boolean tunnel) { putBool(KEY_TUNNEL, tunnel); }

    public static boolean isAudioPrefer() { return getBool(KEY_AUDIO_PREFER); }
    public static void putAudioPrefer(boolean audioPrefer) { putBool(KEY_AUDIO_PREFER, audioPrefer); }

    public static boolean isPreferAAC() { return getBool(KEY_PREFER_AAC); }
    public static void putPreferAAC(boolean preferAAC) { putBool(KEY_PREFER_AAC, preferAAC); }

    public static boolean isDanmakuLoad() { return getBool(KEY_DANMAKU_LOAD, true); }
    public static void putDanmakuLoad(boolean danmakuLoad) { putBool(KEY_DANMAKU_LOAD, danmakuLoad); }

    public static boolean isDanmakuShow() { return getBool(KEY_DANMAKU_SHOW); }
    public static void putDanmakuShow(boolean danmakuShow) { putBool(KEY_DANMAKU_SHOW, danmakuShow); }

    // ==================== 播放速度 & 字幕 ====================

    public static float getSpeed() { return Math.min(Math.max(getFlt(KEY_SPEED, 3), 2), 5); }
    public static void putSpeed(float speed) { putFlt(KEY_SPEED, speed); }

    public static float getDanmakuSize() { return getFlt(KEY_DANMAKU_SIZE, 1.0f); }
    public static void putDanmakuSize(float size) { putFlt(KEY_DANMAKU_SIZE, size); }

    public static float getSubtitleTextSize() { return getFlt(KEY_SUBTITLE_TEXT_SIZE); }
    public static void putSubtitleTextSize(float value) { putFlt(KEY_SUBTITLE_TEXT_SIZE, value); }

    public static float getSubtitlePosition() { return getFlt(KEY_SUBTITLE_POSITION); }
    public static void putSubtitlePosition(float value) { putFlt(KEY_SUBTITLE_POSITION, value); }

    // ==================== 衍生值 ====================

    public static float getThumbnail() { return 0.3f * getQuality() + 0.4f; }

    public static boolean hasCaption() {
        return new Intent(Settings.ACTION_CAPTIONING_SETTINGS).resolveActivity(App.get().getPackageManager()) != null;
    }

    // ==================== 行为开关 ====================

    public static boolean isIncognito() { return getBool(KEY_INCOGNITO); }
    public static void putIncognito(boolean incognito) { putBool(KEY_INCOGNITO, incognito); }

    public static boolean isBootLive() { return getBool(KEY_BOOT_LIVE); }
    public static void putBootLive(boolean boot) { putBool(KEY_BOOT_LIVE, boot); }

    public static boolean isInvert() { return getBool(KEY_INVERT); }
    public static void putInvert(boolean invert) { putBool(KEY_INVERT, invert); }

    public static boolean isAcross() { return getBool(KEY_ACROSS, true); }
    public static void putAcross(boolean across) { putBool(KEY_ACROSS, across); }

    public static boolean isChange() { return getBool(KEY_CHANGE, true); }
    public static void putChange(boolean change) { putBool(KEY_CHANGE, change); }

    public static boolean getUpdate() { return getBool(KEY_UPDATE, true); }
    public static void putUpdate(boolean update) { putBool(KEY_UPDATE, update); }

    public static boolean getAutoUpdateCheck() { return getBool(KEY_AUTO_UPDATE_CHECK, false); }
    public static void putAutoUpdateCheck(boolean autoUpdateCheck) { putBool(KEY_AUTO_UPDATE_CHECK, autoUpdateCheck); }

    public static boolean isZhuyin() { return getBool(KEY_ZHUYIN); }
    public static void putZhuyin(boolean zhuyin) { putBool(KEY_ZHUYIN, zhuyin); }

    public static boolean isPrivacyAgreed() { return getBool(KEY_PRIVACY_AGREED, false); }
    public static void setPrivacyAgreed(boolean agreed) { putBool(KEY_PRIVACY_AGREED, agreed); }

    public static boolean isLiveTabVisible() { return getBool(KEY_LIVE_TAB_VISIBLE, true); }
    public static void putLiveTabVisible(boolean visible) { putBool(KEY_LIVE_TAB_VISIBLE, visible); }

    public static boolean isHistoryVisible() { return getBool(KEY_HISTORY_VISIBLE, true); }
    public static void putHistoryVisible(boolean visible) { putBool(KEY_HISTORY_VISIBLE, visible); }

    public static boolean isAIAdBlockEnabled() { return getBool(KEY_AI_AD_BLOCK, true); }
    public static void putAIAdBlockEnabled(boolean enabled) { putBool(KEY_AI_AD_BLOCK, enabled); }

    // ==================== 局域网同步 ====================

    public static boolean isAutoSync() { return getBool(KEY_AUTO_SYNC, false); }
    public static void putAutoSync(boolean autoSync) { putBool(KEY_AUTO_SYNC, autoSync); }

    public static boolean isSyncEnabled() { return getBool(KEY_SYNC_ENABLED, false); }
    public static void putSyncEnabled(boolean enabled) { putBool(KEY_SYNC_ENABLED, enabled); }

    // ==================== WebDAV 同步 ====================

    public static String getWebDAVUrl() { return getStr(KEY_WEBDAV_URL, ""); }
    public static void putWebDAVUrl(String url) { putStr(KEY_WEBDAV_URL, url); }

    public static String getWebDAVUsername() { return getStr(KEY_WEBDAV_USERNAME, ""); }
    public static void putWebDAVUsername(String username) { putStr(KEY_WEBDAV_USERNAME, username); }

    public static String getWebDAVPassword() { return SecurePrefs.getString(KEY_WEBDAV_PASSWORD, ""); }
    public static void putWebDAVPassword(String password) { SecurePrefs.putString(KEY_WEBDAV_PASSWORD, password); }

    public static String getWebDAVSyncMode() { return getStr(KEY_WEBDAV_SYNC_MODE, "ACCOUNT"); }
    public static void putWebDAVSyncMode(String mode) { putStr(KEY_WEBDAV_SYNC_MODE, mode); }

    public static String getWebDAVSyncCode() { return getStr(KEY_WEBDAV_SYNC_CODE, ""); }
    public static void putWebDAVSyncCode(String code) { putStr(KEY_WEBDAV_SYNC_CODE, code); }

    public static String getWebDAVPublicUrl() { return getStr(KEY_WEBDAV_PUBLIC_URL, ""); }
    public static void putWebDAVPublicUrl(String url) { putStr(KEY_WEBDAV_PUBLIC_URL, url); }

    public static boolean isWebDAVAutoSync() { return getBool(KEY_WEBDAV_AUTO_SYNC, false); }
    public static void putWebDAVAutoSync(boolean autoSync) { putBool(KEY_WEBDAV_AUTO_SYNC, autoSync); }

    public static int getWebDAVSyncInterval() { return getInt(KEY_WEBDAV_SYNC_INTERVAL, 60); }
    public static void putWebDAVSyncInterval(int minutes) { putInt(KEY_WEBDAV_SYNC_INTERVAL, minutes); }
}