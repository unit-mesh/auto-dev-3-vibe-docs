# mpp-vscode 功能迁移快速参考

> 快速查看新旧版本功能对比和迁移要点

---

## 📊 功能对比矩阵

| 功能模块 | 旧版 (Samples/autodev-vscode) | 新版 (mpp-vscode) | 优先级 | 工作量 |
|---------|------------------------------|------------------|--------|--------|
| **Chat UI** | ✅ React (gui-sidebar) | ✅ React (webview) | - | - |
| **CodeLens** | ✅ 完整实现 | ❌ 缺失 | P0 | 2天 |
| **代码补全** | ✅ InlineCompletion | ❌ 缺失 | P0 | 3天 |
| **AutoComment** | ✅ 完整实现 | ❌ 缺失 | P0 | 1天 |
| **AutoTest** | ✅ 完整实现 | ❌ 缺失 | P0 | 1天 |
| **AutoMethod** | ✅ 完整实现 | ❌ 缺失 | P0 | 1天 |
| **QuickFix** | ✅ 完整实现 | ❌ 缺失 | P1 | 2天 |
| **代码库索引** | ✅ LanceDB | ⛔ 暂不实现 | - | - |
| **多语言支持** | ✅ 7种语言 | ⚠️ 基础 | P2 | 4天 |
| **Custom Actions** | ✅ 完整实现 | ❌ 缺失 | P1 | 3天 |
| **Git 集成** | ✅ 提交消息 | ❌ 缺失 | P2 | 1天 |
| **Terminal 集成** | ✅ 错误解释 | ❌ 缺失 | P2 | 1天 |
| **Rename 建议** | ✅ 完整实现 | ❌ 缺失 | P2 | 2天 |
| **国际化** | ✅ 中英文 | ❌ 缺失 | P2 | 1天 |
| **Diff 管理** | ✅ 完整实现 | ✅ 完整实现 | - | - |
| **DevIns 语法** | ✅ 完整实现 | ✅ 完整实现 | - | - |
| **IDE Server** | ❌ 无 | ✅ MCP 协议 | - | - |

**图例**:
- ✅ 已实现
- ⚠️ 部分实现
- ❌ 缺失
- ⛔ 暂不实现
- P0 = 必须迁移
- P1 = 重要功能
- P2 = 增强功能

---

## 🎯 迁移优先级

### P0 - 核心功能 (必须, Week 1)
```
1. CodeLens Provider          [2天] ⭐⭐⭐
2. InlineCompletion           [3天] ⭐⭐⭐
3. AutoComment/Test/Method    [2天] ⭐⭐⭐

总计: 7天 (1周)
```

### P1 - 重要功能 (建议, Week 2)
```
4. QuickFix Provider          [2天] ⭐⭐
5. Custom Actions             [3天] ⭐⭐
6. UI 增强                    [2天] ⭐⭐

总计: 7天 (1周)
```

### P2 - 增强功能 (可选, Week 3)
```
8. UI 增强 (历史/多会话)      [2天] ⭐
9. Git 集成                   [1天] ⭐
10. Terminal 集成             [1天] ⭐
11. 国际化                    [1天] ⭐

总计: 5天 (1周)
```

**总工作量**: 17天 (约 2-3 周)

**注**: 代码库索引和向量数据库功能暂不实现，可后期通过 MCP 协议集成外部服务

---

## 📁 关键文件映射

### 旧版 → 新版文件对应

