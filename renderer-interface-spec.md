# Renderer Interface Specification

## Overview

All renderers in the AutoDev project must implement the unified `JsCodingAgentRenderer` interface defined in Kotlin Multiplatform. This ensures consistency across different platforms and prevents future refactoring issues.

## Core Interface

The interface is defined in:
- **Kotlin**: `mpp-core/src/jsMain/kotlin/cc/unitmesh/agent/RendererExports.kt` (`JsCodingAgentRenderer`)
- **Kotlin Common**: `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/render/CodingAgentRenderer.kt`

## Renderer Implementations

### 1. CliRenderer (TypeScript)
**Location**: `mpp-ui/src/jsMain/typescript/agents/render/CliRenderer.ts`
- **Purpose**: CLI output with colors and formatting
- **Platform**: Node.js CLI
- **Status**: ✅ Implements `JsCodingAgentRenderer`

### 2. ServerRenderer (TypeScript)
**Location**: `mpp-ui/src/jsMain/typescript/agents/render/ServerRenderer.ts`
- **Purpose**: Server-side event rendering for SSE
- **Platform**: Node.js server
- **Status**: ⚠️ Needs to explicitly implement `JsCodingAgentRenderer`
- **Note**: Has additional methods for server-specific events (clone progress, etc.)

### 3. ComposeRenderer (Kotlin)
**Location**: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/ComposeRenderer.kt`
- **Purpose**: Compose UI rendering with timeline
- **Platform**: Desktop (JVM), Android, Web
- **Status**: ✅ Extends `BaseRenderer` which implements `CodingAgentRenderer`

## Interface Contract

All renderers must implement these methods:

### Lifecycle Methods
```kotlin
fun renderIterationHeader(current: Int, max: Int)
fun renderLLMResponseStart()
fun renderLLMResponseChunk(chunk: String)
fun renderLLMResponseEnd()
```

### Tool Execution Methods
```kotlin
fun renderToolCall(toolName: String, paramsStr: String)
fun renderToolResult(toolName: String, success: Boolean, output: String?, fullOutput: String?)
```

### Status and Completion Methods
```kotlin
fun renderTaskComplete()
fun renderFinalResult(success: Boolean, message: String, iterations: Int)
fun renderError(message: String)
fun renderRepeatWarning(toolName: String, count: Int)
```

### Error Recovery Methods
```kotlin
fun renderRecoveryAdvice(recoveryAdvice: String)
```

### Optional Methods
```kotlin
// Only in CodingAgentRenderer (not in JsCodingAgentRenderer yet)
fun renderUserConfirmationRequest(toolName: String, params: Map<String, Any>)
fun addLiveTerminal(sessionId: String, command: String, workingDirectory: String?, ptyHandle: Any?)
```

## Common Patterns

### Devin Block Filtering
All renderers should filter out `<devin>` blocks from LLM output:
```typescript
// TypeScript (BaseRenderer)
protected filterDevinBlocks(content: string): string
protected hasIncompleteDevinBlock(content: string): boolean
```

```kotlin
// Kotlin (BaseRenderer)
protected fun filterDevinBlocks(content: String): String
protected fun hasIncompleteDevinBlock(content: String): Boolean
```

### Tool Call Display
All renderers should format tool calls consistently:
- Extract tool name and parameters
- Show friendly descriptions (e.g., "file reader", "command executor")
- Display file paths or commands being executed

### Tool Result Summary
All renderers should generate concise summaries:
- File operations: "Read X lines", "File created with X lines"
- Shell commands: "Command executed successfully"
- Search operations: "Found X files", "Found X matches"

## Migration Guide

### For New Renderers
1. **TypeScript**: Extend `BaseRenderer` and implement `JsCodingAgentRenderer`
   ```typescript
   export class MyRenderer extends BaseRenderer {
     readonly __doNotUseOrImplementIt: any = {};
     
     renderIterationHeader(current: number, max: number): void { /* ... */ }
     // ... implement all methods
   }
   ```

2. **Kotlin**: Extend `BaseRenderer` and implement `CodingAgentRenderer`
   ```kotlin
   class MyRenderer : BaseRenderer() {
     override fun renderIterationHeader(current: Int, max: Int) { /* ... */ }
     // ... implement all methods
   }
   ```

### For Existing Renderers
1. Check that all interface methods are implemented
2. Ensure consistent behavior across platforms
3. Use `BaseRenderer` helper methods for common operations
4. Add platform-specific methods as extensions, not interface changes

## Checklist for Adding New Methods

When adding a new method to the renderer interface:

1. ✅ Add to `CodingAgentRenderer.kt` (Kotlin common interface)
2. ✅ Add to `JsCodingAgentRenderer` (JS export interface) if needed for TypeScript
3. ✅ Update `JsRendererAdapter` to bridge the interfaces
4. ✅ Add default implementation in `BaseRenderer.kt` if applicable
5. ✅ Update all renderer implementations:
   - `CliRenderer.ts`
   - `ServerRenderer.ts`
   - `ComposeRenderer.kt`
   - Any other custom renderers
6. ✅ Update this documentation

## Anti-Patterns

❌ **DON'T** create separate TypeScript interfaces that duplicate Kotlin interfaces
❌ **DON'T** add methods directly to renderers without updating the interface
❌ **DON'T** use different method signatures across platforms
❌ **DON'T** skip implementing interface methods (use no-op if not applicable)

✅ **DO** use the Kotlin Multiplatform interface as the source of truth
✅ **DO** extend `BaseRenderer` for common functionality
✅ **DO** add platform-specific helpers as protected methods
✅ **DO** keep method signatures consistent across all renderers

## References

- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [Kotlin/JS Interop](https://kotlinlang.org/docs/js-interop.html)
- AGENTS.md - Project-specific guidelines
