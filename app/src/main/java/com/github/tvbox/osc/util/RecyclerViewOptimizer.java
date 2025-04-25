package com.github.tvbox.osc.util;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

/**
 * RecyclerView优化工具类
 * 提供RecyclerView性能优化的方法
 */
public class RecyclerViewOptimizer {

    /**
     * 优化RecyclerView配置
     * @param recyclerView 要优化的RecyclerView
     */
    public static void optimize(RecyclerView recyclerView) {
        if (recyclerView == null) return;

        // 设置固定大小，提高性能
        recyclerView.setHasFixedSize(true);

        // 设置项目动画为null，避免不必要的动画开销
        recyclerView.setItemAnimator(null);

        // 设置预取器，提前加载即将显示的项目
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager != null) {
            // 设置预取项目数量
            int prefetchCount = calculatePrefetchCount(layoutManager);
            if (layoutManager instanceof LinearLayoutManager) {
                ((LinearLayoutManager) layoutManager).setInitialPrefetchItemCount(prefetchCount);
            }

            // 设置视图缓存大小
            recyclerView.setItemViewCacheSize(30); // 增加缓存大小

            // 启用回收视图池
            RecyclerView.RecycledViewPool viewPool = recyclerView.getRecycledViewPool();
            viewPool.setMaxRecycledViews(0, 24); // 增加默认类型的回收视图数量

            // 对于网格布局，增加每种类型的回收视图数量
            if (layoutManager instanceof GridLayoutManager) {
                int spanCount = ((GridLayoutManager) layoutManager).getSpanCount();
                viewPool.setMaxRecycledViews(0, spanCount * 6); // 每种类型的回收视图数量
            }
        }

        // 禁用嵌套滚动，减少不必要的计算
        recyclerView.setNestedScrollingEnabled(false);

        // 设置绘制缓存质量
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);

        // 禁用过度滚动效果，减少不必要的绘制
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    /**
     * 优化TvRecyclerView配置
     * @param recyclerView 要优化的TvRecyclerView
     */
    public static void optimizeTvRecyclerView(TvRecyclerView recyclerView) {
        if (recyclerView == null) return;

        // 设置固定大小，提高性能
        recyclerView.setHasFixedSize(true);

        // 设置项目动画为null，避免不必要的动画开销
        recyclerView.setItemAnimator(null);

        // 设置视图缓存大小
        recyclerView.setItemViewCacheSize(30); // 增加缓存大小

        // 设置预取器，提前加载即将显示的项目
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager != null) {
            // 设置预取项目数量
            int prefetchCount = calculatePrefetchCount(layoutManager);
            if (layoutManager instanceof V7LinearLayoutManager) {
                ((V7LinearLayoutManager) layoutManager).setInitialPrefetchItemCount(prefetchCount);
            }

            // 启用回收视图池
            RecyclerView.RecycledViewPool viewPool = recyclerView.getRecycledViewPool();
            viewPool.setMaxRecycledViews(0, 24); // 增加默认类型的回收视图数量

            // 对于网格布局，增加每种类型的回收视图数量
            if (layoutManager instanceof V7GridLayoutManager) {
                int spanCount = ((V7GridLayoutManager) layoutManager).getSpanCount();
                viewPool.setMaxRecycledViews(0, spanCount * 6); // 每种类型的回收视图数量
            }
        }

        // 禁用嵌套滚动，减少不必要的计算
        recyclerView.setNestedScrollingEnabled(false);

        // 设置绘制缓存质量
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);

        // 禁用过度滚动效果，减少不必要的绘制
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    /**
     * 计算预取项目数量
     * @param layoutManager 布局管理器
     * @return 预取项目数量
     */
    private static int calculatePrefetchCount(RecyclerView.LayoutManager layoutManager) {
        if (layoutManager instanceof GridLayoutManager) {
            return ((GridLayoutManager) layoutManager).getSpanCount() * 2;
        } else if (layoutManager instanceof V7GridLayoutManager) {
            return ((V7GridLayoutManager) layoutManager).getSpanCount() * 2;
        } else {
            return 4; // 默认预取4个项目
        }
    }

    /**
     * 创建网格间距装饰器
     * @param spanCount 列数
     * @param spacing 间距（像素）
     * @param includeEdge 是否包含边缘
     * @return 网格间距装饰器
     */
    public static RecyclerView.ItemDecoration createGridSpacingDecoration(int spanCount, int spacing, boolean includeEdge) {
        return new GridSpacingItemDecoration(spanCount, spacing, includeEdge);
    }

    /**
     * 网格间距装饰器
     */
    public static class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private final int spanCount;
        private final int spacing;
        private final boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % spanCount;

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount;
                outRect.right = (column + 1) * spacing / spanCount;

                if (position < spanCount) {
                    outRect.top = spacing;
                }
                outRect.bottom = spacing;
            } else {
                outRect.left = column * spacing / spanCount;
                outRect.right = spacing - (column + 1) * spacing / spanCount;

                if (position >= spanCount) {
                    outRect.top = spacing;
                }
            }
        }
    }
}
