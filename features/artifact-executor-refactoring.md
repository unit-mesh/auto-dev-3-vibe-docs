# ArtifactExecutor 重构文档

## 概述

根据 [Issue #526](https://github.com/phodal/auto-dev/issues/526) 的需求，将 `ArtifactExecutor` 重构为支持多种 artifact 类型的执行器架构。

## 重构目标

支持三种主要的 artifact 类型执行：

1. **Web Artifacts (HTML/JS)**: 启动本地 HTTP 服务器预览
2. **Node.js Artifacts**: npm install + node 执行
3. **Python Artifacts**: pip install + python 执行

## 架构设计

### 1. 基础接口

**`ArtifactExecutor`** (`executor/ArtifactExecutor.kt`)
- 定义了统一的执行器接口
- 包含 `execute()` 和 `validate()` 方法
- 定义了 `ExecutionResult` 和 `ValidationResult`

### 2. 具体实现

#### `NodeJsArtifactExecutor`
- 支持 `NODEJS` 和 `REACT` 类型
- 功能：
  - 验证 package.json 和 index.js/index.jsx
  - 运行 `npm install` 安装依赖
  - 执行 `node index.js` 或 `node index.jsx`

#### `PythonArtifactExecutor`
- 支持 `PYTHON` 类型
- 功能：
  - 解析 PEP 723 内联元数据获取依赖
  - 运行 `pip install -r requirements.txt` 安装依赖
  - 执行 `python3 index.py`

#### `WebArtifactExecutor`
- 支持 `HTML` 和 `SVG` 类型
- 功能：
  - 启动本地 HTTP 服务器（Python http.server 或 Node.js http-server）
  - 自动查找可用端口（8000-9000）
  - 返回服务器 URL 供浏览器访问

### 3. 工厂模式

**`ArtifactExecutorFactory`**
- 统一的入口点：`executeArtifact(unitFilePath, onOutput)`
- 自动处理：
  1. 解压 .unit 文件
  2. 从 ARTIFACT.md 确定 artifact 类型
  3. 选择合适的执行器
  4. 执行 artifact

### 4. 向后兼容

保留了旧的 `ArtifactExecutor` 对象，标记为 `@Deprecated`，内部委托给新的工厂。

## 文件结构

```
mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/artifact/
├── executor/
│   ├── ArtifactExecutor.kt          # 基础接口和结果类型
│   ├── NodeJsArtifactExecutor.kt     # Node.js 执行器
│   ├── PythonArtifactExecutor.kt     # Python 执行器
│   ├── WebArtifactExecutor.kt        # Web 执行器
│   └── ArtifactExecutorFactory.kt    # 工厂类
└── ArtifactExecutor.kt               # 旧接口（已废弃，保持兼容性）
```

## 使用示例

### 直接使用工厂（推荐）

```kotlin
val result = ArtifactExecutorFactory.executeArtifact(
    unitFilePath = "/path/to/artifact.unit",
    onOutput = { line -> println(line) }
)

when (result) {
    is ExecutionResult.Success -> {
        println("Output: ${result.output}")
        result.serverUrl?.let { url ->
            println("Server URL: $url")
        }
    }
    is ExecutionResult.Error -> {
        println("Error: ${result.message}")
    }
}
```

### 使用特定执行器

```kotlin
val executor = NodeJsArtifactExecutor()
val result = executor.execute(
    extractDir = File("/extracted/dir"),
    bundleType = ArtifactType.NODEJS,
    onOutput = { line -> println(line) }
)
```

## 执行流程

1. **解压阶段**
   - 将 .unit 文件解压到临时目录
   - 验证 ZIP 文件完整性

2. **类型识别**
   - 读取 ARTIFACT.md 的 frontmatter
   - 提取 `type` 字段
   - 映射到 `ArtifactType` 枚举

3. **验证阶段**
   - 调用执行器的 `validate()` 方法
   - 检查必需文件是否存在
   - 验证文件内容格式

4. **依赖安装**
   - Node.js: `npm install`
   - Python: `pip install -r requirements.txt`
   - Web: 无需安装依赖

5. **执行阶段**
   - Node.js: `node index.js`
   - Python: `python3 index.py`
   - Web: 启动 HTTP 服务器

## 特性

### Node.js 执行器
- ✅ 支持 npm 依赖安装
- ✅ 支持 ES modules (`"type": "module"`)
- ✅ 支持 React (JSX) 文件
- ✅ 验证文件内容（防止 JSON 误写入 JS 文件）

### Python 执行器
- ✅ 支持 PEP 723 内联元数据解析
- ✅ 自动生成 requirements.txt
- ✅ 支持 pip 依赖安装
- ✅ 使用 python3 执行

### Web 执行器
- ✅ 自动查找可用端口
- ✅ 支持 Python http.server（优先）
- ✅ 支持 Node.js http-server（备选）
- ✅ 返回服务器 URL

## 错误处理

所有执行器都包含完整的错误处理：
- 验证失败时返回明确的错误信息
- 依赖安装失败时继续执行（带警告）
- 执行失败时返回详细的错误信息

## 未来扩展

可以轻松添加新的执行器类型：
1. 实现 `ArtifactExecutor` 接口
2. 在 `ArtifactExecutorFactory` 中注册
3. 支持新的 `ArtifactType`

例如：
- `DockerArtifactExecutor`: 构建和运行 Docker 容器
- `JavaArtifactExecutor`: 编译和运行 Java 应用
- `GoArtifactExecutor`: 编译和运行 Go 应用

## 测试

测试文件：
- `ArtifactExecutorTest.kt`: 基础功能测试
- `NodeJsArtifactBundleTest.kt`: Node.js 特定测试

运行测试：
```bash
./gradlew :mpp-core:test
```

## 迁移指南

### 从旧 API 迁移

**旧代码：**
```kotlin
val result = ArtifactExecutor.executeNodeJsArtifact(unitFilePath, onOutput)
```

**新代码：**
```kotlin
val result = ArtifactExecutorFactory.executeArtifact(unitFilePath, onOutput)
```

### 结果类型变化

**旧类型：**
```kotlin
ArtifactExecutor.ExecutionResult.Success(output, workingDirectory)
```

**新类型：**
```kotlin
executor.ExecutionResult.Success(
    output, 
    workingDirectory, 
    serverUrl,  // 新增：Web artifacts 的服务器 URL
    processId   // 新增：长时间运行进程的 ID
)
```

## 相关 Issue

- [AutoDev Unit - the Artifact builder #526](https://github.com/phodal/auto-dev/issues/526)

