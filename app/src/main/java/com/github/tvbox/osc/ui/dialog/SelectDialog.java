package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.blankj.utilcode.util.ConvertUtils;
import com.xmbox.app.R;
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter;
import com.owen.tvrecyclerview.widget.TvRecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SelectDialog<T> extends BaseDialog {

    public SelectDialog(@NonNull @NotNull Context context) {
        super(context);
        setContentView(R.layout.dialog_select_md3);
    }

    public SelectDialog(@NonNull @NotNull Context context, int resId) {
        super(context);
        setContentView(resId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 不调用super.onCreate()，避免BaseDialog的窗口设置干扰
        // super.onCreate(savedInstanceState);
        android.util.Log.d("SelectDialog", "onCreate() called");

        // 直接设置窗口属性，避免继承的冲突
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(getWindow().getAttributes());
        lp.gravity = Gravity.CENTER;
        lp.width = ConvertUtils.dp2px(330);
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.dimAmount = 0.5f;
        getWindow().setAttributes(lp);
        getWindow().setWindowAnimations(R.style.MD3DialogAnimation); // 使用Material Design 3风格的动画
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        // 设置背景透明
        getWindow().getDecorView().setBackgroundResource(android.R.color.transparent);

        // 根据当前模式设置对话框背景
        int nightModeFlags = getContext().getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        View rootView = findViewById(R.id.cl_root);
        if (rootView != null) {
            if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                // 夜间模式使用深色背景
                rootView.setBackgroundResource(R.drawable.bg_dialog_dark);
            } else {
                // 白天模式使用白色背景
                rootView.setBackgroundResource(R.drawable.bg_dialog_md3_light);
            }
        }
        android.util.Log.d("SelectDialog", "onCreate() completed");
    }

    public void setTip(String tip) {
        ((TextView) findViewById(R.id.title)).setText(tip);
    }

    public void setAdapter(SelectDialogAdapter.SelectDialogInterface<T> sourceBeanSelectDialogInterface, DiffUtil.ItemCallback<T> sourceBeanItemCallback, List<T> data, int select) {
        android.util.Log.d("SelectDialog", "setAdapter() called with data size: " + (data != null ? data.size() : 0));
        SelectDialogAdapter<T> adapter = new SelectDialogAdapter(sourceBeanSelectDialogInterface, sourceBeanItemCallback);
        adapter.setData(data, select);
        TvRecyclerView tvRecyclerView = ((TvRecyclerView) findViewById(R.id.list));
        tvRecyclerView.setAdapter(adapter);

        // 确保选择的位置有效
        final int validPosition = (select >= 0 && select < data.size()) ? select : 0;

        tvRecyclerView.setSelectedPosition(validPosition);
        tvRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                // 再次检查适配器是否已经设置并且有数据
                if (tvRecyclerView.getAdapter() != null && tvRecyclerView.getAdapter().getItemCount() > 0) {
                    tvRecyclerView.smoothScrollToPosition(validPosition);
                    tvRecyclerView.setSelectionWithSmooth(validPosition);
                }
            }
        });
    }

    @Override
    public void show() {
        android.util.Log.d("SelectDialog", "show() called");
        try {
            super.show();
            android.util.Log.d("SelectDialog", "show() completed successfully");
        } catch (Exception e) {
            android.util.Log.e("SelectDialog", "show() failed", e);
        }
    }
}
