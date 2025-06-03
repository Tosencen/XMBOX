package com.github.tvbox.osc.util;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简化的内存管理器
 * 借鉴TVBoxOS-Mobile的简洁策略，避免过度复杂的内存管理
 */
public class SimpleMemoryManager implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "SimpleMemoryManager";
    private static SimpleMemoryManager sInstance;
    private static WeakReference<Context> sContextRef;
    private static Handler sMainHandler;
    
    // Activity计数器
    private final ConcurrentHashMap<String, Integer> mActivityCounts = new ConcurrentHashMap<>();
    private int mTotalActivityCount = 0;
    
    // 内存监控
    private boolean mMemoryMonitoringEnabled = true;
    private static final long MEMORY_CHECK_INTERVAL = 60000; // 60秒检查一次，减少频率
    
    private SimpleMemoryManager() {
        sMainHandler = new Handler(Looper.getMainLooper());
    }
    
    public static synchronized SimpleMemoryManager getInstance() {
        if (sInstance == null) {
            sInstance = new SimpleMemoryManager();
        }
        return sInstance;
    }
    
    /**
     * 初始化简化内存管理器
     */
    public void init(Context context) {
        sContextRef = new WeakReference<>(context.getApplicationContext());
        
        if (context instanceof Application) {
            ((Application) context).registerActivityLifecycleCallbacks(this);
        }
        
        // 启动轻量级内存监控
        startLightweightMemoryMonitoring();
        
        LOG.i(TAG, "简化内存管理器已初始化");
    }
    
    /**
     * 启动轻量级内存监控
     */
    private void startLightweightMemoryMonitoring() {
        if (!mMemoryMonitoringEnabled) {
            return;
        }
        
        sMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    // 简单的内存检查
                    checkMemoryUsageSimple();
                    
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
     * 简单的内存使用检查
     */
    private void checkMemoryUsageSimple() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            double usagePercent = (usedMemory * 100.0) / maxMemory;
            
            LOG.i(TAG, String.format("内存使用: %.1fMB/%.1fMB (%.1f%%) 活跃Activity: %d", 
                usedMemory / 1024.0 / 1024.0, 
                maxMemory / 1024.0 / 1024.0, 
                usagePercent,
                mTotalActivityCount));
            
            // 只在内存使用率非常高时才进行清理
            if (usagePercent > 90.0) {
                LOG.w(TAG, "内存使用率过高，触发简单清理");
                performSimpleMemoryCleanup();
            }
        } catch (Exception e) {
            LOG.e(TAG, "检查内存使用异常: " + e.getMessage());
        }
    }
    
    /**
     * 执行简单的内存清理
     */
    private void performSimpleMemoryCleanup() {
        try {
            LOG.i(TAG, "执行简单内存清理");
            
            // 清理图片缓存
            Context context = sContextRef != null ? sContextRef.get() : null;
            if (context != null) {
                try {
                    GlideHelper.clearMemoryCache(context);
                } catch (Exception e) {
                    LOG.w(TAG, "清理图片缓存失败: " + e.getMessage());
                }
            }
            
            // 取消网络请求
            try {
                com.lzy.okgo.OkGo.getInstance().cancelAll();
            } catch (Exception e) {
                LOG.w(TAG, "取消网络请求失败: " + e.getMessage());
            }
            
            // 建议垃圾回收
            System.gc();
            
            LOG.i(TAG, "简单内存清理完成");
        } catch (Exception e) {
            LOG.e(TAG, "简单内存清理失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取简单的内存报告
     */
    public String getSimpleMemoryReport() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            double usagePercent = (usedMemory * 100.0) / maxMemory;
            
            StringBuilder report = new StringBuilder();
            report.append("=== 简化内存报告 ===\n");
            report.append(String.format("已用内存: %.1fMB\n", usedMemory / 1024.0 / 1024.0));
            report.append(String.format("最大内存: %.1fMB\n", maxMemory / 1024.0 / 1024.0));
            report.append(String.format("使用率: %.1f%%\n", usagePercent));
            report.append(String.format("活跃Activity数量: %d\n", mTotalActivityCount));
            
            return report.toString();
        } catch (Exception e) {
            return "获取内存报告失败: " + e.getMessage();
        }
    }
    
    /**
     * 手动触发内存清理
     */
    public void triggerMemoryCleanup() {
        performSimpleMemoryCleanup();
    }
    
    // Activity生命周期回调
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        String activityName = activity.getClass().getSimpleName();
        mActivityCounts.put(activityName, mActivityCounts.getOrDefault(activityName, 0) + 1);
        mTotalActivityCount++;
        LOG.d(TAG, "Activity创建: " + activityName + " (总数: " + mTotalActivityCount + ")");
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
        Integer count = mActivityCounts.get(activityName);
        if (count != null && count > 1) {
            mActivityCounts.put(activityName, count - 1);
        } else {
            mActivityCounts.remove(activityName);
        }
        mTotalActivityCount--;
        LOG.d(TAG, "Activity销毁: " + activityName + " (总数: " + mTotalActivityCount + ")");
        
        // 如果Activity数量为0，触发一次清理
        if (mTotalActivityCount <= 0) {
            sMainHandler.postDelayed(() -> {
                if (mTotalActivityCount <= 0) {
                    LOG.i(TAG, "所有Activity已销毁，触发清理");
                    performSimpleMemoryCleanup();
                }
            }, 2000);
        }
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
            
            mActivityCounts.clear();
            mTotalActivityCount = 0;
            sContextRef = null;
            
            LOG.i(TAG, "简化内存管理器已清理");
        } catch (Exception e) {
            LOG.e(TAG, "清理简化内存管理器失败: " + e.getMessage());
        }
    }
}
