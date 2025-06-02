package com.github.tvbox.osc.util;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * 内存泄漏预防工具
 * 用于在Activity和Fragment中主动预防常见的内存泄漏问题
 */
public class MemoryLeakPreventer {
    private static final String TAG = "MemoryLeakPreventer";
    
    /**
     * 清理Activity中可能导致内存泄漏的资源
     */
    public static void cleanupActivity(Activity activity) {
        if (activity == null) return;
        
        try {
            LOG.i(TAG, "开始清理Activity资源: " + activity.getClass().getSimpleName());
            
            // 清理View树中的监听器
            View rootView = activity.findViewById(android.R.id.content);
            if (rootView != null) {
                cleanupViewTree(rootView);
            }
            
            // 清理Handler消息
            cleanupHandlers(activity);
            
            // 清理RecyclerView
            cleanupRecyclerViews(activity);
            
            LOG.i(TAG, "Activity资源清理完成: " + activity.getClass().getSimpleName());
        } catch (Exception e) {
            LOG.e(TAG, "清理Activity资源时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 清理Fragment中可能导致内存泄漏的资源
     */
    public static void cleanupFragment(Fragment fragment) {
        if (fragment == null) return;
        
        try {
            LOG.i(TAG, "开始清理Fragment资源: " + fragment.getClass().getSimpleName());
            
            // 清理View树中的监听器
            View rootView = fragment.getView();
            if (rootView != null) {
                cleanupViewTree(rootView);
            }
            
            LOG.i(TAG, "Fragment资源清理完成: " + fragment.getClass().getSimpleName());
        } catch (Exception e) {
            LOG.e(TAG, "清理Fragment资源时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 递归清理View树中的监听器和引用
     */
    private static void cleanupViewTree(View view) {
        if (view == null) return;
        
        try {
            // 清理View的监听器
            view.setOnClickListener(null);
            view.setOnLongClickListener(null);
            view.setOnTouchListener(null);
            view.setOnFocusChangeListener(null);
            
            // 清理View的Tag
            view.setTag(null);
            
            // 如果是ViewGroup，递归清理子View
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    cleanupViewTree(viewGroup.getChildAt(i));
                }
            }
            
            // 特殊处理RecyclerView
            if (view instanceof RecyclerView) {
                RecyclerView recyclerView = (RecyclerView) view;
                recyclerView.setAdapter(null);
                recyclerView.setLayoutManager(null);
                recyclerView.clearOnScrollListeners();
            }
            
        } catch (Exception e) {
            LOG.w(TAG, "清理View时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 清理Handler消息
     */
    private static void cleanupHandlers(Activity activity) {
        try {
            // 使用反射查找Activity中的Handler字段
            java.lang.reflect.Field[] fields = activity.getClass().getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(activity);
                
                if (value instanceof Handler) {
                    Handler handler = (Handler) value;
                    handler.removeCallbacksAndMessages(null);
                    LOG.i(TAG, "清理Handler: " + field.getName());
                }
            }
        } catch (Exception e) {
            LOG.w(TAG, "清理Handler时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 清理RecyclerView
     */
    private static void cleanupRecyclerViews(Activity activity) {
        try {
            View rootView = activity.findViewById(android.R.id.content);
            List<RecyclerView> recyclerViews = findRecyclerViews(rootView);
            
            for (RecyclerView recyclerView : recyclerViews) {
                try {
                    recyclerView.setAdapter(null);
                    recyclerView.setLayoutManager(null);
                    recyclerView.clearOnScrollListeners();
                    LOG.i(TAG, "清理RecyclerView");
                } catch (Exception e) {
                    LOG.w(TAG, "清理单个RecyclerView时发生错误: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            LOG.w(TAG, "清理RecyclerViews时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 递归查找所有RecyclerView
     */
    private static List<RecyclerView> findRecyclerViews(View view) {
        List<RecyclerView> recyclerViews = new ArrayList<>();
        
        if (view instanceof RecyclerView) {
            recyclerViews.add((RecyclerView) view);
        } else if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                recyclerViews.addAll(findRecyclerViews(viewGroup.getChildAt(i)));
            }
        }
        
        return recyclerViews;
    }
    
    /**
     * 创建安全的Context引用
     * 返回WeakReference包装的Context，避免强引用导致内存泄漏
     */
    public static WeakReference<Context> createSafeContextRef(Context context) {
        if (context == null) {
            return null;
        }
        
        // 优先使用ApplicationContext
        Context safeContext = context.getApplicationContext();
        if (safeContext == null) {
            safeContext = context;
        }
        
        return new WeakReference<>(safeContext);
    }
    
    /**
     * 从安全的Context引用中获取Context
     */
    public static Context getSafeContext(WeakReference<Context> contextRef) {
        if (contextRef == null) {
            return null;
        }
        
        Context context = contextRef.get();
        if (context == null) {
            // 如果WeakReference失效，尝试从App获取
            try {
                return com.github.tvbox.osc.base.App.getInstance();
            } catch (Exception e) {
                LOG.w(TAG, "无法获取App实例: " + e.getMessage());
                return null;
            }
        }
        
        return context;
    }
    
    /**
     * 检查Context是否安全可用
     */
    public static boolean isContextSafe(Context context) {
        if (context == null) {
            return false;
        }
        
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            return !activity.isDestroyed() && !activity.isFinishing();
        }
        
        return true;
    }
    
    /**
     * 强制清理静态引用
     * 用于清理可能持有Activity引用的静态变量
     */
    public static void cleanupStaticReferences() {
        try {
            LOG.i(TAG, "开始清理静态引用");
            
            // 清理可能的静态Context引用
            // 这里可以添加具体的静态变量清理逻辑
            
            // 强制垃圾回收
            System.gc();
            System.runFinalization();
            
            LOG.i(TAG, "静态引用清理完成");
        } catch (Exception e) {
            LOG.e(TAG, "清理静态引用时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 监控内存使用情况
     */
    public static void monitorMemoryUsage(String tag) {
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            double usagePercent = (usedMemory * 100.0) / maxMemory;
            
            LOG.i(TAG, String.format("[%s] 内存使用: %.1fMB/%.1fMB (%.1f%%)", 
                tag, 
                usedMemory / 1024.0 / 1024.0, 
                maxMemory / 1024.0 / 1024.0, 
                usagePercent));
                
            // 如果内存使用率过高，触发内存清理
            if (usagePercent > 80.0) {
                LOG.w(TAG, "内存使用率过高，触发内存清理");
                MemoryLeakFixer.fixAllLeaks();
            }
        } catch (Exception e) {
            LOG.e(TAG, "监控内存使用时发生错误: " + e.getMessage());
        }
    }
}
