package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.MovieSort;
import com.github.tvbox.osc.ui.adapter.GridFilterKVAdapter;
import com.lihang.ShadowLayout;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

public class GridFilterDialog extends BaseDialog {
    private LinearLayout filterRoot;
    private MovieSort.SortData mSortData;

    public GridFilterDialog(@NonNull @NotNull Context context) {
        super(context);

        setCancelable(true);
        setContentView(R.layout.dialog_grid_filter);
        filterRoot = findViewById(R.id.filterRoot);
        findViewById(R.id.btn_reset).setOnClickListener(view -> {
            mSortData.filterSelect = new HashMap<>();
            selectChange = true;
            setData(mSortData);
        });

        findViewById(R.id.btn_confirm).setOnClickListener(view -> {
            dismiss();
        });
    }

    public interface Callback {
        void change();
    }

    public void setOnDismiss(Callback callback) {
        setOnDismissListener(dialogInterface -> {
            if (selectChange) {
                callback.change();
            }
        });
    }

    public void setData(MovieSort.SortData sortData) {
        mSortData = sortData;
        filterRoot.removeAllViews();
        for (MovieSort.SortFilter filter : sortData.filters) {
            View line = LayoutInflater.from(getContext()).inflate(R.layout.item_grid_filter, null);
            RecyclerView gridView = line.findViewById(R.id.mFilterKv);
            gridView.setHasFixedSize(true);
            gridView.setLayoutManager(new V7LinearLayoutManager(getContext(), 0, false));
            GridFilterKVAdapter filterKVAdapter = new GridFilterKVAdapter();
            gridView.setAdapter(filterKVAdapter);
            String key = filter.key;
            ArrayList<String> values = new ArrayList<>(filter.values.keySet());
            ArrayList<String> keys = new ArrayList<>(filter.values.values());
            filterKVAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
                View pre = null;

                @Override
                public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                    selectChange = true;
                    String filterSelect = sortData.filterSelect.get(key);
                    if (filterSelect == null || !filterSelect.equals(keys.get(position))) {// 没选 或 不是重选
                        sortData.filterSelect.put(key, keys.get(position));
                        if (pre != null) {//上一次点击的view
                            ShadowLayout val = pre.findViewById(R.id.sl);
                            val.setSelected(false);
                        }
                        ShadowLayout val = view.findViewById(R.id.sl);
                        val.setSelected(true);
                        //记录点击的view,下次点击对上一个view做处理
                        pre = view;
                    } else {// 重选 取消
                        sortData.filterSelect.remove(key);
                        if (pre != null){
                            ShadowLayout val = pre.findViewById(R.id.sl);
                            val.setSelected(false);
                        }
                        pre = null;
                    }
                }
            });
            filterKVAdapter.setNewData(values);
            filterRoot.addView(line);
        }
    }

    private boolean selectChange = false;

    public void show() {
        selectChange = false;
        super.show();
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.gravity = Gravity.BOTTOM;
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        layoutParams.dimAmount = 0.5f; // 设置背景遮罩透明度为0.5，符合Material Design 3规范
        getWindow().getDecorView().setPadding(0, 0, 0, 0);
        getWindow().setAttributes(layoutParams);

        // 设置背景色 - 根据当前模式设置不同的背景色
        ShadowLayout shadowLayout = (ShadowLayout) findViewById(R.id.filterRootContainer);
        if (shadowLayout != null) {
            // 判断是否为夜间模式
            boolean isNightMode = (getContext().getResources().getConfiguration().uiMode &
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES;

            if (isNightMode) {
                // 夜间模式使用深色背景 #232626
                shadowLayout.setLayoutBackground(getContext().getResources().getColor(R.color.color_232626));
            } else {
                // 白天模式使用白色背景
                shadowLayout.setLayoutBackground(getContext().getResources().getColor(R.color.white));
            }
        }
    }
}