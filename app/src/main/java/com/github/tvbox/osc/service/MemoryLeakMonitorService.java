package com.github.tvbox.osc.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.github.tvbox.osc.util.LoadSirLeakFixer;
import com.github.tvbox.osc.util.MemoryLeakBootstrap;
import com.github.tvbox.osc.util.MemoryLeakFixer;
import com.github.tvbox.osc.util.ThreadPoolManager;

/**
 * 内存泄漏监控服务
 * 定期检测和修复内存泄漏问题
 */
public class MemoryLeakMonitorService extends Service {
    private static final String TAG = "MemoryLeakMonitorService";
    
    // 监控间隔（毫秒）
    private static final long LIGHT_MONITOR_INTERVAL = 2 * 60 * 1000; // 2分钟
    private static final long HEAVY_MONITOR_INTERVAL = 10 * 60 * 1000; // 10分钟
    
    // 内存使用阈值
    private static final double MEMORY_WARNING_THRESHOLD = 75.0; // 75%
    private static final double MEMORY_CRITICAL_THRESHOLD = 85.0; // 85%
    
    private Handler mHandler;
    private boolean mIsRunning = false;
    
    // 监控任务
    private Runnable mLightMonitorTask;
    private Runnable mHeavyMonitorTask;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "内存泄漏监控服务创建");
        
        mHandler = new Handler(Looper.getMainLooper());
        initMonitorTasks();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "内存泄漏监控服务启动");
        
        if (!mIsRunning) {
            startMonitoring();
        }
        
        // 服务被系统杀死后自动重启
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "内存泄漏监控服务销毁");
        
        stopMonitoring();
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    /**
     * 初始化监控任务
     */
    private void initMonitorTasks() {
        // 轻量级监控任务
        mLightMonitorTask = new Runnable() {
            @Override
            public void run() {
                if (mIsRunning) {
                    performLightMonitoring();
                    // 重新安排下次执行
                    mHandler.postDelayed(this, LIGHT_MONITOR_INTERVAL);
                }
            }
        };
        
        // 重量级监控任务
        mHeavyMonitorTask = new Runnable() {
            @Override
            public void run() {
                if (mIsRunning) {
                    performHeavyMonitoring();
                    // 重新安排下次执行
                    mHandler.postDelayed(this, HEAVY_MONITOR_INTERVAL);
                }
            }
        };
    }
    
    /**
     * 开始监控
     */
    private void startMonitoring() {
        mIsRunning = true;
        
        // 启动轻量级监控
        mHandler.post(mLightMonitorTask);
        
        // 启动重量级监控
        mHandler.postDelayed(mHeavyMonitorTask, HEAVY_MONITOR_INTERVAL);
        
        Log.i(TAG, "内存泄漏监控已启动");
    }
    
    /**
     * 停止监控
     */
    private void stopMonitoring() {
        mIsRunning = false;
        
        if (mHandler != null) {
            mHandler.removeCallbacks(mLightMonitorTask);
            mHandler.removeCallbacks(mHeavyMonitorTask);
        }
        
        Log.i(TAG, "内存泄漏监控已停止");
    }
    
    /**
     * 执行轻量级监控
     */
    private void performLightMonitoring() {
        ThreadPoolManager.executeIO(() -> {
            try {
                Log.d(TAG, "执行轻量级内存监控");
                
                // 检查内存使用情况
                double memoryUsage = getMemoryUsagePercentage();
                Log.d(TAG, String.format("当前内存使用率: %.1f%%", memoryUsage));
                
                // 根据内存使用情况采取不同的措施
                if (memoryUsage > MEMORY_CRITICAL_THRESHOLD) {
                    Log.w(TAG, "内存使用率过高，执行紧急清理");
                    performEmergencyCleanup();
                } else if (memoryUsage > MEMORY_WARNING_THRESHOLD) {
                    Log.w(TAG, "内存使用率较高，执行预防性清理");
                    performPreventiveCleanup();
                }
                
                // 清理LoadSir全局引用
                LoadSirLeakFixer.clearGlobalLoadSirReferences();
                
            } catch (Exception e) {
                Log.e(TAG, "轻量级监控失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 执行重量级监控
     */
    private void performHeavyMonitoring() {
        ThreadPoolManager.executeIO(() -> {
            try {
                Log.d(TAG, "执行重量级内存监控");
                
                // 执行全面的内存泄漏修复
                try {
                    MemoryLeakFixer.fixAllLeaksImmediate();
                } catch (Exception e) {
                    Log.w(TAG, "修复内存泄漏失败: " + e.getMessage());
                }

                // 强制垃圾回收
                System.gc();
                System.gc();
                
                // 监控内存使用情况
                MemoryLeakBootstrap.monitorMemoryUsage("重量级监控后");
                
                Log.d(TAG, "重量级内存监控完成");
            } catch (Exception e) {
                Log.e(TAG, "重量级监控失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 执行紧急清理
     */
    private void performEmergencyCleanup() {
        try {
            Log.w(TAG, "执行紧急内存清理");
            
            // 立即修复所有内存泄漏
            try {
                MemoryLeakFixer.fixAllLeaksImmediate();
            } catch (Exception e) {
                Log.w(TAG, "修复内存泄漏失败: " + e.getMessage());
            }

            // 清理LoadSir
            LoadSirLeakFixer.clearGlobalLoadSirReferences();

            // 强制垃圾回收
            System.gc();
            System.gc();
            
            Log.w(TAG, "紧急内存清理完成");
        } catch (Exception e) {
            Log.e(TAG, "紧急清理失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行预防性清理
     */
    private void performPreventiveCleanup() {
        try {
            Log.i(TAG, "执行预防性内存清理");
            
            // 清理部分内存泄漏
            try {
                MemoryLeakFixer.fixAllLeaksImmediate();
            } catch (Exception e) {
                Log.w(TAG, "修复内存泄漏失败: " + e.getMessage());
            }
            
            // 执行垃圾回收
            System.gc();
            
            Log.i(TAG, "预防性内存清理完成");
        } catch (Exception e) {
            Log.e(TAG, "预防性清理失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取内存使用百分比
     */
    private double getMemoryUsagePercentage() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            return (usedMemory * 100.0 / maxMemory);
        } catch (Exception e) {
            Log.e(TAG, "获取内存使用率失败: " + e.getMessage());
            return 0.0;
        }
    }
}
