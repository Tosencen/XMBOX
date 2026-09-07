package com.fongmi.android.tv.service;
import com.github.catvod.utils.Logger;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
import androidx.media.session.MediaButtonReceiver;

import com.bumptech.glide.Glide;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.event.ActionEvent;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.receiver.ActionReceiver;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.PermissionUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class PlaybackService extends Service {

    private Map<String, Bitmap> cache = new ConcurrentHashMap<>();
    private static Players player;

    public static void start(Players player) {
        PlaybackService.player = player;
        // 进入播放时统一申请一次通知权限，避免在播放过程中（高频通知更新路径上）反复弹权限请求
        if (!PermissionUtil.hasNotificationPermission()) {
            var activity = App.activity();
            if (activity instanceof androidx.fragment.app.FragmentActivity && !activity.isFinishing()) {
                PermissionUtil.requestNotification((androidx.fragment.app.FragmentActivity) activity, granted -> { });
            }
        }
        ContextCompat.startForegroundService(App.get(), new Intent(App.get(), PlaybackService.class));
    }

    public static void stop() {
        player = null;
        App.get().stopService(new Intent(App.get(), PlaybackService.class));
    }

    private boolean isNull() {
        return Objects.isNull(player) || Objects.isNull(player.getSession());
    }

    private boolean nonNull() {
        return Objects.nonNull(player) && Objects.nonNull(player.getSession());
    }

    private NotificationManagerCompat getManager() {
        return NotificationManagerCompat.from(this);
    }

    private NotificationCompat.Action buildNotificationAction(@DrawableRes int icon, @StringRes int title, String action) {
        return new NotificationCompat.Action(icon, getString(title), ActionReceiver.getPendingIntent(this, action));
    }

    private NotificationCompat.Action getPlayPauseAction() {
        if (nonNull() && player.isPlaying()) return buildNotificationAction(R.drawable.ic_notify_pause, androidx.media3.ui.R.string.exo_controls_pause_description, ActionEvent.PAUSE);
        return buildNotificationAction(R.drawable.ic_notify_play, androidx.media3.ui.R.string.exo_controls_play_description, ActionEvent.PLAY);
    }

    private MediaMetadataCompat getMetadata() {
        return isNull() ? null : player.getSession().getController().getMetadata();
    }

    private String getTitle() {
        return getMetadata() == null || getMetadata().getString(MediaMetadataCompat.METADATA_KEY_TITLE).isEmpty() ? null : getMetadata().getString(MediaMetadataCompat.METADATA_KEY_TITLE);
    }

    private String getArtist() {
        return getMetadata() == null || getMetadata().getString(MediaMetadataCompat.METADATA_KEY_ARTIST).isEmpty() ? null : getMetadata().getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
    }

    private String getArtUri() {
        return getMetadata() == null ? "" : getMetadata().getString(MediaMetadataCompat.METADATA_KEY_ART_URI);
    }

    private void addAction(NotificationCompat.Builder builder) {
        builder.addAction(buildNotificationAction(R.drawable.ic_notify_prev, androidx.media3.ui.R.string.exo_controls_previous_description, ActionEvent.PREV));
        builder.addAction(getPlayPauseAction());
        builder.addAction(buildNotificationAction(R.drawable.ic_notify_next, androidx.media3.ui.R.string.exo_controls_next_description, ActionEvent.NEXT));
    }

    private Notification buildNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Notify.DEFAULT);
        builder.setOngoing(true);
        builder.setOnlyAlertOnce(true);
        builder.setContentText(getArtist());
        builder.setContentTitle(getTitle());
        builder.setSmallIcon(R.drawable.ic_logo);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        builder.setDeleteIntent(ActionReceiver.getPendingIntent(this, ActionEvent.STOP));
        if (nonNull()) builder.setContentIntent(player.getSession().getController().getSessionActivity());
        if (nonNull()) builder.setStyle(new MediaStyle().setMediaSession(player.getSession().getSessionToken()));
        addAction(builder);
        setArtwork(builder);
        return builder.build();
    }

    private void setArtwork(NotificationCompat.Builder builder) {
        if (cache.containsKey(getArtUri())) {
            builder.setLargeIcon(cache.get(getArtUri()));
        } else if (!getArtUri().isEmpty()) {
            App.execute(() -> glide(builder));
        }
    }

    private void glide(NotificationCompat.Builder builder) {
        try {
            cache.put(getArtUri(), Glide.with(this).asBitmap().skipMemoryCache(true).dontAnimate().load(ImgUtil.getUrl(getArtUri())).submit().get());
            builder.setLargeIcon(cache.get(getArtUri()));
            Notify.show(builder.build());
        } catch (Exception e) {
            Logger.e("Error", e);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onActionEvent(ActionEvent event) {
        if (event.isUpdate()) Notify.show(buildNotification());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (nonNull()) MediaButtonReceiver.handleIntent(player.getSession(), intent);
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK : 0;
        ServiceCompat.startForeground(this, Notify.ID, buildNotification(), type);
        return START_NOT_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopSelf();
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        // 释放封面 Bitmap 缓存，避免内存泄漏
        for (Bitmap bitmap : cache.values()) {
            if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
        }
        cache.clear();
        getManager().cancel(Notify.ID);
        stopForeground(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
