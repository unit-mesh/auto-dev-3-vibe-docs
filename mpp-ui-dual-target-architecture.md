# MPP-UI Dual Target 架构设计

## 概述

mpp-ui 现在支持两个独立的 JavaScript 目标：
- **Node.js CLI** - 终端命令行工具（使用 Ink/React）
- **Web UI** - 浏览器 Web 应用（使用 React）

通过使用 Kotlin Multiplatform 的 **hierarchical source sets**，我们实现了代码共享的同时保持平台特定代码的分离。

## 架构图

```
mpp-ui/
├── src/
│   ├── commonMain/           # Compose UI (JVM/Android 共享)
│   │   └── kotlin/
│   ├── jsMain/               # JS 平台共享代码
│   │   ├── kotlin/           # Kotlin/JS 共享代码
│   │   ├── typescript/       # TypeScript 共享代码
│   │   └── resources/
│   ├── jsNodeMain/ ✨        # Node.js CLI 特定
│   │   ├── kotlin/           # Node.js 特定 Kotlin 代码
│   │   └── typescript/       # CLI 入口点
│   │       └── index.tsx     # Node.js CLI 主入口
│   └── jsBrowserMain/ ✨     # Web UI 特定
│       ├── kotlin/           # Browser 特定 Kotlin 代码
│       ├── typescript/       # Web UI 代码
│       │   ├── index.tsx     # Web 主入口
│       │   └── App.tsx       # Web App 组件
│       └── resources/
│           └── index.html    # Web HTML 模板
```

## Source Sets 层级关系

```
                    commonMain
                        ↓
                     jsMain (共享)
                        ↓
         ┌──────────────┴──────────────┐
         ↓                             ↓
    jsNodeMain                   jsBrowserMain
   (Node.js CLI)                   (Web UI)
```

## 关键区别

### jsNodeMain (Node.js CLI)

**特点**：
- ✅ 使用 Node.js API（`fs`, `path`, `os`, `child_process`）
- ✅ Ink/React 终端 UI
- ✅ 完整的文件系统访问
- ✅ 本地配置文件管理
- ✅ MCP SDK 集成

**依赖**：
- `ink` - 终端 UI 框架
- `commander` - CLI 参数解析
- `chalk` - 终端颜色
- `@modelcontextprotocol/sdk` - MCP 协议

**入口点**：
```typescript
// src/jsNodeMain/typescript/index.tsx
#!/usr/bin/env node
export * from '../../jsMain/typescript/index.js';
```

### jsBrowserMain (Web UI)

**特点**：
- ✅ 纯浏览器环境（无 Node.js API）
- ✅ React Web UI
- ✅ LocalStorage/IndexedDB 存储
- ✅ Fetch API 网络请求
- ✅ 轻量级（~150KB gzipped）

**依赖**：
- `react` - UI 框架
- `react-dom` - DOM 渲染
- `@autodev/mpp-core` - 核心逻辑（浏览器兼容部分）

**入口点**：
```typescript
// src/jsBrowserMain/typescript/index.tsx
import { createRoot } from 'react-dom/client';
createRoot(document.getElementById('root')).render(<App />);
```

## Gradle 配置

### build.gradle.kts

```kotlin
js(IR) {
    // 同时支持 browser 和 nodejs
    browser {
        commonWebpackConfig {
            outputFileName = "mpp-ui-web.js"
        }
    }
    nodejs {
        // Configure Node.js target for CLI
    }
    binaries.executable()
}

sourceSets {
    val jsMain by getting {
        dependencies {
            // JS 共享依赖
            implementation(compose.html.core)
        }
    }

    // Node.js 特定 source set
    val jsNodeMain by creating {
        dependsOn(jsMain)
        dependencies {
            // Node.js specific dependencies
        }
    }

    // Browser 特定 source set
    val jsBrowserMain by creating {
        dependsOn(jsMain)
        dependencies {
            // Browser specific dependencies
        }
    }
}
```

## NPM Scripts

### package.json

```json
{
  "scripts": {
    "build:kotlin": "cd .. && ./gradlew :mpp-core:assembleJsPackage",
    "build:ts:cli": "cd .. && ./gradlew :mpp-ui:assemble && tsc -p tsconfig.cli.json && chmod +x dist/jsMain/typescript/index.js",
    "build:ts:web": "cd .. && ./gradlew :mpp-ui:jsBrowserProductionWebpack",
    "build": "npm run build:kotlin && npm run build:ts:cli",
    "build:web": "npm run build:kotlin && npm run build:ts:web",
    "start": "node dist/jsMain/typescript/index.js",
    "dev:web": "cd .. && ./gradlew :mpp-ui:jsBrowserDevelopmentRun"
  }
}
```

## TypeScript 配置

### tsconfig.cli.json (Node.js CLI)

