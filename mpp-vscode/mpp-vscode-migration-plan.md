# mpp-vscode 功能迁移实施计划

> 基于 [mpp-vscode-migration-analysis.md](./mpp-vscode-migration-analysis.md) 的详细实施方案

**开始日期**: 2025-12-04  
**预计完成**: 2025-12-24 (3 周)

---

## 🎯 目标

将 `Samples/autodev-vscode` 的核心功能迁移到 `mpp-vscode`，使其成为一个功能完整的、基于 Kotlin Multiplatform 的 AI 编码助手。

---

## 📅 迁移时间表

### Week 1: 核心功能 (P0)

#### Day 1-2: CodeLens Provider
**目标**: 实现代码级别的操作入口

**任务清单**:
- [ ] 创建 `src/providers/codelens-provider.ts`
- [ ] 实现 CodeLensProvider 接口
- [ ] 解析代码元素（类、方法）
  - 评估使用 mpp-core 的能力
  - 或集成简化版 Tree-sitter
- [ ] 注册 CodeLens 命令：
  - [ ] `autodev.codelens.quickChat`
  - [ ] `autodev.codelens.explainCode`
  - [ ] `autodev.codelens.optimizeCode`
  - [ ] `autodev.codelens.autoComment`
  - [ ] `autodev.codelens.autoTest`
  - [ ] `autodev.codelens.autoMethod`
- [ ] 添加配置项到 `package.json`
- [ ] 测试验证

**技术要点**:
```typescript
// 代码元素解析策略
Option 1: 使用 mpp-core 的 DevInsCompiler
  - 优点: 统一技术栈
  - 缺点: 可能能力有限

Option 2: 简化版 Tree-sitter (仅支持主流语言)
  - 优点: 解析准确
  - 缺点: 增加依赖

Option 3: 基于正则的简单解析
  - 优点: 无依赖
  - 缺点: 不够准确
```

**文件结构**:
```
mpp-vscode/src/
├── providers/
│   ├── codelens-provider.ts      # CodeLens Provider
│   └── code-element-parser.ts    # 代码元素解析
├── commands/
│   ├── codelens-commands.ts      # CodeLens 命令实现
│   └── index.ts                  # 命令注册
└── extension.ts                  # 注册 Provider
```

**验证标准**:
- [ ] 打开 `.ts/.js/.py/.java` 文件，函数/类上方显示 CodeLens
- [ ] 点击 "Quick Chat" 将代码发送到聊天
- [ ] 点击 "Explain Code" 自动添加解释提示词
- [ ] CodeLens 可通过配置显示/隐藏

---

#### Day 3-4: AutoComment & AutoTest Actions
**目标**: 实现文档生成和测试生成功能

**任务清单**:
- [ ] 创建 `src/actions/auto-comment.ts`
- [ ] 创建 `src/actions/auto-test.ts`
- [ ] 集成 mpp-core JsCodingAgent
- [ ] 实现 Diff 预览（复用已有 DiffManager）
- [ ] 添加 Prompt 模板
- [ ] 测试验证

**技术要点**:
```typescript
// 调用 mpp-core 生成代码
import { JsCodingAgent, JsKoogLLMService } from '@autodev/mpp-core';

async function generateDocstring(code: string, language: string) {
  const llmService = new JsKoogLLMService(config);
  const agent = new JsCodingAgent(llmService);
  
  const prompt = buildDocstringPrompt(code, language);
  const result = await agent.chat(prompt);
  
  return result;
}
```

**Prompt 模板**:
```
// prompts/auto-comment.md
根据以下 {language} 代码，生成符合规范的文档注释：

{code}

要求：
1. 描述函数/类的功能
2. 说明参数和返回值
3. 符合 {language} 的文档注释规范（JSDoc/Javadoc/docstring）
```

**验证标准**:
- [ ] 选择函数，点击 "AutoComment"，生成文档注释
- [ ] 预览 Diff，可接受/拒绝
- [ ] 选择函数，点击 "AutoTest"，生成测试代码
- [ ] 测试代码符合项目结构（放在正确的测试目录）

---