| 旧版文件 | 新版文件 | 说明 |
|---------|---------|------|
| `src/action/providers/AutoDevCodeLensProvider.ts` | `src/providers/codelens-provider.ts` | CodeLens |
| `src/action/providers/AutoDevCodeInlineCompletionProvider.ts` | `src/providers/inline-completion.ts` | 代码补全 |
| `src/action/providers/AutoDevQuickFixProvider.ts` | `src/providers/quickfix-provider.ts` | QuickFix |
| `src/action/autodoc/AutoDocActionExecutor.ts` | `src/actions/auto-comment.ts` | 文档生成 |
| `src/action/autotest/AutoTestActionExecutor.ts` | `src/actions/auto-test.ts` | 测试生成 |
| ~~`src/code-search/indexing/CodebaseIndexer.ts`~~ | ~~暂不实现~~ | ~~代码索引~~ |
| ~~`src/code-search/indexing/LanceDbIndex.ts`~~ | ~~暂不实现~~ | ~~向量数据库~~ |
| `src/prompt-manage/PromptManager.ts` | `src/prompts/prompt-manager.ts` | 提示词管理 |
| `src/prompt-manage/team-prompts/TeamPromptsBuilder.ts` | `src/prompts/team-prompts.ts` | 团队提示 |
| `src/action/devops/CommitMessageGenAction.ts` | `src/integrations/git-integration.ts` | Git 集成 |
| `gui-sidebar/src/` | `webview/src/` | React UI |

---

## 🔧 技术栈对比

| 技术 | 旧版 | 新版 | 迁移策略 |
|------|------|------|---------|
| **语言** | TypeScript | TypeScript + Kotlin/JS | 保持双语言 |
| **依赖注入** | InversifyJS | 简单注入 | 简化架构 |
| **代码解析** | Tree-sitter | 简化版 Tree-sitter / mpp-core | 评估 mpp-core |
| **向量数据库** | LanceDB | 暂不实现 | 后期考虑 MCP 集成 |
| **Embeddings** | Transformers.js | 暂不实现 | 后期考虑 MCP 集成 |
| **LLM 服务** | 内置多provider | mpp-core | 统一使用 mpp-core |
| **UI 框架** | React | React | 保持一致 |
| **提示词引擎** | Velocity | 待定 | 简化为模板字符串 |

---

## 💡 迁移策略

### 1. 直接复用 (推荐)
适用于纯 TypeScript 实现，无 mpp-core 替代品的模块

**复用模块**:
- ✅ UI 组件 (`gui-sidebar/src/components/`)
- ✅ Prompt 管理相关代码

**操作**:
```bash
# 复制文件到新版
cp -r Samples/autodev-vscode/gui-sidebar/src/components mpp-vscode/webview/src/components
```

### 2. 适配重写 (推荐)
适用于有 mpp-core 替代品，但需要 VSCode 接口的模块

**适配模块**:
- ⚡ CodeLens Provider
- ⚡ InlineCompletion Provider
- ⚡ QuickFix Provider
- ⚡ Auto Actions (Comment/Test/Method)

**策略**:
```typescript
// 实现 VSCode 接口
export class CodeLensProvider implements vscode.CodeLensProvider {
  // 调用 mpp-core
  private agent = new JsCodingAgent(llmService);
  
  async provideCodeLenses() {
    // 使用 mpp-core 的能力
    const result = await this.agent.generateCode();
    return result;
  }
}
```

### 3. 完全重写 (谨慎)
适用于架构差异大，无法直接复用的模块

**重写模块**:
- ⚠️ 主入口 (`extension.ts`) - 架构不同
- ⚠️ 配置管理 - 简化设计
- ⚠️ 命令注册 - 统一管理

---

## 🚨 关键技术决策

### 决策 1: 代码解析方案
**选择**: 简化版 Tree-sitter + 正则备用

**理由**:
- mpp-core 的代码解析能力未知
- Tree-sitter 成熟可靠
- 正则可覆盖更多语言

### 决策 2: ~~向量数据库~~
**选择**: 暂不实现

**理由**:
- 增加大量依赖和复杂度
- 非核心功能
- 后期可通过 MCP 协议集成外部服务

### 决策 3: ~~Embeddings~~
**选择**: 暂不实现

**理由**:
- 依赖于向量数据库功能
- 非核心功能

### 决策 4: 提示词引擎
**选择**: 简单模板字符串

**理由**:
- Velocity 太重
- 模板字符串足够用
- 减少依赖

---

## 📦 依赖包清单

### 需要安装的新依赖
```json
{
  "dependencies": {
    "web-tree-sitter": "^0.22.2",    // 代码解析（可选）
    "gray-matter": "^4.0.3",         // Frontmatter 解析（Custom Actions）
    "csv-parse": "^5.5.0"            // 团队术语解析
  },
  "devDependencies": {
    "@types/web-tree-sitter": "^0.22.0"
  },
  "optionalDependencies": {
    "simple-git": "^3.19.0"          // Git 集成
  }
}
```

