# 中国LLM提供商支持

本文档说明如何使用 GLM（智谱AI）、Qwen（阿里通义千问）和 Kimi（月之暗面）等中国常用的 LLM 提供商。

## 概述

之前这些提供商需要使用通用的 `custom-openai-base` 配置，现在它们已成为独立的 Provider 类型，提供更好的用户体验：

- ✅ 预设的模型列表
- ✅ 自动填充的 baseUrl
- ✅ 中文界面友好提示
- ✅ 针对性的配置验证

## 支持的提供商

### 1. GLM (智谱AI)

**API 密钥获取**: https://open.bigmodel.cn/

**Base URL** (自动填充): `https://open.bigmodel.cn/api/paas/v4`

**推荐模型**:
- `glm-4-plus` - 智能体增强版，128K上下文
- `glm-4-air` - 高性价比版本
- `glm-4-airx` - 超高性价比版本
- `glm-4-flash` - 免费版本
- `glm-4-flashx` - 超快版本
- `glm-4-long` - 长文本版本，1M上下文
- `glm-4` - 标准版
- `glm-3-turbo` - 快速版

**配置示例** (`~/.autodev/config.yaml`):
```yaml
- name: my-glm
  provider: glm
  apiKey: your-glm-api-key.xxxxx
  model: glm-4-plus
  baseUrl: https://open.bigmodel.cn/api/paas/v4
  temperature: 0.7
  maxTokens: 128000
```

### 2. Qwen (阿里通义千问)

**API 密钥获取**: https://dashscope.console.aliyun.com/

**Base URL** (自动填充): `https://dashscope.aliyuncs.com/api/v1`

**推荐模型**:
- `qwen-max` - 最强版本，8K上下文
- `qwen-max-latest` - 最新最强版本
- `qwen-plus` - 增强版
- `qwen-plus-latest` - 最新增强版
- `qwen-turbo` - 快速版
- `qwen-turbo-latest` - 最新快速版
- `qwen-long` - 长文本版本，10M上下文
- `qwen2.5-72b-instruct` - 开源最强，131K上下文
- `qwen2.5-32b-instruct` - 开源增强版
- `qwen2.5-14b-instruct` - 开源标准版
- `qwen2.5-7b-instruct` - 开源轻量版

**配置示例**:
```yaml
- name: my-qwen
  provider: qwen
  apiKey: sk-your-qwen-api-key
  model: qwen-max
  baseUrl: https://dashscope.aliyuncs.com/api/v1
  temperature: 0.7
  maxTokens: 8000
```

### 3. Kimi (月之暗面 Moonshot AI)

**API 密钥获取**: https://platform.moonshot.cn/

**Base URL** (自动填充): `https://api.moonshot.cn/v1`

**推荐模型**:
- `moonshot-v1-8k` - 8K 上下文
- `moonshot-v1-32k` - 32K 上下文
- `moonshot-v1-128k` - 128K 上下文

**配置示例**:
```yaml
- name: my-kimi
  provider: kimi
  apiKey: sk-your-kimi-api-key
  model: moonshot-v1-32k
  baseUrl: https://api.moonshot.cn/v1
  temperature: 0.7
  maxTokens: 8192
```

## 使用方法

### CLI 配置

使用交互式配置工具：

```bash
cd mpp-ui && npm run start
```

选择对应的 Provider：
- 🔹 智谱AI (GLM)
- 🔹 阿里通义千问 (Qwen)
- 🔹 月之暗面 (Kimi)

系统会自动填充默认的 baseUrl，你只需要：
1. 输入 API Key
2. 选择或输入模型名称
3. 保存配置

### JVM Desktop UI 配置

1. 运行 Desktop 应用：
   ```bash
   ./gradlew :mpp-ui:run
   ```

2. 点击右上角的模型选择器

3. 选择 "Configure Model"

4. 在 Provider 下拉菜单中选择：
   - GLM
   - Qwen
   - Kimi

5. 系统会自动填充 baseUrl，你只需输入：
   - 配置名称
   - API Key
   - 选择模型

### 配置文件直接编辑

你也可以直接编辑 `~/.autodev/config.yaml`：

