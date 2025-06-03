#!/bin/bash

# 内存泄漏修复验证脚本
# 用于验证LoadSir和其他内存泄漏修复的效果

echo "========== 内存泄漏修复验证脚本 =========="
echo "开始验证内存泄漏修复效果..."

# 检查应用是否在运行
APP_PACKAGE="com.xmbox.app"
PID=$(adb shell ps | grep $APP_PACKAGE | awk '{print $2}')

if [ -z "$PID" ]; then
    echo "应用未运行，启动应用..."
    adb shell am start -n $APP_PACKAGE/com.github.tvbox.osc.ui.activity.SplashActivity
    sleep 3
    PID=$(adb shell ps | grep $APP_PACKAGE | awk '{print $2}')
fi

echo "应用PID: $PID"

# 1. 检查LoadSir修复日志
echo ""
echo "1. 检查LoadSir内存泄漏修复日志..."
echo "最近的LoadSir修复日志:"
adb logcat -d -s "LoadSirLeakFixer" | tail -10

# 2. 检查Activity泄漏检测日志
echo ""
echo "2. 检查Activity泄漏检测日志..."
echo "最近的Activity泄漏检测日志:"
adb logcat -d -s "ActivityLeakDetector" | tail -5

# 3. 检查内存使用情况
echo ""
echo "3. 检查当前内存使用情况..."
if [ ! -z "$PID" ]; then
    # 获取内存信息
    MEMORY_INFO=$(adb shell dumpsys meminfo $PID | grep -E "TOTAL|Native Heap|Dalvik Heap")
    echo "$MEMORY_INFO"
else
    echo "无法获取内存信息，应用未运行"
fi

# 4. 检查Fragment清理日志
echo ""
echo "4. 检查Fragment清理日志..."
echo "最近的Fragment清理日志:"
adb logcat -d -s "BaseLazyFragment" | grep "LoadSir引用已清理" | tail -5

# 5. 模拟页面切换来触发内存泄漏修复
echo ""
echo "5. 模拟页面切换来触发内存泄漏修复..."

# 模拟一些用户操作来触发Fragment的创建和销毁
echo "模拟用户操作..."
adb shell input tap 200 800  # 点击某个位置
sleep 1
adb shell input tap 300 800  # 点击另一个位置
sleep 1
adb shell input keyevent 4   # 返回键
sleep 1

# 6. 检查修复后的日志
echo ""
echo "6. 检查操作后的修复日志..."
echo "新的LoadSir修复日志:"
adb logcat -d -s "LoadSirLeakFixer" | tail -5

# 7. 强制GC并检查内存变化
echo ""
echo "7. 强制垃圾回收并检查内存变化..."
if [ ! -z "$PID" ]; then
    echo "GC前内存:"
    adb shell dumpsys meminfo $PID | grep "TOTAL" | head -1
    
    # 强制GC
    adb shell am force-stop $APP_PACKAGE
    sleep 1
    adb shell am start -n $APP_PACKAGE/com.github.tvbox.osc.ui.activity.SplashActivity
    sleep 3
    
    NEW_PID=$(adb shell ps | grep $APP_PACKAGE | awk '{print $2}')
    echo "重启后新PID: $NEW_PID"
    
    if [ ! -z "$NEW_PID" ]; then
        echo "重启后内存:"
        adb shell dumpsys meminfo $NEW_PID | grep "TOTAL" | head -1
    fi
fi

# 8. 生成内存泄漏报告
echo ""
echo "8. 生成内存泄漏修复报告..."
echo "========== 内存泄漏修复报告 =========="

# 统计修复次数
LOADSIR_FIX_COUNT=$(adb logcat -d -s "LoadSirLeakFixer" | grep "开始修复LoadService内存泄漏" | wc -l)
FRAGMENT_CLEANUP_COUNT=$(adb logcat -d -s "BaseLazyFragment" | grep "LoadSir引用已清理" | wc -l)
ACTIVITY_LEAK_COUNT=$(adb logcat -d -s "ActivityLeakDetector" | grep "检测到Activity内存泄漏" | wc -l)
VIEWPAGER2_FIX_COUNT=$(adb logcat -d -s "ViewPager2LeakFixer" | grep "开始修复ViewPager2内存泄漏" | wc -l)
BOOTSTRAP_INIT_COUNT=$(adb logcat -d -s "MemoryLeakBootstrap" | grep "内存泄漏修复系统初始化完成" | wc -l)

echo "LoadSir修复次数: $LOADSIR_FIX_COUNT"
echo "Fragment清理次数: $FRAGMENT_CLEANUP_COUNT"
echo "Activity泄漏检测次数: $ACTIVITY_LEAK_COUNT"
echo "ViewPager2修复次数: $VIEWPAGER2_FIX_COUNT"
echo "内存泄漏启动器初始化次数: $BOOTSTRAP_INIT_COUNT"

# 检查是否有错误日志
ERROR_COUNT=$(adb logcat -d | grep -E "LoadSirLeakFixer.*失败|ActivityLeakDetector.*失败|BaseLazyFragment.*失败|ViewPager2LeakFixer.*失败|MemoryLeakBootstrap.*失败" | wc -l)
echo "修复过程中的错误次数: $ERROR_COUNT"

if [ $ERROR_COUNT -eq 0 ]; then
    echo "✅ 内存泄漏修复工作正常，未发现错误"
else
    echo "⚠️  修复过程中发现 $ERROR_COUNT 个错误，请检查日志"
    echo "错误详情:"
    adb logcat -d | grep -E "LoadSirLeakFixer.*失败|ActivityLeakDetector.*失败|BaseLazyFragment.*失败" | tail -5
fi

# 9. 检查ViewPager2内存泄漏修复日志
echo ""
echo "9. 检查ViewPager2内存泄漏修复日志..."
echo "最近的ViewPager2修复日志:"
adb logcat -d -s "ViewPager2LeakFixer" | tail -10

# 10. 检查内存泄漏启动器日志
echo ""
echo "10. 检查内存泄漏启动器日志..."
echo "最近的内存泄漏启动器日志:"
adb logcat -d -s "MemoryLeakBootstrap" | tail -10

# 11. 检查LeakCanary报告
echo ""
echo "11. 检查LeakCanary内存泄漏报告..."
LEAKCANARY_LOGS=$(adb logcat -d | grep -i "leakcanary" | tail -5)
if [ ! -z "$LEAKCANARY_LOGS" ]; then
    echo "LeakCanary日志:"
    echo "$LEAKCANARY_LOGS"
else
    echo "未发现LeakCanary相关日志"
fi

echo ""
echo "========== 验证完成 =========="
echo "建议："
echo "1. 观察应用使用过程中的内存使用情况"
echo "2. 检查LeakCanary是否还报告LoadLayout相关的内存泄漏"
echo "3. 如果仍有泄漏，可以查看详细的修复日志进行进一步优化"
echo ""
echo "要实时监控修复过程，可以运行："
echo "adb logcat -s 'LoadSirLeakFixer' 'ActivityLeakDetector' 'BaseLazyFragment' 'ViewPager2LeakFixer' 'MemoryLeakBootstrap'"
echo ""
echo "要手动触发内存清理，可以运行："
echo "adb shell am broadcast -a com.github.tvbox.osc.TRIGGER_MEMORY_CLEANUP"
echo ""
echo "要获取详细内存报告，可以运行："
echo "adb shell am broadcast -a com.github.tvbox.osc.GET_MEMORY_REPORT"
