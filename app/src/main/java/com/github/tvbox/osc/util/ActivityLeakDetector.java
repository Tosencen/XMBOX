package com.github.tvbox.osc.util;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;

import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Activity内存泄漏检测和修复工具
 * 专门用于检测和修复Activity相关的内存泄漏
 */
public class ActivityLeakDetector {
    private static final String TAG = "ActivityLeakDetector";

    // 存储Activity的弱引用，用于检测泄漏
    private static final ConcurrentHashMap<String, WeakReference<Activity>> sActivityRefs = new ConcurrentHashMap<>();

    // 检测Handler
    private static final Handler sDetectHandler = new Handler(Looper.getMainLooper());

    /**
     * 注册Activity进行泄漏检测
     */
    public static void registerActivity(Activity activity) {
        if (activity != null) {
            String key = activity.getClass().getName() + "@" + activity.hashCode();
            sActivityRefs.put(key, new WeakReference<>(activity));
            Log.d(TAG, "注册Activity进行泄漏检测: " + activity.getClass().getSimpleName());
        }
    }

    /**
     * Activity销毁时调用，开始泄漏检测
     */
    public static void onActivityDestroyed(Activity activity) {
        if (activity == null) return;

        String activityName = activity.getClass().getSimpleName();
        Log.d(TAG, "Activity销毁，开始泄漏检测: " + activityName);

        // 立即进行修复
        fixActivityLeaksImmediate(activity);

        // 延迟检测是否真的被回收了
        sDetectHandler.postDelayed(() -> {
            checkActivityLeak(activity, activityName);
        }, 3000);
    }

    /**
     * 立即修复Activity的内存泄漏
     */
    private static void fixActivityLeaksImmediate(Activity activity) {
        try {
            Log.i(TAG, "开始立即修复Activity内存泄漏: " + activity.getClass().getSimpleName());

            // 1. 清理EventBus注册
            fixEventBusLeak(activity);

            // 2. 清理View树中的监听器
            fixViewTreeLeaks(activity);

            // 3. 清理RecyclerView
            fixRecyclerViewLeaks(activity);

            // 4. 清理Handler
            fixHandlerLeaks(activity);

            // 5. 清理动画
            fixAnimationLeaks(activity);

            // 6. 清理Lottie动画
            fixLottieAnimationLeaks(activity);

            // 7. 清理Timer
            fixTimerLeaks(activity);

            // 8. 清理LoadSir相关泄漏
            fixLoadSirLeaks(activity);

            Log.i(TAG, "Activity内存泄漏立即修复完成: " + activity.getClass().getSimpleName());
        } catch (Exception e) {
            Log.e(TAG, "立即修复Activity内存泄漏失败: " + e.getMessage());
        }
    }

    /**
     * 修复EventBus相关泄漏
     */
    private static void fixEventBusLeak(Activity activity) {
        try {
            if (EventBus.getDefault().isRegistered(activity)) {
                EventBus.getDefault().unregister(activity);
                Log.d(TAG, "清理Activity的EventBus注册: " + activity.getClass().getSimpleName());
            }
        } catch (Exception e) {
            Log.w(TAG, "清理EventBus注册失败: " + e.getMessage());
        }
    }

    /**
     * 修复View树中的监听器泄漏
     */
    private static void fixViewTreeLeaks(Activity activity) {
        try {
            View rootView = activity.findViewById(android.R.id.content);
            if (rootView != null) {
                clearViewListeners(rootView);
            }
        } catch (Exception e) {
            Log.w(TAG, "清理View监听器失败: " + e.getMessage());
        }
    }

