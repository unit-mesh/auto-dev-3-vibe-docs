# Streaming Output Fix

## 修改日期
2025-11-01

## 问题描述

用户反馈 `[DEBUG]` stream 输出太混乱，每个 token 都单独一行输出。

### 具体问题

在 `--verbose` 模式下运行时，LLM 的 streaming 响应每个 token 都单独输出一行 `[DEBUG]`：

```
[DEBUG] I
[DEBUG] 'll
[DEBUG]  help
[DEBUG]  you
[DEBUG]  create
[DEBUG]  a
[DEBUG]  hello
[DEBUG]  world
[DEBUG]  application
[DEBUG] .
[DEBUG]  Let
[DEBUG]  me
[DEBUG]  start
...
```

这导致：
1. **输出混乱**：DevIns 命令被拆成了无数行
2. **难以阅读**：无法看清 AI 真正在说什么
3. **性能问题**：每个 token 都调用一次 console.log

### 示例输出（修复前）

```
[DEBUG] /w
[DEBUG] rite
[DEBUG] -file
[DEBUG]  path
[DEBUG] ="
[DEBUG] src
[DEBUG] /main
[DEBUG] /k
[DEBUG] ot
[DEBUG] lin
[DEBUG] /com
[DEBUG] /
[DEBUG] example
[DEBUG] /M
[DEBUG] ain
[DEBUG] .
[DEBUG] kt
[DEBUG] "
[DEBUG]  content
[DEBUG] ="
[DEBUG] package
[DEBUG]  com
[DEBUG] .example
[DEBUG] \n
[DEBUG] \n
[DEBUG] fun
[DEBUG]  main
[DEBUG] ()
[DEBUG]  {\
[DEBUG] n
[DEBUG]    
[DEBUG]  println
[DEBUG] (\
[DEBUG] "
[DEBUG] Hello
[DEBUG] ,
[DEBUG]  World
[DEBUG] !
[DEBUG] \"
[DEBUG] )\
[DEBUG] n
[DEBUG] }"
```

用户完全看不清这是一个 `write-file` 命令。

## 根本原因

**文件**: `mpp-ui/src/jsMain/typescript/services/CodingAgentService.ts`

在 `getNextAction` 方法中，streaming callback 里每个 chunk 都调用了 `this.formatter.debug(chunk)`：

```typescript
await this.llmService.streamMessageWithSystem(
  systemPrompt,
  userPrompt,
  (chunk) => {
    response += chunk;
    // Only output streaming in verbose mode
    this.formatter.debug(chunk);  // ❌ 问题：每个 token 都输出一行
  }
);
```

由于 LLM streaming 返回的是一个个 token（如 "I", "'ll", " help"），每个 token 都会触发一次 `formatter.debug()`，导致每行只有一个 token。

## 解决方案

### 修改策略

1. **移除 chunk 级别的日志**：不再在每个 chunk 到达时输出
2. **添加汇总日志**：在接收完整响应后，输出一次汇总信息
3. **保留详细信息**：在 `executeAction` 中会输出完整的 DevIns 命令

### 代码修改

**文件**: `mpp-ui/src/jsMain/typescript/services/CodingAgentService.ts:431-456`

**修改前**:
```typescript
let response = '';

try {
  this.formatter.debug('Getting next action from LLM...');
  
  await this.llmService.streamMessageWithSystem(
    systemPrompt,
    userPrompt,
    (chunk) => {
      response += chunk;
      // Only output streaming in verbose mode
      this.formatter.debug(chunk);  // ❌ 每个 token 一行
    }
  );

  return response;
```

**修改后**:
```typescript
let response = '';

try {
  this.formatter.debug('Getting next action from LLM...');
  
  await this.llmService.streamMessageWithSystem(
    systemPrompt,
    userPrompt,
    (chunk) => {
      response += chunk;
      // Don't output individual chunks - too noisy
      // The full response will be logged via executeAction
    }
  );

  // Log the complete response in debug mode
  if (response.length > 0) {
    this.formatter.debug(`Received ${response.length} chars from LLM`);
  }

  return response;
```

## 效果

### ✅ 修复前的问题

```
[DEBUG] Getting next action from LLM...
[DEBUG] I
[DEBUG] 'll
[DEBUG]  help
[DEBUG]  you
[DEBUG]  create
[DEBUG]  a
[DEBUG]  hello
[DEBUG]  world
... (几百行)
```

### ✅ 修复后的输出

