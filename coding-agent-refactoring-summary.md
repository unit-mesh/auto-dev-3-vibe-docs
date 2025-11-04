# CodingAgent 重构总结

## 概述

将 `CodingAgentService` 的提示词生成逻辑从 TypeScript 迁移到 mpp-core，实现跨平台统一的架构设计。

## 重构目标

1. ✅ 将提示词模板和上下文构建逻辑移到 mpp-core
2. ✅ 创建 CodingAgent 的抽象接口支持跨平台实现
3. ✅ 使用模板引擎（类似 sketch.vm）动态渲染提示词
4. ✅ 工具列表和配置应该可以动态注入

## 架构设计

### 参考模式：SketchRunContext + sketch.vm

重构参考了 JetBrains 插件中 `SketchRunContext.kt` 和 `sketch.vm` 的设计模式：

- **SketchRunContext**: 数据类，收集所有上下文信息
- **sketch.vm**: Velocity 模板，使用 `${context.xxx}` 引用变量
- **TemplateCompiler**: 模板编译器，替换变量占位符

### 新架构组件

#### 1. mpp-core 核心抽象

**文件位置**: `/mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/`

##### CodingAgentContext.kt
```kotlin
data class CodingAgentContext(
    val currentFile: String?,
    val projectPath: String,
    val projectStructure: String = "",
    val osInfo: String,
    val timestamp: String,
    val toolList: String = "",
    val agentRules: String = "",
    val buildTool: String = "",
    val shell: String = "/bin/bash",
    val moduleInfo: String = "",
    val frameworkContext: String = "",
)
```

- 收集所有需要的上下文信息
- 提供 `toVariableTable()` 方法转换为模板变量
- 定义 `Builder` 接口用于平台特定的实现

##### CodingAgentTemplate.kt
```kotlin
object CodingAgentTemplate {
    const val EN = """..."""  // 英文提示词模板
    const val ZH = """..."""  // 中文提示词模板
}
```

- 包含英文和中文两个版本的提示词模板
- 使用 `${variableName}` 语法引用上下文变量
- 支持条件语句 `#if($variable)...#end`

##### CodingAgentService.kt
```kotlin
interface CodingAgentService {
    suspend fun executeTask(task: AgentTask): AgentResult
    fun buildSystemPrompt(context: CodingAgentContext, language: String = "EN"): String
    suspend fun initializeWorkspace(projectPath: String)
    fun getMaxIterations(): Int = 10
}
```

- 定义了 CodingAgent 的核心接口
- 可以被不同平台（JVM, JS, Android, iOS）实现

##### CodingAgentPromptRenderer.kt
```kotlin
class CodingAgentPromptRenderer {
    fun render(context: CodingAgentContext, language: String = "EN"): String {
        val template = when (language.uppercase()) {
            "ZH", "CN" -> CodingAgentTemplate.ZH
            else -> CodingAgentTemplate.EN
        }
        val variableTable = context.toVariableTable()
        val compiler = TemplateCompiler(variableTable)
        return compiler.compile(template)
    }
}
```

- 使用 `TemplateCompiler` 渲染模板
- 支持多语言（EN/ZH）

#### 2. JS Platform Exports

**文件位置**: `/mpp-core/src/jsMain/kotlin/cc/unitmesh/agent/CodingAgentExports.kt`

##### 导出的 JS 类：
- `JsCodingAgentContext` - JS 友好的上下文数据类
- `JsCodingAgentContextBuilder` - 用于构建上下文的 Builder
- `JsCodingAgentPromptRenderer` - 提示词渲染器
- `JsAgentStep`, `JsAgentEdit`, `JsAgentResult` - Agent 执行相关的数据类

#### 3. TypeScript 实现

**文件位置**: `/mpp-ui/src/jsMain/typescript/services/CodingAgentService.ts`

重构后的 TypeScript 实现：
```typescript
// 导入 mpp-core 的类
const { JsCompletionManager, JsToolRegistry } = MppCore.cc.unitmesh.llm;
const { JsCodingAgentContextBuilder, JsCodingAgentPromptRenderer } = MppCore.cc.unitmesh.agent;

// 使用 Builder 构建上下文
const builder = new JsCodingAgentContextBuilder();
const context = builder
  .setProjectPath(this.projectPath)
  .setOsInfo(osInfo)
  .setTimestamp(timestamp)
  .setProjectStructure(projectStructure)
  .setToolList(toolList)
  .build();

// 使用 Renderer 渲染提示词
const systemPrompt = this.promptRenderer.render(context, 'EN');
```

## 重构前后对比

### 之前 (TypeScript 硬编码)
```typescript
async buildSystemPrompt(task: AgentTask): Promise<string> {
  return `You are AutoDev...
