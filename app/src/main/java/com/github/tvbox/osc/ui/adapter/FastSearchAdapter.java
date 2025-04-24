package com.github.tvbox.osc.ui.adapter;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.GlideHelper;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.MD5;
import com.github.tvbox.osc.util.ThreadPoolManager;
import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import me.jessyan.autosize.utils.AutoSizeUtils;

public class FastSearchAdapter extends BaseQuickAdapter<Movie.Video, BaseViewHolder> {
    // Cache for image dimensions to avoid recalculating
    private static final int IMAGE_WIDTH = 110;
    private static final int IMAGE_HEIGHT = 160;
    private static final int IMAGE_RADIUS = 20;

    // Cache for already processed images to avoid redundant loading
    private Map<String, Boolean> preloadedImages = new HashMap<>();

    public FastSearchAdapter() {
        super(R.layout.item_search, new ArrayList<>());
    }

    /**
     * 优化版本的convert方法，提高图片加载性能
     */
    @Override
    protected void convert(BaseViewHolder helper, Movie.Video item) {
        // 先设置文本内容，提高感知性能
        helper.setText(R.id.tvName, item.name);

        // 添加空值检查，防止空指针异常
        SourceBean sourceBean = ApiConfig.get().getSource(item.sourceKey);
        String siteName = sourceBean != null ? sourceBean.getName() : (item.sourceKey != null ? item.sourceKey : "未知来源");
        helper.setText(R.id.tvSite, siteName);

        // 处理备注
        boolean hasNote = item.note != null && !item.note.isEmpty();
        helper.setVisible(R.id.tvNote, hasNote);
        if (hasNote) {
            helper.setText(R.id.tvNote, item.note);
        }

        // 图片加载优化
        ImageView ivThumb = helper.getView(R.id.ivThumb);

        // 立即设置占位图，提高用户体验
        ivThumb.setImageResource(R.drawable.img_loading_placeholder);

        if (!TextUtils.isEmpty(item.pic)) {
            // 处理图片URL
            final String imageUrl = DefaultConfig.checkReplaceProxy(item.pic);
            final String imageKey = MD5.string2MD5(imageUrl);

            // 缓存尺寸计算结果
            final int width = AutoSizeUtils.dp2px(mContext, IMAGE_WIDTH);
            final int height = AutoSizeUtils.dp2px(mContext, IMAGE_HEIGHT);
            final int radius = AutoSizeUtils.dp2px(mContext, IMAGE_RADIUS);

            // 使用低优先级加载图片
            // 如果需要预加载图片，使用单独的低优先级线程
            if (!preloadedImages.containsKey(imageKey)) {
                preloadedImages.put(imageKey, true);
                // 使用最低优先级线程预加载
                ThreadPoolManager.executeImageLoading(() -> {
                    try {
                        GlideHelper.preloadImage(mContext, imageUrl);
                    } catch (Exception e) {
                        // 忽略预加载错误
                    }
                });
            }

            // 使用延迟加载技术，避免在滚动时加载过多图片
            helper.itemView.post(() -> {
                // 检查视图是否仍然附加到父视图
                if (ivThumb.getWindowToken() != null) {
                    GlideHelper.loadRoundedImage(ivThumb, imageUrl, width, height, radius);
                }
            });
        }
    }

    /**
     * Clear the preloaded images cache when adapter is detached
     */
    @Override
    public void onDetachedFromRecyclerView(androidx.recyclerview.widget.RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        preloadedImages.clear();
    }

    /**
     * 清理数据并释放资源
     */
    public void releaseResources() {
        // 清空数据列表
        this.getData().clear();
        this.notifyDataSetChanged();

        // 清理图片缓存
        preloadedImages.clear();

        // 如果有其他需要释放的资源，可以在这里添加
    }
}