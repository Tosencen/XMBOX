#!/bin/bash

echo "=== 检查模拟器状态 ==="
adb devices

echo ""
echo "=== 安装APK到模拟器 ==="
adb install -r /Users/chen/Desktop/XMBOX/app/build/outputs/apk/mobileArm64_v8a/debug/3.2.0-mobile-arm64_v8a.apk

echo ""
echo "=== 启动应用 ==="
adb shell am start -n com.fongmi.android.tv/.ui.activity.HomeActivity

echo ""
echo "=== 等待应用启动... ==="
sleep 3

echo ""
echo "=== 查看应用日志 ==="
echo "按 Ctrl+C 停止日志监控"
adb logcat -c
adb logcat | grep -E "custom_spider|Spider|JarLoader|fongmi" --color=always