```yaml
active: my-glm

configs:
  - name: my-glm
    provider: glm
    apiKey: your-glm-api-key.xxxxx
    model: glm-4-plus
    baseUrl: https://open.bigmodel.cn/api/paas/v4
    temperature: 0.7
    maxTokens: 128000

  - name: my-qwen
    provider: qwen
    apiKey: sk-your-qwen-api-key
    model: qwen-max
    baseUrl: https://dashscope.aliyuncs.com/api/v1
    temperature: 0.7
    maxTokens: 8000

  - name: my-kimi
    provider: kimi
    apiKey: sk-your-kimi-api-key
    model: moonshot-v1-32k
    baseUrl: https://api.moonshot.cn/v1
    temperature: 0.7
    maxTokens: 8192
```

## 技术实现

### 核心组件

1. **ModelConfig.kt** - Provider 枚举定义
   - 新增 `GLM`, `QWEN`, `KIMI` 枚举值

2. **ModelRegistry.kt** - 模型注册和管理
   - 为每个 Provider 预设模型列表
   - `getDefaultBaseUrl()` 返回默认 API 端点
   - 自动创建模型对象

3. **ExecutorFactory.kt** - LLM 客户端创建
   - 使用 `CustomOpenAILLMClient` 实现
   - 自动填充 baseUrl（如果未提供）

4. **CustomOpenAILLMClient.kt** - OpenAI 兼容客户端
   - 默认路径：`/chat/completions`（带前导斜杠）
   - 支持 GLM、Qwen、Kimi 等所有 OpenAI 兼容 API

### UI 组件

- **CLI**: `ModelConfigForm.tsx` - 交互式配置表单
- **Desktop**: `ModelConfigDialog.kt` - Compose UI 配置对话框
- **i18n**: 中英文界面支持

## 与 custom-openai-base 的区别

| 特性 | 独立 Provider (GLM/Qwen/Kimi) | custom-openai-base |
|------|------------------------------|---------------------|
| 模型列表 | ✅ 预设的模型列表 | ❌ 需要手动输入 |
| Base URL | ✅ 自动填充 | ❌ 需要手动输入 |
| 配置提示 | ✅ 针对性提示 | ⚠️ 通用提示 |
| 模型上下文 | ✅ 自动配置 | ❌ 使用默认值 |
| 使用场景 | 常用中国 LLM | 其他 OpenAI 兼容 API |

## 配置名称自动递增

如果你创建的配置名称已存在，系统会自动添加后缀：
- `my-glm` → `my-glm-1` → `my-glm-2` ...

这样可以避免意外覆盖已有配置。

## 故障排除

### 405 错误 (Method Not Allowed) - 已修复 ✅

**问题**: `Expected status code 200 but was 405`

**原因**: Ktor URL 拼接问题 - 当 `chatCompletionsPath` 以 `/` 开头时，Ktor 将其视为绝对路径，会丢弃 `baseUrl` 的路径部分。

**示例**:
- baseUrl = `https://open.bigmodel.cn/api/paas/v4`
- chatCompletionsPath = `/chat/completions` (带前导斜杠)
- **错误结果**: `https://open.bigmodel.cn/chat/completions` ❌ (丢失了 `/api/paas/v4`)

**修复**: 
- 将 `chatCompletionsPath` 改为 `"chat/completions"` (不带前导斜杠)
- **正确结果**: `https://open.bigmodel.cn/api/paas/v4/chat/completions` ✅

**当前状态**: 已在代码中修复，默认值现在是 `"chat/completions"`（无前导斜杠）

### URL 路径段丢失 (v4 丢失) - 已修复 ✅

**问题**: API 调用时路径中的版本号（如 `v4`）丢失

**原因**: Ktor URL 相对路径解析 - 如果 baseUrl 不以 `/` 结尾，相对 path 会替换最后一个路径段

**示例**:
- baseUrl: `https://open.bigmodel.cn/api/paas/v4` (无尾部斜杠)
- path: `chat/completions`
- **错误结果**: `https://open.bigmodel.cn/api/paas/chat/completions` ❌ (v4 被替换了!)

**修复**: 系统现在自动确保 baseUrl 在内存中以 `/` 结尾

**当前状态**: ✅ 已修复。`ModelRegistry.getDefaultBaseUrl` 返回带尾部斜杠的 URL

