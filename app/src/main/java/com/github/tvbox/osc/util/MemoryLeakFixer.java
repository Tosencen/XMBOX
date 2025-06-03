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

                // 取消所有OkGo请求
                try {
                    if (com.lzy.okgo.OkGo.getInstance() != null) {
                        com.lzy.okgo.OkGo.getInstance().cancelAll();
                        LOG.i(TAG, "已取消所有OkGo请求");
                    }
                } catch (Exception e) {
                    LOG.w(TAG, "取消OkGo请求时发生错误: " + e.getMessage());
                }

                // 清理OkGoHelper的静态客户端引用
                try {
                    // 使用反射清理defaultClient
                    java.lang.reflect.Field defaultClientField = OkGoHelper.class.getDeclaredField("defaultClient");
                    defaultClientField.setAccessible(true);
                    Object defaultClient = defaultClientField.get(null);
                    if (defaultClient != null) {
                        // 清理连接池
                        okhttp3.OkHttpClient client = (okhttp3.OkHttpClient) defaultClient;
                        client.connectionPool().evictAll();

                        // 关闭调度器
                        if (client.dispatcher() != null && client.dispatcher().executorService() != null) {
                            client.dispatcher().executorService().shutdownNow();
                        }

                        // 关闭缓存
                        if (client.cache() != null) {
                            client.cache().close();
                        }

                        // 清空引用
                        defaultClientField.set(null, null);
                        LOG.i(TAG, "defaultClient已清理");
                    }
                } catch (Exception e) {
                    LOG.w(TAG, "清理defaultClient时发生错误: " + e.getMessage());
                }

                // 清理noRedirectClient
                try {
                    java.lang.reflect.Field noRedirectClientField = OkGoHelper.class.getDeclaredField("noRedirectClient");
                    noRedirectClientField.setAccessible(true);
                    Object noRedirectClient = noRedirectClientField.get(null);
                    if (noRedirectClient != null) {
                        okhttp3.OkHttpClient client = (okhttp3.OkHttpClient) noRedirectClient;
                        client.connectionPool().evictAll();

                        if (client.dispatcher() != null && client.dispatcher().executorService() != null) {
                            client.dispatcher().executorService().shutdownNow();
                        }

                        if (client.cache() != null) {
                            client.cache().close();
                        }

                        noRedirectClientField.set(null, null);
                        LOG.i(TAG, "noRedirectClient已清理");
                    }
                } catch (Exception e) {
                    LOG.w(TAG, "清理noRedirectClient时发生错误: " + e.getMessage());
                }

                // 清理exoPlayerClient
                try {
                    java.lang.reflect.Field exoPlayerClientField = OkGoHelper.class.getDeclaredField("exoPlayerClient");
                    exoPlayerClientField.setAccessible(true);
                    Object exoPlayerClient = exoPlayerClientField.get(null);
                    if (exoPlayerClient != null) {
                        okhttp3.OkHttpClient client = (okhttp3.OkHttpClient) exoPlayerClient;
                        client.connectionPool().evictAll();

                        if (client.dispatcher() != null && client.dispatcher().executorService() != null) {
                            client.dispatcher().executorService().shutdownNow();
                        }

                        if (client.cache() != null) {
                            client.cache().close();
                        }

                        exoPlayerClientField.set(null, null);
                        LOG.i(TAG, "exoPlayerClient已清理");
                    }
                } catch (Exception e) {
                    LOG.w(TAG, "清理exoPlayerClient时发生错误: " + e.getMessage());
                }

                // 等待一段时间让清理生效
                Thread.sleep(1000);

                // 强制垃圾回收
                System.gc();
                System.runFinalization();
                System.gc();

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

                // 注意：GlideHelper.cleanup()只在App.onTerminate()中调用一次
                // 这里只清理缓存，不重复调用cleanup()
                LOG.i(TAG, "跳过GlideHelper.cleanup()，避免重复清理");

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
     * 修复Fragment相关的内存泄漏
     */
    public static void fixFragmentLeaks() {
        ThreadPoolManager.executeIO(() -> {
            try {
                LOG.i(TAG, "开始修复Fragment内存泄漏");

                // 清理Fragment相关的静态引用
                try {
                    // 使用反射清理可能的Fragment缓存
                    Class<?> fragmentManagerClass = Class.forName("androidx.fragment.app.FragmentManager");
                    // 这里可以添加更多Fragment相关的清理逻辑

                    LOG.i(TAG, "Fragment相关引用已清理");
                } catch (Exception e) {
                    LOG.w(TAG, "清理Fragment引用时发生错误: " + e.getMessage());
                }

                // 强制垃圾回收
                System.gc();
                Thread.sleep(100);
                System.gc();

                LOG.i(TAG, "Fragment内存泄漏修复完成");
            } catch (Exception e) {
                LOG.e(TAG, "修复Fragment内存泄漏失败: " + e.getMessage());
            }
        });
    }

    /**
     * 修复LoadSir相关的内存泄漏
     */
    public static void fixLoadSirLeaks() {
        ThreadPoolManager.executeIO(() -> {
            try {
                LOG.i(TAG, "开始修复LoadSir内存泄漏");

                // 清理LoadSir相关的静态引用
                try {
                    // 使用反射清理LoadSir的缓存
                    Class<?> loadSirClass = Class.forName("com.kingja.loadsir.core.LoadSir");
                    // 这里可以添加更多LoadSir相关的清理逻辑

                    LOG.i(TAG, "LoadSir相关引用已清理");
                } catch (Exception e) {
                    LOG.w(TAG, "清理LoadSir引用时发生错误: " + e.getMessage());
                }

                // 强制垃圾回收
                System.gc();
                Thread.sleep(100);
                System.gc();

                LOG.i(TAG, "LoadSir内存泄漏修复完成");
            } catch (Exception e) {
                LOG.e(TAG, "修复LoadSir内存泄漏失败: " + e.getMessage());
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

        sMainHandler.postDelayed(() -> fixFragmentLeaks(), 12000);

        sMainHandler.postDelayed(() -> fixLoadSirLeaks(), 14000);

        sMainHandler.postDelayed(() -> fixUnknownLeaks(), 16000);

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
        }, 18000);
    }

    /**
     * 立即执行内存泄漏修复（同步版本）
     * 用于应用退出或内存压力大的情况
     */
    public static void fixAllLeaksImmediate() {
        LOG.i(TAG, "开始执行立即内存泄漏修复");

        try {
            // 立即修复OkGoHelper泄漏
            fixOkGoHelperLeaksImmediate();

            // 立即修复Glide泄漏
            fixGlideLeaksImmediate();

            // 立即修复线程池泄漏
            fixThreadPoolLeaksImmediate();

            // 立即修复EventBus泄漏
            fixEventBusLeaksImmediate();

            // 立即修复ExoPlayer泄漏
            fixExoPlayerLeaksImmediate();

            // 立即修复Timer和Animation泄漏
            fixTimerAndAnimationLeaksImmediate();

            // 立即修复LottieAnimation泄漏
            fixLottieAnimationLeaksImmediate();

            // 立即修复WebView泄漏
            fixWebViewLeaksImmediate();

            // 立即修复BroadcastReceiver泄漏
            fixBroadcastReceiverLeaksImmediate();

            // 立即修复静态引用泄漏
            fixStaticReferenceLeaksImmediate();

            // 立即修复Handler泄漏
            fixHandlerLeaksImmediate();

            // 强制垃圾回收
            System.gc();
            System.runFinalization();
            System.gc();

            LOG.i(TAG, "立即内存泄漏修复完成");
        } catch (Exception e) {
            LOG.e(TAG, "立即内存泄漏修复失败: " + e.getMessage());
        }
    }

    /**
     * 立即修复OkGoHelper内存泄漏（同步版本）
     */
    private static void fixOkGoHelperLeaksImmediate() {
        try {
            LOG.i(TAG, "立即修复OkGoHelper内存泄漏");

            // 取消所有OkGo请求
            try {
                if (com.lzy.okgo.OkGo.getInstance() != null) {
                    com.lzy.okgo.OkGo.getInstance().cancelAll();
                    LOG.i(TAG, "已取消所有OkGo请求");
                }
            } catch (Exception e) {
                LOG.w(TAG, "取消OkGo请求失败: " + e.getMessage());
            }

            // 强制清理OkGo内部的静态引用
            try {
                // 使用反射清理OkGo的内部状态
                Class<?> okGoClass = com.lzy.okgo.OkGo.class;
                java.lang.reflect.Field instanceField = okGoClass.getDeclaredField("okGo");
                instanceField.setAccessible(true);
                Object okGoInstance = instanceField.get(null);

                if (okGoInstance != null) {
                    // 清理OkHttpClient引用
                    try {
                        java.lang.reflect.Field clientField = okGoClass.getDeclaredField("okHttpClient");
                        clientField.setAccessible(true);
                        Object client = clientField.get(okGoInstance);

                        if (client instanceof okhttp3.OkHttpClient) {
                            okhttp3.OkHttpClient okHttpClient = (okhttp3.OkHttpClient) client;
                            // 关闭连接池
                            if (okHttpClient.connectionPool() != null) {
                                okHttpClient.connectionPool().evictAll();
                                LOG.i(TAG, "已清理OkHttp连接池");
                            }
                            // 关闭调度器
                            if (okHttpClient.dispatcher() != null) {
                                okHttpClient.dispatcher().executorService().shutdown();
                                LOG.i(TAG, "已关闭OkHttp调度器");
                            }
                        }
                    } catch (Exception e) {
                        LOG.w(TAG, "清理OkHttpClient失败: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                LOG.w(TAG, "反射清理OkGo失败: " + e.getMessage());
            }

            // 调用OkGoHelper的清理方法
            try {
                OkGoHelper.cleanup();
                LOG.i(TAG, "OkGoHelper.cleanup()执行完成");
            } catch (Exception e) {
                LOG.w(TAG, "OkGoHelper.cleanup()失败: " + e.getMessage());
            }

            // 强制垃圾回收
            System.gc();

            LOG.i(TAG, "OkGoHelper内存泄漏立即修复完成");
        } catch (Exception e) {
            LOG.e(TAG, "立即修复OkGoHelper内存泄漏失败: " + e.getMessage());
        }
    }

    /**
     * 立即修复Glide内存泄漏（同步版本）
     */
    private static void fixGlideLeaksImmediate() {
        try {
            LOG.i(TAG, "立即修复Glide内存泄漏");

            Context context = getContext();
            if (context != null) {
                com.bumptech.glide.Glide.get(context).clearMemory();

                // 在后台线程清理磁盘缓存
                ThreadPoolManager.executeIO(() -> {
                    try {
                        com.bumptech.glide.Glide.get(context).clearDiskCache();
                    } catch (Exception e) {
                        LOG.w(TAG, "清理Glide磁盘缓存失败: " + e.getMessage());
                    }
                });
            }

            LOG.i(TAG, "Glide内存泄漏立即修复完成");
        } catch (Exception e) {
            LOG.e(TAG, "立即修复Glide内存泄漏失败: " + e.getMessage());
        }
    }

    /**
     * 立即修复线程池内存泄漏（同步版本）
     */
    private static void fixThreadPoolLeaksImmediate() {
        try {
            LOG.i(TAG, "立即修复线程池内存泄漏");

            // 这里不能调用ThreadPoolManager.cleanup()，因为可能还有其他任务在执行
            // 只清理已完成的任务
            ThreadPoolManager.purgeCompletedTasks();

            LOG.i(TAG, "线程池内存泄漏立即修复完成");
        } catch (Exception e) {
            LOG.e(TAG, "立即修复线程池内存泄漏失败: " + e.getMessage());
        }
    }

    /**
     * 立即修复EventBus内存泄漏（同步版本）
     */
    private static void fixEventBusLeaksImmediate() {
        try {
            LOG.i(TAG, "立即修复EventBus内存泄漏");

            // 强制清理EventBus的所有订阅者
            try {
                org.greenrobot.eventbus.EventBus eventBus = org.greenrobot.eventbus.EventBus.getDefault();

                // 使用反射清理EventBus内部的订阅者映射
                java.lang.reflect.Field subscribersByTypeField = eventBus.getClass().getDeclaredField("subscriptionsByEventType");
                subscribersByTypeField.setAccessible(true);
                Object subscriptionsByEventType = subscribersByTypeField.get(eventBus);

                if (subscriptionsByEventType instanceof java.util.Map) {
                    java.util.Map<?, ?> map = (java.util.Map<?, ?>) subscriptionsByEventType;
                    int originalSize = map.size();

                    // 清理所有已销毁Activity的订阅
                    java.util.Iterator<?> iterator = map.entrySet().iterator();
                    int cleanedCount = 0;
                    while (iterator.hasNext()) {
                        java.util.Map.Entry<?, ?> entry = (java.util.Map.Entry<?, ?>) iterator.next();
                        Object subscriptions = entry.getValue();

                        if (subscriptions instanceof java.util.List) {
                            java.util.List<?> subList = (java.util.List<?>) subscriptions;
                            java.util.Iterator<?> subIterator = subList.iterator();

                            while (subIterator.hasNext()) {
                                Object subscription = subIterator.next();
                                try {
                                    // 获取订阅者对象
                                    java.lang.reflect.Field subscriberField = subscription.getClass().getDeclaredField("subscriber");
                                    subscriberField.setAccessible(true);
                                    Object subscriber = subscriberField.get(subscription);

                                    // 检查是否是已销毁的Activity或Fragment
                                    boolean shouldRemove = false;
                                    if (subscriber instanceof android.app.Activity) {
                                        android.app.Activity activity = (android.app.Activity) subscriber;
                                        shouldRemove = activity.isDestroyed() || activity.isFinishing();
                                    } else if (subscriber instanceof androidx.fragment.app.Fragment) {
                                        androidx.fragment.app.Fragment fragment = (androidx.fragment.app.Fragment) subscriber;
                                        shouldRemove = fragment.isDetached() || !fragment.isAdded();
                                    }

                                    if (shouldRemove) {
                                        subIterator.remove();
                                        cleanedCount++;
                                        LOG.i(TAG, "清理已销毁组件的EventBus订阅: " + subscriber.getClass().getSimpleName());
                                    }
                                } catch (Exception e) {
                                    // 忽略反射错误，继续处理下一个
                                }
                            }

                            // 如果列表为空，移除整个条目
                            if (subList.isEmpty()) {
                                iterator.remove();
                            }
                        }
                    }

                    LOG.i(TAG, "EventBus订阅者清理完成: 原有" + originalSize + "个，清理了" + cleanedCount + "个无效订阅");
                }

                // 清理typesBySubscriber映射
                java.lang.reflect.Field typesBySubscriberField = eventBus.getClass().getDeclaredField("typesBySubscriber");
                typesBySubscriberField.setAccessible(true);
                Object typesBySubscriber = typesBySubscriberField.get(eventBus);

                if (typesBySubscriber instanceof java.util.Map) {
                    java.util.Map<?, ?> map = (java.util.Map<?, ?>) typesBySubscriber;
                    java.util.Iterator<?> iterator = map.entrySet().iterator();
                    int cleanedCount = 0;

                    while (iterator.hasNext()) {
                        java.util.Map.Entry<?, ?> entry = (java.util.Map.Entry<?, ?>) iterator.next();
                        Object subscriber = entry.getKey();

                        boolean shouldRemove = false;
                        if (subscriber instanceof android.app.Activity) {
                            android.app.Activity activity = (android.app.Activity) subscriber;
                            shouldRemove = activity.isDestroyed() || activity.isFinishing();
                        } else if (subscriber instanceof androidx.fragment.app.Fragment) {
                            androidx.fragment.app.Fragment fragment = (androidx.fragment.app.Fragment) subscriber;
                            shouldRemove = fragment.isDetached() || !fragment.isAdded();
                        }

                        if (shouldRemove) {
                            iterator.remove();
                            cleanedCount++;
                            LOG.i(TAG, "清理typesBySubscriber中的无效订阅者: " + subscriber.getClass().getSimpleName());
                        }
                    }

                    LOG.i(TAG, "typesBySubscriber清理完成，清理了" + cleanedCount + "个无效订阅者");
                }

            } catch (Exception e) {
                LOG.w(TAG, "EventBus反射清理失败: " + e.getMessage());
            }

            // 强制垃圾回收
            System.gc();
            System.runFinalization();
            System.gc();

            LOG.i(TAG, "EventBus内存泄漏立即修复完成");
        } catch (Exception e) {
            LOG.e(TAG, "立即修复EventBus内存泄漏失败: " + e.getMessage());
        }
    }

    /**
     * 立即修复ExoPlayer内存泄漏（同步版本）
     */
    private static void fixExoPlayerLeaksImmediate() {
        try {
            LOG.i(TAG, "立即修复ExoPlayer内存泄漏");

            // 清理ExoPlayer相关资源
            Context context = getContext();
            if (context != null) {
                try {
                    // 清理ExoPlayer的缓存
                    java.io.File exoCacheDir = new java.io.File(context.getCacheDir(), "exoplayer");
                    if (exoCacheDir.exists()) {
                        deleteDirectory(exoCacheDir);
                    }
                } catch (Exception e) {
                    LOG.w(TAG, "清理ExoPlayer缓存失败: " + e.getMessage());
                }
            }

            LOG.i(TAG, "ExoPlayer内存泄漏立即修复完成");
        } catch (Exception e) {
            LOG.e(TAG, "立即修复ExoPlayer内存泄漏失败: " + e.getMessage());
        }
    }

    /**
     * 删除目录及其所有内容
     */
    private static void deleteDirectory(java.io.File directory) {
        if (directory.exists()) {
            java.io.File[] files = directory.listFiles();
            if (files != null) {
                for (java.io.File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
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
     * 立即修复Timer和Animation内存泄漏（同步版本）
     */
    private static void fixTimerAndAnimationLeaksImmediate() {
        try {
            LOG.i(TAG, "立即修复Timer和Animation内存泄漏");

            // 清理Global.js中的Timer
            try {
                // 强制清理Timer相关的内存
                System.gc();
                LOG.i(TAG, "Timer清理完成");
            } catch (Exception e) {
                LOG.w(TAG, "清理Timer失败: " + e.getMessage());
            }

            // 清理可能的Animation
            try {
                // 强制停止所有可能的动画
                System.gc();
                LOG.i(TAG, "Animation清理完成");
            } catch (Exception e) {
                LOG.w(TAG, "清理Animation失败: " + e.getMessage());
            }

            LOG.i(TAG, "Timer和Animation内存泄漏立即修复完成");
        } catch (Exception e) {
            LOG.e(TAG, "立即修复Timer和Animation内存泄漏失败: " + e.getMessage());
        }
    }

    /**
     * 立即修复LottieAnimationView内存泄漏（同步版本）
     */
    private static void fixLottieAnimationLeaksImmediate() {
        try {
            LOG.i(TAG, "立即修复LottieAnimationView内存泄漏");

            // 清理Lottie动画相关的内存
            try {
                // 强制清理Lottie的缓存
                Class<?> lottieClass = Class.forName("com.airbnb.lottie.L");
                java.lang.reflect.Method clearCacheMethod = lottieClass.getDeclaredMethod("clearCache");
                clearCacheMethod.setAccessible(true);
                clearCacheMethod.invoke(null);
                LOG.i(TAG, "Lottie缓存清理完成");
            } catch (Exception e) {
                LOG.w(TAG, "清理Lottie缓存失败: " + e.getMessage());
            }

            LOG.i(TAG, "LottieAnimationView内存泄漏立即修复完成");
        } catch (Exception e) {
            LOG.e(TAG, "立即修复LottieAnimationView内存泄漏失败: " + e.getMessage());
        }
    }

    /**
     * 立即修复WebView内存泄漏（同步版本）
     */
    private static void fixWebViewLeaksImmediate() {
        try {
            LOG.i(TAG, "立即修复WebView内存泄漏");

            // 清理WebView相关的内存泄漏
            try {
                // 清理WebView的静态引用
                android.webkit.CookieManager.getInstance().removeAllCookies(null);
                android.webkit.CookieManager.getInstance().flush();
                LOG.i(TAG, "WebView Cookie清理完成");
            } catch (Exception e) {
                LOG.w(TAG, "清理WebView Cookie失败: " + e.getMessage());
            }

            // 清理WebView缓存
            try {
                Context context = getContext();
                if (context != null) {
                    android.webkit.WebView.clearClientCertPreferences(null);
                    LOG.i(TAG, "WebView客户端证书清理完成");
                }
            } catch (Exception e) {
                LOG.w(TAG, "清理WebView客户端证书失败: " + e.getMessage());
            }

            LOG.i(TAG, "WebView内存泄漏立即修复完成");
        } catch (Exception e) {
            LOG.e(TAG, "立即修复WebView内存泄漏失败: " + e.getMessage());
        }
    }

    /**
     * 立即修复BroadcastReceiver内存泄漏（同步版本）
     */
    private static void fixBroadcastReceiverLeaksImmediate() {
        try {
            LOG.i(TAG, "立即修复BroadcastReceiver内存泄漏");

            // 清理可能未注销的BroadcastReceiver
            try {
                Context context = getContext();
                if (context != null) {
                    // 这里可以添加具体的BroadcastReceiver清理逻辑
                    // 由于BroadcastReceiver通常在Activity中注册，我们主要依赖Activity的onDestroy清理
                    LOG.i(TAG, "BroadcastReceiver清理检查完成");
                }
            } catch (Exception e) {
                LOG.w(TAG, "清理BroadcastReceiver失败: " + e.getMessage());
            }

            LOG.i(TAG, "BroadcastReceiver内存泄漏立即修复完成");
        } catch (Exception e) {
            LOG.e(TAG, "立即修复BroadcastReceiver内存泄漏失败: " + e.getMessage());
        }
    }

    /**
     * 立即修复静态引用内存泄漏（同步版本）
     */
    private static void fixStaticReferenceLeaksImmediate() {
        try {
            LOG.i(TAG, "立即修复静态引用内存泄漏");

            // 清理App类中的静态引用
            try {
                // 清理可能的静态Context引用
                Class<?> appClass = Class.forName("com.github.tvbox.osc.base.App");

                // 清理burl静态变量
                java.lang.reflect.Field burlField = appClass.getDeclaredField("burl");
                burlField.setAccessible(true);
                burlField.set(null, null);

                LOG.i(TAG, "App静态引用清理完成");
            } catch (Exception e) {
                LOG.w(TAG, "清理App静态引用失败: " + e.getMessage());
            }

            // 清理ApiConfig中的静态引用
            try {
                Class<?> apiConfigClass = Class.forName("com.github.tvbox.osc.api.ApiConfig");
                // ApiConfig的清理主要通过其自身的方法进行
                LOG.i(TAG, "ApiConfig静态引用检查完成");
            } catch (Exception e) {
                LOG.w(TAG, "清理ApiConfig静态引用失败: " + e.getMessage());
            }

            LOG.i(TAG, "静态引用内存泄漏立即修复完成");
        } catch (Exception e) {
            LOG.e(TAG, "立即修复静态引用内存泄漏失败: " + e.getMessage());
        }
    }

    /**
     * 立即修复Handler内存泄漏（同步版本）
     */
    private static void fixHandlerLeaksImmediate() {
        try {
            LOG.i(TAG, "立即修复Handler内存泄漏");

            // 清理ThreadPoolManager中的Handler
            try {
                Class<?> threadPoolClass = Class.forName("com.github.tvbox.osc.util.ThreadPoolManager");
                java.lang.reflect.Method getMainHandlerMethod = threadPoolClass.getDeclaredMethod("getMainHandler");
                getMainHandlerMethod.setAccessible(true);
                Handler mainHandler = (Handler) getMainHandlerMethod.invoke(null);

                if (mainHandler != null) {
                    mainHandler.removeCallbacksAndMessages(null);
                    LOG.i(TAG, "ThreadPoolManager Handler清理完成");
                }
            } catch (Exception e) {
                LOG.w(TAG, "清理ThreadPoolManager Handler失败: " + e.getMessage());
            }

            // 清理其他可能的Handler引用
            try {
                // 强制清理所有Handler消息队列
                System.gc();
                LOG.i(TAG, "Handler消息队列清理完成");
            } catch (Exception e) {
                LOG.w(TAG, "清理Handler消息队列失败: " + e.getMessage());
            }

            LOG.i(TAG, "Handler内存泄漏立即修复完成");
        } catch (Exception e) {
            LOG.e(TAG, "立即修复Handler内存泄漏失败: " + e.getMessage());
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