#### Day 5: InlineCompletion Provider (Part 1)
**目标**: 实现基础代码补全功能

**任务清单**:
- [ ] 创建 `src/providers/inline-completion.ts`
- [ ] 实现 InlineCompletionItemProvider 接口
- [ ] 集成 mpp-core JsCompletionManager
- [ ] 实现触发逻辑（延迟触发）
- [ ] 基础测试

**技术要点**:
```typescript
import * as vscode from 'vscode';
import { JsCompletionManager, JsKoogLLMService } from '@autodev/mpp-core';

export class InlineCompletionProvider implements vscode.InlineCompletionItemProvider {
  private manager: JsCompletionManager;
  private debounceTimer?: NodeJS.Timeout;
  
  async provideInlineCompletionItems(
    document: vscode.TextDocument,
    position: vscode.Position,
    context: vscode.InlineCompletionContext,
    token: vscode.CancellationToken
  ): Promise<vscode.InlineCompletionItem[] | null> {
    // 1. 提取前缀和后缀
    const prefix = document.getText(new vscode.Range(new vscode.Position(0, 0), position));
    const suffix = document.getText(new vscode.Range(position, document.lineAt(document.lineCount - 1).range.end));
    
    // 2. 调用 mpp-core
    const completion = await this.manager.complete({ prefix, suffix, language: document.languageId }, token);
    
    // 3. 返回补全项
    return [new vscode.InlineCompletionItem(completion.text)];
  }
}
```

**配置项**:
```json
{
  "autodev.completion.enable": false,
  "autodev.completion.model": "gpt-4",
  "autodev.completion.delay": 500
}
```

---

### Week 2: 重要功能 (P1)

#### Day 6-7: InlineCompletion 完善 (Part 2)
**目标**: 完善补全功能，支持多模型和 FIM

**任务清单**:
- [ ] 添加 FIM (Fill-In-Middle) 支持
- [ ] 支持多模型切换
- [ ] 添加补全缓存
- [ ] 优化触发逻辑（智能触发）
- [ ] 性能优化

**FIM 配置**:
```json
{
  "autodev.completion.fimTokens": {
    "prefix": "<|fim_prefix|>",
    "suffix": "<|fim_suffix|>",
    "middle": "<|fim_middle|>"
  },
  "autodev.completion.stopTokens": ["\n\n", "```"]
}
```

**验证标准**:
- [ ] 输入代码时，自动显示补全建议
- [ ] 按 Tab 接受补全
- [ ] 补全延迟可配置
- [ ] 支持 Ollama/OpenAI 等多种模型

---

#### Day 8-10: Custom Actions / Team Prompts
**目标**: 支持自定义操作和团队提示词

**任务清单**:
- [ ] 创建 `src/prompts/prompt-manager.ts`
- [ ] 支持从 `prompts/` 目录加载模板
- [ ] 实现 Custom Action 执行器
- [ ] 添加到 CodeLens 和右键菜单
- [ ] 支持团队术语 (Team Terms)

**Prompt 目录结构**:
```
workspace/
└── prompts/
    ├── custom-actions/
    │   ├── add-logging.md
    │   ├── refactor-extract-method.md
    │   └── translate-to-python.md
    ├── team-terms.csv
    └── README.md
```

**Prompt 模板格式** (Markdown + YAML Frontmatter):
```markdown
---
name: add-logging
title: "Add Logging Statements"
description: "Add appropriate logging to the selected code"
---

为以下代码添加日志记录语句：

{code}

