package com.fongmi.android.tv.player;
import com.github.catvod.utils.Logger;

import static androidx.media3.common.Player.COMMAND_SET_SPEED_AND_PITCH;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.media3.common.C;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.drm.FrameworkMediaDrm;
import androidx.media3.ui.PlayerView;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Danmaku;
import com.fongmi.android.tv.bean.Drm;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.bean.Track;
import com.fongmi.android.tv.event.ActionEvent;
import com.fongmi.android.tv.event.ErrorEvent;
import com.fongmi.android.tv.event.PlayerEvent;
import com.fongmi.android.tv.impl.ParseCallback;
import com.fongmi.android.tv.impl.SessionCallback;
import com.fongmi.android.tv.player.danmaku.DanPlayer;
import com.fongmi.android.tv.player.engine.ExoPlayerEngine;
import com.fongmi.android.tv.player.engine.MpvPlayerEngine;
import com.fongmi.android.tv.player.engine.PlayerEngine;
import com.fongmi.android.tv.player.exo.ExoUtil;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.UrlUtil;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.utils.Path;
import com.google.common.net.HttpHeaders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import master.flame.danmaku.ui.widget.DanmakuView;

public class Players implements ParseCallback {

    private static final String TAG = Players.class.getSimpleName();

    public static final int SOFT = 0;
    public static final int HARD = 1;
    public static final int AUTO = 2;
    public static final int MPV = 3;

    // 播放器引擎常量
    public static final int ENGINE_EXO = 0;
    public static final int ENGINE_MPV = 1;

    private final StringBuilder builder;
    private final Formatter formatter;
    private final Runnable runnable;

    private PlayerEngine engine;
    private Map<String, String> headers;
    private MediaSessionCompat session;
    private List<Danmaku> danmakus;
    private DanPlayer danPlayer;
    private ParseJob parseJob;
    private PlayerView view;
    private List<Sub> subs;
    private String format;
    private String tag;
    private String key;
    private String url;
    private Drm drm;
    private Sub sub;

    private int decode;
    private int playerEngine;
    private int retry;

    public static Players create(Activity activity) {
        Players player = new Players(activity);
        Server.get().setPlayer(player);
        return player;
    }

    private Players(Activity activity) {
        decode = Setting.getDecode();
        playerEngine = Setting.getPlayerEngine();
        builder = new StringBuilder();
        runnable = () -> ErrorEvent.timeout(tag);
        formatter = new Formatter(builder, Locale.getDefault());
        createSession(activity);
    }

    private void createSession(Activity activity) {
        session = new MediaSessionCompat(activity, "TV");
        session.setCallback(SessionCallback.create(this));
        session.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        session.setSessionActivity(PendingIntent.getActivity(App.get(), 0, new Intent(App.get(), activity.getClass()), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        MediaControllerCompat.setMediaController(activity, session.getController());
    }

    public void init(PlayerView view) {
        releasePlayer();
        this.view = view;
        playerEngine = Setting.getPlayerEngine();
        createEngine(view);
    }

    private void createEngine(PlayerView view) {
        if (playerEngine == ENGINE_MPV) {
            initMpvEngine(view);
        } else {
            initExoEngine(view);
        }
    }

    private void initExoEngine(PlayerView view) {
        ExoPlayerEngine exoEngine = new ExoPlayerEngine();
        exoEngine.setListener(createEngineListener());
        exoEngine.init(view);
        engine = exoEngine;
    }

    private void initMpvEngine(PlayerView view) {
        if (!MpvPlayerEngine.isLibraryAvailable()) {
            Logger.e("Players: mpv-android library not available, falling back to ExoPlayer");
            Notify.show("MPV 库未加载，自动切换到 ExoPlayer");
            Setting.putPlayerEngine(ENGINE_EXO);
            playerEngine = ENGINE_EXO;
            initExoEngine(view);
            return;
        }

        // PlayerView 内部需要 SurfaceView 用于 MPV 渲染
        android.view.View surfaceView = view.getVideoSurfaceView();
        if (surfaceView instanceof android.view.SurfaceView) {
            MpvPlayerEngine mpvEngine = new MpvPlayerEngine();
            mpvEngine.setListener(createEngineListener());
            mpvEngine.init((android.view.SurfaceView) surfaceView);
            engine = mpvEngine;
        } else {
            Logger.e("Players: no SurfaceView in PlayerView, falling back to ExoPlayer");
            Setting.putPlayerEngine(ENGINE_EXO);
            playerEngine = ENGINE_EXO;
            initExoEngine(view);
        }
    }

    private PlayerEngine.Listener createEngineListener() {
        return new PlayerEngine.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (danPlayer != null) danPlayer.check(state);
                PlayerEvent.state(tag, state);
            }

            @Override
            public void onPlayerError(int errorCode, String message) {
                Logger.e(errorCode + "," + url);
                String friendlyMsg = message != null ? message : "播放错误";
                Logger.e("Error: " + friendlyMsg);

                if (retried()) {
                    ErrorEvent.extract(tag, friendlyMsg);
                } else if (engine instanceof ExoPlayerEngine) {
                    handleExoError(errorCode, friendlyMsg);
                } else {
                    ErrorEvent.extract(tag, friendlyMsg);
                }
            }

            @Override
            public void onVideoSizeChanged(int width, int height) {
                PlayerEvent.size(tag);
            }

            @Override
            public void onTracksChanged() {
                if (engine instanceof ExoPlayerEngine) {
                    ExoPlayerEngine exoEngine = (ExoPlayerEngine) engine;
                    if (exoEngine.getExoPlayer() != null) {
                        String key = getKey();
                        App.execute(() -> {
                            List<Track> tracks = Track.find(key);
                            App.post(() -> {
                                setTrack(tracks);
                                PlayerEvent.track(tag);
                            });
                        });
                    }
                }
            }

            @Override
            public void onPositionChanged(long position, long duration) {
                // 用于弹幕同步
                if (danPlayer != null) danPlayer.check(Player.STATE_READY);
            }
        };
    }

