# 会话管理功能使用指南

## 📋 功能概述

已实现的多端协同功能包括：

1. **用户认证**：用户名密码登录/注册
2. **会话管理**：创建、查看、删除会话
3. **历史会话**：查看用户的所有历史会话
4. **实时同步**：订阅会话的实时事件流（SSE）
5. **多端查看**：多个客户端可以同时查看同一个会话的进度

## 🚀 快速开始

### 1. 启动 mpp-server

```bash
cd /Volumes/source/ai/autocrud
./gradlew :mpp-server:run
```

服务器将在 `http://localhost:8080` 启动

### 2. 运行 Demo 应用（Desktop）

```bash
./gradlew :mpp-ui:run -PmainClass=cc.unitmesh.devins.ui.SessionDemoMainKt
```

或者直接运行：

```bash
cd mpp-ui
../gradlew run -PmainClass=cc.unitmesh.devins.ui.SessionDemoMainKt
```

### 3. 使用流程

#### 3.1 登录/注册

1. 启动应用后，进入登录界面
2. 输入用户名和密码
3. 点击"注册"创建新账号，或"登录"使用已有账号
4. 默认测试账号：`admin` / `admin123`

#### 3.2 查看会话列表

登录成功后，进入会话列表界面：

- **进行中**：显示 RUNNING 或 PENDING 状态的会话
- **全部**：显示所有会话（包括已完成、失败等）
- 点击 **刷新** 按钮更新列表
- 点击 **+** 按钮创建新会话

#### 3.3 查看会话详情

点击任意会话卡片，进入会话详情界面：

- 实时显示会话事件流
- 自动滚动到最新事件
- 显示会话状态和事件数量
- 点击返回按钮回到列表

#### 3.4 实时同步测试

1. 在一个客户端创建会话并订阅
2. 在另一个客户端登录同一个账号
3. 打开相同的会话
4. 两个客户端会同时看到相同的事件流

## 🔧 API 端点

### 认证 API

| 方法 | 端点 | 描述 |
|------|------|------|
| POST | `/api/auth/login` | 用户登录 |
| POST | `/api/auth/register` | 用户注册 |
| POST | `/api/auth/logout` | 用户登出 |
| GET | `/api/auth/validate` | 验证 token |

### 会话 API

| 方法 | 端点 | 描述 |
|------|------|------|
| GET | `/api/sessions` | 获取当前用户的所有会话 |
| GET | `/api/sessions/active` | 获取当前用户的活跃会话 |
| POST | `/api/sessions` | 创建新会话 |
| GET | `/api/sessions/{id}` | 获取指定会话 |
| GET | `/api/sessions/{id}/state` | 获取会话状态快照 |
| GET | `/api/sessions/{id}/stream` | 订阅会话事件流（SSE） |
| POST | `/api/sessions/{id}/execute` | 启动会话执行 |
| DELETE | `/api/sessions/{id}` | 删除会话 |

## 📝 使用示例

### Kotlin 客户端

```kotlin
import cc.unitmesh.devins.ui.session.*

// 1. 创建客户端
val sessionClient = SessionClient("http://localhost:8080")
val viewModel = SessionViewModel(sessionClient)

// 2. 登录
val success = viewModel.login("admin", "admin123")

// 3. 创建会话
val session = viewModel.createSession(
    projectId = "my-project",
    task = "Implement user authentication",
    metadata = SessionMetadata(maxIterations = 50)
)

// 4. 订阅会话
viewModel.joinSession(session.id)

// 5. 观察事件
viewModel.sessionEvents.collect { events ->
    events.forEach { envelope ->
        println("Event: ${envelope.eventType}")
    }
}
```

### cURL 测试

```bash
# 1. 登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 响应：{"success":true,"username":"admin","token":"xxx"}

# 2. 创建会话
curl -X POST http://localhost:8080/api/sessions \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "projectId": "test-project",
    "task": "Test task",
    "userId": "admin"
  }'

# 3. 获取会话列表
curl http://localhost:8080/api/sessions \
  -H "Authorization: Bearer {token}"

# 4. 订阅会话事件（SSE）
curl -N http://localhost:8080/api/sessions/{sessionId}/stream \
  -H "Authorization: Bearer {token}"
```

## 🛠️ 架构说明

### 数据模型

- **Session**: 会话模型（id, projectId, task, status, ownerId, createdAt, updatedAt）
- **SessionEventEnvelope**: 会话事件包装器（sessionId, eventId, timestamp, sequenceNumber, eventType, eventData）
- **SessionState**: 会话状态快照（sessionId, status, currentIteration, maxIterations, events）

### 状态流转

```
PENDING → RUNNING → COMPLETED
                  ↘ FAILED
                  ↘ CANCELLED
```

### 事件类型

- `iteration`: 迭代开始
- `llm_chunk`: LLM 响应片段
- `tool_call`: 工具调用
- `tool_result`: 工具执行结果
- `clone_log`: Git 克隆日志
- `clone_progress`: Git 克隆进度
- `error`: 错误信息
- `complete`: 完成

## 🔍 调试技巧

### 1. 查看服务器日志

```bash
tail -f ~/.autodev/logs/autodev-app.log
```

### 2. 测试 SSE 连接

```bash
curl -N http://localhost:8080/api/sessions/{sessionId}/stream \
  -H "Authorization: Bearer {token}" \
  -H "Accept: text/event-stream"
```

### 3. 查看数据库

会话数据存储在 SQLDelight 数据库中：

- JVM: `~/.autodev/devins.db`
- Android: `/data/data/cc.unitmesh.devins.ui/databases/devins.db`

## ⚠️ 注意事项

1. **认证**: 当前使用简单的 token 认证，生产环境应使用 JWT
2. **存储**: 当前使用内存存储（mpp-server），重启后数据会丢失
3. **权限**: 当前只有会话 owner 可以查看和操作会话
4. **SSE**: 确保客户端支持 Server-Sent Events
5. **CORS**: 如果 Web 客户端跨域访问，需要配置 CORS

## 🎯 下一步

已完成的功能：

- ✅ 用户认证（登录/注册）
- ✅ 会话管理（CRUD）
- ✅ 历史会话查看
- ✅ 实时事件同步（SSE）
- ✅ 多端查看支持

待完善的功能：

- 🔲 JWT 认证
- 🔲 数据库持久化（服务端）
- 🔲 会话执行集成（与 CodingAgent 集成）
- 🔲 权限管理（Owner/Viewer 角色）
- 🔲 断线重连优化
- 🔲 性能优化（事件批处理、分页）

## 📞 问题反馈

如有问题，请检查：

1. mpp-server 是否正常运行
2. 端口 8080 是否被占用
3. 网络连接是否正常
4. 客户端日志和服务器日志

