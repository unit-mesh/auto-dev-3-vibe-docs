# UnifiedApp 架构说明

## 概述

UnifiedApp 是一个统一的 Compose Multiplatform 应用，支持远程 Session 管理和 AI Agent 任务执行。它整合了之前的 SessionApp 功能，并提供了更好的用户体验。

## 架构设计

### 三段式侧边栏布局

UnifiedApp 采用三段式侧边栏设计，提供清晰的功能分区：

```
┌─────────────────┬──────────────────────────┐
│  顶部区域        │                          │
│  - 新建 Session  │                          │
│  - 新建 Project  │                          │
│  - 本地 Chat     │      主内容区域           │
├─────────────────┤                          │
│  中间区域        │                          │
│  - Sessions 列表 │                          │
│  - Projects 列表 │                          │
│  (可切换)        │                          │
├─────────────────┤                          │
│  底部区域        │                          │
│  - Settings      │                          │
│  - Profile       │                          │
│  - 退出登录      │                          │
└─────────────────┴──────────────────────────┘
```

### 核心组件

#### 1. UnifiedApp
- **位置**: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/app/UnifiedApp.kt`
- **功能**: 应用主入口，管理整体布局和状态
- **参数**:
  - `serverUrl`: 后端服务器地址
  - `onOpenLocalChat`: 打开本地 Chat 的回调（可选）

#### 2. SessionViewModel
- **位置**: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/session/SessionViewModel.kt`
- **功能**: 管理用户登录、Session 创建和执行
- **主要方法**:
  - `login(username, password)`: 用户登录
  - `createSession(projectId, task, metadata)`: 创建 Session
  - `executeSession(sessionId)`: 执行 Session

#### 3. ProjectViewModel
- **位置**: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/project/ProjectViewModel.kt`
- **功能**: 管理项目的 CRUD 操作
- **主要方法**:
  - `loadProjects()`: 加载项目列表
  - `createProject(request)`: 创建新项目
  - `deleteProject(projectId)`: 删除项目

#### 4. TaskViewModel
- **位置**: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/task/TaskViewModel.kt`
- **功能**: 管理任务创建和执行，集成 RemoteAgentClient
- **主要方法**:
  - `setCurrentProject(project)`: 设置当前项目
  - `createTask(description, maxIterations)`: 创建任务
  - `executeTask(sessionId)`: 执行任务并监听 Agent 事件

## 使用方式

### Desktop (Main.kt)

Main.kt 现在支持两种模式：

1. **本地 Chat 模式**（默认）
   - 使用 AutoDevApp
   - 本地 AI 对话功能

2. **远程 Session 模式**
   - 使用 UnifiedApp
   - 通过 `--remote` 参数启动
   - 或在运行时切换

```kotlin
// 启动远程模式
./gradlew :mpp-ui:run --args="--remote"
```

### Android (MainActivity.kt)

Android 使用 SessionApp，提供底部导航栏：

- **Sessions**: Session 列表和管理
- **Projects**: 项目列表和管理
- **Profile**: 用户信息和设置

### SessionDemoMain.kt

专门用于测试远程 Session 功能的 Demo 应用：

```kotlin
./gradlew :mpp-ui:SessionDemoMain
```

## 工作流程

### 1. 用户登录

```
用户输入用户名/密码 → SessionViewModel.login() → 获取 authToken → 更新 UI 状态
```

### 2. 创建项目

```
点击"新建 Project" → CreateProjectDialog → ProjectViewModel.createProject() → 刷新项目列表
```

### 3. 创建和执行任务

```
选择项目 → 点击"新建 Session" → CreateSessionDialog → 
SessionViewModel.createSession() → TaskViewModel.executeTask() → 
RemoteAgentClient.executeStream() → 实时显示 Agent 事件
```

## 技术栈

- **UI 框架**: Compose Multiplatform
- **状态管理**: StateFlow / MutableStateFlow
- **异步处理**: Kotlin Coroutines
- **网络请求**: Ktor Client
- **序列化**: kotlinx.serialization
- **事件流**: Server-Sent Events (SSE)

## 与后端集成

### SessionManager
- **位置**: `mpp-server/src/main/kotlin/cc/unitmesh/server/session/SessionManager.kt`
- **功能**: 管理 Session 生命周期和事件订阅

### AgentService
- **位置**: `mpp-server/src/main/kotlin/cc/unitmesh/server/service/AgentService.kt`
- **功能**: 执行 AI Agent 任务，通过 SSE 流式返回事件

### RemoteAgentClient
- **位置**: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/remote/RemoteAgentClient.kt`
- **功能**: 连接到 AgentService，接收 RemoteAgentEvent 流

## 未来改进

1. **持久化**: 使用本地数据库缓存 Session 和 Project 数据
2. **离线支持**: 支持离线创建任务，在线时同步
3. **实时协作**: 多用户同时查看和编辑 Session
4. **更多平台**: 支持 iOS 和 Web 平台
5. **性能优化**: 虚拟滚动、懒加载等

## 相关文件

### UI 组件
- `UnifiedApp.kt` - 主应用
- `SessionApp.kt` - Android 应用（底部导航）
- `ProfileScreen.kt` - 用户信息页面
- `CreateSessionDialog.kt` - 创建 Session 对话框
- `CreateProjectDialog.kt` - 创建 Project 对话框
- `CreateTaskDialog.kt` - 创建 Task 对话框
- `SessionDetailScreen.kt` - Session 详情页面
- `ProjectListScreen.kt` - 项目列表页面
- `TaskListScreen.kt` - 任务列表页面
- `TaskExecutionScreen.kt` - 任务执行监控页面

### ViewModel
- `SessionViewModel.kt` - Session 管理
- `ProjectViewModel.kt` - Project 管理
- `TaskViewModel.kt` - Task 管理

### 网络客户端
- `SessionClient.kt` - Session API 客户端
- `ProjectClient.kt` - Project API 客户端
- `RemoteAgentClient.kt` - Agent 执行客户端

### 入口文件
- `Main.kt` - Desktop 主入口
- `MainActivity.kt` - Android 主入口
- `SessionDemoMain.kt` - Session Demo 入口

