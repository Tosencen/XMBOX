package com.fongmi.android.tv.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.StatFs;
import android.os.SystemClock;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.UpdatePackage;
import com.fongmi.android.tv.Updater;
import com.fongmi.android.tv.utils.Download;
import com.github.catvod.utils.Logger;
import com.github.catvod.utils.Path;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 更新下载前台服务（对齐 FongMi/TV 上游设计）。
 *
 * - 前台通知显示进度/速度/剩余时间，支持暂停/继续/取消
 * - 切换页面、锁屏不中断下载
 * - 下载完成后先做 digest 与签名校验，防止镜像链路篡改
 * - 同版本 APK 已缓存且校验通过时直接复用，不重复下载
 */
public class UpdateService extends Service implements Download.Callback {

    private static final String ACTION_PAUSE = "pause";
    private static final String ACTION_RESUME = "resume";
    private static final String ACTION_CANCEL = "cancel";
    private static final String CHANNEL_ID = "update_channel";
    private static final int ID = 9529;
    private static final long MIN_EXTRA_SPACE = 50L * 1024 * 1024;

    private ExecutorService executor;
    private NotificationManager manager;
    private UpdatePackage update;
    private Download download;
    private File file;
    private volatile boolean finished;
    private volatile boolean paused;
    private long lastNotify;

    public static void start(UpdatePackage update) {
        Intent intent = new Intent(App.get(), UpdateService.class);
        intent.putExtra("version", update.version())
                .putExtra("urls", update.urls().toArray(new String[0]))
                .putExtra("size", update.size())
                .putExtra("digest", update.digest())
                .putExtra("notes", update.notes());
        ContextCompat.startForegroundService(App.get(), intent);
    }

    public static void pause() {
        App.get().startService(new Intent(App.get(), UpdateService.class).setAction(ACTION_PAUSE));
    }

    public static void resume() {
        App.get().startService(new Intent(App.get(), UpdateService.class).setAction(ACTION_RESUME));
    }

