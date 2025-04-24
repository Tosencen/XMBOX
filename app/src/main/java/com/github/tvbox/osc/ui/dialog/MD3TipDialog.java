package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.util.Utils;
import com.google.android.material.button.MaterialButton;
import com.lxj.xpopup.core.BasePopupView;
import com.lxj.xpopup.core.CenterPopupView;

/**
 * Material Design 3风格的提示对话框
 */
public class MD3TipDialog extends CenterPopupView {

    private boolean isShowing = false;

    public void hide() {
        dismiss();
    }

    public boolean isShowing() {
        return isShowing;
    }

    @Override
    public BasePopupView show() {
        isShowing = true;
        return super.show();
    }

    @Override
    public void dismiss() {
        super.dismiss();
        isShowing = false;
    }

    private final String title;
    private final String content;
    private final String leftBtnText;
    private final String rightBtnText;
    private final OnListener listener;

    public interface OnListener {
        void left();
        void right();
        void cancel();
        void onTitleClick();
    }

    public MD3TipDialog(@NonNull Context context, String content, String leftBtnText, String rightBtnText, OnListener listener) {
        this(context, "提示", content, leftBtnText, rightBtnText, listener);
    }

    public MD3TipDialog(@NonNull Context context, String title, String content, String leftBtnText, String rightBtnText, OnListener listener) {
        super(context);
        this.title = title;
        this.content = content;
        this.leftBtnText = leftBtnText;
        this.rightBtnText = rightBtnText;
        this.listener = listener;
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.dialog_tip_md3;
    }

    @Override
    protected void onCreate() {
        super.onCreate();

        // 根据主题设置背景
        LinearLayout rootLayout = findViewById(R.id.root_layout);
        if (rootLayout != null) {
            if (Utils.isDarkTheme()) {
                rootLayout.setBackgroundResource(R.drawable.bg_dialog_md3_dark);
            } else {
                rootLayout.setBackgroundResource(R.drawable.bg_dialog_md3_light);
            }
        }

        TextView tvTitle = findViewById(R.id.tv_title);
        TextView tvContent = findViewById(R.id.tv_content);
        MaterialButton btnLeft = findViewById(R.id.btn_left);
        MaterialButton btnRight = findViewById(R.id.btn_right);

        tvTitle.setText(title);
        tvContent.setText(content);
        btnLeft.setText(leftBtnText);
        btnRight.setText(rightBtnText);

        tvTitle.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTitleClick();
            }
        });

        btnLeft.setOnClickListener(v -> {
            if (listener != null) {
                listener.left();
            }
        });

        btnRight.setOnClickListener(v -> {
            if (listener != null) {
                listener.right();
            }
        });

        super.dismissWith(() -> {
            if (listener != null) {
                listener.cancel();
            }
        });
    }
}
