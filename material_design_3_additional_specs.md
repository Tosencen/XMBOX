# Material Design 3 补充规范

本文档补充了Material Design 3的其他重要设计规范，包括排版系统、动画与过渡、空状态设计、列表与网格、以及无障碍设计等方面。

## 排版系统规范

Material Design 3推荐使用一个具有13个不同样式的排版系统，以创建清晰的视觉层次结构。

### 字体家族

推荐使用Roboto字体作为默认字体，或者选择一个符合品牌特性的自定义字体。

### 排版比例

| 样式名称 | 字重 | 字号 | 行高 | 字母间距 |
|---------|------|------|------|---------|
| Display Large | Regular | 57sp | 64sp | -0.25px |
| Display Medium | Regular | 45sp | 52sp | 0px |
| Display Small | Regular | 36sp | 44sp | 0px |
| Headline Large | Regular | 32sp | 40sp | 0px |
| Headline Medium | Regular | 28sp | 36sp | 0px |
| Headline Small | Regular | 24sp | 32sp | 0px |
| Title Large | Regular | 22sp | 28sp | 0px |
| Title Medium | Medium | 16sp | 24sp | 0.15px |
| Title Small | Medium | 14sp | 20sp | 0.1px |
| Body Large | Regular | 16sp | 24sp | 0.5px |
| Body Medium | Regular | 14sp | 20sp | 0.25px |
| Body Small | Regular | 12sp | 16sp | 0.4px |
| Label | Medium | 12sp | 16sp | 0.5px |

### 在XML中定义排版样式

```xml
<style name="TextAppearance.App.DisplayLarge" parent="TextAppearance.Material3.DisplayLarge">
    <item name="fontFamily">@font/roboto</item>
    <item name="android:fontFamily">@font/roboto</item>
    <item name="android:textSize">57sp</item>
    <item name="android:lineHeight">64sp</item>
    <item name="android:letterSpacing">-0.0044</item>
    <item name="android:textColor">?attr/colorOnSurface</item>
</style>

<!-- 其他排版样式类似定义 -->
```

### 文本颜色规范

| 文本类型 | 颜色 | 透明度 |
|---------|------|--------|
| 主要文本 | `?attr/colorOnSurface` | 100% |
| 次要文本 | `?attr/colorOnSurfaceVariant` | 100% |
| 提示文本 | `?attr/colorOnSurfaceVariant` | 100% |
| 禁用文本 | `?attr/colorOnSurface` | 38% |
| 链接文本 | `?attr/colorPrimary` | 100% |
| 错误文本 | `?attr/colorError` | 100% |

## 动画与过渡规范

### 动画持续时间

| 动画类型 | 持续时间 |
|---------|---------|
| 标准过渡 | 300ms |
| 进入动画 | 225ms |
| 退出动画 | 195ms |
| 强调动画 | 500ms |
| 微交互 | 100ms |

### 动画曲线

| 动画类型 | 曲线 |
|---------|------|
| 标准曲线 | Fast Out, Slow In |
| 减速曲线 | Linear Out, Slow In |
| 加速曲线 | Fast Out, Linear In |
| 强调曲线 | Fast Out, Slow In, Fast Out |

### 常见UI元素动画规范

#### 1. 按钮点击反馈

- **涟漪效果**：从点击点向外扩散
- **持续时间**：300ms
- **颜色**：`?attr/colorOnPrimary`的20%透明度（对于主要按钮）

#### 2. 弹窗显示/隐藏

- **显示**：淡入+缩放（从95%到100%）
- **隐藏**：淡出+缩放（从100%到95%）
- **持续时间**：显示225ms，隐藏195ms

#### 3. 页面切换

- **进入**：从右侧滑入
- **退出**：向左侧滑出
- **持续时间**：300ms

#### 4. 列表项动画

- **添加**：淡入+从下向上移动
- **删除**：淡出+向上收缩
- **持续时间**：300ms
- **错开时间**：相邻项目间隔20ms

## 空状态设计规范

空状态是指当页面或容器中没有内容显示时的界面状态。设计良好的空状态可以提高用户体验，减少用户困惑。

### 空状态组成元素

1. **插图**：视觉表达当前状态
2. **标题**：简洁地描述状态
3. **副标题**（可选）：提供额外信息或指导
4. **操作按钮**（可选）：提供解决方案

### 空状态插图规范

- **尺寸**：建议宽度为200dp-250dp
- **样式**：符合应用整体设计语言，使用应用主题色
- **位置**：通常居中显示，距离顶部约1/3处

### 空状态文本规范

- **标题**：
  - 字体：Title Large (22sp)
  - 颜色：`?attr/colorOnSurface`
  - 简洁明了，不超过20个字符
  
- **副标题**：
  - 字体：Body Medium (14sp)
  - 颜色：`?attr/colorOnSurfaceVariant`
  - 提供有用的上下文，不超过60个字符

### 空状态按钮规范

- **类型**：通常使用文本按钮或轮廓按钮
- **位置**：居中，位于副标题下方
- **文本**：清晰表达操作，如"添加收藏"、"开始搜索"

### 常见空状态场景