    public static void cancel() {
        App.get().startService(new Intent(App.get(), UpdateService.class).setAction(ACTION_CANCEL));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newSingleThreadExecutor();
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createChannel();
        startForeground(ID, notification(getString(R.string.update_downloading), 0, true, true));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            fail(getString(R.string.update_interrupted));
            return START_NOT_STICKY;
        }
        String action = intent.getAction();
        Logger.d("UpdateService: onStartCommand action=" + action + " download=" + (download != null) + " finished=" + finished);
        if (ACTION_PAUSE.equals(action)) {
            pauseDownload();
            return START_NOT_STICKY;
        }
        if (ACTION_RESUME.equals(action)) {
            resumeDownload();
            return START_NOT_STICKY;
        }
        if (ACTION_CANCEL.equals(action)) {
            cancelDownload();
            return START_NOT_STICKY;
        }
        if (download != null || finished) return START_NOT_STICKY;
        try {
            prepare(intent);
        } catch (Exception e) {
            Logger.e("UpdateService", e);
            fail(e.getMessage());
        }
        return START_NOT_STICKY;
    }

    private void prepare(Intent intent) {
        String[] urls = intent.getStringArrayExtra("urls");
        Logger.d("UpdateService: prepare version=" + intent.getStringExtra("version") + " urls=" + (urls == null ? "null" : urls.length));
        update = new UpdatePackage(intent.getStringExtra("version"), urls == null ? null : Arrays.asList(urls), intent.getLongExtra("size", -1), intent.getStringExtra("digest"));
        update.setNotes(intent.getStringExtra("notes"));
        file = update.file();
        // 缓存复用：同版本文件存在且大小吻合时直接进入校验安装；
        // 大小不符（上次下载中断）保留断点，由 Download 走 Range 续传（不支持时自动整包重下）
        if (file.isFile() && file.length() > 0 && (update.size() <= 0 || file.length() == update.size())) {
            verify(file);
            return;
        }
        if (!hasSpace()) {
            fail(getString(R.string.update_no_space));
            return;
        }
        download = Download.create(update.urls(), file, this);
        Logger.d("UpdateService: start download urls=" + update.urls());
        executor.execute(download::run);
    }

    private boolean hasSpace() {
        try {
            if (update.size() <= 0) return true;
            StatFs stat = new StatFs(Path.cache().getAbsolutePath());
            return stat.getAvailableBytes() > update.size() + MIN_EXTRA_SPACE;
        } catch (Exception e) {
            return true;
        }
    }

    private void pauseDownload() {
        if (download == null || finished || paused) return;
        paused = true;
        download.pause();
        // pause() 会 cancel 掉进行中的 call，下载线程在异常分支静默返回不会回调 paused()，
        // 这里直接刷新通知与弹窗，保证 UI 立即进入暂停态
        updateNotification(getString(R.string.update_paused), -1, true, true);
        Updater.get().onPaused();
    }

    private void resumeDownload() {
        if (download == null || finished || !paused) return;
        paused = false;
        updateNotification(getString(R.string.update_downloading), 0, true, true);
        executor.execute(download::run);
    }

    private void cancelDownload() {
        if (finished) {
            stopSelf();
            return;
        }
        finished = true;
        if (download != null) download.cancel();
        Updater.get().onCancelled();
        stopSelf();
    }

    // ==================== Download.Callback（下载线程回调） ====================

    @Override
    public void progress(int progress) {
        progress(progress, 0, -1);
    }

    @Override
    public void progress(int progress, long speed, long eta) {
        if (finished) return;
        long now = SystemClock.elapsedRealtime();
        if (progress >= 0 && progress < 100 && now - lastNotify < 1000) {
            Updater.get().onProgress(progress, speed, eta);
            return;
        }
        // 不要在这里更新 lastNotify：updateNotification 内部用同一字段节流，提前写入会导致通知永不刷新
        String text = text(progress, speed, eta);
        updateNotification(text, progress, true, true);
        Updater.get().onProgress(progress, speed, eta);
    }

    @Override
    public void paused() {
        if (finished) return;
        paused = true;
        updateNotification(getString(R.string.update_paused), -1, true, true);
        Updater.get().onPaused();
    }

    @Override
    public void error(String msg) {
        fail(msg);
    }

    @Override
    public void success(File f) {
        verify(f);
    }

    // ==================== 校验与收尾 ====================

    private void verify(File f) {
        if (finished) return;
        if (!verifyDigest(f, update.digest()) || !update.verifyApk()) {
            Logger.e("UpdateService: 更新包校验失败，删除 " + f.getName());
            Path.clear(f);
            fail(getString(R.string.update_verify_failed));
            return;
        }
        finished = true;
        Logger.d("UpdateService: verify passed " + f.getName());
        updateNotification(getString(R.string.update_confirm) + " " + update.version(), -1, false, false);
        Updater.get().onSuccess(f);
        stopSelf();
    }

    private static boolean verifyDigest(File file, String digest) {
        if (digest == null || digest.isEmpty()) return true;
        try {
            String expected = digest.contains(":") ? digest.substring(digest.indexOf(':') + 1) : digest;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) md.update(buffer, 0, read);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) sb.append(String.format(java.util.Locale.ROOT, "%02x", b));
            return sb.toString().equalsIgnoreCase(expected.trim());
        } catch (Exception e) {
            // 摘要通道失败不阻断（签名校验兜底）
            return true;
        }
    }

    private void fail(String msg) {
        Logger.e("UpdateService: fail " + msg);
        if (finished) return;
        finished = true;
        String text = msg == null || msg.isEmpty() ? getString(R.string.update_interrupted) : msg;
        Updater.get().onError(text);
        stopSelf();
    }

    // ==================== 通知 ====================

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null) executor.shutdown();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW);
        channel.setShowBadge(false);
        manager.createNotificationChannel(channel);
    }

    private String text(int progress, long speed, long eta) {
        if (progress < 0) return getString(R.string.update_downloading);
        StringBuilder sb = new StringBuilder().append(progress).append("%");
        if (speed > 0) sb.append(" · ").append(android.text.format.Formatter.formatFileSize(this, speed)).append("/s");
        if (eta > 0) sb.append(" · ").append(getString(R.string.update_remaining, android.text.format.DateUtils.formatElapsedTime(eta)));
        return sb.toString();
    }

    private void updateNotification(String text, int progress, boolean showPause, boolean showCancel) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastNotify < 500 && progress >= 0) return;
        lastNotify = now;
        notify(notification(text, progress >= 0 && progress < 100 ? progress : 0, showPause, showCancel));
    }

    private Notification notification(String text, int progress, boolean showPause, boolean showCancel) {
        boolean pausedNow = paused && !finished;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_action_download)
                .setContentTitle(getString(R.string.app_name) + " · " + getString(R.string.update_version_short, update != null ? update.version() : ""))
                .setContentText(text)
                .setContentIntent(launchIntent())
                .setOnlyAlertOnce(true)
                .setOngoing(!finished)
                .setAutoCancel(finished)
                .setProgress(100, progress, finished ? false : progress <= 0);
        if (showPause) {
            String label = getString(pausedNow ? R.string.update_resume : R.string.update_pause);
            String action = pausedNow ? ACTION_RESUME : ACTION_PAUSE;
            builder.addAction(0, label, PendingIntent.getService(this, ID, new Intent(this, UpdateService.class).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        }
        if (showCancel) {
            builder.addAction(0, getString(R.string.dialog_negative), PendingIntent.getService(this, ID, new Intent(this, UpdateService.class).setAction(ACTION_CANCEL), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        }
        return builder.build();
    }

    private PendingIntent launchIntent() {
        Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
        return PendingIntent.getActivity(this, ID, launch != null ? launch : new Intent(), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void notify(Notification notification) {
        if (manager != null) manager.notify(ID, notification);
    }
}
