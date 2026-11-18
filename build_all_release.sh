#!/bin/bash

VERSION="3.1.1"
DESKTOP_PATH="$HOME/Desktop"
PROJECT_PATH="/Users/chen/Desktop/XMBOX-3.1.0"

cd "$PROJECT_PATH"

echo "========================================="
echo "  构建 XMBOX 所有 Release 包 (v${VERSION})"
echo "========================================="
echo ""

echo "=== 1. 清理旧的构建文件 ==="
./gradlew clean

echo ""
echo "=== 2. 构建所有 Release APK ==="
./gradlew assembleMobileArm64_v8aRelease \
         assembleMobileArmeabi_v7aRelease \
         assembleLeanbackArm64_v8aRelease \
         assembleLeanbackArmeabi_v7aRelease

echo ""
echo "=== 3. 复制 APK 到桌面 ==="

# 定义APK路径和输出文件名
declare -a APKS=(
    "app/build/outputs/apk/mobileArm64_v8a/release/${VERSION}-mobile-arm64_v8a.apk|${VERSION}-mobile-arm64_v8a.apk"
    "app/build/outputs/apk/mobileArmeabi_v7a/release/${VERSION}-mobile-armeabi_v7a.apk|${VERSION}-mobile-armeabi_v7a.apk"
    "app/build/outputs/apk/leanbackArm64_v8a/release/${VERSION}-leanback-arm64_v8a.apk|${VERSION}-leanback-arm64_v8a.apk"
    "app/build/outputs/apk/leanbackArmeabi_v7a/release/${VERSION}-leanback-armeabi_v7a.apk|${VERSION}-leanback-armeabi_v7a.apk"
)

SUCCESS_COUNT=0
FAIL_COUNT=0

for apk_info in "${APKS[@]}"; do
    IFS='|' read -r source_path target_name <<< "$apk_info"
    
    if [ -f "$source_path" ]; then
        cp "$source_path" "$DESKTOP_PATH/$target_name"
        if [ $? -eq 0 ]; then
            echo "✅ $target_name"
            ls -lh "$DESKTOP_PATH/$target_name" | awk '{print "   大小: " $5}'
            SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
        else
            echo "❌ 复制失败: $target_name"
            FAIL_COUNT=$((FAIL_COUNT + 1))
        fi
    else
        echo "❌ 文件不存在: $source_path"
        FAIL_COUNT=$((FAIL_COUNT + 1))
    fi
done

echo ""
echo "========================================="
if [ $FAIL_COUNT -eq 0 ]; then
    echo "✅ 所有 APK 构建并复制成功！"
    echo "   成功: $SUCCESS_COUNT 个"
    echo "   位置: $DESKTOP_PATH"
else
    echo "⚠️  构建完成，但有 $FAIL_COUNT 个失败"
    echo "   成功: $SUCCESS_COUNT 个"
    echo "   失败: $FAIL_COUNT 个"
fi
echo "========================================="

