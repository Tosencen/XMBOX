package com.github.tvbox.osc.util.urlhttp;

import com.github.tvbox.osc.util.ThreadPoolManager;

/**
 * 线程管理类
 * 使用ThreadPoolManager统一管理线程池
 */
public class ThreadsManager {

    /**
     * 初始化
     * 此方法保留以兼容现有代码，但实际上不需要调用
     */
    public static void init() {
        // 不需要初始化，使用ThreadPoolManager
    }

    /**
     * 清除所有任务
     */
    public static void clear() {
        // 不需要实现，由ThreadPoolManager统一管理
    }

    /**
     * 提交任务
     * @param runnable 要执行的任务
     * @return 任务ID
     */
    public static Integer post(Runnable runnable) {
        return ThreadPoolManager.executeIO(runnable);
    }

    /**
     * 停止任务
     * @param taskId 任务ID
     */
    public static void stop(Integer taskId) {
        ThreadPoolManager.cancelTask(taskId);
    }
}
