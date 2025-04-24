package com.github.tvbox.osc.ui.adapter;

import android.view.View;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.VodInfo;

import java.util.ArrayList;

/**
 * @author pj567
 * @date :2020/12/22
 * @description:
 */
public class SeriesFlagAdapter extends BaseQuickAdapter<VodInfo.VodSeriesFlag, BaseViewHolder> {
    public SeriesFlagAdapter() {
        super(R.layout.item_select_flag, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, VodInfo.VodSeriesFlag item) {
        View select = helper.getView(R.id.vFlag);
        TextView tvFlag = helper.getView(R.id.tvFlag);
        com.lihang.ShadowLayout shadowLayout = (com.lihang.ShadowLayout) helper.itemView;

        // 清理线路名称中的表情符号
        String flagName = item.name;
        flagName = flagName.replaceAll("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+", "").trim(); // 移除表情符号

        if (item.selected) {
            select.setVisibility(View.VISIBLE);
            tvFlag.setTextColor(helper.itemView.getContext().getResources().getColor(R.color.selected_text_color_light));
            shadowLayout.setSelected(true);
        } else {
            select.setVisibility(View.GONE);
            // 使用更明显的颜色显示文本
            tvFlag.setTextColor(helper.itemView.getContext().getResources().getColor(R.color.md3_on_surface_variant));
            shadowLayout.setSelected(false);
        }

        helper.setText(R.id.tvFlag, flagName);

        // 确保整个项目可点击
        helper.itemView.setClickable(true);
        helper.itemView.setFocusable(true);
    }
}