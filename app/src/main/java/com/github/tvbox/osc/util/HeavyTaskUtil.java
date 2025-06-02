package com.github.tvbox.osc.util;

import java.util.concurrent.ExecutorService;

/**
 * 重任务工具类
 * 使用ThreadPoolManager统一管理线程池
 */
public class HeavyTaskUtil {

    /**
     * 执行新任务
     * @param command 要执行的任务
     * @return 任务ID，可用于取消任务
     */
    public static int executeNewTask(Runnable command) {
        return ThreadPoolManager.executeCompute(command);
    }

    /**
     * 执行大型任务
     * @param command 要执行的任务
     * @return 任务ID，可用于取消任务
     */
    public static int executeBigTask(Runnable command) {
        return ThreadPoolManager.executePriority(command, ThreadPoolManager.PRIORITY_LOW);
    }

    /**
     * 获取大型任务执行服务
     * @return 线程池执行器
     */
    public static ExecutorService getBigTaskExecutorService() {
        return ThreadPoolManager.getComputeThreadPool();
    }

    /**
     * 取消任务
     * @param taskId 任务ID
     * @return 是否成功取消
     */
    public static boolean cancelTask(int taskId) {
        return ThreadPoolManager.cancelTask(taskId);
    }
}
