package com.github.tvbox.osc.base;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.multidex.MultiDexApplication;

import com.github.catvod.crawler.JsLoader;
import com.xmbox.app.R;
import com.github.tvbox.osc.bean.Subscription;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.callback.EmptyCallback;
import com.github.tvbox.osc.callback.EmptyCollectCallback;
import com.github.tvbox.osc.callback.EmptyHistoryCallback;
import com.github.tvbox.osc.callback.EmptySubscriptionCallback;
import com.github.tvbox.osc.callback.LoadingCallback;
import com.github.tvbox.osc.data.AppDataManager;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.ui.activity.MainActivity;
import com.github.tvbox.osc.util.EpgUtil;
import com.github.tvbox.osc.util.FileUtils;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.LOG;

import com.github.tvbox.osc.util.MaterialSymbolsLoader;
import com.github.tvbox.osc.util.OkGoHelper;
import com.github.tvbox.osc.util.PlayerHelper;
import com.github.tvbox.osc.util.ThreadPoolManager;
import com.github.tvbox.osc.util.Utils;
import com.kingja.loadsir.core.LoadSir;
import com.orhanobut.hawk.Hawk;
import com.p2p.P2PClass;
import com.whl.quickjs.android.QuickJSLoader;

import java.util.ArrayList;
import java.util.List;

import static android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE;
import static android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE;

import cat.ereza.customactivityoncrash.config.CaocConfig;
import me.jessyan.autosize.AutoSizeConfig;
import me.jessyan.autosize.unit.Subunits;

/**
 * @author pj567
 * @date :2020/12/17
 * @description:
 */
public class App extends MultiDexApplication {
    private static App instance;

    private static P2PClass p;
    public static String burl;

    public boolean isNormalStart;

