# Kotlin Multiplatform (KMP) 平台实现分析报告

## 概述

本文档对 `mpp-core` 模块中的 Kotlin Multiplatform (KMP) expect/actual 实现进行全面分析，识别重复和相似的实现，并提供优化建议。

## 目前支持的平台

```
commonMain/       # 共享代码和 expect 声明
├── jvmMain/      # JVM 平台实现
├── jsMain/       # JavaScript/Node.js 平台实现
├── androidMain/  # Android 平台实现
├── iosMain/      # iOS 平台实现（Native）
└── wasmJsMain/   # WebAssembly 平台实现
```

## KMP 实现分类

### 1. 平台信息类 (Platform Information)

#### `Platform.kt`
- **Expect**: `cc.unitmesh.agent.Platform`
- **功能**: 提供平台识别、OS 信息、时间戳、用户目录等
- **实现平台**: JVM, JS, iOS, Wasm
- **Android**: ❌ 缺失

**相似度分析**:
```
JVM ←→ Android: 可共享大部分实现（都使用 System.getProperty）
JS ←→ Wasm: 可共享部分实现（都使用浏览器/Node.js API）
iOS: 独立实现（使用 Native API）
```

**建议**: 
- 创建 `Platform.android.kt`，与 JVM 共享实现
- 考虑创建 `jvmAndroidShared` source set 合并相似代码

---

### 2. 文件系统类 (File System)

#### `DefaultFileSystem.kt` / `ProjectFileSystem.kt`
- **Expect**: `cc.unitmesh.devins.filesystem.DefaultFileSystem`
- **功能**: 跨平台文件读写、路径解析、文件搜索
- **实现平台**: JVM, JS, iOS, Wasm
- **Android**: ❓ 未知（可能使用 JVM 实现）

**相似度分析**:
```
JVM: 使用 java.io.File
JS: 使用 Node.js fs module
Wasm: 使用 JS interop
iOS: 使用 Native file APIs
Android: 可能与 JVM 共享（但可能需要特殊处理 Context）
```

**建议**:
- JVM 和 Android 可共享文件系统实现
- JS 和 Wasm 可共享部分逻辑（都调用 Node.js APIs）

---

### 3. 会话存储类 (Session Storage)

#### `SessionStorage.kt`
- **Expect**: `cc.unitmesh.devins.llm.SessionStorage`
- **功能**: 保存和加载聊天会话
- **实现平台**: JVM, JS, Android, iOS, Wasm
- **存储方式**:
  - **JVM**: 文件系统 (`~/.autodev/sessions/chat-sessions.json`)
  - **JS**: localStorage (浏览器) 或内存 (Node.js)
  - **Android**: 内存缓存（需要 Context 访问文件系统）
  - **iOS**: 内存缓存
  - **Wasm**: 内存缓存

**相似度分析**:
```
JVM: 文件系统（独立实现）
JS: localStorage/内存（独立实现）
Android ←→ iOS ←→ Wasm: 都使用内存缓存（可合并！）
```

**建议**: 
- **高优先级**: 创建 `nonJvmSessionStorage` 或 `memorySessionStorage`
- 将 Android, iOS, Wasm 的内存缓存实现合并到 `commonMain` 的一个基类
- JVM 和 JS 保持独立实现（特殊需求）

---

### 4. 日志系统类 (Logging)

#### `PlatformLogging.kt` / `AutoDevLogger.kt`
- **Expect**: `initializePlatformLogging()`, `getPlatformLogDirectory()`
- **功能**: 平台特定的日志初始化和日志目录
- **实现平台**: JVM, JS, Android, iOS, Wasm

**相似度分析**:
```
JVM: 使用 Logback（文件日志）
JS: 使用 console（无文件日志）
Android: 使用 Logcat（无文件日志）
iOS: 基础实现
Wasm: 使用 console（无文件日志）
```

**建议**:
- **高优先级**: JS, Android, Wasm 的实现几乎相同（都是空实现或 console）
- 创建 `consolePlatformLogging` 共享实现
- 只有 JVM 需要特殊的文件日志实现

---

### 5. MCP 客户端管理类 (MCP Client Manager)

#### `McpClientManager.kt`
- **Expect**: `cc.unitmesh.agent.mcp.McpClientManager`
- **功能**: 管理 MCP 服务器连接和工具发现
- **实现平台**: JVM, JS, Android, iOS, Wasm