要求：
1. 在关键操作处添加日志
2. 使用合适的日志级别（debug/info/warn/error）
3. 不改变原有逻辑
```

**验证标准**:
- [ ] 可从 `prompts/` 目录加载自定义操作
- [ ] CodeLens 显示自定义操作
- [ ] 可执行自定义操作并预览结果
- [ ] 团队术语可在提示词中使用

---

#### Day 11-12: QuickFix Provider
**目标**: 实现错误修复建议

**任务清单**:
- [ ] 创建 `src/providers/quickfix-provider.ts`
- [ ] 实现 CodeActionProvider 接口
- [ ] 集成诊断信息 (Diagnostics)
- [ ] 调用 mpp-core 生成修复
- [ ] 测试验证

**技术要点**:
```typescript
export class QuickFixProvider implements vscode.CodeActionProvider {
  async provideCodeActions(
    document: vscode.TextDocument,
    range: vscode.Range,
    context: vscode.CodeActionContext,
    token: vscode.CancellationToken
  ): Promise<vscode.CodeAction[]> {
    const diagnostics = context.diagnostics;
    if (diagnostics.length === 0) return [];
    
    // 为每个诊断生成修复建议
    const actions: vscode.CodeAction[] = [];
    for (const diagnostic of diagnostics) {
      const action = new vscode.CodeAction(
        `AutoDev: Fix "${diagnostic.message}"`,
        vscode.CodeActionKind.QuickFix
      );
      
      action.command = {
        title: 'Fix with AutoDev',
        command: 'autodev.quickFix',
        arguments: [document, diagnostic]
      };
      
      actions.push(action);
    }
    
    return actions;
  }
}
```

**验证标准**:
- [ ] 代码有错误时，灯泡显示 "AutoDev: Fix" 选项
- [ ] 点击后自动生成修复代码
- [ ] 预览 Diff 并可接受/拒绝

---

### Week 3: 增强功能 (P1/P2)

#### Day 13-14: UI 增强
**目标**: 完善 Chat UI，提升用户体验

**任务清单**:
- [ ] 添加 Chat 历史记录
  - [ ] 本地存储 (localStorage)
  - [ ] 历史记录列表 UI
  - [ ] 切换历史会话
- [ ] 添加多会话管理
  - [ ] 新建会话按钮
  - [ ] 会话列表
  - [ ] 删除会话
- [ ] 添加 Model 选择器
  - [ ] 下拉菜单选择模型
  - [ ] 显示当前模型
- [ ] 优化 Markdown 渲染
  - [ ] 代码高亮
  - [ ] 代码块工具栏（复制、插入）
  - [ ] 数学公式支持

**参考旧版组件**:
```
Samples/autodev-vscode/gui-sidebar/src/components/
├── mainInput/ContinueInputBox.tsx      # 输入框
├── markdown/StyledMarkdownPreview.tsx  # Markdown 渲染
├── modelSelection/ModelSelect.tsx      # 模型选择
└── loaders/ProgressBar.tsx            # 进度条
```

**实现路径**:
```
mpp-vscode/webview/src/
├── components/
│   ├── ChatHistory.tsx              # 历史记录
│   ├── ModelSelector.tsx            # 模型选择器
│   ├── SessionList.tsx              # 会话列表
│   └── CodeBlock.tsx                # 代码块（带工具栏）
└── hooks/
    ├── useChatHistory.ts            # 历史记录 Hook
    └── useModelSelection.ts         # 模型选择 Hook
```

**验证标准**:
- [ ] 可查看历史聊天记录
- [ ] 可创建新会话
- [ ] 可切换模型
- [ ] Markdown 渲染美观，代码高亮正确

---


#### Day 17-18: Git 集成
**目标**: 自动生成提交消息

**任务清单**:
- [ ] 创建 `src/integrations/git-integration.ts`
- [ ] 实现 `autodev.git.generateCommitMessage` 命令
- [ ] 集成到 SCM 面板
- [ ] 分析 Git Diff
- [ ] 生成提交消息

**技术要点**:
```typescript
import * as vscode from 'vscode';
import simpleGit from 'simple-git';

