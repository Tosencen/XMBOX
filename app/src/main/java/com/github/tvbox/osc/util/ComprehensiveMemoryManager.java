package com.github.tvbox.osc.util;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 综合内存管理器
 * 集成所有内存泄漏检测、修复和优化功能
 */
public class ComprehensiveMemoryManager implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "ComprehensiveMemoryManager";
    private static ComprehensiveMemoryManager sInstance;
    private static WeakReference<Context> sContextRef;
    private static Handler sMainHandler;

    // Activity生命周期跟踪
    private final ConcurrentHashMap<String, WeakReference<Activity>> mActivityRefs = new ConcurrentHashMap<>();
    private final List<String> mLeakedActivities = new ArrayList<>();

    // 内存监控
    private boolean mMemoryMonitoringEnabled = true;
    private long mLastMemoryCheckTime = 0;
    private static final long MEMORY_CHECK_INTERVAL = 30000; // 30秒检查一次

    private ComprehensiveMemoryManager() {
        sMainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized ComprehensiveMemoryManager getInstance() {
        if (sInstance == null) {
            sInstance = new ComprehensiveMemoryManager();
        }
        return sInstance;
    }

    /**
     * 初始化内存管理器
     */
    public void init(Context context) {
        sContextRef = new WeakReference<>(context.getApplicationContext());

        if (context instanceof Application) {
            ((Application) context).registerActivityLifecycleCallbacks(this);
        }

        // 启动内存监控
        startMemoryMonitoring();

        LOG.i(TAG, "综合内存管理器已初始化");
    }

    /**
     * 启动内存监控
     */
    private void startMemoryMonitoring() {
        if (!mMemoryMonitoringEnabled) {
            return;
        }

        sMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    // 检查内存使用情况
                    checkMemoryUsage();

                    // 检查Activity泄漏
                    checkActivityLeaks();

                    // 清理已完成的任务
                    ThreadPoolManager.purgeCompletedTasks();

                    // 继续监控
                    if (mMemoryMonitoringEnabled) {
                        sMainHandler.postDelayed(this, MEMORY_CHECK_INTERVAL);
                    }
                } catch (Exception e) {
                    LOG.e(TAG, "内存监控异常: " + e.getMessage());
                }
            }
        }, MEMORY_CHECK_INTERVAL);
    }

    /**
     * 检查内存使用情况
     */
    private void checkMemoryUsage() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();

            double usagePercent = (usedMemory * 100.0) / maxMemory;

            LOG.i(TAG, String.format("内存使用: %.1fMB/%.1fMB (%.1f%%)",
                usedMemory / 1024.0 / 1024.0,
                maxMemory / 1024.0 / 1024.0,
                usagePercent));

            // 如果内存使用率过高，触发内存清理
            if (usagePercent > 85.0) {
                LOG.w(TAG, "内存使用率过高，触发紧急内存清理");
                performEmergencyMemoryCleanup();
            } else if (usagePercent > 75.0) {
                LOG.w(TAG, "内存使用率较高，触发常规内存清理");
                performRegularMemoryCleanup();
            }
        } catch (Exception e) {
            LOG.e(TAG, "检查内存使用异常: " + e.getMessage());
        }
    }

    /**
     * 检查Activity泄漏
     */
    private void checkActivityLeaks() {
        try {
            Iterator<String> iterator = mActivityRefs.keySet().iterator();
            while (iterator.hasNext()) {
                String activityName = iterator.next();
                WeakReference<Activity> ref = mActivityRefs.get(activityName);

                if (ref == null || ref.get() == null) {
                    // Activity已被回收，移除引用
                    iterator.remove();
                } else {
                    Activity activity = ref.get();
                    if (activity.isDestroyed() || activity.isFinishing()) {
                        // Activity应该被回收但还在内存中
                        if (!mLeakedActivities.contains(activityName)) {
                            LOG.w(TAG, "检测到可能的Activity内存泄漏: " + activityName);
                            mLeakedActivities.add(activityName);

                            // 尝试修复泄漏
                            fixActivityLeak(activity);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.e(TAG, "检查Activity泄漏异常: " + e.getMessage());
        }
    }

    /**
     * 修复Activity泄漏
     */
    private void fixActivityLeak(Activity activity) {
        try {
            // 这里可以添加具体的Activity泄漏修复逻辑
            // 例如清理静态引用、注销监听器等
            LOG.i(TAG, "尝试修复Activity泄漏: " + activity.getClass().getSimpleName());

            // 强制垃圾回收
            System.gc();
            System.runFinalization();
        } catch (Exception e) {
            LOG.e(TAG, "修复Activity泄漏失败: " + e.getMessage());
        }
    }

    /**
     * 执行紧急内存清理
     */
    private void performEmergencyMemoryCleanup() {
        try {
            LOG.i(TAG, "执行紧急内存清理");

            // 立即修复所有内存泄漏
            MemoryLeakFixer.fixAllLeaksImmediate();

            // 清理图片缓存
            Context context = sContextRef != null ? sContextRef.get() : null;
            if (context != null) {
                GlideHelper.clearMemoryCache(context);
            }

            // 清理网络缓存
            try {
                // 取消所有网络请求
                com.lzy.okgo.OkGo.getInstance().cancelAll();
            } catch (Exception e) {
                LOG.w(TAG, "清理网络缓存失败: " + e.getMessage());
            }

            // 强制垃圾回收
            System.gc();
            System.runFinalization();
            System.gc();

            LOG.i(TAG, "紧急内存清理完成");
        } catch (Exception e) {
            LOG.e(TAG, "紧急内存清理失败: " + e.getMessage());
        }
    }

    /**
     * 执行常规内存清理
     */
    private void performRegularMemoryCleanup() {
        try {
            LOG.i(TAG, "执行常规内存清理");

            // 清理线程池中已完成的任务
            ThreadPoolManager.purgeCompletedTasks();

            // 清理部分图片缓存
            Context context = sContextRef != null ? sContextRef.get() : null;
            if (context != null) {
                GlideHelper.clearMemoryCache(context);
            }

            // 建议垃圾回收
            System.gc();

            LOG.i(TAG, "常规内存清理完成");
        } catch (Exception e) {
            LOG.e(TAG, "常规内存清理失败: " + e.getMessage());
        }
    }

    /**
     * 获取内存使用报告
     */
    public String getMemoryReport() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();

            double usagePercent = (usedMemory * 100.0) / maxMemory;

            StringBuilder report = new StringBuilder();
            report.append("=== 内存使用报告 ===\n");
            report.append(String.format("已用内存: %.1fMB\n", usedMemory / 1024.0 / 1024.0));
            report.append(String.format("最大内存: %.1fMB\n", maxMemory / 1024.0 / 1024.0));
            report.append(String.format("使用率: %.1f%%\n", usagePercent));
            report.append(String.format("活跃Activity数量: %d\n", mActivityRefs.size()));
            report.append(String.format("疑似泄漏Activity数量: %d\n", mLeakedActivities.size()));

            if (!mLeakedActivities.isEmpty()) {
                report.append("疑似泄漏的Activity:\n");
                for (String activityName : mLeakedActivities) {
                    report.append("  - ").append(activityName).append("\n");
                }
            }

            return report.toString();
        } catch (Exception e) {
            return "获取内存报告失败: " + e.getMessage();
        }
    }

    // Activity生命周期回调
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        String activityName = activity.getClass().getSimpleName();
        mActivityRefs.put(activityName, new WeakReference<>(activity));
        LOG.d(TAG, "Activity创建: " + activityName);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        // 不需要处理
    }

    @Override
    public void onActivityResumed(Activity activity) {
        // 不需要处理
    }

    @Override
    public void onActivityPaused(Activity activity) {
        // 不需要处理
    }

    @Override
    public void onActivityStopped(Activity activity) {
        // 不需要处理
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        // 不需要处理
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        String activityName = activity.getClass().getSimpleName();
        LOG.d(TAG, "Activity销毁: " + activityName);

        // 延迟检查是否真正被回收
        sMainHandler.postDelayed(() -> {
            WeakReference<Activity> ref = mActivityRefs.get(activityName);
            if (ref != null && ref.get() != null) {
                LOG.w(TAG, "Activity销毁后仍在内存中: " + activityName);
            }
        }, 3000);
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        try {
            mMemoryMonitoringEnabled = false;

            if (sMainHandler != null) {
                sMainHandler.removeCallbacksAndMessages(null);
            }

            mActivityRefs.clear();
            mLeakedActivities.clear();
            sContextRef = null;

            LOG.i(TAG, "综合内存管理器已清理");
        } catch (Exception e) {
            LOG.e(TAG, "清理综合内存管理器失败: " + e.getMessage());
        }
    }
}
