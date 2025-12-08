# Temporal Lite - 执行摘要

> **一句话总结**: 在不引入 Temporal 库的前提下，用 2-3 周时间，基于现有 mpp-server 和 mpp-core 实现 80% 的 Temporal 核心能力，成本降低 80%。

---

## 🎯 核心价值

### 问题
当前 Agent 执行存在的问题：
- ❌ 服务器崩溃后任务丢失，无法恢复
- ❌ 无法实现"暂停-等待审批-继续"的长时间运行任务
- ❌ 多 Agent 协作缺乏统一的编排机制
- ❌ 执行历史不完整，难以审计和调试

### 解决方案
实现"Temporal Lite"（轻量级 Temporal）：
- ✅ **持久化执行**: 服务器崩溃后自动恢复
- ✅ **事件溯源**: 完整记录执行历史
- ✅ **长时间运行**: 支持天/周级别的暂停-恢复
- ✅ **Signal/Query**: 外部控制运行中的任务
- ✅ **低成本**: 单机部署，简单维护

---

## 📊 方案对比

### vs. Temporal（官方）

| 维度 | Temporal | 我们的方案 |
|-----|---------|-----------|
| **实施时间** | 1-2 个月（学习 + 集成） | 2-3 周 |
| **运维成本** | $800-1000/月（多服务 + DB） | $170/月（单服务 + DB） |
| **开发成本** | 学习曲线陡峭 | 基于现有知识 |
| **功能完整度** | 100%（全功能） | 80%（核心功能） |
| **并发能力** | 10,000+ 工作流 | 500-1000 工作流 |
| **部署复杂度** | 高（4 个服务） | 低（单进程） |
| **迁移路径** | - | 可平滑迁移到 Temporal |

**结论**: 我们的方案是 **"Temporal 的 80/20 法则"** - 用 20% 的成本实现 80% 的功能。

### vs. 现状（不做任何改进）

| 场景 | 现状 | 改进后 |
|-----|-----|-------|
| **服务器崩溃** | ❌ 任务丢失 | ✅ 自动恢复 |
| **代码审查等待审批** | ❌ 无法实现 | ✅ 暂停数天后恢复 |
| **多 Agent 协作** | ⚠️ 手动协调 | ✅ 自动编排 |
| **执行历史** | ⚠️ 部分日志 | ✅ 完整事件流 |
| **调试困难** | ❌ 难以复现 | ✅ 重放历史 |

---

## 🏗️ 核心架构（简化版）

```
用户 → REST API → WorkflowEngine → Database (事件存储)
                       ↓
                 DurableAgent (执行器)
                       ↓
                 CodingAgent (现有逻辑)
```

**关键组件**:
1. **EventStore**: 持久化所有执行步骤（事件溯源）
2. **CheckpointManager**: 定期保存状态快照（加速恢复）
3. **SignalQueue**: 外部发送信号给运行中的任务
4. **DurableAgentExecutor**: 包装现有 CodingAgent，添加持久化能力

**数据流**:
```
启动任务 → 记录事件 → 执行步骤 → 记录事件 → 创建检查点 → ...
                                    ↓
                          服务器崩溃（可能发生）
                                    ↓
                  启动 → 恢复检查点 → 重放事件 → 继续执行
```

---

## 📋 实施路线图

### Phase 1: 基础设施（2 周）⭐ 核心
**目标**: 能够持久化和恢复工作流

**交付物**:
- [ ] 数据库设计（4 张表）
- [ ] EventStore 实现
- [ ] CheckpointManager 实现
- [ ] WorkflowEngine 核心逻辑

**验收标准**:
- 服务器重启后工作流自动恢复并继续执行

### Phase 2: Agent 集成（1 周）⭐ 核心
**目标**: DurableAgentExecutor 能运行真实任务

**交付物**:
- [ ] DurableAgentExecutor 包装器
- [ ] Activity 包装（LLM、Tool）
- [ ] 暂停/恢复机制

