# Speckit + Superpowers 混合使用流程

**版本**: 2.1.0 | **日期**: 2026-05-13

适用于任何软件项目，从模糊想法到可交付代码的完整开发链路。

## 定位

| 工具 | 角色 | 输入 | 输出 |
|------|------|------|------|
| **Superpowers** | 前期探索 + 后期执行 | 模糊想法 | design doc → 可运行代码 |
| **Speckit** | 中期结构化 | 明确的需求描述 | spec.md → plan.md → tasks.md |

Superpowers 负责"想清楚要做什么"和"把它做出来"，Speckit 负责中间的结构化规范、一致性检查、任务编排。

## 完整流程（7 步）

```
模糊想法
    │
    ▼
┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
│1. 探索    │ → │2. 规范    │ → │3. 计划    │ → │4. 拆解    │ → │5. 检查    │ → │6. 准备    │ → │7. 执行    │
│brainstorm│   │specify   │   │plan      │   │tasks     │   │analyze   │   │环境      │   │subagent  │
└──────────┘   └──────────┘   └──────────┘   └──────────┘   └──────────┘   └──────────┘   └──────────┘
     │              │              │              │              │              │              │
     ▼              ▼              ▼              ▼              ▼              ▼              ▼
 design doc     spec.md        plan.md        tasks.md       分析报告      编译通过       可交付代码
 (WHAT+HOW)    (纯 WHAT)     research.md                   修复提交      pnpm install
                             data-model.md
                             contracts/
                             quickstart.md
```

---

## 步骤 1：探索（brainstorming）

**执行**: `/superpowers:brainstorming`

**做什么**：
- 读取项目现有代码、文档、规范
- 评估需求规模，过大的拆分为多个子项目
- 逐个提问澄清关键设计决策（一次一个问题，多项选择优先）
- 提出 2-3 种方案及权衡，给出推荐
- 逐节展示设计并获取用户批准

**产出 — design doc 两段式结构**，由固定标题分隔：

```markdown
## 数据库设计           ← 技术方案章节（WHAT + HOW）
## 后端设计             ← 数量任意，顺序任意
## 前端设计             ← speckit-plan 读取这些
## ...

## 功能描述（供 speckit-specify 使用）   ← 固定标题，不可改名
> 纯 WHAT 摘要，无任何技术术语          ← speckit-specify 读取这个
```

| 区域 | 消费者 | 约束 |
|------|--------|------|
| `## 功能描述` 之前的章节 | speckit-plan | WHAT + HOW 混合，可写任何技术细节 |
| `## 功能描述` 及之后 | speckit-specify | 纯 WHAT，自然语言，禁止框架名/库名/代码结构 |

**关键规则**：
- "功能描述" 标题是接口契约，改名会破坏下游工具链
- 功能描述只描述用户能做什么、系统提供什么
- brainstorming 结束必须 commit design doc

---

## 步骤 2：规范（speckit-specify）

**执行**: `/speckit-specify <"功能描述" 章节的纯 WHAT 文本>`

**输入**: 从 design doc "功能描述" 章节**复制文本**，不要传整个文件路径。传文件路径会导致技术细节渗入 spec。

**产出**：
- `spec.md` — 用户故事（P1/P2/P3 优先级）、验收场景、功能需求（FR-xxx）、成功标准（SC-xxx）、边界场景、关键实体、假设条件
- `checklists/requirements.md` — 质量检查清单

**注意**: speckit 模板的章节标题已翻译为中文，但 AI 指令注释和格式标记（`[P]`、`[US1]`、FR-/SC- 前缀）保持英文。

---

## 步骤 3：计划（speckit-plan）

**执行**: `/speckit-plan`

> ⚠️ **speckit-plan 不会自动读取 design doc**。执行时 AI MUST 主动读取 `docs/superpowers/specs/` 下对应的 design doc，将 "功能描述" 标题之前所有章节的技术决策作为技术上下文填充。不做这一步 = 重复讨论已决策的方案。

**产出**: `plan.md` + `research.md` + `data-model.md` + `contracts/` + `quickstart.md`

---

## 步骤 4：拆解（speckit-tasks）

**执行**: `/speckit-tasks`

**产出**: `tasks.md` — 按 User Story 分组的任务列表，标注依赖关系和并行标记 [P]。

