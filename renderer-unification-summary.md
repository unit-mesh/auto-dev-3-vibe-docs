# Renderer Interface Unification - Summary

## 问题
三个 renderer 实现（CliRenderer.ts、ServerRenderer.ts、ComposeRenderer.kt）没有明确统一的接口规范，存在未来重构时遗漏修改的风险。

## 解决方案
使用 **Kotlin Multiplatform 的 `JsCodingAgentRenderer` 接口** 作为唯一的 source of truth，所有 renderer 都必须实现这个接口。

## 统一接口来源

| 层级 | 文件位置 | 说明 |
|------|---------|------|
| **Core Interface** | `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/render/CodingAgentRenderer.kt` | Kotlin 通用接口，定义完整的 renderer 契约 |
| **JS Export** | `mpp-core/src/jsMain/kotlin/cc/unitmesh/agent/RendererExports.kt` | `JsCodingAgentRenderer` - 导出给 TypeScript 使用 |
| **Base Implementation (Kotlin)** | `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/render/BaseRenderer.kt` | Kotlin 端基础实现，提供通用功能（devin block 过滤、相似度检测等）|
| **Base Implementation (TypeScript)** | `mpp-ui/src/jsMain/typescript/agents/render/BaseRenderer.ts` | TypeScript 端基础实现，镜像 Kotlin BaseRenderer 的功能 |

## Renderer 实现对照

| Renderer | 位置 | 平台 | 继承关系 | 状态 |
|----------|------|------|---------|------|
| **CliRenderer** | `mpp-ui/src/jsMain/typescript/agents/render/CliRenderer.ts` | Node.js CLI | `extends BaseRenderer` → `JsCodingAgentRenderer` | ✅ 已更新 |
| **ServerRenderer** | `mpp-ui/src/jsMain/typescript/agents/render/ServerRenderer.ts` | Node.js Server | `extends BaseRenderer` → `JsCodingAgentRenderer` | ✅ 已更新 |
| **TuiRenderer** | `mpp-ui/src/jsMain/typescript/agents/render/TuiRenderer.ts` | Node.js TUI (React/Ink) | 直接实现 `JsCodingAgentRenderer` | ⚠️ 特殊架构 |
| **ComposeRenderer** | `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/ComposeRenderer.kt` | Desktop/Android/Web | `extends BaseRenderer` → `CodingAgentRenderer` | ✅ 已确认 |

## 核心接口方法

所有 renderer 都必须实现以下方法：

### 生命周期方法
```typescript
renderIterationHeader(current: number, max: number): void
renderLLMResponseStart(): void
renderLLMResponseChunk(chunk: string): void
renderLLMResponseEnd(): void
```

### 工具执行方法
```typescript
renderToolCall(toolName: string, paramsStr: string): void
renderToolResult(toolName: string, success: boolean, output: string | null, fullOutput?: string | null): void
```

### 状态和完成方法
```typescript
renderTaskComplete(): void
renderFinalResult(success: boolean, message: string, iterations: number): void
renderError(message: string): void
renderRepeatWarning(toolName: string, count: int): void
```

### 错误恢复方法
```typescript
renderRecoveryAdvice(recoveryAdvice: string): void
```

## 特殊说明

### TuiRenderer 的特殊情况
TuiRenderer 使用 React/Ink 架构，通过 `ModeContext` 进行状态管理。虽然它实现了 `JsCodingAgentRenderer` 接口，但由于其特殊的渲染机制（基于 React 组件），它直接实现接口而不继承 `BaseRenderer`。未来如果需要，可以考虑提取 BaseRenderer 的辅助方法为独立的工具函数供 TuiRenderer 使用。
ServerRenderer 有一些服务器特定的方法（不属于核心接口）：
- `renderCloneProgress(stage: string, progress?: number)` - Git clone 进度
- `renderCloneLog(message: string, isError?: boolean)` - Git clone 日志
- `renderEvent(event: AgentEvent)` - SSE 事件分发器

这些方法是平台特定的扩展，不影响核心接口的统一性。

### ComposeRenderer 的额外功能
ComposeRenderer 有额外的状态管理和 UI 交互方法：
- `addUserMessage(content: string)` - 添加用户消息
- `clearMessages()` - 清空消息
- `openFileViewer(filePath: string)` - 打开文件查看器
- `forceStop()` - 强制停止

这些是 Compose UI 特定的交互功能，不属于渲染接口本身。

## 维护规则

### ✅ 正确做法
1. 修改接口时，先更新 `CodingAgentRenderer.kt`
2. 如果 TypeScript 需要，同步更新 `JsCodingAgentRenderer`
3. 更新 `JsRendererAdapter` 桥接适配器
4. 更新所有三个 renderer 实现
5. 运行测试确保没有遗漏

### ❌ 错误做法
1. ~~创建新的 TypeScript 接口文件~~ - 不要重复定义接口
2. ~~直接在 renderer 中添加新方法~~ - 必须先更新接口
3. ~~不同平台使用不同的方法签名~~ - 必须保持一致

## 相关文档
- `docs/renderer-interface-spec.md` - 详细的接口规范和最佳实践
- `AGENTS.md` - 项目级别的开发规范

## 验证清单
- [x] CliRenderer 实现 `JsCodingAgentRenderer`
- [x] ServerRenderer 实现 `JsCodingAgentRenderer`
- [x] ComposeRenderer 实现 `CodingAgentRenderer`
- [x] 所有方法签名一致
- [x] 删除了重复的接口定义
- [x] 添加了接口文档说明
- [x] 编译无错误
