package com.github.tvbox.osc.util;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池管理类
 * 优化应用中的线程使用，避免线程滥用导致的性能问题
 */
public class ThreadPoolManager {
    // CPU核心数
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    // 核心线程数 = CPU核心数 + 1，但不少于2，不多于6
    private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT + 1, 6));
    // 最大线程数 = CPU核心数 * 2 + 2
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 2;
    // 非核心线程闲置超时时间
    private static final long KEEP_ALIVE_TIME = 30L;
    // 线程优先级
    private static final int THREAD_PRIORITY = Thread.NORM_PRIORITY - 1;

    // IO密集型线程池
    private static ThreadPoolExecutor sIOThreadPool;
    // 计算密集型线程池
    private static ThreadPoolExecutor sComputeThreadPool;
    // 主线程Handler
    private static Handler sMainHandler;

    /**
     * 获取IO密集型线程池（用于网络请求、文件操作等IO操作）
     */
    public static ThreadPoolExecutor getIOThreadPool() {
        if (sIOThreadPool == null || sIOThreadPool.isShutdown()) {
            synchronized (ThreadPoolManager.class) {
                if (sIOThreadPool == null || sIOThreadPool.isShutdown()) {
                    sIOThreadPool = new ThreadPoolExecutor(
                            CORE_POOL_SIZE,
                            MAXIMUM_POOL_SIZE * 2, // IO线程池允许更多线程
                            KEEP_ALIVE_TIME,
                            TimeUnit.SECONDS,
                            new LinkedBlockingQueue<>(512),
                            new IOThreadFactory(),
                            new RejectedHandler());
                    // 允许核心线程超时回收
                    sIOThreadPool.allowCoreThreadTimeOut(true);
                }
            }
        }
        return sIOThreadPool;
    }

    /**
     * 获取计算密集型线程池（用于复杂计算、图片处理等CPU密集操作）
     */
    public static ThreadPoolExecutor getComputeThreadPool() {
        if (sComputeThreadPool == null || sComputeThreadPool.isShutdown()) {
            synchronized (ThreadPoolManager.class) {
                if (sComputeThreadPool == null || sComputeThreadPool.isShutdown()) {
                    sComputeThreadPool = new ThreadPoolExecutor(
                            CORE_POOL_SIZE,
                            MAXIMUM_POOL_SIZE,
                            KEEP_ALIVE_TIME,
                            TimeUnit.SECONDS,
                            new LinkedBlockingQueue<>(128),
                            new ComputeThreadFactory(),
                            new RejectedHandler());
                    // 允许核心线程超时回收
                    sComputeThreadPool.allowCoreThreadTimeOut(true);
                }
            }
        }
        return sComputeThreadPool;
    }

    /**
     * 在IO线程池执行任务
     */
    public static void executeIO(Runnable runnable) {
        if (runnable != null) {
            getIOThreadPool().execute(runnable);
        }
    }

    /**
     * 在IO线程池执行任务，并指定优先级
     * @param runnable 要执行的任务
     * @param priority 线程优先级，如Thread.MIN_PRIORITY
     */
    public static void executeIOWithPriority(Runnable runnable, int priority) {
        if (runnable != null) {
            final Runnable priorityRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        // 设置线程优先级
                        Thread.currentThread().setPriority(priority);
                        runnable.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            getIOThreadPool().execute(priorityRunnable);
        }
    }

    /**
     * 使用低优先级执行图片加载任务
     * @param runnable 要执行的任务
     */
    public static void executeImageLoading(Runnable runnable) {
        executeIOWithPriority(runnable, Thread.MIN_PRIORITY);
    }

    /**
     * 在计算线程池执行任务
     */
    public static void executeCompute(Runnable runnable) {
        if (runnable != null) {
            getComputeThreadPool().execute(runnable);
        }
    }

    /**
     * 在主线程执行任务
     */
    public static void executeMain(Runnable runnable) {
        if (runnable != null) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                runnable.run();
            } else {
                getMainHandler().post(runnable);
            }
        }
    }

    /**
     * 在主线程延迟执行任务
     */
    public static void executeMainDelayed(Runnable runnable, long delayMillis) {
        if (runnable != null) {
            getMainHandler().postDelayed(runnable, delayMillis);
        }
    }

    /**
     * 获取主线程Handler
     */
    public static Handler getMainHandler() {
        if (sMainHandler == null) {
            synchronized (ThreadPoolManager.class) {
                if (sMainHandler == null) {
                    sMainHandler = new Handler(Looper.getMainLooper());
                }
            }
        }
        return sMainHandler;
    }

    /**
     * 关闭线程池
     */
    public static void shutdown() {
        if (sIOThreadPool != null && !sIOThreadPool.isShutdown()) {
            sIOThreadPool.shutdown();
            sIOThreadPool = null;
        }
        if (sComputeThreadPool != null && !sComputeThreadPool.isShutdown()) {
            sComputeThreadPool.shutdown();
            sComputeThreadPool = null;
        }
    }

    /**
     * IO线程工厂
     */
    private static class IOThreadFactory implements ThreadFactory {
        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread thread = new Thread(r, "TVBox-IO-" + mCount.getAndIncrement());
            thread.setPriority(THREAD_PRIORITY);
            return thread;
        }
    }

    /**
     * 计算线程工厂
     */
    private static class ComputeThreadFactory implements ThreadFactory {
        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread thread = new Thread(r, "TVBox-Compute-" + mCount.getAndIncrement());
            thread.setPriority(THREAD_PRIORITY);
            return thread;
        }
    }

    /**
     * 拒绝策略处理器
     */
    private static class RejectedHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            // 使用调用者所在的线程执行任务
            if (!executor.isShutdown()) {
                r.run();
            }
        }
    }
}
