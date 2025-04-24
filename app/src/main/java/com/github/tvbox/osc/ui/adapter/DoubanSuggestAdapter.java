package com.github.tvbox.osc.ui.adapter;

import android.widget.ImageView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.DoubanSuggestBean;
import com.github.tvbox.osc.util.GlideHelper;
import com.github.tvbox.osc.util.MD5;

import java.util.ArrayList;
import java.util.List;

import me.jessyan.autosize.utils.AutoSizeUtils;

public class DoubanSuggestAdapter extends BaseQuickAdapter<DoubanSuggestBean, BaseViewHolder> {
    public DoubanSuggestAdapter(List<DoubanSuggestBean> list) {
        super(R.layout.item_douban_suggest, list);
    }

    @Override
    protected void convert(BaseViewHolder helper, DoubanSuggestBean item) {
        helper.setText(R.id.tvName,item.getTitle())
                .setText(R.id.tvRating,"豆瓣: "+item.getDoubanRating()+"\n烂番茄: "+item.getRottenRating()+"\nIMDB: "+item.getImdbRating());

        int width = AutoSizeUtils.dp2px(mContext, 110);
        int height = AutoSizeUtils.dp2px(mContext, 160);
        int radius = AutoSizeUtils.dp2px(mContext, 6);
        GlideHelper.loadRoundedImage((ImageView) helper.getView(R.id.ivThumb), item.getImg(), width, height, radius);
    }
}