    private void handleExoError(int errorCode, String friendlyMsg) {
        switch (errorCode) {
            case PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW:
                seekToDefaultPosition();
                break;
            case PlaybackException.ERROR_CODE_DECODER_INIT_FAILED:
            case PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED:
            case PlaybackException.ERROR_CODE_DECODING_FAILED:
                toggleDecode();
                break;
            case PlaybackException.ERROR_CODE_IO_UNSPECIFIED:
            case PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED:
            case PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED:
            case PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED:
            case PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED:
                setFormat(ExoUtil.getMimeType(errorCode));
                break;
            default:
                ErrorEvent.extract(tag, friendlyMsg);
                break;
        }
    }

    public void setDanmakuView(DanmakuView view) {
        danPlayer = new DanPlayer();
        danPlayer.setPlayer(this);
        danPlayer.setView(view);
    }

    public ExoPlayerEngine getExoEngine() {
        return engine instanceof ExoPlayerEngine ? (ExoPlayerEngine) engine : null;
    }

    public MpvPlayerEngine getMpvEngine() {
        return engine instanceof MpvPlayerEngine ? (MpvPlayerEngine) engine : null;
    }

    public boolean isMpvActive() {
        return engine instanceof MpvPlayerEngine;
    }

    public MediaSessionCompat getSession() {
        return session;
    }

