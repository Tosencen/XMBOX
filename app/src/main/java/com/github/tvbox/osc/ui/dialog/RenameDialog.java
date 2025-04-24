package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.ToastUtils;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.util.Utils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.lxj.xpopup.core.CenterPopupView;

/**
 * 订阅重命名对话框
 */
public class RenameDialog extends CenterPopupView {

    public interface OnRenameListener {
        void onRename(String text);
    }

    private final String mDefaultText;
    private final String mTitle;
    private final String mHint;
    private final OnRenameListener mListener;

    public RenameDialog(@NonNull Context context, String title, String hint, String defaultText, OnRenameListener listener) {
        super(context);
        this.mTitle = title;
        this.mHint = hint;
        this.mDefaultText = defaultText;
        this.mListener = listener;
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.dialog_rename_m3;
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

        TextInputEditText etInput = rootView.findViewById(R.id.et_input);
        MaterialButton btnCancel = rootView.findViewById(R.id.btn_cancel);
        MaterialButton btnConfirm = rootView.findViewById(R.id.btn_confirm);

        // 设置标题和提示
        if (!TextUtils.isEmpty(mTitle)) {
            ((android.widget.TextView) rootView.findViewById(R.id.tv_title)).setText(mTitle);
        }

        if (!TextUtils.isEmpty(mHint)) {
            etInput.setHint(mHint);
        }

        // 设置默认文本
        if (!TextUtils.isEmpty(mDefaultText)) {
            etInput.setText(mDefaultText);
            etInput.setSelection(mDefaultText.length());
        }

        // 取消按钮
        btnCancel.setOnClickListener(v -> dismiss());

        // 确认按钮
        btnConfirm.setOnClickListener(v -> {
            String text = etInput.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                ToastUtils.showShort("请输入名称");
                return;
            }

            if (text.length() > 8) {
                ToastUtils.showShort("不要过长，不方便记忆");
                return;
            }

            if (mListener != null) {
                mListener.onRename(text);
            }
            dismiss();
        });
    }
}
