package com.github.tvbox.osc.util;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.ui.activity.MainActivity;
import com.orhanobut.hawk.Hawk;

/**
 * 应用生命周期管理器
 * 负责监控应用前后台状态，并在长时间后台后重启应用
 */
public class AppLifecycleManager implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "AppLifecycleManager";
    private static final String KEY_BACKGROUND_TIME = "app_background_time";

    private static AppLifecycleManager instance;
    private int activityCount = 0;
    private boolean isAppInBackground = false;
    private long backgroundTime = 0;
    private Handler mainHandler;
    private Activity currentActivity;

    private AppLifecycleManager() {
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static AppLifecycleManager getInstance() {
        if (instance == null) {
            synchronized (AppLifecycleManager.class) {
                if (instance == null) {
                    instance = new AppLifecycleManager();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化生命周期管理器
     */
    public void init(Application application) {
        application.registerActivityLifecycleCallbacks(this);

        // 初始化重启配置
        AppRestartConfig.initDefaults();

        LOG.i(TAG, "应用生命周期管理器已初始化");
        LOG.i(TAG, AppRestartConfig.getConfigSummary());
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        LOG.d(TAG, "Activity创建: " + activity.getClass().getSimpleName());
        currentActivity = activity;
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        activityCount++;
        currentActivity = activity;

        if (isAppInBackground) {
            // 应用从后台回到前台
            onAppForeground(activity);
        }

        LOG.d(TAG, "Activity启动: " + activity.getClass().getSimpleName() + ", 活跃数量: " + activityCount);
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivity = activity;
        LOG.d(TAG, "Activity恢复: " + activity.getClass().getSimpleName());
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        LOG.d(TAG, "Activity暂停: " + activity.getClass().getSimpleName());
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        activityCount--;

        if (activityCount <= 0) {
            // 应用进入后台
            onAppBackground();
        }

        LOG.d(TAG, "Activity停止: " + activity.getClass().getSimpleName() + ", 活跃数量: " + activityCount);
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        LOG.d(TAG, "Activity保存状态: " + activity.getClass().getSimpleName());
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (currentActivity == activity) {
            currentActivity = null;
        }
        LOG.d(TAG, "Activity销毁: " + activity.getClass().getSimpleName());
    }

    /**
     * 应用进入后台
     */
    private void onAppBackground() {
        if (!isAppInBackground) {
            isAppInBackground = true;
            backgroundTime = System.currentTimeMillis();

            // 保存后台时间到本地
            Hawk.put(KEY_BACKGROUND_TIME, backgroundTime);

            LOG.i(TAG, "应用进入后台，时间: " + new java.util.Date(backgroundTime));

            // 延迟执行内存清理，避免影响用户体验
            mainHandler.postDelayed(() -> {
                if (isAppInBackground) {
                    // 清理内存
                    MemoryOptimizer.cleanMemory();
                    LOG.i(TAG, "后台内存清理完成");
                }
            }, 5000); // 5秒后清理
        }
    }

    /**
     * 应用回到前台
     */
    private void onAppForeground(Activity activity) {
        if (isAppInBackground) {
            isAppInBackground = false;
            long currentTime = System.currentTimeMillis();
            long backgroundDuration = currentTime - backgroundTime;

            LOG.i(TAG, "应用回到前台，后台时长: " + (backgroundDuration / 1000) + "秒");

            // 检查是否需要重启应用
            if (shouldRestartApp(backgroundDuration)) {
                restartApp(activity);
                return;
            }

            // 清除保存的后台时间
            Hawk.delete(KEY_BACKGROUND_TIME);

            // 应用回到前台时的优化
            onAppResumed();
        }
    }

    /**
     * 检查是否需要重启应用
     */
    private boolean shouldRestartApp(long backgroundDuration) {
        // 检查是否启用自动重启
        if (!AppRestartConfig.isAutoRestartEnabled()) {
            LOG.d(TAG, "自动重启已禁用");
            return false;
        }

        long restartThreshold = AppRestartConfig.getRestartThresholdMillis();

        // 检查后台时长是否超过阈值
        if (backgroundDuration >= restartThreshold) {
            LOG.i(TAG, "后台时长超过" + AppRestartConfig.getRestartThresholdHours() + "小时，需要重启应用");
            return true;
        }

        // 检查启动时是否有保存的后台时间（应用被系统杀死后重启的情况）
        if (Hawk.contains(KEY_BACKGROUND_TIME)) {
            long savedBackgroundTime = Hawk.get(KEY_BACKGROUND_TIME, 0L);
            if (savedBackgroundTime > 0) {
                long totalBackgroundDuration = System.currentTimeMillis() - savedBackgroundTime;
                if (totalBackgroundDuration >= restartThreshold) {
                    LOG.i(TAG, "检测到应用被系统杀死，且后台时长超过" + AppRestartConfig.getRestartThresholdHours() + "小时，需要重启应用");
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 重启应用
     */
    private void restartApp(Activity currentActivity) {
        try {
            LOG.i(TAG, "开始重启应用");

            // 显示重启提示（如果启用）
            if (AppRestartConfig.isShowRestartToast()) {
                MD3ToastUtils.showToast("应用已长时间后台运行，正在重新启动以优化性能...");
            }

            // 延迟重启，让用户看到提示
            mainHandler.postDelayed(() -> {
                try {
                    // 清理应用状态
                    cleanupAppState();

                    // 创建重启Intent
                    Intent restartIntent = new Intent(currentActivity, MainActivity.class);
                    restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    restartIntent.putExtra("app_restarted", true);

                    // 启动新的MainActivity
                    currentActivity.startActivity(restartIntent);

                    // 结束当前Activity
                    if (currentActivity != null) {
                        currentActivity.finish();
                    }

                    // 结束所有其他Activity
                    AppManager.getInstance().finishAllActivity();

                    LOG.i(TAG, "应用重启完成");

                } catch (Exception e) {
                    LOG.e(TAG, "重启应用时发生错误: " + e.getMessage());
                    e.printStackTrace();
                }
            }, 1500); // 1.5秒后重启

        } catch (Exception e) {
            LOG.e(TAG, "重启应用失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 清理应用状态
     */
    private void cleanupAppState() {
        try {
            LOG.i(TAG, "清理应用状态");

            // 检查是否启用重启时清理
            if (AppRestartConfig.isCleanupOnRestart()) {
                // 清理App中的数据
                App.getInstance().clearVodInfo();

                // 清理内存泄漏
                MemoryLeakFixer.fixAllLeaks();

                // 清理缓存
                FileUtils.cleanPlayerCache();

                LOG.i(TAG, "重启清理已执行");
            } else {
                LOG.i(TAG, "重启清理已禁用，跳过清理步骤");
            }

            // 清除后台时间记录（总是执行）
            Hawk.delete(KEY_BACKGROUND_TIME);

            LOG.i(TAG, "应用状态清理完成");
        } catch (Exception e) {
            LOG.e(TAG, "清理应用状态时发生错误: " + e.getMessage());
        }
    }

    /**
     * 应用恢复时的优化
     */
    private void onAppResumed() {
        try {
            LOG.i(TAG, "应用恢复，执行优化操作");

            // 检查内存使用情况
            MemoryLeakPreventer.monitorMemoryUsage("AppResumed");

            // 如果内存使用率过高，执行清理
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            double usagePercent = (usedMemory * 100.0) / maxMemory;

            if (usagePercent > 70.0) {
                LOG.w(TAG, "内存使用率较高(" + String.format("%.1f%%", usagePercent) + ")，执行内存清理");
                MemoryOptimizer.cleanMemory();
            }

        } catch (Exception e) {
            LOG.e(TAG, "应用恢复优化时发生错误: " + e.getMessage());
        }
    }

    /**
     * 获取当前Activity
     */
    public Activity getCurrentActivity() {
        return currentActivity;
    }

    /**
     * 检查应用是否在后台
     */
    public boolean isAppInBackground() {
        return isAppInBackground;
    }

    /**
     * 获取后台时长（毫秒）
     */
    public long getBackgroundDuration() {
        if (isAppInBackground && backgroundTime > 0) {
            return System.currentTimeMillis() - backgroundTime;
        }
        return 0;
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        currentActivity = null;
        LOG.i(TAG, "应用生命周期管理器已清理");
    }
}
