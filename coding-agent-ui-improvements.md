# Coding Agent UI/UX Improvements

## 概述

针对 AI Coding Agent 的输出体验进行了全面改进，提升用户体验和信息可读性。

## 改进内容

### 1. **日志级别控制** ✅

**问题**: 输出了大量对用户无意义的调试信息
```
[INFO] [TextSegmentProcessor] Processing TextSegment
[INFO] [TextSegmentProcessor] Processing Token(NEWLINE)
```

**解决方案**:
- 在 `CompilerLogger` 中添加 `debug()` 方法和 `enableDebug` 标志
- 将处理器的日志从 `info` 改为 `debug` 级别
- 默认关闭 debug 日志，只显示对用户有用的信息

**修改文件**:
- `mpp-core/src/commonMain/kotlin/cc/unitmesh/devins/compiler/context/CompilerContext.kt`
- `mpp-core/src/commonMain/kotlin/cc/unitmesh/devins/compiler/processor/DevInsNodeProcessor.kt`

### 2. **美化输出工具** ✅

创建了 `OutputFormatter` 类，提供以下功能：

#### 彩色输出
- ✅ Success (绿色)
- ❌ Error (红色)  
- ⚠️ Warning (黄色)
- ℹ️ Info (蓝色)
- 🐛 Debug (灰色，quiet 模式下隐藏)

#### 结构化输出
```
═════════════════════════════════════════════════════════════
  AutoDev Coding Agent
═════════════════════════════════════════════════════════════

▶ Initializing Workspace
✓ Workspace initialized

▶ Executing Task
[1/10] Analyzing and executing...
✓ Executed read-file
✓ Executed write-file
```

#### Diff 展示
```
────────────────────────────────────────────────────────────
✨ CREATE src/main/kotlin/Main.kt
────────────────────────────────────────────────────────────
fun main() {
    println("Hello, World!")
}
```

对于文件更新，会显示统一 diff：
```
────────────────────────────────────────────────────────────
📝 UPDATE src/main/kotlin/Main.kt
────────────────────────────────────────────────────────────
@@ -1,3 +1,4 @@
 fun main() {
-    println("Hello, World!")
+    println("Hello, World!")
+    println("Welcome to AutoDev!")
 }
```

#### 总结统计
```
═════════════════════════════════════════════════════════════
  Summary
═════════════════════════════════════════════════════════════
Iterations:  5
Total Edits: 3
  ✨ Creates:  2
  📝 Updates:  1
  🗑️  Deletes:  0
Duration:    12.34s
```

**新增文件**:
- `mpp-ui/src/jsMain/typescript/utils/outputFormatter.ts`

### 3. **简化 Agent 输出** ✅

**改进前**:
```
🔧 Executing DevIns:
/read-file path="build.gradle.kts"

✓ DevIns executed successfully
Output:
plugins {
    java
    id("org.springframework.boot") version "2.7.10"
... (500+ lines)
```

**改进后**:
```
✓ Executed read-file
```

详细输出移到 debug 模式，通过 `--verbose` 标志开启。

**修改文件**:
- `mpp-ui/src/jsMain/typescript/services/CodingAgentService.ts`

### 4. **CLI 标志** ✅

添加了新的命令行选项：

```bash
# 安静模式 - 只显示重要信息
node dist/index.js code --path ./project --task "..." --quiet

# 详细模式 - 显示所有调试信息
node dist/index.js code --path ./project --task "..." --verbose
```

**修改文件**:
- `mpp-ui/src/jsMain/typescript/index.tsx`

### 5. **依赖更新** ✅

添加了必要的 npm 包：
- `diff`: ^7.0.0 - 用于生成和显示文件 diff
- `@types/diff`: ^6.0.0 - TypeScript 类型定义

**修改文件**:
- `mpp-ui/package.json`

## 使用示例

### 默认模式（推荐）
```bash
cd mpp-ui
npm run build:ts
node dist/index.js code --path /path/to/project --task "Create a hello world"
```

输出简洁、结构化，只显示关键信息。

### 安静模式
```bash
node dist/index.js code --path /path/to/project --task "..." --quiet
```

只显示最终结果和错误，适合 CI/CD 环境。

### 详细模式
```bash
node dist/index.js code --path /path/to/project --task "..." --verbose
```

