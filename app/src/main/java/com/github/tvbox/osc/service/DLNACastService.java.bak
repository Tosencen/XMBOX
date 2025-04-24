package com.github.tvbox.osc.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration;
import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.message.header.STAllHeader;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;

import java.util.ArrayList;
import java.util.List;

/**
 * DLNA投屏服务
 */
public class DLNACastService extends Service {

    private UpnpService upnpService;
    private List<RegistryListener> listeners = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        createUpnpService();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createUpnpService() {
        upnpService = new UpnpServiceImpl(new AndroidUpnpServiceConfiguration(), new RegistryListener() {
            @Override
            public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
                // 远程设备发现开始
                for (RegistryListener listener : listeners) {
                    listener.remoteDeviceDiscoveryStarted(registry, device);
                }
            }

            @Override
            public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception ex) {
                // 远程设备发现失败
                for (RegistryListener listener : listeners) {
                    listener.remoteDeviceDiscoveryFailed(registry, device, ex);
                }
            }

            @Override
            public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
                // 添加远程设备
                for (RegistryListener listener : listeners) {
                    listener.remoteDeviceAdded(registry, device);
                }
            }

            @Override
            public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
                // 更新远程设备
                for (RegistryListener listener : listeners) {
                    listener.remoteDeviceUpdated(registry, device);
                }
            }

            @Override
            public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
                // 移除远程设备
                for (RegistryListener listener : listeners) {
                    listener.remoteDeviceRemoved(registry, device);
                }
            }

            @Override
            public void localDeviceAdded(Registry registry, LocalDevice device) {
                // 添加本地设备
                for (RegistryListener listener : listeners) {
                    listener.localDeviceAdded(registry, device);
                }
            }

            @Override
            public void localDeviceRemoved(Registry registry, LocalDevice device) {
                // 移除本地设备
                for (RegistryListener listener : listeners) {
                    listener.localDeviceRemoved(registry, device);
                }
            }

            @Override
            public void beforeShutdown(Registry registry) {
                // 关闭前
                for (RegistryListener listener : listeners) {
                    listener.beforeShutdown(registry);
                }
            }

            @Override
            public void afterShutdown() {
                // 关闭后
                for (RegistryListener listener : listeners) {
                    listener.afterShutdown();
                }
            }
        });

        // 启动服务
        upnpService.getControlPoint().search(new STAllHeader());
    }

    public void addListener(RegistryListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(RegistryListener listener) {
        listeners.remove(listener);
    }

    public ControlPoint getControlPoint() {
        return upnpService != null ? upnpService.getControlPoint() : null;
    }

    public Registry getRegistry() {
        return upnpService != null ? upnpService.getRegistry() : null;
    }

    @Override
    public void onDestroy() {
        if (upnpService != null) {
            upnpService.shutdown();
        }
        super.onDestroy();
    }
}
