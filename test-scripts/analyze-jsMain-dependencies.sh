#!/bin/bash

# Script to analyze jsMain dependencies and usage
# This helps understand what needs to be migrated

echo "🔍 Analyzing mpp-ui jsMain Dependencies and Usage..."
echo "=================================================="

# Test 1: Check current jsMain structure
echo ""
echo "1. Current jsMain directory structure:"
if [ -d "mpp-ui/src/jsMain" ]; then
    echo "📁 jsMain structure:"
    tree mpp-ui/src/jsMain 2>/dev/null || find mpp-ui/src/jsMain -type f | head -20
else
    echo "❌ jsMain directory not found"
fi

# Test 2: Analyze Kotlin/JS vs TypeScript code
echo ""
echo "2. Code distribution analysis:"
if [ -d "mpp-ui/src/jsMain/kotlin" ]; then
    KOTLIN_FILES=$(find mpp-ui/src/jsMain/kotlin -name "*.kt" | wc -l)
    KOTLIN_LINES=$(find mpp-ui/src/jsMain/kotlin -name "*.kt" -exec wc -l {} + 2>/dev/null | tail -1 | awk '{print $1}' || echo "0")
    echo "   Kotlin files: $KOTLIN_FILES files, ~$KOTLIN_LINES lines"
else
    echo "   Kotlin files: 0 files"
fi

if [ -d "mpp-ui/src/jsMain/typescript" ]; then
    TS_FILES=$(find mpp-ui/src/jsMain/typescript -name "*.ts" -o -name "*.tsx" | wc -l)
    TS_LINES=$(find mpp-ui/src/jsMain/typescript -name "*.ts" -o -name "*.tsx" -exec wc -l {} + 2>/dev/null | tail -1 | awk '{print $1}' || echo "0")
    echo "   TypeScript files: $TS_FILES files, ~$TS_LINES lines"
else
    echo "   TypeScript files: 0 files"
fi

# Test 3: Check package.json dependencies
echo ""
echo "3. Package.json analysis:"
if [ -f "mpp-ui/package.json" ]; then
    echo "   📦 Current dependencies:"
    grep -A 20 '"dependencies"' mpp-ui/package.json | grep -E '^\s*"' | head -10
    
    echo ""
    echo "   🛠️  Dev dependencies:"
    grep -A 10 '"devDependencies"' mpp-ui/package.json | grep -E '^\s*"' | head -5
else
    echo "   ❌ package.json not found"
fi

# Test 4: Check build configuration
echo ""
echo "4. Build configuration analysis:"
echo "   📋 Gradle JS configuration:"
if grep -q "jsMain" mpp-ui/build.gradle.kts; then
    grep -A 5 -B 2 "jsMain" mpp-ui/build.gradle.kts
else
    echo "   No jsMain configuration found in build.gradle.kts"
fi

echo ""
echo "   📋 TypeScript configuration:"
if [ -f "mpp-ui/tsconfig.json" ]; then
    echo "   ✅ tsconfig.json exists"
    grep -E '"target"|"module"|"outDir"|"rootDir"' mpp-ui/tsconfig.json
else
    echo "   ❌ tsconfig.json not found"
fi

# Test 5: Check actual usage patterns
echo ""
echo "5. Usage pattern analysis:"
if [ -d "mpp-ui/src/jsMain/typescript" ]; then
    echo "   🔍 TypeScript imports from mpp-core:"
    grep -r "from.*mpp-core" mpp-ui/src/jsMain/typescript/ | wc -l | xargs echo "     Import statements:"
    
    echo "   🔍 Main entry points:"
    if [ -f "mpp-ui/src/jsMain/typescript/index.tsx" ]; then
        echo "     ✅ CLI entry: index.tsx"
        head -5 mpp-ui/src/jsMain/typescript/index.tsx | grep -E "#!/usr/bin/env|export|import" | head -3
    fi
fi

if [ -d "mpp-ui/src/jsMain/kotlin" ]; then
    echo "   🔍 Kotlin/JS main files:"
    find mpp-ui/src/jsMain/kotlin -name "Main.kt" | while read file; do
        echo "     📄 $file"
        grep -E "fun main|CanvasBasedWindow|AutoDevApp" "$file" | head -2
    done
fi

# Test 6: Check current build outputs
echo ""
echo "6. Build output analysis:"
if [ -d "mpp-ui/dist" ]; then
    echo "   📦 TypeScript build output:"
    ls -la mpp-ui/dist/ | head -5
else
    echo "   📦 No TypeScript build output found"
fi

if [ -d "mpp-ui/build/js" ]; then
    echo "   📦 Kotlin/JS build output:"
    ls -la mpp-ui/build/js/ | head -5
else
    echo "   📦 No Kotlin/JS build output found"
fi

# Test 7: Dependency analysis
echo ""
echo "7. Cross-dependency analysis:"
echo "   🔗 mpp-core dependency in package.json:"
if [ -f "mpp-ui/package.json" ]; then
    grep -E '"@autodev/mpp-core"' mpp-ui/package.json || echo "     Not found"
fi

echo "   🔗 mpp-core usage in TypeScript:"
if [ -d "mpp-ui/src/jsMain/typescript" ]; then
    grep -r "mppCore\|KotlinCC" mpp-ui/src/jsMain/typescript/ | wc -l | xargs echo "     Usage count:"
fi

# Summary
echo ""
echo "📊 Analysis Summary"
echo "=================="
echo "Current architecture appears to be:"

if [ "$TS_LINES" -gt "$KOTLIN_LINES" ]; then
    echo "✅ TypeScript-dominant: $TS_LINES TS lines vs $KOTLIN_LINES Kotlin lines"
    echo "💡 Recommendation: Consider extracting CLI to separate project"
else
    echo "✅ Kotlin-dominant: $KOTLIN_LINES Kotlin lines vs $TS_LINES TS lines"
    echo "💡 Recommendation: Consider migrating to pure Kotlin/JS"
fi

echo ""
echo "🎯 Next steps:"
echo "1. Review the analysis above"
echo "2. Decide on architecture direction"
echo "3. Plan migration strategy"
echo "4. Update documentation"

echo ""
echo "📋 Files to review:"
echo "   - mpp-ui/src/jsMain/typescript/index.tsx (CLI entry)"
echo "   - mpp-ui/src/jsMain/kotlin/cc/unitmesh/devins/ui/Main.kt (Web entry)"
echo "   - mpp-ui/package.json (dependencies)"
echo "   - mpp-ui/build.gradle.kts (build config)"