    /**
     * 递归清理View的监听器
     */
    private static void clearViewListeners(View view) {
        if (view == null) return;

        try {
            // 清理常见的监听器
            view.setOnClickListener(null);
            view.setOnLongClickListener(null);
            view.setOnTouchListener(null);
            view.setOnFocusChangeListener(null);
            view.setOnKeyListener(null);

            // 如果是ViewGroup，递归清理子View
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    clearViewListeners(viewGroup.getChildAt(i));
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "清理单个View监听器失败: " + e.getMessage());
        }
    }

    /**
     * 修复RecyclerView相关泄漏
     */
    private static void fixRecyclerViewLeaks(Activity activity) {
        try {
            View rootView = activity.findViewById(android.R.id.content);
            if (rootView != null) {
                clearRecyclerViews(rootView);
            }
        } catch (Exception e) {
            Log.w(TAG, "清理RecyclerView失败: " + e.getMessage());
        }
    }

    /**
     * 递归清理RecyclerView
     */
    private static void clearRecyclerViews(View view) {
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
                    clearRecyclerViews(viewGroup.getChildAt(i));
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "清理单个RecyclerView失败: " + e.getMessage());
        }
    }

    /**
     * 修复Handler相关泄漏
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
            Log.w(TAG, "清理Handler失败: " + e.getMessage());
        }
    }

    /**
     * 修复动画相关泄漏
     */
    private static void fixAnimationLeaks(Activity activity) {
        try {
            View rootView = activity.findViewById(android.R.id.content);
            if (rootView != null) {
                clearAnimations(rootView);
            }
        } catch (Exception e) {
            Log.w(TAG, "清理动画失败: " + e.getMessage());
        }
    }

    /**
     * 递归清理动画
     */
    private static void clearAnimations(View view) {
        if (view == null) return;

        try {
            // 清理View动画
            view.clearAnimation();
            view.animate().cancel();

            // 如果是ViewGroup，递归清理子View
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    clearAnimations(viewGroup.getChildAt(i));
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "清理单个View动画失败: " + e.getMessage());
        }
    }

    /**
     * 修复Lottie动画相关泄漏
     */
    private static void fixLottieAnimationLeaks(Activity activity) {
        try {
            View rootView = activity.findViewById(android.R.id.content);
            if (rootView != null) {
                clearLottieAnimations(rootView);
            }
        } catch (Exception e) {
            Log.w(TAG, "清理Lottie动画失败: " + e.getMessage());
        }
    }

    /**
     * 递归清理Lottie动画
     */
    private static void clearLottieAnimations(View view) {
        if (view == null) return;

        try {
            if (view instanceof LottieAnimationView) {
                LottieAnimationView lottieView = (LottieAnimationView) view;
                lottieView.cancelAnimation();
                lottieView.clearAnimation();
                Log.d(TAG, "清理LottieAnimationView");
            } else if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    clearLottieAnimations(viewGroup.getChildAt(i));
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "清理单个Lottie动画失败: " + e.getMessage());
        }
    }

    /**
     * 修复Timer相关泄漏
     */
    private static void fixTimerLeaks(Activity activity) {
        try {
            // 使用反射查找Activity中的Timer字段
            Field[] fields = activity.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(activity);

                if (value instanceof Timer) {
                    Timer timer = (Timer) value;
                    timer.cancel();
                    timer.purge();
                    Log.d(TAG, "清理Timer: " + field.getName());
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "清理Timer失败: " + e.getMessage());
        }
    }

    /**
     * 修复LoadSir相关泄漏
     */
    private static void fixLoadSirLeaks(Activity activity) {
        try {
            // 使用反射查找Activity中的LoadService字段
            Field[] fields = activity.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(activity);

                if (value != null) {
                    String className = value.getClass().getName();
                    if (className.contains("LoadService") || className.contains("loadsir")) {
                        // 使用专门的LoadSir修复工具
                        try {
                            Class<?> fixerClass = Class.forName("com.github.tvbox.osc.util.LoadSirLeakFixer");
                            java.lang.reflect.Method fixMethod = fixerClass.getMethod("fixLoadServiceLeak", Object.class);
                            fixMethod.invoke(null, value);
                            Log.d(TAG, "清理LoadService: " + field.getName());
                        } catch (Exception e) {
                            Log.w(TAG, "使用LoadSir修复工具失败: " + e.getMessage());
                        }

                        // 清理字段引用
                        field.set(activity, null);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "清理LoadSir失败: " + e.getMessage());
        }
    }

    /**
     * 检查Activity是否真的被回收了
     */
    private static void checkActivityLeak(Activity activity, String activityName) {
        String key = activity.getClass().getName() + "@" + activity.hashCode();
        WeakReference<Activity> ref = sActivityRefs.get(key);

        if (ref != null && ref.get() != null) {
            Log.w(TAG, "检测到Activity内存泄漏: " + activityName);

            // 强制GC
            System.gc();
            System.runFinalization();

            // 再次检查
            sDetectHandler.postDelayed(() -> {
                if (ref.get() != null) {
                    Log.e(TAG, "确认Activity内存泄漏: " + activityName + " 在强制GC后仍未被回收");
                } else {
                    Log.i(TAG, "Activity内存泄漏已解决: " + activityName + " 在强制GC后被成功回收");
                    sActivityRefs.remove(key);
                }
            }, 2000);
        } else {
            Log.i(TAG, "Activity正常回收: " + activityName);
            sActivityRefs.remove(key);
        }
    }

    /**
     * 清理所有检测数据
     */
    public static void cleanup() {
        sActivityRefs.clear();
        sDetectHandler.removeCallbacksAndMessages(null);
        Log.i(TAG, "Activity泄漏检测器已清理");
    }

    /**
     * 获取当前可能泄漏的Activity数量
     */
    public static int getLeakedActivityCount() {
        return sActivityRefs.size();
    }
}
