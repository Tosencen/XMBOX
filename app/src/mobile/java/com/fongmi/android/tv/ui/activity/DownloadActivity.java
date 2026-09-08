package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Download;
import com.fongmi.android.tv.databinding.ActivityDownloadBinding;
import com.fongmi.android.tv.service.DownloadService;
import com.fongmi.android.tv.ui.adapter.DownloadAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class DownloadActivity extends BaseActivity implements DownloadAdapter.OnClickListener {

    private ActivityDownloadBinding mBinding;
    private DownloadAdapter mAdapter;
    private Handler mHandler;
    private Runnable mRefreshRunnable;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, DownloadActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityDownloadBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        setRecyclerView();
        getDownloads();
    }

    @Override
    protected void initEvent() {
        mBinding.back.setOnClickListener(v -> finish());
        mBinding.delete.setOnClickListener(this::onDelete);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startRefresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRefresh();
    }

    private void setRecyclerView() {
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.setLayoutManager(new LinearLayoutManager(this));
        mBinding.recycler.setAdapter(mAdapter = new DownloadAdapter(this));
    }

    private void getDownloads() {
        List<Download> downloads = Download.getAll();
        mAdapter.addAll(downloads);
        updateEmptyState();
    }

    private void updateEmptyState() {
        boolean isEmpty = mAdapter.isEmpty();
        mBinding.emptyLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        mBinding.recycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        mBinding.delete.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void onDelete(View view) {
        if (mAdapter.isEmpty()) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_delete_record)
                .setMessage(R.string.dialog_delete_download)
                .setNegativeButton(R.string.dialog_negative, null)
                .setPositiveButton(R.string.dialog_positive, (dialog, which) -> {
                    // 删除所有下载
                    Download.clear();
                    mAdapter.addAll(Download.getAll());
                    updateEmptyState();
                })
                .show();
    }

    @Override
    public void onItemClick(Download item) {
        // 可以点击打开已下载的视频
    }

    @Override
    public void onActionClick(Download item) {
        // 删除单个下载
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_delete_record)
                .setMessage("Delete " + item.getVodName() + "?")
                .setNegativeButton(R.string.dialog_negative, null)
                .setPositiveButton(R.string.dialog_positive, (dialog, which) -> {
                    // 停止下载服务
                    Intent intent = new Intent(this, DownloadService.class);
                    intent.setAction(DownloadService.ACTION_STOP);
                    intent.putExtra("id", item.getId());
                    startService(intent);

                    // 删除记录
                    App.execute(item::delete);
                    mAdapter.remove(item);
                    updateEmptyState();
                })
                .show();
    }

    private void startRefresh() {
        mHandler = new Handler(Looper.getMainLooper());
        mRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                // 刷新下载列表
                List<Download> downloads = Download.getAll();
                for (Download download : downloads) {
                    mAdapter.update(download);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mHandler.post(mRefreshRunnable);
    }

    private void stopRefresh() {
        if (mHandler != null && mRefreshRunnable != null) {
            mHandler.removeCallbacks(mRefreshRunnable);
        }
    }
}
