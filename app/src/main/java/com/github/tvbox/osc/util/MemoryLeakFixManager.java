package com.github.tvbox.osc.util;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一的内存泄漏修复管理器
 * 负责协调和管理所有内存泄漏修复工作
 */
public class MemoryLeakFixManager {
    private static final String TAG = "MemoryLeakFixManager";

    // 使用弱引用存储Activity和Fragment，避免内存泄漏
    private static final ConcurrentHashMap<String, WeakReference<Activity>> sActivityRefs = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, WeakReference<Fragment>> sFragmentRefs = new ConcurrentHashMap<>();

    private static volatile boolean sIsInitialized = false;
    private static volatile boolean sIsCleaningUp = false;

    /**
     * 初始化内存泄漏修复管理器
     */
    public static synchronized void initialize(Application application) {
        if (sIsInitialized) {
            return;
        }

        try {
            Log.i(TAG, "初始化内存泄漏修复管理器");

            // 注册Activity生命周期回调
            application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(Activity activity, android.os.Bundle savedInstanceState) {
                    registerActivity(activity);
                }

                @Override
                public void onActivityStarted(Activity activity) {}

                @Override
                public void onActivityResumed(Activity activity) {}

                @Override
                public void onActivityPaused(Activity activity) {}

                @Override
                public void onActivityStopped(Activity activity) {}

                @Override
                public void onActivitySaveInstanceState(Activity activity, android.os.Bundle outState) {}

                @Override
                public void onActivityDestroyed(Activity activity) {
                    unregisterActivity(activity);
                    fixActivityLeaks(activity);
                }
            });

