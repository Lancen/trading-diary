# Speckit + Superpowers 混合使用流程

**版本**: 1.2.0 | **日期**: 2026-05-12

适用于任何软件项目。从模糊想法到可交付代码的完整链路。

## 定位

| 工具 | 角色 | 输入 | 输出 |
|------|------|------|------|
| **Superpowers** | 前期探索 + 后期执行 | 模糊想法 | design doc → 可运行代码 |
| **Speckit** | 中期结构化 | 明确的 WHAT 描述 | spec.md → plan.md → tasks.md |

Superpowers 负责"想清楚要做什么"和"把它做出来"；Speckit 负责中间的结构化规范、一致性检查、任务编排。

## 完整流程

```
模糊想法
    │
    ▼
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│1. 探索    │ → │2. 规范    │ → │3. 计划    │ → │4. 拆解    │ → │5. 检查    │ → │6. 准备    │ → │7. 执行    │
│brainstorm│   │specify   │   │plan      │   │tasks     │   │analyze   │   │环境      │   │子agent   │
└──────────┘    └──────────┘    └──────────┘    └──────────┘    └──────────┘    └──────────┘    └──────────┘
     │               │               │               │               │               │               │
     ▼               ▼               ▼               ▼               ▼               ▼               ▼
 design doc      spec.md         plan.md         tasks.md        分析报告        编译通过        可交付代码
 (WHAT+HOW)     (纯 WHAT)      research.md                      修复提交        pnpm install
                checklist      data-model.md
                               contracts/
                               quickstart.md
```

## 各步骤详解

### 步骤 1：探索（brainstorming）

**执行**: `/superpowers:brainstorming`

**做什么**:
- 读取项目现有代码、文档、规范
- 评估需求规模，过大的拆分为多个子项目
- 逐个提问澄清关键设计决策
- 提出 2-3 种方案及权衡，给出推荐
- 逐节展示设计并获取批准

**产出格式** — design doc 分为两个区域，由固定标题分隔：

```markdown
## 数据库设计           ← 这些是技术方案章节（WHAT + HOW）
## 后端设计             ← 顺序、数量随意
## 前端设计             ← speckit-plan 读取这些
## ...

## 功能描述（供 speckit-specify 使用）   ← 固定标题，不可改名
> 纯 WHAT 摘要，无框架名/库名/代码结构    ← speckit-specify 读取这个
```

| 区域 | 读消费者 | 约束 |
|------|---------|------|
| `## 功能描述` 之前 | speckit-plan | WHAT + HOW 混合，技术细节都可以写 |
| `## 功能描述` 及之后 | speckit-specify | 纯 WHAT，自然语言，面向业务干系人 |

**关键规则**:
- "功能描述" 标题是接口契约，不能改名
- 内容不含框架名、库名、数据库名、代码结构
- 只描述用户能做什么、系统提供什么

### 步骤 2：规范（speckit-specify）

**执行**: `/speckit-specify <"功能描述" 的文本>`

**输入**: 从 design doc "功能描述" 章节复制的纯 WHAT 文本，**不要传整个文件路径**

**产出**:
- `spec.md` — 用户故事（P1/P2/P3 优先级）、验收场景、功能需求（FR-xxx）、成功标准（SC-xxx）、边界场景、关键实体、假设条件
- `checklists/requirements.md` — 质量检查清单

### 步骤 3：计划（speckit-plan）

**执行**: `/speckit-plan`

> AI MUST 主动读取 design doc 中 "功能描述" 标题之前的所有章节，获取技术决策上下文。speckit 不会自动做这一步。

**产出**: `plan.md` + `research.md` + `data-model.md` + `contracts/` + `quickstart.md`

### 步骤 4：拆解（speckit-tasks）

**执行**: `/speckit-tasks`

**产出**: `tasks.md` — 按 User Story 分组的任务列表，标注依赖和并行标记 [P]

**局限认知**: tasks.md 按 spec 的用户故事分组，不代表代码的真实耦合关系。执行时相邻阶段可能自然合并——这是正常的，不是 tasks.md 的质量问题。

### 步骤 5：检查（speckit-analyze）

