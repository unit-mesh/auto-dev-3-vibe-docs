# iOS MCP Integration Guide

## 概述

本文档描述如何在 iOS 平台上集成 Model Context Protocol (MCP) 能力，使用 Swift MCP SDK 实现与 JVM/JS 平台一致的功能。

## 架构方案

### 1. 技术栈

- **Swift MCP SDK**: https://github.com/modelcontextprotocol/swift-sdk
- **Kotlin/Native**: 用于 iOS 平台的 Kotlin 代码
- **Swift Interop**: Kotlin/Native 与 Swift 的互操作

### 2. 集成方式

由于 Kotlin Multiplatform 对 Swift 的互操作支持有限，我们采用以下方案：

#### 方案 A: Swift 包装层（推荐）

```
┌─────────────────────────────────────────┐
│  Kotlin Code (mpp-core/iosMain)         │
│  ├─ McpClientManager.ios.kt             │
│  └─ 调用 Swift 桥接层                    │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│  Swift Bridge Layer                     │
│  ├─ McpClientBridge.swift               │
│  ├─ 使用 @objc 导出给 Kotlin             │
│  └─ 封装 Swift MCP SDK                   │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│  Swift MCP SDK                          │
│  ├─ Client                              │
│  ├─ StdioTransport                      │
│  └─ Tool/Resource/Prompt APIs           │
└─────────────────────────────────────────┘
```

**优点**:
- 完全利用 Swift MCP SDK 的原生功能
- 类型安全，性能好
- 易于维护和更新

**缺点**:
- 需要编写额外的 Swift 桥接代码
- 需要处理 Swift ↔ Kotlin 类型转换

#### 方案 B: 纯 Kotlin 实现

使用 Kotlin/Native 的 POSIX API 直接实现 MCP 协议。

**优点**:
- 纯 Kotlin 代码，无需 Swift
- 跨平台代码复用

**缺点**:
- 需要重新实现 MCP 协议
- 维护成本高
- 功能可能不如官方 SDK 完善

**结论**: 采用方案 A（Swift 包装层）

## 实现步骤

### Step 1: 添加 Swift MCP SDK 依赖

在 `mpp-core/AutoDevCore.podspec` 中添加依赖：

```ruby
Pod::Spec.new do |spec|
  spec.name                     = 'AutoDevCore'
  spec.version                  = '0.1.5'
  # ... 其他配置 ...
  
  # 添加 Swift MCP SDK 依赖
  spec.dependency 'ModelContextProtocol', '~> 0.10.0'
end
```

### Step 2: 创建 Swift 桥接层

在 `mpp-core/src/iosMain/swift/` 创建桥接代码：

```swift
// McpClientBridge.swift
import Foundation
import MCP

@objc public class McpClientBridge: NSObject {
    private var clients: [String: Client] = [:]
    private var serverStatuses: [String: String] = [:]
    
    @objc public func initialize(configJson: String) async throws {
        // 解析配置并初始化
    }
    
    @objc public func discoverAllTools() async throws -> String {
        // 返回 JSON 格式的工具列表
    }
    
    @objc public func executeTool(
        serverName: String,
        toolName: String,
        arguments: String
    ) async throws -> String {
        // 执行工具并返回结果
    }
}
```

### Step 3: 实现 Kotlin iOS 端

更新 `mpp-core/src/iosMain/kotlin/cc/unitmesh/agent/mcp/McpClientManager.ios.kt`:

```kotlin
package cc.unitmesh.agent.mcp

import kotlinx.cinterop.*
import platform.Foundation.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class McpClientManager {
    private val bridge = McpClientBridge()
    private var discoveryState = McpDiscoveryState.NOT_STARTED
    
    actual suspend fun initialize(config: McpConfig) {
        // 调用 Swift 桥接层
    }
    
    actual suspend fun discoverAllTools(): Map<String, List<McpToolInfo>> {
        // 调用 Swift 桥接层并解析结果
    }
    
    // ... 其他方法实现
}
```

