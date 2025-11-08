# JSON 场景测试 - 功能总结

## 🎯 新增功能

为 AutoDev 测试框架添加了 JSON 场景支持，允许使用声明式配置文件定义复杂的测试场景。

## 📦 新增文件

### 核心组件

1. **JsonScenarioLoader** (`mpp-ui/src/test/framework/loaders/JsonScenarioLoader.ts`)
   - 从 JSON 文件加载测试场景
   - 验证配置有效性
   - 转换为 TestCase 对象

2. **场景生成器** (`mpp-ui/scripts/generate-test-scenario.js`)
   - 根据需求描述自动生成 JSON 配置
   - 支持命令行和交互式模式
   - 智能推断工具调用和文件变更

3. **场景验证器** (`mpp-ui/scripts/validate-scenarios.js`)
   - 验证 JSON 格式和配置
   - 检查必需字段和值有效性
   - 验证正则表达式

4. **测试运行器** (`mpp-ui/src/test/integration-v2/json-scenarios.test.ts`)
   - 加载和运行 JSON 场景
   - 验证测试结果
   - 生成详细报告

5. **GitHub Actions** (`.github/workflows/json-scenario-tests.yml`)
   - 自动验证 JSON 配置
   - 运行场景测试
   - 生成测试报告

### 示例场景

1. **spring-ai-deepseek.json** - Spring AI DeepSeek 集成
2. **complex-multi-tool.json** - 复杂多工具调用场景
3. **business-scenario-add-redis-cache-001.json** - Redis 缓存集成（示例）

### 文档

1. **JSON 场景加载器文档** (`mpp-ui/src/test/framework/loaders/README.md`)
2. **场景目录 README** (`mpp-ui/src/test/integration-v2/scenarios/README.md`)
3. **完整指南** (`docs/json-scenario-testing-guide.md`)

## 🚀 使用方法

### 1. 生成场景

```bash
# 快速生成
npm run generate:scenario -- "Add Spring AI with DeepSeek to project"

# 交互式模式
npm run generate:scenario:interactive
```

### 2. 验证配置

```bash
npm run validate:scenarios
```

### 3. 运行测试

```bash
# 本地运行
npm run test:json-scenarios

# 或使用别名
npm run test:scenarios

# 保留测试项目（调试）
KEEP_TEST_PROJECTS=true npm run test:scenarios

# 详细日志
DEBUG=true npm run test:scenarios
```

### 4. GitHub Actions

- 自动在 Push/PR 时运行
- 手动触发支持参数配置
- 自动生成测试报告

## 📋 NPM Scripts

新增的 npm scripts：

```json
{
  "test:json-scenarios": "运行 JSON 场景测试",
  "test:scenarios": "test:json-scenarios 的别名",
  "generate:scenario": "生成测试场景（命令行模式）",
  "generate:scenario:interactive": "生成测试场景（交互式模式）",
  "validate:scenarios": "验证所有 JSON 场景配置"
}
```

## 🎨 JSON 配置格式

### 基本结构

```json
{
  "id": "unique-test-id",
  "name": "测试场景名称",
  "description": "详细描述",
  "category": "business-scenario",
  "task": {
    "description": "任务描述",
    "context": "上下文信息",
    "documentation": ["https://docs.example.com"]
  },
  "project": {
    "type": "gradle-spring-boot"
  },
  "expectedTools": [
    {
      "tool": "read-file",
      "required": true,
      "minCalls": 1,
      "maxCalls": 10,
      "order": 1,
      "description": "工具说明"
    }
  ],
  "expectedChanges": [
    {
      "type": "file-created",
      "pattern": ".*Service\\.java",
      "required": true,
      "description": "变更说明"
    }
  ],
  "quality": {
    "minToolAccuracy": 0.7,
    "maxExecutionTime": 600000,
    "minTaskCompletion": 0.8,
    "maxCodeIssues": 3
  },
  "config": {
    "timeout": 600000,
    "maxIterations": 15,
    "retryCount": 1
  }
}
```

## 🔧 生成器智能推断

生成器会根据需求描述自动推断：

### 工具调用

- "add", "create", "implement" → `write-file`
- "update", "modify", "change" → `edit-file`
- "build", "test", "run" → `shell`
- 有文档链接 → `web-fetch`
- "explore", "find", "search" → `glob`

### 文件变更

- "dependency" → 修改 build.gradle.kts/pom.xml
- "service" → 创建 *Service.java
- "controller" → 创建 *Controller.java
- "entity", "model" → 创建实体类
- "config" → 创建 *Config.java

## ✅ 验证检查

验证器会检查：

- ✅ JSON 格式正确性
- ✅ 必需字段（id, name, description, category, task, project）
- ✅ 类别有效性（5 种类别）
- ✅ 项目类型有效性（4 种类型）
- ✅ 工具名称有效性（7 种工具）
- ✅ 变更类型有效性（4 种类型）
- ✅ 正则表达式有效性
- ✅ 数值范围合理性
- ✅ minCalls <= maxCalls

## 🤖 GitHub Actions 工作流

### 触发条件

- Push 到 master/main/develop 分支
- Pull Request
- 手动触发（支持参数）

### 工作流程

1. **validate-scenarios**: 验证 JSON 配置
2. **run-json-scenarios**: 运行测试
3. **report-results**: 生成报告

### Artifacts

- 测试结果（保留 7 天）
- 测试项目（可选，保留 3 天）

## 📊 测试结果

测试会验证：

- ✅ 工具调用（准确性、顺序、参数）
- ✅ 文件变更（创建、修改、删除）
- ✅ 代码质量（问题数量、类型）
- ✅ 任务完成度
- ✅ 综合得分

## 💡 优势

### vs 编程式定义

| 特性 | JSON 配置 | 编程式 |
|------|----------|--------|
| 易读性 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| 易维护 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| 非开发人员 | ✅ | ❌ |
| 版本控制 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 自动生成 | ✅ | ❌ |
| 灵活性 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |

## 🎯 适用场景

JSON 配置特别适合：

- ✅ 多工具调用场景
- ✅ 复杂文件变更验证
- ✅ 需要引用外部文档
- ✅ CI/CD 自动化测试
- ✅ 快速创建大量测试用例

编程式定义适合：

- ✅ 需要复杂逻辑
- ✅ 动态生成测试数据
- ✅ 自定义验证器
- ✅ 高度定制化场景

## 📚 文档链接

- [完整指南](./json-scenario-testing-guide.md)
- [JSON 加载器文档](../mpp-ui/src/test/framework/loaders/README.md)
- [场景目录 README](../mpp-ui/src/test/integration-v2/scenarios/README.md)

## 🔄 下一步

可以考虑的增强功能：

1. **AI 增强生成器**
   - 集成 LLM API
   - 更智能的场景生成
   - 自动优化配置

2. **场景模板库**
   - 预定义常见场景模板
   - 一键生成标准场景

3. **可视化编辑器**
   - Web UI 编辑场景
   - 拖拽式配置
   - 实时预览

4. **测试报告增强**
   - HTML 报告
   - 趋势分析
   - 性能对比

5. **场景共享**
   - 场景市场
   - 社区贡献
   - 评分和评论

