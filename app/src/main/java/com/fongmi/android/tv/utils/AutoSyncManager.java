package com.fongmi.android.tv.utils;
import com.github.catvod.utils.Logger;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Device;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.impl.Callback;
import com.github.catvod.net.OkHttp;


import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * 局域网自动同步管理器
 * 支持定时自动同步观看记录到局域网设备
 */
public class AutoSyncManager implements ScanTask.Listener {

    private static AutoSyncManager instance;
    private final OkHttpClient client;
    private ScanTask scanTask;
    private boolean isSyncing = false;

    public static AutoSyncManager get() {
        if (instance == null) {
            instance = new AutoSyncManager();
        }
        return instance;
    }

    private AutoSyncManager() {
        client = OkHttp.client(Constant.TIMEOUT_SYNC);
    }

    /**
     * 检查是否启用自动同步
     */
    public boolean isAutoSyncEnabled() {
        return Setting.isAutoSync();
    }

    /**
     * 获取同步间隔（分钟）
     */
    public int getSyncInterval() {
        return Setting.getSyncInterval();
    }

    /**
     * 执行自动同步
     * 会自动扫描局域网设备并同步
     */
    public void performAutoSync() {
        if (isSyncing) {
            Logger.d("AutoSync: 同步正在进行中，跳过本次");
            return;
        }

        if (!isAutoSyncEnabled()) {
            Logger.d("AutoSync: 自动同步未启用");
            return;
        }

        Logger.d("AutoSync: 开始自动同步");
        isSyncing = true;

        // 获取已保存的设备列表
        List<Device> devices = Device.getAll();
        
        if (devices.isEmpty()) {
            Logger.d("AutoSync: 没有保存的设备，开始扫描局域网");
            // 如果没有保存的设备，扫描局域网
            scanAndSync();
        } else {
            Logger.d("AutoSync: 找到 " + devices.size() + " 个已保存的设备");
            // 使用第一个设备进行同步
            syncToDevice(devices.get(0));
        }
    }

    /**
     * 扫描局域网并同步
     */
    private void scanAndSync() {
        if (scanTask != null) {
            scanTask.stop();
        }
        
        scanTask = new ScanTask(this);
        
        // 获取已保存设备的IP列表用于扫描
        List<Device> savedDevices = Device.getAll();
        List<String> ips = new ArrayList<>();
        for (Device device : savedDevices) {
            ips.add(device.getIp());
        }
        
        scanTask.start(ips);
    }

    /**
     * 同步到指定设备
     */
    private void syncToDevice(Device device) {
        Logger.d("AutoSync: 同步到设备: " + device.getName() + " (" + device.getIp() + ")");
        
        // 构建同步数据
        FormBody.Builder body = new FormBody.Builder();
        body.add("device", Device.get().toString());
        body.add("config", Config.vod().toString());
        body.add("targets", App.gson().toJson(History.get()));

        // 获取同步模式（0=双向，1=上传，2=下载）
        int mode = Setting.getSyncMode();
        
        // 构建同步URL
        String url = String.format(Locale.getDefault(), 
            "%s/action?do=sync&mode=%d&type=history", 
            device.getIp(), mode);

        Logger.d("AutoSync: 发送同步请求: " + url);

        // 发送同步请求
        OkHttp.newCall(client, url, body.build()).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                Logger.d("AutoSync: 同步成功");
                isSyncing = false;
                
                // 发送刷新事件，更新UI
                App.post(() -> {
                    // 可以在这里发送通知或更新UI
                });
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Logger.e("AutoSync: 同步失败: " + e.getMessage());
                isSyncing = false;
            }
        });
    }

    @Override
    public void onFind(List<Device> devices) {
        Logger.d("AutoSync: 扫描到 " + devices.size() + " 个设备");
        
        if (scanTask != null) {
            scanTask.stop();
            scanTask = null;
        }

        if (!devices.isEmpty()) {
            // 使用第一个找到的设备进行同步（同步内含数据库读取，必须在后台线程）
            App.execute(() -> syncToDevice(devices.get(0)));
        } else {
            Logger.d("AutoSync: 未找到可用设备");
            isSyncing = false;
        }
    }

    /**
     * 停止同步任务
     */
    public void stop() {
        if (scanTask != null) {
            scanTask.stop();
            scanTask = null;
        }
        isSyncing = false;
    }
}
