package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.databinding.DialogLiveBinding;
import com.fongmi.android.tv.impl.LiveCallback;
import com.fongmi.android.tv.ui.adapter.LiveAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class LiveDialog implements LiveAdapter.OnClickListener {

    private final DialogLiveBinding binding;
    private final LiveCallback callback;
    private final LiveAdapter adapter;
    private final AlertDialog dialog;

    public static LiveDialog create(Activity activity) {
        return new LiveDialog(activity);
    }

    private LiveDialog(Activity activity) {
        this.adapter = new LiveAdapter(this);
        this.callback = (LiveCallback) activity;
        this.binding = DialogLiveBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
    }

    public LiveDialog action() {
        adapter.setAction(true);
        return this;
    }

    public void show() {
        setRecyclerView();
        setDialog();
    }

    private void setRecyclerView() {
        binding.recycler.setAdapter(adapter);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setItemAnimator(null);
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        binding.recycler.post(() -> binding.recycler.scrollToPosition(LiveConfig.getHomeIndex()));
    }

    private void setDialog() {
        if (adapter.getItemCount() == 0) return;
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (ResUtil.getScreenWidth() * 0.4f);
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setDimAmount(0);
        // 设置对话框背景为透明，让布局的深色背景显示
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    @Override
    public void onItemClick(Live item) {
        callback.setLive(item);
        dialog.dismiss();
    }

    @Override
    public void onBootClick(int position, Live item) {
        item.boot(!item.isBoot());
        App.execute(item::save);
        adapter.notifyItemChanged(position);
    }

    @Override
    public void onPassClick(int position, Live item) {
        item.pass(!item.isPass());
        App.execute(item::save);
        adapter.notifyItemChanged(position);
    }

    @Override
    public boolean onBootLongClick(Live item) {
        boolean result = !item.isBoot();
        for (Live live : LiveConfig.get().getLives()) live.boot(result);
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
        App.execute(() -> {
            for (Live live : LiveConfig.get().getLives()) live.save();
        });
        return true;
    }

    @Override
    public boolean onPassLongClick(Live item) {
        boolean result = !item.isPass();
        for (Live live : LiveConfig.get().getLives()) live.pass(result);
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
        App.execute(() -> {
            for (Live live : LiveConfig.get().getLives()) live.save();
        });
        return true;
    }
}
