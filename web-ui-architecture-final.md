# AutoDev Web UI 架构 - 最终方案

## ✅ 构建成功！

### 性能对比

| 方案 | 构建工具 | 构建时间 | Bundle 大小 | Gzipped | 状态 |
|-----|---------|---------|------------|---------|------|
| ❌ 旧方案 (Kotlin/JS + Compose Web + Webpack) | Gradle | **11分35秒** | **5MB+** | N/A | 失败 |
| ✅ 新方案 (TypeScript + React + Vite) | Vite | **3.3秒** | 716KB | 204KB | **成功** |

**性能提升**：
- 构建速度：**快 210 倍** (11分35秒 → 3.3秒)
- Bundle 大小：**小 7 倍** (5MB+ → 716KB)

## 架构设计

### 核心思想

**与 CLI 保持一致** - 都是 TypeScript + mpp-core：

```
mpp-ui (CLI)                     mpp-web (Web UI)
├── TypeScript/React (Ink)       ├── TypeScript/React (DOM)
└── @autodev/mpp-core             └── @autodev/mpp-core ← 相同！
     ↑                                 ↑
     └─────────────────────────────────┘
               共享核心逻辑
```

### 项目结构

```
AutoDev Project/
├── mpp-core/                    # Kotlin 核心逻辑
│   └── build/
│       └── compileSync/js/...   # Kotlin/JS 编译产物
├── mpp-ui/                      # Node.js CLI
│   ├── src/
│   │   ├── jsMain/              # Kotlin + TypeScript (CLI)
│   │   │   ├── kotlin/          # CLI 特定 Kotlin代码
│   │   │   └── typescript/      # Ink/React TUI
│   │   ├── commonMain/          # Compose (JVM/Android)
│   │   ├── jvmMain/             # Desktop
│   │   └── androidMain/         # Android
│   └── package.json             # CLI 依赖
└── mpp-web/ ✨                  # Web UI (独立项目)
    ├── src/
    │   ├── App.tsx              # React Web UI
    │   ├── main.tsx             # 入口
    │   └── index.css            # 样式
    ├── index.html               # HTML 模板
    ├── vite.config.ts           # Vite 配置
    ├── tsconfig.json            # TypeScript 配置
    └── package.json             # Web 依赖
```

## 关键设计决策

### 1. 为什么独立项目？

❌ **不采用** Gradle 的 `jsBrowserMain` source set

原因：
- Gradle Kotlin/JS 编译太慢（11分钟）
- Webpack 配置复杂
- Compose Web 体积大（5MB+）
- 需要 Node.js polyfills

✅ **采用** 独立的 `mpp-web` 项目

优势：
- Vite 构建极快（3.3秒）
- 标准的 React/TypeScript 开发体验
- 轻量级 bundle（204KB gzipped）
- 不需要 Gradle/Kotlin 编译

### 2. 如何使用 mpp-core？

**与 CLI 完全相同的方式**：

```typescript
// mpp-ui/src/jsMain/typescript/index.tsx (CLI)
import mppCore from '@autodev/mpp-core';
const { cc } = mppCore;

// mpp-web/src/App.tsx (Web UI)
import * as mppCore from '@autodev/mpp-core';
const { cc } = mppCore;
```

mpp-core 编译一次，两处使用！

### 3. 依赖关系

```
mpp-core (Kotlin/JS)
    ↓ (编译)
autodev-mpp-core.js (共享)
    ↙           ↘
mpp-ui      mpp-web
(CLI)       (Web UI)
```

## 开发工作流

### CLI 开发

```bash
cd mpp-ui
npm run build       # 构建 Kotlin + TypeScript
npm start           # 运行 CLI
```

### Web UI 开发

```bash
cd mpp-web
npm run build:kotlin    # 构建 mpp-core (只需一次)
npm run dev             # 启动开发服务器
npm run build           # 生产构建
```

## 技术栈对比

### mpp-ui (CLI)

```json
{
  "dependencies": {
    "@autodev/mpp-core": "^0.1.4",
    "ink": "^5.0.1",              // 终端 UI
    "react": "^18.3.1",
    "@modelcontextprotocol/sdk": "^1.0.4"  // MCP
  }
}
```

**特点**：
- Node.js 环境
- 使用 Node.js API (fs, path, os)
- Gradle + TypeScript 编译

