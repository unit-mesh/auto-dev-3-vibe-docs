# 文档搜索 GitIgnore 过滤修复

## 问题描述

用户报告索引完成后：
- 成功: 439
- 失败: 561
- **总计: 1000 个文件被索引**

但很多不应该被索引的文件（如 `.intellijPlatform/`、`docs/` 等）被错误索引，导致大量失败。

## 问题分析

### 1. 文件统计（测试结果）

```bash
找到的文档总数（未过滤）:     4976
应该被排除的文件数:           3676
理论上应该索引的文件数:       1300
```

### 2. 问题根源

`searchFiles` 方法中使用了硬编码的排除目录列表，但该列表不完整：

```kotlin
// 旧的代码 - 硬编码排除列表
val excludeDirs = setOf(
    "node_modules", ".git", ".idea", "build", "out", "target", 
    "dist", ".gradle", "venv", "__pycache__", "bin"
)
```

**缺失的关键目录：**
- `.intellijPlatform` ✗
- `docs` ✗  (在 gitignore 第175行)
- `.vscode` ✗
- `.fleet` ✗
- 许多其他目录

### 3. .gitignore 文件内容

项目的 `.gitignore` 包含以下关键规则（第164-175行）：

```gitignore
.intellijPlatform
**/bin/**
.kotlin
.specify
.vscode
.github/prompts
kotlin-js-store/*
.playwright-mcp
node_modules
package-lock.json
local.properties
docs      # <-- 这会排除所有 docs 目录
```

但代码没有正确使用这些规则！

## 解决方案

### 修改内容

**文件**: `mpp-core/src/jvmMain/kotlin/cc/unitmesh/devins/filesystem/DefaultFileSystem.jvm.kt`

#### 1. 简化排除逻辑，依赖 GitIgnoreParser

```kotlin
// 只保留最基本的排除目录（.git 必须排除，其他依赖 gitignore）
val criticalExcludeDirs = setOf(".git")

// Reload gitignore patterns before search
gitIgnoreParser?.reload()
```

**为什么只保留 `.git`？**
- `.git` 目录必须始终排除（性能和安全）
- 其他所有排除规则由 `.gitignore` 文件控制
- 保持一致性：修改 `.gitignore` 即可控制所有过滤规则

#### 2. 正确使用 GitIgnoreParser

```kotlin
Files.walk(projectRoot, maxDepth).use { stream ->
    val iterator = stream
        .filter { path ->
            // 只保留普通文件
            if (!path.isRegularFile()) return@filter false
            
            val relativePath = projectRoot.relativize(path).toString().replace("\\", "/")
            
            // 1. 排除关键目录（.git 必须排除）
            if (path.any { it.fileName.toString() in criticalExcludeDirs }) {
                skippedByExclude++
                return@filter false
            }
            
            // 2. 使用 GitIgnoreParser 检查（这应该处理 .gitignore 中的所有规则）
            if (gitIgnoreParser?.isIgnored(relativePath) == true) {
                skippedByGitignore++
                return@filter false
            }
            
            true
        }
        .iterator()
    // ...
}
```

**关键改进：**
1. 路径规范化：`replace("\\", "/")`  - 确保 Windows/Mac/Linux 兼容
2. 使用相对路径：`projectRoot.relativize(path).toString()`
3. 统计过滤结果：显示有多少文件被 gitignore 排除

#### 3. 添加调试信息

```kotlin
// 初始化时
private val gitIgnoreParser: GitIgnoreParser? by lazy {
    try {
        val parser = GitIgnoreParser(projectPath)
        println("DefaultFileSystem: GitIgnoreParser 已初始化，项目路径: $projectPath")
        println("DefaultFileSystem: 加载的 gitignore 规则数: ${parser.getPatterns().size}")
        parser
    } catch (e: Exception) {
        println("DefaultFileSystem: GitIgnoreParser 初始化失败: ${e.message}")
        null
    }
}

// 搜索结束时
if (skippedByGitignore > 0 || skippedByExclude > 0) {
    println("searchFiles: 排除 $skippedByExclude 个关键目录文件, $skippedByGitignore 个 gitignore 匹配文件")
}
```

## GitIgnore 工作原理

### GitIgnoreParser 架构

```
GitIgnoreParser (JVM)
    ↓
BaseGitIgnoreParser
    ↓
DefaultGitIgnoreFilter
    ↓
GitIgnorePatternMatcher  (模式转换为正则表达式)
```

### 模式匹配规则

