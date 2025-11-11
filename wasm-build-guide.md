# WASM Target 构建与使用指南

## ✅ 构建状态

WASM target 已经成功配置并可以构建！ 🎉

### 构建产物

- **文件**: `mpp-core/build/libs/mpp-core-wasm-js-0.1.6.klib` (约 1.5 MB)
- **类型**: Kotlin 库文件（.klib），可以被其他 Kotlin/Wasm 项目使用

## 📦 构建 WASM Target

### 快速开始

```bash
# 1. 构建 WASM Kotlin 库（.klib）
./gradlew :mpp-core:wasmJsJar

# 2. 或者构建完整包
./gradlew :mpp-core:assembleWasmJsPackage

# 3. 运行 WASM 测试
./gradlew :mpp-core:wasmJsTest
```

### 构建产物位置

构建成功后，产物位于：

```
mpp-core/build/libs/
└── mpp-core-wasm-js-0.1.6.klib   # WASM Kotlin 库文件
```

## 🏗️ WASM Target 配置

### build.gradle.kts 配置

```kotlin
kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()  // 支持浏览器环境
        nodejs()   // 支持 Node.js 环境
    }
}
```

### 源码结构

```
mpp-core/src/
├── commonMain/          # 平台无关代码
├── wasmJsMain/          # WASM 特定实现
│   └── kotlin/
│       └── cc/unitmesh/
│           ├── agent/
│           │   ├── Platform.wasmJs.kt              # 平台信息
│           │   ├── config/
│           │   │   └── McpServerLoadingState.wasmJs.kt
│           │   ├── logging/
│           │   │   └── PlatformLogging.wasmJs.kt
│           │   ├── mcp/
│           │   │   └── McpClientManager.wasmJs.kt
│           │   ├── platform/
│           │   │   └── GitOperations.wasmJs.kt
│           │   └── tool/
│           │       ├── gitignore/
│           │       │   └── GitIgnoreParser.wasmJs.kt
│           │       ├── impl/
│           │       │   ├── HttpClientFactory.wasmJs.kt
│           │       │   └── HttpFetcherFactory.wasmJs.kt
│           │       ├── shell/
│           │       │   └── DefaultShellExecutor.wasmJs.kt
│           │       └── tracking/
│           │           └── FileChange.wasmJs.kt
│           └── devins/
│               └── filesystem/
│                   └── DefaultFileSystem.wasmJs.kt
└── wasmJsTest/          # WASM 测试代码
```

## 💡 WASM 实现特点

### 限制与约束

由于 WASM 环境的限制，以下功能提供 stub 实现：

1. **文件系统访问**
   - ❌ 无法直接访问本地文件系统
   - ✅ 通过虚拟文件系统或浏览器 API

2. **进程执行**
   - ❌ 无法执行 shell 命令
   - ❌ 无法启动子进程
   - ❌ Git 操作不可用

3. **MCP 客户端**
   - ❌ 无法直接连接 MCP 服务器
   - ✅ 可以通过代理或 WebSocket

4. **网络请求**
   - ✅ 支持 HTTP/HTTPS 请求（通过 Ktor JS 引擎）
   - ✅ 使用浏览器 Fetch API 或 Node.js fetch

### 支持的功能

- ✅ 核心数据模型（序列化/反序列化）
- ✅ Agent 逻辑处理
- ✅ HTTP 客户端（Ktor）
- ✅ 日志记录（console）
- ✅ 时间戳获取（kotlinx-datetime）
- ✅ JSON 处理
- ✅ YAML 处理
- ✅ 协程支持

## 🔧 使用 WASM 库

### 在其他 Kotlin 项目中使用

```kotlin
// build.gradle.kts
kotlin {
    wasmJs {
        browser()
        nodejs()
    }
    
    sourceSets {
        wasmJsMain {
            dependencies {
                implementation(project(":mpp-core"))
            }
        }
    }
}
```

### 平台检测示例

```kotlin
import cc.unitmesh.agent.Platform

fun checkPlatform() {
    when {
        Platform.isWasm -> {
            println("Running on WebAssembly")
            println("Platform: ${Platform.name}")
            println("OS Info: ${Platform.getOSInfo()}")
        }
    }
}
```

