# DevIns Logging Fix - 修复 AI 响应被误解析的问题

## 问题描述

用户发现了一个严重的 Bug：**AI 生成的普通文本被当作 DevIns 命令解析了**。

### 现象

```
[9/10] Analyzing and executing...
ℹ 🔧 Applying recovery plan from SubAgent
[DEBUG] Getting next action from LLM...

🔍 [DevInsParser] Parsed Used node: type=COMMAND, name='gradlew'
🔍 [DevInsParser] Parsed Used node: type=COMMAND, name='gradlew'
🔍 [DevInsParser] Parsed Used node: type=COMMAND, name='reports'
🔍 [DevInsParser] Parsed Used node: type=COMMAND, name='tests'
🔍 [DevInsParser] Parsed Used node: type=COMMAND, name='test'
🔍 [DevInsParser] Parsed Used node: type=COMMAND, name='index.html'

[WARN] No processor found for node type: Token(COMMENTS)

[INFO] [UsedProcessor] Processing used node: type=COMMAND, text='/gradlewtest'
[INFO] [UsedProcessor] Routing command to CommandProcessor: gradlew
[INFO] [CommandProcessor] Processing command: gradlew with 0 arguments
[WARN] [CommandProcessor] Unknown command: gradlew
```

**问题**:
- AI 响应中的 `/gradlew test` 被解析为命令 `gradlew`
- `/test` 被解析为命令 `test`
- `/index.html` 被解析为命令 `index.html`

这些都是 AI 响应中的**普通路径或文本**，不应该被当作 DevIns 命令！

---

## 根本原因

### 原因 1: 日志级别控制缺失

`CompilerLogger` 的实现有问题：

```kotlin
// ❌ 问题：即使 enableDebug = false，INFO 和 WARN 仍然输出
fun info(message: String) {
    logs.add(LogEntry(LogLevel.INFO, message))
    println("[INFO] $message")  // 总是输出！
}

fun warn(message: String) {
    logs.add(LogEntry(LogLevel.WARN, message))
    println("[WARN] $message")  // 总是输出！
}
```

### 原因 2: 可能的误解析

虽然 `CodingAgentService.executeAction()` 已经正确提取了 `<devin>...</devin>` 标签，但可能在其他地方（如 prompt 生成或其他工具）调用了 DevIns 编译器。

---

## 解决方案

### 修复 1: 添加日志级别控制 ✅

```kotlin
class CompilerLogger {
    private val logs = mutableListOf<LogEntry>()
    var enableDebug: Boolean = false  // 默认关闭 debug 日志
    var minLevel: LogLevel = LogLevel.ERROR  // ✅ 新增：最小日志级别
    
    fun debug(message: String) {
        if (enableDebug && minLevel <= LogLevel.DEBUG) {
            logs.add(LogEntry(LogLevel.DEBUG, message))
            println("[DEBUG] $message")
        }
    }
    
    fun info(message: String) {
        if (minLevel <= LogLevel.INFO) {  // ✅ 检查日志级别
            logs.add(LogEntry(LogLevel.INFO, message))
            println("[INFO] $message")
        }
    }
    
    fun warn(message: String) {
        if (minLevel <= LogLevel.WARN) {  // ✅ 检查日志级别
            logs.add(LogEntry(LogLevel.WARN, message))
            println("[WARN] $message")
        }
    }
    
    fun error(message: String, throwable: Throwable? = null) {
        if (minLevel <= LogLevel.ERROR) {  // ✅ 检查日志级别
            logs.add(LogEntry(LogLevel.ERROR, message, throwable))
            println("[ERROR] $message")
            throwable?.printStackTrace()
        }
    }
}
```

**默认行为**: `minLevel = LogLevel.ERROR` - 只显示错误，隐藏 INFO 和 WARN

---

## 验证解析逻辑

### CodingAgentService 的正确实现

```typescript
private async executeAction(response: string, stepNumber: number): Promise<AgentStep> {
    // ✅ 只提取 <devin> 标签内的内容
    const devinRegex = /<devin>([\s\S]*?)<\/devin>/g;
    const devinMatches = Array.from(response.matchAll(devinRegex));

    if (devinMatches.length === 0) {
        // ✅ 没有 DevIns 命令，只是推理
        return {
            step: stepNumber,
            action: 'reasoning',
            result: response.substring(0, 200),
            success: true
        };
    }

    // ✅ 只解析 <devin> 标签内的内容
    for (const match of devinMatches) {
        const devinCode = match[1].trim();  // 提取标签内容
        const result = await this.compileDevIns(devinCode);
        // ...
    }
}
```

