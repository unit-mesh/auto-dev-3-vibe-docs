# mpp-vscode 功能迁移 GitHub Issues

> 将以下 issues 创建到 GitHub 仓库中，追踪功能迁移进度

---

## Issue 1: [P0] 实现 CodeLens Provider

### 标签
`enhancement`, `p0-critical`, `migration`

### 描述
从旧版 autodev-vscode 迁移 CodeLens Provider 功能，在函数/类上方显示操作按钮。

### 功能需求
- [ ] 实现 `CodeLensProvider` 接口
- [ ] 解析代码元素（类、方法）
- [ ] 注册以下 CodeLens 操作：
  - [ ] Quick Chat - 将代码发送到聊天
  - [ ] Explain Code - 解释代码
  - [ ] Optimize Code - 优化代码
  - [ ] AutoComment - 生成文档注释
  - [ ] AutoTest - 生成测试代码
  - [ ] AutoMethod - 方法补全
- [ ] 支持配置项：
  - [ ] `autodev.codelens.enable` - 启用/禁用
  - [ ] `autodev.codelens.displayMode` - 展开/折叠模式
  - [ ] `autodev.codelens.items` - 显示项目列表

### 技术要点
- 代码解析方案：简化版 Tree-sitter 或基于正则
- 集成 mpp-core 的 `JsCodingAgent` 和 `JsDevInsCompiler`
- 参考旧版实现：`Samples/autodev-vscode/src/action/providers/AutoDevCodeLensProvider.ts`

### 验收标准
- [ ] 打开支持的文件，函数/类上方显示 CodeLens
- [ ] 点击操作能正常执行
- [ ] 配置项生效

### 预估工作量
2天

### 依赖
- mpp-core 已构建
- Chat UI 已实现