**验收标准**:
- 完整的 Agent 任务能执行并持久化
- LLM 调用能确定性重放

### Phase 3: API 与集成（3-5 天）⭐ 核心
**目标**: 前端能通过 API 控制工作流

**交付物**:
- [ ] REST API（启动、Signal、Query、SSE）
- [ ] 与现有 SessionManager 集成

**验收标准**:
- Android/Web 客户端能启动和控制工作流

### Phase 4: 高级特性（可选，1-2 周）
**目标**: 增强功能

**交付物**:
- [ ] 子工作流支持
- [ ] 侧信道流式传输（LLM 流式输出）
- [ ] 监控面板

---

## 🎬 典型用例

### 用例 1: 代码审查 Agent（人机回环）

**场景**: Agent 分析代码，等待人类审批，然后执行后续操作

**代码示例**:
```kotlin
// 工作流定义
suspend fun codeReviewWorkflow(code: String) {
    // 1. LLM 分析代码
    val analysis = llmActivity.execute("Review this code: $code")
    
    // 2. 发送通知
    notifyActivity.execute("Code review ready for $reviewer")
    
    // 3. 等待审批（可能数天）- 自动创建检查点，释放资源
    val approval = waitForSignal("CodeReviewApproval", timeoutMs = 7.days)
    
    // 4. 根据审批结果执行
    if (approval.data["approved"] == true) {
        mergeActivity.execute(code)
    } else {
        refactorActivity.execute(code, approval.data["feedback"])
    }
}

// 前端/CLI 发送审批
POST /api/workflows/{id}/signal
{
  "signalName": "CodeReviewApproval",
  "data": {"approved": true}
}
```

**价值**:
- ✅ 任务可以暂停数天，不占用资源
- ✅ 服务器重启后自动恢复等待状态
- ✅ 完整的审计日志

### 用例 2: 多 Agent 协作开发

**场景**: Master Agent 拆解任务，3 个 Worker Agent 并行工作

**代码示例**:
```kotlin
suspend fun multiAgentWorkflow(task: String) {
    // 1. Master Agent 拆解任务
    val subtasks = masterAgent.planTask(task)
    
    // 2. 启动 3 个子工作流（并行）
    val jobs = subtasks.map { subtask ->
        async { 
            startChildWorkflow(subtask)
        }
    }
    
    // 3. 等待所有子任务完成
    val results = jobs.awaitAll()
    
    // 4. 汇总结果
    masterAgent.summarize(results)
}
```

**价值**:
- ✅ 自动管理子工作流生命周期
- ✅ 子任务失败自动重试
- ✅ 父任务等待期间不占用资源

### 用例 3: 崩溃恢复（确定性重放）

**场景**: Agent 执行到第 50 步时服务器崩溃

**恢复流程**:
```
1. 服务器重启
2. WorkflowEngine 扫描数据库，发现未完成的工作流
3. 加载最新检查点（假设第 40 步）
4. 重放第 41-50 步的事件（不重新执行 LLM/Tool）
   - 第 42 步：LLM 调用 → 从历史读取结果 "X"（不重新调用）
   - 第 45 步：Tool 调用 → 从历史读取结果 "Y"
5. 从第 51 步继续执行
```

**价值**:
- ✅ LLM 调用不会重复计费
- ✅ 工具执行不会重复副作用（如不会重复发邮件）
- ✅ 状态完全一致

---

## 💰 成本分析

### 基础设施成本（月）

| 方案 | 服务器 | 数据库 | 总计 |
|-----|-------|-------|-----|
| **Temporal 自托管** | $500 | $200 | $700 |
| **Temporal Cloud** | - | - | $1000+ (按 Action 计费) |
| **我们的方案** | $100 | $50 | $150 |

### 开发成本

