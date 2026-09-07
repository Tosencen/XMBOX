package com.fongmi.android.tv.ui.dialog;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.config.WallConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.databinding.DialogConfigBinding;
import com.fongmi.android.tv.impl.ConfigCallback;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.ui.custom.CustomTextListener;
import com.fongmi.android.tv.utils.FileChooser;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ConfigDialog {

    private final DialogConfigBinding binding;
    private final ConfigCallback callback;
    private final Fragment fragment;
    private AlertDialog dialog;
    private boolean append;
    private boolean edit;
    private String ori;
    private int type;

    public static ConfigDialog create(Fragment fragment) {
        return new ConfigDialog(fragment);
    }

    public ConfigDialog type(int type) {
        this.type = type;
        return this;
    }

    public ConfigDialog edit() {
        this.edit = true;
        return this;
    }

    public ConfigDialog(Fragment fragment) {
        this.fragment = fragment;
        // 确保fragment实现了ConfigCallback接口
        if (!(fragment instanceof ConfigCallback)) {
            throw new IllegalArgumentException("Fragment must implement ConfigCallback");
        }
        this.callback = (ConfigCallback) fragment;
        this.binding = DialogConfigBinding.inflate(LayoutInflater.from(fragment.getContext()));
        this.append = true;
    }

    public void show() {
        initDialog();
        initView();
        initEvent();
    }

    private void initDialog() {
        dialog = new MaterialAlertDialogBuilder(binding.getRoot().getContext()).setTitle(type == 0 ? R.string.setting_vod : type == 1 ? R.string.setting_live : R.string.setting_wall).setView(binding.getRoot()).setPositiveButton(edit ? R.string.dialog_edit : R.string.dialog_positive, this::onPositive).setNegativeButton(R.string.dialog_negative, this::onNegative).create();
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void initView() {
        binding.name.setText(getConfig().getName());
        binding.url.setText(ori = getConfig().getUrl());
        binding.input.setVisibility(edit ? View.VISIBLE : View.GONE);
        binding.url.setSelection(TextUtils.isEmpty(ori) ? 0 : ori.length());
    }

    private void initEvent() {
        binding.choose.setEndIconOnClickListener(this::onChoose);
        binding.paste.setOnClickListener(this::onPaste);
        binding.url.addTextChangedListener(new CustomTextListener() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                detect(s.toString());
            }
        });
        binding.url.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
            return true;
        });
    }

    private Config getConfig() {
        switch (type) {
            case 0:
                return VodConfig.get().getConfig();
            case 1:
                return LiveConfig.get().getConfig();
            case 2:
                return WallConfig.get().getConfig();
            default:
                return null;
        }
    }

    private void onChoose(View view) {
        FileChooser.from(fragment).show();
        dialog.dismiss();
    }

    private void onPaste(View view) {
        try {
            ClipboardManager clipboard = (ClipboardManager) fragment.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null) {
                CharSequence text = clipboard.getPrimaryClip().getItemAt(0).getText();
                if (text != null) {
                    binding.url.setText(text);
                    binding.url.setSelection(text.length());
                }
            }
        } catch (Exception e) {
            Toast.makeText(fragment.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void detect(String s) {
        if (append && "h".equalsIgnoreCase(s)) {
            append = false;
            binding.url.append("ttp://");
        } else if (append && "f".equalsIgnoreCase(s)) {
            append = false;
            binding.url.append("ile://");
        } else if (append && "a".equalsIgnoreCase(s)) {
            append = false;
            binding.url.append("ssets://");
        } else if (s.length() > 1) {
            append = false;
        } else if (s.length() == 0) {
            append = true;
        }
    }

    private void onPositive(DialogInterface dialog, int which) {
        String url = binding.url.getText().toString().trim();
        String name = binding.name.getText().toString().trim();
        
        android.util.Log.d("ConfigDialog", "onPositive: type=" + type + ", url=" + url + ", name=" + name);
        
        // 如果是编辑模式，更新现有配置
        if (edit) Config.find(ori, type).url(url).name(name).update();
        
        // 如果URL为空，删除配置
        if (url.isEmpty()) {
            android.util.Log.d("ConfigDialog", "URL is empty, deleting config");
            Config.delete(ori, type);
            dialog.dismiss();
            return;
        }
        
        // 只有URL不为空时，才设置配置
        // 保存原始URL，以便在添加失败时恢复
        String originalUrl = ori;
        android.util.Log.d("ConfigDialog", "Calling Config.find with url=" + url + ", type=" + type);
        
        Config config = Config.find(url, type);
        android.util.Log.d("ConfigDialog", "Config.find returned: " + (config != null ? config.toString() : "null"));
        
        android.util.Log.d("ConfigDialog", "Checking callback: " + (callback != null ? callback.getClass().getName() : "null"));
        android.util.Log.d("ConfigDialog", "Checking fragment: " + (fragment != null ? fragment.getClass().getName() : "null"));
        
        android.util.Log.d("ConfigDialog", "Calling callback.setConfig");
        callback.setConfig(config);
        
        android.util.Log.d("ConfigDialog", "setConfig completed");
        
        // 添加一个延迟检查，如果配置没有成功加载，则恢复原始URL
        new android.os.Handler().postDelayed(() -> {
            // 检查配置是否成功加载
            Config currentConfig = getConfig();
            if (currentConfig == null || !currentConfig.getUrl().equals(url)) {
                // 配置加载失败，恢复原始URL
                if (!TextUtils.isEmpty(originalUrl)) {
                    // 如果有原始URL，恢复原始URL
                    // Config 读数据库移到后台线程，读完后回主线程设置
                    App.execute(() -> {
                        Config cfg = Config.find(originalUrl, type);
                        App.post(() -> callback.setConfig(cfg));
                    });
                } else {
                    // 如果没有原始URL，设置为空
                    // Config 读数据库移到后台线程，读完后回主线程恢复默认配置
                    App.execute(() -> {
                        Config cfg;
                        switch (type) {
                            case 1: cfg = Config.live(); break;
                            case 2: cfg = Config.wall(); break;
                            default: cfg = Config.vod(); break;
                        }
                        App.post(() -> {
                            switch (type) {
                                case 0:
                                    VodConfig.get().clear().config(cfg).load(new Callback() {
                                        @Override public void success() {}
                                        @Override public void success(String result) {}
                                        @Override public void error(String msg) {}
                                    });
                                    break;
                                case 1:
                                    LiveConfig.get().clear().config(cfg).load(new Callback() {
                                        @Override public void success() {}
                                        @Override public void success(String result) {}
                                        @Override public void error(String msg) {}
                                    });
                                    break;
                                case 2:
                                    WallConfig.get().clear().config(cfg).load(new Callback() {
                                        @Override public void success() {}
                                        @Override public void success(String result) {}
                                        @Override public void error(String msg) {}
                                    });
                                    break;
                            }
                        });
                    });
                }
            }
        }, 2000); // 2秒后检查
        
        dialog.dismiss();
    }

    private void onNegative(DialogInterface dialog, int which) {
        dialog.dismiss();
    }
}
