# Xiuper Design System - GitHub Issues

以下 Issue 可以通过 `gh issue create` 命令创建，或手动复制到 GitHub。

---

## Issue 1: 增强 AutoDevColors 支持 Xiuper 主题

**命令:**
```bash
gh issue create \
  --title "[Design System] 增强 AutoDevColors 支持 Xiuper 主题" \
  --label "enhancement,design-system,P0" \
  --body "## 描述

在现有 AutoDevColors 中添加 Xiuper 子对象，包含虚空色阶、能量色阶和信号色。
Xiuper 是 AutoDev 的特殊版本，不新建独立文件，增强现有设计系统。

## 背景

Xiuper 的核心美学是「霓虹暗夜」与「气韵流动」的结合：
- **虚空色阶 (Void)**: 带微量蓝色的冷色调背景
- **能量色阶 (Energy)**: 区分用户意图 (电光青) 与 AI 响应 (霓虹紫)
- **信号色 (Signal)**: 高饱和度的状态指示色

## 色彩定义

\`\`\`kotlin
object AutoDevColors {
    // 现有色阶保持不变...
    
    // 新增 Xiuper 子对象
    object Xiuper {
        object Void {
            val bg = Color(0xFF0B0E14)       // 全局底层背景
            val surface1 = Color(0xFF151922) // 侧边栏、面板
            val surface2 = Color(0xFF1F2430) // 悬停、输入框
            val surface3 = Color(0xFF2A3040) // 边框、分割线
        }
        
        object Energy {
            val xiu = Color(0xFF00F3FF)      // 电光青 - 用户意图
            val xiuDim = Color(0x4000F3FF)   // 光晕效果
            val ai = Color(0xFFD946EF)       // 霓虹紫 - AI 生成
            val aiDim = Color(0x40D946EF)    // 光晕效果
        }
        
        object Signal {
            val success = Color(0xFF00E676)
            val error = Color(0xFFFF1744)
            val warn = Color(0xFFFFEA00)
        }
    }
}
\`\`\`

## 任务清单

- [ ] 在 \`AutoDevColors.kt\` 添加 \`Xiuper\` 子对象
- [ ] 更新 \`AutoDevTheme.kt\` 添加 Xiuper 主题模式
- [ ] 更新 TypeScript \`colors.ts\` 同步色彩定义
- [ ] 添加 CSS 变量定义

## 验收标准

- [ ] 虚空背景色 (#0B0E14) 可应用到 Xiuper 主题
- [ ] accent-xiu (#00F3FF) 用于用户交互元素
- [ ] accent-ai (#D946EF) 用于 AI 生成内容
- [ ] 保持与现有 AutoDevColors 的向后兼容

## 参考

- [Xiuper 设计系统规范](docs/design-system/xuiper-ds.md)
- [迁移分析文档](docs/design-system/xiuper-migration-analysis.md)"
```

---

## Issue 2: 实现 Xiuper 动效系统与 Launch 启动动画

**命令:**
```bash
gh issue create \
  --title "[Animation] 实现 Xiuper 动效系统与 Launch 启动动画" \
  --label "enhancement,animation,P0" \
  --body "## 描述

实现 Xiuper「唯快不破」的动效系统，并创建品牌 Launch 启动动画，展示「咻」的极速美学。

## 动效曲线定义

\`\`\`kotlin
object AutoDevAnimation {
    // Xiu 曲线 - 极速响应，瞬间到达
    val EaseXiu = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    
    // 线性流 - AI 代码流式输出的稳定感
    val EaseStream = LinearEasing
    
    // 武侠弹簧 - 微交互的触觉反馈
    val SpringTactile = spring<Float>(
        dampingRatio = 0.6f,
        stiffness = 180f
    )
    
    object Duration {
        const val INSTANT = 100
        const val FAST = 150
        const val NORMAL = 250
        const val SLOW = 400
    }
}
\`\`\`

## Launch 启动动画

展示 Xiuper「咻」的品牌美学：

### 视觉序列 (总时长 ~850ms)

\`\`\`
[0ms]      黑屏 (bg-void #0B0E14)
[50ms]     Logo 出现，scale: 0.3, opacity: 0
[50-350ms] Logo 放大 (ease-xiu) + 电光青光晕扩散
[350-550ms] \"Xiuper Fast\" 文字从下方滑入
[550-700ms] 短暂停留，光晕呼吸效果
[700-850ms] 整体淡出，进入主界面
\`\`\`

### 视觉元素

- **背景**: 虚空黑 \`#0B0E14\`
- **Logo**: AutoDev Logo
- **光晕**: 电光青 \`#00F3FF\`，模糊半径 20-40dp
- **文字**: \"Xiuper Fast\"，电光青色

## 任务清单

- [ ] 创建 \`AutoDevAnimation.kt\` 定义动效曲线
- [ ] 实现 \`XiuperLaunchScreen.kt\` 启动动画组件
- [ ] 在 Desktop App 启动流程中集成 Launch 动画
- [ ] 创建可复用的动效扩展函数
- [ ] 添加 \`prefers-reduced-motion\` 支持
- [ ] TypeScript/CSS 同步动效变量

## 验收标准

- [ ] 应用启动时展示 Launch 动画
- [ ] 动画总时长 < 850ms，流畅无卡顿
- [ ] 支持 reduced-motion 时跳过动画
- [ ] ease-xiu 曲线应用到按钮、弹窗等交互

## 参考

- [Xiuper 设计系统规范 - 动效章节](docs/design-system/xuiper-ds.md)
- [迁移分析文档](docs/design-system/xiuper-migration-analysis.md)"
```

---

