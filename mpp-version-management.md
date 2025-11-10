# MPP 模块统一版本管理

## 概述

本文档说明了 `mpp-core`、`mpp-ui` 和 `mpp-server` 三个模块的统一版本管理策略。

## 版本号位置

所有 MPP 模块的版本号统一在 **根目录的 `gradle.properties`** 文件中定义：

```properties
# MPP Unified Version (mpp-core, mpp-ui, mpp-server)
mppVersion = 0.1.5
```

## 各模块配置

### mpp-core (build.gradle.kts)

```kotlin
version = project.findProperty("mppVersion") as String? ?: "0.1.5"
```

### mpp-ui (build.gradle.kts)

```kotlin
version = project.findProperty("mppVersion") as String? ?: "0.1.5"
```

### mpp-server (build.gradle.kts)

```kotlin
version = project.findProperty("mppVersion") as String? ?: "0.1.5"
```

## 发布流程

### 1. 更新版本号

在 `gradle.properties` 中修改 `mppVersion`：

```properties
mppVersion = 0.2.0
```

### 2. 构建所有模块

```bash
# 构建 mpp-core
./gradlew :mpp-core:assemble

# 构建 mpp-server fat JAR
./gradlew :mpp-server:fatJar

# 构建 mpp-ui (所有平台)
./gradlew :mpp-ui:assembleDebug        # Android Debug APK
./gradlew :mpp-ui:assembleRelease      # Android Release APK
./gradlew :mpp-ui:packageDeb           # Linux .deb
./gradlew :mpp-ui:packageMsi           # Windows .msi
./gradlew :mpp-ui:packageDmg           # macOS .dmg
```

### 3. 验证生成的制品

```bash
# mpp-server
ls -lh mpp-server/build/libs/mpp-server-*-all.jar

# mpp-ui Android
ls -lh mpp-ui/build/outputs/apk/debug/*.apk
ls -lh mpp-ui/build/outputs/apk/release/*.apk

# mpp-ui Desktop
ls -lh mpp-ui/build/compose/binaries/main/deb/*.deb
ls -lh mpp-ui/build/compose/binaries/main/msi/*.msi
ls -lh mpp-ui/build/compose/binaries/main/dmg/*.dmg
```

### 4. 创建 GitHub Release

使用 GitHub Actions 自动发布：

```bash
# 创建并推送 tag
git tag compose-v0.2.0
git push origin compose-v0.2.0
```

或手动触发 workflow。

## mpp-server 使用说明

### 运行 fat JAR

```bash
java -jar mpp-server-0.1.5-all.jar
```

### 环境变量配置

参考 `mpp-server/README.md` 或 `config.yaml` 进行配置。

## GitHub Actions 自动构建

workflow 文件：`.github/workflows/compose-release.yml`

触发条件：
- 推送 `compose-*` tag
- 手动触发（workflow_dispatch）

生成的制品：
- **server-jar**: `mpp-server-*-all.jar` (可执行 fat JAR)
- **android-apks**: Debug 和 Release APK
- **linux-deb**: Linux .deb 安装包
- **windows-msi**: Windows .msi 安装程序
- **macos-dmg**: macOS .dmg 镜像

## 注意事项

1. **版本号同步**：修改 `gradle.properties` 中的 `mppVersion` 后，所有三个模块会自动使用新版本号
2. **后备版本**：如果 `gradle.properties` 中未定义 `mppVersion`，各模块会使用默认值 `0.1.5`
3. **CI/CD 集成**：GitHub Actions 会自动读取 `gradle.properties` 中的版本号并生成对应版本的制品
4. **发布前测试**：建议在本地先运行 `./gradlew build` 确保所有模块都能成功构建

## 版本历史

- **0.1.5** (2024-11-10): 
  - 统一版本管理实施
  - 添加 mpp-server fat JAR 支持
  - 更新 GitHub Actions workflow