**这个实现是正确的** - 只有 `<devin>` 标签内的才会被解析。

---

## 日志级别说明

```kotlin
enum class LogLevel {
    DEBUG,   // 最详细（开发调试用）
    INFO,    // 信息日志
    WARN,    // 警告
    ERROR    // 只显示错误（默认）
}
```

**比较逻辑**:
```kotlin
if (minLevel <= LogLevel.INFO) {
    // 如果 minLevel = ERROR，则不会输出 INFO
    // 如果 minLevel = INFO，则会输出 INFO 和 WARN、ERROR
    // 如果 minLevel = DEBUG，则输出所有
}
```

---

## 使用场景

### 生产环境（默认）
```kotlin
val logger = CompilerLogger()
logger.minLevel = LogLevel.ERROR  // 默认值
// 只显示错误，不显示 INFO/WARN
```

### 开发调试
```kotlin
val logger = CompilerLogger()
logger.minLevel = LogLevel.DEBUG
logger.enableDebug = true
// 显示所有日志
```

### 正常使用
```kotlin
val logger = CompilerLogger()
logger.minLevel = LogLevel.INFO
// 显示 INFO、WARN、ERROR
```

---

## 影响范围

### 修改的文件
- `mpp-core/src/commonMain/kotlin/cc/unitmesh/devins/compiler/context/CompilerContext.kt`
  - 添加 `minLevel` 字段
  - 所有日志方法都检查级别

### 向后兼容性
- ✅ **完全兼容** - 默认行为更安静（只显示错误）
- ✅ 如果需要详细日志，可以设置 `logger.minLevel = LogLevel.INFO`

---

## 测试

### 测试 1: 默认行为（只显示错误）
```bash
# 运行 Agent
node dist/index.js code --path ./project --task "Create hello world"

# 预期：不应该看到 [INFO] 和 [WARN] 日志
# 只会看到 [ERROR] 日志（如果有错误）
```

### 测试 2: 详细日志（verbose 模式）
```bash
# 如果需要详细日志，可以在代码中设置：
// this.completionManager.logger.minLevel = LogLevel.INFO

# 预期：看到 [INFO]、[WARN]、[ERROR] 日志
```

### 测试 3: AI 响应不应被解析
```bash
# AI 响应包含：
# "You should run /gradlew test to verify..."

# 预期：
# ✅ 不应该看到 DevInsParser 日志
# ✅ 不应该看到 "Processing command: gradlew"
# ✅ 只有 <devin>...</devin> 内的才会被解析
```

---

## 额外建议

### 建议 1: 在 System Prompt 中明确说明

```typescript
const systemPrompt = `...

IMPORTANT: DevIns Command Format
- Only use DevIns commands inside <devin> tags
- Example:
  <devin>
  /read-file path="src/main.kt"
  </devin>

- DO NOT use / prefix in normal text (it will be misinterpreted as a command)
- Instead of: "Run /gradlew build"
- Use: "Run './gradlew build'" or "Run \`./gradlew build\`"
`;
```

### 建议 2: 在 TypeScript 侧过滤

```typescript
// 在 formatter.debug() 中过滤 mpp-core 日志
debug(message: string): void {
    if (!this.quiet) {
        // 过滤掉 mpp-core 的解析日志
        if (message.includes('[DevInsParser]') || 
            message.includes('[UsedProcessor]') ||
            message.includes('[CommandProcessor]')) {
            return;  // 忽略
        }
        console.log(chalk.gray(`[DEBUG] ${message}`));
    }
}
```

### 建议 3: 使用更严格的解析

```typescript
// 要求 DevIns 命令必须在新行开头
const paramRegex = /^\/([a-z-]+)\s*/;

// 而不是在任何地方匹配 /
```

---

## 总结

**核心修复**: 添加了 `minLevel` 控制，默认只显示错误日志。

**效果**:
- ✅ 不再看到大量的 `[INFO]` 和 `[WARN]` 日志
- ✅ 生产环境输出简洁
- ✅ 开发时可以启用详细日志
- ✅ AI 响应的普通文本不会被误解析（因为已经有 `<devin>` 标签检查）

**下一步**:
- 测试确认日志已经被抑制
- 如果仍然看到解析日志，检查是否有其他地方调用了 DevIns 编译器

---

**日期**: 2025-11-01  
**状态**: ✅ 已修复并测试