**相似度分析**:
```
JVM: 完整实现（使用 io.modelcontextprotocol:kotlin-sdk）
JS: 完整实现（使用 @modelcontextprotocol/sdk）
Android ←→ iOS ←→ Wasm: 都是 stub 实现（可合并！）
```

**建议**:
- **高优先级**: Android, iOS, Wasm 实现几乎完全相同（都是空操作）
- 创建 `stubMcpClientManager` 在 `commonMain` 中
- 使用 intermediate source set 合并这些平台

---

### 6. Linter 注册类 (Linter Registry)

#### `LinterRegistry.kt`
- **Expect**: `registerPlatformLinters()`
- **功能**: 注册平台特定的代码检查工具
- **实现平台**: JVM, JS, Android, iOS, Wasm

**相似度分析**:
```
JVM ←→ JS: 完全相同！（都注册所有 linters）
Android ←→ iOS ←→ Wasm: 都是空实现（可合并！）
```

**建议**:
- **高优先级**: JVM 和 JS 的实现完全相同，应该合并
- 创建 `desktopLinterRegistry` 合并 JVM 和 JS
- 创建 `mobileLinterRegistry` 合并 Android, iOS, Wasm（空实现）

---

### 7. HTTP 客户端工厂类 (HTTP Client Factory)

#### `HttpClientFactory.kt`
- **Expect**: `cc.unitmesh.agent.tool.impl.http.HttpClientFactory`
- **功能**: 创建平台特定的 Ktor HttpClient
- **实现平台**: JVM, JS, Android, iOS, Wasm

**相似度分析**:
```
JVM ←→ Android: 完全相同（都使用 CIO engine）
JS: 使用 Js engine（独立实现）
iOS: 使用 Darwin engine（独立实现）
Wasm: 使用 Js engine（独立实现）
```

**建议**:
- **高优先级**: JVM 和 Android 使用完全相同的代码
- 创建 `cioHttpClientFactory` 合并 JVM 和 Android
- JS 和 Wasm 可能可以共享（都使用 Js engine）

---

### 8. Git 操作类 (Git Operations)

#### `GitOperations.kt`
- **Expect**: `cc.unitmesh.agent.platform.GitOperations`
- **功能**: Git 命令执行和仓库操作
- **实现平台**: JVM, JS, Android, iOS, Wasm

**相似度分析**:
```
JVM: 使用 ProcessBuilder
JS: 使用 Node.js child_process
Android ←→ iOS: 都是 stub 实现（可合并）
Wasm: 特殊的 JS interop 实现
```

**建议**:
- Android 和 iOS 的空实现可以合并
- Wasm 需要保持独立（特殊的 interop 逻辑）

---

### 9. Shell 执行器类 (Shell Executor)

#### `DefaultShellExecutor.kt`
- **Expect**: `cc.unitmesh.agent.tool.shell.DefaultShellExecutor`
- **功能**: 执行 shell 命令
- **实现平台**: JVM, JS, iOS, Wasm
- **Android**: ❌ 缺失

**建议**:
- 检查是否需要 Android 实现
- iOS 和其他移动平台可能需要受限的 shell 访问

---

### 10. 其他工具类

#### `GitIgnoreParser.kt`
- **实现平台**: JVM, JS, Android, iOS, Wasm
- **相似度**: Android 可能与 JVM 共享

#### `HttpFetcherFactory.kt`
- **实现平台**: JVM, JS, Android, iOS, Wasm
- **相似度**: 各平台实现差异较大

#### `McpServerLoadingState.kt` - `getCurrentTimeMillis()`
- **实现平台**: JVM, JS, Android, iOS, Wasm
- **相似度分析**:
```
JVM ←→ Android ←→ iOS: 完全相同（System.currentTimeMillis()）
JS ←→ Wasm: 完全相同（Date.now()）
```

**建议**:
- **高优先级**: 这是最容易合并的！
- 创建 `nativePlatformTime` 合并 JVM/Android/iOS
- 创建 `jsPlatformTime` 合并 JS/Wasm

---

## 优化建议总结

### 立即可执行的高优先级优化

#### 1. 创建 Intermediate Source Sets

在 `build.gradle.kts` 中创建中间源集：

