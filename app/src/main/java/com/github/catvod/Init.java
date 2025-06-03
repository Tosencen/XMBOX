package com.github.catvod;

import android.content.Context;
import android.app.Activity;
import android.util.Log;

import java.lang.ref.WeakReference;

public class Init {

    private WeakReference<Context> context;

    private static class Loader {
        static volatile Init INSTANCE = new Init();
    }

    private static Init get() {
        return Loader.INSTANCE;
    }

    public static void set(Context context) {
        if (context != null) {
            // 确保使用ApplicationContext，避免Activity被销毁导致的null引用
            Context appContext = context.getApplicationContext();
            if (appContext != null) {
                get().context = new WeakReference<>(appContext);
                Log.d("CatVod.Init", "Context设置成功: " + appContext.getClass().getSimpleName());
            } else {
                get().context = new WeakReference<>(context);
                Log.d("CatVod.Init", "使用原始Context: " + context.getClass().getSimpleName());
            }
        } else {
            Log.e("CatVod.Init", "尝试设置null Context");
        }
    }

    public static Context context() {
        Context ctx = get().context != null ? get().context.get() : null;
        if (ctx == null) {
            Log.e("CatVod.Init", "Context为null，可能已被垃圾回收");
        }
        return ctx;
    }

    /**
     * 安全获取Context，如果为null则返回Application Context
     */
    public static Context safeContext() {
        Context ctx = context();
        if (ctx == null) {
            // 尝试从App实例获取Context
            try {
                Class<?> appClass = Class.forName("com.github.tvbox.osc.base.App");
                Object appInstance = appClass.getMethod("getInstance").invoke(null);
                ctx = (Context) appInstance;
                Log.d("CatVod.Init", "从App实例获取Context成功");
            } catch (Exception e) {
                Log.e("CatVod.Init", "从App实例获取Context失败: " + e.getMessage());
            }
        }
        return ctx;
    }
}
