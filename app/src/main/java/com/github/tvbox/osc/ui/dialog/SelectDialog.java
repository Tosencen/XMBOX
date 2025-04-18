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
import com.github.tvbox.osc.R;
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
        super.onCreate(savedInstanceState);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(getWindow().getAttributes());
        lp.gravity = Gravity.CENTER;
        lp.width = ConvertUtils.dp2px(330);
        getWindow().setAttributes(lp);
        getWindow().setWindowAnimations(R.style.DialogFadeAnimation); // Set the animation style

        // 设置深色主题
        getWindow().getDecorView().setBackgroundResource(android.R.color.transparent);
        int nightModeFlags = getContext().getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            // 已经是深色模式
        } else {
            // 强制使用深色主题颜色
            View rootView = findViewById(R.id.cl_root);
            if (rootView != null) {
                rootView.setBackgroundResource(R.drawable.bg_dialog_dark);
            }
            TextView titleView = findViewById(R.id.title);
            if (titleView != null) {
                titleView.setTextColor(getContext().getResources().getColor(R.color.color_FFFFFF));
            }
        }
    }

    public void setTip(String tip) {
        ((TextView) findViewById(R.id.title)).setText(tip);
    }

    public void setAdapter(SelectDialogAdapter.SelectDialogInterface<T> sourceBeanSelectDialogInterface, DiffUtil.ItemCallback<T> sourceBeanItemCallback, List<T> data, int select) {
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
}
