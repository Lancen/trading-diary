# Speckit + Superpowers 混合使用流程

**版本**: 1.0.0 | **日期**: 2026-05-11

## 定位

| 工具 | 擅长的 | 输入 | 输出 |
|------|--------|------|------|
| **Superpowers** | 前期探索、设计对齐、执行纪律、验证闭环 | 模糊想法 | design doc（WHAT + HOW） |
| **Speckit** | 结构化规范、模板驱动、artifact 一致性、任务编排 | 明确的 WHAT 描述 | spec.md → plan.md → tasks.md |

核心原则：**Superpowers 管前期和后期，Speckit 管中期结构化**。

## 完整流程

```
模糊想法
    │
    ▼
┌─────────────────────────────────┐
│ 1. brainstorming (superpowers)   │  探索、提问、对齐
│    → design doc                  │  产出：设计文档（§1-§6 为 WHAT+HOW，§7 为纯 WHAT 摘要）
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ 2. speckit-specify               │  将 design doc §7 纯 WHAT 描述转为结构化 spec
│    → spec.md                     │  产出：用户故事、功能需求、成功标准（纯 WHAT）
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ 3. speckit-plan                  │  生成实施计划
│    → plan.md                     │  引用 design doc §1-§6 中的技术决策
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ 4. speckit-tasks                 │  生成依赖排序的任务列表
│    → tasks.md                    │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ 5. executing-plans (superpowers) │  分批执行，验证检查点
│    + test-driven-development     │  每步验证，不跳步骤
│    + verification-before-complete│
└─────────────────────────────────┘
```

## 各阶段详解

### 1. brainstorming → design doc

**执行**: `/superpowers:brainstorming`

**职责**:
- 探索项目上下文
- 评估范围，必要时拆分子系统
- 逐个提问澄清需求
- 提出 2-3 种方案及权衡
- 逐节展示设计并获取批准

**产出 — design doc 两段式结构**:

| 章节 | 内容 | 用途 |
|------|------|------|
| §1-§6 | 数据库、后端、前端、脚手架（WHAT + HOW 混合） | speckit-plan 的技术参考 |
| §7 | 纯 WHAT 功能描述（无技术术语） | speckit-specify 的输入 |

**关键规则**:
- §7 必须不含技术实现细节（无框架名、无库名、无代码结构）
- §7 用自然语言描述用户能做什么、系统提供什么
- brainstorming 结束必须 commit design doc

### 2. speckit-specify → spec.md

**执行**: `/speckit-specify <§7 纯 WHAT 描述>`

**职责**:
- 将纯 WHAT 描述转为结构化规范
- 生成用户故事（带优先级和验收场景）
- 生成功能需求（FR-xxx，可测试）
- 生成成功标准（SC-xxx，可度量、技术无关）
- 识别关键实体和边界场景

**输入方式**:
```
/speckit-specify 搭建项目脚手架和用户认证体系。管理员通过种子数据初始化...
```

**注意**: 直接复制 design doc §7 内容，不要传整个 design doc 文件路径。

**产出**: `specs/NNN-feature-name/spec.md` + 质量检查清单

### 3. speckit-plan → plan.md

**执行**: `/speckit-plan`

> ⚠️ `/speckit-plan` 不会自动读取 design doc。执行时 AI MUST 主动读取 `docs/superpowers/specs/` 下对应的 design doc，将其 §1-§6 的技术决策（数据库设计、组件清单、配置方案）作为 plan.md 的技术上下文填充。

**职责**:
- 基于 spec.md 生成技术实施计划
- **主动读取 design doc §1-§6**，复用已决策的技术方案（不对已决策事项重复讨论）
- 产出分步骤的实施路线图

**输出**: `specs/NNN-feature-name/plan.md`

### 4. speckit-tasks → tasks.md

**执行**: `/speckit-tasks`

**职责**:
- 将 plan.md 分解为可执行的任务列表
- 标注任务依赖关系
- 每个任务应有明确的验证标准

**输出**: `specs/NNN-feature-name/tasks.md`

### 5. 执行（superpowers）

**执行**: `/superpowers:executing-plans` 或 `/superpowers:subagent-driven-development`

**职责**:
- 按 tasks.md 顺序执行
- 每个任务写测试 → 写代码 → 验证
- 关键节点请求 code review
- 完成前运行完整验证

**相关技能**:
- `test-driven-development` — 每个任务先写测试
- `verification-before-completion` — 声称完成前必须验证
- `requesting-code-review` — 关键节点请求审查
- `finishing-a-development-branch` — 完成后决定合并方式

## 接口契约

### brainstorming → speckit-specify

design doc §7 的格式规范：

```markdown
## §7 功能描述（供 speckit-specify 使用）

> 此章节是纯 WHAT 摘要，不含技术实现细节，专门作为 speckit 工具链的输入接口。

<2-3 句话的自然语言描述，覆盖：谁、能做什么、关键约束>
```

**示例**:
> 搭建项目脚手架和用户认证体系。管理员通过种子数据初始化（用户名 admin，密码从环境变量配置）。系统支持账号密码登录，验证成功后签发短期访问令牌和长期刷新令牌。令牌过期后可刷新，登出后刷新令牌失效。所有密码加密存储。预置 ADMIN 和 USER 两种角色用于权限控制。开发阶段启动时自动以管理员身份登录，开发者无需手动输入密码即可访问所有接口。生产模式下自动登录失效，所有接口必须携带有效令牌。

### design doc → speckit-plan

speckit-plan 执行时，AI 应读取 design doc §1-§6 获取技术决策上下文，避免重复讨论已确定的方案。

## 不做的事

| 只用一个 | 理由 |
|----------|------|
| brainstorming > speckit-clarify | brainstorming 已充分澄清，clarify 冗余 |
| speckit-plan > writing-plans | speckit-plan 与 spec/tasks 联动，artifact 一致性更好 |
| superpowers code-review > speckit-analyze | superpowers 的 review 更严格、更交互 |
| superpowers verification > speckit-checklist | 功能重叠，选更严格的 |

## 反模式

- 跳过 brainstorming 直接 speckit-specify → 需求未充分探索，spec 可能偏离意图
- speckit-specify 输入整个 design doc 文件 → 技术细节渗入 spec，违反"纯 WHAT"
- speckit-plan 不读 design doc → 重复讨论已确定的技术决策
- brainstorming 后跳过 speckit 直接写代码 → 缺少结构化 spec/plan/tasks，缺乏可追溯性
- 执行阶段不用 TDD → 违反宪法测试规范
