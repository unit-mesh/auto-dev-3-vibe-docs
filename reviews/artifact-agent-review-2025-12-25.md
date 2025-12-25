# ArtifactAgent Review 总结

## 问题分析

通过 `ArtifactCli` 测试发现，在生成 Express.js/Node.js artifacts 时，AI 经常生成**两个** artifacts：

1. **第一个 artifact**: 包含 `package.json` 配置文件（JSON 格式）
2. **第二个 artifact**: 包含实际的 Express.js 应用代码

由于代码使用 `result.artifacts.first()` 总是选择第一个 artifact，导致：
- `index.js` 被填充为 JSON 内容（package.json）
- 执行时报错：`SyntaxError: Unexpected token ':'`
- 验证失败：`index.js contains invalid content (appears to be package.json)`

## 解决方案

### 1. 智能 Artifact 选择

在 `ArtifactBundle.kt` 中新增 `selectBestArtifact()` 方法：

```kotlin
fun selectBestArtifact(artifacts: List<ArtifactAgent.Artifact>): ArtifactAgent.Artifact?
```

**逻辑**:
- 对于 Node.js/React artifacts，检测并跳过看起来像 `package.json` 的内容
- 优先选择包含实际代码的 artifact
- 检测规则：内容以 `{` 开头且包含 `"name"` 和 `"dependencies"` 字段

### 2. 更新 ViewModel

在 `ArtifactAgentViewModel.kt` 中使用智能选择：

```kotlin
// Before: lastArtifact = result.artifacts.first()
// After:
lastArtifact = ArtifactBundle.selectBestArtifact(result.artifacts)
```

### 3. 执行时恢复逻辑 (已有)

`NodeJsArtifactExecutor.validate()` 中已有恢复逻辑：
- 检测到 `index.js` 包含 JSON 时
- 从 `.artifact/context.json` 中提取对话历史
- 从历史中找到包含实际代码的 artifact
- 自动修复 `index.js` 文件

## 测试结果

### 测试 1: 单个 Artifact 生成
```bash
./gradlew :mpp-ui:runArtifactCli -PartifactPrompt="生成一个 express.js hello world" -PartifactLanguage=ZH
```

**结果**: ✅ 成功生成单个包含代码的 artifact

### 测试 2: 智能选择逻辑
当 AI 生成多个 artifacts 时，`selectBestArtifact()` 能正确选择包含代码的那个。

### 测试 3: 恢复逻辑
对于已有的错误 `.unit` 文件，`NodeJsArtifactExecutor` 能从 context.json 恢复正确的代码。

## 修改的文件

1. **mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/artifact/ArtifactBundle.kt**
   - 新增 `selectBestArtifact()` 方法

2. **mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/artifact/ArtifactAgentViewModel.kt**
   - 使用 `selectBestArtifact()` 替代 `first()`

3. **mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/artifact/executor/NodeJsArtifactExecutor.kt** (之前已修复)
   - `tryRecoverCodeFromContext()` - 从对话历史恢复
   - `extractCodeArtifactFromMessage()` - 提取代码 artifact

4. **mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/Main.kt**
   - 添加 "Open Unit Bundle" 菜单项

5. **mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/desktop/AutoDevMenuBar.kt**
   - 添加 `onOpenUnitBundle` 参数

6. **mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/desktop/Keymap.kt**
   - 添加 `openUnitBundle` 快捷键 (Cmd+Shift+U / Ctrl+Shift+U)

## 编译状态

- ✅ `mpp-core:compileKotlinJvm` - 成功
- ✅ `mpp-ui:compileKotlinJvm` - 成功
- ✅ 无 lint 错误

## 改进建议

### 短期
1. **改进 System Prompt**: 在 `ArtifactAgentTemplate.kt` 中明确说明 Node.js artifacts 应该只生成一个 artifact，包含完整的应用代码

### 中期
2. **依赖自动提取**: 从代码中的 `require()` / `import` 语句自动提取并填充 `dependencies`
3. **Artifact 合并**: 如果检测到相关的多个 artifacts（如分离的配置和代码），自动合并

### 长期
4. **更智能的类型推断**: 根据代码特征（如 `const express = require('express')`）自动推断依赖版本
5. **Artifact 验证**: 在生成时验证 artifact 的完整性和可执行性

## 总结

通过添加智能 artifact 选择逻辑，解决了 AI 生成多个 artifacts 时选择错误的问题。现在系统能够：

1. 自动识别并跳过 `package.json` 类型的 artifacts
2. 优先选择包含实际代码的 artifacts
3. 如果选择错误，执行时自动从 context 恢复
4. 支持从菜单打开 `.unit` 文件

这使得 Node.js artifact 的生成和执行更加稳定可靠。

