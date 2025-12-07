# AutoDev 设计系统 - 色彩系统文档

## 概述

AutoDev 的色彩系统基于色彩心理学原理，旨在传达**智能（活力）**、**信任（稳定）**和**清晰（中性）**。系统完全支持亮色和暗色模式，并遵循 WCAG 无障碍标准。

## 设计原则

### 1. 色彩心理学

- **主色（Intelligent Indigo）**：融合蓝色的稳定性和紫色的创造力，与"智能/巫师"主题相呼应
- **辅色（Spark Cyan）**：代表 AI 的"火花"，用于关键操作和需要用户注意的焦点元素
- **中性色**：精细的灰度色阶（10 个层级），定义界面层次结构

### 2. 暗黑模式最佳实践

- ❌ **避免纯黑**：界面背景不使用 `#000000`，而是使用深灰色 `#171717`（neutral-900）
- ❌ **避免纯白**：文本不使用 `#FFFFFF`，而是使用浅灰色 `#f5f5f5`（neutral-100）
- ✅ **去饱和化**：明亮、饱和的颜色在暗色背景上会"振动"，所有非中性色都有独立的暗黑模式变体

## 核心调色板

### 主色（Primary - Intelligent Indigo）

```typescript
indigo: {
  50: '#eef2ff',
  100: '#e0e7ff',
  200: '#c7d2fe',
  300: '#a5b4fc',  // 暗黑模式主色
  400: '#818cf8',  // 暗黑模式悬停
  500: '#6366f1',
  600: '#4f46e5',  // 亮色模式主色
  700: '#4338ca',  // 亮色模式悬停
  800: '#3730a3',
  900: '#312e81',
}
```

### 辅色（Accent - Spark Cyan）

```typescript
cyan: {
  50: '#ecfeff',
  100: '#cffafe',
  200: '#a5f3fc',
  300: '#67e8f9',
  400: '#22d3ee',  // 暗黑模式辅色
  500: '#06b6d4',  // 亮色模式辅色
  600: '#0891b2',
  700: '#0e7490',
  800: '#155e75',
  900: '#164e63',
}
```

### 中性色（Neutral）

```typescript
neutral: {
  50: '#fafafa',   // 亮色模式背景
  100: '#f5f5f5',  // 暗黑模式主文本
  200: '#e5e5e5',  // 亮色模式边框
  300: '#d4d4d4',  // 暗黑模式辅文本
  400: '#a3a3a3',
  500: '#737373',
  600: '#525252',
  700: '#404040',  // 暗黑模式边框
  800: '#262626',  // 暗黑模式卡片
  900: '#171717',  // 暗黑模式背景
}
```

## 语义化颜色

### 成功（Success - Green）

```typescript
green: {
  300: '#86efac',  // 暗黑模式
  600: '#16a34a',  // 亮色模式
  // ... 其他色阶
}
```

### 警告（Warning - Amber）

```typescript
amber: {
  300: '#fcd34d',  // 暗黑模式
  500: '#f59e0b',  // 亮色模式
  // ... 其他色阶
}
```

### 错误（Error - Red）

```typescript
red: {
  300: '#fca5a5',  // 暗黑模式
  600: '#dc2626',  // 亮色模式
  // ... 其他色阶
}
```

### 信息（Info - Blue）

```typescript
blue: {
  300: '#93c5fd',  // 暗黑模式
  500: '#3b82f6',  // 亮色模式
  // ... 其他色阶
}
```

## 色彩令牌（Color Tokens）

### 亮色模式

| 令牌名称 | 用途 | 色值 |
|---------|------|------|
| `--color-primary` | 主要交互色 | `indigo[600]` #4f46e5 |
| `--color-primary-hover` | 主要悬停 | `indigo[700]` #4338ca |
| `--color-accent` | AI 火花/辅色 | `cyan[500]` #06b6d4 |
| `--color-text-primary` | 主文本 | `neutral[900]` #171717 |
| `--color-text-secondary` | 辅文本 | `neutral[700]` #404040 |
| `--color-surface-bg` | 界面背景 | `neutral[50]` #fafafa |
| `--color-surface-card` | 卡片/容器 | `#ffffff` |
| `--color-border` | 边框/分割线 | `neutral[200]` #e5e5e5 |
| `--color-semantic-success` | 成功状态 | `green[600]` #16a34a |
| `--color-semantic-warning` | 警告状态 | `amber[500]` #f59e0b |
| `--color-semantic-error` | 错误状态 | `red[600]` #dc2626 |
| `--color-semantic-info` | 信息状态 | `blue[500]` #3b82f6 |

### 暗色模式

| 令牌名称 | 用途 | 色值 |
|---------|------|------|
| `--color-primary` | 主要交互色 | `indigo[300]` #a5b4fc |
| `--color-primary-hover` | 主要悬停 | `indigo[400]` #818cf8 |
| `--color-accent` | AI 火花/辅色 | `cyan[400]` #22d3ee |
| `--color-text-primary` | 主文本 | `neutral[100]` #f5f5f5 |
| `--color-text-secondary` | 辅文本 | `neutral[300]` #d4d4d4 |
| `--color-surface-bg` | 界面背景 | `neutral[900]` #171717 |
| `--color-surface-card` | 卡片/容器 | `neutral[800]` #262626 |
| `--color-border` | 边框/分割线 | `neutral[700]` #404040 |
| `--color-semantic-success` | 成功状态 | `green[300]` #86efac |
| `--color-semantic-warning` | 警告状态 | `amber[300]` #fcd34d |
| `--color-semantic-error` | 错误状态 | `red[300]` #fca5a5 |
| `--color-semantic-info` | 信息状态 | `blue[300]` #93c5fd |

