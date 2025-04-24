package com.github.tvbox.osc.cast.dlna;

import android.content.Context;
import android.util.Log;

import com.github.tvbox.osc.cast.CastDevice;

import java.util.ArrayList;
import java.util.List;

/**
 * DLNA投屏助手类 - 已禁用所有功能
 */
public class DLNACastHelper {
    private static final String TAG = "DLNACastHelper";
    private static DLNACastHelper instance;

    private DLNACastHelper() {
        // 私有构造函数
    }

    public static DLNACastHelper getInstance() {
        if (instance == null) {
            synchronized (DLNACastHelper.class) {
                if (instance == null) {
                    instance = new DLNACastHelper();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化DLNA投屏
     */
    public void initialize(Context context) {
        Log.d(TAG, "DLNA功能已禁用");
    }

    /**
     * 开始搜索设备
     */
    public void startDiscovery() {
        Log.d(TAG, "DLNA功能已禁用");
    }

    /**
     * 停止搜索设备
     */
    public void stopDiscovery() {
        Log.d(TAG, "DLNA功能已禁用");
    }

    /**
     * 获取设备列表
     */
    public List<CastDevice> getDevices() {
        return new ArrayList<>();
    }

    /**
     * 设置设备监听器
     */
    public void setDeviceListener(DeviceListener listener) {
        Log.d(TAG, "DLNA功能已禁用");
    }

    /**
     * 投屏到指定设备
     */
    public void cast(CastDevice device, Object video) {
        Log.d(TAG, "DLNA功能已禁用");
    }

    /**
     * 停止投屏
     */
    public void stopCast(CastDevice device) {
        Log.d(TAG, "DLNA功能已禁用");
    }

    /**
     * 暂停投屏
     */
    public void pauseCast(CastDevice device) {
        Log.d(TAG, "DLNA功能已禁用");
    }

    /**
     * 继续投屏
     */
    public void resumeCast(CastDevice device) {
        Log.d(TAG, "DLNA功能已禁用");
    }

    /**
     * 设置播放进度
     */
    public void seekTo(CastDevice device, long position) {
        Log.d(TAG, "DLNA功能已禁用");
    }

    /**
     * 释放资源
     */
    public void release(Context context) {
        Log.d(TAG, "DLNA功能已禁用");
    }

    /**
     * 设备监听器接口
     */
    public interface DeviceListener {
        void onDeviceAdded(CastDevice device);
        void onDeviceRemoved(CastDevice device);
    }
}
