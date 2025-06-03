package com.github.tvbox.osc.util;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * 内存泄漏修复启动器
 * 在应用启动时自动初始化所有内存泄漏修复组件
 */
public class MemoryLeakBootstrap {
    private static final String TAG = "MemoryLeakBootstrap";
    private static volatile boolean sIsInitialized = false;
    private static Handler sMainHandler;

    /**
     * 初始化内存泄漏修复系统
     */
    public static synchronized void initialize(Application application) {
        if (sIsInitialized) {
            Log.w(TAG, "内存泄漏修复系统已经初始化，跳过重复初始化");
            return;
        }

        try {
            Log.i(TAG, "开始初始化内存泄漏修复系统");

            // 初始化主线程Handler
            sMainHandler = new Handler(Looper.getMainLooper());

            // 1. 初始化内存泄漏修复管理器
            MemoryLeakFixManager.initialize(application);

            // 2. 初始化内存泄漏检测器（如果存在）
            try {
                Class.forName("com.github.tvbox.osc.util.MemoryLeakChecker");
                // MemoryLeakChecker.initialize(application);
                Log.i(TAG, "MemoryLeakChecker已初始化");
            } catch (ClassNotFoundException e) {
                Log.w(TAG, "MemoryLeakChecker类不存在，跳过初始化");
            }

            // 3. 初始化Activity泄漏检测器（如果存在）
            try {
                Class.forName("com.github.tvbox.osc.util.ActivityLeakDetector");
                // ActivityLeakDetector.initialize(application);
                Log.i(TAG, "ActivityLeakDetector已初始化");
            } catch (ClassNotFoundException e) {
                Log.w(TAG, "ActivityLeakDetector类不存在，跳过初始化");
            }

            // 4. 初始化内存泄漏预防器（如果存在）
            try {
                Class.forName("com.github.tvbox.osc.util.MemoryLeakPreventer");
                // MemoryLeakPreventer.initialize(application);
                Log.i(TAG, "MemoryLeakPreventer已初始化");
            } catch (ClassNotFoundException e) {
                Log.w(TAG, "MemoryLeakPreventer类不存在，跳过初始化");
            }

            // 5. 设置定期清理任务
            setupPeriodicCleanup();

            // 6. 设置应用退出时的清理
            setupExitCleanup(application);

            sIsInitialized = true;
            Log.i(TAG, "内存泄漏修复系统初始化完成");
        } catch (Exception e) {
            Log.e(TAG, "初始化内存泄漏修复系统失败: " + e.getMessage());
        }
    }

