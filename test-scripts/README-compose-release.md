# Compose Release Test Guide

这个文档说明如何使用 mpp-ui 的发布测试系统。

## 概述

我们创建了一个 GitHub Action 工作流来自动构建和发布 mpp-ui 的 Android 和 Desktop 版本。该系统支持：

- **Android**: Debug 和 Release APK
- **Desktop**: Linux (.deb), Windows (.msi), macOS (.dmg)

## 文件结构

```
.github/workflows/compose-release-test.yml  # GitHub Action 工作流
docs/test-scripts/compose-release-test.sh   # 本地测试脚本
docs/test-scripts/README-compose-release.md # 本文档
```

## 本地测试

在推送标签触发 GitHub Action 之前，建议先在本地测试构建过程：

```bash
# 运行本地测试脚本
./docs/test-scripts/compose-release-test.sh
```

该脚本会：
1. 清理之前的构建
2. 构建 mpp-core 依赖
3. 构建 Android Debug 和 Release APK
4. 根据当前平台构建对应的 Desktop 包
5. 显示构建结果摘要

## 触发 GitHub Action

### 方法 1: 通过标签触发（推荐）

创建以 `compose-` 开头的标签：

```bash
# 创建标签
git tag compose-v1.0.0-test

# 推送标签到远程仓库
git push origin compose-v1.0.0-test
```

### 方法 2: 手动触发

1. 访问 GitHub 仓库的 Actions 页面
2. 选择 "Compose Release Test" 工作流
3. 点击 "Run workflow"
4. 输入版本号（如 1.0.0-test）
5. 点击 "Run workflow"

## 构建产物

成功构建后，会生成以下产物：

### Android APKs
- `mpp-ui-debug.apk` - Debug 版本
- `mpp-ui-release-unsigned.apk` - Release 版本（未签名）

### Desktop 包
- **Linux**: `autodev-desktop_1.0.0-1_amd64.deb`
- **Windows**: `AutoDev Desktop-1.0.0.msi`
- **macOS**: `AutoDev Desktop-1.0.0.dmg`

## 发布流程

1. **本地测试**: 运行 `./docs/test-scripts/compose-release-test.sh`
2. **创建标签**: `git tag compose-v1.0.0-test`
3. **推送标签**: `git push origin compose-v1.0.0-test`
4. **监控构建**: 在 GitHub Actions 页面查看构建状态
5. **下载产物**: 从 Release 页面下载构建的包

## 注意事项

### 构建要求
- Java 17
- Android SDK（用于 Android 构建）
- 各平台的原生工具（用于 Desktop 包）

### 标签命名
- 必须以 `compose-` 开头
- 建议格式：`compose-v<version>-<suffix>`
- 例如：`compose-v1.0.0-test`, `compose-v1.0.0-beta`

### 发布类型
- 所有通过标签触发的构建都会创建 **预发布版本**
- 如需正式发布，需要在 GitHub Release 页面手动编辑

## 故障排除

### 常见问题

1. **mpp-core 构建失败**
   - 确保 mpp-core 模块没有编译错误
   - 检查依赖版本兼容性

2. **Android 构建失败**
   - 检查 Android SDK 配置
   - 确保 compileSdk 和 targetSdk 版本正确

3. **Desktop 构建失败**
   - 检查 JDK 版本（需要 Java 17）
   - 确保平台特定的构建工具已安装

### 调试步骤

1. 查看 GitHub Actions 日志
2. 在本地运行测试脚本
3. 检查 Gradle 构建配置
4. 验证依赖项版本

## 配置自定义

如需修改构建配置，可以编辑：
- `.github/workflows/compose-release-test.yml` - GitHub Action 配置
- `mpp-ui/build.gradle.kts` - Gradle 构建配置
- `docs/test-scripts/compose-release-test.sh` - 本地测试脚本
