package com.github.tvbox.osc.util;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存泄漏修复报告工具
 * 统计和报告内存泄漏修复的效果
 */
public class MemoryLeakReporter {
    private static final String TAG = "MemoryLeakReporter";
    
    // 统计计数器
    private static final AtomicInteger sLoadSirFixCount = new AtomicInteger(0);
    private static final AtomicInteger sViewPager2FixCount = new AtomicInteger(0);
    private static final AtomicInteger sOkHttpFixCount = new AtomicInteger(0);
    private static final AtomicInteger sFragmentFixCount = new AtomicInteger(0);
    private static final AtomicInteger sActivityFixCount = new AtomicInteger(0);
    private static final AtomicInteger sManualCleanupCount = new AtomicInteger(0);
    private static final AtomicInteger sAutoCleanupCount = new AtomicInteger(0);
    
    // 内存统计
    private static final AtomicLong sMemorySavedBytes = new AtomicLong(0);
    private static final AtomicLong sLastMemoryUsage = new AtomicLong(0);
    
    // 时间统计
    private static long sStartTime = System.currentTimeMillis();
    private static long sLastReportTime = System.currentTimeMillis();
    
    /**
     * 记录LoadSir修复
     */
    public static void recordLoadSirFix() {
        sLoadSirFixCount.incrementAndGet();
        Log.d(TAG, "记录LoadSir修复，总计: " + sLoadSirFixCount.get());
    }
    
    /**
     * 记录ViewPager2修复
     */
    public static void recordViewPager2Fix() {
        sViewPager2FixCount.incrementAndGet();
        Log.d(TAG, "记录ViewPager2修复，总计: " + sViewPager2FixCount.get());
    }
    
    /**
     * 记录OkHttp修复
     */
    public static void recordOkHttpFix() {
        sOkHttpFixCount.incrementAndGet();
        Log.d(TAG, "记录OkHttp修复，总计: " + sOkHttpFixCount.get());
    }
    
    /**
     * 记录Fragment修复
     */
    public static void recordFragmentFix() {
        sFragmentFixCount.incrementAndGet();
        Log.d(TAG, "记录Fragment修复，总计: " + sFragmentFixCount.get());
    }
    
    /**
     * 记录Activity修复
     */
    public static void recordActivityFix() {
        sActivityFixCount.incrementAndGet();
        Log.d(TAG, "记录Activity修复，总计: " + sActivityFixCount.get());
    }
    
    /**
     * 记录手动清理
     */
    public static void recordManualCleanup() {
        sManualCleanupCount.incrementAndGet();
        Log.d(TAG, "记录手动清理，总计: " + sManualCleanupCount.get());
    }
    
    /**
     * 记录自动清理
     */
    public static void recordAutoCleanup() {
        sAutoCleanupCount.incrementAndGet();
        Log.d(TAG, "记录自动清理，总计: " + sAutoCleanupCount.get());
    }
    
    /**
     * 记录内存节省
     */
    public static void recordMemorySaved(long bytes) {
        sMemorySavedBytes.addAndGet(bytes);
        Log.d(TAG, "记录内存节省: " + formatBytes(bytes) + "，总计: " + formatBytes(sMemorySavedBytes.get()));
    }
    
    /**
     * 更新内存使用情况
     */
    public static void updateMemoryUsage() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long currentMemory = runtime.totalMemory() - runtime.freeMemory();
            long lastMemory = sLastMemoryUsage.get();
            
            if (lastMemory > 0 && lastMemory > currentMemory) {
                // 内存使用减少了，记录节省的内存
                long saved = lastMemory - currentMemory;
                recordMemorySaved(saved);
            }
            
