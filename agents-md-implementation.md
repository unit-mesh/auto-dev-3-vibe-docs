# AGENTS.md Implementation for CodingAgent

## 概述

本实现为 CodingAgent 添加了 [AGENTS.md 标准](https://agents.md/) 支持，允许项目通过分层的 markdown 文件自定义 AI 代理的行为和规则。

## 设计思路

### 1. 参考实现分析

通过分析 Samples 中的两个参考实现：

#### Codex (Rust 实现)
- **文件**: `Samples/codex/codex-rs/core/src/project_doc.rs`
- **特点**:
  - 从 Git 根目录向下搜索到当前工作目录
  - 支持 `AGENTS.override.md` 优先级机制
  - 字节限制 (32KB 默认) 防止上下文溢出
  - 支持 fallback 文件名配置

#### Gemini-CLI (TypeScript 实现)
- **文件**: `Samples/gemini-cli/packages/core/src/utils/memoryDiscovery.ts`
- **特点**:
  - 层次化文件发现（向上遍历查找 .git）
  - 多文件名变体支持 (GEMINI.md, AGENTS.md, CLAUDE.md)
  - 并发文件读取优化
  - 格式化输出带路径标记

### 2. 架构设计

```
AgentContextDiscovery
    ↓ uses
ToolFileSystem (多平台文件抽象)
    ↓ implementations
DefaultToolFileSystem (kotlinx-io)
```

**关键决策**: 
- **复用 `ToolFileSystem`**: 而不是创建新的平台抽象，保持架构一致性
- **使用 kotlinx-io**: 原生多平台支持，无需 expect/actual
- **字符串路径操作**: kotlinx.io.files.Path 提供跨平台路径处理

## 实现细节

### 核心类: `AgentContextDiscovery`

```kotlin
class AgentContextDiscovery(
    private val fileSystem: ToolFileSystem,
    private val maxBytes: Int = 32 * 1024  // 32KB default
)
```

#### 主要功能

1. **文件发现** (`discoverContextFiles`)
   - 从当前目录向上查找 Git 根目录 (`.git` 标记)
   - 构建从根到当前目录的搜索链
   - 按优先级搜索候选文件名：
     1. `AGENTS.override.md` (本地覆盖，不提交)
     2. `AGENTS.md` (标准文件名)
     3. Fallback 文件名 (CLAUDE.md, GEMINI.md, .agents.md)

2. **文件读取与合并** (`readAndConcatenate`)
   - 按层次顺序读取文件 (root → leaf)
   - 应用字节限制，超出部分截断
   - 格式化输出带文件路径标记：
     ```
     --- AGENTS.md from: path/to/file ---
     [content]
     --- End of AGENTS.md from: path/to/file ---
     ```

3. **路径操作**
   - 使用 `kotlinx.io.files.Path` 进行跨平台路径处理
   - 相对路径计算用于显示
   - 父目录遍历直到找到 Git 根或系统根

### 集成到 CodingAgentContext

```kotlin
// CodingAgentContext.kt
suspend fun fromTask(
    task: AgentTask,
    toolList: List<ExecutableTool<*, *>>,
    fileSystem: ToolFileSystem? = null,
    loadAgentRules: Boolean = true,
    ...
): CodingAgentContext {
    val agentRules = if (loadAgentRules) {
        val fs = fileSystem ?: DefaultToolFileSystem(projectPath = task.projectPath)
        val discovery = AgentContextDiscovery(fs, maxBytes)
        discovery.loadAgentContext(task.projectPath, fallbackFilenames)
    } else ""
    
    return CodingAgentContext(..., agentRules = agentRules)
}
```

### 模板支持

```velocity
## Project-Specific Rules
#if ($agentRules)
$agentRules
#end
```

系统提示模板 (CodingAgentTemplate) 已包含对 `agentRules` 变量的支持，当发现 AGENTS.md 文件时自动注入。

## 使用方式

### 1. 项目根目录

```markdown
# /project/AGENTS.md

## Code Style
- Use TypeScript for all new code
- Follow existing project structure
...
```

### 2. 子目录特定规则

```markdown
# /project/backend/AGENTS.md

## Backend Rules
- Use Express.js middleware pattern
- Validate all API inputs
...
```

### 3. 本地开发覆盖

```markdown
# /project/AGENTS.override.md (不提交到 Git)

## Local Development
- Use debug logging
- Skip authentication for testing
...
```

### 4. 兼容性文件名

如果项目已经使用其他文件名：
- `CLAUDE.md` - Claude Code 兼容
- `GEMINI.md` - Gemini CLI 兼容
- `.agents.md` - 隐藏文件变体

这些会自动作为 fallback 搜索。

## 配置选项

```kotlin
CodingAgentContext.fromTask(
    task = task,
    toolList = tools,
    loadAgentRules = true,           // 启用/禁用 AGENTS.md 加载
    fallbackFilenames = listOf(      // 自定义 fallback 文件名
        "CLAUDE.md",
        "TEAM_RULES.md"
    ),
    maxBytes = 64 * 1024            // 自定义字节限制
)
```

## 文件优先级

在每个目录层级中，按以下顺序查找（找到第一个即停止）：

1. `AGENTS.override.md` ⚠️ **最高优先级**
2. `AGENTS.md` ⭐ **标准文件名**
3. `CLAUDE.md` 🔄 **兼容性**
4. `.agents.md` 🔒 **隐藏变体**
5. `GEMINI.md` 🔄 **兼容性**

## 性能考虑

1. **字节限制**: 默认 32KB，防止上下文窗口溢出
2. **文件缓存**: 通过 ToolFileSystem 的实现处理
3. **懒加载**: 只在创建 Context 时读取
4. **错误容忍**: 读取失败不影响 Agent 启动

## 测试

完整的单元测试套件：`AgentContextDiscoveryTest.kt`

测试覆盖：
- ✅ 无文件场景
- ✅ 单文件读取
- ✅ 层次化文件合并
- ✅ Override 优先级
- ✅ Fallback 文件名
- ✅ 字节限制截断
- ✅ 禁用加载 (maxBytes = 0)

## 示例项目

查看 `docs/test-scripts/AGENTS.md.example` 获取完整的示例配置。

## 与其他实现的对比

| 特性 | Codex (Rust) | Gemini-CLI (TS) | 本实现 (Kotlin) |
|------|--------------|-----------------|----------------|
| 平台支持 | macOS/Linux | Node.js | JVM/JS/WasmJS/Android |
| 文件系统抽象 | 自定义 | Node fs | ToolFileSystem |
| Git 根检测 | ✅ | ✅ | ✅ |
| Override 支持 | ✅ | ❌ | ✅ |
| Fallback 文件名 | ✅ | ✅ | ✅ |
| 字节限制 | ✅ (32KB) | ✅ | ✅ (可配置) |
| 并发读取 | ❌ | ✅ | ToolFileSystem 决定 |
| 路径标记输出 | ❌ | ✅ | ✅ |

## 未来改进

1. **缓存机制**: 文件内容缓存，避免重复读取
2. **监听更新**: 文件变化时自动重新加载
3. **模式验证**: AGENTS.md 文件的结构验证
4. **IDE 集成**: 提供 IDE 插件支持编辑和预览
5. **模板变量**: 支持 AGENTS.md 中的动态变量替换

## 相关文件

- `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/context/AgentContextDiscovery.kt`
- `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/CodingAgentContext.kt`
- `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/CodingAgentTemplate.kt`
- `mpp-core/src/commonTest/kotlin/cc/unitmesh/agent/context/AgentContextDiscoveryTest.kt`
- `docs/test-scripts/AGENTS.md.example`

## 参考资源

- [AGENTS.md 官方标准](https://agents.md/)
- [Codex 实现](https://github.com/anthropics/codex)
- [Gemini-CLI 实现](https://github.com/google/gemini-cli)

