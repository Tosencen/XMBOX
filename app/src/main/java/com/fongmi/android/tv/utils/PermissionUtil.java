package com.fongmi.android.tv.utils;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.impl.PermissionCallback;
import com.permissionx.guolindev.PermissionX;

import java.util.function.Consumer;

public class PermissionUtil {

    private static final int REQUEST_MANAGE_STORAGE = 1001;

    public static void requestAudio(FragmentActivity activity, Consumer<Boolean> callback) {
        PermissionX.init(activity).permissions(Manifest.permission.RECORD_AUDIO).request(new PermissionCallback(callback));
    }

    public static void requestFile(FragmentActivity activity, Consumer<Boolean> callback) {
        if (Build.VERSION.SDK_INT >= 33) {
            requestManageStorage(activity, callback);
        } else {
            PermissionX.init(activity).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request(new PermissionCallback(callback));
        }
    }

    public static void requestFile(Fragment fragment, Consumer<Boolean> callback) {
        if (Build.VERSION.SDK_INT >= 33) {
            FragmentActivity activity = fragment.getActivity();
            if (activity != null) {
                requestManageStorage(activity, callback);
            }
        } else {
            PermissionX.init(fragment).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request(new PermissionCallback(callback));
        }
    }

    public static void requestManageStorage(FragmentActivity activity, Consumer<Boolean> callback) {
        if (hasManageStorage()) {
            if (callback != null) callback.accept(true);
            return;
        }
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        intent.setData(Uri.parse("package:" + App.get().getPackageName()));
        activity.startActivityForResult(intent, REQUEST_MANAGE_STORAGE);
        if (callback != null) callback.accept(false);
    }

    public static boolean hasManageStorage() {
        if (Build.VERSION.SDK_INT < 30) return true;
        return android.os.Environment.isExternalStorageManager();
    }

    public static boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return true;
        return androidx.core.content.ContextCompat.checkSelfPermission(App.get(), Manifest.permission.POST_NOTIFICATIONS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    public static void requestNotification(FragmentActivity activity, Consumer<Boolean> callback) {
        PermissionX.init(activity).permissions(Manifest.permission.POST_NOTIFICATIONS).request(new PermissionCallback(callback));
    }
}
