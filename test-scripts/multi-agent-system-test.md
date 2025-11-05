# 多Agent体系测试脚本

## 概述

这个测试脚本验证新的多Agent体系，包括：
1. ContentHandlerAgent 的长内容处理能力
2. SubAgent 间的通信机制
3. 自动长内容检测和委托
4. Agent 间的问答功能

## 测试场景

### 场景1：长内容自动处理

```bash
# 1. 构建项目
cd /Volumes/source/ai/autocrud
./gradlew :mpp-core:assembleJsPackage

# 2. 运行CLI并执行glob命令（会产生长内容）
cd mpp-ui
npm run build:ts
node dist/index.js

# 在CLI中执行：
glob pattern="*" 
```

**预期结果**：
- 系统检测到长内容（超过5000字符）
- 自动委托给ContentHandlerAgent处理
- 显示内容摘要而不是完整的文件列表
- 在输出中看到 "📊 Detected long content" 消息

### 场景2：向SubAgent提问

```bash
# 在CLI中执行长内容处理后，使用ask-subagent工具：
ask-subagent subAgentName="content-handler" question="What are the main file types found in this project?"

# 或者询问具体信息：
ask-subagent subAgentName="content-handler" question="How many files were found in total?"
```

**预期结果**：
- ContentHandlerAgent 基于之前处理的内容回答问题
- 提供具体的文件类型统计
- 显示处理过的内容摘要信息

### 场景3：错误恢复Agent对话

```bash
# 1. 先执行一个会失败的命令
shell command="nonexistent-command"

# 2. 向ErrorRecoveryAgent询问
ask-subagent subAgentName="error-recovery" question="What was the last error and how can I fix it?"
```

**预期结果**：
- ErrorRecoveryAgent 提供错误分析和修复建议
- 基于历史错误信息给出具体的解决方案

## 验证要点

### 1. 长内容检测
- [ ] 系统能自动检测超过阈值的内容
- [ ] 长内容被正确委托给ContentHandlerAgent
- [ ] 显示摘要而不是完整内容

### 2. SubAgent状态持久化
- [ ] ContentHandlerAgent保持处理历史
- [ ] 可以基于历史内容回答问题
- [ ] 状态在多次交互中保持一致

### 3. Agent间通信
- [ ] ask-subagent工具正常工作
- [ ] 能够向不同的SubAgent提问
- [ ] 获得相关和有用的回答

### 4. 系统集成
- [ ] 新功能不影响现有工具
- [ ] 所有SubAgent正确注册
- [ ] 工具配置正确生效

## 调试信息

在测试过程中，注意以下调试输出：

```
🔧 [CodingAgent] Initializing ToolRegistry with configService: true
🤖 Registered SubAgent: content-handler
🤖 Registered SubAgent: error-recovery
📊 Detected long content (9480 chars), delegating to ContentHandlerAgent
📊 ContentHandler: Processing text content from glob (9480 chars)
🔍 Content Handler Agent started
✅ Content analysis completed
💬 Asking SubAgent 'content-handler': What are the main file types found?
```

## 故障排除

### 问题1：ContentHandlerAgent未注册
**症状**：看不到长内容处理消息
**解决**：检查配置中是否启用了 "content-handler"

### 问题2：ask-subagent工具不可用
**症状**：工具未找到错误
**解决**：确保SubAgentManager正确传递给ToolRegistry

### 问题3：SubAgent无法回答问题
**症状**：返回"不支持问答"错误
**解决**：检查SubAgent是否实现了handleQuestion方法

## 性能测试

测试大量内容的处理性能：

```bash
# 生成大文件进行测试
shell command="find /usr -name '*.txt' | head -1000 > large_file_list.txt"
read-file path="large_file_list.txt"

# 验证内容处理时间和内存使用
```

## 预期改进

这个多Agent体系应该显著改善以下方面：
1. **用户体验**：长内容不再淹没界面
2. **交互性**：可以与处理过的内容进行对话
3. **智能化**：系统自动决定如何处理不同类型的内容
4. **可扩展性**：容易添加新的SubAgent处理特定任务
