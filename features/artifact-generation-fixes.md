# Artifact Generation 修复文档

## 问题描述

在生成 Node.js artifacts 时，AI 可能会生成**多个** artifact：
1. 第一个 artifact 包含 `package.json` 内容
2. 第二个 artifact 包含实际的 Express.js/Node.js 代码

如果系统只取第一个 artifact（`result.artifacts.first()`），会导致 `index.js` 文件包含 JSON 内容而不是代码，执行时报错：

```
Error: Validation failed: index.js contains invalid content (appears to be package.json)
```

## 根本原因

1. **AI 生成行为**: LLM 有时会将 `package.json` 和应用代码分成两个独立的 `<autodev-artifact>` 标签
2. **简单选择逻辑**: 代码使用 `artifacts.first()` 总是选择第一个，可能选到配置文件而不是代码

## 解决方案

### 1. 智能 Artifact 选择 (`ArtifactBundle.selectBestArtifact()`)

在 `ArtifactBundle.kt` 中添加了智能选择逻辑：

```kotlin
/**
 * Select the best artifact from multiple artifacts
 * 
 * For Node.js/React artifacts, avoids selecting package.json as main content
 * and prefers the artifact containing actual code.
 */
fun selectBestArtifact(artifacts: List<ArtifactAgent.Artifact>): ArtifactAgent.Artifact? {
    if (artifacts.isEmpty()) return null
    if (artifacts.size == 1) return artifacts.first()

    // Group artifacts by type
    val nodeJsArtifacts = artifacts.filter { 
        it.type == ArtifactAgent.Artifact.ArtifactType.NODEJS ||
        it.type == ArtifactAgent.Artifact.ArtifactType.REACT
    }

    if (nodeJsArtifacts.size > 1) {
        // For Node.js, skip artifacts that look like package.json
        val codeArtifact = nodeJsArtifacts.find { artifact ->
            val content = artifact.content.trim()
            // Skip if it's clearly JSON (package.json)
            !(content.startsWith("{") && content.contains("\"name\"") && content.contains("\"dependencies\""))
        }
        
        if (codeArtifact != null) {
            return codeArtifact
        }
    }

    // Fallback: return first artifact
    return artifacts.first()
}
```

**逻辑**:
- 如果只有一个 artifact，直接返回
- 如果有多个 Node.js/React artifacts:
  - 跳过看起来像 `package.json` 的 artifact（以 `{` 开头且包含 `"name"` 和 `"dependencies"`）
  - 返回包含代码的 artifact
- 兜底：返回第一个

### 2. 更新 ViewModel 使用智能选择

在 `ArtifactAgentViewModel.kt` 中：

```kotlin
// Before
lastArtifact = result.artifacts.first()

// After  
lastArtifact = ArtifactBundle.selectBestArtifact(result.artifacts)
```

### 3. 执行时的恢复逻辑 (NodeJsArtifactExecutor)

即使选择了错误的 artifact，执行器也会尝试从 `.artifact/context.json` 中恢复：

```kotlin
override suspend fun validate(...) {
    // Verify main file is actually code, not JSON
    val content = mainFile.readText()
    if (content.trim().startsWith("{") && content.contains("\"name\"")) {
        // Try to recover from context.json
        logger.warn { "⚠️ ${mainFile.name} contains JSON. Attempting recovery..." }
        val recovered = tryRecoverCodeFromContext(extractDir, bundleType)
        if (recovered != null) {
            logger.info { "✅ Recovered code from context, fixing ${mainFile.name}..." }
            mainFile.writeText(recovered)
        } else {
            errors.add("${mainFile.name} contains invalid content. Could not recover.")
        }
    }
}
```

## 测试验证

### 测试场景 1：单个 Artifact
```bash
./gradlew :mpp-ui:runArtifactCli -PartifactPrompt="生成一个 express.js hello world" -PartifactLanguage=ZH
```

**预期**: 生成单个包含代码的 artifact

### 测试场景 2：多个 Artifacts
```bash
./gradlew :mpp-ui:runArtifactCli -PartifactPrompt="生成一个 express.js hello world，要有 package.json 和 app.js 两个文件" -PartifactLanguage=ZH
```

**预期**: 
- AI 可能生成 2 个 artifacts
- `selectBestArtifact()` 选择包含代码的那个
- 生成的 `.unit` 文件可以正常执行

### 测试场景 3：已有错误的 .unit 文件
打开包含错误内容的 `.unit` 文件（`index.js` 是 JSON）

**预期**:
- `NodeJsArtifactExecutor.validate()` 检测到错误
- 从 `context.json` 恢复正确的代码
- 自动修复 `index.js`
- 正常执行

## 相关文件

- `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/artifact/ArtifactBundle.kt`
  - `selectBestArtifact()` 方法

- `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/artifact/ArtifactAgentViewModel.kt`
  - 使用 `selectBestArtifact()` 而不是 `first()`

- `mpp-core/src/jvmMain/kotlin/cc/unitmesh/agent/artifact/executor/NodeJsArtifactExecutor.kt`
  - `tryRecoverCodeFromContext()` - 从对话历史恢复代码
  - `extractCodeArtifactFromMessage()` - 提取正确的 artifact

## 未来改进

1. **改进 AI Prompt**: 在系统 prompt 中明确指示 Node.js artifacts 应该只生成一个 artifact，包含完整的代码（package.json 由系统自动生成）

2. **依赖提取**: 从生成的代码中自动提取 `require()` 或 `import` 语句，自动填充 `dependencies`

3. **更智能的 Artifact 合并**: 如果检测到多个相关的 artifacts（如 package.json + code），自动合并成一个完整的 bundle

## 参考

- [Issue #526 - AutoDev Unit](https://github.com/phodal/auto-dev/issues/526)
- [Claude Artifacts System](https://gist.github.com/dedlim/6bf6d81f77c19e20cd40594aa09e3ecd)

