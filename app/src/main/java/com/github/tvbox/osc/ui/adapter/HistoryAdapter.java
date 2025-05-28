package com.github.tvbox.osc.ui.adapter;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.xmbox.app.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.GlideHelper;
import com.github.tvbox.osc.util.MD5;

import java.util.ArrayList;

import me.jessyan.autosize.utils.AutoSizeUtils;

/**
 * @author pj567
 * @date :2020/12/21
 * @description:
 */
public class HistoryAdapter extends BaseQuickAdapter<VodInfo, BaseViewHolder> {
    public HistoryAdapter() {
        super(R.layout.item_grid, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, VodInfo item) {
        TextView tvYear = helper.getView(R.id.tvYear);
        /*if (item.year <= 0) {
            tvYear.setVisibility(View.GONE);
        } else {
            tvYear.setText(String.valueOf(item.year));
            tvYear.setVisibility(View.VISIBLE);
        }*/
        tvYear.setText(ApiConfig.get().getSource(item.sourceKey).getName());

        TextView tvNote = helper.getView(R.id.tvNote);
        if (item.note == null || item.note.isEmpty()) {
            tvNote.setVisibility(View.GONE);
        } else {
            tvNote.setText(item.note);
            Drawable drawable = ContextCompat.getDrawable(mContext, R.drawable.ic_history_18_m3);
            tvNote.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
        }
        helper.setText(R.id.tvName, item.name);
        // helper.setText(R.id.tvActor, item.actor);
        ImageView ivThumb = helper.getView(R.id.ivThumb);
        // 使用Glide加载图片
        if (!TextUtils.isEmpty(item.pic)) {
            GlideHelper.loadImage(ivThumb, DefaultConfig.checkReplaceProxy(item.pic), 300, 400);
        } else {
            ivThumb.setImageResource(R.drawable.img_loading_placeholder);
        }
    }
}