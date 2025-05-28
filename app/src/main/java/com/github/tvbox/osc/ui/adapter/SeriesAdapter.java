package com.github.tvbox.osc.ui.adapter;

import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.ConvertUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.xmbox.app.R;
import com.github.tvbox.osc.bean.VodInfo;
import com.lihang.ShadowLayout;

import java.util.ArrayList;

/**
 * @author pj567
 * @date :2020/12/22
 * @description:
 */
public class SeriesAdapter extends BaseQuickAdapter<VodInfo.VodSeries, BaseViewHolder> {
    private boolean isGird;

    public SeriesAdapter(boolean isGird) {
        super(R.layout.item_series, new ArrayList<>());
        this.isGird = isGird;
    }

    @Override
    protected void convert(BaseViewHolder helper, VodInfo.VodSeries item) {
        ShadowLayout sl = helper.getView(R.id.sl);
        TextView tvSeries = helper.getView(R.id.tvSeries);

        // 确保所有集数名称都能正确显示
        String seriesName = item.name;
        if (seriesName == null || seriesName.trim().isEmpty()) {
            seriesName = "第" + (helper.getAdapterPosition() + 1) + "集";
        }
        tvSeries.setText(seriesName);

        // 确保TextView可见
        tvSeries.setVisibility(android.view.View.VISIBLE);

        // 设置选中状态
        sl.setSelected(item.selected);

        if (!isGird){// 详情页横向展示时固定宽度
            ViewGroup.LayoutParams layoutParams = sl.getLayoutParams();
            layoutParams.width = ConvertUtils.dp2px(120);
            sl.setLayoutParams(layoutParams);
        } else {
            // 网格布局模式
            int orientation = helper.itemView.getContext().getResources().getConfiguration().orientation;
            if (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                // 横屏模式下调整文本大小
                tvSeries.setTextSize(12);
            } else {
                // 竖屏模式
                tvSeries.setTextSize(14);
            }
        }

        // 最后设置文本颜色，确保不被覆盖
        tvSeries.post(() -> {
            if (item.selected) {
                tvSeries.setTextColor(Color.WHITE);
            } else {
                tvSeries.setTextColor(Color.parseColor("#333333"));
            }
        });
    }

    public void setGird(boolean gird) {
        isGird = gird;
    }

}