# Linter 模块重构总结

## 重构目标
将 @linter 模块的核心代码提取到新的 `mpp-linter` 模块，方便未来复用。

## 重构结果

### 新增模块：mpp-linter

创建了一个全新的 Kotlin Multiplatform 模块 `mpp-linter`，包含 Linter 的核心接口和数据类。

#### 模块结构
```
mpp-linter/
├── build.gradle.kts
└── src/
    └── commonMain/kotlin/cc/unitmesh/linter/
        └── Linter.kt  # 核心接口和数据类
```

#### 包含的核心 API

1. **LintIssue** - 表示单个代码检查问题
2. **LintSeverity** - 问题严重程度（ERROR, WARNING, INFO）
3. **LintResult** - Linter 执行结果
4. **Linter** - Linter 基础接口
5. **LinterSummary** - Linter 可用性总结
6. **LinterAvailability** - Linter 可用性信息

### 模块依赖关系

```
mpp-core  →  依赖  →  mpp-linter
```

- `mpp-linter`：提供核心接口，无外部依赖（除 Kotlin 标准库和协程）
- `mpp-core`：包含具体的 Linter 实现，依赖 `mpp-linter`

### 构建配置更新

1. **settings.gradle.kts**
   - 添加了 `include("mpp-linter")`

2. **build.gradle.kts（根项目）**
   - 排除 `mpp-linter` 模块以使用其独立配置

3. **mpp-core/build.gradle.kts**
   - 添加了对 `mpp-linter` 的依赖：`implementation(project(":mpp-linter"))`

### 支持的平台

mpp-linter 支持以下平台：
- JVM
- JS (Browser & Node.js)
- Android
- iOS (arm64, x64, simulatorArm64)
- WebAssembly (WasmJS)

### 构建验证

✅ mpp-linter 模块构建成功
- 所有平台目标编译通过
- 测试通过
- TypeScript 定义生成成功

### 使用示例

```kotlin
// 在其他项目中使用 mpp-linter
dependencies {
    implementation("cc.unitmesh:mpp-linter:0.1.5")
}

// 实现自定义 Linter
class MyCustomLinter : Linter {
    override val name = "my-linter"
    override val description = "My custom linter"
    override val supportedExtensions = listOf("txt")
    
    override suspend fun isAvailable() = true
    
    override suspend fun lintFile(filePath: String, projectPath: String): LintResult {
        // 实现具体的 lint 逻辑
        return LintResult(
            filePath = filePath,
            issues = emptyList(),
            success = true,
            linterName = name
        )
    }
    
    override fun getInstallationInstructions() = "Install my-linter"
}
```

### 未来改进

1. 可以添加更多平台特定的实现
2. 可以扩展接口支持更多 lint 场景
3. 可以添加 Linter 插件系统

### 注意事项

- 具体的 Linter 实现（如 RuffLinter, BiomeLinter 等）仍保留在 `mpp-core` 中
- `AILinter`、`ShellBasedLinter` 等依赖 `mpp-core` 基础设施的类保留在 `mpp-core`
- mpp-linter 仅提供核心接口，保持最小依赖

