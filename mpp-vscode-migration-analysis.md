# mpp-vscode 功能迁移分析报告

> 对比分析 `mpp-vscode` (新版本) 和 `Samples/autodev-vscode` (旧版本) 的功能差异

**日期**: 2025-12-04  
**目标**: 将旧版 autodev-vscode 的核心功能迁移到基于 Kotlin Multiplatform 的新版 mpp-vscode

---

## 📊 整体架构对比

### 旧版本架构 (Samples/autodev-vscode)
```
- TypeScript + InversifyJS (依赖注入)
- Tree-sitter (代码解析)
- LanceDB (向量数据库)
- SQLite (本地数据存储)
- React Sidebar (gui-sidebar/)
- 内置 LLM 集成 (OpenAI, Anthropic, 国内厂商)
- 内置 Embeddings (Transformers.js, ONNX)
- 完整的代码分析工具链
```

### 新版本架构 (mpp-vscode)
```
- TypeScript + Kotlin/JS (mpp-core)
- React Webview (内嵌 UI)
- MCP 协议 IDE Server
- 通过 mpp-core 复用核心能力
- 轻量级设计，核心逻辑在 Kotlin
```

---

## ✅ 已实现功能

| 功能 | 状态 | 实现方式 |
|------|------|----------|
| Chat UI | ✅ | React Webview (`webview/`) |
| 基础聊天能力 | ✅ | 通过 mpp-core JsKoogLLMService |
| Diff 管理 | ✅ | DiffManager + DiffContentProvider |
| IDE Server (MCP) | ✅ | Express HTTP Server |
| DevIns 语法高亮 | ✅ | TextMate Grammar |
| DevIns 自动补全 | ✅ | CompletionItemProvider |
| 配置管理 | ✅ | ConfigManager |
| 状态栏显示 | ✅ | StatusBarManager |

---

## ❌ 缺失的核心功能

### 1. **Code Actions & CodeLens Provider** (高优先级)

#### 旧版实现
- **CodeLens Provider** (`AutoDevCodeLensProvider.ts`)
  - 在函数/类上方显示操作按钮
  - 支持的操作：
    - Quick Chat
    - Explain Code
    - Optimize Code
    - AutoComment (自动生成文档)
    - AutoTest (自动生成测试)
    - AutoMethod (方法补全)
    - Custom Action (自定义操作)
  - 可配置显示模式：展开/折叠
  - 可配置显示项目

#### 迁移策略
```typescript
// 新版实现路径建议
mpp-vscode/src/providers/codelens-provider.ts

// 依赖 mpp-core 的能力
- JsDevInsCompiler (解析代码结构)
- JsCodingAgent (执行生成任务)
- JsCompletionManager (代码补全)
```

#### 相关配置
```json
{
  "autodev.codelensDisplayMode": "expand|collapse",
  "autodev.codelensDislayItems": [
    "quickChat",
    "autoTest",
    "autoComment",
    "AutoMethod"
  ]
}
```

---

### 2. **代码补全 (InlineCompletion)** (高优先级)

#### 旧版实现
- **InlineCompletionProvider** (`AutoDevCodeInlineCompletionProvider.ts`)
- FIM (Fill-In-Middle) 模式
- 支持多种补全模型
- 可配置触发延迟、停止词、模板

#### 缺失原因
- mpp-core 提供了 `JsCompletionManager`，但新版 VSCode 扩展未接入
- 需要实现 VSCode 的 `InlineCompletionItemProvider` 接口

#### 迁移策略
```typescript
// 新版实现路径建议
mpp-vscode/src/providers/inline-completion.ts

// 通过 mpp-core 的 JsCompletionManager
const completionManager = new JsCompletionManager(llmService);
await completionManager.complete(request, cancellationToken);
```

#### 相关配置
```json
{
  "autodev.completions.enable": false,
  "autodev.completions.provider": "ollama",
  "autodev.completions.model": "codeqwen:7b-code-v1.5-q5_1",
  "autodev.completions.requestDelay": 500,
  "autodev.completions.fimSpecialTokens": {
    "prefix": "<|fim_prefix|>",
    "suffix": "<|fim_suffix|>",
    "middle": "<|fim_middle|>"
  }
}
```

---

### 3. **QuickFix Provider** (中优先级)

#### 旧版实现
- **QuickFixProvider** (`AutoDevQuickFixProvider.ts`)
- 基于诊断信息提供修复建议
- 集成到 VSCode 的灯泡提示

#### 迁移策略
- 通过 mpp-core 的 JsCodingAgent 生成修复代码
- 实现 VSCode 的 `CodeActionProvider`

---

### 4. **代码库索引与检索 (Codebase Indexing)** ~~(暂不实现)~~

#### 决策
**不在初期实现**，原因：
- 增加大量依赖（vectordb, transformers.js, lancedb）
- 实现复杂度高，工作量大
- 不是核心功能，Chat 功能可以通过其他方式提供上下文
- 后期可考虑通过 MCP 协议集成外部索引服务

