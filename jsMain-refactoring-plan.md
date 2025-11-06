# mpp-ui jsMain 改造方案

## 📊 **分析结果总结**

基于依赖分析脚本的结果：

- **TypeScript 代码**：47 个文件，~1473 行代码
- **Kotlin/JS 代码**：5 个文件，~691 行代码
- **主要功能**：TypeScript 实现了完整的 CLI 应用，Kotlin/JS 只是简单的 Web 演示
- **依赖关系**：TypeScript 大量使用 mmp-core（29 处引用）

## 🎯 **推荐方案：拆分架构**

### **原因**
1. **TypeScript 占主导**：1473 行 vs 691 行，CLI 功能完整
2. **功能差异大**：TypeScript = 完整 CLI，Kotlin = 简单 Web 演示
3. **维护负担**：双技术栈增加复杂性
4. **用户需求**：CLI 用户和 Desktop 用户需求不同

## 🚀 **具体实施方案**

### **阶段一：创建独立 CLI 项目**

#### **1. 项目结构**
```
autodev-cli/                        # 新的独立 CLI 项目
├── src/
│   ├── index.ts                    # 主入口（从 index.tsx 迁移）
│   ├── ui/                         # TUI 组件
│   ├── agents/                     # AI 代理逻辑
│   ├── config/                     # 配置管理
│   ├── modes/                      # 模式管理
│   ├── processors/                 # 命令处理器
│   └── utils/                      # 工具函数
├── tests/                          # 测试文件
├── package.json
├── tsconfig.json
├── README.md
└── scripts/
    ├── build.sh
    └── publish.sh
```

#### **2. 迁移步骤**
```bash
# 创建新项目
mkdir autodev-cli
cd autodev-cli

# 复制 TypeScript 源码
cp -r ../mpp-ui/src/jsMain/typescript/* ./src/
cp -r ../mpp-ui/src/jsMain/typescript/__tests__ ./tests/

# 复制配置文件
cp ../mpp-ui/package.json ./package.json.template
cp ../mpp-ui/tsconfig.json ./
```

#### **3. 更新配置**
```json
{
  "name": "@autodev/cli",
  "version": "0.1.4",
  "description": "AutoDev CLI - AI-powered development assistant",
  "type": "module",
  "bin": {
    "autodev": "./dist/index.js"
  },
  "main": "./dist/index.js",
  "scripts": {
    "build": "tsc",
    "dev": "tsc --watch",
    "start": "node dist/index.js",
    "test": "vitest run",
    "clean": "rm -rf dist",
    "prepublish": "npm run build"
  },
  "dependencies": {
    "@autodev/mpp-core": "^0.1.4",
    "@modelcontextprotocol/sdk": "^1.0.4",
    "chalk": "^5.3.0",
    "commander": "^12.1.0",
    "ink": "^5.0.1",
    "react": "^18.3.1"
  }
}
```

### **阶段二：简化 mpp-ui**

#### **1. 移除 jsMain**
```bash
# 备份现有代码
mv mpp-ui/src/jsMain mpp-ui/src/jsMain.backup

# 移除 TypeScript 相关文件
rm mpp-ui/package.json
rm mpp-ui/tsconfig.json
rm -rf mpp-ui/node_modules
rm -rf mpp-ui/dist
```

#### **2. 更新 build.gradle.kts**
```kotlin
// 移除 jsMain 配置
sourceSets {
    val commonMain by getting {
        dependencies {
            implementation(project(":mpp-core"))
            // ... 其他依赖
        }
    }
    
    val jvmMain by getting {
        dependencies {
            implementation(compose.desktop.currentOs)
            implementation("ch.qos.logback:logback-classic:1.5.19")
            // ... 其他 JVM 依赖
        }
    }
    
    // 移除 jsMain 配置块
    // val jsMain by getting { ... }
}
```

#### **3. 更新项目描述**
```kotlin
// build.gradle.kts
description = "AutoDev Desktop Application - Compose Multiplatform UI"
```

### **阶段三：更新构建和发布**

#### **1. CLI 项目构建**
```bash
# autodev-cli/scripts/build.sh
#!/bin/bash
echo "🔨 Building AutoDev CLI..."
npm ci
npm run build
chmod +x dist/index.js
echo "✅ CLI build completed"
```

#### **2. Desktop 项目构建**
```bash
# mpp-ui/scripts/build.sh
#!/bin/bash
echo "🔨 Building AutoDev Desktop..."
./gradlew :mpp-ui:jvmJar
echo "✅ Desktop build completed"
```

#### **3. 独立发布**
```bash
# 发布 CLI
cd autodev-cli
npm publish

# 发布 Desktop
cd ../mpp-ui
./gradlew publish
```

## 📋 **迁移检查清单**

### **CLI 项目（autodev-cli）**
- [ ] 创建项目目录结构
- [ ] 复制 TypeScript 源码
- [ ] 更新 package.json 配置
- [ ] 更新 tsconfig.json 配置
- [ ] 修复导入路径
- [ ] 更新测试配置
- [ ] 测试构建和运行
- [ ] 创建 README.md
- [ ] 设置 CI/CD

### **Desktop 项目（mpp-ui）**
- [ ] 备份 jsMain 代码
- [ ] 移除 jsMain 目录
- [ ] 更新 build.gradle.kts
- [ ] 移除 Node.js 相关文件
- [ ] 测试 JVM 构建
- [ ] 更新项目文档
- [ ] 验证 Desktop 功能

### **文档更新**
- [ ] 更新根目录 README.md
- [ ] 创建 CLI 项目文档
- [ ] 更新 Desktop 项目文档
- [ ] 更新安装指南
- [ ] 更新开发指南

## 🎯 **预期效果**

### **优势**
1. **职责清晰**：CLI 专注命令行，Desktop 专注图形界面
2. **技术栈优化**：TypeScript 适合 CLI，Kotlin 适合 Desktop
3. **维护简化**：独立开发、测试、发布
4. **用户体验**：更专业的工具，更好的用户体验

### **最终架构**
```
autodev/
├── mpp-core/           # 核心逻辑（Kotlin Multiplatform）
├── mpp-ui/             # Desktop 应用（JVM + Compose）
└── autodev-cli/        # CLI 应用（Node.js + TypeScript）
```

这样的架构更清晰、更专业，每个项目都有明确的目标和技术栈。