**执行**: `/speckit-analyze`

**为什么必须**: tasks.md 是基于 spec.md 生成的，但 spec 不包含所有技术细节。analyze 在三份 artifact（spec/plan/tasks）之间做交叉检查，发现缺口必须在实施前修复。

**常见发现**:
- data-model 缺表/缺字段 → 补充
- tasks 遗漏关键步骤 → 补充
- 成功标准无对应验证手段 → 标注验证阶段
- 术语不一致（spec 用"凭证"，plan 用"token"）→ 确认是可接受的还是需要统一

### 步骤 6：环境准备

在执行代码之前，确保以下条件满足。这些步骤 AI agent 通常无法完成：

| 检查项 | 为什么必须 |
|--------|-----------|
| 语言运行时版本 | 框架和库有最低版本要求 |
| 包管理器安装 | agent 沙箱通常不能执行 `install` |
| 首次编译通过 | 在编译过的代码上开发，不会复合错误 |
| 敏感配置就位 | `.env` / 密钥 / 数据库连接等信息不会进 git |

> **原则**: 编译通过是派发 agent 的前置条件。不要在编译不过的情况下继续加代码。

### 步骤 7：执行（subagent-driven-development）

**执行**: `/superpowers:subagent-driven-development`

**核心策略**：tasks.md 的细粒度任务不适合逐条派发。应合并为合理的批量，原则是：

| 规则 | 说明 |
|------|------|
| [P] 标记的独立文件组可以并行派发 | 无文件重叠即无冲突 |
| 紧密耦合的模块不拆分 | 同一 Service/Controller/Config 的代码必须同一个 agent |
| 每批必须 commit | 编译验证 + 提交是"完成"的定义 |
| agent 无法执行 shell 时 | 主 session 接管编译和提交 |
| 同一批内出现失败 | 修复 → 编译 → 提交后再进入下一批 |

**Agent 能力边界**：agent 沙箱通常无法执行 `gradle`/`pnpm`/`brew` 等命令。职责划分——agent 负责代码生成，主 session 负责编译和提交。

**与 executing-plans 的选择**:

| 选 subagent-driven-development 当 | 选 executing-plans 当 |
|----------------------------------|---------------------|
| 在当前 session 一口气完成 | 需要跨 session、分阶段审查 |
| 任务间独立性强、可并行派发 | 首次实现、方案可能还有变数 |
| 方案已充分讨论，无需每阶段停下来 | 想每完成一个 User Story 就 review 一次 |

## 接口契约总结

| 上游 | 下游 | 传递方式 | 关键约束 |
|------|------|---------|---------|
| brainstorming | speckit-specify | 复制 "功能描述" 文本 | 纯 WHAT，不传文件路径 |
| brainstorming | speckit-plan | AI 主动读取 "功能描述" 之前的章节 | speckit 不会自动读 design doc |
| speckit-analyze | 修复流程 | 分析报告 → 手动/自动修复 spec/plan/tasks | 修复后才能执行 |

## 不做的事

| 替代 | 被替代 | 理由 |
|------|--------|------|
| brainstorming | speckit-clarify | brainstorming 已充分澄清 |
| speckit-plan | writing-plans | speckit-plan 与 spec/tasks 联动，artifact 一致性更好 |
| speckit-analyze | speckit-checklist | analyze 覆盖更全面：跨 artifact + 宪法 |
| superpowers verification | speckit-checklist | 功能重叠，选更严格的 |

## 反模式

| 反模式 | 后果 |
|--------|------|
| 跳过 brainstorming 直接 speckit-specify | 需求未探索，spec 偏离意图 |
| speckit-specify 输入整个 design doc 文件 | 技术细节渗入 spec |
| speckit-plan 不读 design doc | 重复讨论已决策方案 |
| brainstorming 后直接写代码，跳过 speckit | 缺结构化 spec/plan/tasks，无可追溯性 |
| 跳过 speckit-analyze | data-model 缺口带入实施，边写边补 |
| 跳过环境准备 | agent 批量失败，反复修复环境问题 |
| 编译不过继续加代码 | 错误复合，修复成本指数增长 |
