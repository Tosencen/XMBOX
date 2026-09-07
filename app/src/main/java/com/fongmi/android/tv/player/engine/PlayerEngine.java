package com.fongmi.android.tv.player.engine;

import androidx.annotation.Nullable;
import androidx.media3.common.Player;

import com.fongmi.android.tv.bean.Drm;
import com.fongmi.android.tv.bean.Sub;

import java.util.List;
import java.util.Map;

/**
 * 播放器引擎接口 — 统一 ExoPlayer 和 MPV 的交互方式。
 *
 * 所有播放器相关的操作通过此接口调用，上层 {@code Players} 无需关心
 * 底层是 ExoPlayer 还是 MPV，消除 {@code isMpvActive()} 条件分支。
 */
public interface PlayerEngine {

    /** 初始化播放器并绑定到视图 */
    void init(android.view.View view);

    /** 播放 */
    void play();

    /** 暂停 */
    void pause();

    /** 停止 */
    void stop();

    /** 释放所有资源 */
    void release();

    /** 跳转到指定位置 (毫秒) */
    void seekTo(long timeMs);

    /** 跳转到默认起始位置 */
    void seekToDefaultPosition();

    /** 准备播放 */
    void prepare();

    /** 加载媒体 */
    void load(String url, @Nullable Map<String, String> headers, @Nullable String format,
              @Nullable Drm drm, @Nullable List<Sub> subs, int decodeMode);

    /** 清除媒体项 */
    void clearMediaItems();

    // ==================== 查询 ====================

    long getCurrentPosition();
    long getDuration();
    long getBufferedPosition();
    float getSpeed();
    boolean isPlaying();
    boolean isEnded();
    boolean isIdle();
    boolean isHard();
    int getVideoWidth();
    int getVideoHeight();

    /** 设置播放速度 */
    void setSpeed(float speed);

    /** 设置监听器 */
    void setListener(@Nullable Listener listener);

    /** 引擎事件监听 */
    interface Listener {
        default void onPlaybackStateChanged(int state) {}
        default void onPlayerError(int errorCode, @Nullable String message) {}
        default void onVideoSizeChanged(int width, int height) {}
        default void onTracksChanged() {}
        default void onPositionChanged(long position, long duration) {}
    }
}