```kotlin
kotlin {
    // 共享 JVM 和 Android 的实现
    sourceSets {
        val jvmAndroidMain by creating {
            dependsOn(commonMain.get())
        }
        
        jvmMain {
            dependsOn(jvmAndroidMain)
        }
        
        androidMain {
            dependsOn(jvmAndroidMain)
        }
        
        // 共享所有 stub 实现（Android, iOS, Wasm）
        val stubPlatformMain by creating {
            dependsOn(commonMain.get())
        }
        
        androidMain {
            dependsOn(stubPlatformMain)  // 部分功能是 stub
        }
        
        iosMain {
            dependsOn(stubPlatformMain)
        }
        
        wasmJsMain {
            dependsOn(stubPlatformMain)
        }
        
        // 共享 JS 和 Wasm 的相似实现
        val jsCommonMain by creating {
            dependsOn(commonMain.get())
        }
        
        jsMain {
            dependsOn(jsCommonMain)
        }
        
        wasmJsMain {
            dependsOn(jsCommonMain)
        }
    }
}
```

#### 2. 合并相同实现

**第一批（最简单）**:
- ✅ `getCurrentTimeMillis()` - JVM/Android/iOS 相同，JS/Wasm 相同
- ✅ `HttpClientFactory` - JVM/Android 完全相同
- ✅ `LinterRegistry` - JVM/JS 完全相同
- ✅ `PlatformLogging` - JS/Android/Wasm 基本相同

**第二批（需要少量重构）**:
- 🔧 `SessionStorage` - Android/iOS/Wasm 都用内存缓存
- 🔧 `McpClientManager` - Android/iOS/Wasm 都是 stub

**第三批（需要设计考虑）**:
- 🔧 `Platform` - JVM/Android 可以共享大部分
- 🔧 `GitOperations` - Android/iOS 的 stub 可以合并

---

## 文件迁移计划

### 阶段 1: 创建共享实现（jvmAndroidMain）

**迁移到 `jvmAndroidMain/`**:
```
HttpClientFactory.kt                    # CIO engine 实现
Platform.kt                              # 基础 System.getProperty 实现
GitIgnoreParser.kt                       # 如果实现相同
getCurrentTimeMillis() 相关实现          # System.currentTimeMillis()
```

### 阶段 2: 创建 stub 实现（stubPlatformMain）

**迁移到 `stubPlatformMain/`**:
```
McpClientManager.kt                      # 空实现
LinterRegistry.kt                        # 空注册
部分 GitOperations.kt                    # 空实现
```

### 阶段 3: 创建 JS 共享实现（jsCommonMain）

**迁移到 `jsCommonMain/`**:
```
部分 Platform.kt                         # JS/Wasm 共享的浏览器 API
getCurrentTimeMillis() - Date.now()     # JS/Wasm 时间实现
PlatformLogging.kt                       # console 实现
```

### 阶段 4: 保留的平台特定实现

**JVM 独有**:
```
JvmLoggingInitializer.kt                 # Logback 配置
SessionStorage.kt                        # 文件系统存储
DefaultFileSystem.kt                     # java.io.File
McpClientManager.kt                      # kotlin-sdk
```

**JS 独有**:
```
SessionStorage.kt                        # localStorage
McpClientManager.kt                      # @modelcontextprotocol/sdk
DefaultFileSystem.kt                     # Node.js fs
```

**Wasm 独有**:
```
WasmGitInterop.kt                        # JS interop
GitOperations.kt                         # 特殊实现
```

**iOS 独有**:
```
大部分实现                                # Native APIs
```

---

## 重复代码统计

### 完全相同的实现

| 功能 | 相同平台 | 重复行数（估算） | 优先级 |
|------|----------|------------------|--------|
| `getCurrentTimeMillis()` | JVM/Android/iOS | ~10 行 | 🔴 高 |
| `HttpClientFactory` | JVM/Android | ~35 行 | 🔴 高 |
| `LinterRegistry.registerPlatformLinters()` | JVM/JS | ~70 行 | 🔴 高 |
| `PlatformLogging.initializePlatformLogging()` | JS/Android/Wasm | ~15 行 | 🔴 高 |
| `McpClientManager` (stub) | Android/iOS/Wasm | ~60 行 | 🟡 中 |
| `SessionStorage` (memory) | Android/iOS/Wasm | ~50 行 | 🟡 中 |

**总计重复代码**: 约 **240 行**（保守估计）

### 高度相似的实现（可抽取共享逻辑）

