#!/bin/bash

# WASM UTF-8 字体支持验证脚本
# 用途：验证字体是否正确下载、构建和部署

set -e

echo "🔍 WASM UTF-8 字体支持验证"
echo "======================================"
echo ""

# 1. 检查字体源文件
echo "📁 1. 检查字体源文件..."
FONT_DIR="mpp-ui/src/commonMain/composeResources/font"
if [ ! -d "$FONT_DIR" ]; then
    echo "❌ 字体目录不存在: $FONT_DIR"
    exit 1
fi

echo "   查找 TTF 字体文件..."
TTF_COUNT=$(find "$FONT_DIR" -name "*.ttf" | wc -l | tr -d ' ')
if [ "$TTF_COUNT" -gt 0 ]; then
    echo "   ✅ 找到 $TTF_COUNT 个 TTF 字体文件："
    find "$FONT_DIR" -name "*.ttf" -exec ls -lh {} \; | awk '{print "      -", $9, "("$5")"}'
else
    echo "   ⚠️  未找到 TTF 字体文件，尝试下载..."
    ./gradlew :mpp-ui:downloadWasmFonts
fi

# 检查是否有 OTF 文件（不应该有）
OTF_COUNT=$(find "$FONT_DIR" -name "*.otf" 2>/dev/null | wc -l | tr -d ' ')
if [ "$OTF_COUNT" -gt 0 ]; then
    echo "   ⚠️  警告：发现 OTF 文件（WASM 不支持）："
    find "$FONT_DIR" -name "*.otf" -exec ls -lh {} \; | awk '{print "      -", $9}'
    echo "   建议删除 OTF 文件并使用 TTF 格式"
fi

echo ""

# 2. 检查 Gradle 任务
echo "📦 2. 检查 Gradle 任务配置..."
if ./gradlew :mpp-ui:tasks --all | grep -q "downloadWasmFonts"; then
    echo "   ✅ downloadWasmFonts 任务已配置"
else
    echo "   ❌ downloadWasmFonts 任务未找到"
    exit 1
fi

echo ""

# 3. 测试编译
echo "🔨 3. 测试 WASM 编译..."
if ./gradlew :mpp-ui:compileKotlinWasmJs --quiet; then
    echo "   ✅ WASM 编译成功"
else
    echo "   ❌ WASM 编译失败"
    exit 1
fi

echo ""

# 4. 检查生成的资源类
echo "🔧 4. 检查生成的资源类..."
RES_FILE="mpp-ui/build/generated/compose/resourceGenerator/kotlin/commonMain/autodev_intellij/mpp_ui/generated/resources/Res.kt"
if [ -f "$RES_FILE" ]; then
    echo "   ✅ Res.kt 已生成"
    
    # 检查字体资源是否注册
    if grep -q "NotoSansSC_Regular" "$RES_FILE"; then
        echo "   ✅ NotoSansSC_Regular 已注册"
    else
        echo "   ⚠️  NotoSansSC_Regular 未在 Res.kt 中找到"
    fi
    
    if grep -q "NotoColorEmoji" "$RES_FILE"; then
        echo "   ✅ NotoColorEmoji 已注册"
    else
        echo "   ⚠️  NotoColorEmoji 未在 Res.kt 中找到"
    fi
else
    echo "   ❌ Res.kt 未生成"
    exit 1
fi

echo ""

# 5. 检查 Main.kt 实现
echo "📝 5. 检查 Main.kt 字体加载实现..."
MAIN_FILE="mpp-ui/src/wasmJsMain/kotlin/Main.kt"
if grep -q "preloadFont" "$MAIN_FILE"; then
    echo "   ✅ preloadFont 已实现"
else
    echo "   ❌ preloadFont 未实现"
    exit 1
fi

if grep -q "fontFamilyResolver.preload" "$MAIN_FILE"; then
    echo "   ✅ fontFamilyResolver.preload 已实现"
else
    echo "   ❌ fontFamilyResolver.preload 未实现"
    exit 1
fi

echo ""

# 6. 构建完整分发版（可选，耗时较长）
echo "📦 6. 构建完整分发版（可选）..."
read -p "   是否构建完整分发版？这可能需要几分钟。(y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    if ./gradlew :mpp-ui:wasmJsBrowserDistribution; then
        echo "   ✅ 分发版构建成功"
        
        # 检查输出目录中的字体
        DIST_DIR="mpp-ui/build/dist/wasmJs/productionExecutable"
        if [ -d "$DIST_DIR/composeResources/autodev_intellij.mpp_ui.generated.resources/font" ]; then
            echo "   ✅ 字体文件已包含在分发版中："
            ls -lh "$DIST_DIR/composeResources/autodev_intellij.mpp_ui.generated.resources/font/" | tail -n +2 | awk '{print "      -", $9, "("$5")"}'
        else
            echo "   ❌ 字体文件未包含在分发版中"
            exit 1
        fi
    else
        echo "   ❌ 分发版构建失败"
        exit 1
    fi
else
    echo "   ⏭️  跳过分发版构建"
fi

echo ""

# 7. 检查 GitHub Actions
echo "🚀 7. 检查 GitHub Actions 配置..."
WORKFLOW_FILE=".github/workflows/deploy-wasm.yml"
if grep -q "downloadWasmFonts" "$WORKFLOW_FILE"; then
    echo "   ✅ GitHub Actions 已配置字体下载"
else
    echo "   ⚠️  GitHub Actions 未配置字体下载"
fi

echo ""

# 8. 检查 .gitignore
echo "🔒 8. 检查 .gitignore 配置..."
if grep -q "composeResources/font" .gitignore; then
    echo "   ✅ .gitignore 已配置忽略字体文件"
else
    echo "   ⚠️  .gitignore 未配置忽略字体文件"
fi

echo ""
echo "======================================"
echo "✅ WASM UTF-8 字体支持验证完成！"
echo ""
echo "📚 下一步："
echo "   1. 运行开发服务器测试："
echo "      cd mpp-ui/build/dist/wasmJs/productionExecutable"
echo "      python3 -m http.server 8080"
echo ""
echo "   2. 在浏览器中打开 http://localhost:8080"
echo ""
echo "   3. 测试 UTF-8 字符显示："
echo "      - 中文：你好世界"
echo "      - Emoji：😀 🎉 ✅"
echo "      - 日文：こんにちは"
echo "      - 韩文：안녕하세요"
echo ""