#### 备选方案
1. **使用 VSCode 内置搜索**: `vscode.workspace.findTextInFiles()`
2. **通过 MCP 集成外部服务**: 如果需要语义搜索
3. **手动选择上下文**: 用户主动选择相关文件/代码

---

### 5. **多语言代码上下文 Provider** (中优先级)

#### 旧版实现
支持的语言：
- Java (`JavaProfile`, `JavaRelevantCodeProvider`, `JavaTestGenProvider`)
- TypeScript (`TypeScriptProfile`, `TypeScriptStructurer`)
- Python (`PythonProfile`, `PythonTestGenProvider`)
- Go (`GolangProfile`, `GoTestGenProvider`)
- Kotlin (`KotlinProfile`)
- Rust (`RustProfile`)
- C# (`CsharpProfile`)

每个语言 Profile 包含：
- **LanguageProfile**: 语言元数据 (测试框架、文件扩展名等)
- **StructurerProvider**: 代码结构分析
- **RelevantCodeProvider**: 相关代码查找
- **TestGenProvider**: 测试生成
- **CodeCorrector**: 代码修正

#### 新版状态
- ❌ 完全缺失
- mpp-core 可能有部分能力

#### 迁移策略
- 评估 mpp-core 中是否已有语言分析能力
- 可能需要保留 Tree-sitter 集成（TypeScript 侧）
- 或通过 MCP 协议扩展

---

### 6. **Custom Actions / Team Prompts** (中优先级)

#### 旧版实现
- **TeamPromptsBuilder** (`TeamPromptsBuilder.ts`)
- **CustomActionExecutor** (`CustomActionExecutor.ts`)
- 从 CSV 加载团队术语
- 从 `prompts/` 目录加载自定义提示模板
- Velocity 模板引擎

#### 新版状态
- ❌ 完全缺失
- DevIns 自动补全有部分能力（`/`, `@`, `$`）

#### 迁移策略
- mpp-core 有 DevIns 编译器，可以扩展
- 需要实现自定义 Action 的 UI 和执行器

#### 相关配置
```json
{
  "autodev.customPromptDir": "prompts"
}
```

---

### 7. **Terminal 集成** (低优先级)

#### 旧版实现
- **Terminal 命令建议** (`autodev.editor.suggestCommand`)
- **Terminal 错误解释** (`autodev.terminal.explainTerminalSelectionContextMenu`)
- **调试终端** (`autodev.debugTerminal`)

#### 迁移策略
- 基于 MCP 协议实现
- 通过 IDE Server 访问终端上下文

---

### 8. **Git 集成** (低优先级)

#### 旧版实现
- **生成提交消息** (`CommitMessageGenAction.ts`)
- 集成到 SCM 面板

#### 迁移策略
- 通过 MCP 协议访问 Git 状态
- 使用 mpp-core 的 LLM 服务生成消息

---

### 9. **Rename Suggestions** (低优先级)

#### 旧版实现
- **RenameProvider** (`AutoDevRenameProvider.ts`)
- 基于上下文建议重命名

#### 迁移策略
- 可选功能，后续迁移

---

### 10. **UI 增强** (低优先级)

#### 旧版实现
- **Chat 历史记录** (`showChatHistory`)
- **多会话管理** (`newChatSession`)
- **进度条** (`IndexingProgressBar.tsx`)
- **Model 选择器** (`ModelSelect.tsx`)
- **Context 管理** (`ContextItemsPeek.tsx`)
- **Monaco 代码块** (`MonacoCodeBlock.tsx`)

#### 新版状态
- ⚠️ 部分实现（基础聊天 UI）

#### 迁移策略
- 参考旧版 `gui-sidebar/` 的 React 组件
- 逐步增强新版 `webview/` 的 UI

---

### 11. **国际化 (i18n)** (低优先级)

#### 旧版实现
- `l10n/` 目录 (VSCode 内置 i18n)
- `package.nls.json`, `package.nls.zh-cn.json`
- 代码中使用 `l10n.t()`

#### 新版状态
- ❌ 缺失

#### 迁移策略
- 添加 i18n 支持
- 复用旧版的翻译文件

---

### 12. **Tutorial & Feedback** (低优先级)

#### 旧版实现
- **Tutorial** (`autodev.showTutorial`)
- **Feedback** (`autodev.feedback`)

#### 迁移策略
- 可选功能，后续添加

---

## 🎯 迁移优先级建议

### P0 - 核心功能 (必须迁移)
1. ✅ **CodeLens Provider** - 提供代码操作入口
2. ✅ **InlineCompletion Provider** - 代码补全
3. ✅ **CodeLens Actions 实现**:
   - AutoComment (文档生成)
   - AutoTest (测试生成)
   - AutoMethod (方法补全)

### P1 - 重要功能 (建议迁移)
4. ⚠️ **QuickFix Provider** - 错误修复建议
5. ⚠️ **Custom Actions** - 自定义操作
6. ⚠️ **多语言支持** - 至少支持主流语言 (Java, TS, Python) (可选)

