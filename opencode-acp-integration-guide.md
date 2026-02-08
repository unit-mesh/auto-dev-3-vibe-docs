# OpenCode ACP 集成使用指南

## 概述

OpenCode 是一个开源的 AI 编码助手，通过 Agent Client Protocol (ACP) 协议与 IntelliJ IDEA 插件集成。本指南介绍如何在 Xiuper (AutoDev) 插件中使用 OpenCode。

## 前提条件

### 1. 安装 OpenCode

使用官方安装脚本：

```bash
curl -fsSL https://opencode.ai/install | bash
```

或者使用包管理器：

```bash
# macOS/Linux (Homebrew)
brew install anomalyco/tap/opencode

# macOS/Linux (Arch)
paru -S opencode-bin

# Windows (Chocolatey)
choco install opencode
```

### 2. 验证安装

```bash
opencode --version
# 应该输出: 1.1.53 (或更高版本)

which opencode
# 应该输出: /Users/你的用户名/.opencode/bin/opencode
```

## 配置

### 方法 1: 自动检测（推荐）

如果 OpenCode 在 PATH 中，插件会自动检测并添加到可用代理列表。

1. 打开 IntelliJ IDEA
2. 打开 AutoDev 工具窗口
3. 切换到 **ACP** 标签
4. 在代理下拉列表中选择 **OpenCode**
5. 点击 **Connect** 按钮

### 方法 2: 手动配置

编辑 `~/.autodev/config.yaml`：

```yaml
acpAgents:
  "opencode":
    name: "OpenCode"
    command: "/完整路径/到/opencode"  # 使用 which opencode 获取
    args: "acp"
    env: ""

activeAcpAgent: "opencode"  # 设置为默认代理
```

## 使用方法

### 1. 连接到 OpenCode

在 ACP 面板中：

1. 从下拉列表选择 "OpenCode"
2. 点击 "Connect" 或直接发送消息
3. 等待连接成功（通常 2-5 秒）

### 2. 发送提示

连接成功后，在输入框中输入您的问题或任务：

```
示例提示：
- "解释这个文件的功能"
- "重构这个函数以提高可读性"
- "为这个类添加单元测试"
- "找出这段代码中的潜在 bug"
```

### 3. 查看响应

OpenCode 会：
- 分析项目结构和代码
- 提供详细的解释或建议
- 执行文件读取、编辑等操作（需要权限确认）
- 显示思考过程和工具调用

### 4. 权限管理

当 OpenCode 需要执行操作时（如读取文件、运行命令），会弹出权限对话框：

- **Allow Once**: 仅允许此次操作
- **Allow Always**: 始终允许此类操作
- **Reject Once**: 拒绝此次操作
- **Reject Always**: 始终拒绝此类操作

建议对于读取文件选择 "Allow Always"，对于写入操作谨慎选择。

## 高级功能

### 1. MCP 服务器集成

OpenCode 支持 Model Context Protocol (MCP) 服务器，可以扩展其能力。

在 `config.yaml` 中配置 MCP 服务器：

```yaml
mcpServers:
  filesystem:
    command: "npx"
    args: "@modelcontextprotocol/server-filesystem"
    env:
      ALLOWED_DIRECTORIES: "/path/to/project"
```

### 2. 工作空间索引

首次连接时，OpenCode 会索引项目结构（可能需要权限）：
- 文件树分析
- 代码结构识别
- 依赖关系映射

### 3. 多轮对话

OpenCode 支持上下文感知的多轮对话：
- 记住之前的交互
- 引用先前的代码更改
- 逐步完成复杂任务

### 4. 代码编辑

OpenCode 可以直接编辑代码文件：
- 自动生成差异（diff）
- 支持撤销/重做
- 实时预览更改

## 故障排除

### 问题 1: "ACP agent is not connected"

**解决方案:**
```bash
# 检查 OpenCode 是否在 PATH 中
which opencode

# 检查版本
opencode --version

# 测试 ACP 协议
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":1}}' | opencode acp
```

### 问题 2: 连接超时

**可能原因:**
- OpenCode 进程启动失败
- 防火墙阻止本地通信
- PATH 配置不正确

**解决方案:**
1. 查看 stderr 日志（在 ACP 面板底部）
2. 检查 `~/.autodev/logs/autodev-app.log`
3. 尝试在终端手动运行: `opencode acp`

### 问题 3: "No output from agent"

**解决方案:**
1. 检查 OpenCode 是否需要认证:
   ```bash
   opencode auth login
   ```
2. 验证 API 密钥配置
3. 查看 stderr 输出中的错误信息

### 问题 4: 权限请求无响应

**解决方案:**
- 确保 IntelliJ IDEA 没有运行在无头模式
- 重启 IDE 和连接
- 检查系统通知权限

## 日志和调试

### 查看日志

```bash
# AutoDev 插件日志
tail -f ~/.autodev/logs/autodev-app.log

# ACP 特定日志
ls -lt ~/.autodev/acp-logs/
```

### 启用详细日志

在 IDEA 中:
1. Help → Diagnostic Tools → Debug Log Settings
2. 添加: `#cc.unitmesh.devins.idea.toolwindow.acp`
3. 重启连接

## 性能优化

### 1. 预热连接

OpenCode 支持进程复用。首次连接后，进程会保持活动状态，后续连接更快。

### 2. 限制工作空间大小

对于大型项目，可以在 `.gitignore` 或 `.autodevignore` 中排除不必要的目录：

```
node_modules/
build/
.idea/
*.log
```

### 3. 调整超时设置

在代码中（需要重新编译插件）：

```kotlin
// IdeaAcpAgentViewModel.kt
val timeout = 60000L  // 60 秒超时
```

## 与其他 ACP 代理对比

| 特性 | OpenCode | Codex | Kimi | Gemini |
|------|----------|-------|------|--------|
| 开源 | ✅ | ❌ | ❌ | ❌ |
| 本地运行 | ❌ | ❌ | ❌ | ❌ |
| 免费层 | ✅ | ❌ | ✅ | ✅ |
| MCP 支持 | ✅ | ✅ | ❌ | ❌ |
| 中文支持 | ✅ | ✅ | ✅ | ✅ |
| 代码编辑 | ✅ | ✅ | ✅ | ✅ |

## 最佳实践

1. **明确的提示**: 提供具体的上下文和期望结果
2. **迭代式交互**: 从简单任务开始，逐步添加复杂性
3. **代码审查**: 始终审查 AI 生成的代码，不要盲目应用
4. **版本控制**: 使用 Git 跟踪 AI 做的更改，便于回滚
5. **权限谨慎**: 对写入操作使用 "Allow Once" 而不是 "Allow Always"

## 相关链接

- [OpenCode 官方文档](https://opencode.ai/docs/)
- [OpenCode ACP 支持](https://opencode.ai/docs/acp/)
- [Agent Client Protocol 规范](https://github.com/agent-client-protocol/spec)
- [AutoDev 插件文档](../README.md)

## 获取帮助

- GitHub Issues: [xiuper/issues](https://github.com/phodal/xiuper/issues)
- OpenCode Discord: [链接见官网]
- 邮件支持: [支持邮箱]

---

**最后更新:** 2026-02-08  
**适用版本:** OpenCode 1.1.53+, Xiuper 最新版
