package com.github.tvbox.osc.ui.adapter;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.xmbox.app.R;
import com.github.tvbox.osc.bean.Movie;
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
public class GridAdapter extends BaseQuickAdapter<Movie.Video, BaseViewHolder> {

    public GridAdapter() {
        super(R.layout.item_grid, new ArrayList<>());

    }

    @Override
    protected void convert(BaseViewHolder helper, Movie.Video item) {

        TextView tvYear = helper.getView(R.id.tvYear);
        if (item.year <= 0) {
            tvYear.setVisibility(View.GONE);
        } else {
            tvYear.setText(String.valueOf(item.year));
            tvYear.setVisibility(View.VISIBLE);
        }

        if (TextUtils.isEmpty(item.note)) {
            helper.setVisible(R.id.tvNote, false);
        } else {
            helper.setVisible(R.id.tvNote, true);
            helper.setText(R.id.tvNote, item.note);
        }
        helper.setText(R.id.tvName, item.name);
        ImageView ivThumb = helper.getView(R.id.ivThumb);
        // 使用GlideHelper的完整处理流程，包括图片URL清理
        if (!TextUtils.isEmpty(item.pic)) {
            item.pic = item.pic.trim();

            android.util.Log.d("GridAdapter", "加载首页图片: " + item.name);
            android.util.Log.d("GridAdapter", "原始URL: " + item.pic);

            // 专门调试ok杰克源的图片问题
            if (item.name != null && (item.name.contains("ok") || item.name.contains("杰克"))) {
                com.github.tvbox.osc.util.ImageUrlDebugger.debugImageUrl(item.pic, "ok杰克", item.name);
            }

            // 使用会完整处理URL的加载方法（包括清理@Referer=等参数）
            GlideHelper.loadImage(ivThumb, item.pic, 300, 400);
        } else {
            android.util.Log.d("GridAdapter", "首页图片URL为空: " + item.name);
            ivThumb.setImageResource(R.drawable.img_loading_placeholder);
        }
    }
}