### P2 - 增强功能 (可选迁移)
8. ⬜ **UI 增强** - 历史记录、多会话等
9. ⬜ **Git 集成** - 提交消息生成
10. ⬜ **Terminal 集成** - 命令建议
11. ⬜ **i18n** - 国际化支持

---

## 🔧 技术难点与挑战

### 1. Tree-sitter 集成
- **问题**: Kotlin/JS 没有成熟的 Tree-sitter 绑定
- **方案**: 
  - 保留 TypeScript 侧的 Tree-sitter 使用
  - 通过 MCP 协议传递解析结果
  - 或评估 mpp-core 中的代码解析能力

### 2. ~~向量数据库~~ (暂不实现)
- **决策**: 不在初期版本实现代码库索引功能
- **理由**: 增加复杂度，非核心功能

### 3. ~~Embeddings 计算~~ (暂不实现)
- **决策**: 不在初期版本实现
- **理由**: 依赖于代码库索引功能

### 4. 依赖注入
- **问题**: 旧版使用 InversifyJS，新版使用简单注入
- **方案**: 
  - 新版保持简单设计
  - 不需要完全复制旧版的 DI 结构

---

## 📦 推荐迁移路径

### Phase 1: CodeLens & Actions (2-3 days)
```
1. 实现 CodeLensProvider
2. 实现 Code Actions:
   - QuickChat (直接调用 Chat)
   - ExplainCode (调用 Chat + 提示词)
   - AutoComment (调用 mpp-core)
   - AutoTest (调用 mpp-core)
3. 添加配置项
4. 测试验证
```

### Phase 2: InlineCompletion (1-2 days)
```
1. 实现 InlineCompletionProvider
2. 接入 mpp-core JsCompletionManager
3. 添加 FIM 配置
4. 测试补全效果
```

### Phase 3: UI 增强 (2-3 days)
```
1. 添加历史记录
2. 添加多会话管理
3. 添加 Model 选择器
4. 优化 Markdown 渲染
```

### Phase 4: 其他功能 (按需)
```
1. QuickFix Provider
2. Git 集成
3. Terminal 集成
4. i18n 支持
```

---

## 🚀 快速开始

### 第一步：理解 mpp-core 能力
```bash
# 查看 mpp-core 导出
cd /Volumes/source/ai/autocrud
./gradlew :mpp-core:assembleJsPackage
cat mpp-core/build/packages/js/kotlin/mpp-core.mjs | grep "export"
```

### 第二步：实现 CodeLens Provider
```typescript
// mpp-vscode/src/providers/codelens-provider.ts
import * as vscode from 'vscode';
import { MppCoreBridge } from '../bridge/mpp-core';

export class CodeLensProvider implements vscode.CodeLensProvider {
  // 参考旧版 AutoDevCodeLensProvider.ts
  // 使用 mpp-core 的能力
}
```

### 第三步：注册 Provider
```typescript
// mpp-vscode/src/extension.ts
import { CodeLensProvider } from './providers/codelens-provider';

export async function activate(context: vscode.ExtensionContext) {
  // ...
  context.subscriptions.push(
    vscode.languages.registerCodeLensProvider(
      { pattern: '**/*' },
      new CodeLensProvider()
    )
  );
}
```

---

## 📚 参考资源

### 旧版关键文件
- `src/action/providers/AutoDevCodeLensProvider.ts` - CodeLens 实现
- `src/action/providers/AutoDevCodeInlineCompletionProvider.ts` - 补全实现
- `src/code-search/indexing/CodebaseIndexer.ts` - 索引实现
- `src/AutoDevExtension.ts` - 主入口
- `gui-sidebar/src/` - React UI 组件

### mpp-core 关键 API
- `JsKoogLLMService` - LLM 服务
- `JsCodingAgent` - 编码 Agent
- `JsCompletionManager` - 补全管理
- `JsDevInsCompiler` - DevIns 编译器
- `JsToolRegistry` - 工具注册

### VSCode API 文档
- [CodeLens Provider](https://code.visualstudio.com/api/references/vscode-api#CodeLensProvider)
- [InlineCompletion Provider](https://code.visualstudio.com/api/references/vscode-api#InlineCompletionItemProvider)
- [CodeAction Provider](https://code.visualstudio.com/api/references/vscode-api#CodeActionProvider)

---

## ✅ 总结

### 关键发现
1. **架构差异**: 旧版是完整的独立实现，新版依赖 mpp-core
2. **核心缺失**: CodeLens, InlineCompletion, 代码索引
3. **技术挑战**: Tree-sitter, 向量数据库需要保留在 TS 侧
4. **迁移策略**: 渐进式迁移，优先核心功能

### 建议
1. **先实现 CodeLens** - 最直观的用户入口
2. **再实现补全** - 提升开发体验
3. **然后代码索引** - 增强 RAG 能力
4. **最后增强 UI** - 优化交互体验

### 预估工作量
- **P0 核心功能**: 5-7 天
- **P1 重要功能**: 7-10 天
- **P2 增强功能**: 5-7 天
- **总计**: 3-4 周

---

**更新记录**:
- 2025-12-04: 初始版本，完成功能对比分析