### 404 错误

**问题**: `Expected status code 200 but was 404`

**原因**: URL 路径配置错误

**解决**:
1. 确保 baseUrl **不包含** `/chat/completions` 路径
2. 在配置文件中，baseUrl 可以有或没有尾部 `/`（系统会自动处理）

✅ 正确: `https://open.bigmodel.cn/api/paas/v4`  
✅ 正确: `https://open.bigmodel.cn/api/paas/v4/`  
❌ 错误: `https://open.bigmodel.cn/api/paas/v4/chat/completions`

### object 字段缺失

**问题**: `Field 'object' is required... but it was missing`

**解决**: 这个问题已修复。`CustomOpenAILLMClient` 现在将 `object` 字段标记为可选。

### Provider 名称不匹配

**问题**: 配置文件中的 provider 名称与代码不一致

**解决**: `ConfigFile.kt` 会自动标准化 provider 名称：
- `glm` / `GLM` / `glm` → `GLM`
- `qwen` / `QWEN` / `qwen` → `QWEN`
- `custom-openai-base` / `custom_openai_base` → `CUSTOM_OPENAI_BASE`

## 更新日志

### 2025-01-06 (Update 3) - 修复 URL 路径段丢失问题

🐛 **关键修复 - URL 拼接问题**:
- **问题**: baseUrl 的最后一个路径段（如 `v4`）在拼接时丢失
  - baseUrl = `https://open.bigmodel.cn/api/paas/v4`
  - path = `chat/completions`
  - **错误结果**: `https://open.bigmodel.cn/api/paas/chat/completions` ❌ (v4 丢失!)
  
- **根本原因**: Ktor URL 相对路径解析规则
  - 如果 baseUrl 不以 `/` 结尾，相对 path 会**替换最后一个路径段**
  - 这是标准的 URL 相对路径行为
  
- **解决方案**: baseUrl 必须以 `/` 结尾
  - baseUrl = `https://open.bigmodel.cn/api/paas/v4/` ✅ (注意尾部斜杠)
  - path = `chat/completions` (无前导斜杠)
  - **正确结果**: `https://open.bigmodel.cn/api/paas/v4/chat/completions` ✅

- **修复内容**:
  1. `ModelRegistry.getDefaultBaseUrl` 现在返回带尾部斜杠的 URL
  2. `ConfigFile.toModelConfig` 确保加载时添加尾部斜杠
  3. `ConfigFile.fromModelConfig` 保存时移除尾部斜杠（YAML 可读性）
  4. 内存中：baseUrl 有尾部斜杠（正确拼接）
  5. YAML 中：baseUrl 无尾部斜杠（更易读）

### 2025-01-06 (Update 2) - 修复 405 错误

🐛 **关键修复**:
- **405 错误修复**: 将 `chatCompletionsPath` 从 `"/chat/completions"` 改为 `"chat/completions"`（移除前导斜杠）
  - **原因**: Ktor 中，带 `/` 前缀的 path 会被视为绝对路径，导致 baseUrl 的路径部分被丢弃
  - **影响**: 修复了 GLM、Qwen、Kimi 等所有 OpenAI 兼容 API 的调用问题
  - **详情**: 参见代码注释中的 "IMPORTANT URL Construction in Ktor"

### 2025-01-06 (Update 1) - 新增中国 LLM 提供商

✨ **新增**: GLM、Qwen、Kimi 作为独立 Provider

- 新增 3 个 LLM Provider 枚举值
- 为每个 Provider 添加预设模型列表和默认 baseUrl
- 更新 CLI 和 Desktop UI 配置界面
- 添加中英文 i18n 支持
- 更新配置示例文件

🐛 **修复**:
- CustomOpenAILLMClient 的 `object` 字段改为可选
- ConfigFile 自动移除 baseUrl 的尾部斜杠
- 配置名称自动递增以避免覆盖

📝 **文档**: 新增本文档

## 参考链接

- [智谱AI API 文档](https://open.bigmodel.cn/dev/api)
- [通义千问 API 文档](https://help.aliyun.com/zh/dashscope/)
- [Moonshot AI API 文档](https://platform.moonshot.cn/docs)

