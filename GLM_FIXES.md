# GLM 4.6 Bug修复总结

## 用户报告的问题

### 1. GLM API 响应格式错误 ❌
```
Error from CustomOpenAILLMClient API: 200 OK: Field 'object' is required 
for type 'CustomOpenAIChatCompletionStreamResponse', but it was missing
```

**原因**: GLM 的流式响应中不包含 `object` 字段，但我们的模型定义中将其标记为必需。

### 2. Provider 名称不一致 ⚠️
用户配置文件中：
```yaml
provider: custom_openai_base  # 下划线
```

但系统使用：
```kotlin
LLMProviderType.CUSTOM_OPENAI_BASE  // 下划线
```

序列化时产生不一致。

### 3. baseUrl 末尾斜杠 ⚠️
```yaml
baseUrl: https://open.bigmodel.cn/api/paas/v4/  # 末尾有斜杠
```

可能导致路径拼接问题。

### 4. 配置覆盖问题 ❌
用户报告：保存 GLM 配置时覆盖了原来的 DeepSeek 配置。

**原因**: 
- 用户选择了 `my-deepseek` 配置
- 修改了 provider 从 DEEPSEEK 改为 CUSTOM_OPENAI_BASE
- 保存时还是用 `my-deepseek` 这个名字
- 结果覆盖了原配置

---

## 修复方案

### ✅ 修复 1: GLM API 兼容性

**文件**: `mpp-core/src/commonMain/kotlin/cc/unitmesh/llm/clients/CustomOpenAILLMClient.kt`

**修改**:
```kotlin
// 修改前
@Serializable
data class CustomOpenAIChatCompletionStreamResponse(
    override val id: String,
    val `object`: String,  // Required
    ...
)

// 修改后
@Serializable
data class CustomOpenAIChatCompletionStreamResponse(
    override val id: String,
    val `object`: String? = null,  // Optional: GLM may not include this
    ...
)
```

同样修复了非流式响应：
```kotlin
data class CustomOpenAIChatCompletionResponse(
    override val id: String,
    val `object`: String? = null,  // Optional
    ...
)
```

### ✅ 修复 2: Provider 名称规范化

**文件**: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/config/ConfigFile.kt`

**修改 toModelConfig()**:
```kotlin
fun toModelConfig(): ModelConfig {
    // 修改前：简单的 ignoreCase 匹配
    val providerType = cc.unitmesh.llm.LLMProviderType.entries.find {
        it.name.equals(provider, ignoreCase = true)
    } ?: cc.unitmesh.llm.LLMProviderType.OPENAI

    // 修改后：规范化处理，支持下划线和中划线
    val normalizedProvider = provider.replace("-", "_").uppercase()
    val providerType = cc.unitmesh.llm.LLMProviderType.entries.find {
        it.name == normalizedProvider
    } ?: cc.unitmesh.llm.LLMProviderType.OPENAI
    
    return ModelConfig(
        ...
        baseUrl = baseUrl.trimEnd('/')  // 移除末尾斜杠
    )
}
```

**修改 fromModelConfig()**:
```kotlin
fun fromModelConfig(name: String, config: ModelConfig): NamedModelConfig {
    // 修改前：使用下划线
    provider = config.provider.name.lowercase(),  // CUSTOM_OPENAI_BASE -> custom_openai_base
    
    // 修改后：使用中划线（更符合 YAML 习惯）
    val providerName = config.provider.name.lowercase().replace("_", "-")
    return NamedModelConfig(
        ...
        provider = providerName,  // CUSTOM_OPENAI_BASE -> custom-openai-base
        baseUrl = config.baseUrl.trimEnd('/'),  // 移除末尾斜杠
    )
}
```

**效果**:
- ✅ 支持 `custom_openai_base`（下划线，用户手动编辑）
- ✅ 支持 `custom-openai-base`（中划线，系统生成）
- ✅ 支持 `CUSTOM_OPENAI_BASE`（大写，enum 名称）
- ✅ 自动移除 baseUrl 末尾的斜杠

### ✅ 修复 3: 防止配置覆盖

**问题根源**: 
在 `ModelSelector.kt` 中，保存时使用当前选中配置的名称：
```kotlin
val configName = currentConfigName ?: "default"  // ❌ 错误
```

这导致：
1. 选择 `my-deepseek` 配置
2. 修改 provider
3. 保存时还用 `my-deepseek`
4. 覆盖了原配置

**解决方案**: 在 `ModelConfigDialog` 中添加配置名称输入字段

**文件**: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/editor/ModelConfigDialog.kt`

**修改签名**:
```kotlin
// 修改前
@Composable
fun ModelConfigDialog(
    currentConfig: ModelConfig,
    onDismiss: () -> Unit,
    onSave: (ModelConfig) -> Unit
)

// 修改后
@Composable
fun ModelConfigDialog(
    currentConfig: ModelConfig,
    currentConfigName: String? = null,  // 新增：当前配置名称
    onDismiss: () -> Unit,
    onSave: (configName: String, config: ModelConfig) -> Unit  // 修改：返回配置名称
)
```

