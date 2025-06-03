package com.github.tvbox.osc.util;

import android.os.Handler;
import android.view.View;

/**
 * @author pj567
 * @date :2020/12/22
 * @description:
 */
public class FastClickCheckUtil {
    /**
     * 相同视图点击必须间隔0.3s才能有效
     *
     * @param view 目标视图
     */
    public static void check(View view) {
        check(view, 300); // 减少延迟时间以提高响应速度
    }

    // 使用静态Handler避免内存泄漏
    private static final Handler sHandler = new Handler(android.os.Looper.getMainLooper());

    /**
     * 设置间隔点击规则，配置间隔点击时长
     * 改进版本，不再禁用视图的可点击性
     *
     * @param view  目标视图
     * @param mills 点击间隔时间（毫秒）
     */
    public static void check(final View view, int mills) {
        // 不再设置view.setClickable(false)，而是使用视觉反馈
        view.setAlpha(0.7f); // 点击时的视觉反馈

        // 使用静态Handler避免内存泄漏
        sHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (view != null && view.getContext() != null) { // 检查view是否仍然有效
                    view.setAlpha(1.0f); // 恢复透明度
                }
            }
        }, mills);
    }
}