**局限认知**: tasks.md 按 spec 的用户故事分组，不代表代码的真实耦合关系。例如 login/refresh/logout 在 spec 中是两个 User Story，但在代码中是同一个 AuthService/AuthController 实现。执行时相邻阶段可能自然合并——这是正常的，不是 tasks.md 的质量问题。

---

## 步骤 5：检查（speckit-analyze）

**执行**: `/speckit-analyze`

**为什么必须在实施前做**: tasks.md 基于 spec.md 生成，但 spec 不包含所有技术细节。analyze 在 spec/plan/tasks 之间做交叉检查，发现的缺口必须在实施前修复，否则边写边补会导致返工。

**常见发现**：
| 类型 | 示例 | 修复方式 |
|------|------|---------|
| data-model 缺表/缺字段 | 刷新令牌失效需要 `sys_refresh_token` 表未建模 | 补充到 data-model.md、plan.md、tasks.md |
| tasks 遗漏关键步骤 | token 刷新/登出的实现任务被拆分到另一阶段 | 合并或补充任务 |
| 成功标准无验证手段 | 性能指标无对应测试任务 | 标注验证阶段 |
| 术语不一致 | spec 用"凭证"，plan 用"token" | 确认是可接受的还是需要统一 |

---

## 步骤 6：环境准备

**为什么必须**: AI agent 在沙箱中通常无法执行包管理器、系统级安装命令。这些必须在派发 agent 之前由人（或主 session）完成。

| 检查项 | 验证命令 | agent 能做吗 |
|--------|---------|-------------|
| 语言运行时版本 | `java -version`、`node -v` | ❌ 不能安装 |
| 包管理器 | `gradle --version`、`pnpm --version` | ❌ 不能安装 |
| 首次编译通过 | `./gradlew compileJava` | ⚠️ 需要 JDK 路径 |
| 前端依赖安装 | `cd frontend && pnpm install` | ❌ 沙箱限制 |
| 敏感配置 | `.env` 文件就位 | ✅ 但不会自动创建 |

> **核心原则：编译通过是派发 agent 的前置条件。不要在编译不过的情况下继续加代码——问题会复合。**

额外建议：
- 统一配置方式（所有环境通过 `.env` + `application.yml` 默认值管理）
- 一个能跑通的最小启动流程，确保 agent 生成的代码不会因环境问题批量失败

---

## 步骤 7：执行（subagent-driven-development）

**执行**: `/superpowers:subagent-driven-development`

### 批量策略

tasks.md 的细粒度任务不适合逐条派发（浪费且容易冲突）。按文件边界和依赖关系合并为批：

```
Phase 1: 项目脚手架            ← 1-2 agent 并行
Phase 2: 基础设施              ← N agent 并行（互不重叠的文件组）
Phase 3-N: User Story 实现     ← 1 agent/Story（紧密耦合不拆分）
Phase N+1: 测试                ← N agent 并行（后端 + 前端）
Phase N+2: 收尾                ← 手动完成
```

### Agent 派发规则

| 规则 | 说明 |
|------|------|
| [P] 标记的独立文件组可并行派发 | 无文件重叠 = 无冲突 |
| 紧密耦合的模块不拆分 | 同一 Service/Controller/Config 必须同一个 agent |
| 每批必须 commit | 编译验证 + `git commit` = agent 的"完成"定义 |
| agent 无法执行 shell 时 | 主 session 接管编译和提交 |
| 不接受"文件创建了但没编译" | BLOCKED 状态，必须补完 |
| 同一批内出现失败 | 修复 → 编译 → 提交后再进下一批 |

### Agent 能力边界

| agent 能做 | agent 不能做（需主 session） |
|-----------|---------------------------|
| 创建/编辑源文件 | 执行 `gradle wrapper` |
| 运行编译命令（如有权限） | `brew install` |
| git commit | `pnpm install` |
| 读取项目文件 | 启动应用服务 |
| 搜索代码 | 浏览器测试（需 Playwright MCP） |

### 执行顺序原则

- **Phase 1→2→3 串行**，Phase 2 内部可大规模并行
- **每批提交后立即编译验证**，不在编译不过的情况下继续加代码
- **测试 Phase 可与收尾并行**，测试编写和最终验证可以覆盖

### 两阶段审查

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

#### Spec 合规审查

**目的**: 验证实现是否完全符合需求——不多不少。

