# Material Design 3 设计规范

本文档定义了应用程序的Material Design 3设计规范，包括色彩系统、弹窗样式、按钮规范和图标设计，以确保在日间和夜间模式下都能提供一致的用户体验。

## 色彩角色定义

Material Design 3定义了一系列语义化的色彩角色，每个角色在UI中有特定的用途：

| 色彩角色 | 用途 |
|---------|------|
| Primary | 主要品牌色，用于浮动操作按钮、主要按钮等关键UI元素 |
| On Primary | 在Primary背景上的内容（文本、图标）颜色 |
| Primary Container | Primary的容器变体，用于次要UI元素如芯片、辅助按钮 |
| On Primary Container | 在Primary Container背景上的内容颜色 |
| Secondary | 次要品牌色，用于不太突出的UI元素 |
| On Secondary | 在Secondary背景上的内容颜色 |
| Secondary Container | Secondary的容器变体 |
| On Secondary Container | 在Secondary Container背景上的内容颜色 |
| Tertiary | 第三级品牌色，用于平衡Primary和Secondary |
| Surface | 组件表面的背景色，如卡片、表格 |
| On Surface | 在Surface背景上的内容颜色 |
| Surface Variant | Surface的变体，用于区分不同层次的表面 |
| On Surface Variant | 在Surface Variant背景上的内容颜色 |
| Outline | 用于边界和分隔线 |
| Error | 错误状态的颜色 |

## 青色系列色彩方案

### 日间模式色彩方案

```xml
<!-- 主色系 -->
<color name="md3_primary">#006A6A</color>
<color name="md3_on_primary">#FFFFFF</color>
<color name="md3_primary_container">#83F4F4</color>
<color name="md3_on_primary_container">#002020</color>

<!-- 次要色系 -->
<color name="md3_secondary">#4A6363</color>
<color name="md3_on_secondary">#FFFFFF</color>
<color name="md3_secondary_container">#CDE7E7</color>
<color name="md3_on_secondary_container">#051F1F</color>

<!-- 第三色系 -->
<color name="md3_tertiary">#4B607C</color>
<color name="md3_on_tertiary">#FFFFFF</color>
<color name="md3_tertiary_container">#D3E4FF</color>
<color name="md3_on_tertiary_container">#041C36</color>

<!-- 错误色系 -->
<color name="md3_error">#BA1A1A</color>
<color name="md3_on_error">#FFFFFF</color>
<color name="md3_error_container">#FFDAD6</color>
<color name="md3_on_error_container">#410002</color>

<!-- 背景和表面色系 -->
<color name="md3_background">#FAFDFD</color>
<color name="md3_on_background">#191C1C</color>
<color name="md3_surface">#FAFDFD</color>
<color name="md3_on_surface">#191C1C</color>
<color name="md3_surface_variant">#DAE4E4</color>
<color name="md3_on_surface_variant">#3F4949</color>

<!-- 轮廓色 -->
<color name="md3_outline">#6F7979</color>
<color name="md3_outline_variant">#BFC8C8</color>

<!-- 表面色层次 -->
<color name="md3_surface_container_lowest">#F0F3F3</color>
<color name="md3_surface_container_low">#E6E9E9</color>
<color name="md3_surface_container">#DCDEDE</color>
<color name="md3_surface_container_high">#D2D4D4</color>
<color name="md3_surface_container_highest">#C8CACA</color>

<!-- 反转色系 -->
<color name="md3_inverse_surface">#2D3131</color>
<color name="md3_inverse_on_surface">#EFF1F1</color>
<color name="md3_inverse_primary">#64D8D8</color>
```

### 夜间模式色彩方案

```xml
<!-- 主色系 -->
<color name="md3_primary">#64D8D8</color>
<color name="md3_on_primary">#003737</color>
<color name="md3_primary_container">#004F4F</color>
<color name="md3_on_primary_container">#83F4F4</color>

<!-- 次要色系 -->
<color name="md3_secondary">#B1CBCB</color>
<color name="md3_on_secondary">#1C3434</color>
<color name="md3_secondary_container">#334B4B</color>
<color name="md3_on_secondary_container">#CDE7E7</color>

<!-- 第三色系 -->
<color name="md3_tertiary">#B5C8E8</color>
<color name="md3_on_tertiary">#1C314D</color>
<color name="md3_tertiary_container">#334764</color>
<color name="md3_on_tertiary_container">#D3E4FF</color>

<!-- 错误色系 -->
<color name="md3_error">#FFB4AB</color>
<color name="md3_on_error">#690005</color>
<color name="md3_error_container">#93000A</color>
<color name="md3_on_error_container">#FFDAD6</color>

<!-- 背景和表面色系 -->
<color name="md3_background">#191C1C</color>
<color name="md3_on_background">#E0E3E3</color>
<color name="md3_surface">#191C1C</color>
<color name="md3_on_surface">#E0E3E3</color>
<color name="md3_surface_variant">#3F4949</color>
<color name="md3_on_surface_variant">#BFC8C8</color>

<!-- 轮廓色 -->
<color name="md3_outline">#899393</color>
<color name="md3_outline_variant">#3F4949</color>

<!-- 表面色层次 -->
<color name="md3_surface_container_lowest">#0F1212</color>
<color name="md3_surface_container_low">#191C1C</color>
<color name="md3_surface_container">#1D2020</color>
<color name="md3_surface_container_high">#232626</color>
<color name="md3_surface_container_highest">#292C2C</color>

<!-- 反转色系 -->
<color name="md3_inverse_surface">#E0E3E3</color>
<color name="md3_inverse_on_surface">#2D3131</color>
<color name="md3_inverse_primary">#006A6A</color>
```

