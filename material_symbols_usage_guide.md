# Material Symbols 图标使用指南

本文档介绍如何在项目中使用Material Symbols图标库。

## 简介

Material Symbols是Google提供的Material Design 3图标库，包含了2500多个图标，可以通过字体的方式使用。本项目已经集成了Material Symbols Rounded字体，可以直接在布局或代码中使用。

## 在XML布局中使用

### 方法1：使用字符串资源

```xml
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="@string/ms_home"
    style="@style/Widget.App.MaterialSymbols" />
```

### 方法2：直接使用Unicode

```xml
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="&#xe88a;"  <!-- 这是HOME图标的Unicode -->
    style="@style/Widget.App.MaterialSymbols" />
```

## 在代码中使用

### 方法1：使用MaterialSymbols常量

```kotlin
import com.github.tvbox.osc.util.MaterialSymbols
import com.github.tvbox.osc.util.MaterialSymbolsLoader

// 设置图标
val textView = findViewById<TextView>(R.id.iconTextView)
MaterialSymbolsLoader.setIcon(textView, MaterialSymbols.HOME)
```

### 方法2：使用字符串资源

```kotlin
import com.github.tvbox.osc.util.MaterialSymbolsLoader

// 设置图标
val textView = findViewById<TextView>(R.id.iconTextView)
MaterialSymbolsLoader.apply(textView)
textView.setText(R.string.ms_home)
```

## 可用样式

项目中提供了以下几种样式：

1. `Widget.App.MaterialSymbols` - 标准尺寸(24sp)
2. `Widget.App.MaterialSymbols.Small` - 小尺寸(20sp)
3. `Widget.App.MaterialSymbols.Large` - 大尺寸(32sp)
4. `Widget.App.MaterialSymbols.Primary` - 主题色

你也可以自定义样式：

```xml
<style name="Widget.App.MaterialSymbols.Custom" parent="Widget.App.MaterialSymbols">
    <item name="android:textSize">28sp</item>
    <item name="android:textColor">#FF5722</item>
</style>
```

## 可用图标

所有可用的图标都定义在`MaterialSymbols`类中，按照功能分类：

- 导航图标：HOME, SEARCH, SETTINGS, PERSON, FAVORITE, HISTORY
- 媒体控制图标：PLAY_ARROW, PAUSE, STOP, SKIP_NEXT, SKIP_PREVIOUS, VOLUME_UP等
- 操作图标：ADD, REMOVE, DELETE, EDIT, SHARE, DOWNLOAD等
- 界面元素图标：MENU, MORE_VERT, ARROW_BACK, ARROW_FORWARD等
- 特殊功能图标：PICTURE_IN_PICTURE, CAST, FULLSCREEN, SUBTITLES等

同时，这些图标也以字符串资源的形式提供，命名格式为`ms_图标名称`，如`ms_home`、`ms_search`等。

## 图标查找

如果需要查找更多图标，可以访问Material Symbols官方网站：
https://fonts.google.com/icons

在网站上找到需要的图标后，可以：

1. 查看图标的名称，转换为驼峰命名法后在`MaterialSymbols`类中查找
2. 查看图标的Unicode值，可以直接在XML中使用`&#eXXX;`格式

## 注意事项

1. Material Symbols字体需要在应用启动时初始化，这已经在`App.java`中完成
2. 如果图标显示不正确，可能是字体加载失败，请检查日志
3. 在深色模式和浅色模式下，图标颜色会自动适应主题
4. 图标大小可以通过`textSize`属性调整
5. 图标颜色可以通过`textColor`属性调整

## 示例

查看`material_symbols_demo.xml`和`MaterialSymbolsDemoActivity.kt`了解更多使用示例。
