package com.github.tvbox.osc.util;

import android.app.Activity;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.blankj.utilcode.util.ToastUtils;
import com.github.tvbox.osc.R;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 权限管理帮助类，采用最小权限原则
 * 根据实际功能需要，分组请求权限
 */
public class PermissionHelper {

    /**
     * 基本存储权限 - 适用于读写普通文件
     * 在 Android 13 及以上使用 READ_MEDIA_IMAGES 等权限
     * 在 Android 10 及以下使用 READ/WRITE_EXTERNAL_STORAGE
     */
    public static List<String> getBasicStoragePermissions() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            permissions.add(Permission.READ_MEDIA_IMAGES);
            permissions.add(Permission.READ_MEDIA_VIDEO);
        } else {
            permissions.add(Permission.READ_EXTERNAL_STORAGE);
        }
        return permissions;
    }

    /**
     * 获取备份所需权限
     * 备份功能需要 MANAGE_EXTERNAL_STORAGE 权限
     */
    public static List<String> getBackupPermissions() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11+
            permissions.add(Permission.MANAGE_EXTERNAL_STORAGE);
        } else {
            permissions.add(Permission.WRITE_EXTERNAL_STORAGE);
        }
        return permissions;
    }

    /**
     * 请求基础存储权限
     */
    public static void requestBasicStoragePermission(@NonNull Activity activity, OnPermissionCallback callback) {
        List<String> permissions = getBasicStoragePermissions();
        if (XXPermissions.isGranted(activity, permissions)) {
            callback.onGranted(permissions, true);
            return;
        }
        
        requestPermission(activity, permissions, callback, R.string.basic_storage_permission_tip);
    }

    /**
     * 请求备份权限
     */
    public static void requestBackupPermission(@NonNull Activity activity, OnPermissionCallback callback) {
        List<String> permissions = getBackupPermissions();
        if (XXPermissions.isGranted(activity, permissions)) {
            callback.onGranted(permissions, true);
            return;
        }
        
        requestPermission(activity, permissions, callback, R.string.backup_permission_tip);
    }

    /**
     * 基础请求权限方法，附带权限说明对话框
     */
    private static void requestPermission(@NonNull Activity activity, List<String> permissions, 
                                        OnPermissionCallback callback, int messageResId) {
        // 首次申请权限前显示说明对话框
        if (!XXPermissions.isGranted(activity, permissions) && 
            !shouldShowRequestPermissionNow(activity, permissions)) {
            new AlertDialog.Builder(activity)
                .setTitle(R.string.dialog_permission_title)
                .setMessage(messageResId)
                .setPositiveButton(R.string.dialog_permission_confirm, (dialog, which) -> {
                    // 显示说明后申请权限
                    performRequestPermission(activity, permissions, callback);
                })
                .setNegativeButton(R.string.dialog_permission_cancel, (dialog, which) -> {
                    // 用户拒绝权限申请
                    callback.onDenied(permissions, false);
                })
                .setCancelable(false)
                .show();
        } else {
            // 已经请求过权限，或权限已经被拒绝过，直接申请权限
            performRequestPermission(activity, permissions, callback);
        }
    }

    /**
     * 执行权限请求
     */
    private static void performRequestPermission(@NonNull Activity activity, List<String> permissions, 
                                               OnPermissionCallback callback) {
        XXPermissions.with(activity)
            .permission(permissions)
            .request(new OnPermissionCallback() {
                @Override
                public void onGranted(List<String> permissions, boolean all) {
                    if (callback != null) {
                        callback.onGranted(permissions, all);
                    }
                }

                @Override
                public void onDenied(List<String> permissions, boolean never) {
                    if (never) {
                        // 用户选择了"不再询问"，引导用户手动授权
                        MD3ToastUtils.showToast(activity.getString(R.string.permission_denied_forever));
                        XXPermissions.startPermissionActivity(activity, permissions);
                    } else {
                        // 权限被拒绝但未选"不再询问"
                        MD3ToastUtils.showToast(activity.getString(R.string.permission_denied));
                    }
                    
                    if (callback != null) {
                        callback.onDenied(permissions, never);
                    }
                }
            });
    }

    /**
     * 判断是否应该直接显示权限请求，而不是先显示提示对话框
     */
    private static boolean shouldShowRequestPermissionNow(Context context, List<String> permissions) {
        // 任一权限被拒绝过，则直接请求权限
        for (String permission : permissions) {
            if (XXPermissions.isPermanentDenied(context, permission)) {
                return true;
            }
        }
        // 检查是否首次请求
        return false;
    }
} 