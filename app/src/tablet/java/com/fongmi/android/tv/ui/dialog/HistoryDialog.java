package com.fongmi.android.tv.ui.dialog;
import com.github.catvod.utils.Logger;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.databinding.DialogHistoryBinding;
import com.fongmi.android.tv.impl.ConfigCallback;
import com.fongmi.android.tv.ui.adapter.ConfigAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.Notify;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class HistoryDialog implements ConfigAdapter.OnClickListener {

    private final DialogHistoryBinding binding;
    private final ConfigCallback callback;
    private final ConfigAdapter adapter;
    private final AlertDialog dialog;
    private int type;

    public static HistoryDialog create(Fragment fragment) {
        return new HistoryDialog(fragment);
    }

    public HistoryDialog type(int type) {
        this.type = type;
        return this;
    }

    public HistoryDialog(Fragment fragment) {
        this.callback = (ConfigCallback) fragment;
        this.binding = DialogHistoryBinding.inflate(LayoutInflater.from(fragment.getContext()));
        this.dialog = new MaterialAlertDialogBuilder(fragment.getActivity()).setView(binding.getRoot()).create();
        this.adapter = new ConfigAdapter(this);
    }

    public void show() {
        setRecyclerView();
        setDialog();
    }

    private void setRecyclerView() {
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setAdapter(adapter);
        adapter.addAll(type);
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 8));
    }

    private void setDialog() {
        if (adapter.getItemCount() == 0) return;
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    @Override
    public void onTextClick(Config item) {
        // 防止重复点击和空值
        if (!dialog.isShowing() || item == null) return;
        
        // 检查callback是否有效
        if (callback == null) {
            dialog.dismiss();
            return;
        }
        
        // 先关闭对话框，避免时序冲突
        dialog.dismiss();
        
        // 延迟执行配置设置，确保对话框完全关闭
        App.post(() -> {
            try {
                // 双重检查callback和item是否仍然有效
                if (callback != null && item != null && !item.isEmpty()) {
                    callback.setConfig(item);
                }
            } catch (Exception e) {
                Logger.e("Error", e);
                // 如果出现异常，显示错误提示
                try {
                    Notify.show("配置切换失败: " + e.getMessage());
                } catch (Exception ex) {
                    Logger.e("Error", e);
                }
            }
        }, 150); // 增加延迟到150毫秒
    }

    @Override
    public void onCopyClick(Config item) {
        ClipboardManager manager = (ClipboardManager) App.get().getSystemService(Context.CLIPBOARD_SERVICE);
        manager.setPrimaryClip(ClipData.newPlainText("url", item.getUrl()));
        Notify.showCenter("复制成功");
    }

    @Override
    public void onDeleteClick(Config item) {
        int count = adapter.remove(item);
        if (count == 0) {
            dialog.dismiss();
        } else {
            // 强制重新测量布局高度
            binding.recycler.requestLayout();
            dialog.getWindow().setLayout(dialog.getWindow().getAttributes().width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
