package com.github.tvbox.osc.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.github.tvbox.osc.base.App;

/**
 * 内存泄漏测试工具
 * 用于测试和验证内存泄漏修复是否有效
 */
public class MemoryLeakTester {
    private static final String TAG = "MemoryLeakTester";
    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());

    /**
     * 测试OkGoHelper内存泄漏修复
     */
    public static void testOkGoHelperLeakFix() {
        LOG.i(TAG, "开始测试OkGoHelper内存泄漏修复");

        ThreadPoolManager.executeIO(() -> {
            try {
                // 记录修复前的内存状态
                Runtime runtime = Runtime.getRuntime();
                long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
                LOG.i(TAG, "修复前内存使用: " + (beforeMemory / 1024 / 1024) + "MB");

                // 执行修复
                MemoryLeakFixer.fixOkGoHelperLeaks();

                // 等待修复完成
                Thread.sleep(3000);

                // 记录修复后的内存状态
                long afterMemory = runtime.totalMemory() - runtime.freeMemory();
                LOG.i(TAG, "修复后内存使用: " + (afterMemory / 1024 / 1024) + "MB");

                long memoryDiff = beforeMemory - afterMemory;
                if (memoryDiff > 0) {
                    LOG.i(TAG, "OkGoHelper内存泄漏修复成功，释放了 " + (memoryDiff / 1024 / 1024) + "MB 内存");
                } else {
                    LOG.w(TAG, "OkGoHelper内存泄漏修复效果不明显");
                }

            } catch (Exception e) {
                LOG.e(TAG, "测试OkGoHelper内存泄漏修复失败: " + e.getMessage());
            }
        });
    }

    /**
     * 测试Glide内存泄漏修复
     */
    public static void testGlideLeakFix() {
        LOG.i(TAG, "开始测试Glide内存泄漏修复");

        ThreadPoolManager.executeIO(() -> {
            try {
                // 记录修复前的内存状态
                Runtime runtime = Runtime.getRuntime();
                long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
                LOG.i(TAG, "修复前内存使用: " + (beforeMemory / 1024 / 1024) + "MB");

                // 执行修复
                MemoryLeakFixer.fixGlideLeaks();

                // 等待修复完成
                Thread.sleep(3000);

                // 记录修复后的内存状态
                long afterMemory = runtime.totalMemory() - runtime.freeMemory();
                LOG.i(TAG, "修复后内存使用: " + (afterMemory / 1024 / 1024) + "MB");

                long memoryDiff = beforeMemory - afterMemory;
                if (memoryDiff > 0) {
                    LOG.i(TAG, "Glide内存泄漏修复成功，释放了 " + (memoryDiff / 1024 / 1024) + "MB 内存");
                } else {
                    LOG.w(TAG, "Glide内存泄漏修复效果不明显");
                }

            } catch (Exception e) {
                LOG.e(TAG, "测试Glide内存泄漏修复失败: " + e.getMessage());
            }
        });
    }

    /**
     * 测试全面内存泄漏修复
     */
    public static void testAllLeaksFix() {
        LOG.i(TAG, "开始测试全面内存泄漏修复");

        ThreadPoolManager.executeIO(() -> {
            try {
                // 记录修复前的内存状态
                Runtime runtime = Runtime.getRuntime();
                long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
                LOG.i(TAG, "修复前内存使用: " + (beforeMemory / 1024 / 1024) + "MB");

                // 执行全面修复
                MemoryLeakFixer.fixAllLeaks();

                // 等待修复完成
                Thread.sleep(8000);

                // 记录修复后的内存状态
                long afterMemory = runtime.totalMemory() - runtime.freeMemory();
                LOG.i(TAG, "修复后内存使用: " + (afterMemory / 1024 / 1024) + "MB");

                long memoryDiff = beforeMemory - afterMemory;
                if (memoryDiff > 0) {
                    LOG.i(TAG, "全面内存泄漏修复成功，释放了 " + (memoryDiff / 1024 / 1024) + "MB 内存");
                } else {
                    LOG.w(TAG, "全面内存泄漏修复效果不明显");
                }

            } catch (Exception e) {
                LOG.e(TAG, "测试全面内存泄漏修复失败: " + e.getMessage());
            }
        });
    }

    /**
     * 测试ExoPlayer内存泄漏修复
     */
    public static void testExoPlayerLeakFix() {
        LOG.i(TAG, "开始测试ExoPlayer内存泄漏修复");

        ThreadPoolManager.executeIO(() -> {
            try {
                // 记录修复前的内存状态
                Runtime runtime = Runtime.getRuntime();
                long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
                LOG.i(TAG, "修复前内存使用: " + (beforeMemory / 1024 / 1024) + "MB");

                // 执行修复
                MemoryLeakFixer.fixExoPlayerLeaks();

                // 等待修复完成
                Thread.sleep(3000);

                // 记录修复后的内存状态
                long afterMemory = runtime.totalMemory() - runtime.freeMemory();
                LOG.i(TAG, "修复后内存使用: " + (afterMemory / 1024 / 1024) + "MB");

                long memoryDiff = beforeMemory - afterMemory;
                if (memoryDiff > 0) {
                    LOG.i(TAG, "ExoPlayer内存泄漏修复成功，释放了 " + (memoryDiff / 1024 / 1024) + "MB 内存");
                } else {
                    LOG.w(TAG, "ExoPlayer内存泄漏修复效果不明显");
                }

            } catch (Exception e) {
                LOG.e(TAG, "测试ExoPlayer内存泄漏修复失败: " + e.getMessage());
            }
        });
    }

    /**
     * 获取当前内存使用情况
     */
    public static void printMemoryInfo() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();

            LOG.i(TAG, "=== 内存使用情况 ===");
            LOG.i(TAG, "已使用内存: " + (usedMemory / 1024 / 1024) + "MB");
            LOG.i(TAG, "总分配内存: " + (totalMemory / 1024 / 1024) + "MB");
            LOG.i(TAG, "空闲内存: " + (freeMemory / 1024 / 1024) + "MB");
            LOG.i(TAG, "最大可用内存: " + (maxMemory / 1024 / 1024) + "MB");
            LOG.i(TAG, "内存使用率: " + String.format("%.1f%%", (usedMemory * 100.0 / maxMemory)));
            LOG.i(TAG, "==================");

        } catch (Exception e) {
            LOG.e(TAG, "获取内存信息失败: " + e.getMessage());
        }
    }

    /**
     * 运行完整的内存泄漏测试套件
     */
    public static void runFullTest() {
        LOG.i(TAG, "开始运行完整的内存泄漏测试套件");

        // 打印初始内存状态
        printMemoryInfo();

        // 延迟执行各项测试
        sMainHandler.postDelayed(() -> testOkGoHelperLeakFix(), 1000);
        sMainHandler.postDelayed(() -> testGlideLeakFix(), 5000);
        sMainHandler.postDelayed(() -> testExoPlayerLeakFix(), 9000);
        sMainHandler.postDelayed(() -> testAllLeaksFix(), 13000);

        // 最后打印内存状态
        sMainHandler.postDelayed(() -> {
            LOG.i(TAG, "内存泄漏测试套件完成");
            printMemoryInfo();
        }, 25000);
    }
}
