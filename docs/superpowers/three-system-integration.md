# Speckit + Superpowers + MattPocock Skills 三系统使用指南

**日期**: 2026-05-20 | **版本**: 1.0.0

## 一、三句话定位

| 系统 | 一句话 | 管什么 |
|------|--------|--------|
| **Speckit** | 流程框架 — 把需求变成可执行的任务清单 | 开发流程的结构化（spec → plan → tasks） |
| **Superpowers** | 编排引擎 — 探索需求 + 调度执行 + 收尾归档 | 前期 brainstorm 和后期 subagent 编排 |
| **MattPocock Skills** | 工程纪律 — 写代码时的行为约束和质量标准 | 编码行为本身的质量（TDD、调试、沟通） |

**记忆口诀**：Speckit 管流程走到哪一步，Superpowers 管谁来做怎么做，MattPocock 管做的过程对不对。

---

## 二、按开发阶段 — 时间线视角

```
                         Speckit 的阶段门控
        ┌──────────────────────────────────────────────────┐
        │  specify  →  plan  →  tasks  →     implement     │
        └──────────────────────────────────────────────────┘
           ↑          ↑         ↑              ↑
           │          │         │              │
    ┌──────┴──────┐   │         │     ┌────────┴────────┐
    │ Superpowers │   │         │     │  MattPocock     │
    │ brainstorm  │   │         │     │  tdd            │
    │ grill-me    │   │         │     │  diagnose       │
    └─────────────┘   │         │     │  caveman        │
                      │         │     │  handoff        │
              ┌───────┴──┐  ┌───┴──────┐  └────────────────┘
              │Superpowers│  │ Speckit  │
              │ + Speckit │  │ tasks    │
              │ plan      │  │          │
              └───────────┘  └──────────┘
```

### 阶段 1：需求澄清（探索期）

**主力**：`superpowers:brainstorming` + `mattpocock:grill-me`

| 场景 | 用哪个 | 原因 |
|------|--------|------|
| 完全不知道要做什么 | brainstorming | 开放式探索，输出 design doc |
| 大致知道但怕遗漏 | grill-me | 追问式收敛，穷举决策分支 |
| 需求涉及多个系统边界 | brainstorming → grill-me | 先探索后收敛 |
| 需求一句话就说清了 | 跳过，直接 specify | 不需要过度设计 |

**本项目实践**：从 brainstorming 开始（已有 workflow），如果 design doc 出来后有 3 个以上待决策点，加一轮 grill-me。

### 阶段 2：规范编写（Spec）

**主力**：`speckit:specify`，纯 WHAT，不涉及技术方案。

此时 mattpocock 和 superpowers **不介入**。specify 是机械性的格式转换，不需要工程判断。

### 阶段 3：技术方案（Plan）

**主力**：`speckit:plan`，产出 plan.md + data-model.md + contracts/。

可选增强：如果涉及架构层面的决策（表结构变更、API 契约变更、跨模块重构），在 plan 之后用 `mattpocock:grill-with-docs` 做一轮架构审查并写入 ADR。

### 阶段 4：任务拆解（Tasks）

**主力**：`speckit:tasks`，按垂直切片拆解。

可选增强：用 `speckit:analyze` 做跨文件一致性检查（spec ↔ plan ↔ tasks 是否对齐）。

### 阶段 5：编码执行（Implement）

**这是 mattpocock/skills 的主场。**

```
每个 task:
  ┌─────────────────────────────────────────┐
  │  mattpocock:tdd                          │
  │  RED → GREEN → REFACTOR                  │
  │     │                                    │
  │     ├── 遇到 Bug? → mattpocock:diagnose  │
  │     ├── token 超了? → mattpocock:caveman │
  │     └── 完成 task? → mattpocock:handoff  │
  └─────────────────────────────────────────┘
```

**Superpowers 的角色**：用 `subagent-driven-development` 或 `dispatching-parallel-agents` 调度多个 task 并行执行。Superpowers 管"派谁做"，MattPocock 管"怎么做"。

### 阶段 6：收尾归档

**主力**：`superpowers:finishing-a-development-branch` + `superpowers:verification-before-completion`。

用 `mattpocock:handoff` 生成上下文交接文档（如果后续还有其他人在这个分支上工作）。

---

## 三、按职责分工 — "What / How / Quality" 视角

