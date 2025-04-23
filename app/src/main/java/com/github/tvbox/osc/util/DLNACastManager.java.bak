package com.github.tvbox.osc.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.github.tvbox.osc.bean.CastVideo;
import com.github.tvbox.osc.service.DLNACastService;

import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.item.VideoItem;
import org.fourthline.cling.support.model.ProtocolInfo;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;

import java.util.UUID;

/**
 * DLNA投屏管理器
 */
public class DLNACastManager {

    private static DLNACastManager instance;
    private DLNACastService castService;
    private boolean isBound = false;

    private DLNACastManager() {
    }

    public static DLNACastManager getInstance() {
        if (instance == null) {
            synchronized (DLNACastManager.class) {
                if (instance == null) {
                    instance = new DLNACastManager();
                }
            }
        }
        return instance;
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            castService = null;
        }
    };

    public void bindCastService(Context context) {
        Intent intent = new Intent(context, DLNACastService.class);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        context.startService(intent);
    }

    public void unbindCastService(Context context) {
        if (isBound) {
            context.unbindService(serviceConnection);
            isBound = false;
        }
    }

    public void registerDeviceListener(RegistryListener listener) {
        if (castService != null) {
            castService.addListener(listener);
        }
    }

    public void unregisterListener(RegistryListener listener) {
        if (castService != null) {
            castService.removeListener(listener);
        }
    }

    public void search(RegistryListener listener, int timeout) {
        if (castService != null) {
            if (listener != null) {
                castService.addListener(listener);
            }

            // 搜索设备
            ControlPoint controlPoint = castService.getControlPoint();
            if (controlPoint != null) {
                controlPoint.search();
            }
        }
    }

    public void cast(Device device, CastVideo video) {
        if (castService == null || device == null || video == null) return;

        Service avTransportService = device.findService(new UDAServiceType("AVTransport"));
        if (avTransportService == null) return;

        // 创建DIDL内容
        DIDLContent didlContent = new DIDLContent();
        // 使用正确的VideoItem构造函数
        org.fourthline.cling.support.model.item.VideoItem videoItem = new org.fourthline.cling.support.model.item.VideoItem(
                UUID.randomUUID().toString(),
                "0",
                video.getTitle(),
                "",
                null
        );
        // 添加资源
        String mimeType = "video/mp4";
        String url = video.getUrl();
        if (url.toLowerCase().endsWith(".mkv")) {
            mimeType = "video/x-matroska";
        } else if (url.toLowerCase().endsWith(".mp4")) {
            mimeType = "video/mp4";
        } else if (url.toLowerCase().endsWith(".avi")) {
            mimeType = "video/avi";
        } else if (url.toLowerCase().endsWith(".rmvb")) {
            mimeType = "video/vnd.rn-realvideo";
        }

        org.fourthline.cling.support.model.ProtocolInfo protocolInfo =
            new org.fourthline.cling.support.model.ProtocolInfo(
                "http-get:*:" + mimeType + ":*");

        videoItem.addResource(new org.fourthline.cling.support.model.Res(
                protocolInfo,
                0L,
                null,
                null,
                video.getUrl()
        ));
        didlContent.addItem(videoItem);

        // 设置播放URI
        // 创建SetAVTransportURI的ActionInvocation
        org.fourthline.cling.model.action.ActionInvocation setAVTransportURIAction =
                new org.fourthline.cling.model.action.ActionInvocation(
                        avTransportService.getAction("SetAVTransportURI"));

        // 设置参数
        setAVTransportURIAction.setInput("InstanceID", new UnsignedIntegerFourBytes(0));
        setAVTransportURIAction.setInput("CurrentURI", video.getUrl());
        setAVTransportURIAction.setInput("CurrentURIMetaData", didlContent.toString());

        // 执行操作
        castService.getControlPoint().execute(
                new org.fourthline.cling.controlpoint.ActionCallback(setAVTransportURIAction) {
            @Override
            public void success(ActionInvocation invocation) {
                // URI设置成功后开始播放
                // 创建Play的ActionInvocation
                org.fourthline.cling.model.action.ActionInvocation playAction =
                        new org.fourthline.cling.model.action.ActionInvocation(
                                avTransportService.getAction("Play"));

                // 设置参数
                playAction.setInput("InstanceID", new UnsignedIntegerFourBytes(0));
                playAction.setInput("Speed", "1");

                // 执行操作
                castService.getControlPoint().execute(
                        new org.fourthline.cling.controlpoint.ActionCallback(playAction) {
                    @Override
                    public void success(ActionInvocation invocation) {
                        // 播放成功
                    }

                    @Override
                    public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                        // 播放失败
                    }
                });
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                // 设置URI失败
            }
        });
    }

    public Registry getRegistry() {
        return castService != null ? castService.getRegistry() : null;
    }
}