## UI元素色彩应用指南

### 按钮

| 按钮类型 | 状态 | 背景色 | 文本/图标色 |
|---------|------|--------|------------|
| 主要按钮 | 正常 | `?attr/colorPrimary` | `?attr/colorOnPrimary` |
| | 禁用 | `?attr/colorPrimary` (透明度30%) | `?attr/colorOnPrimary` (透明度30%) |
| | 按下 | `?attr/colorPrimary` (暗色变体) | `?attr/colorOnPrimary` |
| 次要按钮 | 正常 | 透明 | `?attr/colorPrimary` |
| | 禁用 | 透明 | `?attr/colorOnSurface` (透明度38%) |
| | 按下 | `?attr/colorPrimaryContainer` | `?attr/colorPrimary` |
| 轮廓按钮 | 正常 | 透明 | `?attr/colorPrimary` |
| | 禁用 | 透明 | `?attr/colorOnSurface` (透明度38%) |
| | 按下 | `?attr/colorPrimaryContainer` | `?attr/colorPrimary` |
| 文本按钮 | 正常 | 透明 | `?attr/colorPrimary` |
| | 禁用 | 透明 | `?attr/colorOnSurface` (透明度38%) |
| | 按下 | `?attr/colorPrimary` (透明度12%) | `?attr/colorPrimary` |

### 选择控件

| 控件类型 | 状态 | 颜色 |
|---------|------|------|
| 复选框 | 选中 | `?attr/colorPrimary` |
| | 未选中 | `?attr/colorOutline` |
| | 禁用选中 | `?attr/colorOnSurface` (透明度38%) |
| | 禁用未选中 | `?attr/colorOnSurface` (透明度38%) |
| 单选按钮 | 选中 | `?attr/colorPrimary` |
| | 未选中 | `?attr/colorOutline` |
| 开关 | 开启 | `?attr/colorPrimary` |
| | 关闭 | `?attr/colorOutline` |

### 导航元素

| 元素类型 | 状态 | 背景色 | 文本/图标色 |
|---------|------|--------|------------|
| 底部导航栏项 | 选中 | 透明 | `?attr/colorPrimary` |
| | 未选中 | 透明 | `?attr/colorOnSurfaceVariant` |
| 侧边导航栏项 | 选中 | `?attr/colorPrimaryContainer` | `?attr/colorOnPrimaryContainer` |
| | 未选中 | 透明 | `?attr/colorOnSurfaceVariant` |
| 标签页 | 选中 | 透明 | `?attr/colorPrimary` |
| | 未选中 | 透明 | `?attr/colorOnSurfaceVariant` |

### 卡片和容器

| 元素类型 | 背景色 | 边框色 |
|---------|--------|--------|
| 标准卡片 | `?attr/colorSurface` | `?attr/colorOutline` |
| 强调卡片 | `?attr/colorPrimaryContainer` | 无或 `?attr/colorPrimary` |
| 对话框 | `?attr/colorSurface` | 无 |
| 底部表单 | `?attr/colorSurfaceContainer` | 无 |

### 文本和图标

| 元素类型 | 颜色 |
|---------|------|
| 主要文本 | `?attr/colorOnSurface` |
| 次要文本 | `?attr/colorOnSurfaceVariant` |
| 链接文本 | `?attr/colorPrimary` |
| 主要图标 | `?attr/colorOnSurface` |
| 次要图标 | `?attr/colorOnSurfaceVariant` |
| 强调图标 | `?attr/colorPrimary` |

## 实现最佳实践

1. **使用主题属性而非直接颜色**
   - 正确: `android:textColor="?attr/colorOnSurface"`
   - 错误: `android:textColor="#000000"`

2. **为状态创建选择器**
   - 使用状态选择器XML为不同状态（正常、按下、禁用等）定义颜色

3. **保持一致性**
   - 同一类型的元素应使用相同的色彩角色
   - 不要为相似元素使用不同的颜色方案

4. **考虑可访问性**
   - 确保文本和背景之间有足够的对比度（至少4.5:1）
   - 不要仅依靠颜色来传达信息

5. **使用Material组件**
   - 优先使用Material组件库中的组件，它们已经内置了对主题色的支持

## 参考资料

- [Material Design 3 色彩系统](https://m3.material.io/styles/color/system)
- [Material Design 3 色彩角色](https://m3.material.io/styles/color/roles)
- [Material Design 3 主题](https://m3.material.io/styles/color/the-color-system/color-theme)

---

通过遵循这些指南，我们可以确保应用在各种设备和环境下都能提供一致、专业的用户体验，同时保持品牌识别度和可访问性。