## Issue 3: 实现幽灵文本 Ghost Text

**命令:**
```bash
gh issue create \
  --title "[Component] 实现幽灵文本 Ghost Text" \
  --label "feature,editor,P0" \
  --body "## 描述

实现 AI 代码补全的幽灵文本效果，清晰区分用户代码与 AI 建议，支持部分采纳。

## 功能需求

### 视觉表现

- AI 预测文本以 \`accent-ai\` (#D946EF) 颜色显示
- 40% 透明度 + 斜体样式
- 清晰表明：「这是建议，尚未成为现实」

### 交互行为

| 操作 | 效果 |
|------|------|
| \`Tab\` | 全量采纳所有预测 |
| \`Ctrl + →\` | 逐词采纳 |
| \`Esc\` | 取消预测 |
| 继续输入 | 自动取消预测 |

### 动效反馈

- 采纳时：文字从「幽灵态」瞬间变为「实体态」
- 伴随 \`spring-tactile\` 弹跳效果
- 高亮色变为正常语法高亮色

## 任务清单

- [ ] 创建 \`GhostTextRenderer.kt\` 组件
- [ ] 实现幽灵文本的视觉样式
- [ ] 实现 Tab 全量采纳逻辑
- [ ] 实现 Ctrl+→ 逐词采纳逻辑
- [ ] 添加 spring-tactile 采纳动效
- [ ] 集成到编辑器组件

## 验收标准

- [ ] AI 预测以幽灵态显示，与用户代码视觉区分明显
- [ ] Tab 键可全量采纳
- [ ] Ctrl+→ 可逐词采纳
- [ ] 采纳时有流畅的弹跳反馈

## 参考

- Cursor 的 Copilot++ 模式
- [Xiuper 设计规范 - 幽灵文本章节](docs/design-system/xuiper-ds.md)"
```

---

## Issue 4: 重构命令面板为全知命令栏 Omnibar

**命令:**
```bash
gh issue create \
  --title "[Component] 重构命令面板为全知命令栏 Omnibar" \
  --label "enhancement,ux,P1" \
  --body "## 描述

将现有命令面板重构为 Xiuper 风格的 Omnibar，成为整个 IDE 的控制中枢。

## 视觉设计

- **尺寸**: 宽幅设计 (750px+)，居中悬浮
- **背景**: 高斯模糊 (\`backdrop-filter: blur(20px)\`) + 半透明深色
- **输入框**: 极简无边框设计，光标高度占满行高
- **上下文芯片**: 在光标左侧显示当前上下文 (如 \`@File: utils.ts > Symbol: processData\`)

## 交互逻辑

### 搜索功能

- 支持模糊匹配 (Fuzzy Search)
- 排序权重: 最近使用 > 上下文相关 > 全局匹配
- 每个结果右侧强制显示快捷键

### 预览面板

- 选中命令/文件时，右侧滑出预览视图
- 使用 \`ease-xiu\` 动画

## 任务清单

- [ ] 设计 Omnibar UI 原型
- [ ] 实现宽幅悬浮布局
- [ ] 添加高斯模糊背景效果
- [ ] 实现上下文芯片显示
- [ ] 实现模糊搜索 + 权重排序
- [ ] 添加快捷键提示
- [ ] 实现预览面板

## 验收标准

- [ ] 命令面板以 Omnibar 形式居中悬浮
- [ ] 模糊搜索响应迅速，排序合理
- [ ] 上下文芯片正确显示当前位置
- [ ] 预览面板平滑滑出

## 参考

- Linear 的命令面板
- Raycast 的搜索交互"
```

---

## Issue 5: 清理硬编码颜色

**命令:**
```bash
gh issue create \
  --title "[Cleanup] 清理硬编码颜色替换为设计系统 Token" \
  --label "tech-debt,refactor,P2" \
  --body "## 描述

替换所有硬编码的 \`Color(0x...)\` 为设计系统 Token，确保主题一致性。

## 当前状态

189 处硬编码颜色，分布在 14 个文件中。

## 重点文件

| 文件 | 硬编码数量 |
|------|-----------|
| \`DiffSketchRenderer.kt\` | 8 处 |
| \`PlanSummaryBar.kt\` | 12 处 |
| \`TaskPanel.kt\` | 5 处 |
| \`AnsiParser.kt\` | 32 处 (终端颜色可保留) |
| \`DevInSyntaxHighlighter.kt\` | 16 处 |
| 其他 | ~116 处 |

## 任务清单

- [ ] \`DiffSketchRenderer.kt\` - 替换为 AutoDevColors.Diff
- [ ] \`PlanSummaryBar.kt\` - 替换为 AutoDevColors 状态色
- [ ] \`TaskPanel.kt\` - 替换为 AutoDevColors 状态色
- [ ] \`DevInSyntaxHighlighter.kt\` - 替换为 AutoDevColors.Syntax
- [ ] 其他文件逐一检查

## 验收标准

- [ ] 无硬编码颜色 (AnsiParser 终端色除外)
- [ ] 主题切换时颜色正确响应
- [ ] 无视觉回归

## 工具

可使用以下命令查找硬编码颜色:
\`\`\`bash
grep -r \"Color(0x\" mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/
\`\`\`"
```

---

## 快速创建所有 Issues

```bash
# 在项目根目录执行
cd /Users/phodal/ai/xiiu

# Issue 1: 色彩系统
gh issue create \
  --title "[Design System] 增强 AutoDevColors 支持 Xiuper 主题" \
  --label "enhancement,design-system" \
  --body-file docs/design-system/github-issues-xiuper.md

# 或者使用 GitHub Web 界面手动创建
```


