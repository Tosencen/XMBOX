package com.github.tvbox.osc.ui.adapter;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.cache.VodCollect;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.GlideHelper;
import com.github.tvbox.osc.util.MD5;

import java.util.ArrayList;

import me.jessyan.autosize.utils.AutoSizeUtils;

public class CollectAdapter extends BaseQuickAdapter<VodCollect, BaseViewHolder> {
    public CollectAdapter() {
        super(R.layout.item_grid, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, VodCollect item) {
        helper.setVisible(R.id.tvNote, false);
        TextView tvYear = helper.getView(R.id.tvYear);
        if (ApiConfig.get().getSource(item.sourceKey)!=null) {
            tvYear.setText(ApiConfig.get().getSource(item.sourceKey).getName());
            tvYear.setVisibility(View.VISIBLE);
        } else {
            tvYear.setVisibility(View.GONE);
        }
        helper.setText(R.id.tvName, item.name);
        ImageView ivThumb = helper.getView(R.id.ivThumb);
        // 使用Glide加载图片
        if (!TextUtils.isEmpty(item.pic)) {
            GlideHelper.loadImage(ivThumb, DefaultConfig.checkReplaceProxy(item.pic), 300, 400);
        } else {
            ivThumb.setImageResource(R.drawable.img_loading_placeholder);
        }
    }
}