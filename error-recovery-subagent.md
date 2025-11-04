# Error Recovery SubAgent

## 概述

Error Recovery SubAgent 是一个智能的错误恢复机制，当主 Agent 的命令执行失败时自动激活。它通过分析错误上下文（包括 git diff）并调用 LLM 来诊断和修复问题。

## 设计理念

**核心思想**：让 AI 自己分析和修复错误，而不是硬编码恢复规则。

### 为什么需要 SubAgent？

1. **动态判断**: 不同的错误需要不同的修复策略，硬编码规则无法覆盖所有情况
2. **上下文感知**: AI 可以结合 git diff 和错误信息来理解"为什么会出错"
3. **自我修复**: Agent 能够从错误中学习并自动恢复

## 工作流程

```
主 Agent 执行命令
    ↓
  命令失败?
    ↓ Yes
┌─────────────────────────────┐
│ Error Recovery SubAgent     │
├─────────────────────────────┤
│ 1. 检查文件修改 (git diff)  │
│ 2. 收集错误上下文            │
│ 3. 调用 LLM 分析错误         │
│ 4. 生成恢复计划              │
└─────────────────────────────┘
    ↓
将恢复计划注入下一次迭代
    ↓
主 Agent 执行恢复命令
    ↓
继续原任务
```

## 关键特性

### 1. Git Diff 检测

**为什么重要？**

当命令失败时，SubAgent 首先检查是否有文件被修改。这对于诊断至关重要：

```bash
# SubAgent 自动执行
$ git diff --name-only
build.gradle.kts

# 如果有修改，获取详细 diff
$ git diff -- build.gradle.kts
```

**两种情况**:

