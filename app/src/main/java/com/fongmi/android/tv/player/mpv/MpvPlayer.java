package com.fongmi.android.tv.player.mpv;

import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.bean.Drm;
import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.player.exo.ExoUtil;
import com.github.catvod.utils.Logger;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MPV 播放器封装
 *
 * 基于 mpv-android (https://github.com/mpv-android/mpv-android) 实现。
 * 使用 SurfaceView 渲染视频输出，通过 JNI 与 libmpv 交互。
 *
 * 依赖（在 app/build.gradle 中添加）：
 *   implementation 'com.github.mpv-android:mpv-android:v1.0.0'
 *
 * 或者将 mpv-android 的 AAR 放入 libs/ 目录：
 *   implementation fileTree(dir: "libs", include: ["*.aar"])
 */
public class MpvPlayer {

    // ==================== 常量 ====================

    public static final int OPT_APPLY = 0;   // 软解
    public static final int OPT_HWA = 1;     // 硬解
    public static final int OPT_AUTO = 2;    // 自动

    private static final int EVENT_NONE = 0;
    private static final int EVENT_LOADING = 1;
    private static final int EVENT_PLAYING = 2;
    private static final int EVENT_PAUSED = 3;
    private static final int EVENT_ENDED = 4;
    private static final int EVENT_ERROR = 5;
    private static final int EVENT_STOPPED = 6;

    // ==================== 状态 ====================

    private static final int STATE_IDLE = 0;
    private static final int STATE_LOADING = 1;
    private static final int STATE_PLAYING = 2;
    private static final int STATE_PAUSED = 3;
    private static final int STATE_ENDED = 4;
    private static final int STATE_ERROR = 5;

    // ==================== 字段 ====================

    private SurfaceView surfaceView;
    private Surface surface;
    private final Handler mainHandler;
    private final List<Listener> listeners;

    // mpv 原生实例 (通过 mpv-android 库获取)
    private Object mpvInstance;      // 实际类型: isandlaTech.mpv.Mpv
    private boolean mpvInitialized;

    // 播放状态
    private int state = STATE_IDLE;
    private String currentUrl;
    private Map<String, String> currentHeaders;
    private long duration = C.TIME_UNSET;
    private long position;
    private float speed = 1.0f;
    private int hwDecodeMode = OPT_AUTO;
    private int videoWidth;
    private int videoHeight;

    // 播放进度轮询
    private static final long POSITION_POLL_INTERVAL_MS = 250;
    private boolean pollingActive;
    private final Runnable positionPollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!pollingActive) return;
            updatePosition();
            mainHandler.postDelayed(this, POSITION_POLL_INTERVAL_MS);
        }
    };

    // ==================== 接口 ====================

    public interface Listener {
        default void onPrepared() {}
        default void onPlaying() {}
        default void onPaused() {}
        default void onPositionChanged(long position, long duration) {}
        default void onBufferingUpdate(int percent) {}
        default void onCompletion() {}
        default void onError(@NonNull String message) {}
        default void onVideoSizeChanged(int width, int height) {}
        default void onSpeedChanged(float speed) {}
    }

    // ==================== 构造 ====================

    public MpvPlayer() {
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.listeners = new CopyOnWriteArrayList<>();
    }

    // ==================== 监听器 ====================

    public void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    // ==================== 初始化 ====================

    /**
     * 初始化 MPV 播放器并绑定 SurfaceView
     */
    public boolean initialize(@NonNull SurfaceView surfaceView) {
        this.surfaceView = surfaceView;

        try {
            // 通过反射加载 mpv-android 库，避免编译时强制依赖
            // 实际使用时，直接调用 Mpv.create() 即可
            Class<?> mpvClass = Class.forName("isandlaTech.mpv.Mpv");
            mpvInstance = mpvClass.getMethod("create").invoke(null);

            if (mpvInstance == null) {
                Logger.e("MpvPlayer: mpv.create() returned null");
                return false;
            }

            // 设置初始选项
            setInitialOptions();

            // 绑定 Surface
            attachSurface();

            mpvInitialized = true;
            Logger.d("MpvPlayer: initialized successfully");
            return true;
        } catch (ClassNotFoundException e) {
            Logger.e("MpvPlayer: mpv-android library not found. " +
                    "Add dependency: implementation 'com.github.mpv-android:mpv-android:v1.0.0'");
            return false;
        } catch (Exception e) {
            Logger.e("MpvPlayer: initialization failed: " + e.getMessage());
            return false;
        }
    }

    private void setInitialOptions() {
        if (mpvInstance == null) return;
        try {
            // 设置缓存
            command("set", "cache", "yes");
            command("set", "cache-size", "102400");  // 100MB
            command("set", "demuxer-max-bytes", "100M");
            command("set", "demuxer-max-back-bytes", "25M");

            // 设置音视频
            command("set", "volume", "100");
            command("set", "audio-file-auto", "no");

            // 设置字幕
            command("set", "sub-auto", "no");

            // 设置网络
            command("set", "user-agent", ExoUtil.getUa());

            // 设置硬件解码
            applyHwDecode(hwDecodeMode);

            // 设置日志
            command("set", "msg-level", "all=no");
            command("set", "msg-level", "cplayer=status");

            // 设置事件回调
            setupEventCallback();
        } catch (Exception e) {
            Logger.e("MpvPlayer: setInitialOptions error: " + e.getMessage());
        }
    }

    private void applyHwDecode(int mode) {
        if (mpvInstance == null) return;
        try {
            switch (mode) {
                case OPT_APPLY:
                    command("set", "hwdec", "no");
                    break;
                case OPT_HWA:
                    command("set", "hwdec", "auto");
                    break;
                case OPT_AUTO:
                default:
                    command("set", "hwdec", "auto-safe");
                    break;
            }
        } catch (Exception e) {
            Logger.e("MpvPlayer: applyHwDecode error: " + e.getMessage());
        }
    }

    private void attachSurface() {
        if (mpvInstance == null || surfaceView == null) return;
        try {
            // 当 SurfaceView 的 Surface 就绪时，传递给 mpv
            surfaceView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                // 重建 Surface 时重新绑定
                if (surface != null && !surface.isValid()) {
                    attachSurface();
                }
            });

            surfaceView.getHolder().addCallback(new android.view.SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(@NonNull android.view.SurfaceHolder holder) {
                    surface = holder.getSurface();
                    if (mpvInstance != null) {
                        try {
                            mpvInstance.getClass().getMethod("setSurface", Surface.class).invoke(mpvInstance, surface);
                        } catch (Exception e) {
                            Logger.e("MpvPlayer: setSurface error: " + e.getMessage());
                        }
                    }
                }

                @Override
                public void surfaceChanged(@NonNull android.view.SurfaceHolder holder, int format, int width, int height) {
                    // 尺寸变化时通知
                    videoWidth = width;
                    videoHeight = height;
                    for (Listener l : listeners) l.onVideoSizeChanged(width, height);
                }

                @Override
                public void surfaceDestroyed(@NonNull android.view.SurfaceHolder holder) {
                    surface = null;
                    if (mpvInstance != null) {
                        try {
                            mpvInstance.getClass().getMethod("setSurface", Surface.class).invoke(mpvInstance, (Surface) null);
                        } catch (Exception e) {
                            Logger.e("MpvPlayer: clearSurface error: " + e.getMessage());
                        }
                    }
                }
            });

            // 如果 Surface 已经就绪，直接绑定
            Surface existingSurface = surfaceView.getHolder().getSurface();
            if (existingSurface != null && existingSurface.isValid()) {
                surface = existingSurface;
                mpvInstance.getClass().getMethod("setSurface", Surface.class).invoke(mpvInstance, surface);
            }

        } catch (Exception e) {
            Logger.e("MpvPlayer: attachSurface error: " + e.getMessage());
        }
    }

    private void setupEventCallback() {
        if (mpvInstance == null) return;
        try {
            // 注册 mpv 事件回调
            // mpv-android 库通过 Mpv.observeProperty() 或事件循环来通知状态变化
            // 这里通过反射调用 setEventCallback
            // 实际使用时，直接使用 Mpv 的 API：mpv.setEventCallback(callback)

            // 由于 mpv-android 的 API 版本不同，这里使用反射适配
            // 主要监听：playback-state, time-pos, duration, width, height, eof-reached
            String[] properties = {"playback-state", "time-pos", "duration", "width", "height", "eof-reached"};
            for (String prop : properties) {
                try {
                    mpvInstance.getClass().getMethod("observeProperty", int.class, String.class)
                            .invoke(mpvInstance, prop.hashCode(), prop);
                } catch (Exception ignored) {
                }
            }

            // 启动事件处理循环
            startEventLoop();
        } catch (Exception e) {
            Logger.e("MpvPlayer: setupEventCallback error: " + e.getMessage());
        }
    }

    private void startEventLoop() {
        // mpv 事件通过回调接收，在回调中更新状态
        // 实际使用时，mpv-android 提供 Mpv.EventCallback 接口
        // 这里使用反射机制注册回调
        if (mpvInstance == null) return;
        try {
            // 设置事件回调
            Class<?> callbackClass = Class.forName("isandlaTech.mpv.Mpv$EventCallback");
            Object callback = java.lang.reflect.Proxy.newProxyInstance(
                    callbackClass.getClassLoader(),
                    new Class<?>[]{callbackClass},
                    (proxy, method, args) -> {
                        if (method.getName().equals("onEvent")) {
                            // args[0] = Mpv.Event 对象
                            handleMpvEvent(args[0]);
                        }
                        return null;
                    });
            mpvInstance.getClass().getMethod("setEventCallback", callbackClass).invoke(mpvInstance, callback);
        } catch (ClassNotFoundException e) {
            // 旧版 mpv-android API 可能没有 EventCallback，改用轮询方式
            Logger.d("MpvPlayer: EventCallback not available, using polling mode");
            startPolling();
        } catch (Exception e) {
            Logger.e("MpvPlayer: startEventLoop error: " + e.getMessage());
            startPolling();
        }
    }

    /**
     * 处理 mpv 事件（通过回调或轮询）
     */
    private void handleMpvEvent(Object event) {
        if (event == null || mpvInstance == null) return;
        try {
            // 获取事件属性
            String eventName = (String) event.getClass().getMethod("name").invoke(event);
            if (eventName == null) return;

            // 获取事件 ID
            int id = (int) event.getClass().getMethod("getId").invoke(event);

            // 根据事件 ID 判断属性变化
            String propName = getPropertyNameFromId(id);
            if (propName == null) return;

            // 获取属性值
            Object property = event.getClass().getMethod("getProperty").invoke(event);
            if (property == null) return;

            String value = property.toString();

            mainHandler.post(() -> {
                switch (propName) {
                    case "playback-state":
                        handlePlaybackState(value);
                        break;
                    case "time-pos":
                        handleTimePos(value);
                        break;
                    case "duration":
                        handleDuration(value);
                        break;
                    case "width":
                        handleVideoWidth(value);
                        break;
                    case "height":
                        handleVideoHeight(value);
                        break;
                    case "eof-reached":
                        if ("yes".equals(value)) {
                            setState(STATE_ENDED);
                            for (Listener l : listeners) l.onCompletion();
                        }
                        break;
                }
            });
        } catch (Exception e) {
            Logger.e("MpvPlayer: handleEvent error: " + e.getMessage());
        }
    }

    private String getPropertyNameFromId(int id) {
        // 反向查找属性名
        if (id == "playback-state".hashCode()) return "playback-state";
        if (id == "time-pos".hashCode()) return "time-pos";
        if (id == "duration".hashCode()) return "duration";
        if (id == "width".hashCode()) return "width";
        if (id == "height".hashCode()) return "height";
        if (id == "eof-reached".hashCode()) return "eof-reached";
        return null;
    }

    private void handlePlaybackState(String value) {
        switch (value) {
            case "playing":
                setState(STATE_PLAYING);
                for (Listener l : listeners) l.onPlaying();
                startPolling();
                break;
            case "paused":
                setState(STATE_PAUSED);
                for (Listener l : listeners) l.onPaused();
                break;
            case "loading":
                setState(STATE_LOADING);
                break;
            case "stopped":
                setState(STATE_IDLE);
                break;
        }
    }

    private void handleTimePos(String value) {
        try {
            position = (long) (Double.parseDouble(value) * 1000);
            for (Listener l : listeners) l.onPositionChanged(position, duration);
        } catch (NumberFormatException ignored) {
        }
    }

    private void handleDuration(String value) {
        try {
            duration = (long) (Double.parseDouble(value) * 1000);
        } catch (NumberFormatException ignored) {
        }
    }

    private void handleVideoWidth(String value) {
        try {
            videoWidth = Integer.parseInt(value);
            for (Listener l : listeners) l.onVideoSizeChanged(videoWidth, videoHeight);
        } catch (NumberFormatException ignored) {
        }
    }

    private void handleVideoHeight(String value) {
        try {
            videoHeight = Integer.parseInt(value);
            for (Listener l : listeners) l.onVideoSizeChanged(videoWidth, videoHeight);
        } catch (NumberFormatException ignored) {
        }
    }

    // ==================== 轮询（回调不可用时的降级方案） ====================

    private void startPolling() {
        if (pollingActive) return;
        pollingActive = true;
        mainHandler.post(positionPollRunnable);
    }

    private void stopPolling() {
        pollingActive = false;
        mainHandler.removeCallbacks(positionPollRunnable);
    }

    private void updatePosition() {
        if (mpvInstance == null) return;
        try {
            // 通过属性查询获取当前播放位置
            String posStr = getPropertyString("time-pos");
            if (posStr != null && !posStr.isEmpty()) {
                position = (long) (Double.parseDouble(posStr) * 1000);
            }

            String durStr = getPropertyString("duration");
            if (durStr != null && !durStr.isEmpty()) {
                duration = (long) (Double.parseDouble(durStr) * 1000);
            }

            // 检查播放状态
            String stateStr = getPropertyString("playback-state");
            if (stateStr != null) {
                int prevState = state;
                switch (stateStr) {
                    case "playing":
                        if (prevState == STATE_LOADING) {
                            for (Listener l : listeners) l.onPlaying();
                        }
                        setState(STATE_PLAYING);
                        break;
                    case "paused":
                        setState(STATE_PAUSED);
                        break;
                    case "loading":
                        setState(STATE_LOADING);
                        break;
                    case "stopped":
                        if (prevState == STATE_PLAYING || prevState == STATE_PAUSED) {
                            setState(STATE_ENDED);
                            for (Listener l : listeners) l.onCompletion();
                        } else {
                            setState(STATE_IDLE);
                        }
                        stopPolling();
                        break;
                }
            }

            for (Listener l : listeners) l.onPositionChanged(position, duration);

        } catch (Exception e) {
            Logger.e("MpvPlayer: updatePosition error: " + e.getMessage());
        }
    }

    // ==================== MPV 命令封装 ====================

    /**
     * 执行 mpv 命令
     */
    private void command(String... args) {
        if (mpvInstance == null) return;
        try {
            mpvInstance.getClass().getMethod("command", String[].class).invoke(mpvInstance, new Object[]{args});
        } catch (Exception e) {
            Logger.e("MpvPlayer: command error: " + e.getMessage());
        }
    }

    /**
     * 设置属性值
     */
    private void setPropertyString(String name, String value) {
        if (mpvInstance == null) return;
        try {
            mpvInstance.getClass().getMethod("setPropertyString", String.class, String.class)
                    .invoke(mpvInstance, name, value);
        } catch (Exception e) {
            Logger.e("MpvPlayer: setProperty error: " + e.getMessage());
        }
    }

    /**
     * 获取属性值
     */
    private String getPropertyString(String name) {
        if (mpvInstance == null) return null;
        try {
            return (String) mpvInstance.getClass().getMethod("getPropertyString", String.class)
                    .invoke(mpvInstance, name);
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 公开 API ====================

    /**
     * 加载并播放媒体
     */
    public void load(String url, @Nullable Map<String, String> headers) {
        if (mpvInstance == null || !mpvInitialized) {
            for (Listener l : listeners) l.onError("MPV not initialized");
            return;
        }

        this.currentUrl = url;
        this.currentHeaders = headers;
        this.duration = C.TIME_UNSET;
        this.position = 0;
        this.videoWidth = 0;
        this.videoHeight = 0;

        setState(STATE_LOADING);

        // 设置网络头信息
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String key = entry.getKey().toLowerCase();
                switch (key) {
                    case "user-agent":
                        command("set", "user-agent", entry.getValue());
                        break;
                    case "referer":
                    case "referrer":
                        command("set", "referrer", entry.getValue());
                        break;
                    case "cookie":
                        command("set", "cookies", "yes");
                        command("set", "cookies-file", "/dev/null");
                        // mpv 不支持直接设置 cookie header，需要额外处理
                        break;
                }
            }
        }

        // 加载视频
        command("loadfile", url, "replace");

        // 设置播放速度
        if (speed != 1.0f) {
            setPropertyString("speed", String.valueOf(speed));
        }

        for (Listener l : listeners) l.onPrepared();
    }

    /**
     * 暂停/继续
     */
    public void setPause(boolean pause) {
        if (mpvInstance == null) return;
        setPropertyString("pause", pause ? "yes" : "no");
    }

    /**
     * 跳转到指定位置
     */
    public void seekTo(long positionMs) {
        if (mpvInstance == null) return;
        command("seek", String.valueOf(positionMs / 1000.0), "absolute");
        this.position = positionMs;
    }

    /**
     * 设置播放速度
     */
    public void setSpeed(float speed) {
        this.speed = speed;
        if (mpvInstance != null) {
            setPropertyString("speed", String.valueOf(speed));
        }
        for (Listener l : listeners) l.onSpeedChanged(speed);
    }

    public float getSpeed() {
        return speed;
    }

    /**
     * 设置音量
     */
    public void setVolume(int volume) {
        if (mpvInstance == null) return;
        command("set", "volume", String.valueOf(Math.max(0, Math.min(100, volume))));
    }

    /**
     * 设置硬件解码模式
     */
    public void setHwDecodeMode(int mode) {
        this.hwDecodeMode = mode;
        applyHwDecode(mode);
    }

    // ==================== 查询方法 ====================

    public long getCurrentPosition() {
        return position;
    }

    public long getDuration() {
        return duration;
    }

    /**
     * 获取缓冲进度（毫秒）。
     * 通过 mpv 的 demuxer-cache-time 属性获取已缓冲的视频时间位置，
     * 不可用时回退到 getCurrentPosition()。
     */
    public long getBufferedPosition() {
        if (mpvInstance == null) return position;
        try {
            String cacheTime = getPropertyString("demuxer-cache-time");
            if (cacheTime != null && !cacheTime.isEmpty()) {
                return (long) (Double.parseDouble(cacheTime) * 1000);
            }
        } catch (Exception ignored) {
            // 降级：返回当前播放位置
        }
        return position;
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public boolean isPlaying() {
        return state == STATE_PLAYING;
    }

    public boolean isPaused() {
        return state == STATE_PAUSED;
    }

    public boolean isEnded() {
        return state == STATE_ENDED;
    }

    public boolean isIdle() {
        return state == STATE_IDLE;
    }

    public boolean isError() {
        return state == STATE_ERROR;
    }

    public boolean isLoading() {
        return state == STATE_LOADING;
    }

    public String getCurrentUrl() {
        return currentUrl;
    }

    // ==================== 内部状态管理 ====================

    private void setState(int newState) {
        if (this.state != newState) {
            this.state = newState;
            if (newState == STATE_IDLE || newState == STATE_ENDED || newState == STATE_ERROR) {
                stopPolling();
            }
        }
    }

    // ==================== 释放资源 ====================

    /**
     * 停止播放
     */
    public void stop() {
        stopPolling();
        if (mpvInstance != null) {
            try {
                command("stop");
            } catch (Exception ignored) {
            }
        }
        setState(STATE_IDLE);
        currentUrl = null;
    }

    /**
     * 释放播放器资源
     */
    public void release() {
        stopPolling();
        if (mpvInstance != null) {
            try {
                command("stop");
                // 释放 mpv 实例
                mpvInstance.getClass().getMethod("destroy").invoke(mpvInstance);
            } catch (Exception ignored) {
            }
            mpvInstance = null;
        }
        mpvInitialized = false;
        surface = null;
        surfaceView = null;
        listeners.clear();
        state = STATE_IDLE;
        currentUrl = null;
        currentHeaders = null;
        duration = C.TIME_UNSET;
        position = 0;
    }

    /**
     * 检查 mpv-android 库是否可用
     */
    public static boolean isLibraryAvailable() {
        try {
            Class.forName("isandlaTech.mpv.Mpv");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * 获取 mpv 版本信息
     */
    @Nullable
    public static String getVersion() {
        try {
            Class<?> mpvClass = Class.forName("isandlaTech.mpv.Mpv");
            Object instance = mpvClass.getMethod("create").invoke(null);
            String version = (String) mpvClass.getMethod("getPropertyString", String.class)
                    .invoke(instance, "mpv-version");
            instance.getClass().getMethod("destroy").invoke(instance);
            return version;
        } catch (Exception e) {
            return null;
        }
    }
}