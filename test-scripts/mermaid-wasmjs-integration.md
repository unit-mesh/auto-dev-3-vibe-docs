# Mermaid WasmJS Integration

## 概述

成功将 Mermaid 渲染器集成到 wasmJs 平台，使其可以直接使用本地的 mermaid.html 资源，而不需要依赖远程服务。

## 实现内容

### 1. 创建 wasmJs 平台的 actual 实现

**文件**: `mpp-viewer-web/src/wasmJsMain/kotlin/cc/unitmesh/viewer/web/MermaidHtml.wasmJs.kt`

- 实现了 `getMermaidHtml()` 函数的 wasmJs actual 版本
- 将 mermaid.html 的完整内容内联到 Kotlin 代码中
- 包含完整的主题配置（dark/light）
- 包含 Mermaid.js CDN 加载逻辑
- 支持 JSBridge 回调机制

**文件**: `mpp-viewer-web/src/wasmJsMain/kotlin/cc/unitmesh/viewer/web/ViewerWebView.wasmJs.kt`

- 实现了 `getViewerHtml()` 函数的 wasmJs actual 版本
- 提供基本的代码查看器功能
- 支持内容显示和清除

### 2. 更新 Gradle 配置

**文件**: `mpp-viewer-web/build.gradle.kts`

- 添加了 wasmJs 目标配置
- 配置了必要的编译选项和依赖
- 添加了 @OptIn 注解以支持实验性 WASM DSL

### 3. 修复 MermaidRenderer 编译错误

**文件**: `mpp-viewer-web/src/commonMain/kotlin/cc/unitmesh/viewer/web/MermaidRenderer.kt`

- 移除了有问题的 timeout LaunchedEffect 块
- 简化了协程作用域的使用

## 使用方式

在 wasmJs 平台上，可以直接使用 `MermaidRenderer` 组件：

```kotlin
MermaidRenderer(
    mermaidCode = """
        graph TD
            A[Start] --> B{Is it working?}
            B -->|Yes| C[Great!]
            B -->|No| D[Debug]
    """,
    isDarkTheme = true,
    modifier = Modifier.fillMaxSize()
)
```

## 技术特点

1. **无需远程依赖**: wasmJs 平台不再需要调用 Kroki 或其他远程服务
2. **完整的主题支持**: 支持 IntelliJ IDEA 的 dark/light 主题
3. **统一的API**: 所有平台（JVM, wasmJs）使用相同的组件接口
4. **JSBridge 集成**: 支持渲染回调和高度自适应

## 编译和测试

```bash
# 编译 mpp-viewer-web 模块
./gradlew :mpp-viewer-web:build

# 更新 package lock（如果需要）
./gradlew kotlinWasmUpgradePackageLock
```

## 与 JVM 平台的对比

| 特性 | JVM | wasmJs |
|------|-----|--------|
| HTML 加载方式 | 从 resources 加载 | 内联到代码中 |
| Mermaid.js 来源 | CDN | CDN（相同） |
| 主题支持 | ✅ | ✅ |
| JSBridge 回调 | ✅ | ✅ |
| 离线使用 | ❌（需要CDN） | ❌（需要CDN） |

## 后续改进空间

1. **离线支持**: 可以考虑将 mermaid.js 也内联或打包到 WASM bundle 中
2. **性能优化**: 考虑使用 tree-shaking 减小 bundle 大小
3. **错误处理**: 增强 CDN 加载失败时的 fallback 机制

## 测试结果

✅ wasmJs 编译成功  
✅ JVM 编译成功  
✅ 无 linter 错误  
✅ 构建通过

---

**日期**: 2025-11-21  
**作者**: AI Assistant  
**相关模块**: mpp-viewer-web

