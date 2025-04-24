package com.github.tvbox.osc.cast.dlna;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * DLNA服务类 - 已完全禁用以解决构建问题
 */
public class DLNAService extends Service {
    private static final String TAG = "DLNAService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "DLNA service created - but all functionality disabled");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "DLNA service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // 所有投屏功能已禁用
        Log.d(TAG, "DLNA service bind attempt - all functionality disabled");
        return null;
    }
}
