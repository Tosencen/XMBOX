package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.util.XPopupAnimUtil;
import com.hjq.bar.TitleBar;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.core.BasePopupView;

import org.jetbrains.annotations.NotNull;

public class TipDialog extends BaseDialog {

    private boolean isShowing = false;
    private BasePopupView md3TipDialog;

    public void hide() {
        if (md3TipDialog != null) {
            md3TipDialog.dismiss();
        } else {
            dismiss();
        }
        isShowing = false;
    }

    public boolean isShowing() {
        return isShowing;
    }

    @Override
    public void show() {
        // 使用MD3TipDialog替代旧的TipDialog
        if (md3TipDialog != null) {
            md3TipDialog.show();
        } else {
            super.show();
        }
        isShowing = true;
    }

    public TipDialog(@NonNull @NotNull Context context, String tip, String left, String right, OnListener listener) {
        super(context);

        // 创建MD3TipDialog实例
        MD3TipDialog md3Dialog = new MD3TipDialog(context, "提示", tip, left, right, new MD3TipDialog.OnListener() {
                    @Override
                    public void left() {
                        listener.left();
                    }

                    @Override
                    public void right() {
                        listener.right();
                    }

                    @Override
                    public void cancel() {
                        listener.cancel();
                    }

                    @Override
                    public void onTitleClick() {
                        listener.onTitleClick();
                    }
                });

        md3TipDialog = XPopupAnimUtil.createMD3Builder(context)
                .asCustom(md3Dialog);

        // 保留旧的实现，以防MD3TipDialog出现问题
        setContentView(R.layout.dialog_tip);

        TextView tipInfo = findViewById(R.id.tipInfo);
        TextView leftBtn = findViewById(R.id.leftBtn);
        TextView rightBtn = findViewById(R.id.rightBtn);
        TitleBar titleBar = findViewById(R.id.title_bar);
        tipInfo.setText(tip);
        leftBtn.setText(left);
        rightBtn.setText(right);
        leftBtn.setOnClickListener(v -> listener.left());
        rightBtn.setOnClickListener(v -> listener.right());
        setOnCancelListener(dialog -> listener.cancel());
        titleBar.getRightView().setOnClickListener(view -> listener.onTitleClick());
    }

    public interface OnListener {
        void left();

        void right();

        void cancel();
        void onTitleClick();
    }
}
