package com.fongmi.android.tv;

import android.app.Activity;
import android.content.DialogInterface;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.databinding.DialogUpdateBinding;
import com.fongmi.android.tv.service.UpdateService;
import com.fongmi.android.tv.utils.Github;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.PermissionUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.UpdateInstaller;
import com.github.catvod.utils.Logger;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * 应用内更新（对齐 FongMi/TV 上游架构 + 国内镜像链）。
 *
 * - 全局单例 + 状态机（checking/downloading/paused/ready），杜绝叠窗与重复检查
 * - 检查走 Github 双通道（官方 API → jsDelivr 降级），10s 短超时快速失败
 * - 下载/校验在前台服务 UpdateService 中进行，切页面、锁屏不中断
 * - 下载完成后经 digest + 签名校验再拉起安装
 */
public class Updater {

    private WeakReference<Activity> activity = new WeakReference<>(null);
    private DialogUpdateBinding binding;
    private AlertDialog dialog;
    private UpdatePackage update;
    private File ready;
    private boolean forced;
    private volatile boolean checking;
    private volatile boolean downloading;
    private volatile boolean paused;

    private static class Loader {
        static final Updater INSTANCE = new Updater();
    }

    private Updater() {
    }

    public static Updater get() {
        return Loader.INSTANCE;
    }

    /**
     * @param force  true=用户手动检查（设置页）；false=自动检查
     */
    public void start(Activity activity, boolean force) {
        Logger.d("Updater: start force=" + force + " activity=" + activity);
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        if (force) {
            Notify.show(R.string.update_check);
            Setting.putUpdate(true);
        }
        if (!Setting.getUpdate()) return;
        this.forced = force;
        this.activity = new WeakReference<>(activity);
        if (downloading || paused || ready != null) {
            // 已有进行中的更新/待安装包，直接回到弹窗，不再重复下载
            if (force) App.post(this::show);
            return;
        }
        if (checking) return;
        checking = true;
        App.execute(this::check);
    }

    private void check() {
        try {
            UpdatePackage result = Github.check();
            App.post(() -> onChecked(result, null));
        } catch (Exception e) {
            Logger.e("Updater", e);
            String msg = e.getMessage();
            App.post(() -> onChecked(null, msg));
        }
    }

    private void onChecked(UpdatePackage result, String error) {
        Logger.d("Updater: checked result=" + (result != null ? result.version() : "null") + " error=" + error);
        checking = false;
        if (error != null) {
            if (forced) Notify.show(ResUtil.getString(R.string.update_check_failed, error));
            return;
        }
        if (result == null) {
            if (forced) showLatest();
            return;
        }
        this.update = result;
        show();
    }

    // ==================== 弹窗 ====================