```json
{
  "extends": "./tsconfig.json",
  "compilerOptions": {
    "types": ["node"]
  },
  "include": [
    "src/jsMain/typescript/**/*",
    "src/jsNodeMain/typescript/**/*"
  ],
  "exclude": [
    "src/jsBrowserMain/**"
  ]
}
```

### tsconfig.web.json (Web UI)

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "lib": ["ES2022", "DOM", "DOM.Iterable"],
    "jsx": "react",
    "types": []
  },
  "include": [
    "src/jsBrowserMain/typescript/**/*"
  ],
  "exclude": [
    "src/jsMain/**",
    "src/jsNodeMain/**"
  ]
}
```

## 构建流程

### CLI 构建

```bash
# 1. 编译 mpp-core
npm run build:kotlin

# 2. 编译 mpp-ui (Kotlin + TypeScript CLI)
npm run build:ts:cli

# 3. 运行 CLI
npm start
```

**输出**：
- `dist/jsMain/typescript/` - TypeScript 编译产物
- Node.js 可执行文件

### Web 构建

```bash
# 1. 编译 mpp-core
npm run build:kotlin

# 2. 编译 Web UI (Kotlin + TypeScript + Webpack)
npm run build:ts:web

# 3. 输出在 mpp-ui/build/kotlin-webpack/js/productionExecutable/
```

**输出**：
- `mpp-ui-web.js` - Webpack 打包的 JS bundle
- `index.html` - HTML 页面
- 体积约 200-500KB（取决于优化）

## 性能对比

| 构建目标 | 编译时间 | 输出大小 | Node.js API | 适用场景 |
|---------|---------|---------|-------------|---------|
| Node.js CLI | ~3秒 | N/A | ✅ | 本地开发工具 |
| Web UI | ~3-5秒 | ~200-500KB | ❌ | 浏览器访问 |
| ~~旧方案（Compose Web）~~ | ~~11分钟~~ | ~~5MB+~~ | ❌ | ~~废弃~~ |

## 代码共享策略

### 可共享的代码（放在 jsMain）

1. **业务逻辑**
   - AI Agent 核心逻辑
   - DevIns 编译器
   - LLM 服务调用

2. **UI 组件**（平台无关）
   - Message 数据模型
   - Markdown 渲染逻辑
   - 代码高亮工具

3. **工具函数**
   - 字符串处理
   - 日期格式化
   - 数据转换

### 平台特定代码

#### jsNodeMain

```typescript
// Node.js 特定功能
import * as fs from 'fs';
import * as path from 'path';
import * as os from 'os';

// 配置文件管理
const configPath = path.join(os.homedir(), '.autodev', 'config.yaml');
const config = fs.readFileSync(configPath, 'utf-8');
```

#### jsBrowserMain

```typescript
// Browser 特定功能
// 使用 LocalStorage
const config = localStorage.getItem('autodev-config');

// 使用 Fetch API
const response = await fetch('/api/config');
```

## 迁移指南

### 从 jsMain 迁移代码

1. **Node.js 特定代码** → 移动到 `jsNodeMain`
   - 文件系统操作
   - 进程管理
   - 本地配置

2. **Browser 特定代码** → 移动到 `jsBrowserMain`
   - DOM 操作
   - Browser API
   - Web 存储

3. **共享代码** → 保留在 `jsMain`
   - 业务逻辑
   - 数据模型
   - 工具函数

## 开发工作流

### 开发 CLI

```bash
# 开发模式
npm run dev

# 构建测试
npm run build
npm start -- --help
```

### 开发 Web UI

```bash
# 开发模式（Webpack dev server）
npm run dev:web

# 构建生产版本
npm run build:web

# 输出在 mpp-ui/build/kotlin-webpack/js/productionExecutable/
```

## 优势

### ✅ 代码复用
- 共享 70-80% 的业务逻辑
- 只需维护一份核心代码

### ✅ 性能优化
- CLI 构建快速（3秒）
- Web 体积小（200-500KB vs 5MB+）

### ✅ 平台隔离
- 避免 Node.js API 污染浏览器代码
- 避免浏览器 polyfills 增加 CLI 体积

### ✅ 灵活性
- 可以独立开发和部署
- 可以使用平台特定的最佳实践

## 未来扩展

### 可能的新 Target

1. **React Native** (移动端)
   - 添加 `jsReactNativeMain`
   - 复用大部分 `jsMain` 代码

2. **Electron** (桌面应用)
   - 同时使用 `jsNodeMain` 和 `jsBrowserMain`
   - 主进程用 Node.js，渲染进程用 Browser

3. **WebWorker** (后台任务)
   - 添加 `jsWorkerMain`
   - 处理计算密集型任务

## 总结

通过 Kotlin Multiplatform 的 hierarchical source sets，我们实现了：

1. **一个代码库，多个目标**
2. **快速的构建速度**
3. **轻量级的输出**
4. **清晰的平台分离**

这是 Kotlin Multiplatform 的最佳实践，既能共享代码，又能保持平台特性。