            sLastMemoryUsage.set(currentMemory);
        } catch (Exception e) {
            Log.e(TAG, "更新内存使用情况失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成详细报告
     */
    public static String generateDetailedReport() {
        try {
            StringBuilder report = new StringBuilder();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            
            report.append("========== 内存泄漏修复详细报告 ==========\n");
            report.append("报告生成时间: ").append(dateFormat.format(new Date())).append("\n");
            report.append("运行时长: ").append(formatDuration(System.currentTimeMillis() - sStartTime)).append("\n");
            report.append("\n");
            
            // 修复统计
            report.append("=== 修复统计 ===\n");
            report.append("LoadSir修复次数: ").append(sLoadSirFixCount.get()).append("\n");
            report.append("ViewPager2修复次数: ").append(sViewPager2FixCount.get()).append("\n");
            report.append("OkHttp修复次数: ").append(sOkHttpFixCount.get()).append("\n");
            report.append("Fragment修复次数: ").append(sFragmentFixCount.get()).append("\n");
            report.append("Activity修复次数: ").append(sActivityFixCount.get()).append("\n");
            report.append("手动清理次数: ").append(sManualCleanupCount.get()).append("\n");
            report.append("自动清理次数: ").append(sAutoCleanupCount.get()).append("\n");
            
            int totalFixes = sLoadSirFixCount.get() + sViewPager2FixCount.get() + 
                           sOkHttpFixCount.get() + sFragmentFixCount.get() + sActivityFixCount.get();
            report.append("总修复次数: ").append(totalFixes).append("\n");
            report.append("\n");
            
            // 内存统计
            report.append("=== 内存统计 ===\n");
            report.append("累计节省内存: ").append(formatBytes(sMemorySavedBytes.get())).append("\n");
            
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            report.append("当前内存使用: ").append(formatBytes(usedMemory)).append("\n");
            report.append("总分配内存: ").append(formatBytes(totalMemory)).append("\n");
            report.append("空闲内存: ").append(formatBytes(freeMemory)).append("\n");
            report.append("最大可用内存: ").append(formatBytes(maxMemory)).append("\n");
            report.append("内存使用率: ").append(String.format("%.1f%%", (usedMemory * 100.0 / maxMemory))).append("\n");
            report.append("\n");
            
            // 效率统计
            report.append("=== 效率统计 ===\n");
            long runningHours = (System.currentTimeMillis() - sStartTime) / (1000 * 60 * 60);
            if (runningHours > 0) {
                report.append("平均每小时修复次数: ").append(String.format("%.1f", totalFixes / (double) runningHours)).append("\n");
                report.append("平均每小时节省内存: ").append(formatBytes(sMemorySavedBytes.get() / runningHours)).append("\n");
            }
            report.append("\n");
            
            // 建议
            report.append("=== 建议 ===\n");
            if (totalFixes > 50) {
                report.append("⚠️  修复次数较多，建议检查代码中的内存泄漏源头\n");
            } else if (totalFixes > 20) {
                report.append("ℹ️  修复次数适中，内存管理基本正常\n");
            } else {
                report.append("✅ 修复次数较少，内存管理良好\n");
            }
            
            double memoryUsagePercent = (usedMemory * 100.0 / maxMemory);
            if (memoryUsagePercent > 80) {
                report.append("⚠️  内存使用率较高，建议优化内存使用\n");
            } else if (memoryUsagePercent > 60) {
                report.append("ℹ️  内存使用率适中\n");
            } else {
                report.append("✅ 内存使用率良好\n");
            }
            
            report.append("==========================================");
            
            return report.toString();
        } catch (Exception e) {
            Log.e(TAG, "生成详细报告失败: " + e.getMessage());
            return "生成报告失败: " + e.getMessage();
        }
    }
    
    /**
     * 生成简要报告
     */
    public static String generateSummaryReport() {
        try {
            int totalFixes = sLoadSirFixCount.get() + sViewPager2FixCount.get() + 
                           sOkHttpFixCount.get() + sFragmentFixCount.get() + sActivityFixCount.get();
            
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            double memoryUsagePercent = (usedMemory * 100.0 / maxMemory);
            
            return String.format("内存修复: %d次 | 节省: %s | 使用率: %.1f%%", 
                totalFixes, formatBytes(sMemorySavedBytes.get()), memoryUsagePercent);
        } catch (Exception e) {
            Log.e(TAG, "生成简要报告失败: " + e.getMessage());
            return "报告生成失败";
        }
    }
    
    /**
     * 重置统计
     */
    public static void resetStatistics() {
        sLoadSirFixCount.set(0);
        sViewPager2FixCount.set(0);
        sOkHttpFixCount.set(0);
        sFragmentFixCount.set(0);
        sActivityFixCount.set(0);
        sManualCleanupCount.set(0);
        sAutoCleanupCount.set(0);
        sMemorySavedBytes.set(0);
        sLastMemoryUsage.set(0);
        sStartTime = System.currentTimeMillis();
        sLastReportTime = System.currentTimeMillis();
        
        Log.i(TAG, "统计数据已重置");
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
     * 格式化时长
     */
    private static String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%d天%d小时%d分钟", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%d小时%d分钟", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%d分钟%d秒", minutes, seconds % 60);
        } else {
            return String.format("%d秒", seconds);
        }
    }
}