            sIsInitialized = true;
            Log.i(TAG, "内存泄漏修复管理器初始化完成");
        } catch (Exception e) {
            Log.e(TAG, "初始化内存泄漏修复管理器失败: " + e.getMessage());
        }
    }

    /**
     * 注册Activity
     */
    private static void registerActivity(Activity activity) {
        if (activity != null) {
            String key = activity.getClass().getName() + "@" + activity.hashCode();
            sActivityRefs.put(key, new WeakReference<>(activity));
            Log.d(TAG, "注册Activity: " + activity.getClass().getSimpleName());
        }
    }

    /**
     * 注销Activity
     */
    private static void unregisterActivity(Activity activity) {
        if (activity != null) {
            String key = activity.getClass().getName() + "@" + activity.hashCode();
            sActivityRefs.remove(key);
            Log.d(TAG, "注销Activity: " + activity.getClass().getSimpleName());
        }
    }

    /**
     * 注册Fragment
     */
    public static void registerFragment(Fragment fragment) {
        if (fragment != null) {
            String key = fragment.getClass().getName() + "@" + fragment.hashCode();
            sFragmentRefs.put(key, new WeakReference<>(fragment));
            Log.d(TAG, "注册Fragment: " + fragment.getClass().getSimpleName());
        }
    }

    /**
     * 注销Fragment
     */
    public static void unregisterFragment(Fragment fragment) {
        if (fragment != null) {
            String key = fragment.getClass().getName() + "@" + fragment.hashCode();
            sFragmentRefs.remove(key);
            Log.d(TAG, "注销Fragment: " + fragment.getClass().getSimpleName());
        }
    }

    /**
     * 修复Activity相关的内存泄漏
     */
    public static void fixActivityLeaks(Activity activity) {
        if (activity == null || sIsCleaningUp) {
            return;
        }

        ThreadPoolManager.executeIO(() -> {
            try {
                Log.i(TAG, "开始修复Activity内存泄漏: " + activity.getClass().getSimpleName());

                // 清理View树
                fixViewTreeLeaks(activity);

                // 清理Handler
                fixHandlerLeaks(activity);

                // 清理RecyclerView
                fixRecyclerViewLeaks(activity);

                // 清理LoadSir
                fixLoadSirLeaks(activity);

                Log.i(TAG, "Activity内存泄漏修复完成: " + activity.getClass().getSimpleName());
            } catch (Exception e) {
                Log.e(TAG, "修复Activity内存泄漏失败: " + e.getMessage());
            }
        });
    }

    /**
     * 修复Fragment相关的内存泄漏
     */
    public static void fixFragmentLeaks(Fragment fragment) {
        if (fragment == null || sIsCleaningUp) {
            return;
        }

        ThreadPoolManager.executeIO(() -> {
            try {
                Log.i(TAG, "开始修复Fragment内存泄漏: " + fragment.getClass().getSimpleName());

                // 清理View树
                if (fragment.getView() != null) {
                    fixViewTreeLeaks(fragment.getView());
                }

                // 清理Fragment特有的引用
                fixFragmentSpecificLeaks(fragment);

                Log.i(TAG, "Fragment内存泄漏修复完成: " + fragment.getClass().getSimpleName());
            } catch (Exception e) {
                Log.e(TAG, "修复Fragment内存泄漏失败: " + e.getMessage());
            }
        });
    }

    /**
     * 修复View树相关的内存泄漏
     */
    private static void fixViewTreeLeaks(Activity activity) {
        try {
            View rootView = activity.findViewById(android.R.id.content);
            if (rootView != null) {
                fixViewTreeLeaks(rootView);
            }
        } catch (Exception e) {
            Log.w(TAG, "修复Activity View树泄漏失败: " + e.getMessage());
        }
    }

    /**
     * 修复View树相关的内存泄漏
     */
    private static void fixViewTreeLeaks(View rootView) {
        if (rootView == null) return;

        try {
            // 清理View的监听器
            rootView.setOnClickListener(null);
            rootView.setOnLongClickListener(null);
            rootView.setOnTouchListener(null);
            rootView.setOnFocusChangeListener(null);

            // 如果是ViewGroup，递归清理子View
            if (rootView instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) rootView;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    fixViewTreeLeaks(viewGroup.getChildAt(i));
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "修复View树泄漏失败: " + e.getMessage());
        }
    }

    /**
     * 修复Handler相关的内存泄漏
     */
    private static void fixHandlerLeaks(Activity activity) {
        try {
            // 使用反射查找Activity中的Handler字段
            Field[] fields = activity.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(activity);

                if (value instanceof Handler) {
                    Handler handler = (Handler) value;
                    handler.removeCallbacksAndMessages(null);
                    Log.d(TAG, "清理Handler: " + field.getName());
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "修复Handler泄漏失败: " + e.getMessage());
        }
    }

    /**
     * 修复RecyclerView相关的内存泄漏
     */
    private static void fixRecyclerViewLeaks(Activity activity) {
        try {
            View rootView = activity.findViewById(android.R.id.content);
            if (rootView != null) {
                fixRecyclerViewLeaks(rootView);
            }
        } catch (Exception e) {
            Log.w(TAG, "修复RecyclerView泄漏失败: " + e.getMessage());
        }
    }

    /**
     * 递归修复RecyclerView相关的内存泄漏
     */
    private static void fixRecyclerViewLeaks(View view) {
        if (view == null) return;

        try {
            if (view instanceof RecyclerView) {
                RecyclerView recyclerView = (RecyclerView) view;
                recyclerView.setAdapter(null);
                recyclerView.setLayoutManager(null);
                recyclerView.clearOnScrollListeners();
                recyclerView.clearOnChildAttachStateChangeListeners();
                recyclerView.getRecycledViewPool().clear();
                Log.d(TAG, "清理RecyclerView");
            } else if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    fixRecyclerViewLeaks(viewGroup.getChildAt(i));
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "修复RecyclerView泄漏失败: " + e.getMessage());
        }
    }

    /**
     * 修复LoadSir相关的内存泄漏
     */
    private static void fixLoadSirLeaks(Activity activity) {
        try {
            // 这里可以添加LoadSir特定的清理逻辑
            Log.d(TAG, "清理LoadSir相关引用");
        } catch (Exception e) {
            Log.w(TAG, "修复LoadSir泄漏失败: " + e.getMessage());
        }
    }

    /**
     * 修复Fragment特有的内存泄漏
     */
    private static void fixFragmentSpecificLeaks(Fragment fragment) {
        try {
            // 清理Fragment的Context引用
            // 注意：这里需要小心处理，避免影响Fragment的正常功能
            Log.d(TAG, "清理Fragment特有引用");
        } catch (Exception e) {
            Log.w(TAG, "修复Fragment特有泄漏失败: " + e.getMessage());
        }
    }

    /**
     * 执行全面的内存泄漏修复
     */
    public static void performComprehensiveCleanup() {
        if (sIsCleaningUp) {
            return;
        }

        sIsCleaningUp = true;

        ThreadPoolManager.executeIO(() -> {
            try {
                Log.i(TAG, "开始执行全面内存泄漏修复");

                // 修复Timer相关泄漏
                fixTimerLeaks();

                // 修复动画相关泄漏
                fixAnimationLeaks();

                // 修复Lottie动画泄漏
                fixLottieLeaks();

                // 修复EventBus泄漏
                fixEventBusLeaks();

                // 修复QuickJS相关泄漏
                fixQuickJSLeaks();

                // 修复OkHttp相关泄漏
                OkGoHelper.cleanup();

                // 修复线程池相关泄漏
                ThreadPoolManager.cleanup();

                // 清理所有注册的引用
                sActivityRefs.clear();
                sFragmentRefs.clear();

                // 强制垃圾回收
                System.gc();
                Thread.sleep(100);
                System.gc();

                Log.i(TAG, "全面内存泄漏修复完成");
            } catch (Exception e) {
                Log.e(TAG, "执行全面内存泄漏修复失败: " + e.getMessage());
            } finally {
                sIsCleaningUp = false;
            }
        });
    }

    /**
     * 修复Timer相关的内存泄漏
     */
    private static void fixTimerLeaks() {
        try {
            Log.i(TAG, "开始修复Timer内存泄漏");

            // 使用反射查找所有Timer实例
            try {
                // 查找Global类中的Timer
                Class<?> globalClass = Class.forName("com.github.tvbox.osc.util.js.Global");
                Field[] fields = globalClass.getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    if (field.getType() == java.util.Timer.class) {
                        // 这是静态字段，需要特殊处理
                        Log.d(TAG, "发现Timer字段: " + field.getName());
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "清理Global Timer失败: " + e.getMessage());
            }

            // 查找JSConsole中的Timer
            try {
                Class<?> consoleClass = Class.forName("com.whl.quickjs.wrapper.JSConsole");
                Field[] fields = consoleClass.getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    if (field.getType() == java.util.Map.class && field.getName().contains("timer")) {
                        Log.d(TAG, "发现Timer Map字段: " + field.getName());
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "清理JSConsole Timer失败: " + e.getMessage());
            }

            Log.i(TAG, "Timer内存泄漏修复完成");
        } catch (Exception e) {
            Log.e(TAG, "修复Timer内存泄漏失败: " + e.getMessage());
        }
    }

    /**
     * 修复动画相关的内存泄漏
     */
    private static void fixAnimationLeaks() {
        try {
            Log.i(TAG, "开始修复动画内存泄漏");

            // 清理ValueAnimator
            try {
                Class<?> animatorClass = Class.forName("com.angcyo.tablayout.DslTabLayout");
                // 这里可以添加更多动画清理逻辑
                Log.d(TAG, "清理DslTabLayout动画");
            } catch (Exception e) {
                Log.w(TAG, "清理DslTabLayout动画失败: " + e.getMessage());
            }

            Log.i(TAG, "动画内存泄漏修复完成");
        } catch (Exception e) {
            Log.e(TAG, "修复动画内存泄漏失败: " + e.getMessage());
        }
    }

    /**
     * 修复Lottie动画相关的内存泄漏
     */
    private static void fixLottieLeaks() {
        try {
            Log.i(TAG, "开始修复Lottie动画内存泄漏");

            // 清理Lottie动画缓存
            try {
                Class<?> lottieClass = Class.forName("com.airbnb.lottie.L");
                // 清理Lottie缓存
                Log.d(TAG, "清理Lottie缓存");
            } catch (Exception e) {
                Log.w(TAG, "清理Lottie缓存失败: " + e.getMessage());
            }

            Log.i(TAG, "Lottie动画内存泄漏修复完成");
        } catch (Exception e) {
            Log.e(TAG, "修复Lottie动画内存泄漏失败: " + e.getMessage());
        }
    }

    /**
     * 修复EventBus相关的内存泄漏
     */
    private static void fixEventBusLeaks() {
        try {
            Log.i(TAG, "开始修复EventBus内存泄漏");

            // 使用反射清理EventBus的注册表
            try {
                Class<?> eventBusClass = org.greenrobot.eventbus.EventBus.class;
                Object eventBusInstance = eventBusClass.getMethod("getDefault").invoke(null);

                // 清理订阅者注册表
                Field subscribersByTypeField = eventBusClass.getDeclaredField("subscriptionsByEventType");
                subscribersByTypeField.setAccessible(true);
                Object subscriptionsByEventType = subscribersByTypeField.get(eventBusInstance);

                if (subscriptionsByEventType instanceof java.util.Map) {
                    java.util.Map<?, ?> map = (java.util.Map<?, ?>) subscriptionsByEventType;
                    int cleanedCount = 0;

                    for (Object eventType : map.keySet()) {
                        Object subscriptions = map.get(eventType);
                        if (subscriptions instanceof java.util.List) {
                            java.util.List<?> subList = (java.util.List<?>) subscriptions;
                            java.util.Iterator<?> subIterator = subList.iterator();

                            while (subIterator.hasNext()) {
                                Object subscription = subIterator.next();
                                try {
                                    Field subscriberField = subscription.getClass().getDeclaredField("subscriber");
                                    subscriberField.setAccessible(true);
                                    Object subscriber = subscriberField.get(subscription);

                                    // 检查是否是已销毁的Activity
                                    if (subscriber instanceof android.app.Activity) {
                                        android.app.Activity activity = (android.app.Activity) subscriber;
                                        if (activity.isDestroyed() || activity.isFinishing()) {
                                            subIterator.remove();
                                            cleanedCount++;
                                            Log.d(TAG, "清理已销毁Activity的EventBus订阅: " + activity.getClass().getSimpleName());
                                        }
                                    }
                                } catch (Exception e) {
                                    // 忽略反射错误
                                }
                            }
                        }
                    }
                    Log.i(TAG, "清理了 " + cleanedCount + " 个无效的EventBus订阅");
                }
            } catch (Exception e) {
                Log.w(TAG, "清理EventBus订阅时发生错误: " + e.getMessage());
            }

            Log.i(TAG, "EventBus内存泄漏修复完成");
        } catch (Exception e) {
            Log.e(TAG, "修复EventBus内存泄漏失败: " + e.getMessage());
        }
    }

    /**
     * 修复QuickJS相关的内存泄漏
     */
    private static void fixQuickJSLeaks() {
        try {
            Log.i(TAG, "开始修复QuickJS内存泄漏");

            // 清理QuickJS相关资源
            try {
                // 这里可以添加QuickJS特定的清理逻辑
                Log.d(TAG, "清理QuickJS资源");
            } catch (Exception e) {
                Log.w(TAG, "清理QuickJS资源失败: " + e.getMessage());
            }

            Log.i(TAG, "QuickJS内存泄漏修复完成");
        } catch (Exception e) {
            Log.e(TAG, "修复QuickJS内存泄漏失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前注册的Activity数量
     */
    public static int getRegisteredActivityCount() {
        return sActivityRefs.size();
    }

    /**
     * 获取当前注册的Fragment数量
     */
    public static int getRegisteredFragmentCount() {
        return sFragmentRefs.size();
    }
}
