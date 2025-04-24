# Material Design 3 弹窗、按钮与图标规范

本文档定义了应用程序中弹窗、按钮和图标的Material Design 3设计规范，以确保在日间和夜间模式下都能提供一致的用户体验。

## 弹窗样式规范

### 基本原则

1. **简洁性**：弹窗应该简洁明了，只包含必要的元素
2. **一致性**：所有弹窗应遵循相同的视觉语言
3. **层次感**：使用适当的阴影和高度来表达层次关系
4. **响应性**：弹窗应该对用户操作提供清晰的反馈

### 弹窗类型与规范

#### 1. 警告/确认弹窗

- **背景色**：`?attr/colorSurface`
- **圆角半径**：16dp
- **内边距**：
  - 顶部：24dp
  - 左右：24dp
  - 底部：16dp
- **标题**：
  - 字体：Roboto Medium
  - 大小：20sp
  - 颜色：`?attr/colorOnSurface`
  - 底部间距：16dp
- **内容**：
  - 字体：Roboto Regular
  - 大小：16sp
  - 颜色：`?attr/colorOnSurfaceVariant`
  - 底部间距：24dp
- **按钮区域**：
  - 对齐方式：右对齐
  - 按钮间距：8dp
  - 上方间距：8dp

#### 2. 选择弹窗

- **背景色**：`?attr/colorSurface`
- **圆角半径**：16dp
- **内边距**：
  - 顶部：24dp
  - 左右：24dp
  - 底部：16dp
- **标题**：
  - 字体：Roboto Medium
  - 大小：20sp
  - 颜色：`?attr/colorOnSurface`
  - 底部间距：16dp
- **选项列表**：
  - 选项高度：48dp
  - 选项文本颜色：`?attr/colorOnSurface`
  - 选项文本大小：16sp
  - 选中状态颜色：`?attr/colorPrimary`
- **按钮区域**：
  - 对齐方式：右对齐
  - 按钮间距：8dp
  - 上方间距：8dp

#### 3. 输入弹窗

- **背景色**：`?attr/colorSurface`
- **圆角半径**：16dp
- **内边距**：
  - 顶部：24dp
  - 左右：24dp
  - 底部：16dp
- **标题**：
  - 字体：Roboto Medium
  - 大小：20sp
  - 颜色：`?attr/colorOnSurface`
  - 底部间距：16dp
- **输入框**：
  - 边框颜色：`?attr/colorOutline`
  - 激活状态边框颜色：`?attr/colorPrimary`
  - 内部填充：16dp
  - 文本颜色：`?attr/colorOnSurface`
  - 提示文本颜色：`?attr/colorOnSurfaceVariant`
- **按钮区域**：
  - 对齐方式：右对齐
  - 按钮间距：8dp
  - 上方间距：16dp

### 弹窗行为规范

1. **避免右上角关闭按钮**：Material Design 3推荐使用底部按钮来关闭弹窗，而非右上角的"X"按钮
2. **动画过渡**：弹窗应使用淡入淡出动画，持续时间为150-200ms
3. **背景暗化**：弹窗显示时，背景应该半透明暗化（黑色，透明度50-60%）
4. **触摸行为**：
   - 点击弹窗外部应关闭弹窗（对于非关键操作）
   - 关键操作弹窗应要求用户明确选择
5. **键盘导航**：支持键盘导航，确保可通过Tab键在各元素间切换

## 弹窗按钮规范

### 按钮布局

1. **位置**：按钮应位于弹窗底部
2. **对齐**：按钮应右对齐
3. **顺序**：
   - 取消/次要操作按钮应位于左侧
   - 确认/主要操作按钮应位于右侧
4. **数量**：一个弹窗通常不应超过3个按钮

### 按钮样式

#### 1. 主要操作按钮

- **类型**：实心按钮（Filled Button）
- **背景色**：`?attr/colorPrimary`
- **文本颜色**：`?attr/colorOnPrimary`
- **内边距**：水平16dp，垂直8dp
- **圆角半径**：8dp
- **文本大小**：14sp
- **文本样式**：Medium
- **状态变化**：
  - 按下：背景色加深10%
  - 禁用：透明度降低到30%

#### 2. 次要操作按钮

- **类型**：文本按钮（Text Button）
- **背景色**：透明
- **文本颜色**：`?attr/colorPrimary`
- **内边距**：水平16dp，垂直8dp
- **圆角半径**：8dp
- **文本大小**：14sp
- **文本样式**：Medium
- **状态变化**：
  - 按下：背景色变为`?attr/colorPrimary`的12%透明度
  - 禁用：文本颜色变为`?attr/colorOnSurface`的38%透明度

#### 3. 轮廓按钮（用于特定场景）

