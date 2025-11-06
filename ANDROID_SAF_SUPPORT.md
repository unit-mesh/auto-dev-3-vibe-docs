# Android Storage Access Framework (SAF) Support

## 问题描述

在 Android 上使用 WriteFileTool 写入文件时，遇到以下错误：

```
Error: Failed to write file: content:/com.android.externalstorage.documents/tree/primary%3ADownload%2Fdemo/HelloWorld.java: open failed: ENOENT (No such file or directory) Error Type: FILE_ACCESS_DENIED
```

## 根本原因

Android 使用 Storage Access Framework (SAF) 返回 `content://` URI 而不是传统文件路径。原有的文件系统实现使用 Java NIO `Path` API，不支持 `content://` URI。

## 解决方案

### 1. 创建 AndroidToolFileSystem

创建了专门的 `AndroidToolFileSystem` 类，支持：

- **Content URI 识别**: 自动识别 `content://` URI
- **DocumentFile API**: 使用 AndroidX DocumentFile API 访问 SAF 文件
- **ContentResolver**: 通过 ContentResolver 读写文件
- **双模式支持**: 同时支持传统文件路径和 content:// URI

**位置**: `mpp-core/src/androidMain/kotlin/cc/unitmesh/agent/tool/filesystem/AndroidToolFileSystem.kt`

### 2. 平台特定的 CodingAgent 工厂

使用 expect/actual 模式创建平台特定的 CodingAgent：

- **Common**: 定义 `createPlatformCodingAgent` expect 函数
- **Android**: 实现 actual 函数，使用 AndroidToolFileSystem 和 AndroidActivityProvider 获取 Context
- **JVM**: 实现 actual 函数，使用默认 ToolFileSystem

**位置**:
- `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/PlatformCodingAgentFactory.kt`
- `mpp-ui/src/androidMain/kotlin/cc/unitmesh/devins/ui/compose/agent/PlatformCodingAgentFactory.android.kt`
- `mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/compose/agent/PlatformCodingAgentFactory.jvm.kt`

### 3. 扩展 CodingAgent 构造函数

修改 `CodingAgent` 构造函数，添加可选的 `fileSystem` 和 `shellExecutor` 参数：

```kotlin
class CodingAgent(
    private val projectPath: String,
    private val llmService: KoogLLMService,
    override val maxIterations: Int = 100,
    private val renderer: CodingAgentRenderer = DefaultCodingAgentRenderer(),
    private val fileSystem: ToolFileSystem? = null,  // 新增
    private val shellExecutor: ShellExecutor? = null  // 新增
)
```

### 4. 添加依赖

在 `mpp-core/build.gradle.kts` 的 androidMain 中添加：

```kotlin
implementation("androidx.documentfile:documentfile:1.0.1")
```

## 功能特性

### AndroidToolFileSystem 支持

1. **Content URI 操作**
   - 读取文件: `readFileFromContentUri()`
   - 写入文件: `writeFileToContentUri()` (自动创建不存在的文件)
   - 检查存在: `existsContentUri()`
   - 列出文件: `listFilesFromContentUri()`
   - 获取信息: `getFileInfoFromContentUri()`

2. **传统路径操作**
   - 完全支持传统文件路径
   - 自动路径解析
   - 目录创建

3. **MIME 类型支持**
   - 自动识别常见文件扩展名
   - 支持 .java, .kt, .js, .json, .xml, .md 等

## 使用示例

### Android 应用中

```kotlin
// MainActivity.kt 中已经设置了 Activity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidActivityProvider.setActivity(this)
        // ...
    }
}

// CodingAgent 会自动使用 AndroidToolFileSystem
val viewModel = CodingAgentViewModel(
    llmService = llmService,
    projectPath = rootPath,
    maxIterations = 100
)
```

### 文件操作

```kotlin
// 使用 SAF 选择目录
val dirPath = fileChooser.chooseDirectory(title = "选择项目目录")
// 返回: content://com.android.externalstorage.documents/tree/primary%3ADownload%2Fdemo

// WriteFileTool 会自动处理
val tool = WriteFileTool(androidFileSystem)
tool.execute(WriteFileParams(
    path = "$dirPath/HelloWorld.java",
    content = "public class HelloWorld { ... }"
))
```

## 测试

### 构建测试

```bash
# 构建 mpp-core Android 版本
./gradlew :mpp-core:assembleDebug

# 构建完整的 Android 应用
./gradlew :mpp-ui:assembleDebug
```

### 真机测试

1. 连接 Android 设备
2. 运行应用
3. 选择项目目录（SAF 会返回 content:// URI）
4. 使用 CodingAgent 创建/修改文件
5. 验证文件操作成功

## 技术细节

### Content URI 格式

```
content://com.android.externalstorage.documents/tree/primary%3ADownload%2Fdemo/document/primary%3ADownload%2Fdemo%2FHelloWorld.java
```

### DocumentFile API

```kotlin
val uri = Uri.parse(uriString)
val documentFile = DocumentFile.fromSingleUri(context, uri)
documentFile?.exists()
```

### 创建文件

```kotlin
val newFileUri = DocumentsContract.createDocument(
    contentResolver,
    parentUri,
    mimeType,
    fileName
)
```

## 已知限制

1. **Context 依赖**: 需要 Android Context，必须在 MainActivity 中设置 `AndroidActivityProvider`
2. **权限**: 需要用户授予存储访问权限（SAF 自动处理）
3. **性能**: Content URI 访问比直接文件路径略慢
4. **测试**: 单元测试需要模拟 Context

## 相关文件

### mpp-core
- `src/androidMain/kotlin/cc/unitmesh/agent/tool/filesystem/AndroidToolFileSystem.kt`
- `src/commonMain/kotlin/cc/unitmesh/agent/CodingAgent.kt`
- `build.gradle.kts` (添加 documentfile 依赖)

### mpp-ui  
- `src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/PlatformCodingAgentFactory.kt`
- `src/androidMain/kotlin/cc/unitmesh/devins/ui/compose/agent/PlatformCodingAgentFactory.android.kt`
- `src/jvmMain/kotlin/cc/unitmesh/devins/ui/compose/agent/PlatformCodingAgentFactory.jvm.kt`
- `src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/CodingAgentViewModel.kt`
- `src/androidMain/kotlin/cc/unitmesh/devins/ui/platform/FileChooser.android.kt`

## 未来改进

1. 添加文件监听支持
2. 优化 Content URI 批量操作性能
3. 支持更多 MIME 类型
4. 添加 SAF 树形目录遍历
5. 改进错误消息和日志






