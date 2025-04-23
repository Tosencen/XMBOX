package com.github.tvbox.osc.base;

import android.content.Intent;
import android.text.TextUtils;

import androidx.multidex.MultiDexApplication;

import com.github.catvod.crawler.JsLoader;
import com.github.tvbox.osc.R;
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
import com.github.tvbox.osc.util.Utils;
import com.kingja.loadsir.core.LoadSir;
import com.orhanobut.hawk.Hawk;
import com.p2p.P2PClass;
import com.whl.quickjs.android.QuickJSLoader;

import java.util.ArrayList;
import java.util.List;

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
        // 只支持ARM架构
        return arch != null && (arch.contains("arm") || arch.contains("ARM"));
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

            try {
                initParams();
                LOG.i("App", "参数初始化成功");
            } catch (Exception e) {
                LOG.e("App", "参数初始化失败: " + e.getMessage());
            }

            try {
                // OKGo
                OkGoHelper.init(); //台标获取
                LOG.i("App", "OkGo初始化成功");
            } catch (Exception e) {
                LOG.e("App", "OkGo初始化失败: " + e.getMessage());
            }

            try {
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
                //初始化数据库
                AppDataManager.init();
                LOG.i("App", "数据库初始化成功");
            } catch (Exception e) {
                LOG.e("App", "数据库初始化失败: " + e.getMessage());
            }

            try {
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
                FileUtils.cleanPlayerCache();
                LOG.i("App", "清理播放器缓存成功");
            } catch (Exception e) {
                LOG.e("App", "清理播放器缓存失败: " + e.getMessage());
            }

            try {
                initCrashConfig();
                LOG.i("App", "崩溃配置初始化成功");
            } catch (Exception e) {
                LOG.e("App", "崩溃配置初始化失败: " + e.getMessage());
            }

            try {
                Utils.initTheme();
                LOG.i("App", "主题初始化成功");
            } catch (Exception e) {
                LOG.e("App", "主题初始化失败: " + e.getMessage());
            }

            try {
                // 初始Material Symbols字体
                MaterialSymbolsLoader.init(this);
                LOG.i("App", "Material Symbols字体初始化成功");
            } catch (Exception e) {
                LOG.e("App", "Material Symbols字体初始化失败: " + e.getMessage());
            }

            LOG.i("App", "应用初始化完成");
        } catch (Throwable e) {
            LOG.e("App", "应用初始化过程中发生严重错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initParams() {
        // Hawk
        Hawk.init(this).build();
        Hawk.put(HawkConfig.DEBUG_OPEN, false);

        putDefault(HawkConfig.HOME_REC, 0);                  //推荐: 0=豆瓣热播, 1=站点推荐
        putDefault(HawkConfig.PLAY_TYPE, 2);                 //播放器: 0=系统, 1=IJK, 2=Exo
        putDefault(HawkConfig.IJK_CODEC, "硬解码");           //IJK解码: 软解码, 硬解码
        putDefault(HawkConfig.BACKGROUND_PLAY_TYPE,2);           //后台播放: 0 关闭,1 开启,2 画中画
        putDefault(HawkConfig.DOH_URL, 0);                   //安全DNS: 0=关闭, 1=腾讯, 2=阿里, 3=360, 4=Google, 5=AdGuard, 6=Quad9
        putDefault(HawkConfig.PLAY_SCALE, 0);                //画面缩放: 0=默认, 1=16:9, 2=4:3, 3=填充, 4=原始, 5=裁剪
        putDefault(HawkConfig.HISTORY_NUM, 2);                //历史记录数量: 0=30, 1=50, 2=70
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
        JsLoader.load();
        // 清理资源
        vodInfo = null;
        // 清理VideoView引用
        try {
            xyz.doikki.videoplayer.player.VideoViewManager.instance().removeAll();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 投屏功能已完全移除
        // 关闭线程池
        com.github.tvbox.osc.util.ThreadPoolManager.shutdown();
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