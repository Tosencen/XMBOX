package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.databinding.ActivityHistoryBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.ui.adapter.HistoryAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.dialog.SyncDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.airbnb.lottie.LottieAnimationView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

public class HistoryActivity extends BaseActivity implements HistoryAdapter.OnClickListener {

    private ActivityHistoryBinding mBinding;
    private HistoryAdapter mAdapter;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, HistoryActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityHistoryBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        setRecyclerView();
        getHistory();
    }

    @Override
    protected void initEvent() {
        mBinding.back.setOnClickListener(v -> finish());
        mBinding.sync.setOnClickListener(this::onSync);
        mBinding.delete.setOnClickListener(this::onDelete);
    }

    private void setRecyclerView() {
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.getItemAnimator().setChangeDuration(0);
        mBinding.recycler.setLayoutManager(new GridLayoutManager(this, Product.getColumn(this)));
        mBinding.recycler.setAdapter(mAdapter = new HistoryAdapter(this));
        mAdapter.setSize(Product.getSpec(getActivity()));
    }

    private void getHistory() {
        // 历史记录读数据库移到后台线程，避免主线程访问 Room
        App.execute(() -> {
            List<History> items = History.getAll(); // 显示所有视频源的观看记录
            App.post(() -> {
                mAdapter.addAll(items);
                mBinding.delete.setVisibility(mAdapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);
                updateEmptyState();
            });
        });
    }

    private void updateEmptyState() {
        boolean isEmpty = mAdapter.getItemCount() == 0;
        mBinding.emptyLayout.getRoot().setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        mBinding.recycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        
        // 控制Lottie动画播放
        if (isEmpty) {
            try {
                LottieAnimationView lottieView = mBinding.emptyLayout.getRoot().findViewById(R.id.lottieAnimation);
                if (lottieView != null) {
                    lottieView.playAnimation();
                }
            } catch (Exception e) {
                // 忽略错误
            }
        }
    }

    private void onSync(View view) {
        // 同步数据（历史/配置）读取移到后台线程，读完后回主线程弹窗
        App.execute(() -> {
            SyncDialog dialog = SyncDialog.create().history();
            App.post(() -> dialog.show(this));
        });
    }

    private void onDelete(View view) {
        if (mAdapter.isDelete()) {
            new MaterialAlertDialogBuilder(this).setTitle(R.string.dialog_delete_record).setMessage(R.string.dialog_delete_history).setNegativeButton(R.string.dialog_negative, null).setPositiveButton(R.string.dialog_positive, (dialog, which) -> {
                mAdapter.clear();
                updateEmptyState();
            }).show();
        } else if (mAdapter.getItemCount() > 0) {
            mAdapter.setDelete(true);
        } else {
            mBinding.delete.setVisibility(View.GONE);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (event.getType().equals(RefreshEvent.Type.HISTORY)) getHistory();
    }

    @Override
    public void onItemClick(History item) {
        VideoActivity.start(this, item.getSiteKey(), item.getVodId(), item.getVodName(), item.getVodPic());
    }

    @Override
    public void onItemDelete(History item) {
        App.execute(item::delete);
        mAdapter.remove(item);
        if (mAdapter.getItemCount() > 0) return;
        mBinding.delete.setVisibility(View.GONE);
        mAdapter.setDelete(false);
        updateEmptyState();
    }

    @Override
    public boolean onLongClick() {
        mAdapter.setDelete(!mAdapter.isDelete());
        return true;
    }

    @Override
    public void onBackPressed() {
        if (mAdapter.isDelete()) mAdapter.setDelete(false);
        else super.onBackPressed();
    }
}
