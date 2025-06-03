package com.github.tvbox.osc.util;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 内存泄漏检查工具类
 * 用于检测和预防常见的内存泄漏问题
 */
public class MemoryLeakChecker {
    private static final String TAG = "MemoryLeakChecker";
    
    // 存储Activity的弱引用，用于检测是否被正确回收
    private static final List<WeakReference<Activity>> sActivityRefs = new ArrayList<>();
    
    // 检查Handler，使用弱引用避免自身造成内存泄漏
    private static final Handler sCheckHandler = new Handler(Looper.getMainLooper());
    
    /**
     * 注册Activity，用于内存泄漏检测
     * 在Activity的onCreate中调用
     */
    public static void registerActivity(Activity activity) {
        if (activity == null) return;
        
        synchronized (sActivityRefs) {
            sActivityRefs.add(new WeakReference<>(activity));
            Log.d(TAG, "注册Activity: " + activity.getClass().getSimpleName());
        }
    }
    
    /**
     * 检查Activity是否存在内存泄漏
     * 在Activity的onDestroy中调用
     */
    public static void checkActivityLeak(Activity activity) {
        if (activity == null) return;
        
        String activityName = activity.getClass().getSimpleName();
        Log.d(TAG, "开始检查Activity内存泄漏: " + activityName);
        
        // 延迟检查，给GC一些时间
        sCheckHandler.postDelayed(() -> {
            synchronized (sActivityRefs) {
                Iterator<WeakReference<Activity>> iterator = sActivityRefs.iterator();
                while (iterator.hasNext()) {
                    WeakReference<Activity> ref = iterator.next();
                    Activity refActivity = ref.get();
                    
                    if (refActivity == null) {
                        // Activity已被回收，移除引用
                        iterator.remove();
                    } else if (refActivity == activity) {
                        // Activity仍然存在，可能存在内存泄漏
                        Log.w(TAG, "检测到可能的内存泄漏: " + activityName + 
                              " 在onDestroy后仍未被GC回收");
                        
                        // 尝试强制GC
                        System.gc();
                        System.runFinalization();
                        
                        // 再次延迟检查
                        sCheckHandler.postDelayed(() -> {
                            if (ref.get() != null) {
                                Log.e(TAG, "确认内存泄漏: " + activityName + 
                                      " 在强制GC后仍未被回收");
                            } else {
                                Log.i(TAG, "内存泄漏已解决: " + activityName + 
                                      " 在强制GC后被成功回收");
                            }
                        }, 2000);
                        break;
                    }
                }
            }
        }, 3000); // 3秒后检查
    }
    
    /**
     * 检查Context是否安全
     * 避免使用已销毁的Activity Context
     */
    public static boolean isContextSafe(Context context) {
        if (context == null) {
            Log.w(TAG, "Context为null");
            return false;
        }
        
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (activity.isDestroyed()) {
                Log.w(TAG, "Activity已被销毁: " + activity.getClass().getSimpleName());
                return false;
            }
            if (activity.isFinishing()) {
                Log.w(TAG, "Activity正在结束: " + activity.getClass().getSimpleName());
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 获取安全的Context
     * 优先返回ApplicationContext
     */
    public static Context getSafeContext(Context context) {
        if (context == null) {
            return null;
        }
        
        try {
            Context appContext = context.getApplicationContext();
            return appContext != null ? appContext : context;
        } catch (Exception e) {
            Log.w(TAG, "获取ApplicationContext失败: " + e.getMessage());
            return context;
        }
    }
    
    /**
     * 清理所有检查数据
     * 在应用退出时调用
     */
    public static void cleanup() {
        synchronized (sActivityRefs) {
            sActivityRefs.clear();
        }
        
        if (sCheckHandler != null) {
            sCheckHandler.removeCallbacksAndMessages(null);
        }
        
        Log.i(TAG, "内存泄漏检查器已清理");
    }
    
    /**
     * 获取当前存活的Activity数量
     */
    public static int getAliveActivityCount() {
        synchronized (sActivityRefs) {
            int count = 0;
            Iterator<WeakReference<Activity>> iterator = sActivityRefs.iterator();
            while (iterator.hasNext()) {
                WeakReference<Activity> ref = iterator.next();
                if (ref.get() != null) {
                    count++;
                } else {
                    iterator.remove();
                }
            }
            return count;
        }
    }
    
    /**
     * 打印当前存活的Activity列表
     */
    public static void printAliveActivities() {
        synchronized (sActivityRefs) {
            Log.d(TAG, "当前存活Activity数量: " + getAliveActivityCount());
            for (WeakReference<Activity> ref : sActivityRefs) {
                Activity activity = ref.get();
                if (activity != null) {
                    Log.d(TAG, "存活Activity: " + activity.getClass().getSimpleName());
                }
            }
        }
    }
}