    private void show() {
        Activity a = activity.get();
        if (a == null || a.isFinishing() || a.isDestroyed() || update == null) return;
        Logger.d("Updater: show dialog " + update.version());
        if (dialog != null && dialog.isShowing()) {
            updateDialogState();
            return;
        }
        binding = DialogUpdateBinding.inflate(LayoutInflater.from(a));
        binding.desc.setText(update.notes() != null ? update.notes() : ResUtil.getString(R.string.update_no_notes));
        dialog = new MaterialAlertDialogBuilder(a)
                .setTitle(ResUtil.getString(R.string.update_version, update.version()))
                .setView(binding.getRoot())
                .setPositiveButton(R.string.update_confirm, null)
                .setNegativeButton(R.string.dialog_negative, null)
                .setCancelable(false)
                .create();
        dialog.setOnDismissListener(d -> {
            if (dialog == d) {
                dialog = null;
                binding = null;
            }
        });
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this::confirm);
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(this::cancel);
        int maxHeight = (int) (a.getResources().getDisplayMetrics().heightPixels * 0.5f);
        binding.getRoot().post(() -> {
            if (binding == null || binding.getRoot().getHeight() <= maxHeight) return;
            ViewGroup.LayoutParams params = binding.getRoot().getLayoutParams();
            params.height = maxHeight;
            binding.getRoot().setLayoutParams(params);
        });
        updateDialogState();
    }

    private void showLatest() {
        Activity a = activity.get();
        if (a == null || a.isFinishing() || a.isDestroyed()) return;
        if (dialog != null && dialog.isShowing()) return;
        binding = DialogUpdateBinding.inflate(LayoutInflater.from(a));
        binding.desc.setText(BuildConfig.VERSION_NAME);
        dialog = new MaterialAlertDialogBuilder(a)
                .setTitle(ResUtil.getString(R.string.update_latest))
                .setView(binding.getRoot())
                .setPositiveButton(R.string.update_confirm, null)
                .setNegativeButton(R.string.dialog_negative, null)
                .create();
        dialog.setOnDismissListener(d -> {
            if (dialog == d) {
                dialog = null;
                binding = null;
            }
        });
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(View.GONE);
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setText(R.string.update_confirm);
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(v -> dismiss());
    }

    /** 下载中隐藏"更新"按钮、显示进度；暂停时显示"继续"；取消按钮始终可用 */
    private void updateDialogState() {
        if (dialog == null || !dialog.isShowing()) return;
        boolean busy = downloading || paused;
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setText(paused ? R.string.update_resume : R.string.update_confirm);
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(busy && !paused ? View.GONE : View.VISIBLE);
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(true);
        if (busy && binding != null) {
            binding.desc.setText(paused ? R.string.update_paused : R.string.update_downloading);
            // 进入下载态后内容变短，解除之前为更新日志锁定的高度，避免弹窗中间大片空白
            ViewGroup.LayoutParams params = binding.getRoot().getLayoutParams();
            if (params != null && params.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                binding.getRoot().setLayoutParams(params);
            }
        }
    }

    private void confirm(View view) {
        Logger.d("Updater: confirm clicked paused=" + paused + " downloading=" + downloading);
        if (update == null) return;
        if (paused) {
            // 暂停态下"更新"按钮变为"继续"
            UpdateService.resume();
            return;
        }
        if (downloading) return;
        downloading = true;
        paused = false;
        updateDialogState();
        Activity a = activity.get();
        if (a instanceof androidx.fragment.app.FragmentActivity && !PermissionUtil.hasNotificationPermission()) {
            // Android 13+ 通知权限：未授权先请求，授权与否都继续下载（进度在弹窗内也可见）
            PermissionUtil.requestNotification((androidx.fragment.app.FragmentActivity) a, granted -> beginDownload());
        } else {
            beginDownload();
        }
    }

    private void beginDownload() {
        if (update == null || !downloading) return;
        UpdateService.start(update);
    }

    private void cancel(View view) {
        Logger.d("Updater: cancel clicked downloading=" + downloading);
        if (downloading || paused) {
            UpdateService.cancel();
            downloading = false;
            paused = false;
        } else {
            // 未开始下载就取消：关闭自动检查更新（对齐上游语义）
            Setting.putUpdate(false);
        }
        dismiss();
    }

    private void dismiss() {
        try {
            if (dialog != null) dialog.dismiss();
        } catch (Exception ignored) {
        }
    }

    /** Activity 销毁时收起弹窗，下载由服务继续并经通知栏呈现 */
    public void onActivityDestroyed(Activity activity) {
        if (this.activity.get() != activity) return;
        App.post(() -> {
            try {
                if (dialog != null) dialog.dismiss();
            } catch (Exception ignored) {
            }
        });
    }

    // ==================== 服务回调（下载线程 → 主线程） ====================

    public void onProgress(int percent, long speed, long eta) {
        App.post(() -> {
            if (paused) {
                // 从通知栏"继续"后收到首个进度，恢复下载态按钮
                paused = false;
                downloading = true;
                updateDialogState();
            }
            if (binding == null) return;
            if (percent < 0) {
                binding.desc.setText(R.string.update_downloading);
            } else {
                StringBuilder sb = new StringBuilder().append(percent).append("%");
                if (speed > 0) sb.append(" · ").append(Formatter.formatFileSize(App.get(), speed)).append("/s");
                if (eta > 0) sb.append(" · ").append(ResUtil.getString(R.string.update_remaining, DateUtils.formatElapsedTime(eta)));
                binding.desc.setText(sb.toString());
            }
        });
    }

    public void onPaused() {
        App.post(() -> {
            downloading = false;
            paused = true;
            updateDialogState();
        });
    }

    public void onSuccess(File file) {
        Logger.d("Updater: onSuccess " + file);
        App.post(() -> {
            downloading = false;
            paused = false;
            ready = file;
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

    public void onError(String msg) {
        Logger.d("Updater: onError " + msg);
        App.post(() -> {
            downloading = false;
            paused = false;
            if (dialog != null && dialog.isShowing()) {
                Notify.show(ResUtil.getString(R.string.update_download_failed, msg));
                dismiss();
            }
        });
    }

    public void onCancelled() {
        Logger.d("Updater: onCancelled");
        App.post(() -> {
            downloading = false;
            paused = false;
            dismiss();
        });
    }
}
