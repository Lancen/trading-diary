# Step 7: 执行（subagent-driven-development）

**执行**: `/superpowers:subagent-driven-development`

## 批量策略

tasks.md 的细粒度任务不适合逐条派发（浪费且容易冲突）。按文件边界和依赖关系合并为批：

```
Phase 1: 项目脚手架            ← 1-2 agent 并行
Phase 2: 基础设施              ← N agent 并行（互不重叠的文件组）
Phase 3-N: User Story 实现     ← 1 agent/Story（紧密耦合不拆分）
Phase N+1: 测试                ← N agent 并行（后端 + 前端）
Phase N+2: 收尾                ← 手动完成
```

## Agent 派发规则

| 规则 | 说明 |
|------|------|
| [P] 标记的独立文件组可并行派发 | 无文件重叠 = 无冲突 |
| 紧密耦合的模块不拆分 | 同一 Service/Controller/Config 必须同一个 agent |
| 每批必须 commit | 编译验证 + `git commit` = agent 的"完成"定义 |
| agent 无法执行 shell 时 | 主 session 接管编译和提交 |
| 不接受"文件创建了但没编译" | BLOCKED 状态，必须补完 |
| 同一批内出现失败 | 修复 → 编译 → 提交后再进下一批 |
| 单 agent 任务数 ≤ 5 | 超过 5 个任务时必须拆批，避免 agent 认知过载 |
| 单 agent 执行时间预警 | > 600s（10 分钟）时检查是否任务过多，下一批减半 |
| 单 agent token 消耗预警 | > 60k token 时说明上下文膨胀，下一批减半 |
| **模型选择** | **派发时 MUST 显式传 `model` 参数**。机械任务用 haiku，审查任务用 opus。判断标准见下表 |
| 遇阻升级 | haiku 返回 BLOCKED → 补充上下文重试 → 仍 BLOCKED → 升级到 opus 并拆小任务 |

### 模型选择速查

| 信号 | 用 `model: "haiku"` | 用 `model: "opus"` |
|------|---------------------|---------------------|
| 文件数 | 1-2 个文件 | 3+ 文件或跨层 |
| 任务性质 | CRUD、实体、Mapper、配置、简单 Service | 架构设计、安全审查、代码审查、跨模块连线 |
| 认知负荷 | spec 明确、无设计判断 | 需要设计判断、全局理解 |
| 典型 Phase | Phase 1-4（脚手架、基础设施、简单 API） | 收尾审查、安全审计 |

### Agent 执行分析

每次 agent 完成时返回 `<usage>` 标签提供三个指标：

| 指标 | 含义 | 健康阈值 |
|------|------|---------|
| `total_tokens` | 消耗的 token 总数 | ≤ 60,000 |
| `tool_uses` | 工具调用次数（Read/Write/Bash 等） | ≤ 80 |
| `duration_ms` | 执行时长（毫秒） | ≤ 600,000 |

**效率诊断**：
- token 高 + tool_uses 高 → 任务太多，拆批
- token 低 + tool_uses 高 → agent 在频繁读文件，上下文给的不足
- duration 高 + token 低 → agent 在等待（编译/网络），非任务问题
- duration 高 + token 高 + tool_uses 低 → agent 推理过度，任务描述不够清晰

### Agent 能力边界

| agent 能做 | agent 不能做（需主 session） |
|-----------|---------------------------|
| 创建/编辑源文件 | 执行 `gradle wrapper` |
| 运行编译命令（如有权限） | `brew install` |
| git commit | `pnpm install` |
| 读取项目文件 | 启动应用服务 |
| 搜索代码 | 浏览器测试（需 Playwright MCP） |

## 执行顺序原则

- **Phase 1→2→3 串行**，Phase 2 内部可大规模并行
- **每批提交后立即编译验证**，不在编译不过的情况下继续加代码
- **测试 Phase 可与收尾并行**，测试编写和最终验证可以覆盖

## TDD 纪律（实现 agent 强制）

所有 **实现类任务**（Service、Controller、核心业务逻辑）的 agent 派发时，prompt 必须包含以下指令：

```
## TDD（强制）

在写任何生产代码之前，调用 Skill 工具加载 superpowers:test-driven-development。
严格遵循 RED → GREEN → REFACTOR：

1. RED:   为每个新方法/类写最小失败单元测试，运行确认它红
2. GREEN: 写最少代码让测试绿
3. REFACTOR: 清理代码，保持绿
4. 循环直到实现完成

不加载 TDD skill = 不要开始写代码。
```

### 适用范围

**适用**：所有实现类任务（Service、Controller、核心逻辑）

**不适用**：
- 纯实体/Mapper/配置文件创建任务（Phase 1）
- 集成测试和 E2E 测试任务（Phase 6/7，这些是独立测试任务，不走 TDD 循环）

### TDD 单元测试 vs Speckit 集成测试