---

## 🎨 UI 组件复用清单

### 可直接复用的组件 (gui-sidebar → webview)

| 旧版组件 | 用途 | 复用难度 |
|---------|------|---------|
| `StyledMarkdownPreview.tsx` | Markdown 渲染 | 低 ⭐ |
| `MonacoCodeBlock.tsx` | 代码块显示 | 中 ⭐⭐ |
| `ModelSelect.tsx` | 模型选择器 | 低 ⭐ |
| `ProgressBar.tsx` | 进度条 | 低 ⭐ |
| `ContinueInputBox.tsx` | 输入框 | 中 ⭐⭐ |
| `CodeBlockToolbar.tsx` | 代码工具栏 | 低 ⭐ |
| `IndexingProgressBar.tsx` | 索引进度 | 低 ⭐ |

### 需要适配的组件

| 组件 | 原因 | 工作量 |
|------|------|--------|
| `mainInput/TipTapEditor.tsx` | 依赖 TipTap 编辑器 | 高 ⭐⭐⭐ |
| `mainInput/MentionExtension.ts` | 自动补全逻辑 | 中 ⭐⭐ |
| `StepContainer.tsx` | 步骤显示 | 中 ⭐⭐ |

**建议**: 优先复用低难度组件，提升 UI 体验

---

## ✅ 迁移检查清单

### Phase 1: 核心功能
- [ ] CodeLens 显示在函数/类上方
- [ ] 点击 CodeLens 可执行操作
- [ ] 代码补全自动触发
- [ ] AutoComment 生成文档注释
- [ ] AutoTest 生成测试代码
- [ ] Diff 预览可接受/拒绝

### Phase 2: 增强功能
- [ ] 自定义操作可执行
- [ ] QuickFix 修复错误
- [ ] 团队提示词加载

### Phase 3: UI 优化
- [ ] Chat 历史记录
- [ ] 多会话管理
- [ ] 模型选择器
- [ ] Markdown 渲染美观
- [ ] 代码高亮正确

### Phase 4: 集成功能
- [ ] Git 提交消息生成
- [ ] Terminal 错误解释
- [ ] 国际化支持
- [ ] Tutorial 和 Feedback

---

## 📞 需要确认的问题

### 技术确认
1. **mpp-core 的代码解析能力如何?**
   - 能否替代 Tree-sitter?
   - 支持哪些语言?

2. **mpp-core 的补全能力如何?**
   - FIM 支持如何?
   - 性能如何?

3. **mpp-core 是否有向量数据库支持?**
   - 如果有，是否跨平台?
   - 性能如何?

### 产品决策
1. **是否需要支持所有旧版功能?**
   - 有些功能使用频率低
   - 可以先实现核心功能

2. **UI 设计是否沿用旧版?**
   - 可以参考 Continue.dev 等现代 AI 工具
   - 或保持旧版风格

3. **配置项是否需要简化?**
   - 旧版配置项很多
   - 可以简化为必需项

---

## 📚 相关文档

- [详细功能对比分析](./mpp-vscode-migration-analysis.md) - 完整的功能对比
- [详细实施计划](./mpp-vscode-migration-plan.md) - 按天的实施步骤
- [mpp-vscode README](../mpp-vscode/README.md) - 当前项目说明

---

## 🚀 快速开始

### 第一步: 确认 mpp-core 能力
```bash
cd /Volumes/source/ai/autocrud
./gradlew :mpp-core:assembleJsPackage

# 查看导出的 API
cat mpp-core/build/packages/js/kotlin/mpp-core.mjs | grep "export"
```

### 第二步: 安装依赖
```bash
cd mpp-vscode
npm install gray-matter csv-parse
```

### 第三步: 开始开发
```bash
# 创建第一个 Provider
mkdir -p src/providers
touch src/providers/codelens-provider.ts

# 参考旧版实现
open Samples/autodev-vscode/src/action/providers/AutoDevCodeLensProvider.ts
```

---

**最后更新**: 2025-12-04

