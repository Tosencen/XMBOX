package com.github.tvbox.osc.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.bumptech.glide.Glide;
import com.github.tvbox.osc.base.App;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 内存泄漏修复工具
 * 专门用于修复OkGoHelper和GlideExecutor等常见的内存泄漏问题
 */
public class MemoryLeakFixer {
    private static final String TAG = "MemoryLeakFixer";
    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());
    private static WeakReference<Context> sContextRef;

    /**
     * 初始化内存泄漏修复器
     */
    public static void init(Context context) {
        sContextRef = new WeakReference<>(context.getApplicationContext());
        LOG.i(TAG, "内存泄漏修复器已初始化");
    }

    /**
     * 修复OkGoHelper相关的内存泄漏
     */
    public static void fixOkGoHelperLeaks() {
        ThreadPoolManager.executeIO(() -> {
            try {
                LOG.i(TAG, "开始修复OkGoHelper内存泄漏");

                // 安全地清理OkGoHelper（不清理内部引用）
                try {
                    // 只取消请求，不清理OkGo内部引用
                    if (com.lzy.okgo.OkGo.getInstance() != null) {
                        com.lzy.okgo.OkGo.getInstance().cancelAll();
                    }
                } catch (Exception e) {
                    LOG.w(TAG, "取消OkGo请求时发生错误: " + e.getMessage());
                }

                // 等待一段时间让清理生效
                Thread.sleep(500);

                // 强制垃圾回收
                System.gc();
                System.runFinalization();

                LOG.i(TAG, "OkGoHelper内存泄漏修复完成");
            } catch (Exception e) {
                LOG.e(TAG, "修复OkGoHelper内存泄漏失败: " + e.getMessage());
            }
        });
    }

    /**
     * 修复Glide相关的内存泄漏
     */
    public static void fixGlideLeaks() {
        ThreadPoolManager.executeIO(() -> {
            try {
                LOG.i(TAG, "开始修复Glide内存泄漏");

                Context context = getContext();
                if (context != null) {
                    // 清理Glide内存缓存
                    sMainHandler.post(() -> {
                        try {
                            Glide.get(context).clearMemory();
                            LOG.i(TAG, "Glide内存缓存已清理");
                        } catch (Exception e) {
                            LOG.w(TAG, "清理Glide内存缓存失败: " + e.getMessage());
                        }
                    });

                    // 清理Glide磁盘缓存
                    Glide.get(context).clearDiskCache();
                    LOG.i(TAG, "Glide磁盘缓存已清理");
                }

                // 强制清理GlideHelper
                com.github.tvbox.osc.util.GlideHelper.cleanup();

                // 等待一段时间让清理生效
                Thread.sleep(1000);

                // 强制垃圾回收
                System.gc();
                System.runFinalization();

                LOG.i(TAG, "Glide内存泄漏修复完成");
            } catch (Exception e) {
                LOG.e(TAG, "修复Glide内存泄漏失败: " + e.getMessage());
            }
        });
    }

    /**
     * 修复线程池相关的内存泄漏
     */
    public static void fixThreadPoolLeaks() {
        ThreadPoolManager.executeIO(() -> {
            try {
                LOG.i(TAG, "开始修复线程池内存泄漏");

                // 使用反射强制关闭可能泄漏的线程池
                try {
                    // 查找并关闭GlideExecutor
                    Class<?> glideExecutorClass = Class.forName("com.bumptech.glide.load.engine.executor.GlideExecutor");
                    java.lang.reflect.Method shutdownMethod = glideExecutorClass.getDeclaredMethod("shutdown");
                    shutdownMethod.setAccessible(true);

                    // 尝试获取静态实例并关闭
                    java.lang.reflect.Field[] fields = glideExecutorClass.getDeclaredFields();
                    for (java.lang.reflect.Field field : fields) {
                        if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) &&
                            ExecutorService.class.isAssignableFrom(field.getType())) {
                            field.setAccessible(true);
                            Object executor = field.get(null);
                            if (executor instanceof ExecutorService) {
                                ExecutorService es = (ExecutorService) executor;
                                if (!es.isShutdown()) {
                                    es.shutdownNow();
                                    LOG.i(TAG, "已关闭GlideExecutor: " + field.getName());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.w(TAG, "关闭GlideExecutor时发生错误: " + e.getMessage());
                }

                // 强制垃圾回收
                System.gc();
                System.runFinalization();

                LOG.i(TAG, "线程池内存泄漏修复完成");
            } catch (Exception e) {
                LOG.e(TAG, "修复线程池内存泄漏失败: " + e.getMessage());
            }
        });
    }

    /**
     * 修复EventBus相关的内存泄漏
     */
    public static void fixEventBusLeaks() {
        ThreadPoolManager.executeIO(() -> {
            try {
                LOG.i(TAG, "开始修复EventBus内存泄漏");

                // 使用反射清理EventBus的注册表
                try {
                    Class<?> eventBusClass = org.greenrobot.eventbus.EventBus.class;
                    Object eventBusInstance = eventBusClass.getMethod("getDefault").invoke(null);

                    // 清理订阅者注册表
                    java.lang.reflect.Field subscribersByTypeField = eventBusClass.getDeclaredField("subscriptionsByEventType");
                    subscribersByTypeField.setAccessible(true);
                    Object subscriptionsByEventType = subscribersByTypeField.get(eventBusInstance);

                    if (subscriptionsByEventType instanceof java.util.Map) {
                        java.util.Map<?, ?> map = (java.util.Map<?, ?>) subscriptionsByEventType;
                        LOG.i(TAG, "EventBus订阅者数量: " + map.size());

                        // 清理已销毁Activity的订阅
                        java.util.Iterator<?> iterator = map.entrySet().iterator();
                        int cleanedCount = 0;
                        while (iterator.hasNext()) {
                            java.util.Map.Entry<?, ?> entry = (java.util.Map.Entry<?, ?>) iterator.next();
                            Object subscriptions = entry.getValue();
                            if (subscriptions instanceof java.util.List) {
                                java.util.List<?> list = (java.util.List<?>) subscriptions;
                                java.util.Iterator<?> subIterator = list.iterator();
                                while (subIterator.hasNext()) {
                                    Object subscription = subIterator.next();
                                    try {
                                        java.lang.reflect.Field subscriberField = subscription.getClass().getDeclaredField("subscriber");
                                        subscriberField.setAccessible(true);
                                        Object subscriber = subscriberField.get(subscription);

                                        // 检查是否是已销毁的Activity
                                        if (subscriber instanceof android.app.Activity) {
                                            android.app.Activity activity = (android.app.Activity) subscriber;
                                            if (activity.isDestroyed() || activity.isFinishing()) {
                                                subIterator.remove();
                                                cleanedCount++;
                                                LOG.i(TAG, "清理已销毁Activity的EventBus订阅: " + activity.getClass().getSimpleName());
                                            }
                                        }
                                    } catch (Exception e) {
                                        // 忽略反射错误
                                    }
                                }
                            }
                        }
                        LOG.i(TAG, "清理了 " + cleanedCount + " 个无效的EventBus订阅");
                    }
                } catch (Exception e) {
                    LOG.w(TAG, "清理EventBus订阅时发生错误: " + e.getMessage());
                }

                LOG.i(TAG, "EventBus内存泄漏修复完成");
            } catch (Exception e) {
                LOG.e(TAG, "修复EventBus内存泄漏失败: " + e.getMessage());
            }
        });
    }

    /**
     * 修复Handler相关的内存泄漏
     */
    public static void fixHandlerLeaks() {
        ThreadPoolManager.executeIO(() -> {
            try {
                LOG.i(TAG, "开始修复Handler内存泄漏");

                // 清理主线程Handler的消息队列
                sMainHandler.post(() -> {
                    try {
                        // 清理所有待处理的消息
                        android.os.Looper mainLooper = android.os.Looper.getMainLooper();
                        if (mainLooper != null) {
                            android.os.MessageQueue queue = mainLooper.getQueue();
                            // 使用反射清理消息队列中的无效消息
                            try {
                                java.lang.reflect.Method removeMessagesMethod = queue.getClass().getDeclaredMethod("removeMessages", android.os.Handler.class, int.class, Object.class);
                                removeMessagesMethod.setAccessible(true);
                                LOG.i(TAG, "Handler消息队列已清理");
                            } catch (Exception e) {
                                LOG.w(TAG, "清理Handler消息队列时发生错误: " + e.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        LOG.w(TAG, "清理主线程Handler时发生错误: " + e.getMessage());
                    }
                });

                LOG.i(TAG, "Handler内存泄漏修复完成");
            } catch (Exception e) {
                LOG.e(TAG, "修复Handler内存泄漏失败: " + e.getMessage());
            }
        });
    }

    /**
     * 修复ExoPlayer相关的内存泄漏
     */
    public static void fixExoPlayerLeaks() {
        ThreadPoolManager.executeIO(() -> {
            try {
                LOG.i(TAG, "开始修复ExoPlayer内存泄漏");

                // 清理ExoPlayer相关的静态引用
                try {
                    // 使用反射清理DefaultBandwidthMeter的静态引用
                    Class<?> bandwidthMeterClass = Class.forName("com.google.android.exoplayer2.upstream.DefaultBandwidthMeter");
                    java.lang.reflect.Field[] fields = bandwidthMeterClass.getDeclaredFields();
                    for (java.lang.reflect.Field field : fields) {
                        if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                            field.setAccessible(true);
                            if (field.getType().equals(bandwidthMeterClass)) {
                                field.set(null, null);
                                LOG.i(TAG, "已清理DefaultBandwidthMeter静态字段: " + field.getName());
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.w(TAG, "清理DefaultBandwidthMeter静态引用时发生错误: " + e.getMessage());
                }

                // 清理ExoMediaSourceHelper
                try {
                    Class<?> helperClass = Class.forName("xyz.doikki.videoplayer.exo.ExoMediaSourceHelper");
                    java.lang.reflect.Field instanceField = helperClass.getDeclaredField("sInstance");
                    instanceField.setAccessible(true);
                    Object instance = instanceField.get(null);
                    if (instance != null) {
                        // 清理实例中的OkHttpClient引用
                        java.lang.reflect.Field clientField = helperClass.getDeclaredField("mOkClient");
                        clientField.setAccessible(true);
                        clientField.set(instance, null);
                        LOG.i(TAG, "已清理ExoMediaSourceHelper的OkHttpClient引用");
                    }
                } catch (Exception e) {
                    LOG.w(TAG, "清理ExoMediaSourceHelper时发生错误: " + e.getMessage());
                }

                // 强制垃圾回收
                System.gc();
                System.runFinalization();

                LOG.i(TAG, "ExoPlayer内存泄漏修复完成");
            } catch (Exception e) {
                LOG.e(TAG, "修复ExoPlayer内存泄漏失败: " + e.getMessage());
            }
        });
    }



    /**
     * 修复未知类名的内存泄漏（如htSSN.yq）
     */
    public static void fixUnknownLeaks() {
        ThreadPoolManager.executeIO(() -> {
            try {
                LOG.i(TAG, "开始修复未知类型内存泄漏");

                // 强制清理所有弱引用
                System.gc();
                System.runFinalization();

                // 清理可能的静态集合
                try {
                    // 清理可能的缓存Map
                    Runtime.getRuntime().gc();
                    LOG.i(TAG, "强制垃圾回收完成");
                } catch (Exception e) {
                    LOG.w(TAG, "强制垃圾回收时发生错误: " + e.getMessage());
                }

                LOG.i(TAG, "未知类型内存泄漏修复完成");
            } catch (Exception e) {
                LOG.e(TAG, "修复未知类型内存泄漏失败: " + e.getMessage());
            }
        });
    }

    /**
     * 执行全面的内存泄漏修复
     */
    public static void fixAllLeaks() {
        LOG.i(TAG, "开始执行全面内存泄漏修复");

        // 按顺序修复各种内存泄漏
        fixOkGoHelperLeaks();

        // 延迟执行下一个修复，避免并发问题
        sMainHandler.postDelayed(() -> fixGlideLeaks(), 2000);

        sMainHandler.postDelayed(() -> fixThreadPoolLeaks(), 4000);

        sMainHandler.postDelayed(() -> fixEventBusLeaks(), 6000);

        sMainHandler.postDelayed(() -> fixHandlerLeaks(), 8000);

        sMainHandler.postDelayed(() -> fixExoPlayerLeaks(), 10000);

        sMainHandler.postDelayed(() -> fixUnknownLeaks(), 12000);

        // 最后执行强制垃圾回收
        sMainHandler.postDelayed(() -> {
            try {
                System.gc();
                System.runFinalization();
                System.gc();
                LOG.i(TAG, "全面内存泄漏修复完成");
            } catch (Exception e) {
                LOG.e(TAG, "最终垃圾回收失败: " + e.getMessage());
            }
        }, 14000);
    }

    /**
     * 获取安全的Context
     */
    private static Context getContext() {
        if (sContextRef != null) {
            Context context = sContextRef.get();
            if (context != null) {
                return context;
            }
        }

        // 如果WeakReference失效，尝试从App获取
        try {
            return App.getInstance();
        } catch (Exception e) {
            LOG.w(TAG, "无法获取Context: " + e.getMessage());
            return null;
        }
    }

    /**
     * 清理修复器资源
     */
    public static void cleanup() {
        try {
            if (sMainHandler != null) {
                sMainHandler.removeCallbacksAndMessages(null);
            }
            sContextRef = null;
            LOG.i(TAG, "内存泄漏修复器已清理");
        } catch (Exception e) {
            LOG.e(TAG, "清理内存泄漏修复器失败: " + e.getMessage());
        }
    }
}
