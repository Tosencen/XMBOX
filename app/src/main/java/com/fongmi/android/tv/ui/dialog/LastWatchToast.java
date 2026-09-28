package com.fongmi.android.tv.ui.dialog;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.ui.activity.VideoActivity;
import com.fongmi.android.tv.utils.Notify;

public class LastWatchToast {

    private final Activity activity;
    private final History history;
    private final Handler handler;
    private PopupWindow popupWindow;
    private View contentView;
    private static final int ANIMATION_DURATION = 300; // 动画持续时间(毫秒)
    private static final int DISPLAY_DURATION = 2500; // 显示持续时间(毫秒)

    public static LastWatchToast create(Activity activity, History history) {
        return new LastWatchToast(activity, history);
    }

    private LastWatchToast(Activity activity, History history) {
        this.activity = activity;
        this.history = history;
        this.handler = new Handler(Looper.getMainLooper());
    }

    public void show() {
        if (popupWindow != null && popupWindow.isShowing()) {
            // 如果已经显示，先取消当前显示的，然后重新显示
            dismiss();
        }
        
        contentView = LayoutInflater.from(activity).inflate(R.layout.view_last_watch_toast, null);
        TextView title = contentView.findViewById(R.id.title);
        TextView content = contentView.findViewById(R.id.content);
        
        title.setText(R.string.last_watch);
        content.setText(history.getVodName());
        
        // 设置点击事件
        contentView.setOnClickListener(v -> {
            dismiss();
            if (!VodConfig.get().hasSite(history.getSiteKey())) {
                Notify.show(R.string.history_site_missing);
                return;
            }
            VideoActivity.start(activity, history.getSiteKey(), history.getVodId(), history.getVodName(), history.getVodPic());
        });
        
        // 初始化时设置透明度为0，准备执行淡入动画
        contentView.setAlpha(0f);
        
        // 创建PopupWindow
        popupWindow = new PopupWindow(contentView, 
                ViewGroup.LayoutParams.WRAP_CONTENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT, 
                true);
        
        // 设置背景为透明，避免PopupWindow有默认背景
        popupWindow.setBackgroundDrawable(null);
        popupWindow.setOutsideTouchable(true);
        
        // 在屏幕中央显示
        popupWindow.showAtLocation(activity.getWindow().getDecorView(), Gravity.CENTER, 0, 0);
        
        // 淡入动画
        animateIn();
        
        // 一段时间后自动关闭
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::animateOut, DISPLAY_DURATION);
    }
    
    private void animateIn() {
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(contentView, "alpha", 0f, 1f);
        fadeIn.setDuration(ANIMATION_DURATION);
        fadeIn.setInterpolator(new DecelerateInterpolator());
        fadeIn.start();
    }
    
    private void animateOut() {
        if (contentView == null || popupWindow == null || !popupWindow.isShowing()) return;
        
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(contentView, "alpha", 1f, 0f);
        fadeOut.setDuration(ANIMATION_DURATION);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                dismiss();
            }
        });
        fadeOut.start();
    }
    
    private void dismiss() {
        handler.removeCallbacksAndMessages(null);
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
        popupWindow = null;
        contentView = null;
    }
} 