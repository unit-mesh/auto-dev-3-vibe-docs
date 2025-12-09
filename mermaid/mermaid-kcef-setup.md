# KCEF Setup for Mermaid Rendering

## 为什么需要"下载"（准备）？

根据 [compose-webview-multiplatform 官方文档](https://raw.githubusercontent.com/KevinnZou/compose-webview-multiplatform/refs/heads/main/README.desktop.md)：

> Starting from version 1.9.40, KCEF v2024.04.20.4 is used and it is possible to load JCEF directly from bundled binary.
> 
> **So if you build your app with the JetBrains Runtime JDK, it's no longer required to download the packages.**

### 实际行为说明

即使使用 JBR JDK，首次启动时 KCEF 仍会显示"准备"进度（progress），但这**不是从互联网下载**，而是：
1. 检测 JBR 中的 bundled JCEF
2. 提取/准备必要的文件到工作目录
3. 这个过程通常只需 **几秒钟**（99%→100% 瞬间完成）

如果**不使用** JBR，则需要从互联网下载约 200MB 的 CEF 包（首次需要 5-10 分钟）。

## 两种情况

### 情况 1：使用 JetBrains Runtime (JBR) JDK ✅ 推荐

**不需要从互联网下载**，KCEF 会直接使用 JBR 中 bundled 的 JCEF。
- 首次启动：准备文件（几秒钟）
- 后续启动：瞬间完成

**如何使用 JBR：**

1. **下载 JBR**:
   ```bash
   # macOS
   https://github.com/JetBrains/JetBrainsRuntime/releases
   
   # 或者使用 SDKMAN
   sdk install java 17.0.10-jbr
   ```

2. **配置 Gradle 使用 JBR**:
   
   在项目根目录创建/编辑 `gradle.properties`:
   ```properties
   org.gradle.java.home=/path/to/jbr
   ```
   
   或者使用环境变量：
   ```bash
   export JAVA_HOME=/path/to/jbr
   ./gradlew :mpp-ui:run
   ```

3. **IntelliJ IDEA 自动使用 JBR**:
   - IntelliJ IDEA 自带 JBR
   - 在 IDE 中直接运行，会自动使用 JBR，无需下载

### 情况 2：使用标准 JDK（非 JBR）

**需要从互联网下载** KCEF 包（约 200MB），但只需下载一次：
- 首次运行时从 GitHub/Maven 下载到本地
- 下载时间取决于网速（通常 5-10 分钟）
- 之后运行会直接使用已下载的包
- 下载进度会显示在界面上

## 当前实现

### 应用启动时的 KCEF 初始化

**文件**: `mpp-ui/src/jvmMain/kotlin/cc/unitmesh/devins/ui/Main.kt`

```kotlin
LaunchedEffect(Unit) {
    withContext(Dispatchers.IO) {
        KCEF.init(builder = {
            installDir(File("kcef-bundle"))
            progress {
                onDownloading {
                    kcefDownloading = max(it, 0F)
                    // 显示下载进度
                }
                onInitialized {
                    kcefInitialized = true
                }
            }
            settings {
                cachePath = File("kcef-cache").absolutePath
            }
        })
    }
}
```

### 初始化流程

```
应用启动
    ↓
检查 JDK 类型
    ↓
┌─────────────────────────────────────────┐
│ JBR JDK?                                │
└─────────────┬───────────────────────────┘
              │
    ┌─────────┴──────────┐
    │                    │
   Yes                  No
    │                    │
    ↓                    ↓
使用 bundled JCEF    检查 kcef-bundle/
  (瞬间完成)              │
    │            ┌───────┴────────┐
    │            │                │
    │          存在              不存在
    │            │                │
    │            ↓                ↓
    │        使用已下载        下载 KCEF
    │         的包              (首次)
    │            │                │
    └────────────┴────────────────┘
                 ↓
            应用启动完成
           (WebView 可用)
```

## 检查当前 JDK

```bash
# 查看 Gradle 使用的 JDK
./gradlew -version

# 输出示例（JBR）：
# Java:       17.0.10 (JetBrains Runtime)
# JVM:        17.0.10+7-b1087.23 (JetBrains s.r.o.)

# 输出示例（标准 JDK）：
# Java:       17.0.9 (Oracle Corporation)
# JVM:        17.0.9+11-LTS-201 (Oracle Corporation)
```

## 推荐配置

### 方案 A：全局使用 JBR（推荐开发环境）

```bash
# 1. 下载 JBR
# https://github.com/JetBrains/JetBrainsRuntime/releases

# 2. 设置为默认 JDK
export JAVA_HOME=/path/to/jbr
export PATH=$JAVA_HOME/bin:$PATH

# 3. 验证
java -version
./gradlew -version
```

### 方案 B：仅为项目配置 JBR

在 `~/.gradle/gradle.properties` 或项目根目录的 `gradle.properties`:

```properties
org.gradle.java.home=/path/to/jbr
```

### 方案 C：接受一次性下载（最简单）

- 首次运行时等待下载完成（5-10分钟，取决于网速）
- 之后永久使用已下载的包
- 无需配置

## 生产环境

### 打包分发

使用 Gradle 构建分发包时：

```bash
./gradlew :mpp-ui:packageDmg        # macOS
./gradlew :mpp-ui:packageMsi        # Windows
./gradlew :mpp-ui:packageDeb        # Linux
```

**注意**：
- 使用 JBR 构建：最终应用包含 JBR，用户无需下载
- 使用标准 JDK 构建：需要将 `kcef-bundle/` 包含在分发包中

## 文件结构

```
autocrud/
├── kcef-bundle/           # KCEF/JCEF 二进制文件
│   └── Frameworks/        # CEF 框架
├── kcef-cache/            # WebView 缓存
│   ├── blob_storage/
│   ├── Cache/
│   └── ...
└── mpp-ui/
    └── src/
```

**清理缓存**：
```bash
# 删除 WebView 缓存
rm -rf kcef-cache/

# 删除 KCEF 包（强制重新下载）
rm -rf kcef-bundle/
```

## 故障排查

### 问题 1：下载很慢

**解决方案**：
1. 使用 JBR JDK（无需下载）
2. 手动下载 KCEF 包并解压到 `kcef-bundle/`
3. 使用代理：
   ```bash
   export HTTP_PROXY=http://proxy:port
   export HTTPS_PROXY=http://proxy:port
   ./gradlew :mpp-ui:run
   ```

### 问题 2：下载失败

**解决方案**：
1. 检查网络连接
2. 查看日志：`~/.autodev/logs/autodev-app.log`
3. 使用 JBR JDK

### 问题 3：WebView 不显示

**可能原因**：
- KCEF 初始化失败
- JDK 版本不兼容

**解决方案**：
1. 查看日志确认 KCEF 状态
2. 使用 JDK 17+ 或 JBR 17+
3. 清理缓存重试

## 性能对比

| 方案 | 首次启动时间 | 首次网络下载 | 后续启动 | 应用大小 |
|------|------------|------------|---------|---------|
| JBR JDK | 3-5秒 | **不需要** | 1-2秒 | ~300MB |
| 标准 JDK（已下载） | 1-2秒 | 不需要 | 1-2秒 | ~300MB |
| 标准 JDK（首次） | 5-10分钟 | ~200MB | 1-2秒 | ~300MB |

## 参考链接

- [compose-webview-multiplatform Desktop Setup](https://raw.githubusercontent.com/KevinnZou/compose-webview-multiplatform/refs/heads/main/README.desktop.md)
- [KCEF Documentation](https://github.com/DatL4g/KCEF/blob/master/COMPOSE.md)
- [JetBrains Runtime Downloads](https://github.com/JetBrains/JetBrainsRuntime/releases)

