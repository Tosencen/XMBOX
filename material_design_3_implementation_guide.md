# Material Design 3 实施指南

本文档提供了如何在实际开发中应用Material Design 3规范的具体指导，包括代码示例和实现方法。

## 如何应用主题色系

### 1. 在styles.xml中定义主题

```xml
<!-- 在res/values/themes.xml中定义应用主题 -->
<style name="Theme.App" parent="Theme.Material3.DayNight.NoActionBar">
    <!-- 主色系 -->
    <item name="colorPrimary">@color/md3_primary</item>
    <item name="colorOnPrimary">@color/md3_on_primary</item>
    <item name="colorPrimaryContainer">@color/md3_primary_container</item>
    <item name="colorOnPrimaryContainer">@color/md3_on_primary_container</item>
    
    <!-- 次要色系 -->
    <item name="colorSecondary">@color/md3_secondary</item>
    <item name="colorOnSecondary">@color/md3_on_secondary</item>
    <item name="colorSecondaryContainer">@color/md3_secondary_container</item>
    <item name="colorOnSecondaryContainer">@color/md3_on_secondary_container</item>
    
    <!-- 表面色系 -->
    <item name="colorSurface">@color/md3_surface</item>
    <item name="colorOnSurface">@color/md3_on_surface</item>
    <item name="colorSurfaceVariant">@color/md3_surface_variant</item>
    <item name="colorOnSurfaceVariant">@color/md3_on_surface_variant</item>
    
    <!-- 其他颜色 -->
    <item name="colorOutline">@color/md3_outline</item>
    <item name="colorError">@color/md3_error</item>
    <item name="colorOnError">@color/md3_on_error</item>
</style>
```

### 2. 在布局中使用主题属性

```xml
<!-- 使用主题属性而非硬编码颜色 -->
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:textColor="?attr/colorOnSurface"
    android:background="?attr/colorSurface" />
```

## 如何创建符合规范的弹窗

### 1. 基本警告/确认弹窗

```kotlin
fun showConfirmationDialog(context: Context, title: String, message: String, 
                          positiveAction: () -> Unit, negativeAction: () -> Unit = {}) {
    MaterialAlertDialogBuilder(context)
        .setTitle(title)
        .setMessage(message)
        .setBackground(ContextCompat.getDrawable(context, R.drawable.bg_dialog_m3))
        .setPositiveButton("确认") { dialog, _ ->
            dialog.dismiss()
            positiveAction()
        }
        .setNegativeButton("取消") { dialog, _ ->
            dialog.dismiss()
            negativeAction()
        }
        .show()
}
```

### 2. 选择弹窗

```kotlin
fun showSelectionDialog(context: Context, title: String, items: Array<String>, 
                       onItemSelected: (Int) -> Unit) {
    MaterialAlertDialogBuilder(context)
        .setTitle(title)
        .setBackground(ContextCompat.getDrawable(context, R.drawable.bg_dialog_m3))
        .setItems(items) { dialog, which ->
            dialog.dismiss()
            onItemSelected(which)
        }
        .setNegativeButton("取消") { dialog, _ ->
            dialog.dismiss()
        }
        .show()
}
```

### 3. 输入弹窗

```kotlin
fun showInputDialog(context: Context, title: String, hint: String, 
                   initialValue: String = "", onConfirm: (String) -> Unit) {
    val inputLayout = TextInputLayout(context).apply {
        boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        boxStrokeColor = context.getColor(R.color.md3_primary)
    }
    
    val editText = TextInputEditText(context).apply {
        hint = hint
        setText(initialValue)
    }
    
    inputLayout.addView(editText)
    
    MaterialAlertDialogBuilder(context)
        .setTitle(title)
        .setBackground(ContextCompat.getDrawable(context, R.drawable.bg_dialog_m3))
        .setView(inputLayout)
        .setPositiveButton("确认") { dialog, _ ->
            dialog.dismiss()
            onConfirm(editText.text.toString())
        }
        .setNegativeButton("取消") { dialog, _ ->
            dialog.dismiss()
        }
        .show()
}
```

### 4. 弹窗背景资源

在`res/drawable/bg_dialog_m3.xml`中定义弹窗背景：

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="?attr/colorSurface" />
    <corners android:radius="16dp" />
</shape>
```

## 如何创建符合规范的按钮

### 1. 主要操作按钮（实心按钮）

在布局XML中：
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btn_primary"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="确认"
    android:textSize="14sp"
    app:cornerRadius="8dp"
    style="@style/mbSolid" />
```

### 2. 次要操作按钮（文本按钮）

```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btn_secondary"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="取消"
    android:textSize="14sp"
    app:cornerRadius="8dp"
    style="@style/Widget.Material3.Button.TextButton" />
```

### 3. 轮廓按钮

```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btn_outline"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="稍后"
    android:textSize="14sp"
    app:cornerRadius="8dp"
    style="@style/mbOutline" />
```

## 如何使用符合规范的图标

