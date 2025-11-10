# 测试远程 Agent 切换功能

## 测试目的
验证 Compose UI 中本地和远程 Agent 的切换功能是否正常工作。

## 测试环境
- **mpp-server**: 需要先启动 mpp-server (默认端口 8080)
- **mpp-ui**: Compose Desktop 或 Android 应用

## 测试步骤

### 1. 启动 mpp-server
```bash
cd /Volumes/source/ai/autocrud
./gradlew :mpp-server:run
```

等待服务器启动,确认看到:
```
✅ Server started on http://localhost:8080
```

### 2. 构建并运行 mpp-ui (Desktop)
```bash
cd /Volumes/source/ai/autocrud
./gradlew :mpp-ui:run
```

或者运行 CLI 版本:
```bash
cd mpp-ui
npm run build
npm run start
```

### 3. 测试 Local Agent (默认)
1. 启动应用后,默认应该是 **Local** Agent 模式
2. 检查 TopBar 右上角是否显示 **Agent Type** 按钮/下拉菜单
3. 点击后应该显示:
   - 🖥️ Local (默认选中,有 ✓ 标记)
   - ☁️ Remote
4. 输入一个简单任务,确认本地 Agent 正常工作

### 4. 切换到 Remote Agent
1. 点击 TopBar 的 **Agent Type** 选择器
2. 选择 **☁️ Remote**
3. 应该看到以下变化:
   - UI 切换到 RemoteAgentChatInterface
   - 如果服务器未连接,会显示连接错误对话框
4. 点击 **Configure Server** (在 Remote 选项下方)
5. 在 RemoteServerConfigDialog 中:
   - 确认 Server URL 为 `http://localhost:8080`
   - 选择是否使用服务器配置 (Use Server Config)
   - 点击 Save

### 5. 测试 Remote Agent 连接
1. 保存配置后,应该自动连接到服务器
2. 如果连接成功:
   - 应该能看到 Project 列表 (如果服务器有项目)
   - 输入框可用
3. 如果连接失败:
   - 显示错误信息
   - 提供 "Retry Connection" 按钮
   - 提供 "Configure" 按钮重新配置

### 6. 测试 Remote Agent 任务执行
1. 在 Remote 模式下输入任务,例如:
   ```
   Create a simple Hello World function in Python
   ```
2. 观察:
   - 消息发送到远程服务器
   - 服务器响应通过 SSE 流式返回
   - UI 实时显示进度和结果
   - Terminal、Tree View 等组件正常工作

### 7. 切换回 Local Agent
1. 再次点击 **Agent Type** 选择器
2. 选择 **🖥️ Local**
3. UI 应该切换回 AgentChatInterface
4. 输入任务确认本地 Agent 正常

### 8. 测试配置持久化
1. 在 Remote 模式下配置服务器 URL
2. 重启应用
3. 切换到 Remote 模式
4. 确认配置是否保存 (TODO: 当前版本可能不持久化)

## 预期结果

### ✅ 成功标准
- [ ] TopBar 显示 Agent Type 选择器 (Desktop 和 Mobile)
- [ ] 可以在 Local/Remote 之间切换
- [ ] Local 模式使用 AgentChatInterface
- [ ] Remote 模式使用 RemoteAgentChatInterface
- [ ] Configure Server 按钮打开配置对话框
- [ ] 配置对话框可以保存 Server URL 和选项
- [ ] Remote 模式可以连接到 mpp-server
- [ ] Remote 模式可以执行任务并显示结果
- [ ] 切换不会导致崩溃或错误

### ❌ 已知问题
- 配置可能不持久化 (需要在每次启动后重新配置)
- 首次连接可能需要手动点击 Configure Server

## 测试数据

### 测试任务示例
```
# Local Agent 测试
1. "列出当前目录的所有 Kotlin 文件"
2. "解释 AutoDevApp.kt 的主要功能"

# Remote Agent 测试
1. "Create a simple REST API endpoint in Kotlin"
2. "Add error handling to the existing code"
```

### 服务器健康检查
```bash
curl http://localhost:8080/health
# 应该返回 200 OK
```

## 故障排查

### 无法连接到 Remote Server
1. 确认 mpp-server 正在运行: `ps aux | grep mpp-server`
2. 检查端口: `lsof -i :8080`
3. 测试连接: `curl http://localhost:8080/health`
4. 查看服务器日志: `~/.autodev/logs/autodev-app.log`

### Agent Type 选择器不显示
1. 确保在 Agent 模式 (不是 Chat 模式)
2. 检查 TopBar 组件是否正确渲染
3. 查看控制台日志

### 切换后崩溃
1. 查看错误日志
2. 确认 RemoteAgentChatInterface 参数传递正确
3. 检查 serverUrl 是否有效

## 下一步测试
- [ ] 测试多个项目的切换
- [ ] 测试 useServerConfig 标志的影响
- [ ] 测试网络断开时的错误处理
- [ ] 性能测试: Remote vs Local 响应时间