    /**
     * 检查设备架构是否支持
     * @return 是否支持当前架构
     */
    private boolean isArchitectureSupported() {
        String arch = System.getProperty("os.arch");
        LOG.i("App", "设备架构: " + arch);
        // 扩大支持架构范围，支持ARM和x86架构
        return arch != null && (arch.contains("arm") || arch.contains("ARM") ||
                               arch.contains("x86") || arch.contains("X86"));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            LOG.i("App", "应用初始化开始");
            instance = this;

            // 检查设备架构
            boolean archSupported = isArchitectureSupported();
            LOG.i("App", "设备架构支持状态: " + (archSupported ? "支持" : "不支持"));
            if (!archSupported) {
                LOG.w("App", "当前设备架构可能不受支持，某些功能可能无法正常使用");
            }

            // 初始化关键组件（主线程）
            initCriticalComponents();

            // 异步初始化非关键组件
            ThreadPoolManager.executeIO(new Runnable() {
                @Override
                public void run() {
                    initNonCriticalComponents();
                }
            });

            LOG.i("App", "应用关键组件初始化完成");
        } catch (Throwable e) {
            LOG.e("App", "应用初始化过程中发生严重错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 初始化关键组件（在主线程执行）
     * 这些组件必须在应用启动时立即初始化
     */
    private void initCriticalComponents() {
        try {
            // 初始化参数（必须最先初始化）
            initParams();
            LOG.i("App", "参数初始化成功");
        } catch (Exception e) {
            LOG.e("App", "参数初始化失败: " + e.getMessage());
        }

        try {
            // 初始化崩溃处理（必须尽早初始化）
            initCrashConfig();
            // 初始化全局异常处理
            // GlobalExceptionHandler.getInstance().init(this); // 暂时注释掉，因为类不存在
            LOG.i("App", "崩溃配置初始化成功");
        } catch (Exception e) {
            LOG.e("App", "崩溃配置初始化失败: " + e.getMessage());
        }

        try {
            // 初始化UI相关配置
            AutoSizeConfig.getInstance()
                    .setExcludeFontScale(true)
                    .setCustomFragment(true)
                    .getUnitsManager()
                    .setSupportDP(false)
                    .setSupportSP(false)
                    .setSupportSubunits(Subunits.MM);
            LOG.i("App", "AutoSize初始化成功");
        } catch (Exception e) {
            LOG.e("App", "AutoSize初始化失败: " + e.getMessage());
        }

        try {
            // 初始化LoadSir（UI相关，需要在主线程初始化）
            LoadSir.beginBuilder()
                    .addCallback(new EmptyCallback())
                    .addCallback(new EmptyCollectCallback())
                    .addCallback(new EmptyHistoryCallback())
                    .addCallback(new EmptySubscriptionCallback())
                    .addCallback(new LoadingCallback())
                    .commit();
            LOG.i("App", "LoadSir初始化成功");
        } catch (Exception e) {
            LOG.e("App", "LoadSir初始化失败: " + e.getMessage());
        }

        try {
            // 初始化主题（UI相关）
            Utils.initTheme();
            LOG.i("App", "主题初始化成功");
        } catch (Exception e) {
            LOG.e("App", "主题初始化失败: " + e.getMessage());
        }
    }

    /**
     * 初始化非关键组件（在后台线程执行）
     * 这些组件可以在应用启动后异步初始化
     */
    private void initNonCriticalComponents() {
        try {
            // 初始化网络相关
            OkGoHelper.init();
            LOG.i("App", "OkGo初始化成功");
        } catch (Exception e) {
            LOG.e("App", "OkGo初始化失败: " + e.getMessage());
        }

        try {
            // 初始化EPG
            EpgUtil.init();
            LOG.i("App", "EPG工具初始化成功");
        } catch (Exception e) {
            LOG.e("App", "EPG工具初始化失败: " + e.getMessage());
        }

        try {
            // 初始化Web服务器
            ControlManager.init(this);
            LOG.i("App", "Web服务器初始化成功");
        } catch (Exception e) {
            LOG.e("App", "Web服务器初始化失败: " + e.getMessage());
        }

        try {
            // 初始化数据库
            AppDataManager.init();
            LOG.i("App", "数据库初始化成功");
        } catch (Exception e) {
            LOG.e("App", "数据库初始化失败: " + e.getMessage());
        }

        try {
            // 初始化播放器
            PlayerHelper.init();
            LOG.i("App", "播放器初始化成功");
        } catch (Exception e) {
            LOG.e("App", "播放器初始化失败: " + e.getMessage());
        }

        // 尝试加载QuickJS库
        if (isArchitectureSupported()) {
            boolean quickJSLoaded = com.whl.quickjs.android.QuickJSLoader.init();
            if (quickJSLoaded) {
                LOG.i("App", "QuickJS库加载成功");
            } else {
                LOG.e("App", "QuickJS库加载失败，相关功能将被禁用");
            }
        } else {
            LOG.e("App", "QuickJS已禁用，因为当前设备架构不受支持");
        }

        try {
            // 清理播放器缓存
            FileUtils.cleanPlayerCache();
            LOG.i("App", "清理播放器缓存成功");
        } catch (Exception e) {
            LOG.e("App", "清理播放器缓存失败: " + e.getMessage());
        }

        try {
            // 初始Material Symbols字体
            ThreadPoolManager.executeMain(new Runnable() {
                @Override
                public void run() {
                    try {
                        MaterialSymbolsLoader.init(App.this);
                        LOG.i("App", "Material Symbols字体初始化成功");
                    } catch (Exception e) {
                        LOG.e("App", "Material Symbols字体初始化失败: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            LOG.e("App", "Material Symbols字体初始化调度失败: " + e.getMessage());
        }

        try {
            // 初始化内存泄漏修复器
            com.github.tvbox.osc.util.MemoryLeakFixer.init(App.this);
            LOG.i("App", "内存泄漏修复器初始化成功");
        } catch (Exception e) {
            LOG.e("App", "内存泄漏修复器初始化失败: " + e.getMessage());
        }

        try {
            // 初始化新的内存泄漏修复管理器
            com.github.tvbox.osc.util.MemoryLeakFixManager.initialize(App.this);
            LOG.i("App", "内存泄漏修复管理器初始化成功");
        } catch (Exception e) {
            LOG.e("App", "内存泄漏修复管理器初始化失败: " + e.getMessage());
        }

        try {
            // 初始化应用生命周期管理器
            com.github.tvbox.osc.util.AppLifecycleManager.getInstance().init(this);
            LOG.i("App", "应用生命周期管理器初始化成功");
        } catch (Exception e) {
            LOG.e("App", "应用生命周期管理器初始化失败: " + e.getMessage());
        }

        try {
            // 初始化简化内存管理器（借鉴TVBoxOS-Mobile策略）
            com.github.tvbox.osc.util.SimpleMemoryManager.getInstance().init(this);
            LOG.i("App", "简化内存管理器初始化成功");
        } catch (Exception e) {
            LOG.e("App", "简化内存管理器初始化失败: " + e.getMessage());
        }

        try {
            // 初始化内存泄漏启动器（统一管理所有内存泄漏修复组件）
            com.github.tvbox.osc.util.MemoryLeakBootstrap.initialize(this);
            LOG.i("App", "内存泄漏启动器初始化成功");
        } catch (Exception e) {
            LOG.e("App", "内存泄漏启动器初始化失败: " + e.getMessage());
        }

        LOG.i("App", "应用非关键组件初始化完成");
    }

    private void initParams() {
        // Hawk
        Hawk.init(this).build();
        Hawk.put(HawkConfig.DEBUG_OPEN, false);

        putDefault(HawkConfig.HOME_REC, 2);                  //推荐: 0=豆瓣热播, 1=站点推荐, 2=关闭
        putDefault(HawkConfig.PLAY_TYPE, 2);                 //播放器: 0=系统, 1=IJK, 2=Exo
        putDefault(HawkConfig.IJK_CODEC, "硬解码");           //IJK解码: 软解码, 硬解码
        putDefault(HawkConfig.BACKGROUND_PLAY_TYPE,2);           //后台播放: 0 关闭,1 开启,2 画中画
        putDefault(HawkConfig.DOH_URL, 1);                   //安全DNS: 0=关闭, 1=阿里DNS, 2=腾讯DNS, 3=360DNS
        putDefault(HawkConfig.PLAY_SCALE, 0);                //画面缩放: 0=默认, 1=16:9, 2=4:3, 3=填充, 4=原始, 5=裁剪
        putDefault(HawkConfig.HISTORY_NUM, 2);                //历史记录数量: 0=30, 1=50, 2=70

        // 应用重启相关配置
        putDefault(HawkConfig.AUTO_RESTART_ENABLED, true);    //自动重启: true=启用, false=禁用
        putDefault(HawkConfig.RESTART_THRESHOLD_HOURS, 3);    //重启阈值: 3小时
        putDefault(HawkConfig.SHOW_RESTART_TOAST, true);      //重启提示: true=显示, false=不显示
        putDefault(HawkConfig.CLEANUP_ON_RESTART, true);      //重启清理: true=清理, false=不清理

        putDefaultApi();
    }

    private void putDefaultApi() {
        String[] apis = getResources().getStringArray(R.array.api);
        if(!Hawk.contains(HawkConfig.API_URL) && !Hawk.contains(HawkConfig.SUBSCRIPTIONS) && !TextUtils.isEmpty(apis[0])){
            List<Subscription> subscriptions = new ArrayList<>();
            for (int i = 0; i < apis.length; i++) {
                if (i==0){
                    subscriptions.add(new Subscription("订阅: 1", apis[0]).setChecked(true));
                    Hawk.put(HawkConfig.API_URL,apis[0]);
                }else {
                    subscriptions.add(new Subscription("订阅: "+(i+1), apis[i]));
                }
            }
            Hawk.put(HawkConfig.SUBSCRIPTIONS,subscriptions);
        }
    }

    public static App getInstance() {
        return instance;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        // 清理 Spider 相关资源
        try {
            JsLoader.load(); // 清理 JS Spider
            com.github.tvbox.osc.api.ApiConfig.get().cleanup(); // 清理 JAR Spider
            LOG.i("App", "Spider资源清理完成");
        } catch (Exception e) {
            LOG.e("App", "Spider资源清理失败: " + e.getMessage());
        }

        // 清理内存泄漏相关的静态引用
        try {
            // 清理Toast引用
            com.github.tvbox.osc.util.MD3ToastUtils.cleanup();
            LOG.i("App", "Toast清理完成");
        } catch (Exception e) {
            LOG.e("App", "Toast清理失败: " + e.getMessage());
        }

        try {
            // 彻底清理OkGo相关引用（仅在应用退出时）
            OkGoHelper.completeCleanup();
            LOG.i("App", "OkGo彻底清理完成");
        } catch (Exception e) {
            LOG.e("App", "OkGo彻底清理失败: " + e.getMessage());
        }

        try {
            // 清理Glide相关引用
            com.github.tvbox.osc.util.GlideHelper.cleanup();
            LOG.i("App", "Glide清理完成");
        } catch (Exception e) {
            LOG.e("App", "Glide清理失败: " + e.getMessage());
        }

        // 清理资源
        vodInfo = null;
        // 清理VideoView引用
        try {
            xyz.doikki.videoplayer.player.VideoViewManager.instance().removeAll();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 投屏功能已完全移除
        // 清理线程池资源
        try {
            com.github.tvbox.osc.util.ThreadPoolManager.cleanup();
            LOG.i("App", "ThreadPoolManager清理完成");
        } catch (Exception e) {
            LOG.e("App", "ThreadPoolManager清理失败: " + e.getMessage());
        }

        // 清理内存泄漏检查器
        try {
            com.github.tvbox.osc.util.MemoryLeakChecker.cleanup();
            LOG.i("App", "内存泄漏检查器清理完成");
        } catch (Exception e) {
            LOG.e("App", "内存泄漏检查器清理失败: " + e.getMessage());
        }

        // 清理Activity泄漏检测器
        try {
            com.github.tvbox.osc.util.ActivityLeakDetector.cleanup();
            LOG.i("App", "Activity泄漏检测器清理完成");
        } catch (Exception e) {
            LOG.e("App", "Activity泄漏检测器清理失败: " + e.getMessage());
        }

        // 立即修复所有内存泄漏（包括EventBus和OkGoHelper）
        try {
            com.github.tvbox.osc.util.MemoryLeakFixer.fixAllLeaksImmediate();
            LOG.i("App", "立即内存泄漏修复完成");
        } catch (Exception e) {
            LOG.e("App", "立即内存泄漏修复失败: " + e.getMessage());
        }

        // 清理内存泄漏修复器
        try {
            com.github.tvbox.osc.util.MemoryLeakFixer.cleanup();
            LOG.i("App", "内存泄漏修复器清理完成");
        } catch (Exception e) {
            LOG.e("App", "内存泄漏修复器清理失败: " + e.getMessage());
        }

        // 清理LoadSir全局引用
        try {
            com.github.tvbox.osc.util.LoadSirLeakFixer.clearGlobalLoadSirReferences();
            LOG.i("App", "LoadSir全局引用清理完成");
        } catch (Exception e) {
            LOG.e("App", "LoadSir全局引用清理失败: " + e.getMessage());
        }

        // 清理新的内存泄漏修复管理器
        try {
            com.github.tvbox.osc.util.MemoryLeakFixManager.performComprehensiveCleanup();
            LOG.i("App", "内存泄漏修复管理器清理完成");
        } catch (Exception e) {
            LOG.e("App", "内存泄漏修复管理器清理失败: " + e.getMessage());
        }

        // 清理应用生命周期管理器
        try {
            com.github.tvbox.osc.util.AppLifecycleManager.getInstance().cleanup();
            LOG.i("App", "应用生命周期管理器清理完成");
        } catch (Exception e) {
            LOG.e("App", "应用生命周期管理器清理失败: " + e.getMessage());
        }

        // 清理简化内存管理器
        try {
            com.github.tvbox.osc.util.SimpleMemoryManager.getInstance().cleanup();
            LOG.i("App", "简化内存管理器清理完成");
        } catch (Exception e) {
            LOG.e("App", "简化内存管理器清理失败: " + e.getMessage());
        }

        // 清理内存泄漏启动器
        try {
            com.github.tvbox.osc.util.MemoryLeakBootstrap.cleanup();
            LOG.i("App", "内存泄漏启动器清理完成");
        } catch (Exception e) {
            LOG.e("App", "内存泄漏启动器清理失败: " + e.getMessage());
        }

        // 强制垃圾回收
        try {
            System.gc();
            System.runFinalization();
            System.gc();
            LOG.i("App", "强制垃圾回收完成");
        } catch (Exception e) {
            LOG.e("App", "强制垃圾回收失败: " + e.getMessage());
        }

        LOG.i("App", "应用程序终止清理完成");
    }

    /**
     * 当系统内存不足时调用
     * 释放非必要资源
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        LOG.w("App", "系统内存不足，开始释放资源");
        // 清理内存
        com.github.tvbox.osc.util.MemoryOptimizer.cleanMemory();
        // 立即修复内存泄漏
        com.github.tvbox.osc.util.MemoryLeakFixer.fixAllLeaksImmediate();
    }

    /**
     * 当系统内存接近不足时调用
     * 释放部分非必要资源
     */
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_MODERATE) {
            // 适中内存压力
            LOG.i("App", "内存压力适中，清理部分缓存");
            FileUtils.cleanPlayerCache();
        } else if (level >= TRIM_MEMORY_COMPLETE) {
            // 高内存压力
            LOG.i("App", "内存压力较大，清理所有非必要缓存");
            FileUtils.cleanPlayerCache();
            FileUtils.cleanThumbnails();
            if (this.vodInfo != null && !isInPlayingState()) {
                this.vodInfo = null;
                System.gc();
            }
        }
    }

    /**
     * 判断当前是否在播放视频状态
     * 如果正在播放视频，则不清理相关资源
     */
    private boolean isInPlayingState() {
        // 检查是否有播放相关的活动处于前台
        // 如果在这些活动中，即使内存压力大也不应清理视频相关资源
        return isActivityInForeground("DetailActivity") ||
               isActivityInForeground("LiveActivity") ||
               isActivityInForeground("LocalPlayActivity");
    }

    /**
     * 判断指定Activity是否在前台
     */
    private boolean isActivityInForeground(String activityName) {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
            if (!tasks.isEmpty()) {
                String topActivityName = tasks.get(0).topActivity.getClassName();
                return topActivityName.contains(activityName);
            }
        } catch (Exception e) {
            LOG.e("App", "检查前台Activity失败: " + e.getMessage());
        }
        return false;
    }

    /**
     * 播放器内存优化
     * 释放不必要的播放器资源
     */
    public void releasePlayerResources() {
        try {
            ThreadPoolManager.executeIO(() -> {
                LOG.i("App", "释放播放器相关资源");
                FileUtils.cleanPlayerCache();
                // 清理其他可释放的资源
            });
        } catch (Exception e) {
            LOG.e("App", "释放播放器资源出错: " + e.getMessage());
        }
    }

    private void putDefault(String key, Object value) {
        if (!Hawk.contains(key)) {
            Hawk.put(key, value);
        }
    }


    private VodInfo vodInfo;
    public void setVodInfo(VodInfo vodinfo){
        this.vodInfo = vodinfo;
    }
    public VodInfo getVodInfo(){
        return this.vodInfo;
    }

    /**
     * 清除当前保存的视频信息
     * 在不需要时调用此方法避免内存泄漏
     */
    public void clearVodInfo() {
        this.vodInfo = null;
    }

    public static P2PClass getp2p() {
        try {
            // 检查设备架构是否支持
            if (!instance.isArchitectureSupported()) {
                LOG.e("App", "P2P功能已禁用，因为当前设备架构不受支持");
                return null;
            }

            // 尝试加载p2p库
            boolean p2pLibLoaded = com.p2p.P2PClass.loadLibrary();
            if (!p2pLibLoaded) {
                LOG.e("App", "P2P库加载失败，相关功能将被禁用");
                return null;
            }

            // 初始化P2P实例
            if (p == null) {
                try {
                    p = new P2PClass(instance.getExternalCacheDir().getAbsolutePath());
                    LOG.i("App", "P2P功能初始化成功");
                } catch (Exception e) {
                    LOG.e("App", "P2P功能初始化失败: " + e.getMessage());
                    p = null;
                }
            }
            return p;
        } catch (Throwable e) {
            LOG.e("App", "P2P功能发生错误: " + e.getMessage());
            return null;
        }
    }

    private void initCrashConfig(){
        //配置全局异常崩溃操作
        CaocConfig.Builder.create()
                .backgroundMode(CaocConfig.BACKGROUND_MODE_SILENT) //背景模式,开启沉浸式
                .enabled(true) //是否启动全局异常捕获
                .showErrorDetails(true) //是否显示错误详细信息
                .showRestartButton(true) //是否显示重启按钮
                .trackActivities(true) //是否跟踪Activity
                .minTimeBetweenCrashesMs(2000) //崩溃的间隔时间(毫秒)
                .errorDrawable(cat.ereza.customactivityoncrash.R.drawable.customactivityoncrash_error_image_m3) //错误图标
                .restartActivity(MainActivity.class) //重新启动后的activity
                .apply();
    }

}