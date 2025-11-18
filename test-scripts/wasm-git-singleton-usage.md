# WASM Git 单例实例使用指南

## 问题与解决方案

### 问题
在 WebAssembly 平台上，每次创建新的 `GitOperations` 实例会导致：
- 重复初始化 wasm-git 模块
- 工作目录状态丢失
- 已克隆的仓库数据无法复用

### 解决方案
创建 `WasmGitManager` 单例管理器，统一管理共享的 `GitOperations` 实例。

## 架构设计

```
WasmGitManager (Singleton)
    └── GitOperations (Shared Instance)
            ├── WasmGitViewModel
            ├── WasmGitCloneScreen  
            └── 其他组件
```

## 修改文件

1. **新建** `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/wasm/WasmGitManager.kt`
   - 提供全局单例 GitOperations 实例管理
   - 支持自定义工作目录（默认 `/workspace`）
   - 提供 `reset()` 方法用于测试

2. **修改** `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/wasm/WasmGitViewModel.kt`
   - 构造函数接受 `GitOperations` 参数，默认使用 `WasmGitManager.getInstance()`
   - 移除内部创建新实例的逻辑
   - 所有操作使用共享实例

3. **修改** `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/wasm/WasmGitCloneScreen.kt`
   - 使用 `WasmGitManager.getInstance()` 创建 ViewModel
   - 通过 `remember` 确保 ViewModel 在重组时保持稳定

## 使用方式

### 基本使用（推荐）
```kotlin
@Composable
fun MyScreen() {
    // 自动使用共享实例
    WasmGitCloneScreen()
}
```

### 自定义场景
```kotlin
// 自定义工作目录
val gitOps = WasmGitManager.getInstance(projectPath = "/custom/path")
val viewModel = WasmGitViewModel(gitOperations = gitOps)
WasmGitCloneScreen(viewModel = viewModel)
```

## 优势

1. ✅ **性能提升** - 避免重复初始化 wasm-git 模块
2. ✅ **状态保持** - 工作目录和克隆的仓库数据跨界面共享
3. ✅ **代码简化** - 统一的实例管理
4. ✅ **向后兼容** - 现有调用代码无需修改

## 验证

- ✅ 编译 `mpp-core:compileKotlinWasmJs` 
- ✅ 编译 `mpp-ui:compileKotlinWasmJs`
- ✅ 无 linter 错误
- ✅ 所有现有调用点兼容