### 1. 在布局中使用矢量图标

```xml
<ImageView
    android:layout_width="24dp"
    android:layout_height="24dp"
    android:src="@drawable/ic_home"
    app:tint="?attr/colorOnSurface"
    android:contentDescription="首页" />
```

### 2. 创建状态选择器图标

在`res/color/icon_color_selector.xml`中：

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:color="?attr/colorPrimary" android:state_selected="true" />
    <item android:color="?attr/colorOnSurfaceVariant" />
</selector>
```

在布局中使用：

```xml
<ImageView
    android:layout_width="24dp"
    android:layout_height="24dp"
    android:src="@drawable/ic_home"
    app:tint="@color/icon_color_selector"
    android:state_selected="true"
    android:contentDescription="首页" />
```

### 3. 使用Material Symbols图标

1. 在`build.gradle`中添加依赖：

```gradle
implementation 'com.google.android.material:material:1.9.0'
```

2. 在布局中使用：

```xml
<com.google.android.material.imageview.ShapeableImageView
    android:layout_width="24dp"
    android:layout_height="24dp"
    android:src="@drawable/ic_material_symbol"
    app:tint="?attr/colorOnSurface"
    android:contentDescription="图标描述" />
```

## 常见UI元素实现示例

### 1. 底部导航栏

```xml
<com.google.android.material.bottomnavigation.BottomNavigationView
    android:id="@+id/bottom_navigation"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_gravity="bottom"
    app:menu="@menu/bottom_nav_menu"
    app:itemIconTint="@color/bottom_nav_item_color"
    app:itemTextColor="@color/bottom_nav_item_color"
    app:itemActiveIndicatorStyle="@style/App.BottomNavigation.ActiveIndicator"
    app:labelVisibilityMode="labeled"
    android:background="?attr/colorSurface" />
```

### 2. 搜索栏

```xml
<com.google.android.material.search.SearchBar
    android:id="@+id/search_bar"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="搜索..."
    app:navigationIconTint="?attr/colorOnSurface" />
```

### 3. 卡片

```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="12dp"
    app:cardElevation="2dp"
    app:strokeWidth="1dp"
    app:strokeColor="?attr/colorOutline"
    android:clickable="true"
    android:focusable="true">
    
    <!-- 卡片内容 -->
    
</com.google.android.material.card.MaterialCardView>
```

## 如何调用设计规范

在开发过程中，您可以通过以下方式调用设计规范：

### 1. 在代码中引用规范

```kotlin
/**
 * 创建符合Material Design 3规范的确认弹窗
 * 
 * 规范参考: material_design_3_dialog_icon_specs.md - 弹窗样式规范
 */
fun createConfirmDialog() {
    // 实现代码
}
```

### 2. 使用代码片段模板

在Android Studio中创建Live Templates，将常用的Material Design 3组件代码片段保存为模板，方便快速调用。

### 3. 创建统一的样式工具类

```kotlin
/**
 * Material Design 3样式工具类
 */
object MD3StyleUtils {
    /**
     * 创建符合规范的警告/确认弹窗
     */
    fun createAlertDialog(context: Context, title: String, message: String): MaterialAlertDialogBuilder {
        return MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setBackground(ContextCompat.getDrawable(context, R.drawable.bg_dialog_m3))
    }
    
    /**
     * 设置图标颜色为主题色
     */
    fun setIconPrimaryColor(imageView: ImageView) {
        imageView.setColorFilter(
            imageView.context.getColorStateList(R.color.md3_primary)?.defaultColor ?: 0,
            PorterDuff.Mode.SRC_IN
        )
    }
    
    // 其他工具方法...
}
```

### 4. 使用设计系统组件库

考虑创建一个应用内的设计系统组件库，将所有符合Material Design 3规范的UI组件封装起来，方便在不同页面中复用。

## 检查清单

在实现UI时，可以使用以下检查清单确保符合Material Design 3规范：

- [ ] 使用主题属性而非硬编码颜色值
- [ ] 弹窗使用正确的圆角半径(16dp)
- [ ] 弹窗按钮位于底部且右对齐
- [ ] 图标使用正确的尺寸(24dp × 24dp)
- [ ] 按钮使用正确的样式(实心、文本或轮廓)
- [ ] 文本使用正确的颜色(主要文本使用colorOnSurface，次要文本使用colorOnSurfaceVariant)
- [ ] 所有交互元素有足够的触摸区域(至少48dp × 48dp)
- [ ] 提供内容描述(ContentDescription)以支持屏幕阅读器

## 参考资料

- [Material Design 3 组件](https://m3.material.io/components)
- [Material Design 3 颜色系统](https://m3.material.io/styles/color/system)
- [Material Design 3 排版](https://m3.material.io/styles/typography)
- [Material Symbols 图标](https://fonts.google.com/icons)

---

通过遵循这些实施指南，您可以确保应用的UI元素符合Material Design 3规范，提供一致、专业的用户体验。
