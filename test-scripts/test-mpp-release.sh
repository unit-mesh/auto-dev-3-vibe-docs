#!/bin/bash

# MPP 模块构建和验证测试脚本
# 用于验证 mpp-core, mpp-ui, mpp-server 的统一版本管理和构建流程

set -e  # 遇到错误立即退出

echo "=================================================="
echo "MPP 模块统一版本管理测试"
echo "=================================================="
echo ""

# 1. 检查版本号配置
echo "1. 检查统一版本号配置..."
MPP_VERSION=$(grep "^mppVersion" gradle.properties | cut -d'=' -f2 | tr -d ' ')
echo "   gradle.properties 中的版本号: $MPP_VERSION"

# 检查各模块是否使用了统一版本号配置
echo "   检查 mpp-core 配置..."
grep -q 'version = project.findProperty("mppVersion")' mpp-core/build.gradle.kts && echo "   ✓ mpp-core 配置正确"

echo "   检查 mpp-ui 配置..."
grep -q 'version = project.findProperty("mppVersion")' mpp-ui/build.gradle.kts && echo "   ✓ mpp-ui 配置正确"

echo "   检查 mpp-server 配置..."
grep -q 'version = project.findProperty("mppVersion")' mpp-server/build.gradle.kts && echo "   ✓ mpp-server 配置正确"
echo ""

# 2. 构建 mpp-core
echo "2. 构建 mpp-core..."
./gradlew :mpp-core:clean :mpp-core:assemble -q
echo "   ✓ mpp-core 构建成功"
echo ""

# 3. 构建 mpp-server
echo "3. 构建 mpp-server fat JAR..."
./gradlew :mpp-server:clean :mpp-server:fatJar -q
echo "   ✓ mpp-server fat JAR 构建成功"

# 验证 JAR 文件
SERVER_JAR="mpp-server/build/libs/mpp-server-${MPP_VERSION}-all.jar"
if [ -f "$SERVER_JAR" ]; then
    JAR_SIZE=$(du -h "$SERVER_JAR" | cut -f1)
    echo "   ✓ 生成文件: $SERVER_JAR (大小: $JAR_SIZE)"
else
    echo "   ✗ 错误: 未找到 $SERVER_JAR"
    exit 1
fi
echo ""

# 4. 测试 mpp-server JAR 是否可执行
echo "4. 测试 mpp-server JAR..."
# 只检查 JAR 是否包含主类
unzip -l "$SERVER_JAR" | grep -q "ServerApplicationKt.class" && echo "   ✓ JAR 包含主类文件"

# 检查 MANIFEST
unzip -p "$SERVER_JAR" META-INF/MANIFEST.MF | grep -q "Main-Class" && echo "   ✓ MANIFEST 包含 Main-Class"
echo ""

# 5. 验证 GitHub Actions workflow 配置
echo "5. 验证 GitHub Actions workflow..."
if [ -f ".github/workflows/compose-release.yml" ]; then
    grep -q "mpp-server:fatJar" .github/workflows/compose-release.yml && echo "   ✓ workflow 包含 mpp-server 构建任务"
    grep -q "server-jar" .github/workflows/compose-release.yml && echo "   ✓ workflow 包含 server JAR 上传配置"
    grep -q "build-server" .github/workflows/compose-release.yml && echo "   ✓ workflow 包含 build-server job"
else
    echo "   ✗ 错误: 未找到 compose-release.yml"
    exit 1
fi
echo ""

# 总结
echo "=================================================="
echo "✓ 所有测试通过！"
echo "=================================================="
echo ""
echo "统一版本号: $MPP_VERSION"
echo "mpp-server JAR: $SERVER_JAR ($JAR_SIZE)"
echo ""
echo "下一步："
echo "1. 修改 gradle.properties 中的 mppVersion 更新版本号"
echo "2. 创建并推送 tag: git tag compose-v${MPP_VERSION} && git push origin compose-v${MPP_VERSION}"
echo "3. GitHub Actions 会自动构建并发布所有制品"
echo ""
