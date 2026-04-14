package com.fongmi.android.tv;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.databinding.DialogUpdateBinding;
import com.fongmi.android.tv.utils.Download;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.UpdateInstaller;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Github;
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
        // 异步执行检查
        new Thread(() -> checkUpdate(activity)).start();
    }

    private boolean need(int code, String name) {
        return Setting.getUpdate() && (dev ? !name.equals(BuildConfig.VERSION_NAME) && code >= BuildConfig.VERSION_CODE : code > BuildConfig.VERSION_CODE);
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

            // 检查是否是错误响应
            if (response.contains("Not Found") || response.contains("404")) {
                App.post(() -> Notify.show("检查更新失败：未找到发布版本"));
                return;
            }

            if (response.contains("rate limit") || response.contains("API rate limit")) {
                App.post(() -> Notify.show("检查更新失败：API请求次数已达上限"));
                return;
            }

            JSONObject release = new JSONObject(response);
            String tagName = release.optString("tag_name");
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
            
            // 兼容多种ABI格式：arm64_v8a -> arm64, armeabi_v7a -> armv7
            String abiShort = abi.replace("arm64_v8a", "arm64").replace("armeabi_v7a", "armv7");

            boolean found = false;
            // 尝试多种匹配方式
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                String assetName = asset.optString("name");
                
                // 匹配逻辑：支持 arm64、arm64-v8a、armv7、armeabi-v7a 等格式
                if (assetName.endsWith(".apk") && 
                    assetName.toLowerCase().contains(mode.toLowerCase()) && 
                    (assetName.contains(abiShort) || assetName.contains(abi.replace("_", "-")))) {
                    releaseApkUrl = asset.optString("browser_download_url");
                    fallbackApkUrl = releaseApkUrl;
                    found = true;
                    break;
                }
            }

            if (!found) {
                App.post(() -> Notify.show("检查更新失败：未找到匹配的APK"));
                return;
            }

            // 显示更新对话框
            this.latestVersion = version;
            App.post(() -> show(activity, version, body));

        } catch (Exception e) {
            Logger.e("Updater: " + e.getMessage());
            App.post(() -> Notify.show("检查更新失败：" + e.getMessage()));
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
        Setting.putUpdate(false);
        if (download != null) {
            download.cancel();
        }
        dialog.dismiss();
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
        }
    }

    @Override
    public void progress(int progress) {
        App.post(() -> {
            if (binding != null) {
                binding.desc.setText(String.format(Locale.getDefault(), "%d%%", progress));
            }
        });
    }

    @Override
    public void success(File file) {
        App.post(() -> {
            if (dialog != null) dialog.dismiss();
            UpdateInstaller.get().install(file);
        });
    }

    @Override
    public void error(String msg) {
        App.post(() -> {
            Notify.show("下载失败: " + msg);
            dismiss();
        });
    }
}