### 相关文档
- [功能对比分析](./mpp-vscode-migration-analysis.md#1-code-actions--codelens-provider-高优先级)
- [实施计划](./mpp-vscode-migration-plan.md#day-1-2-codelens-provider)

---

## Issue 2: [P0] 实现 InlineCompletion Provider

### 标签
`enhancement`, `p0-critical`, `migration`

### 描述
实现代码自动补全功能，在用户输入时提供智能补全建议。

### 功能需求
- [ ] 实现 `InlineCompletionItemProvider` 接口
- [ ] 集成 mpp-core 的 `JsCompletionManager`
- [ ] 实现延迟触发机制
- [ ] 支持 FIM (Fill-In-Middle) 模式
- [ ] 支持配置项：
  - [ ] `autodev.completion.enable` - 启用/禁用
  - [ ] `autodev.completion.model` - 补全模型
  - [ ] `autodev.completion.delay` - 触发延迟
  - [ ] `autodev.completion.fimTokens` - FIM 特殊标记

### 技术要点
- 使用 debounce 避免频繁请求
- 提取前缀和后缀上下文
- 支持多种 LLM 提供商
- 参考旧版实现：`Samples/autodev-vscode/src/action/providers/AutoDevCodeInlineCompletionProvider.ts`

### 验收标准
- [ ] 输入代码时自动显示补全建议
- [ ] 按 Tab 接受补全
- [ ] 延迟触发可配置
- [ ] 支持多种模型

### 预估工作量
3天

### 依赖
- mpp-core `JsCompletionManager` 可用
- LLM 配置已完成

### 相关文档
- [功能对比分析](./mpp-vscode-migration-analysis.md#2-代码补全-inlinecompletion-高优先级)
- [实施计划](./mpp-vscode-migration-plan.md#day-5-inlinecompletion-provider-part-1)

---

## Issue 3: [P0] 实现 Auto Actions (Comment/Test/Method)

### 标签
`enhancement`, `p0-critical`, `migration`

### 描述
实现自动生成功能：文档注释、单元测试、方法补全。

### 功能需求
- [ ] **AutoComment**: 为函数/类生成文档注释
  - [ ] 调用 mpp-core 生成注释
  - [ ] 预览 Diff
  - [ ] 接受/拒绝更改
- [ ] **AutoTest**: 为函数生成单元测试
  - [ ] 分析函数签名和逻辑
  - [ ] 生成测试代码
  - [ ] 自动放置到测试目录
- [ ] **AutoMethod**: 补全空方法实现
  - [ ] 基于方法签名和上下文生成实现
  - [ ] 预览 Diff

### 技术要点
- 集成 mpp-core 的 `JsCodingAgent`
- 复用已有的 `DiffManager`
- 构建合适的 Prompt 模板
- 参考旧版实现：
  - `Samples/autodev-vscode/src/action/autodoc/AutoDocActionExecutor.ts`
  - `Samples/autodev-vscode/src/action/autotest/AutoTestActionExecutor.ts`
  - `Samples/autodev-vscode/src/action/autoMethod/AutoMethodActionExecutor.ts`

### 验收标准
- [ ] 选择函数，点击 "AutoComment"，生成文档
- [ ] 选择函数，点击 "AutoTest"，生成测试
- [ ] 选择空方法，点击 "AutoMethod"，补全实现
- [ ] Diff 预览可接受/拒绝

### 预估工作量
2天

### 依赖
- Issue #1 (CodeLens Provider)
- mpp-core `JsCodingAgent` 可用
- Diff Manager 已实现

### 相关文档
- [功能对比分析](./mpp-vscode-migration-analysis.md#1-code-actions--codelens-provider-高优先级)
- [实施计划](./mpp-vscode-migration-plan.md#day-3-4-autocomment--autotest-actions)

---

## Issue 4: [P1] 实现 QuickFix Provider

### 标签
`enhancement`, `p1-important`, `migration`

### 描述
实现代码错误快速修复功能，在诊断错误时提供 AI 修复建议。

### 功能需求
- [ ] 实现 `CodeActionProvider` 接口
- [ ] 集成 VSCode Diagnostics
- [ ] 调用 mpp-core 生成修复代码
- [ ] 在灯泡菜单中显示 "AutoDev: Fix" 选项
- [ ] 预览修复后的代码

### 技术要点
- 监听 `vscode.languages.getDiagnostics()`
- 构建错误修复 Prompt
- 集成 mpp-core 的 `JsCodingAgent`
- 参考旧版实现：`Samples/autodev-vscode/src/action/providers/AutoDevQuickFixProvider.ts`

### 验收标准
- [ ] 代码有错误时，灯泡显示 "AutoDev: Fix" 选项
- [ ] 点击后自动生成修复代码
- [ ] 预览 Diff 并可接受/拒绝

### 预估工作量
2天

### 依赖
- mpp-core `JsCodingAgent` 可用
- Diff Manager 已实现

### 相关文档
- [功能对比分析](./mpp-vscode-migration-analysis.md#3-quickfix-provider-中优先级)
- [实施计划](./mpp-vscode-migration-plan.md#day-11-12-quickfix-provider)

---

## Issue 5: [P1] 实现 Custom Actions / Team Prompts

### 标签
`enhancement`, `p1-important`, `migration`

### 描述
支持从 `prompts/` 目录加载自定义操作和团队提示词。

### 功能需求
- [ ] 实现 `PromptManager` 加载自定义 Prompt
  - [ ] 支持 Markdown + YAML Frontmatter 格式
  - [ ] 从 `prompts/` 目录加载
  - [ ] 热重载（文件变化时自动更新）
- [ ] 实现团队术语 (Team Terms) 支持
  - [ ] 加载 CSV 格式的术语表
  - [ ] 在 Prompt 中替换术语
- [ ] 将自定义操作添加到 CodeLens
- [ ] 将自定义操作添加到右键菜单
- [ ] 配置项：`autodev.customPromptDir`

### Prompt 模板格式
```markdown
---
name: action-name
title: "Action Title"
description: "When to use this action"
---

Prompt template content here.
Use {code}, {language}, {selection} as placeholders.
```

### 技术要点
- 使用 `gray-matter` 解析 Frontmatter
- 使用 `csv-parse` 解析术语表
- 简单的模板字符串替换（不使用 Velocity）
- 参考旧版实现：
  - `Samples/autodev-vscode/src/prompt-manage/PromptManager.ts`
  - `Samples/autodev-vscode/src/prompt-manage/team-prompts/TeamPromptsBuilder.ts`

### 验收标准
- [ ] 可从 `prompts/` 目录加载自定义操作
- [ ] CodeLens 显示自定义操作
- [ ] 可执行自定义操作并预览结果
- [ ] 团队术语可在提示词中使用

### 预估工作量
3天

### 依赖
- Issue #1 (CodeLens Provider)
- 安装依赖：`gray-matter`, `csv-parse`

### 相关文档
- [功能对比分析](./mpp-vscode-migration-analysis.md#6-custom-actions--team-prompts-中优先级)
- [实施计划](./mpp-vscode-migration-plan.md#day-8-10-custom-actions--team-prompts)

---

## Issue 6: [P1] UI 增强 - Chat 历史记录和多会话

### 标签
`enhancement`, `p1-important`, `ui`

### 描述
完善 Chat UI，添加历史记录、多会话管理、模型选择器等功能。

### 功能需求
- [ ] **Chat 历史记录**
  - [ ] 本地存储 (localStorage/IndexedDB)
  - [ ] 历史记录列表 UI
  - [ ] 切换历史会话
  - [ ] 搜索历史记录
- [ ] **多会话管理**
  - [ ] 新建会话按钮
  - [ ] 会话列表（侧边栏）
  - [ ] 重命名会话
  - [ ] 删除会话
- [ ] **Model 选择器**
  - [ ] 下拉菜单选择模型
  - [ ] 显示当前使用的模型
  - [ ] 快速切换模型
- [ ] **Markdown 渲染优化**
  - [ ] 代码语法高亮
  - [ ] 代码块工具栏（复制、插入）
  - [ ] 数学公式支持（可选）

### 技术要点
- 参考旧版 `gui-sidebar/src/components/` 的 React 组件
- 复用以下组件：
  - `mainInput/ContinueInputBox.tsx` - 输入框
  - `markdown/StyledMarkdownPreview.tsx` - Markdown 渲染
  - `modelSelection/ModelSelect.tsx` - 模型选择器
  - `loaders/ProgressBar.tsx` - 进度条
- 使用 React Hooks 管理状态

### 验收标准
- [ ] 可查看历史聊天记录
- [ ] 可创建新会话
- [ ] 可切换模型
- [ ] Markdown 渲染美观，代码高亮正确
- [ ] UI 响应流畅

### 预估工作量
2天

### 依赖
- 基础 Chat UI 已实现
- React 开发环境已配置

### 相关文档
- [功能对比分析](./mpp-vscode-migration-analysis.md#10-ui-增强-低优先级)
- [实施计划](./mpp-vscode-migration-plan.md#day-13-14-ui-增强)

---

## Issue 7: [P2] Git 集成 - 自动生成提交消息

### 标签
`enhancement`, `p2-nice-to-have`, `git`

### 描述
分析 Git Diff，自动生成符合规范的提交消息。

### 功能需求
- [ ] 实现 `autodev.git.generateCommitMessage` 命令
- [ ] 集成到 SCM 面板（按钮）
- [ ] 分析 staged changes
- [ ] 生成提交消息并填充到输入框
- [ ] 支持配置项：
  - [ ] `autodev.git.autoGenerateMessage` - 自动生成
  - [ ] `autodev.git.messageStyle` - 消息风格（conventional/simple）

### 技术要点
- 使用 `simple-git` 或 VSCode Git API
- 构建 Git Diff 分析 Prompt
- 生成符合 Conventional Commits 规范的消息
- 参考旧版实现：`Samples/autodev-vscode/src/action/devops/CommitMessageGenAction.ts`

### 验收标准
- [ ] SCM 面板有 "生成提交消息" 按钮
- [ ] 点击后分析暂存的更改
- [ ] 自动填充提交消息
- [ ] 消息格式符合规范

### 预估工作量
1天

### 依赖
- mpp-core LLM 服务可用
- 安装依赖：`simple-git`（可选）

### 相关文档
- [功能对比分析](./mpp-vscode-migration-analysis.md#8-git-集成-低优先级)
- [实施计划](./mpp-vscode-migration-plan.md#day-17-18-git-集成)

---

## Issue 8: [P2] Terminal 集成 - 错误解释和命令建议

### 标签
`enhancement`, `p2-nice-to-have`, `terminal`

### 描述
提供终端命令建议和错误解释功能。

### 功能需求
- [ ] **错误解释** (`autodev.terminal.explainError`)
  - [ ] 终端右键菜单添加 "解释错误" 选项
  - [ ] 选中错误信息，发送到 Chat 分析
  - [ ] 显示错误原因和修复建议
- [ ] **命令建议** (`autodev.terminal.suggestCommand`)
  - [ ] 根据任务描述生成命令
  - [ ] 显示命令并可直接执行
  - [ ] 支持命令历史记录

### 技术要点
- 监听终端选中文本
- 构建错误分析 Prompt
- 使用 `vscode.window.activeTerminal` API
- 参考旧版实现：终端相关命令

### 验收标准
- [ ] 终端右键菜单有 "解释错误" 选项
- [ ] 选中错误信息，点击后显示解释
- [ ] 可通过命令请求命令建议
- [ ] 可直接执行建议的命令

### 预估工作量
1天

### 依赖
- Chat UI 已实现
- mpp-core LLM 服务可用

### 相关文档
- [功能对比分析](./mpp-vscode-migration-analysis.md#7-terminal-集成-低优先级)
- [实施计划](./mpp-vscode-migration-plan.md#day-19-terminal-集成)

---

## Issue 9: [P2] 国际化支持 (i18n)

### 标签
`enhancement`, `p2-nice-to-have`, `i18n`

### 描述
添加多语言支持，至少支持中文和英文。

### 功能需求
- [ ] 添加 `l10n/` 目录
- [ ] 创建 `bundle.l10n.json` (英文)
- [ ] 创建 `bundle.l10n.zh-cn.json` (中文)
- [ ] 更新 `package.nls.json` (package.json 翻译)
- [ ] 代码中使用 `l10n.t()` 替换硬编码字符串
- [ ] 测试多语言切换

### 技术要点
- 使用 VSCode 内置 i18n API (`vscode.l10n`)
- 从旧版复制翻译文件
- 参考旧版实现：`Samples/autodev-vscode/l10n/`

### 验收标准
- [ ] UI 文本支持中英文切换
- [ ] 根据 VSCode 语言设置自动切换
- [ ] 所有用户可见文本已翻译

### 预估工作量
1天

### 依赖
- 核心功能已实现

### 相关文档
- [功能对比分析](./mpp-vscode-migration-analysis.md#11-国际化-i18n-低优先级)
- [实施计划](./mpp-vscode-migration-plan.md#day-20-国际化-i18n)

---

## Milestone 1: 核心功能 (Week 1)

**目标**: 实现基础的 AI 编码助手功能

**包含 Issues**:
- Issue #1: CodeLens Provider
- Issue #2: InlineCompletion Provider
- Issue #3: Auto Actions

**完成标准**:
- [ ] 用户可以通过 CodeLens 调用 AI 功能
- [ ] 代码补全可正常工作
- [ ] 可生成文档、测试和方法实现

**预估完成时间**: 7天

---

## Milestone 2: 增强功能 (Week 2)

**目标**: 添加错误修复和自定义操作

**包含 Issues**:
- Issue #4: QuickFix Provider
- Issue #5: Custom Actions
- Issue #6: UI 增强

**完成标准**:
- [ ] 可修复代码错误
- [ ] 支持自定义操作
- [ ] Chat UI 功能完善

**预估完成时间**: 7天

---

## Milestone 3: 集成功能 (Week 3, 可选)

**目标**: 集成 Git、Terminal 等外部工具

**包含 Issues**:
- Issue #7: Git 集成
- Issue #8: Terminal 集成
- Issue #9: 国际化

**完成标准**:
- [ ] Git 提交消息自动生成
- [ ] Terminal 错误解释
- [ ] 支持多语言

**预估完成时间**: 3天

---

## 总工作量

- **P0 核心功能**: 7天 (Milestone 1)
- **P1 重要功能**: 7天 (Milestone 2)
- **P2 增强功能**: 3天 (Milestone 3)
- **总计**: 17天 (约 2-3 周)

---

## 如何创建这些 Issues

### 方法 1: 手动创建
1. 进入 GitHub 仓库
2. 点击 "Issues" → "New Issue"
3. 复制对应 Issue 的内容
4. 添加标签和 Milestone

### 方法 2: 使用 GitHub CLI
```bash
# 安装 gh CLI
brew install gh

# 登录
gh auth login

# 创建 Issues（示例）
gh issue create --title "[P0] 实现 CodeLens Provider" \
  --body-file issue1.md \
  --label "enhancement,p0-critical,migration" \
  --milestone "Milestone 1: 核心功能"
```

### 方法 3: 使用脚本批量创建
创建一个脚本 `create-issues.sh`：
```bash
#!/bin/bash

# Issue 1
gh issue create --title "[P0] 实现 CodeLens Provider" \
  --label "enhancement,p0-critical,migration" \
  --milestone "Milestone 1" \
  --body "$(cat << EOF
从旧版 autodev-vscode 迁移 CodeLens Provider 功能...
详见: docs/github-issues-mpp-vscode-migration.md#issue-1
EOF
)"

# Issue 2
gh issue create --title "[P0] 实现 InlineCompletion Provider" \
  --label "enhancement,p0-critical,migration" \
  --milestone "Milestone 1" \
  --body "$(cat << EOF
实现代码自动补全功能...
详见: docs/github-issues-mpp-vscode-migration.md#issue-2
EOF
)"

# ... 其他 Issues
```

---

**创建日期**: 2025-12-04  
**最后更新**: 2025-12-04

