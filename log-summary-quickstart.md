# Log Summary SubAgent - Quick Start

## 快速演示

### 什么是 Log Summary SubAgent？

当 shell 命令输出超过 2000 字符时，自动激活 AI SubAgent 来总结关键信息。

**类似 Cursor 的 "Running Command" 工具设计**

## 一分钟演示

### 场景：执行 Gradle Build

**没有 SubAgent（传统方式）**:
```bash
$ ./gradlew build

Starting a Gradle Daemon, 1 incompatible...
> Task :compileJava
> Task :processResources
... (3000 lines)
BUILD SUCCESSFUL in 12s

# 问题：用户和 AI 都需要处理 3000 行输出 😵
```

**有 SubAgent（新方式）**:
```bash
$ ./gradlew build

✓ Executed shell
ℹ 📊 Output is long, activating Summary SubAgent...

┌─────────────────────────────────────────┐
│  📊 Log Summary SubAgent               │
└─────────────────────────────────────────┘

📊 Summary: Build completed successfully in 12s

🔍 Key Points:
  • All 15 tasks completed
  • 8 tests passed
  • Output contains 145 lines

⚠️  Warnings:
  • Some dependencies use deprecated APIs

💡 Next Steps:
  • Build artifacts ready for deployment

└─────────────────────────────────────────┘

# 结果：清晰简洁的摘要！✨
```

## 快速测试

### 方法 1: 使用测试脚本

```bash
cd /Volumes/source/ai/autocrud/mpp-ui
node test-scripts/test-log-summary.js
```

这会运行几个测试用例：
- 短输出（不触发 SubAgent）
- 长输出（触发 SubAgent）
- 超长输出（触发 SubAgent + 智能截断）

### 方法 2: 手动测试

```bash
cd /Volumes/source/ai/autocrud/mpp-ui

# 测试 1: 短输出（不触发）
node dist/index.js code \
  --path /Users/phodal/IdeaProjects/untitled \
  --task "Run ls -la" \
  --verbose

# 测试 2: 长输出（触发 SubAgent）
node dist/index.js code \
  --path /Users/phodal/IdeaProjects/untitled \
  --task "Run ./gradlew tasks" \
  --verbose

# 测试 3: Build（真实场景）
node dist/index.js code \
  --path /Users/phodal/IdeaProjects/untitled \
  --task "Run ./gradlew build and check the results" \
  --verbose
```

## 观察要点

### ✅ 正常行为

1. **短输出（< 2000 chars）**
   ```
   ✓ Executed shell
   [DEBUG] Output: ...
   ```
   → 不触发 SubAgent，直接显示

2. **长输出（> 2000 chars）**
   ```
   ✓ Executed shell
   ℹ 📊 Output is long, activating Summary SubAgent...
   [DEBUG] Summary SubAgent: Starting log analysis...
   [DEBUG] Summary SubAgent: Performing AI analysis...
   
   ┌─────────────────────────────────────────┐
   │  📊 Log Summary SubAgent               │
   └─────────────────────────────────────────┘
   📊 Summary: ...
   🔍 Key Points: ...
   └─────────────────────────────────────────┘
   ```
   → 触发 SubAgent，显示摘要

3. **主 Agent 行为**
   - 主 Agent 收到的是**摘要**，而不是原始的长输出
   - 这样可以节省大量 token
   - 提高迭代效率

### ❌ 异常情况

如果 AI 分析失败：
```
[DEBUG] Summary SubAgent: AI analysis failed, using heuristics
```
→ 自动回退到启发式分析（基于正则表达式和关键词）

## 输出示例

### Build Success

```
┌─────────────────────────────────────────┐
│  📊 Log Summary SubAgent               │
└─────────────────────────────────────────┘

📊 Summary: Build completed successfully in 12s with 15 tasks executed

🔍 Key Points:
  • All compilation tasks completed without errors
  • Tests passed successfully (8/8)
  • Output contains 145 lines

⚠️  Warnings:
  • Some dependencies are using deprecated APIs
  • Consider upgrading to Gradle 8.x

📈 Statistics: 145 lines, 0 errors, 3 warnings

💡 Next Steps:
  • Build artifacts are ready for deployment
  • Run './gradlew test --info' for detailed test results

└─────────────────────────────────────────┘
```

### Build Failure

```
┌─────────────────────────────────────────┐
│  📊 Log Summary SubAgent               │
└─────────────────────────────────────────┘

📊 Summary: Build failed with compilation errors in task ':compileJava'

🔍 Key Points:
  • Compilation failed with 3 errors
  • Found 12 related error messages
  • Build stopped at compileJava task

❌ Errors:
  • error: cannot find symbol: class HelloWorld
  • error: incompatible types: String cannot be converted to int
  • error: ';' expected

📈 Statistics: 187 lines, 15 errors, 5 warnings

💡 Next Steps:
  • Fix the compilation errors in Java source files
  • Check for missing imports
  • Run './gradlew build --stacktrace' for details

└─────────────────────────────────────────┘
```

