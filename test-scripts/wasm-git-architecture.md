# Wasm Git Clone 架构说明

## 核心设计原则

**平台特有功能不应强制所有平台实现**

`clone()` 功能是 Wasm 平台特有的（通过 `wasm-git` 库实现），因此不应该在 `GitOperations` 的 `expect class` 中定义。

## 架构层次

### 1. mpp-core 层

#### `GitOperations.kt` (expect class)
- 定义**通用的** Git 操作接口
- 包含所有平台都可能支持的操作：
  - `getModifiedFiles()`
  - `getFileDiff(filePath)`
  - `getRecentCommits(count)`
  - `getTotalCommitCount()`
  - `getCommitDiff(commitHash)`
  - `getDiff(base, target)`
  - `isSupported()`

**不包含** `clone()` - 因为它是平台特有的！

#### `GitOperations.wasmJs.kt` (actual class for Wasm)
- 实现所有 expect class 中定义的方法
- **额外添加**平台特有的 `clone()` 方法（非 actual）
- 使用 `wasm-git` (libgit2) 库实现

```kotlin
actual class GitOperations actual constructor(private val projectPath: String) {
    // ... 实现所有 expect 方法 ...
    
    // Wasm 平台特有方法（注意：没有 actual 关键字）
    suspend fun clone(repoUrl: String, targetDir: String? = null): Boolean {
        // 使用 wasm-git 实现
    }
}
```

### 2. mpp-ui 层

#### `WasmGitViewModel.kt` (commonMain)
- 定义 `expect suspend fun performClone(repoUrl: String, targetDir: String?): Boolean`
- 在 common 代码中调用这个 expect 函数
- 各平台提供自己的 actual 实现

#### `WasmGitViewModelExt.wasmJs.kt` (wasmJsMain)
- 提供 `performClone` 的 actual 实现
- 创建 `WasmGitOperations` 辅助类来访问 Wasm 特有的 `clone()` 方法

```kotlin
actual suspend fun performClone(repoUrl: String, targetDir: String?): Boolean {
    val wasmGit = WasmGitOperations("/workspace")
    return wasmGit.clone(repoUrl, targetDir)
}

class WasmGitOperations(private val projectPath: String) {
    private val ops = cc.unitmesh.agent.platform.GitOperations(projectPath)
    
    suspend fun clone(repoUrl: String, targetDir: String? = null): Boolean {
        // 在 wasmJS 源集中，可以直接调用 GitOperations 的 clone 方法
        return ops.clone(repoUrl, targetDir)
    }
    
    // 提供其他 Git 操作的便捷访问
    suspend fun getRecentCommits(count: Int) = ops.getRecentCommits(count)
    suspend fun getModifiedFiles() = ops.getModifiedFiles()
    suspend fun getFileDiff(filePath: String) = ops.getFileDiff(filePath)
}
```

#### 其他平台的 stub 实现
- `WasmGitViewModelExt.jvm.kt` - 返回 false
- `WasmGitViewModelExt.js.kt` - 返回 false
- `WasmGitViewModelExt.android.kt` - 返回 false
- `WasmGitViewModelExt.ios.kt` - 返回 false

## 为什么这样设计？

### ✅ 优点

1. **解耦**: `GitOperations` 只包含通用功能，不强制所有平台实现不支持的操作
2. **类型安全**: 在 Wasm 平台可以直接调用 `clone()`，编译器知道这个方法存在
3. **清晰性**: 平台特有功能明确标识，不会误导开发者
4. **灵活性**: 其他平台可以选择性地添加自己的特有功能

### ❌ 之前的问题设计

**错误方式 1**: 在 expect class 中定义 clone()
```kotlin
// 错误！强制所有平台实现不需要的功能
expect class GitOperations(projectPath: String) {
    suspend fun clone(repoUrl: String, targetDir: String?): Boolean
}
```

**错误方式 2**: 创建额外的包装类
```kotlin
// 过度设计！已经有 GitOperations 了
class WasmGitOperationsWrapper {
    suspend fun clone() { ... }
}
```

### ✅ 正确方式

- expect class 只包含通用功能
- 平台特有功能在 actual 实现中额外添加
- 使用 expect/actual 函数在 UI 层访问平台特有功能

## 文件清单

### mpp-core
- `src/commonMain/kotlin/cc/unitmesh/agent/platform/GitOperations.kt` - expect class（通用接口）
- `src/wasmJsMain/kotlin/cc/unitmesh/agent/platform/GitOperations.wasmJs.kt` - actual 实现 + clone()
- `src/jvmMain/kotlin/cc/unitmesh/agent/platform/GitOperations.jvm.kt` - actual 实现
- `src/jsMain/kotlin/cc/unitmesh/agent/platform/GitOperations.js.kt` - actual 实现
- `src/androidMain/kotlin/cc/unitmesh/agent/platform/GitOperations.android.kt` - actual 实现
- `src/iosMain/kotlin/cc/unitmesh/agent/platform/GitOperations.ios.kt` - actual 实现

### mpp-ui
- `src/commonMain/kotlin/cc/unitmesh/devins/ui/wasm/WasmGitViewModel.kt` - ViewModel + expect performClone
- `src/wasmJsMain/kotlin/cc/unitmesh/devins/ui/wasm/WasmGitViewModelExt.wasmJs.kt` - actual performClone + WasmGitOperations
- `src/jvmMain/kotlin/cc/unitmesh/devins/ui/wasm/WasmGitViewModelExt.jvm.kt` - stub
- `src/jsMain/kotlin/cc/unitmesh/devins/ui/wasm/WasmGitViewModelExt.js.kt` - stub
- `src/androidMain/kotlin/cc/unitmesh/devins/ui/wasm/WasmGitViewModelExt.android.kt` - stub
- `src/iosMain/kotlin/cc/unitmesh/devins/ui/wasm/WasmGitViewModelExt.ios.kt` - stub
- `src/commonMain/kotlin/cc/unitmesh/devins/ui/wasm/WasmGitCloneScreen.kt` - UI 组件

## 总结

这个架构遵循了 Kotlin Multiplatform 的最佳实践：
1. Common 代码定义通用接口
2. 平台代码添加平台特有功能
3. 使用 expect/actual 机制桥接平台差异
4. 避免强制所有平台实现不相关的功能

