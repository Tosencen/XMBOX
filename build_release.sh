#!/bin/bash

echo "========================================="
echo "  构建 XMBOX Mobile ARM64-V8A Release  "
echo "========================================="
echo ""

cd /Users/chen/Desktop/XMBOX

echo "=== 1. 清理旧的构建文件 ==="
./gradlew clean

echo ""
echo "=== 2. 构建 Release APK ==="
./gradlew assembleMobileArm64_v8aRelease

echo ""
echo "=== 3. 验证构建结果 ==="
if [ -f "app/build/outputs/apk/mobileArm64_v8a/release/3.2.0-mobile-arm64_v8a.apk" ]; then
    echo "✅ Release APK 构建成功！"
    echo ""
    echo "文件信息："
    ls -lh app/build/outputs/apk/mobileArm64_v8a/release/3.2.0-mobile-arm64_v8a.apk
    echo ""
    echo "APK详细信息："
    echo "---"
    unzip -l app/build/outputs/apk/mobileArm64_v8a/release/3.2.0-mobile-arm64_v8a.apk | grep "assets/jar"
    echo ""
    echo "签名信息："
    jarsigner -verify -verbose -certs app/build/outputs/apk/mobileArm64_v8a/release/3.2.0-mobile-arm64_v8a.apk | grep -A 3 "Signed by"
    echo ""
    echo "=== 构建完成！ ==="
    echo "APK路径: app/build/outputs/apk/mobileArm64_v8a/release/3.2.0-mobile-arm64_v8a.apk"
else
    echo "❌ 构建失败！"
    exit 1
fi