显示所有调试信息，包括：
- LLM 响应的实时流式输出
- DevIns 命令的详细输出
- 处理器的调试日志

## 对比效果

### Before（改进前）
```
🤖 Starting AutoDev Agent...
📁 Project: /Users/phodal/IdeaProjects/untitled
📝 Task: Create a hello world

✓ Workspace initialized: /Users/phodal/IdeaProjects/untitled

--- Iteration 1/10 ---
[INFO] [TextSegmentProcessor] Processing TextSegment
[INFO] [TextSegmentProcessor] Processing Token(NEWLINE)
[INFO] [TextSegmentProcessor] Processing TextSegment
I'll help you create a hello world application...

🔧 Executing DevIns:
/read-file path="build.gradle.kts"

✓ DevIns executed successfully
Output:
plugins {
    java
... (大量输出)

============================================================
✅ Task completed successfully in 10 iterations
Total steps: 10
Total edits: 1
============================================================
```

### After（改进后）
```
═════════════════════════════════════════════════════════════
  AutoDev Coding Agent
═════════════════════════════════════════════════════════════
ℹ Project: /Users/phodal/IdeaProjects/untitled
ℹ Task: Create a hello world

▶ Initializing Workspace
✓ Workspace initialized

▶ Executing Task
[1/10] Analyzing and executing...
✓ Executed read-file
✓ Executed glob
[2/10] Analyzing and executing...
✓ Executed write-file

▶ File Changes
────────────────────────────────────────────────────────────
✨ CREATE src/main/kotlin/Main.kt
────────────────────────────────────────────────────────────
fun main() {
    println("Hello, World!")
}

═════════════════════════════════════════════════════════════
  Summary
═════════════════════════════════════════════════════════════
Iterations:  2
Total Edits: 1
  ✨ Creates:  1
  📝 Updates:  0
  🗑️  Deletes:  0
Duration:    5.67s

✓ Task completed successfully
```

## 技术细节

### 日志架构

```
CompilerLogger (mpp-core)
├── debug()    [隐藏，除非 enableDebug=true]
├── info()     [显示]
├── warn()     [显示，黄色]
└── error()    [显示，红色]

OutputFormatter (TypeScript)
├── debug()    [隐藏，除非 quiet=false]
├── info()     [显示]
├── success()  [显示，绿色]
├── warn()     [显示，黄色]
└── error()    [显示，红色]
```

### 控制流

```
CLI Flag (--quiet/--verbose)
    ↓
CodingAgentService(quiet)
    ↓
OutputFormatter(quiet)
    ↓
Terminal Output
```

## 后续改进建议

### 1. 交互式进度条
使用 `cli-progress` 或 `ora` 显示动画进度：
```
⠋ [2/10] Analyzing project structure...
```

### 2. 代码语法高亮
使用 `highlight.js` 或 `prism.js` 对代码片段进行语法高亮：
```typescript
import hljs from 'highlight.js';

const highlighted = hljs.highlight(code, { language: 'kotlin' }).value;
```

### 3. 富文本 Diff
使用 `diff2html` 生成更美观的 diff：
```typescript
import { Diff2Html } from 'diff2html';

const htmlDiff = Diff2Html.getPrettyHtml(diffString, {
  outputFormat: 'line-by-line'
});
```

### 4. 智能错误恢复
当命令失败时，提供建议：
```
✗ Failed shell: ./gradlew build
  → File not found: ./gradlew
  💡 Suggestion: Try 'gradle build' or check if project uses Maven instead
```

### 5. 日志文件
将详细日志写入文件，便于调试：
```bash
node dist/index.js code --task "..." --log-file agent.log
```

## 测试

运行以下命令测试改进后的输出：

```bash
# 1. 构建
cd /Volumes/source/ai/autocrud
./gradlew :mpp-core:assembleJsPackage
cd mpp-ui
npm run build:ts

# 2. 测试默认模式
node dist/index.js code --path /Users/phodal/IdeaProjects/untitled --task "Create a hello world"

# 3. 测试安静模式
node dist/index.js code --path /Users/phodal/IdeaProjects/untitled --task "Create a hello world" --quiet

# 4. 测试详细模式
node dist/index.js code --path /Users/phodal/IdeaProjects/untitled --task "Create a hello world" --verbose
```

## 贡献者

- 改进设计和实现：2025-11-01

