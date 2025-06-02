package com.github.tvbox.osc.util;

import android.content.Context;
import android.util.Log;

/**
 * Spider异常处理器
 * 用于处理外部JAR包中spider初始化时的异常
 */
public class SpiderExceptionHandler {
    private static final String TAG = "SpiderExceptionHandler";
    
    /**
     * 安全执行spider相关操作
     * @param operation 要执行的操作
     * @param errorMessage 错误时显示的消息
     * @return 操作是否成功
     */
    public static boolean safeExecute(Runnable operation, String errorMessage) {
        try {
            operation.run();
            return true;
        } catch (NullPointerException e) {
            Log.e(TAG, "Spider操作发生NullPointerException: " + e.getMessage());
            e.printStackTrace();
            if (e.getMessage() != null && e.getMessage().contains("getSharedPreferences")) {
                Log.e(TAG, "检测到spider尝试访问null Activity的SharedPreferences");
                MD3ToastUtils.showToast("数据源初始化失败，请检查网络连接");
            } else {
                MD3ToastUtils.showToast(errorMessage != null ? errorMessage : "操作失败");
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Spider操作发生异常: " + e.getMessage());
            e.printStackTrace();
            MD3ToastUtils.showToast(errorMessage != null ? errorMessage : "操作失败");
            return false;
        }
    }
    
    /**
     * 安全执行spider相关操作（带返回值）
     * @param operation 要执行的操作
     * @param defaultValue 异常时的默认返回值
     * @param errorMessage 错误时显示的消息
     * @return 操作结果或默认值
     */
    public static <T> T safeExecute(java.util.concurrent.Callable<T> operation, T defaultValue, String errorMessage) {
        try {
            return operation.call();
        } catch (NullPointerException e) {
            Log.e(TAG, "Spider操作发生NullPointerException: " + e.getMessage());
            e.printStackTrace();
            if (e.getMessage() != null && e.getMessage().contains("getSharedPreferences")) {
                Log.e(TAG, "检测到spider尝试访问null Activity的SharedPreferences");
                MD3ToastUtils.showToast("数据源初始化失败，请检查网络连接");
            } else {
                MD3ToastUtils.showToast(errorMessage != null ? errorMessage : "操作失败");
            }
            return defaultValue;
        } catch (Exception e) {
            Log.e(TAG, "Spider操作发生异常: " + e.getMessage());
            e.printStackTrace();
            MD3ToastUtils.showToast(errorMessage != null ? errorMessage : "操作失败");
            return defaultValue;
        }
    }
    
    /**
     * 检查Context是否安全可用
     * @param context 要检查的Context
     * @return Context是否安全
     */
    public static boolean isContextSafe(Context context) {
        if (context == null) {
            Log.w(TAG, "Context为null");
            return false;
        }
        
        try {
            // 尝试访问Context的基本方法来验证其有效性
            context.getApplicationContext();
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Context无效: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取安全的Context
     * 优先返回ApplicationContext，避免Activity被销毁导致的问题
     * @param context 原始Context
     * @return 安全的Context
     */
    public static Context getSafeContext(Context context) {
        if (context == null) {
            try {
                // 尝试从App实例获取Context
                Class<?> appClass = Class.forName("com.github.tvbox.osc.base.App");
                Object appInstance = appClass.getMethod("getInstance").invoke(null);
                return (Context) appInstance;
            } catch (Exception e) {
                Log.e(TAG, "无法获取App实例: " + e.getMessage());
                return null;
            }
        }
        
        try {
            Context appContext = context.getApplicationContext();
            return appContext != null ? appContext : context;
        } catch (Exception e) {
            Log.w(TAG, "无法获取ApplicationContext，使用原始Context: " + e.getMessage());
            return context;
        }
    }
    
    /**
     * 记录spider异常信息
     * @param operation 操作名称
     * @param exception 异常信息
     */
    public static void logSpiderException(String operation, Throwable exception) {
        Log.e(TAG, "Spider操作[" + operation + "]发生异常: " + exception.getMessage());
        exception.printStackTrace();
        
        // 特殊处理常见的spider异常
        if (exception instanceof NullPointerException) {
            if (exception.getMessage() != null && exception.getMessage().contains("getSharedPreferences")) {
                Log.e(TAG, "这是一个已知问题：外部spider尝试访问已销毁Activity的SharedPreferences");
                Log.e(TAG, "建议：确保spider使用ApplicationContext而不是Activity Context");
            }
        }
    }
}