### mpp-web (Web UI)

```json
{
  "dependencies": {
    "@autodev/mpp-core": "^0.1.4",
    "react": "^18.3.1",
    "react-dom": "^18.3.1"        // DOM 渲染
  },
  "devDependencies": {
    "vite": "^6.0.1"              // 构建工具
  }
}
```

**特点**：
- 浏览器环境
- 纯浏览器 API
- Vite 快速构建

## Vite 配置

```typescript
// mpp-web/vite.config.ts
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      // 指向 mpp-core 编译产物
      '@autodev/mpp-core': path.resolve(
        __dirname, 
        '../mpp-core/build/compileSync/js/main/productionLibrary/kotlin/autodev-mpp-core.js'
      ),
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  },
});
```

## 测试结果

### ✅ CLI 构建

```bash
$ cd mpp-ui && time npm run build
BUILD SUCCESSFUL in 5s
npm run build: 5.5 seconds total

$ npm start -- --version
0.1.3 ✅

$ npm start -- --help
Usage: autodev [options] [command] ✅
```

### ✅ Web UI 构建

```bash
$ cd mpp-web && time npm run build
BUILD SUCCESSFUL in 3.3 seconds

Output:
  dist/index.html                   0.40 kB │ gzip:   0.28 kB
  dist/assets/index-*.css           0.33 kB │ gzip:   0.27 kB
  dist/assets/index-*.js          716.13 kB │ gzip: 204.47 kB
✅ Built successfully!
```

## 优势总结

### 🚀 性能

1. **极快的构建** - Vite 比 Kotlin/JS + Webpack 快 210 倍
2. **轻量级产物** - 204KB (gzipped) vs 5MB+
3. **快速的 HMR** - Vite 热更新毫秒级

### 🎯 开发体验

1. **标准的 React 开发** - 不需要学习 Compose Web
2. **熟悉的工具链** - Vite, TypeScript, React
3. **完整的生态** - 可以使用任何 React 库

### 🏗️ 架构清晰

1. **关注点分离** - CLI 和 Web UI 独立
2. **代码复用** - 共享 mpp-core 核心逻辑
3. **易于维护** - 各自使用最佳实践

### 📦 部署灵活

1. **CLI** - npm package, 全局安装
2. **Web UI** - 静态文件, 任意 CDN/服务器
3. **独立版本** - 可以独立发布和更新

## Bundle 分析

### 主要组成

```
716KB total (204KB gzipped)
├── React + React-DOM: ~150KB
├── mpp-core (Kotlin/JS): ~500KB
│   ├── Kotlin stdlib
│   ├── Coroutines
│   ├── Serialization
│   ├── DevIns Compiler
│   └── Agent Logic
└── App code: ~66KB
```

### 优化建议

1. **代码分割** - 使用动态 import()
2. **Tree shaking** - 只导入需要的 mpp-core 模块
3. **Lazy loading** - 按需加载功能
4. **CDN** - React 使用 CDN

## 未来扩展

### 可能的优化

1. **Micro-frontend** - 将 Web UI 模块化
2. **PWA** - 添加离线支持
3. **Web Workers** - 在后台运行 mpp-core
4. **Code Splitting** - 按路由分割代码

### 新功能

1. **实时协作** - WebSocket + mpp-core
2. **浏览器扩展** - Chrome/Firefox extension
3. **移动 Web** - 响应式设计
4. **Electron** - 桌面应用（复用 Web UI）

## 总结

通过将 Web UI 设计为**独立的 TypeScript/React 项目**，我们实现了：

✅ **与 CLI 架构一致** - 都使用 TypeScript + mpp-core
✅ **极快的构建速度** - 3.3秒 vs 11分钟
✅ **轻量级产物** - 204KB vs 5MB+
✅ **标准的开发体验** - 标准 React/Vite 工具链
✅ **易于维护** - 清晰的关注点分离

这是 **Kotlin Multiplatform 的正确用法**：
- Kotlin 写核心逻辑 (mpp-core)
- 平台特定代码用最佳工具 (Node.js/Browser 用 TypeScript)
- 避免强行用 Kotlin/JS 编译浏览器代码

**最佳实践**：
> 让 Kotlin 做它擅长的（业务逻辑），让前端工具做它们擅长的（UI）。