    public List<Danmaku> getDanmakus() {
        return danmakus;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, String> getHeaders() {
        return headers == null ? new HashMap<>() : headers;
    }

    public void setSub(Sub sub) {
        this.sub = sub;
        setMediaItem();
    }

    public void setFormat(String format) {
        this.format = format;
        setMediaItem();
    }

    public String getKey() {
        return key != null ? key : url;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void reset() {
        removeTimeoutCheck();
        retry = 0;
    }

    public void clearMediaItems() {
        if (engine != null) engine.clearMediaItems();
    }

    public void clear() {
        danmakus = null;
        headers = null;
        format = null;
        subs = null;
        drm = null;
        url = null;
    }

    public String stringToTime(long time) {
        return Util.format(builder, formatter, time);
    }

    public int getVideoWidth() {
        return engine == null ? 0 : engine.getVideoWidth();
    }

    public int getVideoHeight() {
        return engine == null ? 0 : engine.getVideoHeight();
    }

    public float getSpeed() {
        return engine == null ? 1.0f : engine.getSpeed();
    }

    public long getPosition() {
        return engine == null ? C.TIME_UNSET : engine.getCurrentPosition();
    }

    public long getDuration() {
        return engine == null ? -1 : engine.getDuration();
    }

    public long getBuffered() {
        return engine == null ? 0 : engine.getBufferedPosition();
    }

    public boolean retried() {
        return ++retry > 2;
    }

    public boolean haveTrack(int type) {
        ExoPlayerEngine exoEngine = getExoEngine();
        return exoEngine != null && exoEngine.haveTrack(type);
    }

    public boolean haveDanmaku() {
        if (danmakus != null) for (Danmaku danmaku : danmakus) if (danmaku.isSelected()) return true;
        return false;
    }

    public boolean isPlaying() {
        return engine != null && engine.isPlaying();
    }

    public boolean isEnded() {
        return engine != null && engine.isEnded();
    }

    public boolean isIdle() {
        return engine != null && engine.isIdle();
    }

    public boolean isEmpty() {
        return TextUtils.isEmpty(getUrl());
    }

    public boolean isLive() {
        return getDuration() < TimeUnit.MINUTES.toMillis(1) || (engine instanceof ExoPlayerEngine
                && getExoEngine() != null && getExoEngine().getExoPlayer() != null
                && getExoEngine().getExoPlayer().isCurrentMediaItemLive());
    }

    public boolean isVod() {
        return getDuration() > TimeUnit.MINUTES.toMillis(1) && engine instanceof ExoPlayerEngine
                && getExoEngine() != null && getExoEngine().getExoPlayer() != null
                && !getExoEngine().getExoPlayer().isCurrentMediaItemLive();
    }

    public boolean isHard() {
        return engine != null && engine.isHard();
    }

    public boolean isPortrait() {
        return getVideoHeight() > getVideoWidth();
    }

    public boolean isLandscape() {
        return getVideoWidth() > getVideoHeight();
    }

    public String getSizeText() {
        return getVideoWidth() == 0 && getVideoHeight() == 0 ? "" : getVideoWidth() + " x " + getVideoHeight();
    }

    public String getSpeedText() {
        return String.format(Locale.getDefault(), "%.2f", getSpeed());
    }

    public String getDecodeText() {
        if (isMpvActive()) return "MPV";
        return ResUtil.getStringArray(R.array.select_decode)[decode];
    }

    public String setSpeed(float speed) {
        if (engine != null) engine.setSpeed(speed);
        return getSpeedText();
    }

    public String addSpeed() {
        float speed = getSpeed();
        float addon = speed >= 2 ? 1f : 0.25f;
        speed = speed >= 5 ? 0.25f : Math.min(speed + addon, 5.0f);
        return setSpeed(speed);
    }

    public String addSpeed(float value) {
        float speed = getSpeed();
        speed = Math.min(speed + value, 5);
        return setSpeed(speed);
    }

    public String subSpeed(float value) {
        float speed = getSpeed();
        speed = Math.max(speed - value, 0.25f);
        return setSpeed(speed);
    }

    public String toggleSpeed() {
        float speed = getSpeed();
        speed = speed == 1 ? Setting.getSpeed() : 1;
        return setSpeed(speed);
    }

    public void toggleDecode() {
        // 如果在 MPV 模式下，先切回 ExoPlayer
        if (isMpvActive()) {
            playerEngine = ENGINE_EXO;
            Setting.putPlayerEngine(ENGINE_EXO);
        }
        // 循环切换：软解 -> 硬解 -> 自动 -> 软解
        if (decode == SOFT) decode = HARD;
        else if (decode == HARD) decode = AUTO;
        else decode = SOFT;
        Setting.putDecode(decode);
        init(view);
    }

    /**
     * 切换播放器引擎：ExoPlayer ↔ MPV
     */
    public void togglePlayerEngine() {
        if (isMpvActive()) {
            playerEngine = ENGINE_EXO;
            Setting.putPlayerEngine(ENGINE_EXO);
            Notify.show("切换到 ExoPlayer 播放器");
        } else {
            playerEngine = ENGINE_MPV;
            Setting.putPlayerEngine(ENGINE_MPV);
            Notify.show("切换到 MPV 播放器");
        }
        init(view);
    }

    public String getPositionTime(long time) {
        time = getPosition() + time;
        if (time > getDuration()) time = getDuration();
        else if (time < 0) time = 0;
        return stringToTime(time);
    }

    public String getDurationTime() {
        long time = getDuration();
        if (time < 0) time = 0;
        return stringToTime(time);
    }

    public void seek(long time) {
        seekTo(getPosition() + time);
    }

    public void seekTo(long time) {
        if (engine != null) engine.seekTo(time);
        if (danPlayer != null) danPlayer.seekTo(time);
    }

    public void seekToDefaultPosition() {
        if (engine != null) engine.seekToDefaultPosition();
        prepare();
    }

    public void prepare() {
        if (engine != null) engine.prepare();
    }

    public void play() {
        if (engine != null) engine.play();
        if (danPlayer != null) danPlayer.play();
    }

    public void pause() {
        if (engine != null) engine.pause();
        if (danPlayer != null) danPlayer.pause();
    }

    public void stop() {
        if (engine != null) engine.stop();
        if (danPlayer != null) danPlayer.stop();
        stopParse();
    }

    public void release() {
        stopParse();
        releasePlayer();
        session.release();
        removeTimeoutCheck();
        Server.get().setPlayer(null);
        App.execute(() -> Source.get().stop());
    }

    private void releasePlayer() {
        if (engine != null) {
            engine.release();
            engine = null;
        }
        if (danPlayer != null) danPlayer.release();
        if (view != null) view.setPlayer(null);
    }

    private void removeTimeoutCheck() {
        App.removeCallbacks(runnable);
    }

    public void start(Channel channel, long timeout) {
        if (channel.getDrm() != null && !FrameworkMediaDrm.isCryptoSchemeSupported(channel.getDrm().getUUID())) {
            ErrorEvent.drm(tag);
        } else if (channel.hasMsg()) {
            ErrorEvent.extract(tag, channel.getMsg());
        } else if (channel.getParse() == 1) {
            startParse(channel.result(), false);
        } else if (isIllegal(channel.getUrl())) {
            ErrorEvent.url(tag);
        } else {
            setMediaItem(channel, timeout);
        }
    }

    public void start(Result result, boolean useParse, long timeout) {
        if (result.getDrm() != null && !FrameworkMediaDrm.isCryptoSchemeSupported(result.getDrm().getUUID())) {
            ErrorEvent.drm(tag);
        } else if (result.hasMsg()) {
            ErrorEvent.extract(tag, result.getMsg());
        } else if (result.getParse() == 1 || result.getJx() == 1) {
            startParse(result, useParse);
        } else if (isIllegal(result.getRealUrl())) {
            ErrorEvent.url(tag);
        } else {
            setMediaItem(result, timeout);
        }
    }

    private void startParse(Result result, boolean useParse) {
        stopParse();
        drm = result.getDrm();
        subs = result.getSubs();
        format = result.getFormat();
        danmakus = result.getDanmaku();
        parseJob = ParseJob.create(this).start(result, useParse);
    }

    private void stopParse() {
        if (parseJob != null) parseJob.stop();
        parseJob = null;
    }

    private Map<String, String> checkUa(Map<String, String> headers) {
        for (Map.Entry<String, String> header : headers.entrySet()) if (HttpHeaders.USER_AGENT.equalsIgnoreCase(header.getKey())) return headers;
        headers.put(HttpHeaders.USER_AGENT, Setting.getUa().isEmpty() ? ExoUtil.getUa() : Setting.getUa());
        return headers;
    }

    private List<Sub> checkSub(List<Sub> subs) {
        if (subs == null) subs = this.subs = new ArrayList<>();
        if (sub == null || subs.contains(sub)) return subs;
        subs.add(0, sub);
        return subs;
    }

    private void setMediaItem() {
        if (url != null) setMediaItem(headers, url, format, drm, subs, danmakus, Constant.TIMEOUT_PLAY);
    }

    public void setMediaItem(String url) {
        setMediaItem(new HashMap<>(), url);
    }

    private void setMediaItem(Map<String, String> headers, String url) {
        setMediaItem(headers, url, format, drm, subs, danmakus, Constant.TIMEOUT_PLAY);
    }

    private void setMediaItem(Channel channel, long timeout) {
        setMediaItem(channel.getHeaders(), channel.getUrl(), channel.getFormat(), channel.getDrm(), new ArrayList<>(), new ArrayList<>(), timeout);
    }

    private void setMediaItem(Result result, long timeout) {
        setMediaItem(result.getHeaders(), result.getRealUrl(), result.getFormat(), result.getDrm(), result.getSubs(), result.getDanmaku(), timeout);
    }

    private void setMediaItem(Map<String, String> headers, String url, String format, Drm drm, List<Sub> subs, List<Danmaku> danmakus, long timeout) {
        this.url = url;
        this.format = format;
        this.drm = drm;
        this.subs = subs;
        this.headers = checkUa(headers);
        this.danmakus = danmakus;

        if (engine != null) {
            engine.load(url, this.headers, format, drm, checkSub(subs), decode);
        }

        if (danPlayer != null) setDanmaku(danmakus);
        App.post(runnable, timeout);
        PlayerEvent.prepare(tag);
        session.setActive(true);
        Logger.d(url);
        prepare();
    }

    private void setDanmaku(List<Danmaku> items) {
        setDanmaku(items == null || items.isEmpty() ? Danmaku.empty() : items.get(0));
    }

    public void setDanmaku(Danmaku item) {
        danPlayer.setDanmaku(item);
        if (danmakus == null) danmakus = new ArrayList<>();
        if (!item.isEmpty() && !danmakus.contains(item)) danmakus.add(0, item);
        for (int i = 0; i < danmakus.size(); i++) danmakus.get(i).setSelected(danmakus.get(i).getUrl().equals(item.getUrl()));
        // 应用弹幕大小设置
        danPlayer.setTextSize(Setting.getDanmakuSize());
    }

    public void setDanmakuSize(float size) {
        if (danPlayer != null) danPlayer.setTextSize(size);
    }

    public void resetTrack() {
        ExoPlayerEngine exoEngine = getExoEngine();
        if (exoEngine != null) exoEngine.resetTrack();
    }

    public void setTrack(List<Track> tracks) {
        if (isMpvActive()) return; // MPV 暂不支持轨道切换
        ExoPlayerEngine exoEngine = getExoEngine();
        if (exoEngine == null) return;
        for (Track track : tracks) exoEngine.setTrack(track);
    }

    private void setTrack(Track item) {
        ExoPlayerEngine exoEngine = getExoEngine();
        if (exoEngine != null) exoEngine.setTrack(item);
    }

    private void setPlaybackState(int state) {
        long actions = PlaybackStateCompat.ACTION_SEEK_TO | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        session.setPlaybackState(new PlaybackStateCompat.Builder().setActions(actions).setState(state, getPosition(), getSpeed()).build());
    }

    private boolean isIllegal(String url) {
        Uri uri = UrlUtil.uri(url);
        String host = UrlUtil.host(uri);
        String scheme = UrlUtil.scheme(uri);
        if ("data".equals(scheme)) return false;
        return scheme.isEmpty() || "file".equals(scheme) ? !Path.exists(url) : host.isEmpty();
    }

    private MediaMetadataCompat.Builder putBitmap(MediaMetadataCompat.Builder builder, Drawable drawable) {
        try {
            return builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, ((BitmapDrawable) drawable).getBitmap());
        } catch (Exception ignored) {
            return builder;
        }
    }

    public void setMetadata(String title, String artist, String artUri, Drawable drawable) {
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
        builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, title);
        builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist);
        builder.putString(MediaMetadataCompat.METADATA_KEY_ART_URI, artUri);
        builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, artUri);
        builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, artUri);
        builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getDuration());
        session.setMetadata(putBitmap(builder, drawable).build());
        ActionEvent.update();
    }

    public void share(Activity activity, CharSequence title) {
        try {
            if (isEmpty()) return;
            Bundle bundle = new Bundle();
            for (Map.Entry<String, String> entry : getHeaders().entrySet()) bundle.putString(entry.getKey(), entry.getValue());
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Intent.EXTRA_TEXT, getUrl());
            intent.putExtra("extra_headers", bundle);
            intent.putExtra("title", title);
            intent.putExtra("name", title);
            intent.setType("text/plain");
            activity.startActivity(Util.getChooser(intent));
        } catch (Exception e) {
            Logger.e("Error", e);
        }
    }

    public void choose(Activity activity, CharSequence title) {
        try {
            if (isEmpty()) return;
            List<String> list = new ArrayList<>();
            for (Map.Entry<String, String> entry : getHeaders().entrySet()) list.addAll(Arrays.asList(entry.getKey(), entry.getValue()));
            Uri data = getUrl().startsWith("file://") || getUrl().startsWith("/") ? FileUtil.getShareUri(getUrl()) : Uri.parse(getUrl());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(data, "video/*");
            intent.putExtra("title", title);
            intent.putExtra("return_result", isVod());
            intent.putExtra("headers", list.toArray(new String[0]));
            if (isVod()) intent.putExtra("position", (int) getPosition());
            activity.startActivityForResult(Util.getChooser(intent), 1001);
        } catch (Exception e) {
            Logger.e("Error", e);
        }
    }

    public void checkData(Intent data) {
        try {
            if (data == null || data.getExtras() == null) return;
            int position = data.getExtras().getInt("position", 0);
            String endBy = data.getExtras().getString("end_by", "");
            if ("playback_completion".equals(endBy)) ActionEvent.next();
            if ("user".equals(endBy)) seekTo(position);
        } catch (Exception e) {
            Logger.e("Error", e);
        }
    }

    @Override
    public void onParseSuccess(Map<String, String> headers, String url, String from) {
        if (!TextUtils.isEmpty(from)) Notify.show(ResUtil.getString(R.string.parse_from, from));
        if (headers != null) headers.remove(HttpHeaders.RANGE);
        setMediaItem(headers, url);
    }

    @Override
    public void onParseError() {
        ErrorEvent.parse(tag);
    }
}