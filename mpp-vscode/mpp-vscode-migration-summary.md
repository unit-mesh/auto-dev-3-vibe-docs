# mpp-vscode 功能迁移快速参考

> 快速查看新旧版本功能对比和迁移要点

---

## 📊 功能对比矩阵

| 功能模块 | 旧版 (Samples/autodev-vscode) | 新版 (mpp-vscode) | 优先级 | 状态 |
|---------|------------------------------|------------------|--------|------|
| **Chat UI** | ✅ React (gui-sidebar) | ✅ React (webview) | - | ✅ 完成 |
| **CodeLens** | ✅ 完整实现 | ✅ Tree-sitter 解析 | P0 | ✅ 完成 |
| **代码补全** | ✅ InlineCompletion | ❌ 缺失 | P0 | 🔲 TODO |
| **AutoComment** | ✅ 完整实现 | ✅ LLM 生成 | P0 | ✅ 完成 |
| **AutoTest** | ✅ 完整实现 | ✅ LLM 生成 | P0 | ✅ 完成 |
| **AutoMethod** | ✅ 完整实现 | ✅ LLM 生成 | P0 | ✅ 完成 |
| **QuickFix** | ✅ 完整实现 | ❌ 缺失 | P1 | 🔲 TODO |
| **代码库索引** | ✅ LanceDB | ⛔ 暂不实现 | - | - |
| **多语言支持** | ✅ 7种语言 | ✅ 7种语言 | P2 | ✅ 完成 |
| **Custom Actions** | ✅ 完整实现 | ❌ 缺失 | P1 | 🔲 TODO |
| **Git 集成** | ✅ 提交消息 | ❌ 缺失 | P2 | 🔲 TODO |
| **Terminal 集成** | ✅ 错误解释 | ❌ 缺失 | P2 | 🔲 TODO |
| **Rename 建议** | ✅ 完整实现 | ❌ 缺失 | P2 | 🔲 TODO |
| **国际化** | ✅ 中英文 | ❌ 缺失 | P2 | 🔲 TODO |
| **Diff 管理** | ✅ 完整实现 | ✅ 完整实现 | - | ✅ 完成 |
| **DevIns 语法** | ✅ 完整实现 | ✅ 完整实现 | - | ✅ 完成 |
| **IDE Server** | ❌ 无 | ✅ MCP 协议 | - | ✅ 完成 |

**图例**:
- ✅ 已实现
- ⚠️ 部分实现
- ❌ 缺失
- ⛔ 暂不实现
- 🔲 TODO

---

## ✅ 已完成功能 (2025-12-04)

### CodeLens Provider
- ✅ Tree-sitter 代码解析 (TypeScript, JavaScript, Python, Java, Kotlin, Go, Rust)
- ✅ 正则表达式 fallback
- ✅ 类/方法/函数识别
- ✅ CodeLens 显示 (Quick Chat, Explain, Optimize, AutoComment, AutoTest, AutoMethod)

### Auto Actions
- ✅ AutoComment - 生成文档注释
- ✅ AutoTest - 生成单元测试
- ✅ AutoMethod - 生成方法实现
- ✅ Diff 预览和应用

### 提示词模板
- ✅ auto-doc 模板
- ✅ test-gen 模板
- ✅ auto-method 模板
- ✅ 语言特定注释符号

---

## 🔲 待完成功能 (TODO)

### P0 - 核心功能
- [ ] **InlineCompletion** - 代码补全
  - 参考: `Samples/autodev-vscode/src/action/providers/AutoDevCodeInlineCompletionProvider.ts`
  - 需要: FIM (Fill-in-the-Middle) 支持

### P1 - 重要功能
- [ ] **QuickFix Provider** - 错误修复建议
  - 参考: `Samples/autodev-vscode/src/action/providers/AutoDevQuickFixProvider.ts`
- [ ] **Custom Actions** - 自定义操作
  - 参考: `Samples/autodev-vscode/src/prompt-manage/custom-action/`

### P2 - 增强功能
- [ ] **Git 集成** - 提交消息生成
  - 参考: `Samples/autodev-vscode/src/action/devops/CommitMessageGenAction.ts`
- [ ] **Terminal 集成** - 错误解释
  - 参考: `Samples/autodev-vscode/src/action/terminal/`
- [ ] **Rename 建议** - 变量/函数重命名
  - 参考: `Samples/autodev-vscode/src/action/refactor/`
- [ ] **国际化** - 中英文支持
  - 参考: `Samples/autodev-vscode/l10n/`

---

## 📁 关键文件映射

### 已完成的文件

| 旧版文件 | 新版文件 | 状态 |
|---------|---------|------|
| `src/action/providers/AutoDevCodeLensProvider.ts` | `src/providers/codelens-provider.ts` | ✅ |
| `src/editor/ast/NamedElementBuilder.ts` | `src/providers/code-element-parser.ts` | ✅ |
| `src/action/autodoc/AutoDocActionExecutor.ts` | `src/actions/auto-actions.ts` | ✅ |
| `src/action/autotest/AutoTestActionExecutor.ts` | `src/actions/auto-actions.ts` | ✅ |
| `src/action/autoMethod/AutoMethodActionExecutor.ts` | `src/actions/auto-actions.ts` | ✅ |
| `prompts/genius/en/code/*.vm` | `src/prompts/prompt-templates.ts` | ✅ |

### 待完成的文件

| 旧版文件 | 新版文件 | 状态 |
|---------|---------|------|
| `src/action/providers/AutoDevCodeInlineCompletionProvider.ts` | `src/providers/inline-completion.ts` | 🔲 |
| `src/action/providers/AutoDevQuickFixProvider.ts` | `src/providers/quickfix-provider.ts` | 🔲 |
| `src/prompt-manage/custom-action/` | `src/actions/custom-actions.ts` | 🔲 |
| `src/action/devops/CommitMessageGenAction.ts` | `src/integrations/git-integration.ts` | 🔲 |

---

## 🔧 技术实现

### Tree-sitter 代码解析
```typescript
// 使用 web-tree-sitter 进行 AST 解析
import Parser from 'web-tree-sitter';

// 语言配置文件定义查询模式
const LANGUAGE_PROFILES = {
  typescript: {
    classQuery: new MemoizedQuery(`(class_declaration ...)`),
    methodQuery: new MemoizedQuery(`(function_declaration ...)`)
  }
};
```

### Auto Actions 架构
```typescript
// 统一的 ActionContext 接口
interface ActionContext {
  document: vscode.TextDocument;
  element: CodeElement;
  config: ModelConfig;
  log: (message: string) => void;
}

// 使用 mpp-core LLMService
const llmService = new LLMService(config);
await llmService.streamMessage(prompt, onChunk);
```

---

## 📦 依赖包

### 已安装
```json
{
  "dependencies": {
    "web-tree-sitter": "^0.22.2",
    "@unit-mesh/treesitter-artifacts": "latest"
  }
}
```

### 待安装 (按需)
```json
{
  "dependencies": {
    "gray-matter": "^4.0.3",    // Custom Actions
    "simple-git": "^3.19.0"     // Git 集成
  }
}
```

---

## 🚀 下一步

1. **InlineCompletion** - 实现代码补全功能
2. **QuickFix** - 实现错误修复建议
3. **Custom Actions** - 支持自定义操作

---

**最后更新**: 2025-12-04
