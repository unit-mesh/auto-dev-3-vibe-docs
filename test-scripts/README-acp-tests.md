# ACP Integration Tests

## 测试脚本列表

### 1. **test-acp-client-loopback.js** ⭐ 主要测试
测试 Xiuper JS Client 连接到 Xiuper JS Agent（loopback）

**用途：** 
- 验证 ACP client 和 agent 双向通信
- 测试完整的 initialize → newSession → prompt 流程
- 确保 session updates 正确接收

**运行：**
```bash
node docs/test-scripts/test-acp-client-loopback.js
```

**预期输出：**
```
✅ ACP Client → Agent test PASSED!
  Client connected: ✅
  Session created: ✅
  Prompt sent: ✅
  Received text: ✅
  Received thought: ✅
```

---

### 2. **test-acp-jvm.sh** ⭐ JVM 测试
运行 JVM 端的 ACP 单元测试

**用途：**
- 验证 Kotlin JVM ACP 实现
- 测试 `AcpClient`, `AcpRenderer`, `AcpAgentServer`
- 确保跨平台（JVM + JS）支持

**运行：**
```bash
./docs/test-scripts/test-acp-jvm.sh
```

**预期输出：**
```
✅ JVM ACP tests passed!
BUILD SUCCESSFUL
```

---

### 3. **test-acp-client-kimi.js** ⭐ 外部兼容性测试
测试 Xiuper Client 连接到 Kimi CLI Agent

**用途：**
- 验证与外部 ACP agent 的兼容性
- 证明符合 ACP 标准协议
- 测试与 Moonshot Kimi CLI 的互操作性

**前置条件：**
```bash
# 安装 Kimi CLI（需要 Python 3.12+）
python3.12 -m pip install kimi-cli

# 登录（可选，某些功能需要）
kimi login
```

**运行：**
```bash
node docs/test-scripts/test-acp-client-kimi.js
```

**预期输出：**
```
✅ ACP Client → Kimi CLI test PASSED!
  Connected to Kimi CLI: ✅
  Session created: ✅
  Prompt sent: ✅
```

---

## 快速验证

运行所有测试：
```bash
# 1. JS 端测试
node docs/test-scripts/test-acp-client-loopback.js

# 2. JVM 端测试
./docs/test-scripts/test-acp-jvm.sh

# 3. 外部兼容性（可选）
node docs/test-scripts/test-acp-client-kimi.js
```

---

## ACP 协议参考

### 关键方法名
- `initialize` - 初始化连接
- `session/new` - 创建新会话（注意 `session/` 前缀）
- `session/prompt` - 发送提示
- `session/update` - 会话更新通知（agent → client）

### 重要参数
- **protocolVersion**: `1` (整数)
- **mcpServers**: `[]` (数组，可为空)
- **prompt**: `[{ type: 'text', text: '...' }]`

---

## 故障排查

### 问题：Kimi CLI 连接失败
```
❌ Test FAILED: spawn kimi ENOENT
```

**解决：** 确保 Kimi CLI 已安装并在 PATH 中
```bash
which kimi
kimi --version
```

### 问题：认证错误
```
❌ Test FAILED: Authentication required
```

**解决：** 运行 `kimi login` 登录

### 问题：JS 测试失败
```
Cannot find module 'AcpClientConnection'
```

**解决：** 先构建 JS CLI
```bash
cd mpp-ui && npm run build
```

---

## 更多信息

完整测试报告：[docs/acp-testing-summary.md](./acp-testing-summary.md)
