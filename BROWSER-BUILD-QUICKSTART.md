# 🌐 浏览器构建快速启动

## TL;DR

```bash
# 1. 安装依赖
cd /Volumes/source/ai/autocrud/mpp-ui
./scripts/enable-browser-build.sh

# 2. 启用浏览器构建（编辑 build.gradle.kts）
# 取消注释 browser { } 配置

# 3. 构建
cd ..
./gradlew :mpp-ui:jsBrowserProductionWebpack

# 4. 测试
cd mpp-ui
python3 -m http.server 8000
# 打开 http://localhost:8000/test-browser.html
```

## 📋 当前状态

### ✅ 已完成
- [x] 创建平台抽象层（`src/jsMain/typescript/platform/`）
- [x] Webpack 配置（`webpack.config.d/browser-support.js`）
- [x] 浏览器依赖清单（`package-browser.json`）
- [x] 测试 HTML（`test-browser.html`）
- [x] 安装脚本（`scripts/enable-browser-build.sh`）

### ⚠️ 需要手动操作
- [ ] 安装浏览器 polyfills
- [ ] 启用 `build.gradle.kts` 中的 browser 配置
- [ ] （可选）重构代码使用平台抽象层

## 🚀 方案选择

### 方案 A: 最小改动（推荐先试）⚡

**时间**: 30分钟  
**改动**: 只配置，不改代码

```bash
# 1. 安装依赖
./scripts/enable-browser-build.sh

# 2. 编辑 build.gradle.kts
# 将第 32-38 行的注释去掉：
js(IR) {
    browser {
        commonWebpackConfig {
            outputFileName = "mpp-ui.js"
        }
    }
    ...
}

# 3. 构建
cd ..
./gradlew :mpp-ui:jsBrowserProductionWebpack

# 4. 查看输出
ls -lh mpp-ui/build/kotlin-webpack/js/productionExecutable/
```

**限制**:
- ⚠️ 文件系统操作会失败（配置读写、文件操作）
- ⚠️ 命令执行不可用
- ⚠️ 体积较大（~3-5MB）

**适用**: 快速验证、原型开发

### 方案 B: 完整功能（生产就绪）🎯

**时间**: 2-3天  
**改动**: 重构 5 个文件

在方案 A 基础上：

```bash
# 1. 重构文件使用平台抽象
# 修改以下文件的导入：
# - src/jsMain/typescript/i18n/index.ts
# - src/jsMain/typescript/config/ConfigManager.ts
# - src/jsMain/typescript/utils/domainDictUtils.ts
# - src/jsMain/typescript/modes/AgentMode.ts
# - src/jsMain/typescript/index.tsx

# 将：
import * as fs from 'fs';
import * as path from 'path';
import * as os from 'os';

# 改为：
import { fs, path, os } from '../platform/index.js';

# 2. 测试
npm run build
npm test
```

**优点**:
- ✅ 完整功能（配置使用 LocalStorage）
- ✅ Node.js 和浏览器都支持
- ✅ 体积可优化到 ~2MB

**适用**: 生产环境

### 方案 C: 独立 Web 模块（长期）🏗️

**时间**: 2-4周  
**改动**: 新建模块

```bash
# 创建专门的浏览器版本
mkdir ../mpp-web
cd ../mpp-web

# 初始化
npm init @vitejs/app
# 选择 React/Vue

# 依赖 mpp-core
npm install ../mpp-core/build/js/packages/mpp-core
```

**优点**:
- ✅ 体积最小（~500KB-1MB）
- ✅ 性能最好
- ✅ 易于维护

**适用**: 独立 Web 应用

## 📦 体积分析

| 组件 | 大小 | 说明 |
|-----|------|------|
| mpp-ui.js | ~1.5MB | 主代码 |
| autodev-mpp-core.js | ~1.6MB | 核心库 |
| compose-runtime.js | ~1.2MB | Compose 运行时 |
| kotlin-stdlib.js | ~680KB | Kotlin 标准库 |
| kotlinx-* | ~1.5MB | Kotlin 扩展库 |
| 其他依赖 | ~500KB | 各种库 |
| **总计** | **~7MB** | 未优化 |
| **Gzip 后** | **~1.5MB** | 压缩后 |

### 优化潜力

