# Compose Release Test Guide

这个文档说明如何使用 mpp-ui 的发布测试系统。

## 快速开始

想要快速创建一个测试发布？只需运行：

```bash
# 1. 本地测试构建
./docs/test-scripts/compose-release-test.sh

# 2. 创建发布（如果测试通过）
./docs/test-scripts/create-compose-release.sh 1.0.0-test
```

就这么简单！GitHub Actions 会自动构建 Android APK 和 Desktop 包。

## 概述

我们创建了一个 GitHub Action 工作流来自动构建和发布 mpp-ui 的 Android 和 Desktop 版本。该系统支持：

- **Android**: Debug 和 Release APK
- **Desktop**: Linux (.deb), Windows (.msi), macOS (.dmg)

## 文件结构

```
.github/workflows/compose-release-test.yml    # GitHub Action 工作流
docs/test-scripts/compose-release-test.sh     # 本地测试脚本
docs/test-scripts/create-compose-release.sh   # 发布创建脚本
docs/test-scripts/test-workflow-isolation.sh  # 工作流隔离测试
docs/test-scripts/README-compose-release.md   # 本文档
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

### 方法 1: 使用发布脚本（推荐）

使用提供的脚本自动创建发布：

```bash
# 交互式创建发布
./docs/test-scripts/create-compose-release.sh

# 或直接指定版本
./docs/test-scripts/create-compose-release.sh 1.0.0-test
```

该脚本会：
1. 验证版本格式
2. 检查是否有未提交的更改
3. 可选运行本地构建测试
4. 创建并推送标签
5. 提供监控链接

### 方法 2: 手动创建标签

创建以 `compose-` 开头的标签：

```bash
# 创建标签
git tag compose-v1.0.0-test

# 推送标签到远程仓库
git push origin compose-v1.0.0-test
```

### 方法 3: 手动触发

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

### 工作流隔离
- `compose-*` 标签只触发 compose 发布工作流
- 其他标签只触发主要的 IntelliJ 插件发布工作流
- 两个工作流完全隔离，不会相互干扰

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

4. **工作流冲突**
   - 如果主要的 release.yml 被意外触发，检查标签是否以 `compose-` 开头
   - 运行 `./docs/test-scripts/test-workflow-isolation.sh` 验证工作流隔离

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
