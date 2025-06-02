package com.github.tvbox.osc.util;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.ui.activity.MainActivity;
import com.orhanobut.hawk.Hawk;

/**
 * 应用重启功能测试工具
 * 用于测试和验证应用重启功能是否正常工作
 */
public class AppRestartTester {
    private static final String TAG = "AppRestartTester";
    private static final String KEY_TEST_BACKGROUND_TIME = "test_background_time";
    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());
    
    /**
     * 模拟应用长时间后台运行
     * @param hours 模拟的后台小时数
     */
    public static void simulateLongBackground(int hours) {
        try {
            LOG.i(TAG, "模拟应用后台运行 " + hours + " 小时");
            
            // 计算模拟的后台时间
            long simulatedBackgroundTime = System.currentTimeMillis() - (hours * 60 * 60 * 1000L);
            
            // 保存模拟的后台时间
            Hawk.put(KEY_TEST_BACKGROUND_TIME, simulatedBackgroundTime);
            
            LOG.i(TAG, "模拟后台时间已设置: " + new java.util.Date(simulatedBackgroundTime));
            
        } catch (Exception e) {
            LOG.e(TAG, "模拟长时间后台运行失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试应用重启逻辑
     */
    public static void testRestartLogic() {
        try {
            LOG.i(TAG, "开始测试应用重启逻辑");
            
            // 获取当前Activity
            Activity currentActivity = AppLifecycleManager.getInstance().getCurrentActivity();
            if (currentActivity == null) {
                LOG.w(TAG, "无法获取当前Activity，测试终止");
                return;
            }
            
            // 检查是否有模拟的后台时间
            if (Hawk.contains(KEY_TEST_BACKGROUND_TIME)) {
                long testBackgroundTime = Hawk.get(KEY_TEST_BACKGROUND_TIME, 0L);
                long backgroundDuration = System.currentTimeMillis() - testBackgroundTime;
                
                LOG.i(TAG, "检测到测试后台时间，后台时长: " + (backgroundDuration / 1000 / 60 / 60) + " 小时");
                
                // 检查是否应该重启
                if (backgroundDuration >= AppRestartConfig.getRestartThresholdMillis()) {
                    LOG.i(TAG, "满足重启条件，执行测试重启");
                    performTestRestart(currentActivity);
                } else {
                    LOG.i(TAG, "不满足重启条件，无需重启");
                }
                
                // 清除测试数据
                Hawk.delete(KEY_TEST_BACKGROUND_TIME);
            } else {
                LOG.i(TAG, "没有检测到测试后台时间");
            }
            
        } catch (Exception e) {
            LOG.e(TAG, "测试应用重启逻辑失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行测试重启
     */
    private static void performTestRestart(Activity currentActivity) {
        try {
            LOG.i(TAG, "执行测试重启");
            
            // 显示测试提示
            MD3ToastUtils.showToast("【测试模式】应用重启功能测试中...");
            
            // 延迟执行重启
            sMainHandler.postDelayed(() -> {
                try {
                    // 创建重启Intent
                    Intent restartIntent = new Intent(currentActivity, MainActivity.class);
                    restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    restartIntent.putExtra("app_restarted", true);
                    restartIntent.putExtra("test_restart", true);
                    
                    // 启动新的MainActivity
                    currentActivity.startActivity(restartIntent);
                    
                    // 结束当前Activity
                    currentActivity.finish();
                    
                    LOG.i(TAG, "测试重启完成");
                    
                } catch (Exception e) {
                    LOG.e(TAG, "执行测试重启失败: " + e.getMessage());
                }
            }, 2000);
            
        } catch (Exception e) {
            LOG.e(TAG, "执行测试重启失败: " + e.getMessage());
        }
    }
    
    /**
     * 快速测试重启功能（模拟3小时后台）
     */
    public static void quickTestRestart() {
        LOG.i(TAG, "快速测试重启功能");
        simulateLongBackground(3);
        
        // 延迟测试，模拟应用回到前台
        sMainHandler.postDelayed(() -> testRestartLogic(), 1000);
    }
    
    /**
     * 测试重启配置
     */
    public static void testRestartConfig() {
        try {
            LOG.i(TAG, "开始测试重启配置");
            
            // 打印当前配置
            LOG.i(TAG, "当前配置:");
            LOG.i(TAG, "- 自动重启: " + AppRestartConfig.isAutoRestartEnabled());
            LOG.i(TAG, "- 重启阈值: " + AppRestartConfig.getRestartThresholdHours() + "小时");
            LOG.i(TAG, "- 显示提示: " + AppRestartConfig.isShowRestartToast());
            LOG.i(TAG, "- 重启清理: " + AppRestartConfig.isCleanupOnRestart());
            
            // 测试配置修改
            boolean originalEnabled = AppRestartConfig.isAutoRestartEnabled();
            int originalThreshold = AppRestartConfig.getRestartThresholdHours();
            
            // 修改配置
            AppRestartConfig.setAutoRestartEnabled(!originalEnabled);
            AppRestartConfig.setRestartThresholdHours(originalThreshold == 3 ? 6 : 3);
            
            LOG.i(TAG, "配置修改后:");
            LOG.i(TAG, "- 自动重启: " + AppRestartConfig.isAutoRestartEnabled());
            LOG.i(TAG, "- 重启阈值: " + AppRestartConfig.getRestartThresholdHours() + "小时");
            
            // 恢复原始配置
            AppRestartConfig.setAutoRestartEnabled(originalEnabled);
            AppRestartConfig.setRestartThresholdHours(originalThreshold);
            
            LOG.i(TAG, "配置已恢复原始值");
            LOG.i(TAG, "重启配置测试完成");
            
        } catch (Exception e) {
            LOG.e(TAG, "测试重启配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试生命周期管理器
     */
    public static void testLifecycleManager() {
        try {
            LOG.i(TAG, "开始测试生命周期管理器");
            
            AppLifecycleManager manager = AppLifecycleManager.getInstance();
            
            LOG.i(TAG, "应用是否在后台: " + manager.isAppInBackground());
            LOG.i(TAG, "后台时长: " + (manager.getBackgroundDuration() / 1000) + "秒");
            
            Activity currentActivity = manager.getCurrentActivity();
            if (currentActivity != null) {
                LOG.i(TAG, "当前Activity: " + currentActivity.getClass().getSimpleName());
            } else {
                LOG.i(TAG, "当前Activity: null");
            }
            
            LOG.i(TAG, "生命周期管理器测试完成");
            
        } catch (Exception e) {
            LOG.e(TAG, "测试生命周期管理器失败: " + e.getMessage());
        }
    }
    
    /**
     * 运行完整的重启功能测试套件
     */
    public static void runFullTestSuite() {
        LOG.i(TAG, "开始运行完整的重启功能测试套件");
        
        // 测试配置
        testRestartConfig();
        
        // 延迟测试生命周期管理器
        sMainHandler.postDelayed(() -> testLifecycleManager(), 2000);
        
        // 延迟测试重启逻辑
        sMainHandler.postDelayed(() -> {
            LOG.i(TAG, "是否执行快速重启测试？(将模拟3小时后台并触发重启)");
            MD3ToastUtils.showToast("重启功能测试套件完成，查看日志了解详情");
        }, 4000);
    }
    
    /**
     * 清理测试数据
     */
    public static void cleanupTestData() {
        try {
            Hawk.delete(KEY_TEST_BACKGROUND_TIME);
            LOG.i(TAG, "测试数据已清理");
        } catch (Exception e) {
            LOG.e(TAG, "清理测试数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取测试状态信息
     */
    public static String getTestStatusInfo() {
        try {
            StringBuilder info = new StringBuilder();
            info.append("=== 应用重启功能测试状态 ===\n");
            info.append("配置状态:\n");
            info.append("- 自动重启: ").append(AppRestartConfig.isAutoRestartEnabled() ? "启用" : "禁用").append("\n");
            info.append("- 重启阈值: ").append(AppRestartConfig.getRestartThresholdHours()).append("小时\n");
            info.append("- 显示提示: ").append(AppRestartConfig.isShowRestartToast() ? "是" : "否").append("\n");
            info.append("- 重启清理: ").append(AppRestartConfig.isCleanupOnRestart() ? "是" : "否").append("\n");
            
            info.append("\n运行状态:\n");
            AppLifecycleManager manager = AppLifecycleManager.getInstance();
            info.append("- 应用在后台: ").append(manager.isAppInBackground() ? "是" : "否").append("\n");
            info.append("- 后台时长: ").append(manager.getBackgroundDuration() / 1000).append("秒\n");
            
            Activity currentActivity = manager.getCurrentActivity();
            info.append("- 当前Activity: ").append(currentActivity != null ? currentActivity.getClass().getSimpleName() : "null").append("\n");
            
            info.append("\n测试数据:\n");
            info.append("- 有测试后台时间: ").append(Hawk.contains(KEY_TEST_BACKGROUND_TIME) ? "是" : "否").append("\n");
            
            info.append("========================");
            
            return info.toString();
        } catch (Exception e) {
            return "获取测试状态信息失败: " + e.getMessage();
        }
    }
}