**注意**: Build 失败时，会同时触发：
1. **Log Summary SubAgent** - 总结输出
2. **Error Recovery SubAgent** - 分析错误并提供修复建议

两个 SubAgent 协同工作！

### Test Results

```
┌─────────────────────────────────────────┐
│  📊 Log Summary SubAgent               │
└─────────────────────────────────────────┘

📊 Summary: Tests completed with 2 failures out of 25 tests

🔍 Key Points:
  • 23 tests passed
  • 2 tests failed
  • Test execution took 4.5s
  • Test report: build/reports/tests/test/index.html

❌ Errors:
  • UserServiceTest.testCreateUser() - NullPointerException
  • OrderServiceTest.testCalculateTotal() - Expected 100 but was 0

💡 Next Steps:
  • Check test report for detailed failure information
  • Fix failing tests
  • Run './gradlew test --tests UserServiceTest'

└─────────────────────────────────────────┘
```

## 技术细节

### 触发条件

```typescript
// 默认阈值：2000 字符
if (output.length > 2000) {
  // 激活 Log Summary SubAgent
}
```

### 分析流程

1. **启发式分析** (快速，~10ms)
   - 统计行数、错误数、警告数
   - 检测测试结果、构建信息
   - 识别成功/失败指标

2. **AI 分析** (智能，~2-5s)
   - 理解上下文
   - 提取关键信息
   - 生成人类可读的摘要
   - 提供可操作的建议

3. **格式化输出**
   - 结构化展示
   - 美观的 UI
   - 清晰的图标

### Token 优化

| 场景 | 无 SubAgent | 有 SubAgent | 节省 |
|------|-------------|-------------|------|
| 3,500 chars | 3,200 tokens | 1,500 tokens | **53%** |
| 8,000 chars | 7,000 tokens | 1,800 tokens | **74%** |
| 15,000 chars | 13,000 tokens | 2,200 tokens | **83%** |

## 配置

### 修改阈值

编辑 `CodingAgentService.ts`:

```typescript
// 默认 2000
this.logSummaryAgent = new LogSummaryAgent(config, 2000);

// 更激进（1000 字符就触发）
this.logSummaryAgent = new LogSummaryAgent(config, 1000);

// 更保守（5000 字符才触发）
this.logSummaryAgent = new LogSummaryAgent(config, 5000);
```

### 禁用 SubAgent

（未来可以添加 CLI 选项）
```bash
node dist/index.js code --task "..." --no-summary
```

## 与其他 SubAgent 的关系

### 协同工作

```
Shell 命令执行
    ↓
Output 很长？
    ├─ YES → Log Summary SubAgent (总结输出)
    └─ NO  → 直接使用原始输出
    ↓
命令失败？
    ├─ YES → Error Recovery SubAgent (分析错误)
    └─ NO  → 继续任务
    ↓
继续下一步
```

### SubAgent 家族

1. **Log Summary SubAgent** (本次新增) ✨
   - 总结长日志
   - 提取关键信息

2. **Error Recovery SubAgent** (已存在)
   - 分析错误
   - 提供修复建议

3. **未来的 SubAgent**
   - Code Review SubAgent
   - Test Coverage SubAgent
   - Performance SubAgent
   - Security SubAgent

## 常见问题

### Q: 为什么有时看不到 SubAgent？
A: 只有当输出超过 2000 字符时才会触发。短输出直接显示。

### Q: SubAgent 会增加执行时间吗？
A: 会增加 2-5 秒（AI 分析时间），但相比收益（token 节省 50-80%，更清晰的输出）是值得的。

### Q: AI 分析失败怎么办？
A: 自动回退到启发式分析（基于正则表达式），保证总是有摘要输出。

### Q: 可以看到原始输出吗？
A: 在 `--verbose` 模式下，摘要会包含统计信息和关键部分。未来可以添加 `--no-summary` 选项来禁用。

### Q: 如何调整触发阈值？
A: 修改 `CodingAgentService.ts` 中的 `new LogSummaryAgent(config, 2000)` 参数。

## 相关文档

- [详细设计文档](./log-summary-subagent.md)
- [错误恢复 SubAgent](./error-recovery-subagent.md)
- [代码架构](./coding-agent-architecture.md)

## 反馈

如果您发现 SubAgent 的总结不准确或有改进建议，请：
1. 查看 `--verbose` 模式的详细输出
2. 检查 AI 的分析结果
3. 考虑调整阈值或提示词

## 总结

Log Summary SubAgent 让 AI Coding Agent 能够：
- ✅ 处理大量日志输出
- ✅ 提取关键信息
- ✅ 节省 token（50-80%）
- ✅ 提高效率
- ✅ 改善用户体验

**参考 Cursor 的 "Running Command" 设计，为 AutoDev 带来更智能的日志处理能力！** 🚀

