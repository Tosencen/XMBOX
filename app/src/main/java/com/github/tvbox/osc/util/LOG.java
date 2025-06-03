package com.github.tvbox.osc.util;

import android.util.Log;

import com.github.tvbox.osc.event.LogEvent;
import com.orhanobut.hawk.Hawk;
import com.github.tvbox.osc.util.HawkConfig;

import org.greenrobot.eventbus.EventBus;

/**
 * @author pj567
 * @date :2020/12/18
 * @description:
 */
public class LOG {
    private static String TAG = "TVBox";

    /**
     * 检查是否处于调试模式
     * @return 是否处于调试模式
     */
    public static boolean isDebug() {
        return Hawk.get(HawkConfig.DEBUG_OPEN, false);
    }

    public static void e(Throwable t) {
        Log.e(TAG, t.getMessage(), t);
        EventBus.getDefault().post(new LogEvent(String.format("E/%s ==> ", TAG) + Log.getStackTraceString(t)));
    }

    public static void e(String tag, Throwable t) {
        Log.e(tag, t.getMessage(), t);
        EventBus.getDefault().post(new LogEvent(String.format("E/%s ==> ", tag) + Log.getStackTraceString(t)));
    }

    public static void e(String msg) {
        Log.e(TAG, "" + msg);
        EventBus.getDefault().post(new LogEvent(String.format("E/%s ==> ", TAG) + msg));
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
        EventBus.getDefault().post(new LogEvent(String.format("E/%s ==> ", tag) + msg));
    }

    public static void i(String msg) {
        Log.i(TAG, msg);
        EventBus.getDefault().post(new LogEvent(String.format("I/%s ==> ", TAG) + msg));
    }

    public static void i(String tag, String msg) {
        Log.i(tag, msg);
        EventBus.getDefault().post(new LogEvent(String.format("I/%s ==> ", tag) + msg));
    }

    public static void w(String msg) {
        Log.w(TAG, msg);
        EventBus.getDefault().post(new LogEvent(String.format("W/%s ==> ", TAG) + msg));
    }

    public static void w(String tag, String msg) {
        Log.w(tag, msg);
        EventBus.getDefault().post(new LogEvent(String.format("W/%s ==> ", tag) + msg));
    }

    /**
     * 调试日志，仅在调试模式下输出
     * @param msg 日志信息
     */
    public static void d(String msg) {
        if (isDebug()) {
            Log.d(TAG, msg);
            EventBus.getDefault().post(new LogEvent(String.format("D/%s ==> ", TAG) + msg));
        }
    }

    /**
     * 调试日志，仅在调试模式下输出
     * @param tag 标签
     * @param msg 日志信息
     */
    public static void d(String tag, String msg) {
        if (isDebug()) {
            Log.d(tag, msg);
            EventBus.getDefault().post(new LogEvent(String.format("D/%s ==> ", tag) + msg));
        }
    }
}