# GLM-4.6V 多模态视觉功能使用指南

## 概述

AutoDev 现在支持 GLM-4.6V 多模态视觉理解功能，可以分析图片内容并提供详细描述。

## 功能特性

- ✅ **自动图片压缩**：减少 token 消耗，节省成本
- ✅ **腾讯云 COS 集成**：自动上传到云存储并生成 URL
- ✅ **流式响应**：实时显示 AI 分析结果
- ✅ **深度思考模式**：可选启用更深层次的推理分析

## 快速开始

### 1. 配置 GLM API Key

在 `~/.autodev/config.yaml` 中配置 GLM：

```yaml
active: "glm"
configs:
  - name: "glm"
    provider: "glm"
    apiKey: "your-glm-api-key"
    model: "glm-4.6v"  # 使用视觉模型
    baseUrl: "https://open.bigmodel.cn/api/paas/v4/"
    temperature: 0.0
    maxTokens: 8192  # GLM-4.6V 最大输出 tokens
```

### 2. 配置腾讯云 COS（必需）

GLM-4.6V 要求图片以 URL 形式提供，不支持 base64。

#### 2.1 创建 COS Bucket

1. 登录 [腾讯云控制台](https://console.cloud.tencent.com/cos/bucket)
2. 创建存储桶，记下：
   - Bucket 名称（格式：`bucketname-appid`）
   - 所在区域（如 `ap-beijing`）

#### 2.2 获取访问凭证

1. 进入 [API 密钥管理](https://console.cloud.tencent.com/cam/capi)
2. 创建密钥，获取：
   - SecretId
   - SecretKey

⚠️ **注意**：不要将密钥提交到代码库！

### 3. 运行 Vision CLI

#### 方法 1：使用命令行参数

```bash
./gradlew :mpp-ui:runVisionCli \
    -PvisionImage=/path/to/image.png \
    -PvisionPrompt="请描述这张图片的内容" \
    -PcosSecretId="YOUR_SECRET_ID" \
    -PcosSecretKey="YOUR_SECRET_KEY" \
    -PcosBucket="your-bucket-appid" \
    -PcosRegion="ap-beijing"
```

#### 方法 2：使用环境变量

```bash
export TENCENT_COS_SECRET_ID="YOUR_SECRET_ID"
export TENCENT_COS_SECRET_KEY="YOUR_SECRET_KEY"
export TENCENT_COS_BUCKET="your-bucket-appid"
export TENCENT_COS_REGION="ap-beijing"

./gradlew :mpp-ui:runVisionCli \
    -PvisionImage=/path/to/image.png \
    -PvisionPrompt="请描述这张图片"
```

#### 启用深度思考模式

```bash
./gradlew :mpp-ui:runVisionCli \
    -PvisionImage=/path/to/image.png \
    -PvisionPrompt="请分析这张图片的设计元素" \
    -PenableThinking=true \
    -PcosSecretId="..." \
    -PcosSecretKey="..." \
    -PcosBucket="..."
```

## 测试 Bucket 区域

如果不确定 Bucket 在哪个区域，可以使用测试工具：

```bash
./gradlew :mpp-ui:runCosTest -Pbucket="your-bucket-appid"
```

该工具会自动扫描所有腾讯云区域并找到你的 Bucket。

## 压缩配置

默认使用 BALANCED 压缩配置（1024x1024，质量 0.8，最大 500KB），可以在代码中调整：

```kotlin
val config = ImageCompressor.Config(
    maxWidth = 2048,       // 最大宽度
    maxHeight = 2048,      // 最大高度
    quality = 0.9f,        // JPEG 质量 (0.0-1.0)
    maxFileSize = 1024 * 1024,  // 最大文件大小
    format = ImageCompressor.OutputFormat.JPEG
)
```

## 支持的模型

- `glm-4.6v` - 旗舰视觉推理（推荐）
- `glm-4.5v` - 视觉理解
- `glm-4.1v-thinking` - 深度思考视觉

## 示例输出

```
📸 Compressing image: screenshot.png
   CompressionResult(original=1488x612 48KB, compressed=1024x421 35KB, saved=26%)
☁️ Uploading to Tencent COS...
   Uploaded: https://bucket.cos.ap-beijing.myqcloud.com/multimodal/2025/12/10/abc123.jpg
🤖 Analyzing image with glm-4.6v...

这张图片展示的是一个命令行界面...
（实时流式输出分析结果）

✅ Done
```

## 成本优化

1. **图片压缩**：自动将图片压缩到合理大小，减少 token 消耗
2. **合理设置 max_tokens**：GLM-4.6V 最大输出 8192 tokens
3. **使用 COS 存储**：相比 base64，URL 方式不占用 token

## 故障排除

### 问题 1：NoSuchBucket 错误

```
Error: Upload failed: 404 Not Found - NoSuchBucket
```

**解决方案**：
1. 检查 Bucket 名称格式是否正确（`bucketname-appid`）
2. 使用 `runCosTest` 工具找到正确的 region
3. 确认 SecretId 和 SecretKey 是否正确

### 问题 2：API 参数错误

```
Error: API request failed: 400 Bad Request - 1210
```

**解决方案**：
1. 确认使用的是 `glm-4.6v` 模型（不是 `glm-4.6`）
2. 检查 max_tokens 是否超过 8192
3. 确认图片 URL 可以公开访问

### 问题 3：图片无法访问

检查 Bucket 是否配置了公共读取权限，或使用 curl 测试：

```bash
curl -I "https://your-image-url"
```

应该返回 `HTTP/1.1 200 OK`

## 进阶用法

### 在代码中使用

```kotlin
import cc.unitmesh.llm.multimodal.MultimodalLLMService
import cc.unitmesh.llm.multimodal.ImageCompressor
import java.io.File

// 创建服务
val service = MultimodalLLMService.createWithCos(
    apiKey = "your-glm-api-key",
    modelName = "glm-4.6v",
    cosSecretId = "your-secret-id",
    cosSecretKey = "your-secret-key",
    cosBucket = "your-bucket-appid",
    cosRegion = "ap-beijing"
)

// 分析图片（流式）
service.streamImageFromFile(
    imageFile = File("/path/to/image.png"),
    prompt = "请描述这张图片的内容",
    compressionConfig = ImageCompressor.Config.BALANCED,
    enableThinking = false
).collect { chunk ->
    print(chunk)  // 实时输出
}

// 关闭服务
service.close()
```

### 不使用 COS（不推荐）

如果不配置 COS，系统会尝试使用 base64 编码，但 GLM-4.6V 可能不支持：

```kotlin
val service = MultimodalLLMService.createWithoutCos(
    apiKey = "your-glm-api-key",
    modelName = "glm-4.6v"
)
```

## 参考资料

- [GLM-4.6V 官方文档](https://docs.bigmodel.cn/cn/guide/models/vlm/glm-4.6v)
- [腾讯云 COS 文档](https://cloud.tencent.com/document/product/436)
- [图片压缩库 Compressor](https://github.com/zetbaitsu/Compressor)

