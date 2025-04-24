package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.util.Utils;
import com.lxj.xpopup.core.BottomPopupView;

import org.jetbrains.annotations.NotNull;

public class AboutDialog extends BottomPopupView {

    public AboutDialog(@NonNull @NotNull Context context) {
        super(context);
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.dialog_about_m3;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        View rootView = getPopupImplView();

        // 根据当前模式设置对话框背景
        if (Utils.isDarkTheme()) {
            // 夜间模式使用深色背景
            rootView.setBackgroundResource(R.drawable.bg_dialog_md3_dark);
        } else {
            // 白天模式使用默认背景
            rootView.setBackgroundResource(R.drawable.bg_dialog_md3);
        }
    }
}