- **类型**：轮廓按钮（Outlined Button）
- **边框颜色**：`?attr/colorPrimary`
- **边框宽度**：1dp
- **背景色**：透明
- **文本颜色**：`?attr/colorPrimary`
- **内边距**：水平16dp，垂直8dp
- **圆角半径**：8dp
- **文本大小**：14sp
- **文本样式**：Medium
- **状态变化**：
  - 按下：背景色变为`?attr/colorPrimaryContainer`
  - 禁用：边框和文本颜色变为`?attr/colorOnSurface`的38%透明度

### 按钮文本规范

1. **简洁明了**：按钮文本应简短明确，通常为1-2个词
2. **动词优先**：使用动词开头，如"保存"、"删除"、"确认"
3. **一致性**：相似操作应使用相同的文本
4. **大小写**：首字母大写（英文），不使用全大写
5. **避免模糊**：避免使用"是"、"否"等模糊词语，应使用具体操作描述

## 图标规范

### 图标尺寸

| 用途 | 尺寸 |
|------|------|
| 导航栏图标 | 24dp × 24dp |
| 工具栏图标 | 24dp × 24dp |
| 列表项图标 | 24dp × 24dp |
| 小型操作图标 | 20dp × 20dp |
| 对话框图标 | 24dp × 24dp |
| 通知图标 | 24dp × 24dp |
| 占位图标 | 100dp × 100dp |

### 图标样式

1. **线条粗细**：2dp
2. **圆角半径**：2dp
3. **内边距**：图标应在其边界内留有2dp的内边距
4. **一致性**：所有图标应遵循相同的视觉语言
5. **简洁性**：避免过于复杂的细节

### 图标颜色

| 图标类型 | 日间模式 | 夜间模式 |
|---------|---------|---------|
| 主要图标 | `?attr/colorOnSurface` | `?attr/colorOnSurface` |
| 次要图标 | `?attr/colorOnSurfaceVariant` | `?attr/colorOnSurfaceVariant` |
| 强调图标 | `?attr/colorPrimary` | `?attr/colorPrimary` |
| 禁用图标 | `?attr/colorOnSurface` (38%透明度) | `?attr/colorOnSurface` (38%透明度) |
| 错误图标 | `?attr/colorError` | `?attr/colorError` |

### 图标状态

#### 1. 选中状态

- **颜色**：`?attr/colorPrimary`
- **动画**：从未选中状态过渡时应有150ms的颜色过渡动画

#### 2. 未选中状态

- **颜色**：`?attr/colorOnSurfaceVariant`
- **动画**：从选中状态过渡时应有150ms的颜色过渡动画

#### 3. 激活状态

- **颜色**：`?attr/colorPrimary`
- **背景**：可选添加`?attr/colorPrimaryContainer`的圆形背景

#### 4. 禁用状态

- **颜色**：`?attr/colorOnSurface`的38%透明度
- **无交互**：不响应点击事件

### 图标资源规范

1. **矢量优先**：优先使用矢量图标（VectorDrawable）以确保在各种屏幕密度下的清晰度
2. **命名规范**：
   - 前缀：`ic_`
   - 功能描述：如`home`、`search`、`settings`
   - 状态（可选）：如`_selected`、`_disabled`
   - 示例：`ic_home_selected.xml`
3. **资源位置**：
   - 矢量图标：`res/drawable/`
   - 位图图标（如需）：相应密度文件夹如`res/drawable-xxhdpi/`

### Material Design 3官方图标

推荐使用Material Design 3官方图标库中的图标，可从以下位置获取：
- [Material Design 3图标库](https://m3.material.io/styles/icons/overview)
- [Material Symbols](https://fonts.google.com/icons)

## 实现最佳实践

1. **使用Material组件**
   - 使用`MaterialAlertDialogBuilder`创建符合Material Design 3规范的对话框
   - 使用`MaterialButton`实现按钮样式

2. **主题属性而非硬编码值**
   - 使用`?attr/colorPrimary`而非硬编码颜色值
   - 使用尺寸资源而非硬编码尺寸

3. **响应式设计**
   - 确保弹窗在不同屏幕尺寸下都能正确显示
   - 弹窗宽度应为：
     - 小屏幕（<600dp）：屏幕宽度减去48dp
     - 大屏幕（≥600dp）：560dp

4. **无障碍设计**
   - 确保所有交互元素有足够的触摸区域（至少48dp×48dp）
   - 提供内容描述（ContentDescription）以支持屏幕阅读器
   - 确保文本和背景之间有足够的对比度

## 参考资料

- [Material Design 3 对话框](https://m3.material.io/components/dialogs/overview)
- [Material Design 3 按钮](https://m3.material.io/components/buttons/overview)
- [Material Design 3 图标](https://m3.material.io/styles/icons/overview)
- [Material Symbols](https://fonts.google.com/icons)

---

通过遵循这些指南，我们可以确保应用中的弹窗、按钮和图标在各种设备和环境下都能提供一致、专业的用户体验，同时保持品牌识别度和可访问性。
