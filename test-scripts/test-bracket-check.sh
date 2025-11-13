#!/bin/bash
# 检查括号是否匹配
file="/Volumes/source/ai/autocrud/mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/AutoDevApp.kt"
open_count=$(grep -o '{' "$file" | wc -l)
close_count=$(grep -o '}' "$file" | wc -l)
echo "Opening braces: $open_count"
echo "Closing braces: $close_count"
echo "Difference: $((open_count - close_count))"


