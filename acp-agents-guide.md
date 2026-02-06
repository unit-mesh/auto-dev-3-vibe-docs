# ACP Agents 使用指南

本项目支持多个 ACP (Agent Client Protocol) agents，包括 Gemini、Kimi 和 GitHub Copilot CLI。

## 支持的 ACP Agents

| Agent | 命令 | 参数 | 状态 |
|-------|------|------|------|
| Kimi | `kimi` | `acp` | ✅ 已验证 |
| Gemini | `gemini` | `--experimental-acp` | ✅ 已验证 |
| **Copilot** | `copilot` | `--acp` | ✅ 新增 |
| Claude | `claude` | `--acp` | ⚠️ 未测试 |
| Codex | `codex` | `--acp` | ⚠️ 未测试 |

## 安装 GitHub Copilot CLI

### 使用 Homebrew (macOS/Linux)
```bash
brew install copilot-cli
```

### 使用 npm
```bash
npm install -g @github/copilot
```

### 使用 WinGet (Windows)
```bash
winget install GitHub.Copilot
```

### 首次使用

首次运行需要登录：
```bash
copilot
# 然后在提示中输入 /login 并按照指示操作
```

## 配置

### 自动检测

应用会自动检测已安装的 ACP agents。在 UI 中选择 ACP agent 时，会显示所有可用的选项。

### 手动配置

编辑 `~/.autodev/config.yaml`：

```yaml
acpAgents:
  copilot:
    name: Copilot
    command: /opt/homebrew/bin/copilot  # 或你的安装路径
    args: --acp
    env: ''  # 可选环境变量，格式：KEY=VALUE
  gemini:
    name: Gemini
    command: /opt/homebrew/bin/gemini
    args: --experimental-acp
    env: ''
  kimi:
    name: Kimi
    command: kimi
    args: acp
    env: 'KIMI_API_KEY=your_api_key_here'
```

## 使用方法

### 在应用中使用

1. 启动应用
2. 在设置中选择 ACP Agent（Copilot / Gemini / Kimi）
3. 开始对话

### 命令行调试

```bash
# 会话测试
./gradlew :mpp-ui:runAcpDebug --args="--agent=copilot --test=session"

# 通配符测试
./gradlew :mpp-ui:runAcpDebug --args="--agent=copilot --test=wildcard"

# Bash 命令测试
./gradlew :mpp-ui:runAcpDebug --args="--agent=copilot --test=bash"
```

### 使用测试脚本

```bash
# Copilot 简单测试
./docs/test-scripts/test-copilot-simple.sh

# Copilot 完整测试
./docs/test-scripts/test-copilot-acp.sh
```

## Agent 特性对比

### Copilot
- ✅ GitHub 原生集成（repos、issues、PRs）
- ✅ 内置 GitHub MCP Server
- ✅ 强大的代码理解和生成能力
- ✅ 支持多种编程语言
- ⚠️ 需要 GitHub Copilot 订阅

### Gemini
- ✅ 多模态支持
- ✅ 强大的推理能力
- ✅ 需要配置 MCP servers（用于 glob/search 等工具）
- ⚠️ 实验性 ACP 支持

### Kimi
- ✅ 支持 `--work-dir` 参数
- ✅ 适合需要频繁文件操作的场景
- ✅ Shell 操作在独立子进程中运行
- ⚠️ 需要 API Key

## 日志和调试

### ACP 日志

所有 ACP 通信都记录在 `~/.autodev/acp-logs/` 目录下：

```bash
# 查看最新日志
ls -lt ~/.autodev/acp-logs/ | head

# 实时查看 Copilot 日志
tail -f ~/.autodev/acp-logs/Copilot_*.jsonl

# 查看特定事件类型
cat ~/.autodev/acp-logs/Copilot_*.jsonl | jq 'select(.update_type == "ToolCallUpdate")'
```

### 应用日志

应用日志位于 `~/.autodev/logs/autodev-app.log`：

```bash
tail -f ~/.autodev/logs/autodev-app.log
```

## 常见问题

### Copilot 提示未登录

运行 `copilot` 命令并按照提示登录：
```bash
copilot
# 输入 /login
```

### Agent 未被检测到

确保命令在 PATH 中：
```bash
which copilot
which gemini
which kimi
```

或在配置文件中使用完整路径。

### 连接超时

检查网络连接和代理设置。某些 agents 可能需要特定的网络配置。

### 工作目录问题

- **Kimi**: 自动注入 `--work-dir` 参数
- **Copilot/Gemini**: 通过 `PWD` 和 `AUTODEV_WORKSPACE` 环境变量

## 更多信息

- [Copilot 集成文档](./copilot-acp-integration.md)
- [ACP 调试指南](./test-scripts/acp-debugging-guide.md)
- [GitHub Copilot CLI 官方文档](https://github.com/github/copilot-cli)

## 贡献

欢迎为其他 ACP agents 添加支持和测试！
