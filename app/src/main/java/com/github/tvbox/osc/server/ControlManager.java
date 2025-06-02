package com.github.tvbox.osc.server;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.receiver.SearchReceiver;
import com.github.tvbox.osc.util.HawkConfig;
import com.orhanobut.hawk.Hawk;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * @author pj567
 * @date :2021/1/4
 * @description:
 */
public class ControlManager {
    private static ControlManager instance;
    private RemoteServer mServer = null;
    // 使用WeakReference避免内存泄漏
    private static WeakReference<Context> mContextRef;

    private ControlManager() {

    }

    public static ControlManager get() {
        if (instance == null) {
            synchronized (ControlManager.class) {
                if (instance == null) {
                    instance = new ControlManager();
                }
            }
        }
        return instance;
    }

    public static void init(Context context) {
        // 使用ApplicationContext避免内存泄漏
        Context appContext = context != null ? context.getApplicationContext() : null;
        mContextRef = new WeakReference<>(appContext);
    }

    /**
     * 获取安全的Context
     * @return Context或null
     */
    private static Context getContext() {
        return mContextRef != null ? mContextRef.get() : null;
    }

    public String getAddress(boolean local) {
        return local ? mServer.getLoadAddress() : mServer.getServerAddress();
    }

    public void startServer() {
        if (mServer != null) {
            return;
        }

        Context context = getContext();
        if (context == null) {
            return; // Context已被回收，无法启动服务器
        }

        do {
            mServer = new RemoteServer(RemoteServer.serverPort, context);
            mServer.setDataReceiver(new DataReceiver() {
                @Override
                public void onTextReceived(String text) {
                    if (!TextUtils.isEmpty(text)) {
                        Context ctx = getContext();
                        if (ctx != null) {
                            Intent intent = new Intent();
                            Bundle bundle = new Bundle();
                            bundle.putString("title", text);
                            intent.setAction(SearchReceiver.action);
                            intent.setPackage(ctx.getPackageName());
                            intent.setComponent(new ComponentName(ctx, SearchReceiver.class));
                            intent.putExtras(bundle);
                            ctx.sendBroadcast(intent);
                        }
                    }
                }

                @Override
                public void onApiReceived(String url) {
                    EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_API_URL_CHANGE, url));
                }

                @Override
                public void onPushReceived(String url) {
                    EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_PUSH_URL, url));
                }
            });
            try {
                mServer.start();
                IjkMediaPlayer.setDotPort(Hawk.get(HawkConfig.DOH_URL, 0) > 0, RemoteServer.serverPort);
                break;
            } catch (IOException ex) {
                RemoteServer.serverPort++;
                mServer.stop();
            }
        } while (RemoteServer.serverPort < 9999);
    }

    public void stopServer() {
        if (mServer != null && mServer.isStarting()) {
            mServer.stop();
        }
    }
}