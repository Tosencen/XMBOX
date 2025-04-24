package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.util.Utils;
import com.google.android.material.button.MaterialButton;
import com.lxj.xpopup.core.CenterPopupView;

/**
 * Material Design 3风格的确认对话框
 */
public class ConfirmDialog extends CenterPopupView {

    private final String title;
    private final String content;
    private final String cancelText;
    private final String confirmText;
    private final OnDialogActionListener listener;

    public interface OnDialogActionListener {
        void onConfirm();
        default void onCancel() {}
    }

    public ConfirmDialog(@NonNull Context context, String title, String content,
                         String cancelText, String confirmText,
                         OnDialogActionListener listener) {
        super(context);
        this.title = title;
        this.content = content;
        this.cancelText = cancelText;
        this.confirmText = confirmText;
        this.listener = listener;
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.dialog_confirm_m3;
    }

    @Override
    protected int getPopupHeight() {
        return -2; // Wrap content
    }

    @Override
    protected int getMaxHeight() {
        return -1; // No max height
    }

    @Override
    protected void beforeShow() {
        super.beforeShow();
        // 在beforeShow中设置背景，确保在显示前应用
        View rootView = getPopupImplView();
        if (Utils.isDarkTheme()) {
            rootView.setBackgroundResource(R.drawable.bg_dialog_dark);
        } else {
            rootView.setBackgroundResource(R.drawable.bg_dialog_md3);
        }
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        View rootView = getPopupImplView();

        // 背景已在beforeShow中设置

        TextView tvTitle = findViewById(R.id.tv_title);
        TextView tvContent = findViewById(R.id.tv_content);
        MaterialButton btnCancel = findViewById(R.id.btn_cancel);
        MaterialButton btnConfirm = findViewById(R.id.btn_confirm);

        tvTitle.setText(title);
        tvContent.setText(content);

        // 设置取消按钮
        if (cancelText != null && !cancelText.isEmpty()) {
            btnCancel.setText(cancelText);
            btnCancel.setVisibility(View.VISIBLE);
        } else {
            btnCancel.setVisibility(View.GONE);
        }

        // 设置确认按钮
        if (confirmText != null && !confirmText.isEmpty()) {
            btnConfirm.setText(confirmText);
        } else {
            btnConfirm.setText("确定");
        }

        btnCancel.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCancel();
            }
            dismiss();
        });

        btnConfirm.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConfirm();
            }
            dismiss();
        });
    }
}
