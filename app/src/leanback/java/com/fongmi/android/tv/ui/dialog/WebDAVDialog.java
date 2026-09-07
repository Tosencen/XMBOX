package com.fongmi.android.tv.ui.dialog;

import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.DialogWebdavBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.WebDAVConfig;
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
    private final FragmentActivity activity;
    private AlertDialog dialog;
    private WebDAVSyncManager syncManager;
    private int selectedProvider = 0;  // 默认选择坚果云
    private boolean isInitializing = false;  // 标记是否正在初始化，防止初始化时触发监听器
    private Handler statusHandler = new Handler(Looper.getMainLooper());
    private Runnable hideStatusRunnable;  // 用于延迟隐藏状态消息

    public static WebDAVDialog create(FragmentActivity activity) {
        return new WebDAVDialog(activity);
    }

    public WebDAVDialog(FragmentActivity activity) {
        this.activity = activity;
        this.binding = DialogWebdavBinding.inflate(LayoutInflater.from(activity));
        this.syncManager = WebDAVSyncManager.get();
    }

    public void show() {
        initDialog();
        initView();
        initEvent();
    }

    private void initDialog() {
        dialog = new MaterialAlertDialogBuilder(activity)
            .setView(binding.getRoot())
            .create();
        
        // 设置对话框大小（适合TV屏幕）
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (ResUtil.getScreenWidth() * 0.45f);
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setDimAmount(0);
        // 设置对话框背景为透明，让布局的深色背景显示
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
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

        // 保存按钮
        binding.positive.setOnClickListener(v -> onPositive(null, 0));

        // 取消按钮
        binding.negative.setOnClickListener(v -> onNegative(null, 0));

        // 密码输入框回车键
        binding.passwordText.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.positive.performClick();
                return true;
            }
            return false;
        });

        // 监听输入框内容变化，清除状态提示
        TextWatcher clearStatusWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearStatus();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
        binding.urlText.addTextChangedListener(clearStatusWatcher);
        binding.usernameText.addTextChangedListener(clearStatusWatcher);
        binding.passwordText.addTextChangedListener(clearStatusWatcher);
    }
    
    private void onSelectProvider() {
        AlertDialog providerDialog = new MaterialAlertDialogBuilder(activity)
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
            .create();
        // 设置对话框深色背景
        providerDialog.getWindow().setBackgroundDrawableResource(R.color.black_90);
        providerDialog.getWindow().setDimAmount(0);
        providerDialog.show();
        
        // 设置标题和按钮文字颜色为白色
        setDialogTextColor(providerDialog, R.color.white);
        
        // 设置列表项文字颜色为白色（使用 post 确保在列表渲染后设置）
        android.widget.ListView listView = providerDialog.getListView();
        if (listView != null) {
            listView.post(() -> {
                for (int i = 0; i < listView.getChildCount(); i++) {
                    View itemView = listView.getChildAt(i);
                    setTextViewColorRecursive(itemView, R.color.white);
                }
            });
            // 监听列表滚动，确保新显示的项目也是白色
            listView.setOnScrollListener(new android.widget.AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(android.widget.AbsListView view, int scrollState) {
                    if (scrollState == android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                        for (int i = 0; i < view.getChildCount(); i++) {
                            setTextViewColorRecursive(view.getChildAt(i), R.color.white);
                        }
                    }
                }
                @Override
                public void onScroll(android.widget.AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                }
            });
        }
    }

    private void updateSyncIntervalVisibility(boolean visible) {
        binding.syncIntervalContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
    
    /**
     * 递归设置 View 及其子 View 中所有 TextView 的文字颜色
     */
    private void setTextViewColorRecursive(View view, int colorResId) {
        if (view == null) return;
        
        if (view instanceof android.widget.TextView) {
            ((android.widget.TextView) view).setTextColor(activity.getResources().getColor(colorResId));
        } else if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                setTextViewColorRecursive(group.getChildAt(i), colorResId);
            }
        }
    }
    
    /**
     * 设置对话框中的标题和按钮文字颜色
     */
    private void setDialogTextColor(AlertDialog dialog, int colorResId) {
        if (dialog == null) return;
        
        int color = activity.getResources().getColor(colorResId);
        
        // 设置标题文字颜色
        int titleId = activity.getResources().getIdentifier("alertTitle", "id", "android");
        if (titleId != 0) {
            View titleView = dialog.findViewById(titleId);
            if (titleView instanceof android.widget.TextView) {
                ((android.widget.TextView) titleView).setTextColor(color);
            }
        }
        
        // 使用 post 延迟设置按钮文字颜色（按钮可能在显示后才创建）
        dialog.getWindow().getDecorView().post(() -> {
            android.widget.Button negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
            if (negativeButton != null) {
                negativeButton.setTextColor(color);
            }
        });
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
        syncManager.reloadConfig();

        showStatus("正在测试连接...", true);
        binding.testButton.setEnabled(false);
        App.execute(() -> {
            WebDAVConfig.TestResult result = syncManager.getConfig().testConnection();
            App.post(() -> {
                // 检查对话框是否还存在
                if (binding == null || dialog == null || !dialog.isShowing()) {
                    return;
                }
                binding.testButton.setEnabled(true);
                showStatus(result.message, result.success);
                if (!result.success) {
                    // 显示详细错误信息
                    Logger.e("WebDAV测试连接失败: " + result.message);
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

        AlertDialog intervalDialog = new MaterialAlertDialogBuilder(activity)
            .setTitle("选择同步间隔")
            .setSingleChoiceItems(intervals, selectedIndex, (dialog, which) -> {
                int interval = Integer.parseInt(intervals[which]);
                binding.syncIntervalText.setText(String.valueOf(interval));
                // 立即保存同步间隔
                Setting.putWebDAVSyncInterval(interval);
                dialog.dismiss();
            })
            .setNegativeButton("取消", null)
            .create();
        // 设置对话框深色背景
        intervalDialog.getWindow().setBackgroundDrawableResource(R.color.black_90);
        intervalDialog.getWindow().setDimAmount(0);
        intervalDialog.show();
        
        // 设置标题和按钮文字颜色为白色
        setDialogTextColor(intervalDialog, R.color.white);
        
        // 设置列表项文字颜色为白色（使用 post 确保在列表渲染后设置）
        android.widget.ListView listView = intervalDialog.getListView();
        if (listView != null) {
            listView.post(() -> {
                for (int i = 0; i < listView.getChildCount(); i++) {
                    View itemView = listView.getChildAt(i);
                    setTextViewColorRecursive(itemView, R.color.white);
                }
            });
            // 监听列表滚动，确保新显示的项目也是白色
            listView.setOnScrollListener(new android.widget.AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(android.widget.AbsListView view, int scrollState) {
                    if (scrollState == android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                        for (int i = 0; i < view.getChildCount(); i++) {
                            setTextViewColorRecursive(view.getChildAt(i), R.color.white);
                        }
                    }
                }
                @Override
                public void onScroll(android.widget.AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                }
            });
        }
    }

    private void showStatus(String message, boolean isSuccess) {
        // 检查对话框是否还存在
        if (binding == null || dialog == null || !dialog.isShowing()) {
            return;
        }
        // 取消之前的隐藏任务
        if (hideStatusRunnable != null) {
            statusHandler.removeCallbacks(hideStatusRunnable);
            hideStatusRunnable = null;
        }

        binding.statusText.setText(message);
        binding.statusText.setVisibility(TextUtils.isEmpty(message) ? View.GONE : View.VISIBLE);
        binding.statusText.setTextColor(isSuccess ?
            activity.getResources().getColor(R.color.white) :
            activity.getResources().getColor(android.R.color.holo_red_dark));

        // 3秒后自动隐藏状态消息
        if (!TextUtils.isEmpty(message)) {
            hideStatusRunnable = () -> clearStatus();
            statusHandler.postDelayed(hideStatusRunnable, 3000);
        }
    }

    /**
     * 清除状态提示
     */
    private void clearStatus() {
        // 检查对话框是否还存在
        if (binding == null || dialog == null || !dialog.isShowing()) {
            return;
        }
        if (hideStatusRunnable != null) {
            statusHandler.removeCallbacks(hideStatusRunnable);
            hideStatusRunnable = null;
        }
        binding.statusText.setText("");
        binding.statusText.setVisibility(View.GONE);
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

        clearStatus();
        if (this.dialog != null) {
            this.dialog.dismiss();
        }
        
        // 通知设置界面更新状态（通过RefreshEvent）
        // 使用App.post确保对话框关闭后再发送事件，让状态能及时更新
        App.post(() -> RefreshEvent.config());
    }

    private void onNegative(DialogInterface dialog, int which) {
        clearStatus();
        if (this.dialog != null) {
            this.dialog.dismiss();
        }
    }
}