## Environment Information
- OS: ${osInfo}
...`;
}
```

**问题**：
- 提示词硬编码在 TypeScript 中
- 无法跨平台复用
- 工具列表硬编码
- 难以维护和更新

### 之后 (mpp-core 模板)
```kotlin
// 模板定义在 mpp-core
const val EN = """You are AutoDev...
## Environment Information
- OS: ${'$'}{osInfo}
..."""

// TypeScript 只需构建上下文并渲染
const context = builder.setOsInfo(osInfo).build();
const prompt = renderer.render(context, 'EN');
```

**优势**：
- ✅ 提示词逻辑在 mpp-core 中统一管理
- ✅ 支持多平台（JVM, JS, Android, iOS）
- ✅ 使用模板引擎，易于维护
- ✅ 工具列表和配置可以动态注入
- ✅ 支持多语言（EN/ZH）

## 实现细节

### 1. 变量替换
使用 `TemplateCompiler` 替换 `${variableName}` 格式的变量：
```kotlin
table.addVariable("projectPath", VariableType.STRING, "/test/project")
// 模板中的 ${projectPath} 会被替换为 "/test/project"
```

### 2. 条件语句
支持 `#if` 条件判断：
```kotlin
#if (${frameworkContext})
- Framework Context: ${frameworkContext}
#end
```

### 3. 多语言支持
通过 `language` 参数选择模板：
```typescript
renderer.render(context, 'EN')  // 英文提示词
renderer.render(context, 'ZH')  // 中文提示词
```

## 测试验证

创建了完整的测试脚本 `test-scripts/test-coding-agent-refactor.js`：

### 测试内容
1. ✅ Context Building - 使用 Builder 构建上下文
2. ✅ Prompt Rendering (EN) - 渲染英文提示词
3. ✅ Prompt Rendering (ZH) - 渲染中文提示词
4. ✅ Template Variable Substitution - 变量替换验证

### 测试结果
```
✅ All tests passed!

📊 Summary:
   - Context building: ✓
   - Prompt rendering (EN): ✓
   - Prompt rendering (ZH): ✓
   - Template variable substitution: ✓
```

## 构建流程

```bash
# 1. 编译 mpp-core（包含新的 CodingAgent 抽象）
cd /Volumes/source/ai/autocrud
./gradlew :mpp-core:assembleJsPackage

# 2. 编译 mpp-ui TypeScript
cd mpp-ui
npm run build:ts

# 3. 运行测试
node test-scripts/test-coding-agent-refactor.js
```

## 未来扩展

### 1. JVM 实现
可以在 JetBrains 插件中使用相同的抽象：
```kotlin
class JvmCodingAgentService : CodingAgentService {
    override suspend fun executeTask(task: AgentTask): AgentResult {
        val context = JvmCodingAgentContext.create(project, task.requirement)
        val prompt = buildSystemPrompt(context)
        // ... JVM specific implementation
    }
}
```

### 2. Android/iOS 实现
可以在移动应用中复用：
```kotlin
class AndroidCodingAgentService : CodingAgentService {
    // Android specific implementation
}
```

### 3. 工具扩展
工具列表可以通过 `ToolRegistry` 动态获取：
```typescript
const tools = this.toolRegistry.getAgentTools();
const toolList = tools.map(tool => `**${tool.name}** - ${tool.description}`).join('\n');
```

## 文件清单

### mpp-core (commonMain)
- `cc/unitmesh/agent/CodingAgentContext.kt` - 上下文数据类
- `cc/unitmesh/agent/CodingAgentTemplate.kt` - 模板定义
- `cc/unitmesh/agent/CodingAgentService.kt` - 服务接口
- `cc/unitmesh/agent/CodingAgentPromptRenderer.kt` - 提示词渲染器

### mpp-core (jsMain)
- `cc/unitmesh/agent/CodingAgentExports.kt` - JS 平台导出

### mpp-ui
- `src/jsMain/typescript/services/CodingAgentService.ts` - TypeScript 实现
- `test-scripts/test-coding-agent-refactor.js` - 测试脚本

## 结论

这次重构成功地将 CodingAgent 的核心逻辑从 TypeScript 迁移到了 mpp-core，实现了：

1. **统一的抽象层** - 所有平台共享相同的接口和模板
2. **更好的维护性** - 提示词模板集中管理，易于更新
3. **跨平台支持** - 一次实现，多平台复用
4. **动态配置** - 工具和上下文可以动态注入
5. **多语言支持** - 内置英文和中文模板

重构完全遵循了 SketchRunContext 的设计模式，并通过完整的测试验证了实现的正确性。

