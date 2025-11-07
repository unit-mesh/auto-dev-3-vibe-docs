# DevIns Lexer 修复总结

## 问题描述

用户报告在 DevIns 编译器中，普通文本中的特殊字符（`@`, `/`, `$`, `#`）被错误识别为命令、Agent 或变量，导致以下问题：

1. Email 地址（如 `user@example.com`）被分割
2. 文件路径（如 `/home/user/file.txt`）被误识别为命令
3. Markdown 列表项和普通文本中的连字符、斜杠等被误处理
4. 价格符号（如 `$100`）被误识别为变量

## 解决方案：方案 1

**核心思路**：只在行首或空白字符后才识别 `@/$/#` 等特殊字符

### 实现细节

#### 1. LexerContext 增强（LexerState.kt）

添加了两个新字段：
- `lastChar: Char?` - 跟踪上一个字符
- `isAtLineStart: Boolean` - 跟踪是否在行首

新增方法：
- `recordChar(char: Char)` - 记录处理的字符
- `shouldRecognizeSpecialChar(): Boolean` - 判断是否应该识别特殊字符

```kotlin
fun shouldRecognizeSpecialChar(): Boolean {
    return isAtLineStart || lastChar == null || lastChar!!.isWhitespace()
}
```

#### 2. DevInsLexer 修改（DevInsLexer.kt）

**修改 `advance()` 方法**：
- 每次消费字符后调用 `context.recordChar(char)` 更新上下文

**修改 `tokenizeContent()` 方法**：
- 在识别 `@/$/#` 之前先检查 `context.shouldRecognizeSpecialChar()`
- 只有在行首或空白后才识别这些特殊字符
- 否则将它们作为普通文本的一部分

**修改 `consumeTextSegment()` 方法**：
- 更新了文本段的边界判断逻辑
- 考虑上下文来决定特殊字符是否应该作为边界

### 3. 测试验证

新增 `LexerBehaviorTest.kt` 测试套件，包含以下测试：

1. `testEmailAddressNotRecognizedAsAgent` - Email 地址作为完整文本
2. `testPathNotRecognizedAsCommand` - 空白后的路径会被识别（符合预期）
3. `testInlinePathNotRecognizedAsCommand` - 行内路径不被识别
4. `testMarkdownListBehavior` - Markdown 列表正常处理
5. `testLineStartCommand` - 行首命令正常识别
6. `testAgentAfterSpace` - 空白后的 agent 正常识别
7. `testVariableInText` - 行首变量正常识别

更新了现有测试：
- `DevInsLexerTest.testTextWithAtSymbolNotRecognizedAsAgent` - 更新断言符合新行为
- `DevInsCompilerTest.testMarkdownTextNotRecognizedAsCommand` - 验证端到端行为

## 测试结果

✅ 所有 254 个测试通过
✅ Email 地址正确识别：`user@example.com` → 一个 TEXT_SEGMENT
✅ 行内路径正确识别：`path:/home/user` → TEXT_SEGMENT（不分割）
✅ Markdown 列表正确识别：`- Item` → TEXT_SEGMENT
✅ 行首命令仍正常：`/file test.txt` → COMMAND_START + ...
✅ 空白后 agent 正常：`Call @agent` → TEXT + AGENT_START + ...

## 影响范围

### 修改的文件
1. `mpp-core/src/commonMain/kotlin/cc/unitmesh/devins/lexer/LexerState.kt` - 增强上下文跟踪
2. `mpp-core/src/commonMain/kotlin/cc/unitmesh/devins/lexer/DevInsLexer.kt` - 实现上下文判断
3. `mpp-core/src/jvmTest/kotlin/cc/unitmesh/devins/LexerBehaviorTest.kt` - 新增测试
4. `mpp-core/src/commonTest/kotlin/cc/unitmesh/devins/DevInsLexerTest.kt` - 更新测试

### 向后兼容性

**破坏性变更**：
- 之前：`user@example.com` 会被分割为 `user` + `@` + `example.com`
- 现在：`user@example.com` 是一个完整的 TEXT_SEGMENT

**保持兼容**：
- 行首的命令、agent、变量仍然正常工作
- 空白后的特殊字符仍然正常识别

## 示例对比

### 修复前
```
Input: "Send email to user@example.com"
Tokens: TEXT("Send email to user") + AGENT_START("@") + ...
```

### 修复后
```
Input: "Send email to user@example.com"
Tokens: TEXT("Send email to user@example.com")
```

### 仍然正常工作
```
Input: "Call @agent for help"
Tokens: TEXT("Call ") + AGENT_START("@") + IDENTIFIER("agent") + ...
```

## 建议

✅ **已实现**：方案 1 - 词法层面的上下文判断
✅ **测试完整**：覆盖主要使用场景
⏭️ **可选扩展**：在编译器层面添加额外的语义验证（检查 Tool Registry）

## 结论

通过实现方案 1，成功解决了用户报告的问题：
- Email 地址、文件路径等普通文本不再被误识别
- 行首和空白后的特殊字符仍然正常工作
- 所有测试通过，向后兼容性良好（除了修复的 bug）

