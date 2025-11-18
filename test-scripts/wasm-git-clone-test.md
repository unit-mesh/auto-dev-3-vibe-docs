# Wasm Git Clone 功能测试文档

## 功能概述

在 WebAssembly (Wasm) 平台上实现了 Git 仓库克隆功能，允许用户在浏览器中直接克隆 Git 仓库并查看提交历史。

## 实现的组件

### 1. GitOperations (mpp-core)

**文件**: `mpp-core/src/wasmJsMain/kotlin/cc/unitmesh/agent/platform/GitOperations.wasmJs.kt`

- 使用 `wasm-git` (libgit2 编译为 WASM) 提供 Git 功能
- 实现了以下功能：
  - `clone(repoUrl, targetDir)`: 克隆仓库
  - `getRecentCommits(count)`: 获取最近的提交历史
  - `getModifiedFiles()`: 获取修改的文件列表
  - `getFileDiff(filePath)`: 获取文件差异

### 2. performClone expect/actual 函数 (mpp-ui)

**文件**:
- Common: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/wasm/WasmGitViewModel.kt` (expect 声明)
- Wasm: `mpp-ui/src/wasmJsMain/kotlin/cc/unitmesh/devins/ui/wasm/WasmGitViewModelExt.wasmJs.kt` (actual 实现)
  - 包含 `WasmGitOperations` 辅助类用于访问 Wasm 特有的 `clone()` 方法
- 其他平台: `mpp-ui/src/{platform}Main/kotlin/cc/unitmesh/devins/ui/wasm/WasmGitViewModelExt.{platform}.kt` (stub 实现)

**架构说明**:
- `clone()` 方法**不在** `GitOperations` 的 expect class 中，因为它是 Wasm 平台特有的功能
- 在 Wasm 平台，`GitOperations.wasmJs.kt` 有一个平台特有的 `clone()` 方法
- `WasmGitOperations` 辅助类在 wasmJS 源集中直接访问这个平台特有的方法

### 3. WasmGitViewModel (mpp-ui)

**文件**: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/wasm/WasmGitViewModel.kt`

管理 UI 状态和 Git 操作：
- 仓库 URL 输入
- 目标目录配置
- 克隆进度和状态管理
- 日志收集和显示
- 提交历史管理

### 4. WasmGitCloneScreen (mpp-ui)

**文件**: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/wasm/WasmGitCloneScreen.kt`

用户界面组件，提供：
- 仓库 URL 输入框 (默认: https://github.com/unit-mesh/untitled)
- 目标目录输入框 (可选)
- 克隆按钮
- 实时日志输出显示 (终端风格)
- 提交历史列表显示

### 5. 集成到主应用 (AutoDevApp)

- 在顶部菜单栏添加了 Git Clone 按钮 (CloudDownload 图标)
- 仅在 Platform.isWasm 时显示
- 点击按钮打开 WasmGitCloneScreen 对话框

## 测试步骤

### 1. 构建项目

```bash
cd /Volumes/source/ai/autocrud

# 构建 mpp-core (Wasm 平台)
./gradlew :mpp-core:wasmJsBrowserDevelopmentLibraryDistribution

