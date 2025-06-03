package com.github.tvbox.osc.ui.dialog;

import android.content.Context;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.bean.CastVideo;
import com.lxj.xpopup.core.CenterPopupView;

import org.jetbrains.annotations.NotNull;

/**
 * 投屏对话框（已禁用DLNA功能）
 */
public class CastListDialog extends CenterPopupView {

    public CastListDialog(@NonNull @NotNull Context context, CastVideo castVideo) {
        super(context);
    }

    @Override
    protected int getImplLayoutId() {
        return 0;
    }
}
