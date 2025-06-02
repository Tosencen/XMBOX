package com.github.tvbox.osc.util;

import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import com.github.tvbox.osc.base.App;
import com.orhanobut.hawk.Hawk;

/**
 * 主题管理器
 * 统一管理应用主题，解决跟随系统主题时的一致性问题
 */
public class ThemeManager {
    private static final String TAG = "ThemeManager";
    
    private static volatile ThemeManager instance;
    private int lastSystemNightMode = -1;
    private int lastThemeTag = -1;
    
    private ThemeManager() {
        // 初始化时记录当前状态
        updateLastStates();
    }
    
    public static ThemeManager getInstance() {
        if (instance == null) {
            synchronized (ThemeManager.class) {
                if (instance == null) {
                    instance = new ThemeManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 初始化主题
     * 在Application启动时调用
     */
    public void initTheme() {
        applyTheme();
        updateLastStates();
        Log.i(TAG, "主题初始化完成");
    }
    
    /**
     * 检查主题是否需要更新
     * 在Activity创建时调用
     */
    public boolean checkThemeUpdate() {
        int currentThemeTag = Hawk.get(HawkConfig.THEME_TAG, 0);
        int currentSystemNightMode = getCurrentSystemNightMode();
        
        boolean themeChanged = false;
        
        // 检查用户设置是否变化
        if (lastThemeTag != currentThemeTag) {
            Log.i(TAG, "用户主题设置变化: " + lastThemeTag + " -> " + currentThemeTag);
            themeChanged = true;
        }
        
        // 检查系统主题是否变化（仅在跟随系统时）
        if (currentThemeTag == 0 && lastSystemNightMode != currentSystemNightMode) {
            Log.i(TAG, "系统主题变化: " + lastSystemNightMode + " -> " + currentSystemNightMode);
            themeChanged = true;
        }
        
        if (themeChanged) {
            applyTheme();
            updateLastStates();
        }
        
        return themeChanged;
    }
    
    /**
     * 应用主题设置
     */
    private void applyTheme() {
        int themeTag = Hawk.get(HawkConfig.THEME_TAG, 0);
        
        switch (themeTag) {
            case 0: // 跟随系统
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                Log.i(TAG, "应用主题: 跟随系统");
                break;
            case 1: // 浅色模式
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                Log.i(TAG, "应用主题: 浅色模式");
                break;
            case 2: // 深色模式
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                Log.i(TAG, "应用主题: 深色模式");
                break;
            default:
                Log.w(TAG, "未知主题设置: " + themeTag + "，使用跟随系统");
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
    
    /**
     * 获取当前系统夜间模式
     */
    private int getCurrentSystemNightMode() {
        try {
            Context context = App.getInstance();
            if (context != null) {
                return context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            }
        } catch (Exception e) {
            Log.e(TAG, "获取系统夜间模式失败: " + e.getMessage());
        }
        return Configuration.UI_MODE_NIGHT_UNDEFINED;
    }
    
    /**
     * 更新记录的状态
     */
    private void updateLastStates() {
        lastThemeTag = Hawk.get(HawkConfig.THEME_TAG, 0);
        lastSystemNightMode = getCurrentSystemNightMode();
    }
    
    /**
     * 判断当前是否为深色主题
     */
    public boolean isDarkTheme() {
        int themeTag = Hawk.get(HawkConfig.THEME_TAG, 0);
        
        switch (themeTag) {
            case 1: // 强制浅色模式
                return false;
            case 2: // 强制深色模式
                return true;
            case 0: // 跟随系统
            default:
                // 获取当前系统主题
                int currentNightMode = getCurrentSystemNightMode();
                return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
        }
    }
    
    /**
     * 强制刷新主题
     * 在设置页面修改主题后调用
     */
    public void forceRefreshTheme() {
        Log.i(TAG, "强制刷新主题");
        applyTheme();
        updateLastStates();
    }
    
    /**
     * 处理系统配置变化
     * 在Activity的onConfigurationChanged中调用
     */
    public void onConfigurationChanged(Configuration newConfig) {
        int themeTag = Hawk.get(HawkConfig.THEME_TAG, 0);
        
        // 只有在跟随系统主题时才处理系统主题变化
        if (themeTag == 0) {
            int newNightMode = newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (lastSystemNightMode != newNightMode) {
                Log.i(TAG, "系统配置变化，夜间模式: " + lastSystemNightMode + " -> " + newNightMode);
                lastSystemNightMode = newNightMode;
                // 不需要重新应用主题，因为跟随系统模式会自动处理
            }
        }
    }
    
    /**
     * 获取主题描述（用于调试）
     */
    public String getThemeDescription() {
        int themeTag = Hawk.get(HawkConfig.THEME_TAG, 0);
        String[] themes = {"跟随系统", "浅色", "深色"};
        String themeDesc = themeTag < themes.length ? themes[themeTag] : "未知";
        
        if (themeTag == 0) {
            int systemMode = getCurrentSystemNightMode();
            String systemDesc = systemMode == Configuration.UI_MODE_NIGHT_YES ? "深色" : "浅色";
            return themeDesc + "(" + systemDesc + ")";
        } else {
            return themeDesc;
        }
    }
}
