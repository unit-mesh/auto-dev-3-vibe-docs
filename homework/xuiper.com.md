# Homework: xuiper.com Landing Page

## Goal

为 `www.xuiper.com` 创建一个清晰、现代、可部署的 landing page，用于介绍 **Xuiper（AutoDev 3.0）** 的定位、平台覆盖与入口链接。

本作业/实现以本仓库现有模块为依据（尤其是 `mpp-ui/README.md`、`mpp-web/` 现有 Web UI），并复用已有的品牌视觉（如 `mpp-web/index.html` 里的 Xuiper loader 动画）。

## Domain

- Primary: `www.xuiper.com`

## Source of Truth (in this monorepo)

Landing page 实现在 `mpp-web/`：

- Landing：`/#/`
- 内置 Web UI（原有 React Web UI，用于演示 `mpp-core`）：`/#/app`

相关文件：

- `mpp-web/src/LandingPage.tsx`
- `mpp-web/src/Router.tsx`
- `mpp-web/index.html`（SEO/OG + loader 动画）
- `mpp-web/public/CNAME`（GitHub Pages 自定义域名）
- `mpp-web/public/robots.txt`
- `mpp-web/public/sitemap.xml`

## What to deliver (Checklist)

- **品牌首屏**：一句话定位 + 核心卖点 + 明确 CTA（Web / GitHub / Web UI）。
- **特性区块**：从 `mpp-ui`/顶层 `README` 提炼关键能力（KMP、多端、MCP、工具系统、多模型、i18n）。
- **平台覆盖**：IDE/桌面/移动/Web/CLI/Server 的概览。
- **SEO 基础**：title/description/OG + robots/sitemap。
- **部署**：GitHub Pages 或其他静态托管，并绑定 `www.xuiper.com`。

## Build & Run

在正常网络/依赖完整的环境下：

```bash
cd mpp-web
npm run build
```

> 注意：在一些离线环境中 `mpp-web/node_modules` 可能不存在；CI/本地需要先安装依赖后再执行 `vite build`。


