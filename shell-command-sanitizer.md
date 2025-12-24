# Shell Command Sanitizer

## 概述

`ShellCommandSanitizer` 是一个自动修正常见 shell 命令语法错误的工具，特别是针对重定向操作符的错误。

## 问题背景

用户在使用 ShellTool 时，经常会犯一些常见的语法错误，特别是在重定向操作符中添加了不必要的空格：

### 常见错误

```bash
# ❌ 错误：在 2>&1 中添加了空格
nohup ./gradlew bootRun > bootrun.log 2 > &1 &

# ✅ 正确：没有空格
nohup ./gradlew bootRun > bootrun.log 2>&1 &
```

这种错误会导致 shell 语法错误：
```
/bin/bash: -c: line 0: syntax error near unexpected token `&'
```

## 解决方案

`ShellCommandSanitizer` 会自动检测并修正这些常见错误，无需用户手动修改命令。

### 支持的修正

1. **stderr 重定向到 stdout**
   - `2 > &1` → `2>&1`
   - `2> &1` → `2>&1`
   - `2 >&1` → `2>&1`
   - `2  >  &  1` → `2>&1`

2. **stdout 重定向到 stderr**
   - `1 > &2` → `1>&2`
   - `1> &2` → `1>&2`
   - `1 >&2` → `1>&2`

3. **通用重定向**
   - `> &1` → `>&1`
   - `> &2` → `>&2`

## 使用方式

### 自动修正（推荐）

ShellTool 会自动使用 `ShellCommandSanitizer` 修正命令，无需任何额外配置：

```kotlin
val shellTool = ShellTool(DefaultShellExecutor())

// 即使命令有语法错误，也会被自动修正
val params = ShellParams(
    command = "nohup ./gradlew bootRun > bootrun.log 2 > &1 &",
    wait = false
)

val result = shellTool.createInvocation(params).execute(context)
// 命令会被自动修正为: "nohup ./gradlew bootRun > bootrun.log 2>&1 &"
```

### 手动使用

你也可以直接使用 `ShellCommandSanitizer`：

```kotlin
import cc.unitmesh.agent.tool.shell.ShellCommandSanitizer

// 修正命令
val wrongCommand = "command 2 > &1"
val fixedCommand = ShellCommandSanitizer.sanitize(wrongCommand)
// fixedCommand = "command 2>&1"

// 检查是否有可修正的错误
val hasErrors = ShellCommandSanitizer.hasFixableErrors(wrongCommand)
// hasErrors = true

// 获取修正说明
val description = ShellCommandSanitizer.getFixDescription(wrongCommand, fixedCommand)
// description = "Fixed stderr redirection: '2 > &1' → '2>&1'"
```

## 日志

当命令被修正时，会记录 INFO 级别的日志：

```
[DefaultShellExecutor] Command sanitized: Fixed stderr redirection: '2 > &1' → '2>&1'
```

## 测试

项目包含完整的测试套件：

1. **单元测试**: `ShellCommandSanitizerTest` - 测试各种语法修正场景
2. **集成测试**: `NohupTest.testShellToolWithWrongSyntax` - 测试 ShellTool 的自动修正功能

运行测试：

```bash
./gradlew :mpp-core:jvmTest --tests "cc.unitmesh.agent.tool.shell.ShellCommandSanitizerTest"
./gradlew :mpp-core:jvmTest --tests "cc.unitmesh.agent.tool.NohupTest.testShellToolWithWrongSyntax"
```

## 实现细节

### 架构

```
ShellTool
    ↓
ShellInvocation.execute()
    ↓
DefaultShellExecutor.execute()
    ↓
prepareCommand()
    ↓
ShellCommandSanitizer.sanitize()  ← 自动修正
    ↓
ProcessBuilder (执行修正后的命令)
```

### 代码位置

- **Sanitizer**: `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/tool/shell/ShellCommandSanitizer.kt`
- **集成点**: 
  - `mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/tool/shell/DefaultShellExecutor.jvm.kt`
  - `mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/tool/shell/PtyShellExecutor.kt`
- **测试**: 
  - `mpp-core/src/commonTest/kotlin/cc/unitmesh/agent/tool/shell/ShellCommandSanitizerTest.kt`
  - `mpp-core/src/jvmTest/kotlin/cc/unitmesh/agent/tool/NohupTest.kt`

## 注意事项

1. **保守修正**: Sanitizer 只修正已知的常见错误，不会改变命令的语义
2. **日志记录**: 所有修正都会被记录，便于调试和审计
3. **向后兼容**: 正确的命令不会被修改
4. **跨平台**: 在所有支持的平台上工作（JVM, Android, iOS, JS, WASM）

## 未来改进

可能的扩展方向：

1. 支持更多常见的 shell 语法错误
2. 添加配置选项以启用/禁用特定修正
3. 提供修正建议而不是自动修正（可选模式）
4. 支持 PowerShell 和 CMD 的语法修正

