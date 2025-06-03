package com.github.tvbox.osc.util;

import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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

    // 任务优先级定义
    public static final int PRIORITY_HIGHEST = 0;
    public static final int PRIORITY_HIGH = 1;
    public static final int PRIORITY_NORMAL = 2;
    public static final int PRIORITY_LOW = 3;
    public static final int PRIORITY_LOWEST = 4;

    @IntDef({PRIORITY_HIGHEST, PRIORITY_HIGH, PRIORITY_NORMAL, PRIORITY_LOW, PRIORITY_LOWEST})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TaskPriority {}

    // 任务序列号生成器
    private static final AtomicLong SEQUENCE_GENERATOR = new AtomicLong(0);

    // 任务映射表，用于取消任务
    private static final Map<Integer, Future<?>> TASK_MAP = new ConcurrentHashMap<>();

    // IO密集型线程池
    private static volatile ThreadPoolExecutor sIOThreadPool;
    // 计算密集型线程池
    private static volatile ThreadPoolExecutor sComputeThreadPool;
    // 优先级线程池
    private static volatile ThreadPoolExecutor sPriorityThreadPool;
    // 主线程Handler
    private static volatile Handler sMainHandler;

    // 清理标志
    private static volatile boolean isCleaningUp = false;

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
     * 获取优先级线程池（用于需要按优先级执行的任务）
     */
    public static ThreadPoolExecutor getPriorityThreadPool() {
        if (sPriorityThreadPool == null || sPriorityThreadPool.isShutdown()) {
            synchronized (ThreadPoolManager.class) {
                if (sPriorityThreadPool == null || sPriorityThreadPool.isShutdown()) {
                    sPriorityThreadPool = new ThreadPoolExecutor(
                            CORE_POOL_SIZE,
                            MAXIMUM_POOL_SIZE,
                            KEEP_ALIVE_TIME,
                            TimeUnit.SECONDS,
                            new PriorityBlockingQueue<Runnable>(),
                            new PriorityThreadFactory(),
                            new RejectedHandler());
                    // 允许核心线程超时回收
                    sPriorityThreadPool.allowCoreThreadTimeOut(true);
                }
            }
        }
        return sPriorityThreadPool;
    }

    /**
     * 在IO线程池执行任务
     * @return 任务ID，可用于取消任务
     */
    public static int executeIO(Runnable runnable) {
        if (runnable != null) {
            Future<?> future = getIOThreadPool().submit(runnable);
            int taskId = future.hashCode();
            TASK_MAP.put(taskId, future);
            return taskId;
        }
        return -1;
    }

    /**
     * 在IO线程池执行任务，并指定优先级
     * @param runnable 要执行的任务
     * @param threadPriority 线程优先级，如Thread.MIN_PRIORITY
     * @return 任务ID，可用于取消任务
     */
    public static int executeIOWithPriority(Runnable runnable, int threadPriority) {
        if (runnable != null) {
            final Runnable priorityRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        // 设置线程优先级
                        Thread.currentThread().setPriority(threadPriority);
                        runnable.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            Future<?> future = getIOThreadPool().submit(priorityRunnable);
            int taskId = future.hashCode();
            TASK_MAP.put(taskId, future);
            return taskId;
        }
        return -1;
    }

    /**
     * 使用低优先级执行图片加载任务
     * @param runnable 要执行的任务
     * @return 任务ID，可用于取消任务
     */
    public static int executeImageLoading(Runnable runnable) {
        return executeIOWithPriority(runnable, Thread.MIN_PRIORITY);
    }

    /**
     * 在计算线程池执行任务
     * @return 任务ID，可用于取消任务
     */
    public static int executeCompute(Runnable runnable) {
        if (runnable != null) {
            Future<?> future = getComputeThreadPool().submit(runnable);
            int taskId = future.hashCode();
            TASK_MAP.put(taskId, future);
            return taskId;
        }
        return -1;
    }

    /**
     * 按优先级执行任务
     * @param runnable 要执行的任务
     * @param taskPriority 任务优先级，使用PRIORITY_*常量
     * @return 任务ID，可用于取消任务
     */
    public static int executePriority(Runnable runnable, @TaskPriority int taskPriority) {
        if (runnable != null) {
            PriorityRunnable priorityRunnable = new PriorityRunnable(runnable, taskPriority);
            Future<?> future = getPriorityThreadPool().submit(priorityRunnable);
            int taskId = future.hashCode();
            TASK_MAP.put(taskId, future);
            return taskId;
        }
        return -1;
    }

    /**
     * 取消任务
     * @param taskId 任务ID
     * @return 是否成功取消
     */
    public static boolean cancelTask(int taskId) {
        Future<?> future = TASK_MAP.remove(taskId);
        if (future != null && !future.isDone() && !future.isCancelled()) {
            return future.cancel(true);
        }
        return false;
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
        // 取消所有任务
        for (Future<?> future : TASK_MAP.values()) {
            if (future != null && !future.isDone() && !future.isCancelled()) {
                future.cancel(true);
            }
        }
        TASK_MAP.clear();

        if (sIOThreadPool != null && !sIOThreadPool.isShutdown()) {
            sIOThreadPool.shutdown();
            sIOThreadPool = null;
        }
        if (sComputeThreadPool != null && !sComputeThreadPool.isShutdown()) {
            sComputeThreadPool.shutdown();
            sComputeThreadPool = null;
        }
        if (sPriorityThreadPool != null && !sPriorityThreadPool.isShutdown()) {
            sPriorityThreadPool.shutdown();
            sPriorityThreadPool = null;
        }
    }

    /**
     * 优先级任务包装类
     */
    private static class PriorityRunnable implements Runnable, Comparable<PriorityRunnable> {
        private final Runnable runnable;
        private final int priority;
        private final long sequence;

        public PriorityRunnable(Runnable runnable, @TaskPriority int priority) {
            this.runnable = runnable;
            this.priority = priority;
            this.sequence = SEQUENCE_GENERATOR.getAndIncrement();
        }

        @Override
        public void run() {
            try {
                // 设置线程优先级
                int threadPriority;
                switch (priority) {
                    case PRIORITY_HIGHEST:
                        threadPriority = Process.THREAD_PRIORITY_URGENT_DISPLAY;
                        break;
                    case PRIORITY_HIGH:
                        threadPriority = Process.THREAD_PRIORITY_DISPLAY;
                        break;
                    case PRIORITY_LOW:
                        threadPriority = Process.THREAD_PRIORITY_BACKGROUND;
                        break;
                    case PRIORITY_LOWEST:
                        threadPriority = Process.THREAD_PRIORITY_LOWEST;
                        break;
                    case PRIORITY_NORMAL:
                    default:
                        threadPriority = Process.THREAD_PRIORITY_DEFAULT;
                        break;
                }
                Process.setThreadPriority(threadPriority);

                // 执行任务
                runnable.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public int compareTo(PriorityRunnable other) {
            // 优先级越小，优先级越高
            if (this.priority < other.priority) {
                return -1;
            } else if (this.priority > other.priority) {
                return 1;
            } else {
                // 优先级相同时，按提交顺序执行
                return Long.compare(this.sequence, other.sequence);
            }
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
     * 优先级线程工厂
     */
    private static class PriorityThreadFactory implements ThreadFactory {
        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread thread = new Thread(r, "TVBox-Priority-" + mCount.getAndIncrement());
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

    /**
     * 清理所有线程池资源，防止内存泄漏
     * 建议在Application的onTerminate()中调用
     */
    public static synchronized void cleanup() {
        if (isCleaningUp) {
            return;
        }

        isCleaningUp = true;

        try {
            // 取消所有未完成的任务
            for (Future<?> future : TASK_MAP.values()) {
                if (future != null && !future.isDone() && !future.isCancelled()) {
                    future.cancel(true);
                }
            }
            TASK_MAP.clear();

            // 关闭IO线程池
            shutdownThreadPool("IO", sIOThreadPool);
            sIOThreadPool = null;

            // 关闭计算线程池
            shutdownThreadPool("Compute", sComputeThreadPool);
            sComputeThreadPool = null;

            // 关闭优先级线程池
            shutdownThreadPool("Priority", sPriorityThreadPool);
            sPriorityThreadPool = null;

            // 清理主线程Handler
            if (sMainHandler != null) {
                sMainHandler.removeCallbacksAndMessages(null);
                sMainHandler = null;
            }

        } catch (Exception e) {
            android.util.Log.e("ThreadPoolManager", "清理线程池时发生错误: " + e.getMessage());
        } finally {
            isCleaningUp = false;
        }
    }

    /**
     * 安全关闭线程池
     */
    private static void shutdownThreadPool(String name, ThreadPoolExecutor executor) {
        if (executor != null && !executor.isShutdown()) {
            try {
                executor.shutdown();
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                    if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                        android.util.Log.w("ThreadPoolManager", name + "线程池未能在规定时间内关闭");
                    }
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                android.util.Log.e("ThreadPoolManager", "关闭" + name + "线程池时发生错误: " + e.getMessage());
            }
        }
    }
}