- 🎯 移除未使用的 Compose 组件: -500KB
- 🎯 Tree-shaking: -1MB
- 🎯 Code splitting: 按需加载
- 🎯 使用 CDN: -1MB
- **最终目标**: ~2MB 未压缩, ~600KB Gzip

## 🧪 测试流程

### 1. 编译测试

```bash
cd /Volumes/source/ai/autocrud
time ./gradlew :mpp-ui:jsBrowserProductionWebpack
```

**预期**:
- ✅ 2-3分钟首次编译
- ✅ 30秒增量编译
- ⚠️ 一些 Node.js 模块警告（正常）

### 2. 文件检查

```bash
cd mpp-ui/build/kotlin-webpack/js/productionExecutable
ls -lh

# 应该看到:
# mpp-ui.js         (~3-5MB)
# mpp-ui.js.map
# vendors.js        (如果启用了 code splitting)
```

### 3. 浏览器测试

```bash
cd /Volumes/source/ai/autocrud/mpp-ui
python3 -m http.server 8000

# 或使用 npx
npx serve . -p 8000
```

打开: http://localhost:8000/test-browser.html

**检查项**:
- [x] 页面加载正常
- [x] 没有 404 错误
- [x] Console 有成功日志
- [x] LocalStorage 测试通过

### 4. 功能测试

在浏览器 Console 中：

```javascript
// 测试模块加载
console.log('AutoDev:', window['mpp-ui']);

// 测试 LocalStorage
localStorage.setItem('autodev-fs:/test', 'hello');
console.log(localStorage.getItem('autodev-fs:/test'));

// 查看所有 exports
console.log(Object.keys(window).filter(k => k.includes('autodev')));
```

## ⚠️ 已知问题

### 1. Node.js API 不可用

**问题**: `fs.readFileSync is not a function`

**原因**: 浏览器没有文件系统

**解决**:
- 方案 A: 接受限制，只用不依赖文件的功能
- 方案 B: 使用平台抽象层（LocalStorage）

### 2. 编译慢

**问题**: 首次编译 10+ 分钟

**原因**: Source maps 处理慢

**解决**:
```javascript
// webpack.config.d/optimization.js
config.devtool = 'cheap-source-map';  // 更快的 source maps
// 或
config.devtool = false;  // 禁用 source maps
```

### 3. 打包体积大

**问题**: 7MB+ 未压缩

**原因**: 包含所有 Kotlin 和 Compose 运行时

**解决**:
- 启用 tree-shaking（已配置）
- 使用 code splitting（已配置）
- 移除未使用的功能
- 使用 CDN 托管大型库

## 📚 相关文档

- [完整实施指南](mpp-ui-browser-support-guide.md)
- [性能优化](mpp-ui-web-build-fix.md)
- [平台抽象 API](src/jsMain/typescript/platform/)

## 🆘 故障排除

### 编译失败

```bash
# 清理缓存
./gradlew clean

# 重新安装依赖
cd mpp-ui
rm -rf node_modules
npm install

# 更新 yarn.lock
cd ..
./gradlew kotlinUpgradeYarnLock
```

### 浏览器加载失败

1. 检查文件路径
2. 查看浏览器 Console
3. 检查 Network 标签
4. 确认启动了本地服务器

### 功能不工作

- 检查是否使用了 Node.js API
- 查看 Console 错误
- 尝试方案 B（平台抽象）

## 💡 建议

### 立即尝试（5分钟）

```bash
# 1. 快速安装
cd mpp-ui && ./scripts/enable-browser-build.sh

# 2. 启用浏览器构建（手动编辑 build.gradle.kts）

# 3. 测试编译
cd .. && ./gradlew :mpp-ui:jsBrowserProductionWebpack
```

### 如果成功（继续优化）

1. ✅ 优化 webpack 配置
2. ✅ 实施平台抽象
3. ✅ 添加自动化测试
4. ✅ 部署到 CDN

### 如果不满意（替代方案）

考虑创建独立的 `mpp-web` 模块：
- 更小的体积
- 更好的性能
- 专门为浏览器设计

## 📞 需要帮助？

我可以帮你：
1. ✅ 执行任何步骤
2. ✅ 调试编译错误
3. ✅ 重构代码
4. ✅ 优化性能

