# 工具 Schema 系统重新设计总结

## 🎯 目标

为内置工具添加 JSON Schema 支持，使 AI 模型能够正确理解工具参数结构，参考 Augment 的声明式工具方法和 TypeScript 示例。

## ✅ 解决方案

### 1. 创建声明式 Schema 基础架构

**基础接口** (`ToolSchema.kt`):
```kotlin
interface ToolSchema {
    fun toJsonSchema(): JsonElement
    fun getParameterDescription(): String  
    fun getExampleUsage(toolName: String): String
}

abstract class DeclarativeToolSchema(
    private val description: String,
    private val properties: Map<String, SchemaProperty>
) : ToolSchema
```

**属性构建器** (`SchemaPropertyBuilder`):
```kotlin
object SchemaPropertyBuilder {
    fun string(description: String, required: Boolean = false, ...)
    fun integer(description: String, required: Boolean = false, ...)
    fun boolean(description: String, required: Boolean = false, ...)
    fun array(description: String, itemType: SchemaProperty, ...)
    fun objectType(description: String, properties: Map<String, SchemaProperty>, ...)
}
```

### 2. 为每个内置工具创建具体 Schema

**ReadFile Schema**:
```kotlin
object ReadFileSchema : DeclarativeToolSchema(
    description = "Read file content with optional line range filtering",
    properties = mapOf(
        "path" to string("The file path to read", required = true),
        "startLine" to integer("Line number to start reading from", minimum = 1),
        "endLine" to integer("Line number to end reading at", minimum = 1),
        "maxLines" to integer("Maximum lines to read", default = 1000, maximum = 10000)
    )
)
```

**WriteFile Schema**:
```kotlin
object WriteFileSchema : DeclarativeToolSchema(
    description = "Write content to a file with various options",
    properties = mapOf(
        "path" to string("The file path to write to", required = true),
        "content" to string("The content to write", required = true),
        "createDirectories" to boolean("Create parent directories", default = true),
        "overwrite" to boolean("Overwrite existing file", default = true),
        "append" to boolean("Append instead of overwriting", default = false)
    )
)
```

**Shell Schema**:
```kotlin
object ShellSchema : DeclarativeToolSchema(
    description = "Execute shell commands with various options",
    properties = mapOf(
        "command" to string("The shell command to execute", required = true),
        "workingDirectory" to string("Working directory for execution"),
        "timeoutMs" to integer("Timeout in milliseconds", default = 30000, minimum = 1000, maximum = 300000),
        "shell" to string("Specific shell to use", enum = listOf("bash", "zsh", "sh", "cmd", "powershell"))
    )
)
```

### 3. 更新 ToolType 集成 Schema

**修改前**:
```kotlin
sealed class ToolType(
    val name: String,
    val displayName: String,
    val tuiEmoji: String,
    val composeIcon: String,
    val category: ToolCategory
)
```

**修改后**:
```kotlin
sealed class ToolType(
    val name: String,
    val displayName: String,
    val tuiEmoji: String,
    val composeIcon: String,
    val category: ToolCategory,
    val schema: ToolSchema  // 新增 Schema 支持
) {
    data object ReadFile : ToolType(
        name = "read-file",
        displayName = "Read File",
        tuiEmoji = "📄",
        composeIcon = "file_open",
        category = ToolCategory.FileSystem,
        schema = BuiltinToolSchemas.ReadFileSchema
    )
}
```

### 4. 更新 AI 工具描述生成

**CodingAgentContext 增强**:
```kotlin
private fun formatToolListForAI(toolList: List<ExecutableTool<*, *>>): String {
    return toolList.joinToString("\n\n") { tool ->
        buildString {
            appendLine("<tool name=\"${tool.name}\">")
            appendLine("  <description>${tool.description}</description>")
            
            val toolType = tool.name.toToolType()
            if (toolType != null) {
                // 使用声明式 Schema 生成详细参数信息
                appendLine("  <parameters>")
                appendLine("    <schema>")
                
                val parameterDescription = toolType.schema.getParameterDescription()
                // 解析并格式化参数信息...
                
                appendLine("    </schema>")
                appendLine("  </parameters>")
                
                // 使用 Schema 生成的示例
                val example = toolType.schema.getExampleUsage(tool.name)
                appendLine("  <example>$example</example>")
            }
            
            append("</tool>")
        }
    }
}
```