## 实现

### Kotlin (mpp-core)

色彩系统在 `mpp-core` 中定义，可在所有平台（JS、JVM、Android）共享：

```kotlin
// 文件位置: mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/ui/ColorSystem.kt

object IndigoScale {
    const val c600 = "#4f46e5"  // 亮色模式主色
    const val c300 = "#a5b4fc"  // 暗黑模式主色
    // ...
}

val LightTheme.colors = ColorTheme(
    primary = IndigoScale.c600,
    accent = CyanScale.c500,
    // ...
)

val DarkTheme.colors = ColorTheme(
    primary = IndigoScale.c300,
    accent = CyanScale.c400,
    // ...
)
```

### TypeScript (mpp-ui)

在 `mpp-ui` 中提供 TypeScript 实现和便捷的辅助工具：

```typescript
// 文件位置: mpp-ui/src/jsMain/typescript/design-system/colors.ts

import { semanticInk, semanticChalk } from './design-system/theme-helpers.js';

// 在 Ink 组件中使用
<Text color={semanticInk.primary}>主要文本</Text>
<Text color={semanticInk.success}>成功消息</Text>

// 在 CLI 输出中使用
console.log(semanticChalk.success('操作成功'));
console.log(semanticChalk.error('操作失败'));
```

## 使用指南

### 1. UI 组件（Ink/React）

```typescript
import { semanticInk } from '../design-system/theme-helpers.js';

<Box borderStyle="single" borderColor={semanticInk.accent}>
  <Text color={semanticInk.primary}>标题</Text>
  <Text color={semanticInk.muted}>描述文本</Text>
</Box>
```

### 2. 终端输出（Chalk）

```typescript
import { semanticChalk, coloredStatus, dividers } from '../design-system/theme-helpers.js';

// 语义化状态消息
console.log(coloredStatus('success', '文件创建成功'));
console.log(coloredStatus('error', '编译失败'));

// 直接使用 chalk
console.log(semanticChalk.primary('重要信息'));
console.log(semanticChalk.muted('次要信息'));

// 分割线
console.log(dividers.solid(60));
```

### 3. 状态指示

```typescript
import { statusIndicators, coloredStatus } from '../design-system/theme-helpers.js';

// 标准状态指示器
console.log(coloredStatus('success', 'Task completed'));  // ✓ Task completed
console.log(coloredStatus('error', 'Build failed'));      // ✗ Build failed
console.log(coloredStatus('warning', 'Deprecated'));      // ⚠ Deprecated
console.log(coloredStatus('info', 'New version'));        // ℹ New version
```

## 更新的文件清单

### mpp-core
- ✅ `src/commonMain/kotlin/cc/unitmesh/agent/ui/ColorSystem.kt` (新建)
- ✅ `src/jsMain/kotlin/cc/unitmesh/agent/ui/ColorSystemExports.kt` (新建)

### mpp-ui

**设计系统文件：**
- ✅ `src/jsMain/typescript/design-system/colors.ts` (新建)
- ✅ `src/jsMain/typescript/design-system/theme-helpers.ts` (新建)
- ✅ `src/jsMain/typescript/design-system/index.ts` (新建)

**UI 组件：**
- ✅ `src/jsMain/typescript/ui/App.tsx`
- ✅ `src/jsMain/typescript/ui/Banner.tsx`
- ✅ `src/jsMain/typescript/ui/ChatInterface.tsx`
- ✅ `src/jsMain/typescript/ui/CommandSuggestions.tsx`
- ✅ `src/jsMain/typescript/ui/MessageRenderer.tsx`
- ✅ `src/jsMain/typescript/ui/WelcomeScreen.tsx`

**输出格式化：**
- ✅ `src/jsMain/typescript/agents/render/CliRenderer.ts`
- ✅ `src/jsMain/typescript/utils/outputFormatter.ts`

## 可访问性（WCAG）

所有颜色组合都经过设计以满足 WCAG 2.1 AA 级对比度要求：

- **文本与背景对比度**：最小 4.5:1（正常文本）或 3:1（大文本）
- **UI 组件对比度**：最小 3:1
- **暗黑模式优化**：去饱和化处理，避免视觉振动和眼疲劳

## 未来扩展

1. **主题切换**：添加用户主题切换功能
2. **高对比度模式**：为视障用户提供高对比度选项
3. **色盲模式**：支持不同类型的色盲友好调色板
4. **自定义主题**：允许用户自定义品牌颜色

## 参考资源

- [WCAG 2.1 对比度指南](https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html)
- [Material Design 色彩系统](https://m3.material.io/styles/color/overview)
- [Tailwind CSS 调色板](https://tailwindcss.com/docs/customizing-colors)

---

**版本**: 1.0.0  
**最后更新**: 2025-11-07  
**维护者**: AutoDev Team