#### 情况 A: 有 diff（文件被修改）
```
⚠️ Files Modified Before Error

### build.gradle.kts
```diff
 plugins {
-    java
-    id("org.springframework.boot") version "2.7.10"
+    "plugins
```

**SubAgent 会发现**: "哦！build.gradle.kts 被破坏了，第一行的引号丢失了"

#### 情况 B: 无 diff（文件未修改）
```
✓ No files modified
```

**SubAgent 会发现**: "文件没有被修改，可能是环境问题或命令本身的问题"

### 2. 智能错误分析

SubAgent 发送给 LLM 的 context 包括：

```markdown
# Error Recovery Context

## Failed Command
```bash
./gradlew build
```

**Exit Code:** 1

## Error Message
```
Build file 'build.gradle.kts' line: 1
Expecting '"'
```

## ⚠️ Files Modified Before Error

### build.gradle.kts
```diff
-plugins {
+"plugins
     java
```

(No diff available)
```

### 3. LLM 恢复方案

LLM 返回结构化的恢复方案：

```json
{
  "analysis": "build.gradle.kts was corrupted - missing opening quote",
  "rootCause": "Previous write-file operation truncated the content",
  "suggestedActions": [
    "Restore build.gradle.kts from git",
    "Verify the file content is correct",
    "Retry the build command"
  ],
  "recoveryCommands": [
    "git checkout build.gradle.kts",
    "./gradlew build"
  ],
  "shouldRetry": true,
  "shouldAbort": false
}
```

### 4. 自动集成到下一次迭代

恢复方案自动注入到主 Agent 的下一次迭代：

```
## Previous Action Failed - Recovery Needed

build.gradle.kts was corrupted - missing opening quote

**Suggested Actions:**
1. Restore build.gradle.kts from git
2. Verify the file content is correct
3. Retry the build command

**Recovery Commands:**
`git checkout build.gradle.kts`
`./gradlew build`

Please execute these recovery commands first, then continue with the original task.

**Original Task:** Create a hello world

**What to do next:**
1. Execute the recovery commands to fix the error
2. Verify the fix worked
3. Continue with the original task
```

## 代码架构

### ErrorRecoveryAgent.ts

```typescript
export class ErrorRecoveryAgent {
  private projectPath: string;
  private llmService: LLMService;

  // 1. 主入口
  async analyzeAndRecover(errorContext: ErrorContext): Promise<RecoveryResult>
  
  // 2. 检查修改
  private async getModifiedFiles(): Promise<string[]>
  private async getFileDiffs(files: string[]): Promise<Map<string, string>>
  
  // 3. 构建上下文
  private buildErrorContext(...): string
  
  // 4. 调用 LLM
  private async askLLMForFix(context: string): Promise<RecoveryResult>
  
  // 5. 解析响应
  private parseRecoveryResponse(response: string): RecoveryResult
  
  // 6. 执行恢复（可选）
  async executeRecovery(recoveryCommands: string[]): Promise<boolean>
}
```

### CodingAgentService.ts 集成

```typescript
export class CodingAgentService {
  private errorRecoveryAgent: ErrorRecoveryAgent;
  private lastRecoveryResult: RecoveryResult | null = null;

  // 在 shell 失败时激活
  case 'shell':
    result = await this.toolRegistry.shell(...);
    
    if (!result.success && result.errorMessage) {
      // 🔧 激活 SubAgent
      const recoveryResult = await this.errorRecoveryAgent.analyzeAndRecover({
        command: params.command,
        errorMessage: result.errorMessage,
        stdout: result.output,
        stderr: result.errorMessage
      });
      
      // 保存恢复方案
      if (recoveryResult.success && !recoveryResult.shouldAbort) {
        this.lastRecoveryResult = recoveryResult;
      }
    }
    break;

  // 在下一次迭代时注入
  private async getNextAction(...) {
    if (this.lastRecoveryResult) {
      userPrompt = `## Previous Action Failed - Recovery Needed
      
${this.lastRecoveryResult.analysis}
...`;
    }
  }
}
```

## 使用示例

### 场景：Build 文件被破坏

```bash
# 1. 用户运行 Agent
$ node dist/index.js code --path ./project --task "Add a new controller"

# 2. Agent 错误地修改了 build.gradle.kts
[2/10] Analyzing and executing...
✓ Executed write-file

# 3. Agent 尝试构建
[3/10] Analyzing and executing...
[DEBUG] Executing: /shell command="./gradlew build"

# 4. 构建失败
✗ Failed shell: Build file 'build.gradle.kts' line: 1: Expecting '"'

# 5. Error Recovery SubAgent 激活 🔧
⚠️ Shell command failed, activating Error Recovery SubAgent...

🔧 Error Recovery Agent activated
   Command: ./gradlew build
   Error: Build file 'build.gradle.kts' line: 1: Expecting '"'...

   📝 Found 1 modified file(s)
      - build.gradle.kts
   📄 Got diff for build.gradle.kts (245 chars)

# 6. SubAgent 调用 LLM 分析
   (LLM analyzing error + diff...)

📋 Recovery Analysis:
   build.gradle.kts was corrupted during write operation

💡 Suggested Actions:
   1. Restore build.gradle.kts from git
   2. Verify the restored file is valid
   3. Retry the build

🔧 Recovery Commands:
   $ git checkout build.gradle.kts
   $ ./gradlew build

# 7. 下一次迭代自动执行恢复
[4/10] Analyzing and executing...
🔧 Applying recovery plan from SubAgent

[DEBUG] Executing: /shell command="git checkout build.gradle.kts"
✓ Executed shell

[DEBUG] Executing: /shell command="./gradlew build"
✓ Executed shell

# 8. 继续原任务
[5/10] Analyzing and executing...
✓ Executed write-file (Controller created)
```

## 对比：硬编码 vs SubAgent

### ❌ 硬编码方式（旧）

```typescript
// 固定的错误模式匹配
const errorPatterns = [
  { pattern: /Build file.*Expecting/, action: 'restore_from_git' },
  { pattern: /No such file/, action: 'create_file' }
];

// 问题：
// 1. 无法处理新类型的错误
// 2. 不理解上下文
// 3. 可能误判
```

### ✅ SubAgent 方式（新）

```typescript
// 1. 收集完整上下文（error + diff）
const context = buildErrorContext(error, diff);

// 2. 让 AI 分析
const recovery = await llm.analyze(context);

// 3. AI 理解并生成针对性的修复方案
// 优势：
// ✓ 可以处理任何类型的错误
// ✓ 理解为什么会出错
// ✓ 生成准确的修复步骤
```

## 实现细节

### 1. Git Diff 检测

```typescript
// 检查哪些文件被修改
const { stdout } = await execAsync('git diff --name-only', {
  cwd: this.projectPath
});

// 对每个修改的文件获取 diff
for (const file of modifiedFiles) {
  const { stdout } = await execAsync(`git diff -- "${file}"`, {
    cwd: this.projectPath
  });
  diffs.set(file, stdout);
}
```

### 2. Context 构建

```typescript
const context = `
# Error Recovery Context

## Failed Command
\`\`\`bash
${command}
\`\`\`

## Error Message
\`\`\`
${errorMessage}
\`\`\`

## ⚠️ Files Modified Before Error

### ${file}
\`\`\`diff
${diff}
\`\`\`
`;
```

### 3. LLM System Prompt

```typescript
const systemPrompt = `You are an Error Recovery Agent. Your job is to:
1. Analyze why a command failed
2. Identify the root cause (especially if files were corrupted)
3. Suggest specific fixes

Focus on:
- Build file corruption (build.gradle.kts, pom.xml, package.json, etc.)
- Syntax errors introduced by recent changes
- File permission or path issues

Respond in JSON format with:
- analysis: Brief explanation
- rootCause: Specific cause
- suggestedActions: List of actions
- recoveryCommands: Shell commands to execute
- shouldRetry: Whether to retry after fix
`;
```

### 4. JSON 解析

```typescript
// 从 LLM 响应提取 JSON
const jsonMatch = response.match(/```json\s*([\s\S]*?)\s*```/) || 
                 response.match(/\{[\s\S]*\}/);

if (jsonMatch) {
  const parsed = JSON.parse(jsonStr);
  return {
    analysis: parsed.analysis,
    suggestedActions: parsed.suggestedActions,
    recoveryCommands: parsed.recoveryCommands,
    shouldRetry: parsed.shouldRetry,
    shouldAbort: parsed.shouldAbort
  };
}
```

## 优势

### 1. 智能化
- AI 理解错误的**上下文**和**原因**
- 可以处理**未知的错误类型**
- 生成**针对性的**修复方案

### 2. 自动化
- 无需人工干预
- 自动检测文件修改
- 自动集成到工作流

### 3. 透明性
- 显示完整的分析过程
- 展示建议的恢复步骤
- 用户可以看到 SubAgent 的决策

### 4. 可扩展
- 容易添加新的错误类型
- 可以支持更复杂的恢复策略
- 可以集成更多工具（如 linter、formatter）

## 未来改进

### 1. 自动执行恢复
目前：SubAgent 生成方案 → 主 Agent 执行
未来：SubAgent 可以直接执行（带确认）

### 2. 学习机制
- 记录成功的恢复案例
- 构建恢复知识库
- 优先尝试已知有效的方案

### 3. 预防机制
- 在执行危险操作前备份
- 检测潜在的破坏性修改
- 提前警告可能的问题

### 4. 多种恢复策略
- Git 恢复（当前支持）
- Undo 机制
- 手动修复建议
- 回滚到安全点

## 测试

```bash
# 1. 构建
cd /Volumes/source/ai/autocrud/mpp-ui
npm run build:ts

# 2. 故意破坏 build.gradle.kts
cd /Users/phodal/IdeaProjects/untitled
echo '"plugins' > build.gradle.kts

# 3. 运行 Agent
cd /Volumes/source/ai/autocrud/mpp-ui
node dist/index.js code \
  --path /Users/phodal/IdeaProjects/untitled \
  --task "Run ./gradlew build"

# 4. 观察 SubAgent 激活并修复
# 应该看到：
# - Error Recovery Agent activated
# - Found 1 modified file(s): build.gradle.kts
# - Got diff
# - Recovery Analysis
# - Suggested fix: git checkout build.gradle.kts
```

## 总结

Error Recovery SubAgent 是一个**智能的自我修复机制**：

1. ✅ **自动检测**: 使用 git diff 发现问题
2. ✅ **智能分析**: AI 理解错误原因
3. ✅ **生成方案**: 针对性的修复步骤
4. ✅ **自动集成**: 无缝融入工作流

**核心价值**: 让 Agent 能够从错误中恢复，就像人类开发者一样 - "哦，我搞坏了这个文件，让我恢复一下"。