# 构建 mpp-ui (Wasm 平台)
./gradlew :mpp-ui:wasmJsBrowserDevelopmentExecutableDistribution
```

### 2. 运行 Wasm 应用

```bash
cd mpp-ui
npm run build
npm run start
```

或者使用 Gradle:

```bash
./gradlew :mpp-ui:wasmJsBrowserDevelopmentRun
```

### 3. 测试 Git Clone 功能

1. 在浏览器中打开应用
2. 点击顶部菜单栏的云下载图标 (CloudDownload)
3. 在弹出的对话框中：
   - 输入仓库 URL (默认: https://github.com/unit-mesh/untitled)
   - 可选输入目标目录
   - 点击 "Clone Repository" 按钮
4. 观察：
   - 控制台输出窗口显示实时 Git 操作日志
   - 克隆成功后显示成功消息
   - 自动加载并显示最近 20 条提交历史
5. 可以点击 "Fetch Commits" 按钮刷新提交历史

## 日志类型

UI 中显示的日志分为以下类型：

- ✓ **SUCCESS** (绿色): 操作成功
- ✗ **ERROR** (红色): 错误信息
- ⚠ **WARNING** (橙色): 警告信息
- ℹ **INFO** (蓝色): 一般信息
- → **DEBUG** (灰色): 调试信息

## 示例输出

```
ℹ WebAssembly Git client initialized
→ Default repository: https://github.com/unit-mesh/untitled
ℹ Starting repository clone...
→ Repository: https://github.com/unit-mesh/untitled
ℹ Cloning repository: https://github.com/unit-mesh/untitled
[Git] Cloning into 'untitled'...
[Git] remote: Enumerating objects: 142, done.
[Git] remote: Counting objects: 100% (142/142), done.
[Git] Receiving objects: 100% (142/142), 45.67 KiB | 2.09 MiB/s, done.
[Git] Resolving deltas: 100% (56/56), done.
✓ Repository cloned successfully
✓ Clone completed successfully!
ℹ Fetching 20 recent commits...
✓ Found 20 commits
```

## 故障排查

### 问题 1: wasm-git 未加载

**症状**: 控制台显示 "Failed to initialize wasm-git"

**解决方案**:
1. 确保 `wasm-git` npm 包已正确安装
2. 检查 `package.json` 中是否包含 `wasm-git` 依赖
3. 重新构建: `npm install && npm run build`

### 问题 2: 克隆失败

**症状**: 显示 "Clone failed with exit code: X"

**可能原因**:
1. 仓库 URL 不正确
2. 仓库不存在或无访问权限
3. 网络连接问题

**解决方案**:
1. 验证仓库 URL 是否正确
2. 尝试使用公开仓库进行测试
3. 检查浏览器控制台是否有 CORS 错误

### 问题 3: 日志不显示

**症状**: 克隆过程中没有日志输出

**解决方案**:
1. 检查浏览器控制台是否有错误
2. 确认 `WasmConsole` 是否正常工作
3. 查看 `commandOutputBuffer` 是否正确收集输出

## 技术细节

### wasm-git 集成

项目使用 [wasm-git](https://github.com/petersalomonsen/wasm-git)，这是 libgit2 编译为 WebAssembly 的版本。

### 跨平台设计

**核心原则**: 直接使用 `mpp-core` 中的 `GitOperations` expect/actual 类

1. **mpp-core 层**: `GitOperations` 提供跨平台的 Git 基础操作
   - JVM: 调用系统 git 命令
   - Wasm: 使用 wasm-git (libgit2 for WebAssembly)
   - 其他平台: Stub 实现

2. **mpp-ui 层**: 只需要为 `clone` 方法添加平台特定的调用
   - `performClone` expect/actual 函数
   - Wasm 平台: 通过 dynamic 调用 `GitOperations.clone()`
   - 其他平台: 返回 false

### 日志收集

日志通过 `GitOperations.wasmJs.kt` 中的 `createModuleConfig` 自动收集到 `commandOutputBuffer`：

```kotlin
val config = createModuleConfig(
    onPrint = { text ->
        WasmConsole.log("[Git] $text")
        commandOutputBuffer.add(text)
    },
    onPrintErr = { text ->
        WasmConsole.error("[Git Error] $text")
    }
)
```

ViewModel 通过查询 `GitOperations` 的内部状态来获取这些日志。

## 后续改进建议

1. **进度条**: 添加克隆进度的可视化显示
2. **文件浏览**: 克隆后允许浏览仓库文件
3. **认证支持**: 支持私有仓库的认证
4. **错误恢复**: 更好的错误处理和重试机制
5. **性能优化**: 大仓库的增量克隆支持
6. **本地存储**: 使用 IndexedDB 持久化克隆的仓库

## 相关文件

### mpp-core (Git 核心实现)
- **Common**: `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/platform/GitOperations.kt` (expect 定义)
- **Wasm 实现**: `mpp-core/src/wasmJsMain/kotlin/cc/unitmesh/agent/platform/GitOperations.wasmJs.kt`
- **平台桥接**: `mpp-core/src/wasmJsMain/kotlin/cc/unitmesh/agent/platform/WasmGitInterop.kt`

### mpp-ui (UI 层)
- **UI 组件**: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/wasm/WasmGitCloneScreen.kt`
- **ViewModel**: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/wasm/WasmGitViewModel.kt`
- **Wasm 扩展**: `mpp-ui/src/wasmJsMain/kotlin/cc/unitmesh/devins/ui/wasm/WasmGitViewModelExt.wasmJs.kt`
- **其他平台**: `mpp-ui/src/{platform}Main/kotlin/cc/unitmesh/devins/ui/wasm/WasmGitViewModelExt.{platform}.kt`
- **图标**: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/icons/AutoDevComposeIcons.kt`

## 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                         mpp-ui                               │
│  ┌────────────────────────────────────────────────────┐    │
│  │  WasmGitCloneScreen (UI)                           │    │
│  │          ↓                                          │    │
│  │  WasmGitViewModel                                   │    │
│  │          ↓                                          │    │
│  │  performClone() ← expect/actual                     │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                         ↓ (直接调用)
┌─────────────────────────────────────────────────────────────┐
│                        mpp-core                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │  GitOperations ← expect class                       │    │
│  │          ↓                                          │    │
│  │  Wasm 平台: GitOperations.wasmJs.kt                │    │
│  │  • clone(url, dir)                                  │    │
│  │  • getRecentCommits(count)                          │    │
│  │  • getModifiedFiles()                               │    │
│  │          ↓                                          │    │
│  │  wasm-git (libgit2 WASM)                            │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

