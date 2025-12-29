# E2E Testing Agent 实现计划

> GitHub Issue: https://github.com/phodal/auto-dev/issues/532

## Issue: AI-Driven E2E Testing Agent

### 概述

实现一个基于 AI 的端到端测试 Agent，能够通过自然语言指令自动执行 Web UI 测试，具备多模态感知、智能规划和自愈能力。

### 背景

当前项目已具备关键基础设施：
- `mpp-viewer-web/webedit/` - KCEF 浏览器控制、DOM 提取、Vision Helper
- `mpp-core/agent/` - Agent 架构、Tool 系统、SubAgent 机制
- `WebElementSourceMapperTool` - DOM 元素到源码映射

### 目标

1. 支持自然语言描述测试场景，自动生成并执行测试
2. 多模态感知：DOM + Accessibility Tree + 视觉截图
3. 自愈定位器：元素变化时自动修复选择器
4. 确定性执行：生成阶段用 AI，执行阶段用确定性引擎

### 技术方案

#### 架构

```
E2E Testing Agent
├── Perception Layer (感知层)
│   ├── PageStateExtractor - 页面状态提取
│   ├── SetOfMarkTagger - SoM 视觉标记
│   └── DOMCleaner - DOM 精简
├── Planner Layer (规划层)
│   ├── TestActionPlanner - 动作规划
│   ├── ActionSpace - 动作空间 (click, type, scroll, wait, assert)
│   └── TestMemory - 短期记忆
├── Executor Layer (执行层)
│   ├── BrowserActionExecutor - 浏览器动作执行
│   └── SelfHealingLocator - 自愈定位器
└── Supervisor Layer (监督层)
    └── TestSupervisor - 执行验证
```

#### 模块位置

```
mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/e2etest/
├── E2ETestAgent.kt
├── E2ETestContext.kt
├── perception/
├── planner/
├── executor/
└── model/
```

#### 平台适配

| 平台 | 浏览器控制 | 实现方式 |
|------|-----------|---------|
| JVM Desktop | KCEF | 复用 WebEditBridge |
| Android | WebView | Android WebView API |
| iOS | WKWebView | Swift 桥接 |
| JS/WASM | Playwright MCP | MCP Server |

### 实现阶段

#### Phase 1: 核心接口和数据模型
- [ ] 定义 `TestAction` 动作空间
- [ ] 定义 `E2ETestContext` 上下文
- [ ] 定义 `PageStateExtractor` expect 接口
- [ ] 定义 `E2ETestResult` 结果模型

#### Phase 2: JVM 平台实现
- [ ] 实现 `JvmPageStateExtractor` (复用 KCEF)
- [ ] 实现 `BrowserActionExecutor`
- [ ] 实现 `SelfHealingLocator` (L1 算法级)
- [ ] 实现 `TestActionPlanner`

#### Phase 3: Agent 集成
- [ ] 实现 `E2ETestAgent` 作为 SubAgent
- [ ] 集成到 CodingAgent
- [ ] 添加 Prompt 模板

#### Phase 4: 高级功能
- [ ] SoM (Set-of-Mark) 视觉标记
- [ ] L2 LLM 语义自愈
- [ ] Playwright MCP Server 支持
- [ ] 测试脚本生成和导出

### 关键技术点

1. **Accessibility Tree vs DOM**: 优先使用 A11y Tree，Token 效率高 60-80%
2. **Set-of-Mark**: 在截图上标记数字，LLM 输出数字而非坐标
3. **分层自愈**: L1 算法级 (毫秒) + L2 LLM 语义级 (秒)
4. **确定性执行**: 生成阶段用 LLM，执行阶段用固化脚本

### 参考资料

- [Browser-use](https://github.com/browser-use/browser-use) - 开源 Web Agent
- [ScenGen](https://arxiv.org/html/2506.05079v3) - 场景驱动 GUI 测试
- [Set-of-Mark](https://huggingface.co/papers/2310.11441) - 视觉定位
- [WebArena](https://arxiv.org/html/2307.13854v4) - 动作空间设计

### 验收标准

1. 能通过自然语言执行简单的登录测试流程
2. 元素选择器变化时能自动修复
3. 支持 JVM Desktop 平台
4. 测试执行结果可视化展示

### 相关文档

- `docs/test-agent/AI E2E 测试 Agent 实现方案.md` - 详细研究报告
- `docs/features/webedit-agent-completed.md` - WebEdit 功能文档
