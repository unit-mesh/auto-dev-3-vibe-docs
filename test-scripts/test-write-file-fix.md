# WriteFile Tool 转义序列修复测试

## 测试目的
验证 WriteFileTool 是否正确处理转义序列，特别是 `\n` 应该被转换为真正的换行符。

## 测试步骤

### 1. 创建测试目录
```bash
cd /tmp
mkdir write-file-test
cd write-file-test
```

### 2. 使用 CLI 创建包含换行符的文件
```bash
cd /Volumes/source/ai/autocrud/mpp-ui
node dist/index.js
```

### 3. 输入测试命令
在 CLI 中输入：
```
Create a simple Java Hello World file at src/main/java/com/example/HelloWorld.java
```

### 4. 验证文件内容
```bash
# 检查文件是否正确创建
cat src/main/java/com/example/HelloWorld.java

# 应该看到正确的换行符，而不是 \n 字符串
# 正确的输出应该是：
# package com.example;
# 
# public class HelloWorld {
#     public static void main(String[] args) {
#         System.out.println("Hello World!");
#     }
# }

# 错误的输出会是：
# package com.example;\n\npublic class HelloWorld {\n    public static void main(String[] args) {\n        System.out.println("Hello World!");\n    }\n}
```

### 5. 使用 hexdump 验证
```bash
# 使用 hexdump 查看文件的实际字节内容
hexdump -C src/main/java/com/example/HelloWorld.java | head -10

# 应该看到 0a (换行符) 而不是 5c 6e (\n 的 ASCII)
```

## 预期结果
- 文件应该包含真正的换行符 (0x0A)
- 不应该包含字面的 `\n` 字符串
- Java 代码应该正确格式化，每行独立显示

## 修复说明
修复了 `CodingAgent.kt` 中的 `parseAction` 方法：
1. 添加了完整的 `processEscapeSequences` 函数
2. 处理 `\n`, `\r`, `\t`, `\"`, `\'`, `\\` 等转义序列
3. 确保工具参数中的转义序列被正确转换

## 相关文件
- `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/CodingAgent.kt`
- `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/tool/impl/WriteFileTool.kt`
