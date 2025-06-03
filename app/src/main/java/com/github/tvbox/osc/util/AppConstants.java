package com.github.tvbox.osc.util;

/**
 * 应用常量类
 * 用于存储应用中使用的各种常量，避免硬编码
 */
public class AppConstants {
    
    /**
     * 外部应用包名
     */
    public static class ExternalApps {
        // 1DM下载管理器
        public static final String PKG_1DM = "idm.internet.download.manager.plus";
        public static final String CLS_1DM_MAIN = "idm.internet.download.manager.MainActivity";
        public static final String CLS_1DM_DOWNLOADER = "idm.internet.download.manager.Downloader";
        
        // 阿狸云盘
        public static final String PKG_ALI_CLOUD = "com.alicloud.databox";
        public static final String CLS_ALI_CLOUD_SPLASH = "com.alicloud.databox.launcher.splash.SplashActivity";
        
        // UC浏览器
        public static final String PKG_UC_BROWSER = "com.UCMobile";
        public static final String CLS_UC_BROWSER_MAIN = "com.uc.browser.InnerUCMobile";
        
        // 夸父浏览器
        public static final String PKG_QUARK_BROWSER = "com.quark.browser";
        public static final String CLS_QUARK_BROWSER_MAIN = "com.ucpro.MainActivity";
    }
    
    /**
     * 网络相关常量
     */
    public static class Network {
        // 超时时间（毫秒）
        public static final int DEFAULT_TIMEOUT_MS = 10000;
        
        // 线程池大小
        public static final int SEARCH_THREAD_POOL_SIZE = 5;
    }
    
    /**
     * 播放器相关常量
     */
    public static class Player {
        // 字幕缩放比例（非全屏模式）
        public static final double SUBTITLE_SCALE_NON_FULLSCREEN = 0.6;
    }
}