```
         WHAT                     HOW                      QUALITY
    ┌──────────────┐      ┌──────────────┐        ┌──────────────────┐
    │   Speckit    │      │ Superpowers  │        │  MattPocock      │
    │              │      │              │        │                  │
    │ spec.md      │      │ brainstorm   │        │ tdd              │
    │ plan.md      │      │ subagent     │        │ diagnose         │
    │ tasks.md     │      │ dispatch     │        │ grill-me         │
    │ analyze      │      │ finish       │        │ caveman          │
    │              │      │ verify       │        │ handoff          │
    └──────────────┘      └──────────────┘        │ improve-arch     │
         │                      │                 └──────────────────┘
         ▼                      ▼                        ▼
    做什么、怎么做          谁来探索、谁来做           做得对不对、好不好
    （文档资产）            （编排调度）               （行为约束）
```

---

## 四、功能重叠与选择

| 重叠领域 | Speckit | Superpowers | MattPocock | 选谁 | 理由 |
|---------|---------|-------------|------------|------|------|
| 需求澄清 | clarify | brainstorming | grill-me | brainstorming + grill-me | clarify 是问卷式，brainstorming 是开放式探索，grill-me 是追问式收敛 |
| 任务拆解 | tasks | — | to-issues | tasks | 与 speckit 流程联动，artifact 一致性更好 |
| 计划编写 | plan | writing-plans | — | plan | 与 spec/tasks 联动 |
| 代码生成 | — | subagent-driven | tdd | 两个一起用 | subagent 调度并发，tdd 约束单个 agent 行为 |
| 调试 | — | systematic-debugging | diagnose | diagnose | 6 阶段更结构化 |
| 跨文件检查 | analyze | — | — | analyze | spec ↔ plan ↔ tasks 一致性，独有的 |
| 收尾 | — | finishing-a-branch | — | finishing | 独有的 PR/合并流程 |

---

## 五、按场景 — 实用组合速查

### 场景 A：新功能开发（标准流程）

```
brainstorming → specify → plan → tasks → analyze → tdd(+subagent) → verify → finish
└─Superpowers─┘ └───────Speckit───────┘ └──MattPocock──┘ └Superpowers┘
```

### 场景 B：Bug 修复（快速）

```
diagnose → tdd → verify
└MattPocock┘  └Superpowers┘
```

不经过 speckit，因为 Bug 修复不需要 spec/plan/tasks 全套流程。

### 场景 C：小改动（单文件、纯机械）

```
直接 Edit/Write（CLAUDE.md A6 规则）
```

三套系统都不用。不要过度流程化。

### 场景 D：架构重构（高风险）

```
brainstorming → grill-with-docs → plan → tasks → tdd(+subagent) → improve-codebase-architecture → verify
└─Superpowers─┘ └─MattPocock──┘ └Speckit┘ └──MattPocock──┘ └──MattPocock──┘ └Superpowers┘
```

比标准流程多了 grill-with-docs（写 ADR）和 improve-codebase-architecture（事后体检）。

### 场景 E：长任务 token 预算紧张

```
caveman（开启压缩模式）→ handoff（关键节点保存上下文）
└──────MattPocock──────┘
```

在 speckit 流程的 implement 阶段穿插使用，不改变流程结构。

---

## 六、反模式警示

| 反模式 | 后果 | 正确做法 |
|--------|------|---------|
| 每个改动都走三套系统全流程 | 流程成本 > 开发成本 | 场景 A/B/C/D 按复杂度选择 |
| 用 grill-me 替代 brainstorming | grill-me 是收敛性的，不适合开放式探索 | 先 brainstorming 再 grill-me |
| 用 tdd 替代 speckit tasks | tasks.md 提供全局视图，tdd 只管当前切片 | tasks.md 是地图，tdd 是走路方式 |
| subagent 不加 tdd 约束直接写代码 | agent 跳过测试直接实现 | subagent 派发时必须要求 tdd |
| analyze 通过就跳过 verify | analyze 检查文档一致性，verify 检查功能正确性 | 两者互补，不能替代 |
| caveman 模式下做架构决策 | 压缩模式丢失关键上下文 | 重要决策前关闭 caveman |
| 三套系统的 skill 全部预加载 | 上下文膨胀，token 浪费 | 按阶段按需加载 |

---

## 七、本项目当前状态

已有 workflow（`speckit-superpowers-workflow.md`）覆盖了 Speckit + Superpowers 的 8 步流程。MattPocock Skills 的融入点：

| 现有步骤 | 融入的 MattPocock Skill | 何时用 |
|---------|------------------------|--------|
| 步骤 1（brainstorming） | grill-me | design doc 有 3+ 待决策点时追加 |
| 步骤 3（plan） | grill-with-docs | 涉及架构决策时写 ADR |
| 步骤 7（执行） | tdd | 每个 task 必须 |
| 步骤 7（执行） | diagnose | 遇到 Bug 时 |
| 步骤 7（执行） | caveman | token 过半时 |
| 步骤 8（归档） | handoff | 跨会话长任务 |
| 独立（非流程内） | improve-codebase-architecture | 定期体检，不绑定特定步骤 |
