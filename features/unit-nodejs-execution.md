# AutoDev Unit - Node.js 执行支持

## 概述

本文档描述了 AutoDev Unit 对 Node.js 应用的支持，包括 `.unit` 文件内的 `npm install` 和执行功能。

## 实现功能

### 1. NODEJS 类型支持

- ✅ 在 `ArtifactType` 枚举中添加了 `NODEJS` 类型
- ✅ 在 `ArtifactAgent.Artifact.ArtifactType` 中添加了 `NODEJS` 类型
- ✅ 更新了 `ArtifactBundle` 以支持 Node.js 应用的元数据生成
- ✅ 更新了 `ArtifactAgentTemplate` 以包含 Node.js 类型说明

### 2. .unit 文件执行功能

实现了 `ArtifactExecutor` 类，支持：

- ✅ 解压 `.unit` 文件到临时目录
- ✅ 检查 `package.json` 是否存在
- ✅ 自动运行 `npm install` 安装依赖
- ✅ 执行 `node index.js` 运行应用
- ✅ 实时输出执行日志

### 3. UI 集成

在 `ArtifactPreviewPanel` 中添加了：

- ✅ Node.js 类型检测
- ✅ 执行按钮（播放图标）
- ✅ 执行状态指示器（加载动画）
- ✅ 终端输出显示区域
- ✅ 错误信息显示

## 使用方法

### 创建 Express.js 测试应用

运行以下命令创建测试用的 `.unit` 文件：

```bash
./gradlew :mpp-core:run --args="express"
```

这会在 `/tmp/express-test-app.unit` 创建一个包含 Express.js 应用的 `.unit` 文件。

### 在 AutoDev 中使用

1. **生成 Node.js Artifact**
   - 在 Artifact 模式下，请求生成一个 Express.js 应用
   - 例如："创建一个 Express.js REST API 服务器"

2. **导出为 .unit 文件**
   - 点击导出按钮
   - 选择保存为 `.unit` 格式

3. **执行应用**
   - 如果 artifact 类型是 NODEJS，会显示播放按钮
   - 点击播放按钮开始执行
   - 系统会：
     - 解压 `.unit` 文件
     - 运行 `npm install` 安装依赖
     - 执行 `node index.js` 启动应用
   - 终端输出会实时显示在预览面板中

### 手动测试

```bash
# 1. 生成测试 .unit 文件
./gradlew :mpp-core:run --args="express"

# 2. 解压文件
unzip /tmp/express-test-app.unit -d /tmp/express-extracted

# 3. 进入目录
cd /tmp/express-extracted

# 4. 安装依赖
npm install

# 5. 运行应用
node index.js
```

## 文件结构

### .unit 文件内容

```
express-test-app.unit/
├── ARTIFACT.md          # 元数据和说明
├── package.json         # Node.js 依赖和脚本
├── index.js            # Express.js 应用主文件
└── .artifact/
    └── context.json    # AI 上下文信息
```

### package.json 示例

```json
{
  "name": "express-test-app",
  "version": "1.0.0",
  "type": "module",
  "main": "index.js",
  "scripts": {
    "start": "node index.js",
    "setup": "npm install"
  },
  "dependencies": {
    "express": "^4.18.2"
  },
  "engines": {
    "node": ">=18"
  }
}
```

## 实现细节

### ArtifactExecutor

位置：`mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/artifact/ArtifactExecutor.kt`

主要功能：
- `executeNodeJsArtifact()`: 执行 Node.js artifact 的主函数
- `executeCommand()`: 执行 shell 命令并捕获输出

### ArtifactPreviewPanel

位置：`mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/compose/agent/artifact/ArtifactPreviewPanel.jvm.kt`

主要组件：
- `NodeJsExecutionView`: Node.js 执行视图组件
- `executeNodeJsArtifact()`: 执行逻辑的 UI 集成

## 未来改进

- [ ] 支持后台运行和停止功能
- [ ] 支持端口配置和冲突检测
- [ ] 支持环境变量配置
- [ ] 支持多个 Node.js 版本
- [ ] 支持其他 Node.js 框架（Koa, Fastify 等）
- [ ] 支持 Python 应用的类似执行功能

## 相关 Issue

- [AutoDev Unit - the Artifact builder #526](https://github.com/phodal/auto-dev/issues/526)

