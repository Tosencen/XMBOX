package com.fongmi.android.tv.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.bean.Config;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        registerCallback();
    }

    private void registerCallback() {
        ConnectivityManager manager = (ConnectivityManager) App.get().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) manager.registerDefaultNetworkCallback(new Callback());
        else manager.registerNetworkCallback(new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), new Callback());
    }

    static class Callback extends ConnectivityManager.NetworkCallback {

        private boolean first;

        @Override
        public void onAvailable(@NonNull Network network) {
            if (first) doJob();
            else first = true;
        }

        @Override
        public void onLost(@NonNull Network network) {
        }

        private void doJob() {
            // Config 读数据库移到后台线程，读完后回主线程初始化并加载
            App.execute(() -> {
                Config config = Config.live();
                App.post(() -> LiveConfig.get().init(config).load());
            });
            ((ConnectivityManager) App.get().getSystemService(Context.CONNECTIVITY_SERVICE)).unregisterNetworkCallback(this);
        }
    }
}
