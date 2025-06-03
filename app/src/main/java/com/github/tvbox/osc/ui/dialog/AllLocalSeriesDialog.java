package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.ColorUtils;
import com.xmbox.app.R;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.ui.adapter.SeriesAdapter;
import com.github.tvbox.osc.ui.widget.GridSpacingItemDecoration;
import com.github.tvbox.osc.util.Utils;
import com.lxj.xpopup.core.DrawerPopupView;
import com.lxj.xpopup.interfaces.OnSelectListener;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @Author : Liu XiaoRan
 * @Email : 592923276@qq.com
 * @Date : on 2023/10/25 10:43.
 * @Description : 本地视频全集弹窗
 */
public class AllLocalSeriesDialog extends DrawerPopupView {

    List<VodInfo.VodSeries> mList;
    private final OnSelectListener mSelectListener;

    public AllLocalSeriesDialog(@NonNull @NotNull Context context, List<VodInfo.VodSeries> list, OnSelectListener selectListener) {
        super(context);
        mList = list;
        mSelectListener = selectListener;
    }

    @Override
    protected int getImplLayoutId() {//使用MD3样式的弹窗布局
        return R.layout.dialog_all_series_m3;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        View bg = findViewById(R.id.bg);
        // 使用MD3主题色彩
        bg.setBackgroundColor(ColorUtils.getColor(R.color.md3_surface_container_high));
        RecyclerView rv = findViewById(R.id.rv);

        // 根据屏幕方向和弹窗位置调整列数
        int orientation = getContext().getResources().getConfiguration().orientation;
        int spanCount;
        if (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            // 横屏时使用更多列
            spanCount = 6;
        } else {
            // 竖屏时使用较少列
            spanCount = 4;
        }

        rv.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
        rv.addItemDecoration(new GridSpacingItemDecoration(spanCount, com.blankj.utilcode.util.ConvertUtils.dp2px(12), true));


        SeriesAdapter seriesAdapter = new SeriesAdapter(true);

        // 调试：检查数据内容
        android.util.Log.d("AllLocalSeriesDialog", "数据列表大小: " + mList.size());
        for (int i = 0; i < Math.min(mList.size(), 5); i++) {
            VodInfo.VodSeries series = mList.get(i);
            android.util.Log.d("AllLocalSeriesDialog", "第" + i + "项: name=" + series.name + ", selected=" + series.selected);
        }

        seriesAdapter.setNewData(mList);
        rv.setAdapter(seriesAdapter);

        for (int i = 0; i < mList.size(); i++) {
            if (mList.get(i).selected){
                rv.scrollToPosition(i);
            }
        }

        seriesAdapter.setOnItemClickListener((adapter, view, position) -> {
            // 更新选中状态
            for (int j = 0; j < seriesAdapter.getData().size(); j++) {
                seriesAdapter.getData().get(j).selected = false;
                seriesAdapter.notifyItemChanged(j);
            }
            seriesAdapter.getData().get(position).selected = true;
            seriesAdapter.notifyItemChanged(position);

            // 回调选择事件
            mSelectListener.onSelect(position,"");

            // 延迟关闭弹窗，让用户看到选中效果
            view.postDelayed(() -> {
                dismiss();
            }, 200);
        });
    }
}