### Step 4: 配置构建系统

#### 4.1 更新 `mpp-core/build.gradle.kts`

```kotlin
kotlin {
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "AutoDevCore"
            isStatic = true
            
            // 添加 Swift 互操作配置
            export("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
        
        iosTarget.compilations.getByName("main") {
            cinterops {
                val mcpBridge by creating {
                    defFile(project.file("src/iosMain/cinterop/mcpBridge.def"))
                    packageName("cc.unitmesh.agent.mcp.bridge")
                }
            }
        }
    }
}
```

#### 4.2 创建 cinterop 定义文件

`mpp-core/src/iosMain/cinterop/mcpBridge.def`:

```
language = Objective-C
headers = McpClientBridge.h
compilerOpts = -framework Foundation
```

### Step 5: 在 iOS 应用中使用

在 `mpp-ios/AutoDevApp/` 中使用：

```swift
import AutoDevCore
import AutoDevUI

class MCPViewModel: ObservableObject {
    private let mcpManager = McpClientManager()
    
    func initializeMCP() async {
        let config = McpConfig(mcpServers: [
            "filesystem": McpServerConfig(
                command: "npx",
                args: ["-y", "@modelcontextprotocol/server-filesystem", "/tmp"]
            )
        ])
        
        try? await mcpManager.initialize(config: config)
        let tools = try? await mcpManager.discoverAllTools()
        print("Discovered tools: \(tools)")
    }
}
```

## 技术挑战与解决方案

### 挑战 1: Swift ↔ Kotlin 类型转换

**问题**: Swift 和 Kotlin 的类型系统不完全兼容

**解决方案**:
- 使用 JSON 作为中间格式传递复杂数据
- 简单类型使用 `@objc` 兼容的类型（String, Int, Bool）
- 异步操作使用 Kotlin Coroutines + Swift async/await

### 挑战 2: 进程管理

**问题**: iOS 上启动子进程（stdio transport）受限

**解决方案**:
- 优先支持 HTTP/SSE transport（网络传输）
- stdio transport 仅在开发环境（模拟器）中支持
- 生产环境建议使用远程 MCP 服务器

### 挑战 3: 内存管理

**问题**: Swift ARC 和 Kotlin/Native 内存管理的互操作

**解决方案**:
- 使用 `@objc` 类确保正确的引用计数
- 在 Kotlin 端使用 `StableRef` 管理长生命周期对象
- 及时释放不再使用的资源

## 测试策略

### 单元测试

```kotlin
// mpp-core/src/iosTest/kotlin/McpClientManagerTest.kt
class McpClientManagerTest {
    @Test
    fun testInitialize() = runTest {
        val manager = McpClientManager()
        val config = McpConfig(mcpServers = emptyMap())
        manager.initialize(config)
        // 验证初始化成功
    }
}
```

### 集成测试

在 `mpp-ios` 应用中创建测试场景：

1. 连接到本地 MCP 服务器
2. 发现工具列表
3. 执行工具调用
4. 处理错误情况

## 限制与注意事项

1. **iOS 沙盒限制**: 
   - stdio transport 在真机上可能无法使用
   - 建议使用 HTTP/SSE transport

2. **性能考虑**:
   - Swift ↔ Kotlin 互操作有一定开销
   - 大量数据传输时考虑使用共享内存

3. **版本兼容性**:
   - Swift MCP SDK 版本需与 Kotlin SDK 保持兼容
   - 定期更新依赖以获取最新功能

## 参考资料

- [Swift MCP SDK](https://github.com/modelcontextprotocol/swift-sdk)
- [Kotlin/Native Interop](https://kotlinlang.org/docs/native-objc-interop.html)
- [MCP Specification](https://spec.modelcontextprotocol.io/)
- [AutoDev JVM Implementation](../mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/mcp/)

