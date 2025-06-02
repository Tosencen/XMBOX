package com.github.tvbox.osc.util;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Debug;

import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.util.LOG;

/**
 * 内存优化工具类
 * 提供内存使用监控和优化功能
 */
public class MemoryOptimizer {

    private static final String TAG = "MemoryOptimizer";
    private static final int LOW_MEMORY_THRESHOLD_MB = 100; // 低内存阈值，单位MB

    /**
     * 获取当前应用内存使用情况
     * @return 内存使用信息字符串
     */
    public static String getMemoryInfo() {
        Context context = App.getInstance();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024; // 最大可用内存，单位MB
        long totalMemory = runtime.totalMemory() / 1024 / 1024; // 已分配内存，单位MB
        long freeMemory = runtime.freeMemory() / 1024 / 1024; // 已分配内存中的空闲内存，单位MB
        long usedMemory = totalMemory - freeMemory; // 已使用内存，单位MB

        StringBuilder sb = new StringBuilder();
        sb.append("内存使用情况：\n");
        sb.append("最大可用内存：").append(maxMemory).append("MB\n");
        sb.append("已分配内存：").append(totalMemory).append("MB\n");
        sb.append("已使用内存：").append(usedMemory).append("MB\n");
        sb.append("空闲内存：").append(freeMemory).append("MB\n");
        sb.append("系统可用内存：").append(memoryInfo.availMem / 1024 / 1024).append("MB\n");
        sb.append("系统是否处于低内存状态：").append(memoryInfo.lowMemory);

        return sb.toString();
    }

    /**
     * 检查是否处于低内存状态
     * @return 是否处于低内存状态
     */
    public static boolean isLowMemory() {
        Context context = App.getInstance();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        // 系统报告的低内存状态
        if (memoryInfo.lowMemory) {
            return true;
        }

        // 可用内存低于阈值
        long availableMB = memoryInfo.availMem / 1024 / 1024;
        if (availableMB < LOW_MEMORY_THRESHOLD_MB) {
            return true;
        }

        return false;
    }

    /**
     * 清理内存
     * 在内存不足时调用
     */
    public static void cleanMemory() {
        // 记录清理前内存
        if (LOG.isDebug()) {
            LOG.d(TAG, "清理内存前：" + getMemoryInfo());
        }

        // 清理图片缓存
        ThreadPoolManager.executeIO(new Runnable() {
            @Override
            public void run() {
                try {
                    GlideHelper.clearMemoryCache(App.getInstance());
                    LOG.i(TAG, "已清理Glide内存缓存");
                } catch (Exception e) {
                    LOG.e(TAG, "清理Glide内存缓存失败: " + e.getMessage());
                }
            }
        });

        // 请求系统进行GC
        System.gc();
        System.runFinalization();

        // 在Android 8.0及以上版本，可以使用更多内存管理API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // 提示系统进行内存整理
                Debug.startAllocCounting();
                Debug.stopAllocCounting();

                // 请求系统进行GC
                Runtime.getRuntime().gc();
            } catch (Exception e) {
                LOG.e(TAG, "高级内存清理失败: " + e.getMessage());
            }
        }

        // 记录清理后内存
        if (LOG.isDebug()) {
            LOG.d(TAG, "清理内存后：" + getMemoryInfo());
        }
    }

    /**
     * 监控内存使用情况
     * 在适当的时候调用此方法检查内存状态
     */
    public static void monitorMemory() {
        if (isLowMemory()) {
            LOG.w(TAG, "检测到内存不足，开始清理内存");
            cleanMemory();
        }
    }
}
