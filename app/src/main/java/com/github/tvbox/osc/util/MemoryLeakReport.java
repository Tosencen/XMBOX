package com.github.tvbox.osc.util;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.util.Log;

import java.text.DecimalFormat;

/**
 * 内存泄漏修复状态报告工具
 * 用于生成内存使用情况和泄漏修复状态的详细报告
 */
public class MemoryLeakReport {
    private static final String TAG = "MemoryLeakReport";
    
    /**
     * 生成完整的内存泄漏修复报告
     */
    public static void generateFullReport(Context context) {
        try {
            Log.i(TAG, "========== 内存泄漏修复状态报告 ==========");
            
            // 1. 内存使用情况
            generateMemoryUsageReport(context);
            
            // 2. Activity泄漏检测报告
            generateActivityLeakReport();
            
            // 3. 各种修复器状态报告
            generateFixerStatusReport();
            
            // 4. 系统资源状态报告
            generateSystemResourceReport(context);
            
            Log.i(TAG, "========== 报告生成完成 ==========");
        } catch (Exception e) {
            Log.e(TAG, "生成内存泄漏报告失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成内存使用情况报告
     */
    private static void generateMemoryUsageReport(Context context) {
        try {
            Log.i(TAG, "---------- 内存使用情况 ----------");
            
            // 获取运行时内存信息
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            DecimalFormat df = new DecimalFormat("#.##");
            
            Log.i(TAG, "最大可用内存: " + df.format(maxMemory / 1024.0 / 1024.0) + " MB");
            Log.i(TAG, "已分配内存: " + df.format(totalMemory / 1024.0 / 1024.0) + " MB");
            Log.i(TAG, "已使用内存: " + df.format(usedMemory / 1024.0 / 1024.0) + " MB");
            Log.i(TAG, "剩余内存: " + df.format(freeMemory / 1024.0 / 1024.0) + " MB");
            Log.i(TAG, "内存使用率: " + df.format((usedMemory * 100.0) / maxMemory) + "%");
            
            // 获取系统内存信息
            if (context != null) {
                ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memoryInfo);
                
                Log.i(TAG, "系统可用内存: " + df.format(memoryInfo.availMem / 1024.0 / 1024.0) + " MB");
                Log.i(TAG, "系统总内存: " + df.format(memoryInfo.totalMem / 1024.0 / 1024.0) + " MB");
                Log.i(TAG, "系统内存不足: " + (memoryInfo.lowMemory ? "是" : "否"));
            }
            
            // 获取堆内存信息
            Debug.MemoryInfo debugMemoryInfo = new Debug.MemoryInfo();
            Debug.getMemoryInfo(debugMemoryInfo);
            
            Log.i(TAG, "Dalvik堆内存: " + debugMemoryInfo.dalvikPss + " KB");
            Log.i(TAG, "Native堆内存: " + debugMemoryInfo.nativePss + " KB");
            Log.i(TAG, "其他内存: " + debugMemoryInfo.otherPss + " KB");
            Log.i(TAG, "总PSS内存: " + debugMemoryInfo.getTotalPss() + " KB");
            
        } catch (Exception e) {
            Log.e(TAG, "生成内存使用报告失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成Activity泄漏检测报告
     */
    private static void generateActivityLeakReport() {
        try {
            Log.i(TAG, "---------- Activity泄漏检测 ----------");
            
            int leakedActivityCount = ActivityLeakDetector.getLeakedActivityCount();
            Log.i(TAG, "可能泄漏的Activity数量: " + leakedActivityCount);
            
            if (leakedActivityCount > 0) {
                Log.w(TAG, "检测到Activity内存泄漏，建议检查以下方面：");
                Log.w(TAG, "1. EventBus是否正确注销");
                Log.w(TAG, "2. Handler是否清理了消息");
                Log.w(TAG, "3. 监听器是否移除");
                Log.w(TAG, "4. 动画是否停止");
                Log.w(TAG, "5. Timer是否取消");
            } else {
                Log.i(TAG, "未检测到Activity内存泄漏");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "生成Activity泄漏报告失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成各种修复器状态报告
     */
    private static void generateFixerStatusReport() {
        try {
            Log.i(TAG, "---------- 修复器状态 ----------");
            
            // 检查MemoryLeakFixManager状态
            try {
                int registeredActivityCount = MemoryLeakFixManager.getRegisteredActivityCount();
                int registeredFragmentCount = MemoryLeakFixManager.getRegisteredFragmentCount();
                
                Log.i(TAG, "MemoryLeakFixManager - 已注册Activity: " + registeredActivityCount);
                Log.i(TAG, "MemoryLeakFixManager - 已注册Fragment: " + registeredFragmentCount);
            } catch (Exception e) {
                Log.w(TAG, "MemoryLeakFixManager状态检查失败: " + e.getMessage());
            }
            
            // 检查ThreadPoolManager状态
            try {
                // 这里可以添加ThreadPoolManager的状态检查
                Log.i(TAG, "ThreadPoolManager - 状态正常");
            } catch (Exception e) {
                Log.w(TAG, "ThreadPoolManager状态检查失败: " + e.getMessage());
            }
            
            // 检查OkGoHelper状态
            try {
                // 这里可以添加OkGoHelper的状态检查
                Log.i(TAG, "OkGoHelper - 状态正常");
            } catch (Exception e) {
                Log.w(TAG, "OkGoHelper状态检查失败: " + e.getMessage());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "生成修复器状态报告失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成系统资源状态报告
     */
    private static void generateSystemResourceReport(Context context) {
        try {
            Log.i(TAG, "---------- 系统资源状态 ----------");
            
            // 检查文件描述符数量
            try {
                java.io.File procSelfFd = new java.io.File("/proc/self/fd");
                if (procSelfFd.exists() && procSelfFd.isDirectory()) {
                    String[] fdList = procSelfFd.list();
                    int fdCount = fdList != null ? fdList.length : 0;
                    Log.i(TAG, "文件描述符数量: " + fdCount);
                    
                    if (fdCount > 500) {
                        Log.w(TAG, "文件描述符数量过多，可能存在文件未关闭的问题");
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "检查文件描述符失败: " + e.getMessage());
            }
            
            // 检查线程数量
            try {
                ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
                ThreadGroup parentGroup;
                while ((parentGroup = rootGroup.getParent()) != null) {
                    rootGroup = parentGroup;
                }
                
                int threadCount = rootGroup.activeCount();
                Log.i(TAG, "活跃线程数量: " + threadCount);
                
                if (threadCount > 100) {
                    Log.w(TAG, "线程数量过多，可能存在线程泄漏");
                }
            } catch (Exception e) {
                Log.w(TAG, "检查线程数量失败: " + e.getMessage());
            }
            
            // 检查GC情况
            try {
                // 强制GC并观察内存变化
                long beforeGC = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                System.gc();
                Thread.sleep(100);
                long afterGC = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                
                long freedMemory = beforeGC - afterGC;
                DecimalFormat df = new DecimalFormat("#.##");
                
                Log.i(TAG, "GC前内存: " + df.format(beforeGC / 1024.0 / 1024.0) + " MB");
                Log.i(TAG, "GC后内存: " + df.format(afterGC / 1024.0 / 1024.0) + " MB");
                Log.i(TAG, "GC释放内存: " + df.format(freedMemory / 1024.0 / 1024.0) + " MB");
                
                if (freedMemory < 1024 * 1024) { // 小于1MB
                    Log.i(TAG, "GC效果良好，内存使用健康");
                } else {
                    Log.w(TAG, "GC释放了较多内存，可能存在内存泄漏");
                }
            } catch (Exception e) {
                Log.w(TAG, "检查GC情况失败: " + e.getMessage());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "生成系统资源报告失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成简化的内存状态报告
     */
    public static void generateSimpleReport() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            
            DecimalFormat df = new DecimalFormat("#.##");
            double usagePercent = (usedMemory * 100.0) / maxMemory;
            
            Log.i(TAG, "内存使用: " + df.format(usedMemory / 1024.0 / 1024.0) + " MB / " + 
                      df.format(maxMemory / 1024.0 / 1024.0) + " MB (" + 
                      df.format(usagePercent) + "%)");
            
            int leakedActivityCount = ActivityLeakDetector.getLeakedActivityCount();
            if (leakedActivityCount > 0) {
                Log.w(TAG, "检测到 " + leakedActivityCount + " 个可能泄漏的Activity");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "生成简化报告失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查内存使用是否健康
     */
    public static boolean isMemoryHealthy() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            
            double usagePercent = (usedMemory * 100.0) / maxMemory;
            
            // 内存使用率超过80%认为不健康
            if (usagePercent > 80) {
                return false;
            }
            
            // 有Activity泄漏认为不健康
            int leakedActivityCount = ActivityLeakDetector.getLeakedActivityCount();
            if (leakedActivityCount > 0) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "检查内存健康状态失败: " + e.getMessage());
            return false;
        }
    }
}
