package com.github.tvbox.osc.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.github.tvbox.osc.util.MemoryLeakBootstrap;
import com.github.tvbox.osc.util.ThreadPoolManager;

/**
 * 内存清理广播接收器
 * 用于接收外部触发的内存清理和报告请求
 */
public class MemoryCleanupReceiver extends BroadcastReceiver {
    private static final String TAG = "MemoryCleanupReceiver";
    
    // 广播动作
    public static final String ACTION_TRIGGER_MEMORY_CLEANUP = "com.github.tvbox.osc.TRIGGER_MEMORY_CLEANUP";
    public static final String ACTION_GET_MEMORY_REPORT = "com.github.tvbox.osc.GET_MEMORY_REPORT";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        
        String action = intent.getAction();
        Log.i(TAG, "收到广播: " + action);
        
        switch (action) {
            case ACTION_TRIGGER_MEMORY_CLEANUP:
                handleMemoryCleanup();
                break;
            case ACTION_GET_MEMORY_REPORT:
                handleMemoryReport();
                break;
            default:
                Log.w(TAG, "未知的广播动作: " + action);
                break;
        }
    }
    
    /**
     * 处理内存清理请求
     */
    private void handleMemoryCleanup() {
        try {
            Log.i(TAG, "开始处理手动内存清理请求");
            
            // 在后台线程执行清理
            ThreadPoolManager.executeIO(() -> {
                try {
                    // 触发手动清理
                    MemoryLeakBootstrap.triggerManualCleanup();
                    
                    // 监控内存使用情况
                    MemoryLeakBootstrap.monitorMemoryUsage("手动清理后");
                    
                    Log.i(TAG, "手动内存清理完成");
                } catch (Exception e) {
                    Log.e(TAG, "手动内存清理失败: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "处理内存清理请求失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理内存报告请求
     */
    private void handleMemoryReport() {
        try {
            Log.i(TAG, "开始处理内存报告请求");
            
            // 在后台线程生成报告
            ThreadPoolManager.executeIO(() -> {
                try {
                    // 获取内存报告
                    String memoryReport = MemoryLeakBootstrap.getMemoryReport();
                    
                    // 输出到日志
                    Log.i(TAG, "内存使用报告:\n" + memoryReport);
                    
                    // 监控当前内存使用情况
                    MemoryLeakBootstrap.monitorMemoryUsage("报告生成时");
                    
                } catch (Exception e) {
                    Log.e(TAG, "生成内存报告失败: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "处理内存报告请求失败: " + e.getMessage());
        }
    }
}
