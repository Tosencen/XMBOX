package com.github.tvbox.osc.ui.adapter;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchWordAdapter extends BaseQuickAdapter<String, BaseViewHolder> {
    private int selectedPosition = 0;
    private Map<String, String> wordKeys = new HashMap<>();

    public SearchWordAdapter() {
        super(R.layout.item_search_word, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, String item) {
        TextView tvWord = helper.getView(R.id.tvWord);
        tvWord.setText(item);

        // 强制确保项目可点击
        helper.itemView.setClickable(true);
        helper.itemView.setFocusable(true);
        tvWord.setClickable(false); // 确保文本不拦截点击

        // 先设置选中状态的视觉效果
        if (helper.getLayoutPosition() == selectedPosition) {
            tvWord.setTextColor(helper.itemView.getContext().getResources().getColor(R.color.md3_on_primary, null)); // 选中状态颜色
            helper.itemView.setBackgroundResource(R.drawable.shape_setting_sort_focus);
            com.blankj.utilcode.util.LogUtils.d("SearchWordAdapter: 项目" + item + "处于选中状态");
        } else {
            tvWord.setTextColor(helper.itemView.getContext().getResources().getColor(R.color.md3_on_surface_variant, null)); // 未选中状态颜色
            helper.itemView.setBackgroundResource(R.drawable.shape_setting_sort_normal);
            com.blankj.utilcode.util.LogUtils.d("SearchWordAdapter: 项目" + item + "处于非选中状态");
        }

        // 使用标准的点击监听器，而不是自定义的触摸监听器
        helper.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = helper.getLayoutPosition();
                com.blankj.utilcode.util.LogUtils.d("SearchWordAdapter: 点击了项目 - position=" + position + ", item=" + item);

                // 设置选中状态
                setSelectedPosition(position);

                // 触发适配器的点击监听器
                if (getOnItemClickListener() != null) {
                    getOnItemClickListener().onItemClick(SearchWordAdapter.this, v, position);
                }
            }
        });

        // 添加点击效果
        helper.itemView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    // 按下时的视觉反馈
                    v.setAlpha(0.7f);
                } else if (event.getAction() == android.view.MotionEvent.ACTION_UP ||
                           event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                    // 松开或取消时恢复
                    v.setAlpha(1.0f);
                }
                // 返回false表示我们只处理了视觉效果，事件应该继续传递
                return false;
            }
        });
    }

    public void setSelectedPosition(int position) {
        if (position >= 0 && position < getData().size()) {
            selectedPosition = position;
            notifyDataSetChanged();
        }
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public void setNewDataWithKeys(List<String> data, Map<String, String> keys) {
        this.wordKeys.clear();
        if (keys != null) {
            this.wordKeys.putAll(keys);
        }
        setNewData(data);
    }

    public String getSelectedKey() {
        if (selectedPosition >= 0 && selectedPosition < getData().size()) {
            return wordKeys.get(getData().get(selectedPosition));
        }
        return "";
    }
}