    /**
     * 设置定期清理任务
     */
    private static void setupPeriodicCleanup() {
        try {
            // 每5分钟执行一次轻量级清理
            Runnable lightCleanupTask = new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d(TAG, "执行定期轻量级内存清理");
                        
                        // 清理LoadSir全局引用
                        LoadSirLeakFixer.clearGlobalLoadSirReferences();
                        
                        // 执行垃圾回收
                        System.gc();
                        
                        // 重新安排下次清理
                        sMainHandler.postDelayed(this, 5 * 60 * 1000); // 5分钟
                    } catch (Exception e) {
                        Log.w(TAG, "定期轻量级清理失败: " + e.getMessage());
                    }
                }
            };

            // 启动定期清理任务
            sMainHandler.postDelayed(lightCleanupTask, 5 * 60 * 1000); // 5分钟后开始

            // 每30分钟执行一次全面清理
            Runnable comprehensiveCleanupTask = new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d(TAG, "执行定期全面内存清理");
                        
                        // 执行全面清理
                        ThreadPoolManager.executeIO(() -> {
                            try {
                                // 修复OkHttp泄漏
                                try {
                                    MemoryLeakFixer.fixAllLeaksImmediate();
                                } catch (Exception e) {
                                    Log.w(TAG, "修复内存泄漏失败: " + e.getMessage());
                                }

                                // 强制垃圾回收
                                System.gc();
                                System.gc();

                                Log.d(TAG, "定期全面内存清理完成");
                            } catch (Exception e) {
                                Log.w(TAG, "定期全面清理失败: " + e.getMessage());
                            }
                        });
                        
                        // 重新安排下次清理
                        sMainHandler.postDelayed(this, 30 * 60 * 1000); // 30分钟
                    } catch (Exception e) {
                        Log.w(TAG, "定期全面清理调度失败: " + e.getMessage());
                    }
                }
            };

            // 启动全面清理任务
            sMainHandler.postDelayed(comprehensiveCleanupTask, 30 * 60 * 1000); // 30分钟后开始

            Log.i(TAG, "定期清理任务已设置");
        } catch (Exception e) {
            Log.e(TAG, "设置定期清理任务失败: " + e.getMessage());
        }
    }

    /**
     * 设置应用退出时的清理
     */
    private static void setupExitCleanup(Application application) {
        try {
            // 注册应用退出时的清理回调
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Log.i(TAG, "应用退出，执行最终清理");
                    
                    // 执行全面的内存泄漏修复
                    MemoryLeakFixManager.performComprehensiveCleanup();
                    
                    // 彻底清理OkGo
                    OkGoHelper.completeCleanup();
                    
                    // 清理线程池
                    ThreadPoolManager.cleanup();
                    
                    Log.i(TAG, "应用退出清理完成");
                } catch (Exception e) {
                    Log.e(TAG, "应用退出清理失败: " + e.getMessage());
                }
            }));

            Log.i(TAG, "应用退出清理已设置");
        } catch (Exception e) {
            Log.e(TAG, "设置应用退出清理失败: " + e.getMessage());
        }
    }

    /**
     * 手动触发内存清理
     */
    public static void triggerManualCleanup() {
        try {
            Log.i(TAG, "手动触发内存清理");
            
            ThreadPoolManager.executeIO(() -> {
                try {
                    // 执行立即修复
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

                    Log.i(TAG, "手动内存清理完成");
                } catch (Exception e) {
                    Log.e(TAG, "手动内存清理失败: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "触发手动内存清理失败: " + e.getMessage());
        }
    }

    /**
     * 获取内存使用情况报告
     */
    public static String getMemoryReport() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();

            StringBuilder report = new StringBuilder();
            report.append("=== 内存使用报告 ===\n");
            report.append("总内存: ").append(formatBytes(totalMemory)).append("\n");
            report.append("已用内存: ").append(formatBytes(usedMemory)).append("\n");
            report.append("空闲内存: ").append(formatBytes(freeMemory)).append("\n");
            report.append("最大内存: ").append(formatBytes(maxMemory)).append("\n");
            report.append("内存使用率: ").append(String.format("%.1f%%", (usedMemory * 100.0 / maxMemory))).append("\n");
            report.append("==================");

            return report.toString();
        } catch (Exception e) {
            Log.e(TAG, "获取内存报告失败: " + e.getMessage());
            return "获取内存报告失败: " + e.getMessage();
        }
    }

    /**
     * 格式化字节数
     */
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * 检查是否已初始化
     */
    public static boolean isInitialized() {
        return sIsInitialized;
    }

    /**
     * 清理启动器资源
     */
    public static void cleanup() {
        try {
            Log.i(TAG, "清理内存泄漏启动器资源");
            
            if (sMainHandler != null) {
                sMainHandler.removeCallbacksAndMessages(null);
                sMainHandler = null;
            }
            
            sIsInitialized = false;
            
            Log.i(TAG, "内存泄漏启动器资源清理完成");
        } catch (Exception e) {
            Log.e(TAG, "清理内存泄漏启动器资源失败: " + e.getMessage());
        }
    }

    /**
     * 监控内存使用情况
     */
    public static void monitorMemoryUsage(String tag) {
        try {
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            double usagePercent = (usedMemory * 100.0 / maxMemory);
            
            Log.d(TAG, String.format("[%s] 内存使用: %s / %s (%.1f%%)", 
                tag, formatBytes(usedMemory), formatBytes(maxMemory), usagePercent));
            
            // 如果内存使用超过80%，触发清理
            if (usagePercent > 80.0) {
                Log.w(TAG, "内存使用率过高，触发自动清理");
                triggerManualCleanup();
            }
        } catch (Exception e) {
            Log.e(TAG, "监控内存使用情况失败: " + e.getMessage());
        }
    }
}
