package com.fongmi.android.tv.player.engine;

import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.media3.common.C;

import com.fongmi.android.tv.bean.Drm;
import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.player.mpv.MpvPlayer;

import java.util.List;
import java.util.Map;

/**
 * MPV 引擎实现。
 * 将 MpvPlayer 适配为 PlayerEngine 接口，上层无需感知 MPV 的细节。
 */
public class MpvPlayerEngine implements PlayerEngine {

    private MpvPlayer mpvPlayer;
    private Listener listener;
    private boolean hard = true;

    @Override
    public void init(View view) {
        release();
        if (!(view instanceof SurfaceView)) {
            // 需要 SurfaceView 做渲染
            return;
        }
        mpvPlayer = new MpvPlayer();
        mpvPlayer.addListener(createMpvListener());
        mpvPlayer.initialize((SurfaceView) view);
    }

    private MpvPlayer.Listener createMpvListener() {
        return new MpvPlayer.Listener() {
            @Override
            public void onPlaying() {
                if (listener != null) listener.onPlaybackStateChanged(
                        androidx.media3.common.Player.STATE_READY);
            }

            @Override
            public void onPaused() {
                // no-op
            }

            @Override
            public void onPositionChanged(long position, long duration) {
                if (listener != null) listener.onPositionChanged(position, duration);
            }

            @Override
            public void onCompletion() {
                if (listener != null) listener.onPlaybackStateChanged(
                        androidx.media3.common.Player.STATE_ENDED);
            }

            @Override
            public void onError(String message) {
                if (listener != null) listener.onPlayerError(-1, message);
            }

            @Override
            public void onVideoSizeChanged(int width, int height) {
                if (listener != null) listener.onVideoSizeChanged(width, height);
            }
        };
    }

    @Override
    public void play() {
        if (mpvPlayer != null) mpvPlayer.setPause(false);
    }

    @Override
    public void pause() {
        if (mpvPlayer != null) mpvPlayer.setPause(true);
    }

    @Override
    public void stop() {
        if (mpvPlayer != null) mpvPlayer.stop();
    }

    @Override
    public void release() {
        if (mpvPlayer != null) {
            mpvPlayer.release();
            mpvPlayer = null;
        }
    }

    @Override
    public void seekTo(long timeMs) {
        if (mpvPlayer != null) mpvPlayer.seekTo(timeMs);
    }

    @Override
    public void seekToDefaultPosition() {
        if (mpvPlayer != null) mpvPlayer.seekTo(0);
        prepare();
    }

    @Override
    public void prepare() {
        // MPV 不需要显式 prepare
    }

    @Override
    public void load(String url, @Nullable Map<String, String> headers, @Nullable String format,
                     @Nullable Drm drm, @Nullable List<Sub> subs, int decodeMode) {
        if (mpvPlayer != null) {
            mpvPlayer.load(url, headers);
        }
    }

    @Override
    public void clearMediaItems() {
        if (mpvPlayer != null) mpvPlayer.stop();
    }

    @Override
    public long getCurrentPosition() {
        return mpvPlayer == null ? C.TIME_UNSET : mpvPlayer.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        return mpvPlayer == null ? -1 : mpvPlayer.getDuration();
    }

    @Override
    public long getBufferedPosition() {
        return mpvPlayer == null ? 0 : mpvPlayer.getBufferedPosition();
    }

    @Override
    public float getSpeed() {
        return mpvPlayer == null ? 1.0f : mpvPlayer.getSpeed();
    }

    @Override
    public boolean isPlaying() {
        return mpvPlayer != null && mpvPlayer.isPlaying();
    }

    @Override
    public boolean isEnded() {
        return mpvPlayer != null && mpvPlayer.isEnded();
    }

    @Override
    public boolean isIdle() {
        return mpvPlayer != null && mpvPlayer.isIdle();
    }

    @Override
    public boolean isHard() {
        return true; // MPV 使用硬件加速
    }

    @Override
    public int getVideoWidth() {
        return mpvPlayer == null ? 0 : mpvPlayer.getVideoWidth();
    }

    @Override
    public int getVideoHeight() {
        return mpvPlayer == null ? 0 : mpvPlayer.getVideoHeight();
    }

    @Override
    public void setSpeed(float speed) {
        if (mpvPlayer != null) mpvPlayer.setSpeed(speed);
    }

    @Override
    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    /** 获取原始 MpvPlayer 实例 */
    public MpvPlayer getMpvPlayer() {
        return mpvPlayer;
    }

    /** 检查 MPV 库是否可用 */
    public static boolean isLibraryAvailable() {
        return MpvPlayer.isLibraryAvailable();
    }
}