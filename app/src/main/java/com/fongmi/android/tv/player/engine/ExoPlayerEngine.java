package com.fongmi.android.tv.player.engine;

import static androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON;
import static androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER;

import android.view.View;

import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoSize;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.util.EventLogger;
import androidx.media3.ui.PlayerView;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.bean.Drm;
import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.bean.Track;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.player.exo.ExoUtil;
import com.github.catvod.utils.Logger;

import java.util.List;
import java.util.Map;

/**
 * ExoPlayer 引擎实现。
 * 封装 ExoPlayer 的创建、生命周期、媒体加载和轨道管理。
 */
public class ExoPlayerEngine implements PlayerEngine, Player.Listener {

    private ExoPlayer exoPlayer;
    private PlayerView view;
    private VideoSize size;
    private int decodeMode;
    private Listener listener;

    public ExoPlayerEngine() {
        this.decodeMode = Setting.getDecode();
    }

    @Override
    public void init(View view) {
        release();
        this.view = (PlayerView) view;
        decodeMode = Setting.getDecode();
        createExoPlayer();
    }

    private void createExoPlayer() {
        int renderMode;
        if (decodeMode == Players.HARD) {
            renderMode = EXTENSION_RENDERER_MODE_ON;
        } else if (decodeMode == Players.SOFT) {
            renderMode = EXTENSION_RENDERER_MODE_PREFER;
        } else {
            renderMode = androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF;
        }

        exoPlayer = new ExoPlayer.Builder(App.get())
                .setLoadControl(ExoUtil.buildLoadControl())
                .setTrackSelector(ExoUtil.buildTrackSelector())
                .setRenderersFactory(ExoUtil.buildRenderersFactory(renderMode))
                .setMediaSourceFactory(ExoUtil.buildMediaSourceFactory())
                .build();
        exoPlayer.setAudioAttributes(AudioAttributes.DEFAULT, true);
        exoPlayer.addAnalyticsListener(new EventLogger());
        exoPlayer.setHandleAudioBecomingNoisy(true);
        exoPlayer.setPlayWhenReady(true);
        exoPlayer.addListener(this);
        view.setPlayer(exoPlayer);
        ExoUtil.setSubtitleView(view);
    }

    @Override
    public void play() {
        if (exoPlayer != null) exoPlayer.play();
    }

    @Override
    public void pause() {
        if (exoPlayer != null) exoPlayer.pause();
    }

    @Override
    public void stop() {
        if (exoPlayer != null) exoPlayer.stop();
    }

    @Override
    public void release() {
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
        if (view != null) {
            view.setPlayer(null);
            view = null;
        }
        size = null;
    }

    @Override
    public void seekTo(long timeMs) {
        if (exoPlayer != null) exoPlayer.seekTo(timeMs);
    }

    @Override
    public void seekToDefaultPosition() {
        if (exoPlayer != null) exoPlayer.seekToDefaultPosition();
        prepare();
    }

    @Override
    public void prepare() {
        if (exoPlayer != null) exoPlayer.prepare();
    }

    @Override
    public void load(String url, @Nullable Map<String, String> headers, @Nullable String format,
                     @Nullable Drm drm, @Nullable List<Sub> subs, int decodeMode) {
        if (exoPlayer == null) return;
        this.decodeMode = decodeMode;
        exoPlayer.setMediaItem(ExoUtil.getMediaItem(headers, Uri.parse(url), format, drm, subs, decodeMode));
    }

    @Override
    public void clearMediaItems() {
        if (exoPlayer != null) exoPlayer.clearMediaItems();
    }

    @Override
    public long getCurrentPosition() {
        return exoPlayer == null ? C.TIME_UNSET : exoPlayer.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        return exoPlayer == null ? -1 : exoPlayer.getDuration();
    }

    @Override
    public long getBufferedPosition() {
        return exoPlayer == null ? 0 : exoPlayer.getBufferedPosition();
    }

    @Override
    public float getSpeed() {
        return exoPlayer == null ? 1.0f : exoPlayer.getPlaybackParameters().speed;
    }

    @Override
    public boolean isPlaying() {
        return exoPlayer != null && exoPlayer.isPlaying();
    }

    @Override
    public boolean isEnded() {
        return exoPlayer != null && exoPlayer.getPlaybackState() == Player.STATE_ENDED;
    }

    @Override
    public boolean isIdle() {
        return exoPlayer != null && exoPlayer.getPlaybackState() == Player.STATE_IDLE;
    }

    @Override
    public boolean isHard() {
        return decodeMode == Players.HARD;
    }

    @Override
    public int getVideoWidth() {
        return size == null ? 0 : size.width;
    }

    @Override
    public int getVideoHeight() {
        return size == null ? 0 : size.height;
    }

    @Override
    public void setSpeed(float speed) {
        if (exoPlayer != null) {
            exoPlayer.setPlaybackParameters(exoPlayer.getPlaybackParameters().withSpeed(speed));
        }
    }

    @Override
    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    /** 获取原始 ExoPlayer 实例（供轨道管理等高级操作使用） */
    public ExoPlayer getExoPlayer() {
        return exoPlayer;
    }

    /** 切换解码模式并重建播放器 */
    public void toggleDecode(int newDecodeMode) {
        this.decodeMode = newDecodeMode;
        if (view != null) init(view);
    }

    /** 设置轨道选择 */
    public void setTrack(Track item) {
        if (item.isSelected()) {
            ExoUtil.selectTrack(exoPlayer, item.getGroup(), item.getTrack());
        } else {
            ExoUtil.deselectTrack(exoPlayer, item.getGroup(), item.getTrack());
        }
    }

    /** 重置轨道 */
    public void resetTrack() {
        ExoUtil.resetTrack(exoPlayer);
    }

    /** 检查是否有指定类型的轨道 */
    public boolean haveTrack(int type) {
        return exoPlayer != null && ExoUtil.haveTrack(exoPlayer.getCurrentTracks(), type);
    }

    // ==================== Player.Listener ====================

    @Override
    public void onPlaybackStateChanged(int state) {
        if (listener != null) listener.onPlaybackStateChanged(state);
    }

    @Override
    public void onVideoSizeChanged(VideoSize videoSize) {
        this.size = videoSize;
        if (listener != null) listener.onVideoSizeChanged(videoSize.width, videoSize.height);
    }

    @Override
    public void onTracksChanged(Tracks tracks) {
        if (tracks.isEmpty()) return;
        if (listener != null) listener.onTracksChanged();
    }

    @Override
    public void onPlayerError(PlaybackException error) {
        if (listener != null) {
            listener.onPlayerError(error.errorCode, error.getMessage());
        }
    }
}