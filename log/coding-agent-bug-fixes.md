# Coding Agent 核心 Bug 修复

## 问题总结

用户报告了3个严重的功能性问题：

1. **write-file 内容截断** - 文件内容被破坏：`println(\%` 而不是 `println("Hello, World!")`
2. **shell 工具执行失败** - gradlew 明明存在但报告找不到
3. **项目类型识别错误** - Java 项目被识别为 Kotlin 项目

## 根本原因分析

### 问题 1: write-file 参数解析 Bug

**根本原因**: 正则表达式不支持匹配换行符

```typescript
// ❌ 错误的实现 (line 476)
const paramRegex = /(\w+)=(?:"([^"]*)"|'([^']*)'|(\S+))/g;
```

**问题**:
- `[^"]*` 匹配除双引号外的任何字符，但**不包括换行符**
- 当参数值包含 `\n` 时，正则在第一个换行符处停止
- 导致 content 被截断

**示例**:
```typescript
content="fun main() {\n    println(\"Hello\")\n}"
//      ^^^^^^^^^^^ 只能匹配到这里，后面的内容被丢弃
```

**修复方案**:
```typescript
// ✅ 正确的实现
const paramRegex = /(\w+)=(?:"((?:[^"\\]|\\.)*)"|'((?:[^'\\]|\\.)*)'|(\S+))/g;
//                          ^^^^^^^^^^^^^^^^  支持转义字符和换行符
```

使用 `(?:[^"\\]|\\.)*` 来：
- `[^"\\]` - 匹配非引号非反斜杠的字符
- `\\.` - 匹配转义序列（如 `\n`, `\"`, `\\`）
- 正确处理包含换行符的内容

**修改文件**: `mpp-ui/src/jsMain/typescript/services/CodingAgentService.ts:476`

---

### 问题 2: shell 工具 workingDirectory 缺失

**根本原因**: shell 命令没有默认工作目录

**问题**:
- 当 LLM 生成 `/shell command="./gradlew build"` 时
- 没有指定 `workingDirectory` 参数
- 命令在默认目录（非项目目录）执行
- 导致 `./gradlew: No such file or directory`

**修复方案**:
```typescript
// Before
case 'shell':
  result = await this.toolRegistry.shell(
    params.command,
    params.workingDirectory,  // 可能是 undefined
    Number(params.timeoutMs) || 30000
  );

// After
case 'shell':
  result = await this.toolRegistry.shell(
    params.command,
    params.workingDirectory || this.projectPath,  // ✅ 默认使用项目路径
    Number(params.timeoutMs) || 30000
  );
```

**修改文件**: `mpp-ui/src/jsMain/typescript/services/CodingAgentService.ts:550`

---

### 问题 3: 项目语言识别缺失

**根本原因**: Agent 只检测了 build tool，没有检测项目语言

**问题**:
- 只知道项目用 Gradle
- 不知道是 Java 项目还是 Kotlin 项目
- LLM 可能随机选择语言

**修复方案**: 添加语言检测功能

```typescript
/**
 * Detect primary language by counting source files
 */
private async detectLanguage(): Promise<string> {
  // 1. 扫描 src 目录
  // 2. 统计各语言文件数量
  // 3. 返回最多的语言

  const counts = {
    '.java': 0,
    '.kt': 0,
    '.ts': 0,
    // ... 其他语言
  };

  // 递归统计
  await countFiles(srcPath);

  // 返回: "Java (26 files)" 或 "Kotlin (5 files)"
}
```

**集成到 context**:
```typescript
const language = await this.detectLanguage();
const enhancedStructure = `
${projectStructure}

Detected Language: ${language}
Build Tool: ${buildTool}
`;
```

现在 LLM 会看到：
```
Detected Language: Java (26 files)
Build Tool: gradle
```

**修改文件**: `mpp-ui/src/jsMain/typescript/services/CodingAgentService.ts:295-360`

---

### 额外改进: 文件内容验证

为了及早发现类似问题，添加了写入验证：

```typescript
case 'write-file':
  result = await this.toolRegistry.writeFile(...);
  
  // ✅ 验证写入
  if (result.success && params.content) {
    const verifyResult = await this.toolRegistry.readFile(params.path);
    if (verifyResult.output !== params.content) {
      this.formatter.warn(`File content mismatch after write: ${params.path}`);
      this.formatter.debug(`Expected ${params.content.length} chars, got ${verifyResult.output.length} chars`);
    }
  }
```

**修改文件**: `mpp-ui/src/jsMain/typescript/services/CodingAgentService.ts:601-613`

---

## 测试计划

### Test 1: write-file 多行内容

