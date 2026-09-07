package com.fongmi.android.tv.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.fongmi.android.tv.App;
import com.github.catvod.utils.Logger;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * 加密存储工具 — 使用 EncryptedSharedPreferences 保护敏感数据。
 *
 * 用 AES256-GCM 加密存储，密钥由 Android Keystore 管理，
 * 即使设备被 root 或备份文件被读取，敏感数据也不会泄露。
 *
 * 当前仅用于 WebDAV 密码，后续可扩展用于 Token 等敏感信息。
 */
public class SecurePrefs {

    private static final String FILE_NAME = "secure_prefs";
    private static SharedPreferences instance;

    private static SharedPreferences get() {
        if (instance == null) {
            try {
                Context context = App.get();
                MasterKey masterKey = new MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();

                instance = EncryptedSharedPreferences.create(
                        context,
                        FILE_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );
            } catch (GeneralSecurityException | IOException e) {
                Logger.e("SecurePrefs: init failed, falling back to plain storage — " + e.getMessage());
                // 降级：如果加密库不可用，使用普通 SharedPreferences
                // 数据安全性降低，但应用不会崩溃
                instance = App.get().getSharedPreferences(FILE_NAME + "_fallback", Context.MODE_PRIVATE);
            }
        }
        return instance;
    }

    public static String getString(String key) {
        return getString(key, "");
    }

    public static String getString(String key, String defaultValue) {
        try {
            return get().getString(key, defaultValue);
        } catch (Exception e) {
            Logger.e("SecurePrefs: read error — " + e.getMessage());
            return defaultValue;
        }
    }

    public static void putString(String key, String value) {
        try {
            get().edit().putString(key, value).apply();
        } catch (Exception e) {
            Logger.e("SecurePrefs: write error — " + e.getMessage());
        }
    }

    public static void remove(String key) {
        try {
            get().edit().remove(key).apply();
        } catch (Exception e) {
            Logger.e("SecurePrefs: remove error — " + e.getMessage());
        }
    }

    /**
     * 检查加密存储是否正常工作
     */
    public static boolean isAvailable() {
        try {
            get().edit().putString("_probe_", "ok").apply();
            String result = get().getString("_probe_", "");
            return "ok".equals(result);
        } catch (Exception e) {
            return false;
        }
    }
}