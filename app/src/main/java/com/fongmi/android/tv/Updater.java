package com.fongmi.android.tv;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.databinding.DialogUpdateBinding;
import com.fongmi.android.tv.utils.Download;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.UpdateInstaller;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Logger;
import com.github.catvod.utils.Path;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Locale;

public class Updater implements Download.Callback {

    private DialogUpdateBinding binding;
    private Download download;
    private AlertDialog dialog;
    private boolean dev;
    private String latestVersion;
    private String releaseApkUrl;
    private String fallbackApkUrl;

    private File getFile() {
        return Path.cache("XMBOX-update.apk");
    }

    private String getApk() {
        if (releaseApkUrl != null && !releaseApkUrl.isEmpty()) {
            return releaseApkUrl;
        }
        return "";
    }

    public static Updater create() {
        return new Updater();
    }

    public Updater() {
    }

    public Updater force() {
        Notify.show(R.string.update_check);
        Setting.putUpdate(true);
        return this;
    }

    public Updater release() {
        this.dev = false;
        return this;
    }

    public Updater dev() {
        this.dev = true;
        return this;
    }

    public Updater auto() {
        return this;
    }

    private Updater check() {
        dismiss();
        return this;
    }

    public void start(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        // 使用统一的线程池执行异步检查
        App.execute(() -> checkUpdate(activity));
    }

    private void checkUpdate(Activity activity) {
        try {
            String releasesUrl = "https://api.github.com/repos/Tosencen/XMBOX/releases/latest";

            // 后台线程执行网络请求
            String response = OkHttp.string(releasesUrl);

            if (response == null || response.isEmpty()) {
                App.post(() -> Notify.show("检查更新失败：网络连接异常"));
                return;
            }

            JSONObject release = new JSONObject(response);
            String tagName = release.optString("tag_name");

            // 非Release响应（404/限流等）：解析message，避免用子串匹配误伤更新日志内容
            if (tagName.isEmpty()) {
                String message = release.optString("message");
                if (message.contains("rate limit")) {
                    App.post(() -> Notify.show("检查更新失败：API请求次数已达上限"));
                } else {
                    App.post(() -> Notify.show("检查更新失败：未找到发布版本"));
                }
                return;
            }

            String body = release.optString("body");
            String version = tagName.startsWith("v") ? tagName.substring(1) : tagName;

            // 检查是否需要更新
            if (!needUpdate(version)) {
                // 已是最新版本
                App.post(() -> showVersionInfo(activity));
                return;
            }

            // 查找APK
            JSONArray assets = release.optJSONArray("assets");
            if (assets == null) {
                App.post(() -> Notify.show("检查更新失败：未找到APK文件"));
                return;
            }

            String mode = BuildConfig.FLAVOR_mode;
            String abi = BuildConfig.FLAVOR_abi;

            // 兼容多种ABI命名：arm64_v8a/arm64/arm64-v8a、armeabi_v7a/armv7/armeabi-v7a
            String abiShort = abi.replace("arm64_v8a", "arm64").replace("armeabi_v7a", "armv7");
            String abiDash = abi.replace("_", "-");

            boolean found = false;
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                String assetName = asset.optString("name");
                if (!assetName.endsWith(".apk") || !assetName.toLowerCase().contains(mode)) continue;
                if (!assetName.contains(abi) && !assetName.contains(abiShort) && !assetName.contains(abiDash)) continue;
                String url = asset.optString("browser_download_url");
                if (url.isEmpty()) continue;
                releaseApkUrl = url;
                fallbackApkUrl = url;
                found = true;
                break;
            }

            if (!found) {
                App.post(() -> Notify.show("检查更新失败：未找到匹配的APK"));
                return;
            }

            // 显示更新对话框
            this.latestVersion = version;
            App.post(() -> show(activity, version, body));

        } catch (Exception e) {
            Logger.e("Updater", e);
            App.post(() -> Notify.show("检查更新失败：" + e.toString()));
        }
    }

    private boolean needUpdate(String remoteVersion) {
        if (!Setting.getUpdate()) return false;
        
        try {
            String[] remoteParts = remoteVersion.split("\\.");
            String[] localParts = BuildConfig.VERSION_NAME.split("\\.");
            
            int maxLength = Math.max(remoteParts.length, localParts.length);
            
            for (int i = 0; i < maxLength; i++) {
                int remotePart = i < remoteParts.length ? Integer.parseInt(remoteParts[i]) : 0;
                int localPart = i < localParts.length ? Integer.parseInt(localParts[i]) : 0;
                
                if (remotePart > localPart) {
                    return true;
                } else if (remotePart < localPart) {
                    return false;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void show(Activity activity, String version, String desc) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        binding = DialogUpdateBinding.inflate(LayoutInflater.from(activity));
        check().create(activity, ResUtil.getString(R.string.update_version, version)).show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this::confirm);
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(this::cancel);
        binding.desc.setText(desc);
        int maxHeight = (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.5f);
        binding.getRoot().post(() -> {
            if (binding.getRoot().getHeight() > maxHeight) {
                ViewGroup.LayoutParams params = binding.getRoot().getLayoutParams();
                params.height = maxHeight;
                binding.getRoot().setLayoutParams(params);
            }
        });
    }

    private void showVersionInfo(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        binding = DialogUpdateBinding.inflate(LayoutInflater.from(activity));
        binding.desc.setText(BuildConfig.VERSION_NAME);
        check().create(activity, "已是最新版本").show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(View.GONE);
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setText("确定");
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(v -> {
            if (dialog != null) dialog.dismiss();
        });
    }

    private AlertDialog create(Activity activity, String title) {
        return dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.update_confirm, null)
                .setNegativeButton(R.string.dialog_negative, null)
                .setCancelable(false)
                .create();
    }

    private void cancel(View view) {
        if (download != null) {
            download.cancel();
            download = null;
        }
        dismiss();
    }

    private void confirm(View view) {
        String downloadUrl = getApk();
        if (downloadUrl == null || downloadUrl.isEmpty()) {
            Notify.show("无法获取下载链接");
            return;
        }
        
        // 使用应用内下载
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
        binding.desc.setText(R.string.update_downloading);
        
        download = Download.create(downloadUrl, getFile(), fallbackApkUrl, this);
        download.start();
    }

    private void dismiss() {
        try {
            if (dialog != null) dialog.dismiss();
        } catch (Exception ignored) {
        } finally {
            dialog = null;
        }
    }

    @Override
    public void progress(int progress) {
        App.post(() -> {
            if (binding == null) return;
            if (progress < 0) {
                binding.desc.setText(R.string.update_downloading);
            } else {
                binding.desc.setText(String.format(Locale.getDefault(), "%d%%", progress));
            }
        });
    }

    @Override
    public void success(File file) {
        App.post(() -> {
            // 用户已取消（弹窗已关）时不再拉起安装
            if (dialog == null || !dialog.isShowing()) return;
            dismiss();
            // checkPermission=true：无安装权限时先保存待安装文件，授权返回后由 App 自动续装
            UpdateInstaller installer = UpdateInstaller.get();
            if (installer.install(file, true)) return;
            if (!installer.hasInstallPermission()) {
                installer.requestInstallPermission();
                Notify.show("请允许安装未知应用，授权返回后将自动安装");
            } else {
                Notify.show("安装失败：更新文件不可用");
            }
        });
    }

    @Override
    public void error(String msg) {
        App.post(() -> {
            // 已取消的下载不再提示
            if (dialog == null || !dialog.isShowing()) return;
            Notify.show("下载失败: " + msg);
            dismiss();
        });
    }
}