| 维度 | TDD 单元测试（实现 agent） | Speckit 测试（Phase 6/7） |
|------|--------------------------|--------------------------|
| 粒度 | 单个方法/类 | 用户故事/完整 API |
| 目的 | 驱动代码设计，防回归 | 验证功能完整可用 |
| 谁写 | 写代码的同一个 agent | 可以和实现不同 |
| 产物 | 单元测试（JUnit + Mockito） | 集成测试（MockMvc）+ E2E（Playwright） |
| 位置 | 实现任务内，RED→GREEN 循环 | tasks.md 独立 Phase |

两者互补，不替代。实现类必须同时满足 TDD 单元测试 + Phase 6/7 的集成/E2E 测试。

### Implementer Report 必须包含 TDD Evidence

```
## TDD Evidence
- [ ] 加载 superpowers:test-driven-development skill
- [ ] RED: 测试文件 + 测试方法名 + 确认失败的截图/输出
- [ ] GREEN: 实现文件 + 测试绿证明
- [ ] 测试文件路径: src/test/java/.../XxxTest.java
```

未提交 TDD Evidence 的 report → 视为 TDD 纪律违规，审查返回 ❌。

## 两阶段审查

每批实现完成后，经过两道审查门禁才能进入下一批：

```
Implementer 实现 + commit
        │
        ▼
┌──────────────────┐
│ 1. Spec 合规审查  │ ← 独立 subagent，读代码对照需求逐行验证
└──────┬───────────┘
       │
   ┌───┴───┐
   │ 通过？  │
   └───┬───┘
       │ ❌ 有问题 → Implementer 修复 → 重新审查
       │ ✅ 通过
       ▼
┌──────────────────┐
│ 2. 代码质量审查   │ ← 独立 subagent，检查代码是否正确实现、干净可维护
└──────┬───────────┘
       │
   ┌───┴───┐
   │ 通过？  │
   └───┬───┘
       │ ❌ 有问题 → Implementer 修复 → 重新审查
       │ ✅ 通过
       ▼
    进入下一批
```

### Spec 合规审查

**目的**: 验证实现是否完全符合需求——不多不少。

**审查方式**: 派发独立的 spec-reviewer subagent，提供需求原文和实现报告，要求 reviewer **读代码**逐行验证，不信任 implementer 的报告。

| 检查维度 | 内容 |
|----------|------|
| 缺失 | 需求里的每一项都实现了吗？report 声称做了但实际没做？ |
| 多余 | 是否实现了需求没提的功能？是否过度设计？ |
| 误解 | 是否把需求理解错了？是否解决了错误的问题？ |
| **TDD 纪律** | **每个实现文件是否有对应的单元测试文件？测试文件是否在实现文件之前提交（git log 时间戳）？report 是否包含 TDD Evidence section？** |

**审查人必须**：读代码 + 读 git log，不信任 report。独立验证一切。

**结果**: ✅ Spec 合规 或 ❌ 具体问题清单（含文件:行号）

### 代码质量审查

**目的**: 验证实现质量——干净、可测试、可维护。

**前置条件**: Spec 合规审查已通过。

| 检查维度 | 内容 |
|------|------|
| 责任单一 | 每个文件只有一个明确的职责，接口清晰 |
| 分解合理 | 每个单元可独立理解和测试 |
| 结构一致 | 遵循 plan.md 定义的文件结构和项目现有模式 |
| 文件膨胀 | 新建文件是否过大？是否显著扩大了已有文件？ |

**结果**: ✅ 通过 或 ❌ 问题清单（Critical / Important / Minor）

### 审查循环规则

- 审查发现的问题由**同一个 implementer subagent** 修复
- 修复后**重新审查**同一个 reviewer，直到通过
- **禁止**在审查未通过时进入下一批
- **禁止**跳过任一审查阶段
- Spec 合规审查未通过前，**禁止**派发代码质量审查
- **每批结束后主 session MUST 自检**：本批是否走了两道审查？实现文件数与 tasks.md 任务数是否匹配？有 client 方法无 orchestrator 调用等"孤岛"代码？
- **审查 agent 的 prompt MUST 包含 design doc 路径**，代码与 spec 不一致时先查 design doc 再判定

## 收尾审查

全部批完成后，派发**最终代码审查 subagent** 检查整体实现：

- 所有需求满足
- 无遗漏文件或功能
- 代码一致性（跨批次的风格、命名、模式统一）
- 可以合并到 main

**审查 agent MUST 按以下优先级读取参考源**：

| 优先级 | 参考源 | 作用 |
|--------|--------|------|
| 1 | **design doc** (`docs/superpowers/specs/*-design.md`) | **WHAT + WHY**：技术决策、折衷方案、分层策略。代码偏离 spec 时先查 design doc，确认是否设计意图 |
| 2 | spec.md | **WHAT**：用户故事、功能需求、成功标准 |
| 3 | plan.md + tasks.md | **HOW**：架构决策、任务拆解 |
| 4 | 代码 | 验证对象 |

**关键规则**：代码与 spec 不一致时，**先查 design doc 再判断**。design doc 明确允许的偏离不算 bug。

收尾审查通过后，执行 `/superpowers:finishing-a-development-branch`。
