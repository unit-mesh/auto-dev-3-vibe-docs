# Platform Context Implementation

## 概述

为 `CodingAgentContext` 实现了多平台支持，包括时间戳、操作系统信息和 shell 路径等功能。

## 实现的功能

### 1. Platform 对象扩展

在 `Platform.kt` 中添加了以下新方法：

- `getCurrentTimestamp()`: 返回 ISO 8601 格式的当前时间戳
- `getOSInfo()`: 返回详细的操作系统信息
- `getOSVersion()`: 返回操作系统版本
- `getDefaultShell()`: 返回默认 shell 路径

### 2. 平台特定实现

#### JVM 平台 (`Platform.jvm.kt`)
- 使用 `java.time.ZonedDateTime` 获取时间戳
- 使用 `System.getProperty()` 获取 OS 信息
- 根据操作系统类型返回正确的 shell（Windows: cmd.exe, macOS: /bin/zsh, Linux: /bin/bash）

#### JavaScript 平台 (`Platform.js.kt`)
- 使用 `kotlin.js.Date` 获取 ISO 格式时间戳
- 支持 Node.js 环境：从 `process` 对象获取平台信息
- 支持浏览器环境：从 `navigator` 对象获取用户代理信息

#### Android 平台 (`Platform.kt`)
- 使用 `android.os.Build` 获取设备详细信息（制造商、型号、版本等）
- 使用 `/system/bin/sh` 作为默认 shell
- 返回完整的 Android 版本和 API 级别信息

#### WebAssembly 平台 (`Platform.wasmJs.kt`)
- 使用 JavaScript 的 Date API 获取时间戳
- 尝试从浏览器环境获取用户代理信息
- 提供回退机制处理不同的 WASM 运行环境

### 3. CodingAgentContext 集成

更新了 `CodingAgentContext.fromTask()` 方法，使用 `Platform` 对象自动填充：
- `osInfo`: 完整的操作系统信息
- `timestamp`: 当前时间戳
- `shell`: 默认 shell 路径

## 测试

### 编译测试
```bash
# 测试所有平台编译
./gradlew :mpp-core:test -x lintDebug -x lintAnalyzeDebug -x lintReportDebug

# 测试 JS 平台构建
./gradlew :mpp-core:assembleJsPackage
```

### 功能测试
```bash
# 运行平台功能测试
./docs/test-scripts/test-platform-simple.sh
```

## 支持的平台

| 平台 | 状态 | 时间戳 | OS 信息 | Shell |
|------|------|--------|---------|-------|
| JVM (Desktop) | ✅ | ISO 8601 | 完整（名称+版本+架构） | 根据 OS 类型 |
| JavaScript (Node.js) | ✅ | ISO 8601 | Node 版本+平台 | 根据平台类型 |
| JavaScript (Browser) | ✅ | ISO 8601 | User Agent | N/A |
| Android | ✅ | ISO 8601 | 完整设备信息 | /system/bin/sh |
| WebAssembly | ✅ | ISO 8601 | Browser 环境 | /bin/bash |

## 代码位置

- Common: `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/Platform.kt`
- JVM: `mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/Platform.jvm.kt`
- JS: `mpp-core/src/jsMain/kotlin/cc/unitmesh/agent/Platform.js.kt`
- Android: `mpp-core/src/androidMain/kotlin/cc/unitmesh/agent/Platform.kt`
- WASM: `mpp-core/src/wasmJsMain/kotlin/cc/unitmesh/agent/Platform.wasmJs.kt`
- Context: `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/CodingAgentContext.kt`

## 示例输出

### JVM (macOS)
```
OS Info: Mac OS X 14.0 (aarch64)
Timestamp: 2024-11-03T21:45:30+08:00
Shell: /bin/zsh
```

### JavaScript (Node.js on macOS)
```
OS Info: Node.js v20.0.0 on darwin (arm64)
Timestamp: 2024-11-03T13:45:30.000Z
Shell: /bin/zsh
```

### Android
```
OS Info: Android 13 (API 33) - Samsung SM-G991B (o1s)
Timestamp: 2024-11-03T21:45:30+08:00
Shell: /system/bin/sh
```

### WebAssembly (Browser)
```
OS Info: WebAssembly in Browser: Mozilla/5.0 ...
Timestamp: 2024-11-03T13:45:30.000Z
Shell: /bin/bash
```

## 注意事项

1. **时区处理**: JVM 和 Android 使用系统时区，JS 和 WASM 使用 UTC
2. **Shell 路径**: 桌面平台根据实际 OS 返回，移动和 Web 平台返回默认值
3. **浏览器环境**: JS 平台在浏览器中运行时，某些功能（如 shell）不适用
4. **错误处理**: WASM 平台包含 try-catch 以处理不同运行环境的差异

## 未来改进

1. 添加时区配置支持
2. 支持自定义 shell 路径
3. 添加更多系统信息（内存、CPU 等）
4. 改进浏览器环境的信息获取






