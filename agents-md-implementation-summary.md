# AGENTS.md Support Implementation Summary

## ✅ 实现完成

成功为 CodingAgent 添加了 [AGENTS.md 标准](https://agents.md/) 支持，使项目可以通过分层的 markdown 文件自定义 AI 代理的行为和指令。

## 🎯 核心特性

### 1. 层次化文件发现
- 从 Git 根目录向下搜索到当前工作目录
- 按优先级合并多层 AGENTS.md 文件（root → leaf）
- 自动检测 `.git` 目录确定仓库边界

### 2. 优先级系统
支持多种文件名变体，按以下优先级搜索：
1. `AGENTS.override.md` (最高优先级，用于临时覆盖)
2. `AGENTS.md` (标准文件名)
3. Fallback 文件名 (可配置，如 `CLAUDE.md`, `OPENAI.md` 等)

### 3. 安全限制
- 字节限制 (默认 32KB) 防止上下文溢出
- 每个目录只加载一个优先级最高的文件
- 自动截断超出限制的内容

## 📁 文件结构

```
mpp-core/src/
├── commonMain/kotlin/cc/unitmesh/agent/
│   ├── context/
│   │   └── AgentContextDiscovery.kt         # 核心发现逻辑
│   ├── CodingAgentContext.kt                # 增强支持 AGENTS.md
│   └── CodingAgentPromptRenderer.kt         # 已支持 agentRules 变量
└── commonTest/kotlin/cc/unitmesh/agent/
    └── context/
        └── AgentContextDiscoveryTest.kt      # 完整测试套件
```

## 🔑 关键设计决策

### 使用 ToolFileSystem
✅ **正确做法**：复用项目已有的 `ToolFileSystem` 抽象
```kotlin
class AgentContextDiscovery(
    private val fileSystem: ToolFileSystem,
    private val maxBytes: Int = DEFAULT_MAX_BYTES
)
```

❌ **避免做法**：创建新的平台抽象 (AgentContextFileOperations)
- 避免重复抽象
- 保持架构一致性
- 利用已有的多平台实现

### 使用 kotlinx.io.files.Path
使用 Kotlin 原生的 `kotlinx.io.files.Path` 进行路径操作：
```kotlin
private fun normalizePath(path: String): String {
    return Path(path).toString()
}

private fun getParentPath(path: String): String? {
    val parent = Path(path).parent
    return parent?.toString()
}
```

- 跨平台兼容 (JVM, JS, WASM)
- 无需 expect/actual
- 标准化路径处理

## 📚 使用方式

### 基本使用

```kotlin
// 在 CodingAgentContext.fromTask() 中自动加载
val context = CodingAgentContext.fromTask(
    task = task,
    toolList = tools,
    loadAgentRules = true  // 默认开启
)

// context.agentRules 将包含合并后的 AGENTS.md 内容
```

### 高级配置

```kotlin
// 自定义配置
val context = CodingAgentContext.fromTask(
    task = task,
    toolList = tools,
    loadAgentRules = true,
    fallbackFilenames = listOf("CLAUDE.md", "GEMINI.md"),
    maxBytes = 16 * 1024  // 16KB 限制
)
```

### 直接使用 AgentContextDiscovery

```kotlin
val discovery = AgentContextDiscovery(
    fileSystem = DefaultToolFileSystem(),
    maxBytes = 32 * 1024
)

val agentRules = discovery.loadAgentContext(
    workingDir = "/path/to/project/subdir",
    fallbackFilenames = listOf("CLAUDE.md")
)
```

## 📝 AGENTS.md 示例

在项目根目录创建 `AGENTS.md`：

```markdown
# Project Instructions for AI Agent

## Code Style
- Use Kotlin idiomatic patterns
- Follow existing project structure
- Prefer coroutines over callbacks

## Testing
- Write comprehensive unit tests
- Use `runTest` for coroutine tests
- Place test files in `src/commonTest/kotlin`

## Build Commands
\```bash
# Build the project
./gradlew :mpp-core:build

# Run tests
./gradlew :mpp-core:jvmTest
\```

## Platform-Specific Notes
### JVM
- Use Java NIO for file operations
### JS
- Use Node.js APIs via external declarations
```

子目录可以添加特定的 AGENTS.md：
```markdown
# UI Module Specific Instructions

## Compose Multiplatform
- Use Material Design 3
- Follow design system in `design-system/`
- Test on all platforms before committing
```

## 🧪 测试覆盖

7个单元测试验证所有关键场景：

1. ✅ **testNoFilesFound** - 没有文件时返回空字符串
2. ✅ **testSingleAgentsMdFile** - 单个文件正常加载
3. ✅ **testHierarchicalFiles** - 层次化文件按顺序合并
4. ✅ **testOverrideFile** - override 文件优先级
5. ✅ **testFallbackFilenames** - fallback 文件名支持
6. ✅ **testByteLimitEnforcement** - 字节限制正确执行
7. ✅ **testNoGitRoot** - 没有 git 根目录时的行为

所有 JVM 测试通过：
```bash
./gradlew :mpp-core:jvmTest
# BUILD SUCCESSFUL
# 7 tests completed, 0 failed
```

## 🎨 系统提示集成

系统提示模板已支持 `$agentRules` 变量（在 `CodingAgentTemplate.kt` 中）：

```velocity
#if($agentRules && $agentRules != "")

## Project-Specific Instructions
$agentRules
#end
```

当 `agentRules` 不为空时，会自动插入到系统提示中。

## 🔍 参考实现分析

### Codex (Rust)
- 文件：`Samples/codex/codex-rs/core/src/project_doc.rs`
- 特点：
  - 使用 `walkdir` 遍历目录
  - 支持 `AGENTS.override.md` 优先级
  - 32KB 默认字节限制
  - 错误处理完善

### Gemini-CLI (TypeScript)
- 文件：`Samples/gemini-cli/packages/core/src/utils/memoryDiscovery.ts`
- 特点：
  - 多文件名变体支持
  - 并发文件读取
  - 格式化输出带路径标记
  - 完整的 Git 根检测

### 本实现的优势
✅ **Kotlin多平台原生** - 使用 kotlinx.io 和 ToolFileSystem
✅ **类型安全** - 完整的 Kotlin 类型系统
✅ **协程友好** - 使用 suspend 函数
✅ **测试完善** - 7个单元测试覆盖所有场景
✅ **架构一致** - 复用现有抽象而非创建新的

## 📊 性能考虑

- **字节限制**：默认 32KB，防止上下文溢出
- **懒加载**：仅在需要时加载文件
- **单次遍历**：从 CWD 向上遍历一次即可找到所有文件
- **优先级短路**：找到高优先级文件后跳过低优先级

## 🚀 后续优化建议

1. **缓存机制**：对于频繁调用，可以缓存已加载的内容
2. **文件监听**：检测 AGENTS.md 变更并自动重载
3. **模板变量**：支持 AGENTS.md 中的变量替换
4. **条件包含**：支持基于环境/平台的条件指令

## 📖 相关文档

- AGENTS.md 标准：https://agents.md/
- 详细实现分析：`docs/agents-md-implementation.md`
- CodingAgent 文档：`mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/CodingAgent.kt`
- ToolFileSystem 抽象：`mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/tool/filesystem/ToolFileSystem.kt`

---

**实现完成时间**：2025-11-18
**测试状态**：✅ 所有 JVM 测试通过
**多平台支持**：JVM, JS, WASM

