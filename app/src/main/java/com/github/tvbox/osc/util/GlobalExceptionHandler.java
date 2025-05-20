package com.github.tvbox.osc.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.DeviceUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局异常捕获处理器
 * 捕获未处理的异常，记录crash信息并优雅地终止应用
 */
public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "GlobalExceptionHandler";
    
    // 单例
    private static GlobalExceptionHandler instance;
    
    // 系统默认的异常处理器
    private Thread.UncaughtExceptionHandler defaultHandler;
    
    // 用于存储设备信息和异常信息
    private final ConcurrentHashMap<String, String> deviceInfoMap = new ConcurrentHashMap<>();
    
    // 应用上下文
    private Context context;
    
    // crash日志保存路径
    private String crashDirPath;

    /**
     * 获取单例
     */
    public static GlobalExceptionHandler getInstance() {
        if (instance == null) {
            synchronized (GlobalExceptionHandler.class) {
                if (instance == null) {
                    instance = new GlobalExceptionHandler();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化
     */
    public void init(Context context) {
        this.context = context;
        // 获取系统默认的异常处理器
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        // 设置为当前线程的异常处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
        // 初始化crash日志保存路径
        crashDirPath = context.getExternalFilesDir(null) + File.separator + "crash";
        File dir = new File(crashDirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // 收集设备信息
        collectDeviceInfo();
    }

    /**
     * 当未捕获的异常发生时会转入该函数处理
     */
    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable ex) {
        try {
            // 将异常信息写入到文件
            handleException(ex);
            LOG.e(TAG, "应用发生未捕获异常，已记录日志：" + ex.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // 如果系统提供了默认异常处理器，则交给系统处理
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, ex);
        } else {
            // 否则结束进程
            Process.killProcess(Process.myPid());
            System.exit(1);
        }
    }

    /**
     * 自定义异常处理
     */
    private void handleException(Throwable ex) {
        if (ex == null) {
            return;
        }
        
        try {
            // 生成日志文件名
            long timestamp = System.currentTimeMillis();
            String time = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date(timestamp));
            String fileName = "crash_" + time + ".log";
            
            // 创建日志文件
            File file = new File(crashDirPath + File.separator + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            
            // 写入崩溃日志
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            pw.println("时间: " + time);
            
            // 写入设备信息
            for (String key : deviceInfoMap.keySet()) {
                pw.println(key + ": " + deviceInfoMap.get(key));
            }
            
            pw.println("\n异常信息: " + ex.getLocalizedMessage());
            pw.println("\n堆栈跟踪:");
            
            // 获取完整的堆栈跟踪
            StringWriter sw = new StringWriter();
            PrintWriter printWriter = new PrintWriter(sw);
            ex.printStackTrace(printWriter);
            Throwable cause = ex.getCause();
            while (cause != null) {
                cause.printStackTrace(printWriter);
                cause = cause.getCause();
            }
            pw.println(sw.toString());
            
            // 关闭资源
            printWriter.close();
            pw.close();
            
            // 删除过期日志（保留最近10个）
            cleanOldCrashLogs();
        } catch (Exception e) {
            Log.e(TAG, "保存异常日志失败：" + e.getMessage());
        }
    }

    /**
     * 收集设备和应用信息
     */
    private void collectDeviceInfo() {
        try {
            // 获取应用信息
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
            
            deviceInfoMap.put("应用版本", pi.versionName);
            deviceInfoMap.put("应用版本号", String.valueOf(pi.versionCode));
            deviceInfoMap.put("Android版本", Build.VERSION.RELEASE);
            deviceInfoMap.put("Android SDK", String.valueOf(Build.VERSION.SDK_INT));
            deviceInfoMap.put("设备型号", Build.MODEL);
            deviceInfoMap.put("设备厂商", Build.MANUFACTURER);
            deviceInfoMap.put("设备ID", DeviceUtils.getUniqueDeviceId());
            deviceInfoMap.put("CPU架构", Build.SUPPORTED_ABIS[0]);
            
        } catch (PackageManager.NameNotFoundException e) {
            LOG.e(TAG, "收集应用信息失败：" + e.getMessage());
        }
    }

    /**
     * 删除旧的崩溃日志，只保留最近的10个
     */
    private void cleanOldCrashLogs() {
        File dir = new File(crashDirPath);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null && files.length > 10) {
                // 按照修改时间排序
                java.util.Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                
                // 删除多余的日志文件
                for (int i = 10; i < files.length; i++) {
                    files[i].delete();
                }
            }
        }
    }
} 