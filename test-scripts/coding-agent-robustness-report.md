# CodingAgent 系统提示词健壮性测试报告

## 测试概述

本报告总结了对 `@mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/CodingAgentTemplate.kt` 系统提示词健壮性的测试结果。

## 测试环境

- **测试项目**: `/tmp/test-project` (Spring Boot Maven 项目)
- **CLI 路径**: `mpp-ui/dist/index.js`
- **LLM 提供商**: DeepSeek Chat
- **测试时间**: 2025-11-04

## 主要发现

### ✅ 成功的方面

1. **工具调用格式修复成功**
   - 修复了 `ToolCallParser.kt` 中 JSON 参数解析问题
   - Agent 现在能正确解析 `<devin>` 标签中的 JSON 格式参数
   - 系统提示词中的工具使用示例格式正确

2. **基本工具调用功能正常**
   - Agent 能够正确调用 `glob` 工具列出文件
   - Agent 能够正确调用 `read-file` 工具读取文件内容
   - Agent 能够正确调用 `write-file` 工具创建/修改文件
   - Agent 能够正确调用 `shell` 工具执行命令

3. **系统提示词结构良好**
   - 清晰的工具使用格式说明
   - 正确的 DevIns 格式示例
   - 合理的任务执行指导原则

### ⚠️ 需要改进的方面

1. **错误恢复机制**
   - ErrorRecoveryAgent 返回结果格式不一致
   - 需要改进错误处理和恢复逻辑

2. **MCP 工具集成**
   - MCP 服务器在 JS 环境中初始化失败
   - "stdio transport not fully supported in browser" 问题

3. **工具配置过滤**
   - 工具配置过滤逻辑可能过于严格
   - 某些有用的内置工具被意外过滤

## 测试用例结果

### 测试 1: 基础项目探索
- **任务**: "List the current directory contents to understand the project structure"
- **结果**: ✅ 成功
- **工具使用**: glob, read-file
- **表现**: Agent 正确使用 glob 工具列出文件，然后读取关键文件了解项目结构

### 测试 2: 创建 REST 端点
- **任务**: "Create a simple hello world REST endpoint"
- **结果**: ✅ 成功
- **工具使用**: glob, read-file, write-file, shell
- **表现**: Agent 探索项目结构，发现已有端点，尝试修复依赖问题

### 测试 3: 添加依赖
- **任务**: "Add Spring AI dependency to the project"
- **结果**: ⚠️ 部分成功
- **工具使用**: glob, read-file, write-file, shell
- **表现**: Agent 正确识别 Maven 项目，添加依赖，但遇到依赖解析问题

## 系统提示词分析

### 优点

1. **清晰的格式指导**
   ```
   /tool-name
   ```json
   {"parameter": "value", "optional_param": 123}
   ```
   ```
   
2. **合理的执行原则**
   - 总是先列出当前目录
   - 先获取上下文再进行更改
   - 增量更改和验证

3. **正确的约束**
   - 每次响应只执行一个工具
   - 明确的响应格式要求

### 改进建议

1. **增强错误处理指导**
   ```
   ## Error Handling Guidelines
   
   When a tool call fails:
   1. Analyze the error message carefully
   2. Try alternative approaches if available
   3. Use error-recovery tool for complex issues
   4. Provide clear feedback about what went wrong
   ```

2. **添加工具选择指导**
   ```
   ## Tool Selection Guidelines
   
   - Use `glob` for listing files and directories
   - Use `read-file` for examining file contents
   - Use `write-file` for creating or modifying files
   - Use `shell` for build commands and system operations
   - Use `grep` for searching within files
   ```

3. **改进 JSON 参数示例**
   - 为每个工具提供更多实际使用示例
   - 包含常见参数组合
   - 添加错误处理示例

## 技术修复

### 已修复的问题

1. **ToolCallParser JSON 支持**
   - 添加了 `parseToolCallWithJson` 方法
   - 支持解析 ```json 代码块中的参数
   - 正确处理 JSON 对象到 Map 的转换

### 待修复的问题

1. **ErrorRecoveryAgent 结果格式**
   - 需要统一返回格式
   - 改进错误分析和建议生成

2. **MCP 工具集成**
   - 需要改进 JS 环境中的 MCP 客户端
   - 考虑使用 HTTP 传输替代 stdio

## 总体评估

**健壮性评分**: 7.5/10

- **工具调用**: 9/10 (修复后工作良好)
- **错误处理**: 6/10 (需要改进)
- **任务完成**: 8/10 (大多数任务能完成)
- **用户体验**: 7/10 (输出清晰但有错误信息)

## 结论

CodingAgentTemplate 的系统提示词整体设计良好，在修复 JSON 参数解析问题后，Agent 能够正确调用工具并完成大多数开发任务。主要需要改进错误处理机制和 MCP 工具集成。

## 下一步行动

1. 修复 ErrorRecoveryAgent 返回格式问题
2. 改进 MCP 工具在 JS 环境中的集成
3. 添加更多工具使用示例到系统提示词
4. 创建更全面的测试套件
5. 优化工具配置过滤逻辑