**添加 UI**:
```kotlin
// Configuration Name
Text(text = "配置名称", ...)
OutlinedTextField(
    value = configName,
    onValueChange = { configName = it },
    placeholder = { Text("e.g., my-glm, work-gpt4, personal-claude") },
    supportingText = { 
        Text("给配置起一个唯一的名称，便于识别和切换") 
    }
)
```

**更新验证逻辑**:
```kotlin
Button(
    ...
    enabled = configName.trim().isNotBlank() && /* 其他验证 */
) {
    onSave(configName.trim(), config)
}
```

**文件**: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/editor/ModelSelector.kt`

**修改调用**:
```kotlin
ModelConfigDialog(
    currentConfig = currentConfig?.toModelConfig() ?: ModelConfig(),
    currentConfigName = currentConfigName,  // 传入当前名称
    onDismiss = { showConfigDialog = false },
    onSave = { configName, newModelConfig ->  // 接收名称参数
        val namedConfig = NamedModelConfig.fromModelConfig(
            name = configName,  // 使用用户输入的名称
            config = newModelConfig
        )
        ConfigManager.saveConfig(namedConfig, setActive = true)
        ...
    }
)
```

**效果**:
- ✅ 用户必须明确输入配置名称
- ✅ 如果编辑现有配置，显示当前名称
- ✅ 如果创建新配置，字段为空，用户必须输入
- ✅ 不会意外覆盖其他配置

---

## 用户配置文件修正建议

当前配置：
```yaml
active: my-deepseek
configs:
  - name: my-deepseek  # ❌ 名字不匹配内容
    provider: custom_openai_base  # 这是 GLM，不是 DeepSeek
    apiKey: 7145ac1bf6474f2783e8b4d52b335ab0.gfq0BBvvFy04iwTb
    model: glm-4.6
    baseUrl: https://open.bigmodel.cn/api/paas/v4/
    temperature: 0.0
    maxTokens: 128000
```

**建议修改为**:
```yaml
active: my-glm

configs:
  # DeepSeek 配置（需要重新添加）
  - name: my-deepseek
    provider: deepseek
    apiKey: YOUR_DEEPSEEK_API_KEY
    model: deepseek-chat
    temperature: 0.7
    maxTokens: 8192

  # GLM 配置
  - name: my-glm
    provider: custom-openai-base  # 或 custom_openai_base（都支持）
    apiKey: 7145ac1bf6474f2783e8b4d52b335ab0.gfq0BBvvFy04iwTb
    model: glm-4.6
    baseUrl: https://open.bigmodel.cn/api/paas/v4  # 末尾斜杠会被自动移除
    temperature: 0.0
    maxTokens: 128000
```

**命名建议**:
- ✅ `my-glm` - 明确是 GLM
- ✅ `work-glm-4-6` - 包含版本信息
- ✅ `personal-chatglm` - 用途+模型
- ❌ `my-deepseek` 用于 GLM - 容易混淆

---

## 测试验证

### 1. 测试 GLM 4.6 API
```bash
# 应该不再报错 "Field 'object' is required"
# 流式响应正常工作
```

### 2. 测试配置加载
```bash
# 测试各种 provider 格式
custom_openai_base  ✅
custom-openai-base  ✅
CUSTOM_OPENAI_BASE  ✅
```

### 3. 测试 baseUrl 处理
```bash
# 输入任何末尾斜杠的 URL，都会被自动移除
https://open.bigmodel.cn/api/paas/v4/  → https://open.bigmodel.cn/api/paas/v4
```

### 4. 测试配置保存
```bash
1. 打开配置对话框
2. 选择现有配置（如 my-glm）
3. 查看配置名称字段显示 "my-glm"
4. 修改配置但保持名称不变 → 更新现有配置
5. 修改配置并改变名称 → 创建新配置
6. 创建新配置 → 名称字段为空，必须输入
```

---

## 修改文件列表

1. ✅ `mpp-core/src/commonMain/kotlin/cc/unitmesh/llm/clients/CustomOpenAILLMClient.kt`
   - `object` 字段标记为可选

2. ✅ `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/config/ConfigFile.kt`
   - Provider 名称规范化（支持下划线和中划线）
   - baseUrl 自动移除末尾斜杠

3. ✅ `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/editor/ModelConfigDialog.kt`
   - 添加配置名称输入字段
   - 更新回调签名以返回配置名称

4. ✅ `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/editor/ModelSelector.kt`
   - 更新 ModelConfigDialog 调用
   - 使用用户输入的配置名称

5. ✅ `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/AutoDevApp.kt`
   - 更新 ModelConfigDialog 调用

---

## 总结

所有问题已修复：

1. ✅ **GLM API 兼容性** - `object` 字段现在是可选的
2. ✅ **Provider 名称处理** - 支持多种格式（下划线/中划线/大写）
3. ✅ **baseUrl 处理** - 自动移除末尾斜杠
4. ✅ **配置覆盖问题** - 用户必须明确输入/确认配置名称

**用户需要做的**:
1. 手动修复 `~/.autodev/config.yaml`，将 `my-deepseek` 配置重命名为 `my-glm`
2. 如果想同时保留 DeepSeek，需要重新添加一个正确的 DeepSeek 配置
3. 重新启动应用，GLM 4.6 应该可以正常工作了

