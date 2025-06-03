package com.github.tvbox.osc.util;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.kingja.loadsir.core.LoadService;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * LoadSir内存泄漏修复工具
 * 专门用于修复LoadSir库相关的内存泄漏问题
 */
public class LoadSirLeakFixer {
    private static final String TAG = "LoadSirLeakFixer";
    
    /**
     * 修复LoadService相关的内存泄漏
     */
    public static void fixLoadServiceLeak(LoadService<?> loadService) {
        if (loadService == null) {
            return;
        }

        try {
            Log.i(TAG, "开始修复LoadService内存泄漏");

            // 1. 清理LoadService内部的所有引用
            clearLoadServiceReferences(loadService);

            // 2. 清理LoadLayout相关引用
            clearLoadLayoutReferences(loadService);

            // 3. 清理Callback相关引用
            clearCallbackReferences(loadService);

            // 4. 记录修复统计
            MemoryLeakReporter.recordLoadSirFix();

            Log.i(TAG, "LoadService内存泄漏修复完成");
        } catch (Exception e) {
            Log.e(TAG, "修复LoadService内存泄漏失败: " + e.getMessage());
        }
    }
    
    /**
     * 清理LoadService内部引用
     */
    private static void clearLoadServiceReferences(LoadService<?> loadService) {
        try {
            Class<?> loadServiceClass = loadService.getClass();
            Field[] fields = loadServiceClass.getDeclaredFields();
            
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(loadService);
                
                if (value != null) {
                    String fieldName = field.getName();
                    String className = value.getClass().getSimpleName();
                    
                    // 清理View相关引用
                    if (value instanceof View || value instanceof ViewGroup) {
                        field.set(loadService, null);
                        Log.d(TAG, "清理LoadService View引用: " + fieldName);
                    }
                    // 清理LoadLayout引用
                    else if (className.contains("LoadLayout") || className.contains("Layout")) {
                        clearSpecificLoadLayout(value);
                        field.set(loadService, null);
                        Log.d(TAG, "清理LoadService LoadLayout引用: " + fieldName);
                    }
                    // 清理Callback引用
                    else if (className.contains("Callback") || fieldName.contains("callback")) {
                        field.set(loadService, null);
                        Log.d(TAG, "清理LoadService Callback引用: " + fieldName);
                    }
                    // 清理Context引用
                    else if (value instanceof android.content.Context) {
                        field.set(loadService, null);
                        Log.d(TAG, "清理LoadService Context引用: " + fieldName);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "清理LoadService内部引用失败: " + e.getMessage());
        }
    }
    
    /**
     * 清理LoadLayout相关引用
     */
    private static void clearLoadLayoutReferences(LoadService<?> loadService) {
        try {
            // 使用反射查找LoadLayout相关字段
            Class<?> loadServiceClass = loadService.getClass();
            
            // 查找可能包含LoadLayout的字段
            Field[] fields = loadServiceClass.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(loadService);
                
                if (value != null) {
                    String className = value.getClass().getName();
                    
                    // 检查是否是LoadLayout或相关类
                    if (className.contains("LoadLayout") || 
                        className.contains("com.kingja.loadsir") ||
                        value.getClass().getSimpleName().contains("Layout")) {
                        
                        clearSpecificLoadLayout(value);
                        field.set(loadService, null);
                        Log.d(TAG, "清理LoadLayout引用: " + field.getName());
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "清理LoadLayout引用失败: " + e.getMessage());
        }
    }
    
    /**
     * 清理特定LoadLayout对象的内部引用
     */
    private static void clearSpecificLoadLayout(Object loadLayout) {
        try {
            if (loadLayout == null) return;

            Class<?> loadLayoutClass = loadLayout.getClass();
            Field[] fields = loadLayoutClass.getDeclaredFields();

            for (Field field : fields) {
                field.setAccessible(true);
                String fieldName = field.getName();
                Object value = field.get(loadLayout);

                // 清理mParent引用 - 这是主要的泄漏源
                if ("mParent".equals(fieldName)) {
                    field.set(loadLayout, null);
                    Log.d(TAG, "清理LoadLayout.mParent");
                }
                // 清理onReload引用 - 这是另一个主要的泄漏源
                else if ("onReload".equals(fieldName) || fieldName.contains("onReload")) {
                    field.set(loadLayout, null);
                    Log.d(TAG, "清理LoadLayout.onReload");
                }
                // 清理其他可能的引用
                else if (fieldName.contains("reload") || fieldName.contains("Reload")) {
                    field.set(loadLayout, null);
                    Log.d(TAG, "清理LoadLayout reload相关引用: " + fieldName);
                }
                // 清理监听器引用
                else if (fieldName.contains("Listener") || fieldName.contains("listener")) {
                    field.set(loadLayout, null);
                    Log.d(TAG, "清理LoadLayout监听器: " + fieldName);
                }
                // 清理Callback引用
                else if (fieldName.contains("Callback") || fieldName.contains("callback")) {
                    field.set(loadLayout, null);
                    Log.d(TAG, "清理LoadLayout回调: " + fieldName);
                }
                // 清理View引用
                else if (value instanceof View || value instanceof ViewGroup) {
                    field.set(loadLayout, null);
                    Log.d(TAG, "清理LoadLayout View引用: " + fieldName);
                }
                // 清理Context引用
                else if (value instanceof android.content.Context) {
                    field.set(loadLayout, null);
                    Log.d(TAG, "清理LoadLayout Context引用: " + fieldName);
                }
                // 清理Activity引用
                else if (value instanceof android.app.Activity) {
                    field.set(loadLayout, null);
                    Log.d(TAG, "清理LoadLayout Activity引用: " + fieldName);
                }
                // 清理Fragment引用
                else if (value instanceof androidx.fragment.app.Fragment) {
                    field.set(loadLayout, null);
                    Log.d(TAG, "清理LoadLayout Fragment引用: " + fieldName);
                }
                // 清理Handler引用
                else if (value instanceof android.os.Handler) {
                    android.os.Handler handler = (android.os.Handler) value;
                    handler.removeCallbacksAndMessages(null);
                    field.set(loadLayout, null);
                    Log.d(TAG, "清理LoadLayout Handler引用: " + fieldName);
                }
            }
            
            // 尝试调用可能的清理方法
            try {
                Method[] methods = loadLayoutClass.getDeclaredMethods();
                for (Method method : methods) {
                    String methodName = method.getName();
                    if ((methodName.contains("clear") || methodName.contains("release") || 
                         methodName.contains("destroy")) && method.getParameterCount() == 0) {
                        method.setAccessible(true);
                        method.invoke(loadLayout);
                        Log.d(TAG, "调用LoadLayout清理方法: " + methodName);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "调用LoadLayout清理方法失败: " + e.getMessage());
            }
            
        } catch (Exception e) {
            Log.w(TAG, "清理特定LoadLayout失败: " + e.getMessage());
        }
    }
    
    /**
     * 清理Callback相关引用
     */
    private static void clearCallbackReferences(LoadService<?> loadService) {
        try {
            Class<?> loadServiceClass = loadService.getClass();
            Field[] fields = loadServiceClass.getDeclaredFields();
            
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(loadService);
                
                if (value != null) {
                    String className = value.getClass().getName();
                    String fieldName = field.getName();
                    
                    // 清理所有Callback相关的引用
                    if (className.contains("Callback") || 
                        className.contains("com.kingja.loadsir.callback") ||
                        fieldName.contains("callback") ||
                        fieldName.contains("Callback")) {
                        
                        // 如果是Callback对象，先清理其内部引用
                        clearCallbackInternalReferences(value);
                        
                        // 然后清理字段引用
                        field.set(loadService, null);
                        Log.d(TAG, "清理Callback引用: " + fieldName);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "清理Callback引用失败: " + e.getMessage());
        }
    }
    
    /**
     * 清理Callback内部引用
     */
    private static void clearCallbackInternalReferences(Object callback) {
        try {
            if (callback == null) return;
            
            Class<?> callbackClass = callback.getClass();
            Field[] fields = callbackClass.getDeclaredFields();
            
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(callback);
                
                if (value != null) {
                    String fieldName = field.getName();
                    
                    // 清理View引用
                    if (value instanceof View || value instanceof ViewGroup) {
                        field.set(callback, null);
                        Log.d(TAG, "清理Callback View引用: " + fieldName);
                    }
                    // 清理Context引用
                    else if (value instanceof android.content.Context) {
                        field.set(callback, null);
                        Log.d(TAG, "清理Callback Context引用: " + fieldName);
                    }
                    // 清理监听器引用
                    else if (fieldName.contains("Listener") || fieldName.contains("listener")) {
                        field.set(callback, null);
                        Log.d(TAG, "清理Callback监听器: " + fieldName);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "清理Callback内部引用失败: " + e.getMessage());
        }
    }
    
    /**
     * 全局清理LoadSir相关的静态引用
     */
    public static void clearGlobalLoadSirReferences() {
        try {
            Log.i(TAG, "开始清理LoadSir全局引用");
            
            // 清理LoadSir的全局配置
            try {
                Class<?> loadSirClass = Class.forName("com.kingja.loadsir.core.LoadSir");
                Field[] staticFields = loadSirClass.getDeclaredFields();
                
                for (Field field : staticFields) {
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                        field.setAccessible(true);
                        Object value = field.get(null);
                        
                        if (value != null) {
                            String fieldName = field.getName();
                            
                            // 清理可能的静态引用
                            if (fieldName.contains("instance") || 
                                fieldName.contains("config") ||
                                fieldName.contains("builder")) {
                                
                                // 不直接设置为null，而是清理其内部引用
                                clearObjectInternalReferences(value);
                                Log.d(TAG, "清理LoadSir静态引用内容: " + fieldName);
                            }
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                Log.w(TAG, "LoadSir类未找到，跳过全局清理");
            }
            
            Log.i(TAG, "LoadSir全局引用清理完成");
        } catch (Exception e) {
            Log.e(TAG, "清理LoadSir全局引用失败: " + e.getMessage());
        }
    }
    
    /**
     * 清理对象内部引用
     */
    private static void clearObjectInternalReferences(Object obj) {
        try {
            if (obj == null) return;
            
            Class<?> objClass = obj.getClass();
            Field[] fields = objClass.getDeclaredFields();
            
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(obj);
                
                if (value instanceof View || 
                    value instanceof ViewGroup || 
                    value instanceof android.content.Context) {
                    field.set(obj, null);
                    Log.d(TAG, "清理对象内部引用: " + field.getName());
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "清理对象内部引用失败: " + e.getMessage());
        }
    }
}
