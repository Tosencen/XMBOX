package com.github.tvbox.osc.ui.dialog;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.blankj.utilcode.util.ScreenUtils;
import com.xmbox.app.R;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.ui.adapter.SearchSourceAdapter;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;

public class SearchSourceDialog extends BaseDialog {

    private TvRecyclerView mGridView;
    private SearchSourceAdapter searchSourceAdapter;
    private final List<SourceBean> mSourceList;
    public HashMap<String, String> mCheckSources;

    public SearchSourceDialog(@NonNull @NotNull Context context, List<SourceBean> sourceList, HashMap<String, String> checkedSources) {
        super(context, R.style.Widget_App_Dialog_M3);
        if (context instanceof Activity) {
            setOwnerActivity((Activity) context);
        }

        setCancelable(true);
        mSourceList = sourceList;
        mCheckSources = checkedSources;
        setContentView(R.layout.dialog_search_source_md3);
        initView();
    }

    @Override
    public void dismiss() {
        searchSourceAdapter.setMCheckedSources();
        super.dismiss();
    }

    protected void initView() {
        mGridView = findViewById(R.id.mGridView);
        searchSourceAdapter = new SearchSourceAdapter(new DiffUtil.ItemCallback<SourceBean>() {
            @Override
            public boolean areItemsTheSame(@NonNull SourceBean oldItem, @NonNull SourceBean newItem) {
                return oldItem.getKey().equals(newItem.getKey());
            }

            @Override
            public boolean areContentsTheSame(@NonNull SourceBean oldItem, @NonNull SourceBean newItem) {
                return oldItem.getName().equals(newItem.getName());
            }
        });
        mGridView.setHasFixedSize(true);
        mGridView.setLayoutManager(new V7GridLayoutManager(getContext(), 2));
        mGridView.setAdapter(searchSourceAdapter);
        searchSourceAdapter.setData(mSourceList, mCheckSources);

        int pos = 0;
        if (mSourceList != null && mCheckSources != null) {
            for(int i=0; i<mSourceList.size(); i++) {
                String key = mSourceList.get(i).getKey();
                if (mCheckSources.containsKey(key)) {
                    pos = i;
                    break;
                }
            }
        }
        // 确保滚动位置有效
        final int scrollPosition = (pos >= 0 && mSourceList != null && pos < mSourceList.size()) ? pos : 0;
        mGridView.post(() -> {
            // 再次检查适配器是否已经设置并且有数据
            if (mGridView.getAdapter() != null && mGridView.getAdapter().getItemCount() > 0) {
                mGridView.smoothScrollToPosition(scrollPosition);
                mGridView.setSelectionWithSmooth(scrollPosition);
            }
        });
    }
}