export async function generateCommitMessage() {
  const git = simpleGit(vscode.workspace.rootPath);
  
  // 1. 获取 staged changes
  const diff = await git.diff(['--cached']);
  if (!diff) {
    vscode.window.showWarningMessage('No staged changes');
    return;
  }
  
  // 2. 调用 LLM 生成消息
  const prompt = `Generate a concise commit message for the following changes:\n\n${diff}`;
  const message = await llmService.chat(prompt);
  
  // 3. 填充到 SCM 输入框
  const scm = vscode.scm.inputBox;
  if (scm) {
    scm.value = message;
  }
}
```

**注册到 SCM 面板**:
```json
// package.json
{
  "contributes": {
    "menus": {
      "scm/title": [
        {
          "when": "scmProvider == git",
          "command": "autodev.git.generateCommitMessage",
          "group": "navigation"
        }
      ]
    }
  }
}
```

**验证标准**:
- [ ] SCM 面板有 "生成提交消息" 按钮
- [ ] 点击后分析暂存的更改
- [ ] 自动填充提交消息
- [ ] 消息格式符合规范（如 Conventional Commits）

---

#### Day 19: Terminal 集成
**目标**: 提供终端命令建议和错误解释

**任务清单**:
- [ ] 创建 `src/integrations/terminal-integration.ts`
- [ ] 实现 `autodev.terminal.explainError` 命令
- [ ] 实现 `autodev.terminal.suggestCommand` 命令
- [ ] 集成到终端右键菜单

**技术要点**:
```typescript
export async function explainTerminalError() {
  const terminal = vscode.window.activeTerminal;
  if (!terminal) return;
  
  // 1. 获取终端选中的文本
  const selection = await vscode.env.clipboard.readText(); // Workaround
  
  // 2. 分析错误
  const prompt = `Explain this terminal error and suggest a fix:\n\n${selection}`;
  const explanation = await llmService.chat(prompt);
  
  // 3. 显示在 Chat
  chatView.addMessage({ role: 'assistant', content: explanation });
}

export async function suggestCommand(task: string) {
  const prompt = `Suggest a terminal command to accomplish: ${task}`;
  const command = await llmService.chat(prompt);
  
  // 显示并可执行
  const result = await vscode.window.showInformationMessage(
    `Suggested command: ${command}`,
    'Execute',
    'Copy'
  );
  
  if (result === 'Execute') {
    vscode.window.activeTerminal?.sendText(command);
  }
}
```

**验证标准**:
- [ ] 终端右键菜单有 "解释错误" 选项
- [ ] 选中错误信息，点击后显示解释
- [ ] 可通过命令请求命令建议
- [ ] 可直接执行建议的命令

---

#### Day 20: 国际化 (i18n)
**目标**: 支持多语言界面

**任务清单**:
- [ ] 添加 `l10n/` 目录
- [ ] 创建 `bundle.l10n.json` (英文)
- [ ] 创建 `bundle.l10n.zh-cn.json` (中文)
- [ ] 更新 `package.nls.json`
- [ ] 代码中使用 `l10n.t()`
- [ ] 测试多语言切换

**文件结构**:
```
mpp-vscode/
├── l10n/
│   ├── bundle.l10n.json           # 英文
│   └── bundle.l10n.zh-cn.json     # 中文
├── package.nls.json               # package.json 英文翻译
└── package.nls.zh-cn.json         # package.json 中文翻译
```

**使用方式**:
```typescript
import { l10n } from 'vscode';

// 简单翻译
const title = l10n.t('Quick Chat');

