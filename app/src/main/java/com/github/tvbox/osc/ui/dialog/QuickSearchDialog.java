package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.LogUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.xmbox.app.R;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.adapter.QuickSearchAdapter;
import com.github.tvbox.osc.ui.adapter.SearchWordAdapter;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class QuickSearchDialog extends BaseDialog {
    private SearchWordAdapter searchWordAdapter;
    private QuickSearchAdapter searchAdapter;
    private TvRecyclerView mGridView;
    private TvRecyclerView mGridViewWord;
    List<Movie.Video> results = new ArrayList<>();

    public QuickSearchDialog(@NonNull @NotNull Context context) {
        super(context, R.style.CustomDialogStyleDim);
        setCanceledOnTouchOutside(true);
        setCancelable(true);
        setContentView(R.layout.dialog_quick_search);
        init(context);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_QUICK_SEARCH) {
            if (event.obj != null) {
                List<Movie.Video> data = (List<Movie.Video>) event.obj;
                results.addAll(data);
                searchAdapter.notifyDataSetChanged();
            }
        } else if (event.type == RefreshEvent.TYPE_QUICK_SEARCH_WORD) {
            if (event.obj != null) {
                List<String> data = (List<String>) event.obj;
                searchWordAdapter.setNewData(data);
            }
        }
    }

    private void init(Context context) {
        EventBus.getDefault().register(this);
        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                // 修复内存泄漏，正确地取消注册EventBus
                if (EventBus.getDefault().isRegistered(QuickSearchDialog.this)) {
                    EventBus.getDefault().unregister(QuickSearchDialog.this);
                }

                // 清理资源
                if (searchAdapter != null) {
                    results.clear();
                }
                if (searchWordAdapter != null) {
                    searchWordAdapter.setNewData(null);
                }
            }
        });
        mGridView = findViewById(R.id.mGridView);
        searchAdapter = new QuickSearchAdapter();
        mGridView.setHasFixedSize(true);
        // lite
        mGridView.setLayoutManager(new V7LinearLayoutManager(getContext(), 1, false));
        // with preview
        // mGridView.setLayoutManager(new V7GridLayoutManager(getContext(), 3));
        mGridView.setAdapter(searchAdapter);
        searchAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                Movie.Video video = searchAdapter.getData().get(position);
                EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH_SELECT, video));
                dismiss();
            }
        });

        searchAdapter.setNewData(results);
        searchWordAdapter = new SearchWordAdapter();
        mGridViewWord = findViewById(R.id.mGridViewWord);
        mGridViewWord.setAdapter(searchWordAdapter);
        mGridViewWord.setLayoutManager(new V7LinearLayoutManager(context, 0, false));
        searchWordAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                searchAdapter.getData().clear();
                searchAdapter.notifyDataSetChanged();
                EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH_WORD_CHANGE, searchWordAdapter.getData().get(position)));
            }
        });
        searchWordAdapter.setNewData(new ArrayList<>());
    }
}