```bash
cd /Volumes/source/ai/autocrud/mpp-ui
node dist/index.js code \
  --path /Users/phodal/IdeaProjects/untitled \
  --task "Create a HelloWorld.java file with a main method that prints Hello World"
```

**预期结果**:
- ✅ 文件内容完整，没有被截断
- ✅ 代码格式正确（带缩进和换行）
- ✅ 创建的是 `.java` 文件（不是 `.kt`）

### Test 2: Shell 命令执行

```bash
node dist/index.js code \
  --path /Users/phodal/IdeaProjects/untitled \
  --task "Run ./gradlew build to build the project"
```

**预期结果**:
- ✅ 找到 gradlew 文件
- ✅ 成功执行构建命令
- ✅ 显示构建输出

### Test 3: 项目语言识别

```bash
node dist/index.js code \
  --path /Users/phodal/IdeaProjects/untitled \
  --task "Add a new REST controller"
```

**预期结果**:
- ✅ 识别为 Java 项目
- ✅ 创建 `.java` 文件（不是 `.kt`）
- ✅ 使用 Java 语法（不是 Kotlin）

---

## 修改文件清单

| 文件 | 行数 | 修改类型 | 描述 |
|------|------|----------|------|
| `mpp-ui/src/jsMain/typescript/services/CodingAgentService.ts` | 476 | Bug Fix | 修复参数解析正则 |
| `mpp-ui/src/jsMain/typescript/services/CodingAgentService.ts` | 550 | Bug Fix | 添加 shell 默认工作目录 |
| `mpp-ui/src/jsMain/typescript/services/CodingAgentService.ts` | 295-360 | Feature | 添加语言检测功能 |
| `mpp-ui/src/jsMain/typescript/services/CodingAgentService.ts` | 220-243 | Enhancement | 集成语言信息到 context |
| `mpp-ui/src/jsMain/typescript/services/CodingAgentService.ts` | 601-613 | Enhancement | 添加写入验证 |

---

## 验证步骤

```bash
# 1. 构建项目
cd /Volumes/source/ai/autocrud
./gradlew :mpp-core:assembleJsPackage
cd mpp-ui
npm run build:ts

# 2. 清理之前的错误文件
cd /Users/phodal/IdeaProjects/untitled
git checkout build.gradle.kts src/
rm -rf src/main/kotlin/

# 3. 运行测试
cd /Volumes/source/ai/autocrud/mpp-ui
node dist/index.js code \
  --path /Users/phodal/IdeaProjects/untitled \
  --task "Create a simple HelloWorld.java file in src/main/java/cc/unitmesh/untitled/demo/ with a main method"

# 4. 验证结果
cd /Users/phodal/IdeaProjects/untitled
cat src/main/java/cc/unitmesh/untitled/demo/HelloWorld.java

# 预期输出：完整的 Java 代码，没有截断
```

---

## 关键要点

### 1. 正则表达式陷阱
- ⚠️ `[^"]*` 不匹配换行符
- ✅ 使用 `(?:[^"\\]|\\.)*` 支持转义和换行
- ✅ 添加 `s` 标志给整体 pattern

### 2. 默认参数的重要性
- ⚠️ 不要假设可选参数总是被提供
- ✅ 为关键参数提供合理的默认值
- ✅ 特别是路径相关参数（如 workingDirectory）

### 3. Context 的关键性
- ⚠️ LLM 完全依赖 context 做决策
- ✅ 提供准确的项目信息（语言、框架、build tool）
- ✅ 避免模糊信息导致错误判断

### 4. 验证和调试
- ✅ 添加写入验证捕获早期问题
- ✅ 使用 formatter.debug() 记录详细信息
- ✅ 在 verbose 模式下显示所有细节

---

## 影响范围

这些修复将改善:

1. **可靠性**: 文件内容不再被截断
2. **兼容性**: Shell 命令在正确的目录执行
3. **准确性**: 创建正确语言的源文件
4. **可调试性**: 验证和日志帮助快速定位问题

---

## 后续改进建议

1. **单元测试**: 为 `parseDevInsCommand` 添加测试覆盖各种边界情况
2. **集成测试**: 自动化测试 write-file → read-file 的往返验证
3. **Schema 验证**: 在 tool execution 前验证参数格式
4. **LLM Prompt 改进**: 在 system prompt 中明确说明：
   - Shell 命令在项目根目录执行
   - 优先使用项目检测到的主要语言
   - 文件路径应该相对于项目根目录

---

## 修复日期

2025-11-01

## 相关文档

- [Coding Agent UI Improvements](./coding-agent-ui-improvements.md) - UI/UX 改进
- [MPP Core Quick Summary](./mpp-core-quick-summary.md) - 核心架构