// 带参数翻译
const message = l10n.t('File {0} not found', fileName);
```

**验证标准**:
- [ ] UI 文本支持中英文切换
- [ ] 根据 VSCode 语言设置自动切换
- [ ] 所有用户可见文本已翻译

---

#### Day 21: 测试与优化
**目标**: 完善测试，优化性能

**任务清单**:
- [ ] 编写单元测试
  - [ ] CodeLens Provider 测试
  - [ ] InlineCompletion Provider 测试
  - [ ] PromptManager 测试
- [ ] 编写集成测试
- [ ] 性能优化
  - [ ] 补全延迟优化
  - [ ] 索引性能优化
  - [ ] 内存使用优化
- [ ] 修复已知 Bug

**测试框架**:
```bash
# 已配置 vitest
npm run test
npm run test:watch
```

**性能指标**:
- CodeLens 渲染延迟 < 100ms
- InlineCompletion 触发延迟 < 500ms
- 索引速度 > 1000 文件/分钟
- 内存占用 < 500MB (中等项目)

---

## 📦 可选功能 (P2) - 按需实施

### Rename Suggestions
**工作量**: 1-2 天  
**优先级**: 低

```typescript
export class RenameProvider implements vscode.RenameProvider {
  async provideRenameEdits(
    document: vscode.TextDocument,
    position: vscode.Position,
    newName: string,
    token: vscode.CancellationToken
  ): Promise<vscode.WorkspaceEdit> {
    // 基于上下文建议更好的命名
    const context = getContext(document, position);
    const suggestion = await llmService.suggestRename(context);
    
    // 返回重命名编辑
    const edit = new vscode.WorkspaceEdit();
    // ... 实现重命名逻辑
    return edit;
  }
}
```

### 多语言支持 (Language Profiles)
**工作量**: 3-4 天  
**优先级**: 中

需要为每种语言实现：
- 测试框架识别
- 相关代码查找
- 代码结构分析

建议优先支持：Java, TypeScript, Python, Go

### Tutorial & Feedback
**工作量**: 1 天  
**优先级**: 低

- 首次使用教程（Walkthrough）
- 反馈表单
- 问题报告

---

## 🔧 技术决策

### 1. 代码解析方案
**决策**: 使用简化版 Tree-sitter + 正则备用

**理由**:
- Tree-sitter 解析准确，适合主流语言
- 正则作为备用，覆盖更多语言
- 不依赖完整的语言分析工具链

**实现**:
```typescript
// 优先使用 Tree-sitter
if (isSupportedByTreeSitter(language)) {
  return parseWithTreeSitter(code, language);
}

// 回退到正则
return parseWithRegex(code, language);
```

### 2. UI 框架
**决策**: React + VSCode Webview

**理由**:
- 已使用 React
- VSCode Webview 限制较多，但足够用
- 可复用旧版 React 组件

---

## 📝 配置项清单

### 新增配置项
```json
{
  // CodeLens
  "autodev.codelens.enable": true,
  "autodev.codelens.displayMode": "expand",
  "autodev.codelens.items": ["quickChat", "autoTest", "autoComment"],
  
  // InlineCompletion
  "autodev.completion.enable": false,
  "autodev.completion.model": "gpt-4",
  "autodev.completion.delay": 500,
  "autodev.completion.fimTokens": {
    "prefix": "<|fim_prefix|>",
    "suffix": "<|fim_suffix|>",
    "middle": "<|fim_middle|>"
  },
  
  // Custom Actions
  "autodev.customPromptDir": "prompts",
  
  // Git
  "autodev.git.autoGenerateMessage": false,
  "autodev.git.messageStyle": "conventional"
}
```

---

## ✅ 验收标准

### 功能完整性
- [ ] P0 功能全部实现
- [ ] P1 功能实现 >= 80%
- [ ] 所有功能通过测试

### 性能指标
- [ ] CodeLens 渲染 < 100ms
- [ ] 补全延迟 < 500ms
- [ ] 内存占用合理 (< 200MB)

### 用户体验
- [ ] UI 美观，交互流畅
- [ ] 错误提示清晰
- [ ] 配置项文档完整
- [ ] 支持中英文

### 代码质量
- [ ] 单元测试覆盖率 > 60%
- [ ] 无严重 Bug
- [ ] 代码规范统一
- [ ] 文档完整

---

## 📚 参考文档

### 内部文档
- [功能对比分析](./mpp-vscode-migration-analysis.md)
- [mpp-vscode README](../mpp-vscode/README.md)

### 外部资源
- [VSCode Extension API](https://code.visualstudio.com/api)
- [Tree-sitter](https://tree-sitter.github.io/)
- [LanceDB](https://lancedb.com/)
- [Transformers.js](https://huggingface.co/docs/transformers.js)

---

## 🚀 开始实施

### 准备工作
```bash
# 1. 更新 mpp-core
cd /Volumes/source/ai/autocrud
./gradlew :mpp-core:assembleJsPackage

# 2. 安装依赖
cd mpp-vscode
npm install

# 3. 创建分支
git checkout -b feature/migration-phase1
```

### 第一个任务
```bash
# 创建 CodeLens Provider
touch mpp-vscode/src/providers/codelens-provider.ts
touch mpp-vscode/src/commands/codelens-commands.ts

# 开始开发...
```

---

**更新记录**:
- 2025-12-04: 初始版本，完成详细实施计划