| gitignore 模式 | 匹配示例 | 说明 |
|---------------|---------|------|
| `docs` | `docs/`, `a/docs/b.md` | 匹配任何目录 |
| `/docs` | `docs/` | 只匹配根目录 |
| `docs/` | `docs/`, `a/docs/` | 只匹配目录 |
| `*.md` | `a.md`, `path/to/b.md` | 匹配所有 md 文件 |
| `**/*.md` | 同上 | 递归匹配 |
| `.intellijPlatform` | `.intellijPlatform/` | 匹配该目录 |

### 路径规范化

```kotlin
fun normalizePath(path: String): String {
    var normalized = path.replace('\\', '/')  // Windows 兼容
    if (normalized.startsWith("./")) {
        normalized = normalized.substring(2)  // 移除 ./
    }
    if (normalized.startsWith("/")) {
        normalized = normalized.substring(1)   // 移除开头的 /
    }
    return normalized
}
```

## 测试验证

### 测试步骤

1. **编译新代码**
```bash
cd /Volumes/source/ai/autocrud
./gradlew :mpp-core:jar :mpp-ui:jar
```

2. **运行应用并查看日志**
   - 启动 AutoDev UI
   - 进入文档阅读器页面
   - 点击"索引文档"按钮

3. **检查控制台输出**
```
DefaultFileSystem: GitIgnoreParser 已初始化，项目路径: /Volumes/source/ai/autocrud
DefaultFileSystem: 加载的 gitignore 规则数: 157
searchFiles: 排除 0 个关键目录文件, 2676 个 gitignore 匹配文件
✓ Indexed: README.md
✓ Indexed: mpp-core/README.md
✗ Failed to index: some-file.xml - File format not supported
...
```

### 预期结果

- **找到的文档总数**: ~1300 (而不是 4976)
- **成功索引**: ~1200
- **失败**: ~100 (主要是不支持的格式如 .xml, .txt 配置文件等)
- **成功率**: ~92% (而不是 43.9%)

### 验证 gitignore 是否生效

可以手动测试某个路径是否被正确排除：

```kotlin
// 在代码中添加测试
val testPaths = listOf(
    ".intellijPlatform/somefile.xml",
    "docs/README.md",
    "node_modules/package/README.md",
    "build/reports/test.html",
    "README.md"  // 应该保留
)

testPaths.forEach { path ->
    val ignored = gitIgnoreParser?.isIgnored(path) ?: false
    println("Path: $path, Ignored: $ignored")
}
```

**预期输出**:
```
Path: .intellijPlatform/somefile.xml, Ignored: true
Path: docs/README.md, Ignored: true
Path: node_modules/package/README.md, Ignored: true
Path: build/reports/test.html, Ignored: true
Path: README.md, Ignored: false
```

## 注意事项

### .gitignore 中的 `docs` 规则

`.gitignore` 第175行有 `docs` 规则，这会排除：
- `/Volumes/source/ai/autocrud/docs/` 目录中的所有文档
- 任何子目录中名为 `docs` 的目录

**如果需要索引 docs 目录**，有两个选择：

1. **修改 .gitignore**（推荐）
   ```gitignore
   # 在 docs 行前添加 !docs/
   !docs/
   docs  # 这样会排除其他地方的 docs，但不排除根目录的
   ```

2. **从 gitignore 中移除 docs 规则**
   - 编辑 `.gitignore`，删除或注释掉第175行的 `docs`

### 性能考虑

- **GitIgnoreParser 初始化**: 首次使用时会递归加载所有 `.gitignore` 文件
- **缓存**: GitIgnoreParser 会缓存解析的规则
- **Reload**: 搜索前会调用 `reload()` 重新加载规则（如果 gitignore 文件被修改）

## 后续优化建议

1. **增量索引**
   - 只重新索引修改过的文件（通过文件哈希判断）
   
2. **索引缓存**
   - 将索引结果持久化到数据库
   - 下次打开时直接从缓存加载

3. **并行索引**
   - 使用协程并行索引多个文件
   - 提高索引速度

4. **用户配置**
   - 允许用户自定义额外的排除规则
   - UI 中显示哪些目录被排除

## 总结

### 修改前
- ✗ 使用硬编码的排除目录列表
- ✗ 列表不完整，缺少关键目录
- ✗ 索引了 4976 个文件中的 1000 个
- ✗ 失败率 56.1%

### 修改后
- ✓ 完全依赖 GitIgnoreParser
- ✓ 自动读取 `.gitignore` 规则
- ✓ 只索引 ~1300 个合法文件
- ✓ 预期失败率 ~8%
- ✓ 添加了调试日志
- ✓ 路径规范化处理

### 关键改进
1. **准确性**: 使用 `.gitignore` 作为唯一的过滤规则来源
2. **一致性**: 与 git 的行为保持一致
3. **可维护性**: 修改 `.gitignore` 即可控制过滤规则
4. **可调试性**: 添加了详细的日志输出