```
[DEBUG] Getting next action from LLM...
[DEBUG] Received 1234 chars from LLM
[DEBUG] Executing: /write-file path="src/main/kotlin/com/example/Main.kt" content="..."
✓ Executed write-file
[DEBUG] Output: Successfully created file: src/main/kotlin/com/example/Main.kt (64 chars, 5 lines)
```

**优势**:
1. **清晰简洁**：只显示必要的信息
2. **易于调试**：可以看到完整的 DevIns 命令
3. **性能更好**：减少了大量的 console.log 调用

## 详细日志策略

### 正常模式（默认）
```
[1/100] Analyzing and executing...
✓ Executed write-file
✓ Executed shell
```

### Quiet 模式 (`--quiet`)
```
✓ Task completed successfully
```

### Verbose 模式 (`--verbose`)
```
[DEBUG] Getting next action from LLM...
[DEBUG] Received 1234 chars from LLM
[DEBUG] Executing: /write-file path="..." content="..."
✓ Executed write-file
[DEBUG] Output: Successfully created file: ... (64 chars, 5 lines)
```

### 保留的详细信息

在 verbose 模式下，用户仍然可以看到：
- LLM 响应的字符数
- 每个 DevIns 命令的完整内容
- 每个命令的执行结果
- SubAgent 的分析过程

但不会看到：
- ❌ 每个 streaming token（太混乱）
- ✓ 完整的 DevIns 命令（有用）
- ✓ 命令执行结果（有用）

## 相关文件

- `mpp-ui/src/jsMain/typescript/services/CodingAgentService.ts`: 主要修改

## 测试命令

### 正常模式
```bash
cd /Volumes/source/ai/autocrud/mpp-ui
node dist/index.js code --path /path/to/project --task "Create a hello world"
```

### Verbose 模式
```bash
node dist/index.js code --path /path/to/project --task "Create a hello world" --verbose
```

### 预期输出（Verbose）
```
[DEBUG] Getting next action from LLM...
[DEBUG] Received 856 chars from LLM
[DEBUG] Executing: /read-file path="build.gradle.kts"
✓ Executed read-file
[DEBUG] Output: plugins {...
[2/100] Analyzing and executing...
[DEBUG] Getting next action from LLM...
[DEBUG] Received 1234 chars from LLM
[DEBUG] Executing: /write-file path="src/main/kotlin/Main.kt" content="fun main() {...}"
✓ Executed write-file
```

**不应该看到**:
```
❌ [DEBUG] fun
❌ [DEBUG]  main
❌ [DEBUG] ()
❌ [DEBUG]  {
```

## Write-file 说明

用户最初担心 "write-file 有问题"，但实际上 write-file 本身工作正常。

### 混淆原因

由于每个 token 都单独输出，用户看到的是：
```
[DEBUG] /w
[DEBUG] rite
[DEBUG] -file
[DEBUG]  path
[DEBUG] ="
[DEBUG] src
```

这让用户以为 write-file 的解析有问题。

### 实际情况

完整的命令是正确的：
```
/write-file path="src/main/kotlin/com/example/Main.kt" content="package com.example\n\nfun main() {\n    println(\"Hello, World!\")\n}" createDirectories=true
```

执行结果也成功：
```
✓ Executed write-file
[DEBUG] Output: Successfully created file: src/main/kotlin/com/example/Main.kt (64 chars, 5 lines)
```

### 验证

修复后，用户可以清楚地看到：
```
[DEBUG] Executing: /write-file path="src/main/kotlin/com/example/Main.kt" content="package com.example\n\nfun main() {\n    println(\"Hello, World!\")\n}" createDirectories=true
✓ Executed write-file
```

这样就不会再混淆了。

## 总结

| 问题 | 原因 | 解决方案 | 效果 |
|------|------|----------|------|
| Streaming 输出混乱 | 每个 token 都调用 `formatter.debug()` | 只输出汇总信息 | ✅ 清晰简洁 |
| Write-file 看起来有问题 | Token 被拆散导致难以阅读 | 完整输出 DevIns 命令 | ✅ 一目了然 |
| 性能问题 | 大量 console.log 调用 | 减少日志输出 | ✅ 更快 |

## 构建命令

```bash
# 构建 CLI
cd /Volumes/source/ai/autocrud/mpp-ui
npm run build:ts

# 测试
node dist/index.js code --path /path/to/project --task "Your task" --verbose
```

