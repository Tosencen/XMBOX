package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.xmbox.app.R;
import com.lxj.xpopup.core.CenterPopupView;

/**
 * Material Design 3风格的权限请求弹窗
 */
public class PermissionM3Dialog extends CenterPopupView {

    private String title;
    private String message;
    private int iconResId;
    private OnPermissionDialogListener listener;

    public interface OnPermissionDialogListener {
        void onConfirm();
        void onCancel();
    }

    public PermissionM3Dialog(@NonNull Context context) {
        super(context);
        this.title = "需要授予权限";
        this.message = "应用需要相关权限才能正常工作";
        this.iconResId = R.drawable.ic_storage_m3;
    }

    public PermissionM3Dialog(@NonNull Context context, String title, String message, int iconResId) {
        super(context);
        this.title = title;
        this.message = message;
        this.iconResId = iconResId;
    }

    public PermissionM3Dialog setTitle(String title) {
        this.title = title;
        return this;
    }

    public PermissionM3Dialog setMessage(String message) {
        this.message = message;
        return this;
    }

    public PermissionM3Dialog setIcon(int iconResId) {
        this.iconResId = iconResId;
        return this;
    }

    public PermissionM3Dialog setListener(OnPermissionDialogListener listener) {
        this.listener = listener;
        return this;
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.dialog_permission_m3;
    }

    @Override
    protected void onCreate() {
        super.onCreate();

        ImageView ivIcon = findViewById(R.id.iv_icon);
        TextView tvTitle = findViewById(R.id.tv_title);
        TextView tvMessage = findViewById(R.id.tv_message);
        View btnCancel = findViewById(R.id.btn_cancel);
        View btnConfirm = findViewById(R.id.btn_confirm);

        // 设置内容
        if (iconResId != 0) {
            ivIcon.setImageResource(iconResId);
            ivIcon.setVisibility(View.VISIBLE);
        } else {
            ivIcon.setVisibility(View.GONE);
        }

        if (title != null && !title.isEmpty()) {
            tvTitle.setText(title);
        }

        if (message != null && !message.isEmpty()) {
            tvMessage.setText(message);
        }

        // 设置点击事件
        btnCancel.setOnClickListener(v -> {
            dismiss();
            if (listener != null) {
                listener.onCancel();
            }
        });

        btnConfirm.setOnClickListener(v -> {
            dismiss();
            if (listener != null) {
                listener.onConfirm();
            }
        });
    }

    @Override
    protected int getMaxWidth() {
        return (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.85f);
    }


}