### HTTP 请求示例

```kotlin
import cc.unitmesh.agent.tool.impl.http.HttpFetcherFactory

suspend fun fetchData() {
    val fetcher = HttpFetcherFactory.create()
    val result = fetcher.fetch("https://api.example.com/data")
    
    if (result.success) {
        println("Content: ${result.content}")
    } else {
        println("Error: ${result.error}")
    }
}
```

## 🧪 测试

### 运行 WASM 测试

```bash
# 在浏览器中运行测试
./gradlew :mpp-core:wasmJsBrowserTest

# 在 Node.js 中运行测试
./gradlew :mpp-core:wasmJsNodeTest

# 运行所有 WASM 测试
./gradlew :mpp-core:wasmJsTest
```

## 📝 开发注意事项

### 1. 避免使用不支持的 API

❌ **不要使用**：
```kotlin
// kotlin.js.Date 在 WASM 中不可用
import kotlin.js.Date
val now = Date()

// js() 函数在 WASM 中受限
val result = js("navigator.userAgent")
```

✅ **应该使用**：
```kotlin
// 使用 kotlinx-datetime
import kotlinx.datetime.Clock
val now = Clock.System.now()

// 使用 expect/actual 机制
expect fun getPlatformInfo(): String
```

### 2. 时间戳处理

```kotlin
// ✅ 正确方式
import kotlinx.datetime.Clock

fun getCurrentTime(): Long {
    return Clock.System.now().toEpochMilliseconds()
}

fun getCurrentIsoString(): String {
    return Clock.System.now().toString()
}
```

### 3. 文件系统操作

WASM 环境中的文件系统操作需要使用抽象接口：

```kotlin
// ✅ 使用虚拟文件系统
import cc.unitmesh.devins.filesystem.DefaultFileSystem

val fs = DefaultFileSystem("project-root")
val exists = fs.exists("/path/to/file")
```

### 4. 网络请求

```kotlin
// ✅ 使用 Ktor 客户端
import io.ktor.client.*
import io.ktor.client.request.*

val client = HttpClient(Js)
val response = client.get("https://api.example.com")
```

## 🚀 与其他平台的差异

| 功能 | JVM | JS | WASM | Android | iOS |
|------|-----|----|----|---------|-----|
| 文件系统 | ✅ | ✅ (Node.js) | ❌ | ✅ (SAF) | ✅ |
| Shell 执行 | ✅ | ✅ (Node.js) | ❌ | ❌ | ❌ |
| Git 操作 | ✅ | ✅ (Node.js) | ❌ | ❌ | ❌ |
| HTTP 客户端 | ✅ | ✅ | ✅ | ✅ | ✅ |
| MCP 客户端 | ✅ | ✅ (Node.js) | ❌ | ❌ | ❌ |
| 日志文件 | ✅ | ✅ (Node.js) | ❌ | ✅ | ✅ |
| Console 日志 | ✅ | ✅ | ✅ | ✅ | ✅ |

## 🔗 相关资源

- [Kotlin/Wasm 官方文档](https://kotlinlang.org/docs/wasm-overview.html)
- [MPP-Core README](./README.md)
- [Kotlin Multiplatform 指南](https://kotlinlang.org/docs/multiplatform.html)

## 📋 已知问题

1. **标准库版本警告**
   ```
   w: The version of the Kotlin/Wasm standard library (2.2.10-release-430) 
      differs from the version of the compiler (2.2.0)
   ```
   - 这是一个警告，不影响编译
   - Kotlin/Wasm 仍在快速发展中，版本差异是正常的

2. **expect/actual 类警告**
   ```
   w: 'expect'/'actual' classes are in Beta
   ```
   - 可以通过添加 `-Xexpect-actual-classes` 编译选项来抑制警告

3. **功能限制**
   - WASM 环境无法执行系统命令
   - 无法访问本地文件系统（除非通过浏览器 API）
   - MCP 客户端需要通过代理或其他方式实现

## 📄 许可证

与项目主许可证相同
