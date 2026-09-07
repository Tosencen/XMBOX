package com.fongmi.android.tv.utils;

import android.Manifest;
import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationManagerCompat;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ViewProgressBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class Notify {

    public static final String DEFAULT = "default";
    public static final int ID = 9527;
    private AlertDialog mDialog;
    private Toast mToast;
    private Handler mHandler;
    // 防止同一轻提示在短时间内重复显示（例如播放时同一提示连弹两遍）
    private String mLastToastText;
    private long mLastToastTime;

    private static class Loader {
        static volatile Notify INSTANCE = new Notify();
    }

    private static Notify get() {
        return Loader.INSTANCE;
    }

    public static void createChannel() {
        NotificationManagerCompat notifyMgr = NotificationManagerCompat.from(App.get());
        notifyMgr.createNotificationChannel(new NotificationChannelCompat.Builder(DEFAULT, NotificationManagerCompat.IMPORTANCE_LOW).setName("XMBOX").build());
    }

    public static String getError(int resId, Throwable e) {
        if (TextUtils.isEmpty(e.getMessage())) return ResUtil.getString(resId);
        return ResUtil.getString(resId) + "\n" + e.getMessage();
    }

    public static void show(Notification notification) {
        // 通知权限的的申请统一放在 PlaybackService.start() 中处理，
        // 这里只做静默判断，未授权时不弹窗、不显示通知，避免播放过程中反复弹权限请求。
        if (Build.VERSION.SDK_INT >= 33 && ActivityCompat.checkSelfPermission(App.get(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManagerCompat.from(App.get()).notify(ID, notification);
    }

    public static void show(int resId) {
        if (resId != 0) show(ResUtil.getString(resId));
    }

    public static void show(String text) {
        get().makeText(text);
    }

    public static void showCenter(int resId) {
        if (resId != 0) showCenter(ResUtil.getString(resId));
    }

    public static void showCenter(String text) {
        get().makeTextCenter(text);
    }

    public static void progress(Context context) {
        dismiss();
        get().create(context);
    }

    public static void dismiss() {
        try {
            if (get().mDialog != null) get().mDialog.dismiss();
        } catch (Exception ignored) {
        }
    }

    private void create(Context context) {
        ViewProgressBinding binding = ViewProgressBinding.inflate(LayoutInflater.from(context));
        mDialog = new MaterialAlertDialogBuilder(context).setView(binding.getRoot()).create();
        mDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        mDialog.show();
    }

    private boolean shouldSkip(String message) {
        if (TextUtils.isEmpty(message)) return true;
        long now = System.currentTimeMillis();
        // 相同文本在 1 秒内重复调用时只显示一次，避免轻提示连弹两遍
        if (message.equals(mLastToastText) && now - mLastToastTime < 1000) return true;
        mLastToastText = message;
        mLastToastTime = now;
        return false;
    }

    private synchronized void showToast(String message, boolean center) {
        if (mToast == null) {
            mToast = new Toast(App.get());
            mToast.setView(LayoutInflater.from(App.get()).inflate(R.layout.view_toast, null));
            mToast.setDuration(Toast.LENGTH_SHORT);
        }
        // 复用同一个 Toast 实例并更新文案/位置，避免 cancel 与 show 之间的异步竞态
        // 导致旧 Toast 未被真正取消又弹出，造成“连弹两遍”的观感。
        TextView view = mToast.getView().findViewById(R.id.message);
        if (view != null) view.setText(message);
        int offsetY = center ? 0 : (int) (60 * App.get().getResources().getDisplayMetrics().density);
        mToast.setGravity(center ? Gravity.CENTER : Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, offsetY);
        if (mHandler == null) mHandler = new Handler(Looper.getMainLooper());
        mHandler.removeCallbacksAndMessages(null);
        // 延迟一帧再显示，确保上一次的 cancel 已生效，彻底消除连弹
        mHandler.post(() -> {
            if (mToast != null) mToast.show();
        });
        mHandler.postDelayed(() -> {
            if (mToast != null) mToast.cancel();
        }, 1000);
    }

    private void makeText(String message) {
        if (shouldSkip(message)) return;
        showToast(message, false);
    }

    private void makeTextCenter(String message) {
        if (shouldSkip(message)) return;
        showToast(message, true);
    }
}
