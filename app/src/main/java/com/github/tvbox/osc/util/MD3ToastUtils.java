package com.github.tvbox.osc.util;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;

import com.xmbox.app.R;
import com.github.tvbox.osc.base.App;

import java.lang.ref.WeakReference;

/**
 * Material Design 3风格的Toast工具类
 */
public class MD3ToastUtils {

    private static WeakReference<Toast> mToastRef;

    // 定义固定的颜色值
    @ColorInt private static final int TOAST_BACKGROUND_COLOR = 0xFF232626; // 深灰色背景
    @ColorInt private static final int TOAST_TEXT_COLOR = 0xFFD2D4D4; // 浅灰色文字

    /**
     * 显示短时间Toast
     * @param message 消息内容
     */
    public static void showToast(String message) {
        showToast(App.getInstance(), message, Toast.LENGTH_SHORT, 0);
    }

    /**
     * 显示带图标的短时间Toast
     * @param message 消息内容
     * @param iconResId 图标资源ID
     */
    public static void showToast(String message, int iconResId) {
        showToast(App.getInstance(), message, Toast.LENGTH_SHORT, iconResId);
    }

    /**
     * 显示长时间Toast
     * @param message 消息内容
     */
    public static void showLongToast(String message) {
        showToast(App.getInstance(), message, Toast.LENGTH_LONG, 0);
    }

    /**
     * 显示带图标的长时间Toast
     * @param message 消息内容
     * @param iconResId 图标资源ID
     */
    public static void showLongToast(String message, int iconResId) {
        showToast(App.getInstance(), message, Toast.LENGTH_LONG, iconResId);
    }

    /**
     * 显示已复制Toast
     * @param message 消息内容
     */
    public static void showCopiedToast(String message) {
        showToast(App.getInstance(), message, Toast.LENGTH_SHORT, R.drawable.ic_check_circle_filled);
    }

    /**
     * 显示已复制Toast，默认显示"已复制"
     */
    public static void showCopiedToast() {
        showCopiedToast("已复制");
    }

    /**
     * 显示自定义Toast
     * @param context 上下文
     * @param message 消息内容
     * @param duration 显示时长
     * @param iconResId 图标资源ID，0表示不显示图标
     */
    private static void showToast(final Context context, final String message, final int duration, final int iconResId) {
        // 确保在主线程上运行
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            // 已经在主线程上，直接显示Toast
            showToastOnMainThread(context, message, duration, iconResId);
        } else {
            // 在后台线程上，需要切换到主线程
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> {
                try {
                    showToastOnMainThread(context, message, duration, iconResId);
                } catch (Exception e) {
                    // 如果显示Toast失败，使用最简单的方式
                    try {
                        Toast.makeText(context.getApplicationContext(), message, duration).show();
                    } catch (Exception ignored) {}
                }
            });
        }
    }

    private static void showToastOnMainThread(Context context, String message, int duration, int iconResId) {
        try {
            // 取消之前的Toast，避免内存泄漏
            Toast previousToast = mToastRef != null ? mToastRef.get() : null;
            if (previousToast != null) {
                previousToast.cancel();
            }
            mToastRef = null; // 清空引用

            // 使用ApplicationContext避免内存泄漏
            Context appContext = context.getApplicationContext();
            Toast newToast = null;

            // 尝试使用自定义布局
            try {
                View view = LayoutInflater.from(appContext).inflate(R.layout.toast_md3, null);
                TextView textView = view.findViewById(R.id.toast_text);
                ImageView iconView = view.findViewById(R.id.toast_icon);

                textView.setText(message);

                // 设置图标
                if (iconResId != 0) {
                    iconView.setImageResource(iconResId);
                    iconView.setVisibility(View.VISIBLE);
                } else {
                    iconView.setVisibility(View.GONE);
                }

                newToast = new Toast(appContext);
                newToast.setDuration(duration);
                newToast.setView(view);
                newToast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 120);
            } catch (Exception e) {
                // 如果自定义布局失败，使用系统Toast
                newToast = Toast.makeText(appContext, message, duration);
                newToast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 120);

                // 尝试为系统 Toast 设置背景和文字颜色
                try {
                    View toastView = newToast.getView();
                    if (toastView != null) {
                        toastView.setBackgroundColor(TOAST_BACKGROUND_COLOR);
                        TextView textView = toastView.findViewById(android.R.id.message);
                        if (textView != null) {
                            textView.setTextColor(TOAST_TEXT_COLOR);
                        }
                    }
                } catch (Exception ignored) {}
            }

            // 显示Toast并保存弱引用
            if (newToast != null) {
                mToastRef = new WeakReference<>(newToast);
                newToast.show();
            }
        } catch (Exception e) {
            // 最后的安全网，使用最简单的Toast
            try {
                Context appContext = context.getApplicationContext();
                Toast toast = Toast.makeText(appContext, message, duration);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 120);
                toast.show();
            } catch (Exception ignored) {}
        }
    }

    /**
     * 清理Toast引用，防止内存泄漏
     * 建议在Application的onTerminate()中调用
     */
    public static void cleanup() {
        try {
            Toast toast = mToastRef != null ? mToastRef.get() : null;
            if (toast != null) {
                toast.cancel();
            }
            mToastRef = null;
        } catch (Exception e) {
            // 忽略清理时的异常
        }
    }
}
