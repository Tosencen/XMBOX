package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.ToastUtils;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.Utils;
import com.google.android.material.card.MaterialCardView;
import com.lxj.xpopup.core.PositionPopupView;
import com.lxj.xpopup.enums.DragOrientation;

public class LastViewedDialog extends PositionPopupView {
    private final VodInfo vodInfo;

    public LastViewedDialog(@NonNull Context context, VodInfo vodInfo) {
        super(context);
        this.vodInfo = vodInfo;
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.dialog_last_viewed_m3_new;
    }

    @Override
    protected void beforeShow() {
        super.beforeShow();
        // 确保弹窗背景是透明的，让卡片背景显示出来
        getPopupImplView().setBackgroundResource(android.R.color.transparent);
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        View rootView = getPopupImplView();

        // 根据当前主题设置卡片背景颜色
        MaterialCardView cardView = rootView.findViewById(R.id.card_view);
        if (cardView != null) {
            if (Utils.isDarkTheme()) {
                cardView.setCardBackgroundColor(getContext().getResources().getColor(R.color.color_dialog_bg_dark));
            } else {
                cardView.setCardBackgroundColor(getContext().getResources().getColor(R.color.white));
            }
        }

        TextView textView = findViewById(R.id.tv);
        textView.setText("上次看到: "+vodInfo.name+" "+vodInfo.note);

        View btnPlay = findViewById(R.id.btn_play);
        View.OnClickListener clickListener = view -> {
            FastClickCheckUtil.check(view);
            dismiss();
            Bundle bundle = new Bundle();
            bundle.putString("id", vodInfo.id);
            bundle.putString("sourceKey", vodInfo.sourceKey);
            getContext().startActivity(new Intent(getContext(),DetailActivity.class).putExtras(bundle));
        };

        // 设置文本和按钮的点击事件
        textView.setOnClickListener(clickListener);
        btnPlay.setOnClickListener(clickListener);
    }

    @Override
    protected DragOrientation getDragOrientation() {
        return DragOrientation.DragToRight;
    }
}