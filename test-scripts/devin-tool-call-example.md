# DevIn Tool Call 渲染示例

## 功能概述

当 LLM 返回的响应中包含 `devin` language id 的代码块时，系统会自动将其解析为 ToolCall 并使用 `CombinedToolItem` 进行渲染。

## 核心改进

### 1. 路径解析

**问题**: LLM 返回的 path 参数通常是相对路径，直接使用会导致 ReadFile 等工具失败。

**解决方案**: `DevInBlockRenderer` 自动将相对路径转换为绝对路径：

```kotlin
// 获取当前工作空间根路径
val workspaceRoot = WorkspaceManager.currentWorkspace?.rootPath

// 解析相对路径为绝对路径
val relativePath = params["path"] as? String
val filePath = resolveAbsolutePath(relativePath, workspaceRoot)
```

### 2. 路径解析逻辑

```kotlin
private fun resolveAbsolutePath(relativePath: String?, workspaceRoot: String?): String? {
    if (relativePath == null) return null
    if (workspaceRoot == null) return relativePath
    
    // 如果已经是绝对路径，直接返回
    if (relativePath.startsWith("/") || relativePath.matches(Regex("^[A-Za-z]:.*"))) {
        return relativePath
    }
    
    // 组合工作空间根路径和相对路径
    val separator = if (workspaceRoot.endsWith("/") || workspaceRoot.endsWith("\\")) "" else "/"
    return "$workspaceRoot$separator$relativePath"
}
```

## 示例场景

### 场景 1: LLM 返回相对路径

**LLM 响应:**
```markdown
让我读取这个文件：

<devin>
/read-file

```json
{
  "path": "mpp-viewer-web/src/commonMain/kotlin/cc/unitmesh/viewer/web/MermaidRenderer.kt"
}
```

</devin>
```

**处理过程:**
1. `SketchRenderer` 检测到 `devin` language id
2. `DevInBlockRenderer` 解析内容，提取 ToolCall
3. 获取工作空间路径: `/Volumes/source/ai/autocrud`
4. 解析相对路径为绝对路径: `/Volumes/source/ai/autocrud/mpp-viewer-web/src/commonMain/kotlin/cc/unitmesh/viewer/web/MermaidRenderer.kt`
5. 将绝对路径传递给 `CombinedToolItem`
6. 用户可以点击 "View File" 按钮查看文件内容

### 场景 2: LLM 返回绝对路径

**LLM 响应:**
```markdown
<devin>
/read-file path="/absolute/path/to/file.kt"
</devin>
```

**处理过程:**
1. 检测到路径已经是绝对路径（以 `/` 开头）
2. 直接使用绝对路径，不做转换
3. 传递给 `CombinedToolItem` 进行渲染

### 场景 3: Windows 绝对路径

**LLM 响应:**
```markdown
<devin>
/read-file path="C:/Users/test/project/Example.kt"
</devin>
```

**处理过程:**
1. 检测到 Windows 绝对路径（匹配 `^[A-Za-z]:.*` 模式）
2. 直接使用，不做转换
3. 传递给 `CombinedToolItem` 进行渲染

## UI 渲染效果

当 `isComplete = true` 时，ToolCall 会被渲染为：

```
┌─────────────────────────────────────────────────┐
│ ▶ read-file  path=mpp-viewer-web/src/...  👁   │
│                                                 │
│ Parameters:                                     │
│   path: mpp-viewer-web/src/commonMain/...      │
└─────────────────────────────────────────────────┘
```

点击 👁 (View File) 按钮可以打开文件查看器，显示完整的文件内容。

## 测试

运行测试：
```bash
./gradlew :mpp-ui:jvmTest --tests "DevInBlockRendererTest"
./gradlew :mpp-ui:jvmTest --tests "PathResolutionTest"
```

## 文件清单

- **DevInBlockRenderer.kt**: 主渲染器，处理 devin 块的解析和渲染
  - 路径: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/sketch/DevInBlockRenderer.kt`
  
- **SketchRenderer.kt**: 主渲染器集成点
  - 路径: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/sketch/SketchRenderer.kt`
  - 添加了 `"devin"` 分支处理

- **CombinedToolItem.kt**: ToolCall UI 组件
  - 路径: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/ToolCallItem.kt`
  - 接收绝对路径用于文件查看功能

- **ToolCallParser.kt**: ToolCall 解析器
  - 路径: `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/parser/ToolCallParser.kt`
  - 从 devin 块中提取工具调用

## 关键点

1. ✅ **自动路径解析**: 相对路径自动转换为绝对路径
2. ✅ **跨平台支持**: 支持 Unix 和 Windows 路径格式
3. ✅ **智能判断**: 自动识别绝对路径和相对路径
4. ✅ **工作空间感知**: 使用当前工作空间根路径进行解析
5. ✅ **流式渲染**: 未完成时显示为代码块，完成后渲染为 ToolCall UI

