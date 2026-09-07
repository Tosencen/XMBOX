package com.fongmi.android.tv.ui.dialog;

import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.DialogWebdavBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.WebDAVSyncManager;
import com.github.catvod.utils.Logger;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class WebDAVDialog {

    // 预设的WebDAV服务提供商
    private static final String[] PROVIDERS = {
        "坚果云",
        "Nextcloud",
        "ownCloud",
        "自定义"
    };
    
    private static final String[] PROVIDER_URLS = {
        "https://dav.jianguoyun.com/dav/XMBOX/",  // 坚果云（添加XMBOX子目录，方便在网页版查看）
        "",  // Nextcloud（需要用户输入）
        "",  // ownCloud（需要用户输入）
        ""   // 自定义（需要用户输入）
    };

    private final DialogWebdavBinding binding;
    private final Fragment fragment;
    private AlertDialog dialog;
    private WebDAVSyncManager syncManager;
    private int selectedProvider = 0;  // 默认选择坚果云
    private boolean isInitializing = false;  // 标记是否正在初始化，防止初始化时触发监听器

    public static WebDAVDialog create(Fragment fragment) {
        return new WebDAVDialog(fragment);
    }

    public WebDAVDialog(Fragment fragment) {
        this.fragment = fragment;
        this.binding = DialogWebdavBinding.inflate(LayoutInflater.from(fragment.getContext()));
        this.syncManager = WebDAVSyncManager.get();
    }

    public void show() {
        initDialog();
        initView();
        initEvent();
    }

    private void initDialog() {
        dialog = new MaterialAlertDialogBuilder(binding.getRoot().getContext())
            .setTitle("WebDAV 配置")
            .setView(binding.getRoot())
            .setPositiveButton("保存", this::onPositive)
            .setNegativeButton("取消", this::onNegative)
            .create();
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void initView() {
        isInitializing = true;  // 标记开始初始化
        
        // 加载已保存的配置
        String url = Setting.getWebDAVUrl();
        String username = Setting.getWebDAVUsername();
        String password = Setting.getWebDAVPassword();
        boolean autoSync = Setting.isWebDAVAutoSync();
        int interval = Setting.getWebDAVSyncInterval();

        // 根据保存的URL判断是哪个服务提供商
        selectedProvider = getProviderIndexByUrl(url);
        binding.providerText.setText(PROVIDERS[selectedProvider]);
        
        // 根据选择的服务提供商决定是否显示URL输入框
        if (selectedProvider == PROVIDERS.length - 1) {
            // 自定义，显示URL输入框
            binding.urlInput.setVisibility(View.VISIBLE);
            binding.urlText.setText(url);
            if (!TextUtils.isEmpty(url)) {
                binding.urlText.setSelection(url.length());
            }
        } else if (selectedProvider == 0) {
            // 坚果云，永远隐藏输入框（有预设URL）
            binding.urlInput.setVisibility(View.GONE);
        } else {
            // Nextcloud或ownCloud需要用户输入URL
            binding.urlInput.setVisibility(View.VISIBLE);
            binding.urlText.setText(url);
            if (!TextUtils.isEmpty(url)) {
                binding.urlText.setSelection(url.length());
            }
        }

        binding.usernameText.setText(username);
        binding.passwordText.setText(password);
        binding.autoSyncSwitch.setChecked(autoSync);
        binding.syncIntervalText.setText(String.valueOf(interval));
        
        // 根据自动同步开关显示/隐藏同步间隔
        updateSyncIntervalVisibility(autoSync);
        
        isInitializing = false;  // 初始化完成
    }
    
    /**
     * 根据URL判断是哪个服务提供商
     */
    private int getProviderIndexByUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return 0; // 默认坚果云
        }
        if (url.contains("jianguoyun.com")) {
            return 0; // 坚果云
        }
        if (url.contains("nextcloud")) {
            return 1; // Nextcloud
        }
        if (url.contains("owncloud")) {
            return 2; // ownCloud
        }
        return PROVIDERS.length - 1; // 自定义
    }
    
    /**
     * 获取当前选择的服务提供商的URL
     */
    private String getProviderUrl() {
        if (selectedProvider < PROVIDER_URLS.length && !TextUtils.isEmpty(PROVIDER_URLS[selectedProvider])) {
            return PROVIDER_URLS[selectedProvider];
        }
        return "";
    }

    private void initEvent() {
        // 服务提供商选择
        binding.providerText.setOnClickListener(v -> onSelectProvider());

        // 自动同步开关监听（立即保存状态）
        // 使用setOnClickListener而不是setOnCheckedChangeListener，避免覆盖CustomSwitch内部的动画监听器
        // AppCompatCheckBox会自动处理状态切换，我们只需要在状态切换后获取新状态
        binding.autoSyncSwitch.setOnClickListener(v -> {
            // 防止初始化时触发监听器
            if (isInitializing) {
                return;
            }
            // 使用post()确保在状态切换后获取新状态
            binding.autoSyncSwitch.post(() -> {
                boolean newState = binding.autoSyncSwitch.isChecked();
                // 立即保存自动同步状态
                Setting.putWebDAVAutoSync(newState);
                // 更新同步间隔的可见性
                updateSyncIntervalVisibility(newState);
            });
        });

        // 测试连接按钮
        binding.testButton.setOnClickListener(v -> onTestConnection());

        // 立即同步按钮
        binding.syncButton.setOnClickListener(v -> onSyncNow());

        // 同步间隔点击（弹出选择对话框）
        binding.syncIntervalContainer.setOnClickListener(v -> onSelectInterval());

        // 密码输入框回车键
        binding.passwordText.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                return true;
            }
            return false;
        });
    }
    
    private void onSelectProvider() {
        new MaterialAlertDialogBuilder(binding.getRoot().getContext())
            .setTitle("选择服务提供商")
            .setSingleChoiceItems(PROVIDERS, selectedProvider, (dialog, which) -> {
                selectedProvider = which;
                binding.providerText.setText(PROVIDERS[which]);
                
                // 如果是自定义，显示URL输入框
                if (which == PROVIDERS.length - 1) {
                    binding.urlInput.setVisibility(View.VISIBLE);
                    String currentUrl = binding.urlText.getText().toString().trim();
                    if (TextUtils.isEmpty(currentUrl)) {
                        binding.urlText.setText("");
                    }
                } else {
                    // 使用预设的URL
                    binding.urlInput.setVisibility(View.GONE);
                    String providerUrl = getProviderUrl();
                    if (!TextUtils.isEmpty(providerUrl)) {
                        // URL会在保存时自动填充
                    } else {
                        // Nextcloud或ownCloud需要用户输入URL
                        binding.urlInput.setVisibility(View.VISIBLE);
                        binding.urlText.setHint("请输入" + PROVIDERS[which] + "服务器地址");
                    }
                }
                dialog.dismiss();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void updateSyncIntervalVisibility(boolean visible) {
        binding.syncIntervalContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void onTestConnection() {
        String url = getServerUrl();
        String username = binding.usernameText.getText().toString().trim();
        String password = binding.passwordText.getText().toString().trim();

        if (TextUtils.isEmpty(url)) {
            showStatus("请选择服务提供商或输入服务器地址", false);
            return;
        }
        if (TextUtils.isEmpty(username)) {
            showStatus("请输入用户名", false);
            return;
        }
        if (TextUtils.isEmpty(password)) {
            showStatus("请输入密码", false);
            return;
        }

        // 临时保存配置用于测试
        Setting.putWebDAVUrl(url);
        Setting.putWebDAVUsername(username);
        Setting.putWebDAVPassword(password);
        
        // 重新加载配置
        syncManager.reloadConfig();

        showStatus("正在测试连接...", true);
        binding.testButton.setEnabled(false);

        // 在后台线程测试连接
        App.execute(() -> {
            boolean success = syncManager.getConfig().testConnection().success;
            App.post(() -> {
                // 检查对话框是否还存在
                if (binding == null || dialog == null || !dialog.isShowing()) {
                    return;
                }
                binding.testButton.setEnabled(true);
                if (success) {
                    showStatus("连接成功！", true);
                } else {
                    showStatus("连接失败，请检查配置", false);
                }
            });
        });
    }

    private void onSyncNow() {
        // 先临时保存当前配置用于测试同步
        String url = getServerUrl();
        String username = binding.usernameText.getText().toString().trim();
        String password = binding.passwordText.getText().toString().trim();
        
        // 验证输入
        if (TextUtils.isEmpty(url)) {
            showStatus("请选择服务提供商或输入服务器地址", false);
            return;
        }
        if (TextUtils.isEmpty(username)) {
            showStatus("请输入用户名", false);
            return;
        }
        if (TextUtils.isEmpty(password)) {
            showStatus("请输入密码", false);
            return;
        }
        
        // 临时保存配置用于同步
        Setting.putWebDAVUrl(url);
        Setting.putWebDAVUsername(username);
        Setting.putWebDAVPassword(password);
        syncManager.reloadConfig();
        
        if (!syncManager.isConfigured()) {
            showStatus("配置无效，无法同步", false);
            return;
        }

        showStatus("正在同步...", true);
        binding.syncButton.setEnabled(false);

        // 在后台线程执行同步
        App.execute(() -> {
            try {
                // 先上传本地记录
                syncManager.uploadHistory();
                // 再下载远程记录并合并
                boolean downloadSuccess = syncManager.downloadHistory();
                
                App.post(() -> {
                    // 检查对话框是否还存在
                    if (binding == null || dialog == null || !dialog.isShowing()) {
                        return;
                    }
                    binding.syncButton.setEnabled(true);
                    if (downloadSuccess) {
                        showStatus("同步完成", true);
                        Notify.show("同步完成");
                    } else {
                        showStatus("同步完成（本地数据已上传）", true);
                        Notify.show("同步完成");
                    }
                });
            } catch (Exception e) {
                App.post(() -> {
                    // 检查对话框是否还存在
                    if (binding == null || dialog == null || !dialog.isShowing()) {
                        return;
                    }
                    binding.syncButton.setEnabled(true);
                    showStatus("同步失败：" + e.getMessage(), false);
                    Notify.show("同步失败");
                    Logger.e("WebDAV: 同步失败: " + e.getMessage());
                });
            }
        });
    }

    private void onSelectInterval() {
        String[] intervals = {"15", "30", "60", "120", "240"};
        int currentInterval = Setting.getWebDAVSyncInterval();
        int selectedIndex = 0;
        for (int i = 0; i < intervals.length; i++) {
            if (Integer.parseInt(intervals[i]) == currentInterval) {
                selectedIndex = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(binding.getRoot().getContext())
            .setTitle("选择同步间隔")
            .setSingleChoiceItems(intervals, selectedIndex, (dialog, which) -> {
                int interval = Integer.parseInt(intervals[which]);
                binding.syncIntervalText.setText(String.valueOf(interval));
                // 立即保存同步间隔
                Setting.putWebDAVSyncInterval(interval);
                dialog.dismiss();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void showStatus(String message, boolean isSuccess) {
        // 检查对话框是否还存在
        if (binding == null || dialog == null || !dialog.isShowing()) {
            return;
        }
        binding.statusText.setText(message);
        binding.statusText.setVisibility(TextUtils.isEmpty(message) ? View.GONE : View.VISIBLE);
        // 可以根据isSuccess设置不同的颜色
        binding.statusText.setTextColor(isSuccess ? 
            fragment.getResources().getColor(R.color.white) : 
            fragment.getResources().getColor(android.R.color.holo_red_dark));
    }

    /**
     * 获取服务器URL（根据选择的服务提供商）
     */
    private String getServerUrl() {
        if (selectedProvider == PROVIDERS.length - 1) {
            // 自定义，从输入框获取
            return binding.urlText.getText().toString().trim();
        } else {
            // 使用预设URL或从输入框获取（Nextcloud/ownCloud）
            String providerUrl = getProviderUrl();
            if (!TextUtils.isEmpty(providerUrl)) {
                return providerUrl;
            } else {
                // Nextcloud或ownCloud需要用户输入
                return binding.urlText.getText().toString().trim();
            }
        }
    }

    private void onPositive(DialogInterface dialog, int which) {
        String url = getServerUrl();
        String username = binding.usernameText.getText().toString().trim();
        String password = binding.passwordText.getText().toString().trim();
        boolean autoSync = binding.autoSyncSwitch.isChecked();
        int interval = Integer.parseInt(binding.syncIntervalText.getText().toString());

        // 验证输入
        if (TextUtils.isEmpty(url)) {
            Notify.show("请选择服务提供商或输入服务器地址");
            return;
        }
        if (TextUtils.isEmpty(username)) {
            Notify.show("请输入用户名");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            Notify.show("请输入密码");
            return;
        }

        // 保存配置
        Setting.putWebDAVUrl(url);
        Setting.putWebDAVUsername(username);
        Setting.putWebDAVPassword(password);
        Setting.putWebDAVAutoSync(autoSync);
        Setting.putWebDAVSyncInterval(interval);

        // 重新加载配置
        syncManager.reloadConfig();

        // 配置保存后，立即执行一次同步（下载远程数据）
        // 这样新设备配置后就能立即看到其他设备的历史记录
        if (syncManager.isConfigured()) {
            Notify.show("WebDAV配置已保存，正在同步数据...");
            App.execute(() -> {
                try {
                    // 先上传本地记录
                    syncManager.uploadHistory();
                    // 再下载远程记录并合并
                    boolean downloadSuccess = syncManager.downloadHistory();
                    App.post(() -> {
                        if (downloadSuccess) {
                            Notify.show("同步完成，已获取远程观看记录");
                        } else {
                            Notify.show("同步完成（本地数据已上传）");
                        }
                    });
                } catch (Exception e) {
                    App.post(() -> {
                        Notify.show("同步失败，请检查网络连接");
                    });
                }
            });
        } else {
            Notify.show("WebDAV配置已保存");
        }
        
        dialog.dismiss();
        
        // 通知设置界面更新状态（通过RefreshEvent）
        // 使用App.post确保对话框关闭后再发送事件，让状态能及时更新
        App.post(() -> RefreshEvent.config());
    }

    private void onNegative(DialogInterface dialog, int which) {
        dialog.dismiss();
    }

    /**
     * 重新加载配置（用于外部调用）
     */
    public void reloadConfig() {
        syncManager.reloadConfig();
    }
}

