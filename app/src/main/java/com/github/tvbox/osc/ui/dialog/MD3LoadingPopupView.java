package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.lxj.xpopup.core.CenterPopupView;

/**
 * Material Design 3 风格的加载弹窗
 */
public class MD3LoadingPopupView extends CenterPopupView {

    public MD3LoadingPopupView(@NonNull Context context) {
        super(context);
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.popup_loading_md3;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onDismiss() {
        super.onDismiss();
    }

    @Override
    protected void onShow() {
        super.onShow();
    }

    @Override
    public View getPopupContentView() {
        return super.getPopupContentView();
    }
}
