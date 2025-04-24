package com.github.tvbox.osc.ui.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.SearchCategoryBean;
import com.github.tvbox.osc.databinding.ActivitySearchDetailBinding;
import com.github.tvbox.osc.ui.adapter.FastSearchAdapter;
import com.github.tvbox.osc.ui.adapter.SearchCategoryAdapter;
import com.github.tvbox.osc.util.FastClickCheckUtil;

import java.util.ArrayList;
import java.util.List;

public class SearchDetailActivity extends BaseActivity {
    private static final String TAG = "SearchDetailActivity";
    // 视图引用
    private ImageView ivBack;
    private ImageView ivSearch;
    private TextView tvTitle;
    private RecyclerView rvCategory;
    private RecyclerView rvContent;
    private SearchCategoryAdapter categoryAdapter;
    private FastSearchAdapter contentAdapter;
    private String searchTitle;
    private List<Movie.Video> allVideos = new ArrayList<>();

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_search_detail;
    }

    @Override
    protected void init() {
        initView();
        initData();
    }

    private void initView() {
        // 初始化视图引用
        ivBack = findViewById(R.id.iv_back);
        ivSearch = findViewById(R.id.iv_search);
        tvTitle = findViewById(R.id.tv_title);
        rvCategory = findViewById(R.id.rv_category);
        rvContent = findViewById(R.id.rv_content);

        ivBack.setOnClickListener(v -> finish());
        ivSearch.setOnClickListener(v -> {
            jumpActivity(FastSearchActivity.class);
        });

        // 初始化左侧分类列表
        categoryAdapter = new SearchCategoryAdapter();
        rvCategory.setLayoutManager(new LinearLayoutManager(this));
        rvCategory.setAdapter(categoryAdapter);
        categoryAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                FastClickCheckUtil.check(view);
                categoryAdapter.setSelectedPosition(position);
                filterContent(categoryAdapter.getData().get(position));
            }
        });

        // 初始化右侧内容列表
        contentAdapter = new FastSearchAdapter();
        rvContent.setLayoutManager(new LinearLayoutManager(this));
        rvContent.setAdapter(contentAdapter);
        contentAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                FastClickCheckUtil.check(view);
                Movie.Video video = contentAdapter.getData().get(position);
                if (video != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("id", video.id);
                    bundle.putString("sourceKey", video.sourceKey);
                    jumpActivity(DetailActivity.class, bundle);
                }
            }
        });
    }

    private void initData() {
        if (getIntent() != null && getIntent().hasExtra("title")) {
            searchTitle = getIntent().getStringExtra("title");
            if (!TextUtils.isEmpty(searchTitle)) {
                tvTitle.setText(searchTitle);
                loadCategories();
            }
        }
    }

    private void loadCategories() {
        List<SearchCategoryBean> categories = new ArrayList<>();

        // 添加全部显示选项
        categories.add(new SearchCategoryBean("全部显示", "all", true, "App", false));

        // 添加其他分类，这里根据截图中的分类添加
        categories.add(new SearchCategoryBean("天天", "tiantian", false, "App", false));
        categories.add(new SearchCategoryBean("追剧", "zhuiju", false, "App", false));
        categories.add(new SearchCategoryBean("热播", "rebo", false, "App", false));
        categories.add(new SearchCategoryBean("巧技", "qiaoji", false, "App", true));
        categories.add(new SearchCategoryBean("时常", "shichang", false, "App", false));
        categories.add(new SearchCategoryBean("巧技三", "qiaoji3", false, "QD4K", false));
        categories.add(new SearchCategoryBean("少年棋士", "shaonian", false, "App", false));
        categories.add(new SearchCategoryBean("巧技", "qiaoji2", false, "App", false));

        categoryAdapter.setNewData(categories);
        // 确保选中第一个选项
        categoryAdapter.setSelectedPosition(0);

        // 模拟加载视频数据
        loadVideoData();
    }

    private void loadVideoData() {
        // 这里应该是从API获取数据，这里只是模拟
        List<Movie.Video> videos = new ArrayList<>();

        // 模拟添加一些视频数据
        for (int i = 0; i < 10; i++) {
            Movie.Video video = new Movie.Video();
            video.name = searchTitle + " - 第" + (i + 1) + "集";
            video.note = "更新至第" + (i + 40) + "集";
            video.id = String.valueOf(i);
            video.sourceKey = categoryAdapter.getData().get(i % categoryAdapter.getData().size()).getKey();
            videos.add(video);
        }

        allVideos.addAll(videos);
        contentAdapter.setNewData(videos);
    }

    private void filterContent(SearchCategoryBean category) {
        if ("all".equals(category.getKey())) {
            contentAdapter.setNewData(allVideos);
            return;
        }

        List<Movie.Video> filteredVideos = new ArrayList<>();
        for (Movie.Video video : allVideos) {
            if (category.getKey().equals(video.sourceKey)) {
                filteredVideos.add(video);
            }
        }

        contentAdapter.setNewData(filteredVideos);
    }
}
