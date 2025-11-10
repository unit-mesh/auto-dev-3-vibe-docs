# Renderer 继承关系重构总结

## 问题
三个 TypeScript renderer（CliRenderer、ServerRenderer、TuiRenderer）没有统一的继承关系，导致代码重复和维护困难。

## 解决方案
建立清晰的继承层次：

```
CodingAgentRenderer (Kotlin interface)
         ↓
  JsCodingAgentRenderer (JS Export)
         ↓
    BaseRenderer (abstract class)
         ↓
  ┌──────┴──────┬──────────────┐
  │             │              │
CliRenderer  ServerRenderer  (TuiRenderer)
```

**注**: TuiRenderer 由于使用 React/Ink 架构，直接实现接口而不继承 BaseRenderer。

## 实现的继承关系

### TypeScript 端

#### BaseRenderer (抽象基类)
**位置**: `mpp-ui/src/jsMain/typescript/agents/render/BaseRenderer.ts`

**提供的功能**:
- `filterDevinBlocks()` - 过滤 devin 标签
- `hasIncompleteDevinBlock()` - 检测不完整的 devin 块
- `calculateSimilarity()` - 计算文本相似度（用于检测重复推理）
- `cleanNewlines()` - 清理多余换行
- `baseLLMResponseStart()` - LLM 响应开始的通用逻辑
- `baseLLMResponseEnd()` - LLM 响应结束的通用逻辑

**抽象方法**:
- `renderIterationHeader()`
- `renderLLMResponseStart()`
- `renderLLMResponseChunk()`
- `renderLLMResponseEnd()`
- `renderToolCall()`
- `renderToolResult()`
- `renderTaskComplete()`
- `renderFinalResult()`
- `renderError()`
- `renderRepeatWarning()`
- `renderRecoveryAdvice()`
- `outputContent()` - 平台特定的输出实现
- `outputNewline()` - 平台特定的换行实现

#### CliRenderer extends BaseRenderer
**位置**: `mpp-ui/src/jsMain/typescript/agents/render/CliRenderer.ts`

**特性**:
- 终端彩色输出（使用 chalk）
- 代码语法高亮
- 格式化工具调用和结果显示
- 继承所有 BaseRenderer 的辅助方法

**平台特定实现**:
```typescript
protected outputContent(content: string): void {
  process.stdout.write(chalk.white(content));
}

protected outputNewline(): void {
  console.log();
}
```

#### ServerRenderer extends BaseRenderer
**位置**: `mpp-ui/src/jsMain/typescript/agents/render/ServerRenderer.ts`

**特性**:
- SSE（Server-Sent Events）事件处理
- Git clone 进度显示
- 服务器端日志格式化
- 继承所有 BaseRenderer 的辅助方法

**额外方法** (不属于核心接口):
- `renderCloneProgress()` - Git clone 进度
- `renderCloneLog()` - Git clone 日志
- `renderEvent()` - SSE 事件分发器

**平台特定实现**:
```typescript
protected outputContent(content: string): void {
  process.stdout.write(content);
}

protected outputNewline(): void {
  console.log();
}
```

#### TuiRenderer (直接实现接口)
**位置**: `mpp-ui/src/jsMain/typescript/agents/render/TuiRenderer.ts`

**特性**:
- React/Ink 组件架构
- ModeContext 状态管理
- 智能输出截断
- 直接实现 `JsCodingAgentRenderer` 接口

**不继承 BaseRenderer 的原因**:
- 使用 React 组件模型，不是直接输出
- 通过 `ModeContext` 管理状态，而非直接写入 stdout
- 渲染逻辑完全不同（React 声明式 vs 命令式输出）

### Kotlin 端

#### BaseRenderer (抽象基类)
**位置**: `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/render/BaseRenderer.kt`

**功能**: 与 TypeScript BaseRenderer 镜像相同的功能

#### ComposeRenderer extends BaseRenderer
**位置**: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/ComposeRenderer.kt`

**特性**:
- Compose 状态管理
- Timeline 数据结构
- 实时 UI 更新
- 继承所有 BaseRenderer 的辅助方法

## 重构前后对比

### 重构前
- ❌ 三个 renderer 各自重复实现 devin block 过滤逻辑
- ❌ 相似度检测代码重复
- ❌ 没有统一的 buffer 管理
- ❌ 难以维护和同步更新

### 重构后
- ✅ 所有通用逻辑集中在 BaseRenderer
- ✅ CliRenderer 和 ServerRenderer 继承 BaseRenderer，复用代码
- ✅ 统一的 `reasoningBuffer`, `lastOutputLength` 等字段
- ✅ 一处修改，所有 renderer 受益
- ✅ 明确的继承层次，易于理解和维护

## 代码复用示例

### 重复代码消除
**之前**: 每个 renderer 都有自己的 `filterDevinBlocks()`

**之后**: 从 BaseRenderer 继承
```typescript
// CliRenderer
this.filterDevinBlocks(this.reasoningBuffer)

// ServerRenderer  
this.filterDevinBlocks(this.reasoningBuffer)

// 都使用 BaseRenderer 的同一个实现
```

### 状态管理统一
**之前**: 每个 renderer 有不同的 buffer 变量名
- CliRenderer: `reasoningBuffer`
- ServerRenderer: `llmBuffer`

**之后**: 统一使用 BaseRenderer 的 `reasoningBuffer`
```typescript
// 所有 renderer 都使用相同的字段名
protected reasoningBuffer: string = '';
protected lastOutputLength: number = 0;
```

## 测试验证

### 编译检查
```bash
cd /Volumes/source/ai/autocrud/mpp-ui
npx tsc --noEmit
# ✅ 无错误
```

### 运行时测试
```bash
node dist/jsMain/typescript/index.js code --task "list files" -p /path/to/project
```

**验证输出**:
- ✅ 💭 LLM 思考emoji正常显示
- ✅ ● 工具调用格式正确
- ✅ ⎿ 工具结果摘要显示
- ✅ ✓ 任务完成标记
- ✅ ✅ 最终结果显示

## 维护指南

### 添加新的通用功能
1. 在 `BaseRenderer.ts` 中添加 protected 方法
2. 在 Kotlin `BaseRenderer.kt` 中添加对应实现（保持一致）
3. 子类自动继承，无需修改

### 添加新的抽象方法（接口变更）
1. 更新 `CodingAgentRenderer.kt`（核心接口）
2. 更新 `JsCodingAgentRenderer`（JS 导出）
3. 更新 `JsRendererAdapter`（桥接）
4. 在 `BaseRenderer.ts` 中声明为抽象方法
5. 在所有子类中实现:
   - `CliRenderer.ts`
   - `ServerRenderer.ts`
   - `TuiRenderer.ts`
   - `ComposeRenderer.kt`

### 平台特定实现
如果需要平台特定的行为：
- 在子类中重写相关方法
- 或添加新的 private/protected 方法
- 不要修改基类的公共接口

## 收益总结

1. **代码复用**: 约 150 行重复代码被消除
2. **维护性**: 通用逻辑只需维护一处
3. **一致性**: 所有 renderer 行为保持一致
4. **扩展性**: 新增 renderer 只需继承 BaseRenderer
5. **类型安全**: TypeScript 编译时检查继承关系
6. **文档化**: 清晰的继承层次易于理解

## 相关文档
- `docs/renderer-interface-spec.md` - 接口规范
- `docs/renderer-unification-summary.md` - 统一化总结
- `AGENTS.md` - 项目开发规范
