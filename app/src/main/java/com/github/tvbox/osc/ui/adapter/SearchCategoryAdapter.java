package com.github.tvbox.osc.ui.adapter;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.xmbox.app.R;
import com.github.tvbox.osc.bean.SearchCategoryBean;

import java.util.ArrayList;
import java.util.List;

public class SearchCategoryAdapter extends BaseQuickAdapter<SearchCategoryBean, BaseViewHolder> {
    private int selectedPosition = 0;

    public SearchCategoryAdapter() {
        super(R.layout.item_search_category, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, SearchCategoryBean item) {
        TextView tvCategory = helper.getView(R.id.tvCategory);
        tvCategory.setText(item.getName());

        if (item.isSelected()) {
            tvCategory.setTextColor(helper.itemView.getContext().getResources().getColor(R.color.md3_on_primary, null)); // 选中状态颜色
            helper.itemView.setBackgroundResource(R.drawable.shape_setting_sort_focus);
        } else {
            tvCategory.setTextColor(helper.itemView.getContext().getResources().getColor(R.color.md3_on_surface_variant, null)); // 未选中状态颜色
            helper.itemView.setBackgroundResource(R.drawable.shape_setting_sort_normal);
        }
    }

    public void setSelectedPosition(int position) {
        if (position >= 0 && position < getData().size()) {
            for (int i = 0; i < getData().size(); i++) {
                getData().get(i).setSelected(i == position);
            }
            selectedPosition = position;
            notifyDataSetChanged();
        }
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }
}