| 方案 | 学习 | 开发 | 集成 | 总计 |
|-----|-----|-----|-----|-----|
| **Temporal** | 1 周 | 2 周 | 2 周 | 5 周 |
| **我们的方案** | 0.5 周 | 2 周 | 0.5 周 | 3 周 |

### ROI 分析（假设 1 年）

| 指标 | Temporal | 我们的方案 | 节省 |
|-----|---------|-----------|-----|
| **基础设施** | $8,400 | $1,800 | $6,600 (79%) |
| **开发人力** | $20,000 | $12,000 | $8,000 (40%) |
| **运维人力** | $12,000 | $4,000 | $8,000 (67%) |
| **总成本** | $40,400 | $17,800 | $22,600 (56%) |

**结论**: 一年节省 **$22,600**（约 ¥160,000）

---

## ⚠️ 风险与限制

### 限制

| 限制 | 影响 | 缓解措施 |
|-----|-----|---------|
| **并发能力** | 单机 < 1000 并发 | 足够应对中小规模；未来可迁移 Temporal |
| **分布式部署** | 不支持多数据中心 | 单数据中心足够；未来可升级 |
| **确定性保证** | 需手动确保 | 通过 Activity 包装 + Code Review |

### 风险

| 风险 | 概率 | 对策 |
|-----|-----|-----|
| **开发超期** | 中 | 迭代式开发，Phase 1 完成即可用 |
| **性能瓶颈** | 低 | PostgreSQL + 索引优化 |
| **状态快照过大** | 中 | 限制对话历史长度 + 压缩 |

---

## ✅ 决策建议

### 推荐：✅ 实施 "Temporal Lite" 方案

**理由**:
1. **快速验证**: 3 周完成，立即可用
2. **低成本**: 节省 50%+ 成本
3. **低风险**: 基于现有技术栈，无需学习新框架
4. **高价值**: 解决核心痛点（崩溃恢复、长时间运行）
5. **可扩展**: 未来可平滑迁移到完整 Temporal

### 何时重新评估？

**触发条件**（任一满足）:
- ✅ 并发工作流数 > 500
- ✅ 单机数据库成为瓶颈
- ✅ 需要多数据中心部署
- ✅ 需要 Temporal 的高级特性（如复杂的超时控制）

**届时选项**:
1. 迁移到完整 Temporal（保持 API 兼容）
2. 实现简单的 Worker 池（多服务器部署）

---

## 📚 文档索引

详细文档已生成：
1. **`temporal-like-implementation-plan.md`** - 详细实施计划（80 页）
   - 数据库设计
   - 代码示例
   - Phase-by-Phase 任务分解
   
2. **`temporal-vs-our-implementation.md`** - 深度对比分析（60 页）
   - 架构对比
   - 性能对比
   - 成本对比

3. **`temporal-lite-executive-summary.md`** (本文档) - 执行摘要（10 页）

---

## 🚀 下一步行动

### 立即行动（本周）
- [ ] 团队评审本方案
- [ ] 确认技术选型（SQLite vs PostgreSQL）
- [ ] 设立 3 周 Sprint

### Week 1-2: Phase 1
- [ ] 数据库设计
- [ ] EventStore 实现
- [ ] CheckpointManager 实现
- [ ] 单元测试

### Week 3: Phase 2 + 3
- [ ] DurableAgentExecutor
- [ ] REST API
- [ ] 集成测试

### Week 4（可选）
- [ ] 生产部署
- [ ] 监控配置
- [ ] 文档完善

---

## 📞 联系与支持

**技术咨询**: 参考 `temporal-like-implementation-plan.md` 的详细设计  
**架构问题**: 参考 `temporal-vs-our-implementation.md` 的对比分析  
**实施支持**: 每个 Phase 都有详细的任务清单和验收标准

---

**最终建议**: 🚀 **Go for it!** 这是一个低风险、高回报的方案，能够快速解决现有问题，同时为未来留出扩展空间。

