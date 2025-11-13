# Session 创建与执行指南

## 功能概述

现在可以通过 UI 界面创建 AI Agent 会话并执行任务，支持本地项目和 Git 远程项目。

## 使用流程

### 1. 打开会话列表

- 登录后自动进入会话列表界面
- 显示所有会话，可按"进行中"/"全部"筛选

### 2. 创建新会话

点击右下角 **+** 按钮，打开创建会话对话框。

#### 步骤 1: 选择项目源

**选项 A：现有项目 (Existing Project)**
- 从当前 Workspace 中选择已打开的项目
- 适合本地开发场景

**选项 B：Git 仓库 (Git Repository)**
- 输入 Git URL（例如：`https://github.com/user/repo.git`）
- 可选配置：
  - **Branch**：分支名（默认 `main`）
  - **Username**：Git 用户名（私有仓库需要）
  - **Password/Token**：Git 密码或 Personal Access Token

#### 步骤 2: 描述任务

使用集成的 **DevInEditorInput** 编辑器输入任务需求：

**支持的功能：**
- `/` - 命令补全（如 `/write`, `/init`, `/clear`）
- `@` - Agent 补全（如 `@architect`, `@reviewer`）
- `Ctrl+P` - 智能提示词增强

**示例任务：**

```
/write Implement user authentication feature

Requirements:
- Create login API endpoint
- Add JWT token generation
- Store user sessions in database

@architect Please design the database schema first
```

#### 步骤 3: 高级设置（可选）

- **Max Iterations**：Agent 最大迭代次数（5-50，默认 20）

#### 步骤 4: 创建并启动

点击 **"Create & Start"** 按钮：

1. 创建 Session
2. 如果选择了 Git 项目，服务端会自动 Clone 仓库
3. 启动 Agent 执行任务
4. 自动订阅会话事件流（SSE），实时查看执行过程

### 3. 查看执行过程

创建后自动跳转到会话详情页面：

- **实时输出**：看到 Agent 的思考过程和工具调用
- **状态变化**：
  - `PENDING` - 等待中
  - `RUNNING` - 执行中
  - `COMPLETED` - 完成
  - `FAILED` - 失败

### 4. 多端协同

**其他客户端可以：**
- 在会话列表看到新创建的会话
- 点击进入查看实时执行过程
- 所有事件通过 SSE 同步到各个客户端

## 技术架构

### 客户端 (mpp-ui)

```
CreateSessionDialog
  ↓
SessionViewModel.createSession()
  ↓
SessionViewModel.executeSession() / executeSessionWithGit()
  ↓
SessionClient.executeSession()
  ↓
SSE Stream ← Session Events
```

### 服务端 (mpp-server)

```
POST /api/sessions
  ↓
SessionManager.createSession()
  ↓
POST /api/sessions/:id/execute
  ↓
AgentService.executeAgentStream()
  ├─ Git Clone (if gitUrl provided)
  └─ Agent Execution
       ↓
GET /api/sessions/:id/stream (SSE)
  ↓
ServerSideRenderer → AgentEvents
```

## API 说明

### 创建会话

**POST** `/api/sessions`

```json
{
  "projectId": "github.com/user/repo",
  "task": "Implement user authentication"
}
```

### 执行会话

**POST** `/api/sessions/:id/execute`

```json
{
  "gitUrl": "https://github.com/user/repo.git",  // 可选
  "branch": "main",                               // 可选
  "username": "user",                             // 可选
  "password": "token"                             // 可选
}
```

### 订阅事件流

**GET** `/api/sessions/:id/stream`

返回 Server-Sent Events (SSE) 流：

```
event: session_event
data: {
  "sessionId": "xxx",
  "eventId": "xxx",
  "eventType": "AGENT_EVENT",
  "payload": "{...}"
}
```

## 示例场景

### 场景 1：本地项目重构

1. 打开本地项目 Workspace
2. 创建 Session，选择"Existing Project"
3. 输入任务：`/refactor Migrate from REST to GraphQL`
4. 点击创建，Agent 开始重构代码

### 场景 2：远程项目修复 Bug

1. 创建 Session，选择"Git Repository"
2. 输入 Git URL：`https://github.com/acme/project.git`
3. 输入任务：`/fix Fix memory leak in user service`
4. 点击创建，Agent 自动 Clone 并修复

### 场景 3：多人协作

**开发者 A（桌面端）：**
- 创建 Session，启动任务
- 看到 Agent 正在执行

**开发者 B（移动端）：**
- 打开会话列表，看到 A 创建的会话
- 点击进入，实时看到 Agent 的执行过程
- 状态同步：RUNNING → COMPLETED

## 下一步

完成后，您可以：
- 在会话详情页查看完整的执行历史
- 查看生成的代码修改
- 重新执行失败的会话
- 分享会话链接给团队成员

---

**注意事项：**
- 确保 `mpp-server` 已启动（默认端口 8080）
- Git 私有仓库需要提供凭据
- Agent 执行过程中请勿关闭客户端，否则无法看到实时更新