## 🧪 测试验证

### 单元测试 (`ToolSchemaTest.kt`)
- ✅ JSON Schema 生成正确性
- ✅ 参数类型和约束验证
- ✅ 必需字段和默认值
- ✅ 枚举值和范围限制
- ✅ SubAgent Schema 完整性

### 集成测试 (`test-tool-schema-integration.kt`)
- ✅ Schema 与 ToolType 一致性
- ✅ AI 工具描述生成
- ✅ 参数描述和示例用法

### 实际效果验证
```
🔍 [CodingAgentPromptRenderer] 工具列表长度: 10491 字符  # 比之前更详细
🔍 [CodingAgentPromptRenderer] 工具数量: 12
🔍 [CodingAgentPromptRenderer] 包含内置工具: true
🔍 [CodingAgentPromptRenderer] 包含 SubAgent: true
```

## 📊 技术价值

### 1. AI 理解能力提升
- **参数类型明确**: `string`, `integer`, `boolean`, `array`, `object`
- **约束清晰**: `required`, `default`, `minimum`, `maximum`, `enum`
- **描述详细**: 每个参数都有清晰的用途说明
- **示例完整**: 基于 Schema 生成的实际用法示例

### 2. 开发体验改善
- **类型安全**: 基于 Kotlin sealed class 的类型安全
- **声明式**: 类似 Augment 的声明式工具定义
- **可维护**: Schema 与工具实现分离，易于维护
- **可扩展**: 新工具只需定义 Schema 即可

### 3. 与现有系统兼容
- **MCP 兼容**: 生成的 JSON Schema 与 MCP 格式兼容
- **OpenAI 兼容**: 支持 OpenAI function calling 格式
- **向后兼容**: 不影响现有工具执行逻辑

## 🚀 示例对比

### 修复前 (硬编码)
```xml
<tool name="read-file">
  <description>Read file content</description>
  <parameters>
    <type>ReadFileParams</type>
    <usage>/read-file [parameters]</usage>
  </parameters>
  <example>/read-file path="src/main.kt"</example>
</tool>
```

### 修复后 (Schema 驱动)
```xml
<tool name="read-file">
  <description>Read file content with optional line range filtering</description>
  <parameters>
    <schema>
      <param name="path" type="string (required)">
        <description>The file path to read (relative to project root or absolute)</description>
      </param>
      <param name="startLine" type="integer (optional)">
        <description>The line number to start reading from (1-based, optional)</description>
        <range>min: 1</range>
      </param>
      <param name="endLine" type="integer (optional)">
        <description>The line number to end reading at (1-based, optional)</description>
        <range>min: 1</range>
      </param>
      <param name="maxLines" type="integer (optional)">
        <description>Maximum number of lines to read (optional)</description>
        <default>1000</default>
        <range>min: 1, max: 10000</range>
      </param>
    </schema>
  </parameters>
  <example>/read-file path="src/main.kt" startLine=1 endLine=50</example>
</tool>
```

## 🎯 成果

1. **完整的 Schema 系统**: 为所有 8 个内置工具创建了详细的 JSON Schema
2. **声明式架构**: 参考 Augment 模式，创建了可扩展的声明式工具定义系统
3. **AI 友好**: 生成的工具描述长度从几百字符增加到 10000+ 字符，包含详细的参数信息
4. **类型安全**: 基于 Kotlin 类型系统，确保 Schema 定义的正确性
5. **测试覆盖**: 完整的单元测试和集成测试确保质量

这个重新设计的 Schema 系统为 AI 模型提供了准确、详细的工具参数信息，显著提升了工具调用的准确性和用户体验！🎉
