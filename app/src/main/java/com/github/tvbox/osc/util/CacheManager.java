package com.github.tvbox.osc.util;

import android.text.TextUtils;

import com.github.tvbox.osc.base.App;
import com.orhanobut.hawk.Hawk;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 缓存管理类，用于管理配置和JAR文件的缓存
 */
public class CacheManager {
    private static final String CACHE_VALIDITY_PREFIX = "cache_validity_";
    private static final String JAR_CACHE_VALIDITY_PREFIX = "jar_cache_validity_";

    // 默认缓存有效期（小时）
    private static final long DEFAULT_CACHE_VALIDITY_HOURS = 24;
    // 特定URL的缓存有效期（小时）
    private static final Map<String, Long> SPECIAL_URL_CACHE_HOURS = new HashMap<>();

    static {
        // 为特定URL设置更长的缓存有效期
        SPECIAL_URL_CACHE_HOURS.put("http://ok321.top/tv", 72L);
        SPECIAL_URL_CACHE_HOURS.put("https://7213.kstore.vip/吃猫的鱼", 72L);
        SPECIAL_URL_CACHE_HOURS.put("http://www.饭太硬.com/tv", 72L);
    }

    /**
     * 检查配置缓存是否有效
     * @param apiUrl API URL
     * @param cacheFile 缓存文件
     * @return 缓存是否有效
     */
    public static boolean isConfigCacheValid(String apiUrl, File cacheFile) {
        if (TextUtils.isEmpty(apiUrl) || !cacheFile.exists()) {
            return false;
        }

        // 获取缓存时间戳
        long cacheTimestamp = Hawk.get(CACHE_VALIDITY_PREFIX + MD5.encode(apiUrl), 0L);
        if (cacheTimestamp == 0L) {
            return false;
        }

        // 获取当前时间
        long currentTime = System.currentTimeMillis();

        // 获取缓存有效期（小时）
        long validityHours = getConfigCacheValidityHours(apiUrl);

        // 检查缓存是否过期
        return currentTime - cacheTimestamp < TimeUnit.HOURS.toMillis(validityHours);
    }

    /**
     * 更新配置缓存时间戳
     * @param apiUrl API URL
     */
    public static void updateConfigCacheValidity(String apiUrl) {
        if (!TextUtils.isEmpty(apiUrl)) {
            Hawk.put(CACHE_VALIDITY_PREFIX + MD5.encode(apiUrl), System.currentTimeMillis());
        }
    }

    /**
     * 检查JAR缓存是否有效
     * @param jarUrl JAR URL
     * @param md5 JAR文件的MD5值
     * @param cacheFile 缓存文件
     * @return 缓存是否有效
     */
    public static boolean isJarCacheValid(String jarUrl, String md5, File cacheFile) {
        // 如果有MD5值，优先使用MD5验证
        if (!TextUtils.isEmpty(md5) && cacheFile.exists()) {
            String fileMd5 = MD5.getFileMd5(cacheFile);
            return md5.equalsIgnoreCase(fileMd5);
        }

        // 否则使用时间戳验证
        if (TextUtils.isEmpty(jarUrl) || !cacheFile.exists()) {
            return false;
        }

        // 获取缓存时间戳
        long cacheTimestamp = Hawk.get(JAR_CACHE_VALIDITY_PREFIX + MD5.encode(jarUrl), 0L);
        if (cacheTimestamp == 0L) {
            return false;
        }

        // 获取当前时间
        long currentTime = System.currentTimeMillis();

        // JAR文件缓存有效期固定为48小时
        long validityHours = 48;

        // 检查缓存是否过期
        return currentTime - cacheTimestamp < TimeUnit.HOURS.toMillis(validityHours);
    }

    /**
     * 更新JAR缓存时间戳
     * @param jarUrl JAR URL
     */
    public static void updateJarCacheValidity(String jarUrl) {
        if (!TextUtils.isEmpty(jarUrl)) {
            Hawk.put(JAR_CACHE_VALIDITY_PREFIX + MD5.encode(jarUrl), System.currentTimeMillis());
        }
    }

    /**
     * 获取配置缓存有效期（小时）
     * @param apiUrl API URL
     * @return 缓存有效期（小时）
     */
    private static long getConfigCacheValidityHours(String apiUrl) {
        // 检查是否是特定URL
        for (Map.Entry<String, Long> entry : SPECIAL_URL_CACHE_HOURS.entrySet()) {
            if (apiUrl.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }

        // 返回默认缓存有效期
        return DEFAULT_CACHE_VALIDITY_HOURS;
    }

    /**
     * 清除所有缓存
     */
    public static void clearAllCache() {
        // 清除缓存文件夹
        File cacheDir = new File(App.getInstance().getFilesDir().getAbsolutePath());
        if (cacheDir.exists() && cacheDir.isDirectory()) {
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && !file.getName().equals("csp.jar")) {
                        file.delete();
                    }
                }
            }
        }

        // 清除Hawk中的缓存有效期记录
        // 由于无法获取所有键，我们只能删除特定的缓存键
        // 这里我们可以在实际使用时添加需要清除的特定键
    }
}