| 功能 | 相似平台 | 可共享逻辑 | 优先级 |
|------|----------|------------|--------|
| `Platform.getOSName()` | JVM/Android | System properties | 🟡 中 |
| `Platform.getUserHomeDir()` | JVM/Android | System properties | 🟡 中 |
| `GitOperations` (stub) | Android/iOS | 空实现 | 🟢 低 |

---

## 推荐的源集结构

```
mpp-core/src/
├── commonMain/                    # 共享代码和 expect 声明
├── commonTest/                    # 共享测试
│
├── jvmAndroidMain/                # 🆕 JVM + Android 共享实现
│   ├── HttpClientFactory.kt
│   ├── Platform.kt (partial)
│   └── getCurrentTimeMillis.kt
│
├── jvmMain/                       # JVM 特有实现
│   ├── SessionStorage.jvm.kt
│   ├── McpClientManager.jvm.kt
│   ├── JvmLoggingInitializer.kt
│   └── DefaultFileSystem.jvm.kt
│
├── androidMain/                   # Android 特有实现
│   └── (非常少，大部分继承自 jvmAndroidMain)
│
├── jsCommonMain/                  # 🆕 JS + Wasm 共享实现
│   ├── PlatformLogging.kt
│   ├── getCurrentTimeMillis.kt
│   └── Platform.kt (partial)
│
├── jsMain/                        # JS 特有实现
│   ├── SessionStorage.js.kt
│   ├── McpClientManager.js.kt
│   └── DefaultFileSystem.js.kt
│
├── wasmJsMain/                    # Wasm 特有实现
│   ├── WasmGitInterop.kt
│   └── GitOperations.wasmJs.kt
│
├── stubPlatformMain/              # 🆕 Stub 实现共享
│   ├── McpClientManager.kt
│   └── LinterRegistry.kt
│
└── iosMain/                       # iOS 特有实现
    └── (大部分保持独立)
```

---

## 行动项

### 立即执行（本周）
1. ✅ 创建 `jvmAndroidMain` source set
2. ✅ 迁移 `HttpClientFactory` 到 `jvmAndroidMain`
3. ✅ 迁移 `getCurrentTimeMillis()` 到共享实现
4. ✅ 合并 `LinterRegistry.jvm.kt` 和 `LinterRegistry.js.kt`

### 短期执行（2周内）
5. 🔧 创建 `jsCommonMain` source set
6. 🔧 合并 JS/Wasm 的 `PlatformLogging`
7. 🔧 创建 `stubPlatformMain` 合并 stub 实现
8. 🔧 重构 `SessionStorage` 的内存实现

### 长期规划（1个月内）
9. 📋 考虑合并 JVM/Android 的 `Platform` 实现
10. 📋 评估 `DefaultFileSystem` 的共享可能性
11. 📋 优化 `GitOperations` 的平台实现策略

---

## 注意事项

### 不应该合并的实现

1. **文件系统操作** - 各平台差异较大（JVM: java.io, JS: Node.js, iOS: Native）
2. **MCP SDK** - JVM 和 JS 使用不同的 SDK（kotlin-sdk vs @modelcontextprotocol/sdk）
3. **Wasm 的 Git 操作** - 特殊的 JS interop 实现

### 合并风险评估

- **低风险**: `getCurrentTimeMillis()`, `HttpClientFactory`
- **中风险**: `LinterRegistry`, `PlatformLogging`
- **高风险**: `Platform`, `DefaultFileSystem`

建议：从低风险项开始，逐步验证后再处理中高风险项。

---

## 测试策略

合并实现后，需要在所有平台上运行测试：

```bash
# JVM
./gradlew :mpp-core:jvmTest

# JS
./gradlew :mpp-core:jsTest

# Android
./gradlew :mpp-core:androidUnitTest

# iOS
./gradlew :mpp-core:iosTest

# Wasm
./gradlew :mpp-core:wasmJsTest
```

---

## 结论

`mpp-core` 中存在大量可合并的重复实现，特别是：
- ✅ **JVM 和 Android** 有高度相似的实现
- ✅ **JS 和 Wasm** 可以共享部分代码
- ✅ **Android, iOS, Wasm** 的 stub 实现可以统一

通过创建中间源集（intermediate source sets），可以减少约 **240+ 行重复代码**，提高代码可维护性。

建议按照上述阶段逐步执行，优先处理完全相同的实现（如 `HttpClientFactory`, `getCurrentTimeMillis()`），再处理相似实现。