1. **首次使用**：引导用户开始使用功能
2. **搜索无结果**：提示用户尝试其他搜索词
3. **筛选无结果**：建议用户修改筛选条件
4. **网络错误**：提示网络问题并提供重试选项
5. **权限缺失**：解释需要的权限并提供授权入口

## 列表与网格规范

### 列表项规范

#### 1. 单行列表项

- **高度**：56dp
- **左边距**：16dp
- **右边距**：16dp
- **图标尺寸**（如有）：24dp × 24dp
- **图标与文本间距**：16dp
- **文本样式**：Body Large (16sp)

#### 2. 双行列表项

- **高度**：72dp
- **左边距**：16dp
- **右边距**：16dp
- **主要文本样式**：Body Large (16sp)
- **次要文本样式**：Body Medium (14sp)
- **文本间距**：4dp

#### 3. 三行列表项

- **高度**：88dp
- **文本行间距**：4dp

### 网格项规范

#### 1. 标准网格项

- **宽高比**：16:9（图片）或1:1（正方形）
- **边距**：8dp
- **圆角**：12dp
- **标题位置**：图片下方
- **标题样式**：Body Large (16sp)
- **副标题样式**（如有）：Body Medium (14sp)

#### 2. 瀑布流网格

- **列间距**：8dp
- **行间距**：8dp
- **内容边距**：16dp

### 分隔线规范

- **颜色**：`?attr/colorOutlineVariant`
- **高度**：1dp
- **左边距**（带图标的列表）：56dp（图标宽度+间距）
- **右边距**：0dp

### 列表与网格交互规范

- **点击反馈**：涟漪效果，从点击点向外扩散
- **长按反馈**：背景色变为`?attr/colorPrimaryContainer`
- **滑动操作**（如删除）：显示操作按钮，背景色为对应操作的语义色

## 无障碍设计规范

### 颜色对比度

- **文本对比度**：
  - 小文本（<18sp）：至少4.5:1
  - 大文本（≥18sp）：至少3:1
- **UI组件对比度**：至少3:1

### 触摸目标尺寸

- **最小尺寸**：48dp × 48dp
- **最小间距**：8dp

### 内容描述

为所有非装饰性元素提供内容描述（ContentDescription）：

```xml
<ImageView
    android:layout_width="24dp"
    android:layout_height="24dp"
    android:src="@drawable/ic_search"
    android:contentDescription="搜索" />
```

### 焦点导航

确保所有交互元素可以通过键盘或方向键导航：

```xml
<Button
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="确认"
    android:focusable="true"
    android:nextFocusDown="@id/cancel_button" />
```

### 文本缩放

确保布局能够适应文本缩放（最多200%）：

- 使用`wrap_content`而非固定高度
- 避免在文本容器中使用`android:maxLines="1"`
- 测试不同文本大小下的布局表现

## 响应式布局规范

### 断点与网格系统

| 断点名称 | 宽度范围 | 列数 | 边距 | 槽宽 |
|---------|---------|------|------|------|
| 紧凑型 | 0-599dp | 4 | 16dp | 16dp |
| 中等型 | 600-839dp | 8 | 32dp | 16dp |
| 扩展型 | 840dp+ | 12 | 32dp | 24dp |

### 响应式布局策略

1. **重排**：根据屏幕宽度重新排列元素
2. **展开/折叠**：在较大屏幕上展开更多内容
3. **显示/隐藏**：在不同尺寸屏幕上显示或隐藏某些元素
4. **替换**：用更适合当前屏幕尺寸的组件替换现有组件

### 常见响应式模式

#### 1. 列表/详情模式

- **紧凑型**：列表和详情使用不同页面
- **中等型/扩展型**：列表和详情并排显示

#### 2. 导航模式

- **紧凑型**：底部导航栏或抽屉式导航
- **中等型/扩展型**：侧边导航栏常驻显示

#### 3. 网格布局

- **紧凑型**：2列网格
- **中等型**：4-6列网格
- **扩展型**：6-8列网格

## 深色模式适配规范

### 深色模式颜色映射

| 日间模式 | 夜间模式 |
|---------|---------|
| 浅色背景 | 深色背景 |
| 深色文本 | 浅色文本 |
| 高饱和度强调色 | 低饱和度强调色 |

### 深色模式设计原则

1. **减少大面积白色**：避免在深色模式下使用大面积高亮度颜色
2. **降低对比度**：深色模式下适当降低元素间对比度，减少视觉疲劳
3. **调整阴影**：深色模式下减少或移除阴影效果
4. **保持品牌识别**：确保主题色在深色模式下仍能体现品牌特性

### 深色模式实现

使用夜间资源限定符创建深色模式专用资源：

```
res/
  values/
    colors.xml       # 日间模式颜色
  values-night/
    colors.xml       # 夜间模式颜色
```

## 参考资料

- [Material Design 3 排版](https://m3.material.io/styles/typography)
- [Material Design 3 动画](https://m3.material.io/styles/motion/overview)
- [Material Design 3 组件](https://m3.material.io/components)
- [Android 无障碍设计](https://developer.android.com/guide/topics/ui/accessibility)
- [Material Design 3 响应式布局](https://m3.material.io/foundations/adaptive-design/overview)

---

通过遵循这些补充规范，您可以进一步完善应用的用户体验，确保应用在各种设备和使用场景下都能提供一致、专业、无障碍的界面。
