# MPP 统一版本管理和发布系统

## 📋 概述

本次更新实现了 `mpp-core`、`mpp-ui` 和 `mpp-server` 三个模块的统一版本管理，并为 `mpp-server` 添加了 fat JAR 打包支持，同时更新了 GitHub Actions workflow 以自动构建和发布所有制品。

## ✨ 主要更新

### 1. 统一版本管理

- **位置**: `gradle.properties`
- **配置项**: `mppVersion = 0.1.5`
- **影响模块**: 
  - `mpp-core/build.gradle.kts`
  - `mpp-ui/build.gradle.kts`
  - `mpp-server/build.gradle.kts`

所有模块现在都从 `gradle.properties` 读取统一的版本号，只需修改一处即可更新所有模块的版本。

### 2. mpp-server Fat JAR 支持

- **构建命令**: `./gradlew :mpp-server:fatJar`
- **输出文件**: `mpp-server/build/libs/mpp-server-{version}-all.jar`
- **大小**: ~46MB (包含所有依赖)
- **运行方式**: `java -jar mpp-server-0.1.5-all.jar`

fat JAR 包含了运行 mpp-server 所需的所有依赖，可以直接部署和运行。

### 3. GitHub Actions 更新

更新了 `.github/workflows/compose-release.yml`，新增：

- **build-server job**: 构建 mpp-server fat JAR
- **制品上传**: 自动上传 server JAR 到 release
- **依赖关系**: create-release job 依赖所有构建 jobs

## 📦 发布流程

### 方式一：自动发布（推荐）

1. 修改版本号：

```bash
# 编辑 gradle.properties
mppVersion = 0.2.0
```

2. 提交并推送：

```bash
git add gradle.properties
git commit -m "Bump version to 0.2.0"
git push origin master
```

3. 创建并推送 tag：

```bash
git tag compose-v0.2.0
git push origin compose-v0.2.0
```

4. GitHub Actions 会自动构建并创建包含所有制品的 release

### 方式二：手动触发

1. 在 GitHub 仓库页面，进入 **Actions** 标签
2. 选择 **MPP Release** workflow
3. 点击 **Run workflow**
4. 输入版本号（如 `1.0.0-test`）并运行

## 🚀 本地构建和测试

### 快速测试脚本

```bash
./docs/test-scripts/test-mpp-release.sh
```

这个脚本会：
- ✅ 验证版本号配置
- ✅ 构建 mpp-core
- ✅ 构建 mpp-server fat JAR
- ✅ 验证 JAR 文件
- ✅ 检查 GitHub Actions 配置

### 单独构建各模块

```bash
# mpp-core
./gradlew :mpp-core:assemble

# mpp-server fat JAR
./gradlew :mpp-server:fatJar

# mpp-ui Android
./gradlew :mpp-ui:assembleDebug
./gradlew :mpp-ui:assembleRelease

# mpp-ui Desktop
./gradlew :mpp-ui:packageDeb    # Linux
./gradlew :mpp-ui:packageMsi    # Windows
./gradlew :mpp-ui:packageDmg    # macOS
```

### 验证生成的制品

```bash
# mpp-server JAR
ls -lh mpp-server/build/libs/mpp-server-*-all.jar

# 测试运行
java -jar mpp-server/build/libs/mpp-server-0.1.5-all.jar
```

## 📁 文件清单

### 修改的文件

1. **gradle.properties** - 添加 `mppVersion`
2. **mpp-core/build.gradle.kts** - 使用统一版本号
3. **mpp-ui/build.gradle.kts** - 使用统一版本号
4. **mpp-server/build.gradle.kts** - 使用统一版本号 + fat JAR 配置
5. **.github/workflows/compose-release.yml** - 添加 mpp-server 构建

### 新增的文件

1. **docs/mpp-version-management.md** - 版本管理文档
2. **docs/test-scripts/test-mpp-release.sh** - 自动化测试脚本
3. **docs/RELEASE-SUMMARY.md** - 本文档

## 🎯 Release 制品

每次发布会生成以下制品：

| 制品名称 | 文件 | 说明 |
|---------|------|------|
| **server-jar** | `mpp-server-{version}-all.jar` | 可执行 fat JAR |
| **android-apks** | `*.apk` | Android Debug & Release APK |
| **linux-deb** | `*.deb` | Linux Debian 安装包 |
| **windows-msi** | `*.msi` | Windows 安装程序 |
| **macos-dmg** | `*.dmg` | macOS 磁盘镜像 |

## 📝 注意事项

1. **版本号格式**: 遵循语义化版本 (SemVer)，如 `0.1.5`, `1.0.0`, `1.2.3-beta`
2. **Tag 命名**: 必须以 `compose-` 开头，如 `compose-v0.1.5`
3. **构建时间**: 完整构建所有平台制品大约需要 15-20 分钟
4. **测试建议**: 发布前先在本地运行测试脚本确保构建成功

## 🔗 相关文档

- [版本管理详细文档](./mpp-version-management.md)
- [GitHub Actions Workflow](./.github/workflows/compose-release.yml)
- [mpp-server README](./mpp-server/README.md)

## 📊 当前版本信息

- **当前版本**: 0.1.5
- **最后更新**: 2024-11-10
- **状态**: ✅ 所有测试通过

---

**下一步行动**:
1. ✅ 统一版本管理已实现
2. ✅ mpp-server fat JAR 已配置
3. ✅ GitHub Actions 已更新
4. ⏭️ 准备好创建下一个 release