**审查方式**: 派发独立的 spec-reviewer subagent，提供需求原文和实现报告，要求 reviewer **读代码**逐行验证，不信任 implementer 的报告。

| 检查维度 | 内容 |
|----------|------|
| 缺失 | 需求里的每一项都实现了吗？report 声称做了但实际没做？ |
| 多余 | 是否实现了需求没提的功能？是否过度设计？ |
| 误解 | 是否把需求理解错了？是否解决了错误的问题？ |

**审查人必须**：读代码，不信任报告。独立验证一切。

**结果**: ✅ Spec 合规 或 ❌ 具体问题清单（含文件:行号）

#### 代码质量审查

**目的**: 验证实现质量——干净、可测试、可维护。

**前置条件**: Spec 合规审查已通过。

| 检查维度 | 内容 |
|----------|------|
| 责任单一 | 每个文件只有一个明确的职责，接口清晰 |
| 分解合理 | 每个单元可独立理解和测试 |
| 结构一致 | 遵循 plan.md 定义的文件结构和项目现有模式 |
| 文件膨胀 | 新建文件是否过大？是否显著扩大了已有文件？ |

**结果**: ✅ 通过 或 ❌ 问题清单（Critical / Important / Minor）

#### 审查循环规则

- 审查发现的问题由**同一个 implementer subagent** 修复
- 修复后**重新审查**同一个 reviewer，直到通过
- **禁止**在审查未通过时进入下一批
- **禁止**跳过任一审查阶段
- Spec 合规审查未通过前，**禁止**派发代码质量审查

### 收尾审查

全部批完成后，派发**最终代码审查 subagent** 检查整体实现：

- 所有需求满足
- 无遗漏文件或功能
- 代码一致性（跨批次的风格、命名、模式统一）
- 可以合并到 main

收尾审查通过后，执行 `/superpowers:finishing-a-development-branch`。

---

## 接口契约

| 上游 | 下游 | 传递方式 | 关键约束 |
|------|------|---------|---------|
| brainstorming | speckit-specify | 复制 "功能描述" 文本 | 纯 WHAT，不传文件路径 |
| brainstorming | speckit-plan | AI 主动读取 "功能描述" 之前的章节 | speckit 不会自动读 design doc |
| speckit-analyze | 修复 | 分析报告 → 修复 spec/plan/tasks | 修复后才能执行 |

---

## 工具链选择

| 用这个 | 不用那个 | 理由 |
|--------|---------|------|
| brainstorming | speckit-clarify | brainstorming 已充分澄清，clarify 冗余 |
| speckit-plan | writing-plans | speckit-plan 与 spec/tasks 联动，artifact 一致性更好 |
| speckit-analyze | speckit-checklist | analyze 跨 artifact 检查，checklist 仅在单个 spec 内 |
| subagent-driven-development | executing-plans | 同一 session，快速迭代；任务量大时选 executing-plans |

---

## 反模式

| 反模式 | 后果 | 预防 |
|--------|------|------|
| 跳过 brainstorming 直接 speckit-specify | 需求未探索，spec 偏离意图 | 永远从 brainstorming 开始 |
| speckit-specify 传入整个 design doc 文件路径 | 技术细节渗入 spec | 只复制 "功能描述" 段落文本 |
| speckit-plan 不读 design doc | 重复讨论已决策方案 | 在 workflow 文档中明确标注 |
| 跳过 speckit-analyze | data-model 缺口带入实施，边写边补 | analyze → 修复 → 再执行 |
| 跳过环境准备 | agent 批量失败，反复修复环境问题 | 编译通过是派发 agent 的前置条件 |
| 编译不过继续加代码 | 错误复合，修复成本指数增长 | 每批提交后立即编译 |
| 僵硬按 tasks.md 阶段执行 | 相邻阶段重复实现 | 执行时识别自然合并，不僵化 |
| agent 文件创建了没编译 | 代码质量无保证 | "没编译 = BLOCKED，不是 DONE" |
| 跳过 spec 合规审查直接做代码审查 | spec 偏离未发现，实现与需求不一致 | 严格遵守审查顺序：先 spec 后质量 |
| 审查发现问题后跳过重新审查 | 修复可能不完全或引入新问题 | 修复后必须由同一个 reviewer 重新审查 |
| implementer 自审查替代正式审查 | 自己写的东西自己查不出所有问题 | 审查必须由独立 subagent 执行 |
