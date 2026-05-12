# Speckit + Superpowers 混合使用流程

**版本**: 1.2.0 | **日期**: 2026-05-12

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
│    → design doc                  │  产出：技术方案章节 + "功能描述" 章节（纯 WHAT 摘要）
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ 2. speckit-specify               │  读取 "功能描述" 章节 → 转为结构化 spec
│    → spec.md                     │  产出：用户故事、功能需求、成功标准（纯 WHAT）
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ 3. speckit-plan                  │  读取 "功能描述" 之前的章节 → 填充技术上下文
│    → plan.md + research.md       │  复用已决策的技术方案，不重复讨论
│    + data-model.md + contracts/  │
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
│ 5. speckit-analyze               │  跨 artifact 一致性检查（spec/plan/tasks）
│    → 修复 spec/plan/tasks 不一致  │  必须在实施前执行，避免带着矛盾写代码
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ 6. 环境准备（一次性）             │  JDK 版本、Gradle wrapper、pnpm install
│                                  │  这些对 AI agent 透明执行至关重要
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ 7. 执行 (superpowers)            │  按 tasks.md 分批执行
│    subagent-driven-development   │  每批提交 + 编译验证，不允许跳过
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

| 区域 | 内容 | 用途 |
|------|------|------|
| `## 功能描述` 之前的章节 | 数据库、后端、前端、脚手架等（WHAT + HOW 混合） | speckit-plan 的技术参考 |
| `## 功能描述（供 speckit-specify 使用）` | 纯 WHAT 功能描述（无技术术语） | speckit-specify 的输入 |

> 两段由固定标题 `## 功能描述（供 speckit-specify 使用）` 分隔。此标题之前的章节数量可以任意，标题之后必须紧跟纯 WHAT 摘要。

**关键规则**:
- "功能描述" 章节必须不含技术实现细节（无框架名、无库名、无代码结构）
- "功能描述" 用自然语言描述用户能做什么、系统提供什么
- 章节序号不重要，标题文字是接口契约
- brainstorming 结束必须 commit design doc

### 2. speckit-specify → spec.md

**执行**: `/speckit-specify <"功能描述" 章节的纯 WHAT 文本>`

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

**注意**: 直接复制 design doc 中 "功能描述" 章节的文本内容，不要传整个 design doc 文件路径。

**产出**: `specs/NNN-feature-name/spec.md` + 质量检查清单

### 3. speckit-plan → plan.md

**执行**: `/speckit-plan`

> ⚠️ `/speckit-plan` 不会自动读取 design doc。执行时 AI MUST 主动读取 `docs/superpowers/specs/` 下对应的 design doc，将其中 `## 功能描述` 标题之前所有章节的技术决策作为 plan.md 的技术上下文填充。

**职责**:
- 基于 spec.md 生成技术实施计划
- **主动读取 design doc 中 "功能描述" 之前的所有章节**，复用已决策的技术方案（不对已决策事项重复讨论）
- 产出分步骤的实施路线图

**输出**: `specs/NNN-feature-name/plan.md` + `research.md` + `data-model.md` + `contracts/` + `quickstart.md`

### 4. speckit-tasks → tasks.md

**执行**: `/speckit-tasks`

**职责**:
- 将 plan.md 分解为可执行的任务列表
- 标注任务依赖关系和并行标记 [P]
- 按 User Story 分组，每个 Story 独立可测试

**输出**: `specs/NNN-feature-name/tasks.md`

**已知局限**: tasks.md 的阶段划分基于 spec 的用户故事，不完全等于代码的耦合关系。例如认证体系的 login/refresh/logout 在代码层面由同一个 AuthController/AuthService 实现，tasks.md 将它们拆到 Phase 3 和 Phase 5，执行时会自然合并——这属于正常现象，不是 tasks.md 的质量问题。

### 5. speckit-analyze → 一致性修复

**执行**: `/speckit-analyze`

**职责**:
- 跨 spec.md / plan.md / tasks.md 检查一致性
- 发现的 CRITICAL 问题必须修复后再实施
- MEDIUM/LOW 问题可记录后进入实施，但不建议跳过

**常见发现**:
- data-model 缺表/缺字段 → 补充到 data-model.md 和 plan.md
- tasks.md 遗漏关键任务 → 补充
- spec 成功标准无对应验证任务 → 明确验证阶段
- 技术术语在 spec 中泄漏 → 移除

**输出**: 分析报告 + 修复提交

### 6. 环境准备（一次性）

在执行代码任务之前，确保以下工具链就位。这些步骤 AI agent 无法跨沙箱执行，需要用户完成：

| 检查项 | 命令 | 为什么必须 |
|--------|------|-----------|
| JDK 版本 | `java -version` → 17+ | Lombok + Spring Boot 3.3.x 要求 |
| JAVA_HOME | `echo $JAVA_HOME` | Gradle 找不到 JDK 会静默失败 |
| Gradle wrapper | `gradle wrapper --gradle-version 8.10` | 系统 Gradle 版本可能不兼容 Spring Boot 插件 |
| 编译验证 | `./gradlew compileJava` | 确认所有源文件语法正确 |
| pnpm install | `cd frontend && pnpm install` | AI 沙箱通常不能执行包管理器 |

> **原则**：**编译通过是 agent 派发的前置条件**。不要在编译不过的情况下继续加代码——问题会复合。

### 7. 执行（subagent-driven-development）

**执行**: `/superpowers:subagent-driven-development`

**批量策略**：tasks.md 的细粒度任务不适合逐条派发（68 个任务 = 68 次 agent 调用 = 过于昂贵）。按文件边界和依赖关系合并批次：

```text
Phase 1: 后端脚手架 (T001-T004)  ← 1 agent
Phase 1: 前端脚手架 (T005-T006)  ← 1 agent（与后端并行）
Phase 2: 基础设施（21 任务）      ← 4 agent 并行
         ├ Flyway 迁移           ← 独立
         ├ 实体类                ← 5 个文件，独立
         ├ Mapper + ApiResponse   ← 6 个文件，独立
         └ 异常体系 + VO/Request  ← 8 个文件，独立
Phase 3: 管理员登录（10 任务）    ← 1 agent（核心认证链，耦合紧密不能拆）
Phase 4: 自动登录（3 任务）       ← 1 agent
Phase 5: 令牌刷新登出             ← 已在 Phase 3 中一并实现（自然的阶段重叠）
Phase 6: 前端 UI（11 任务）       ← 1 agent
Phase 7: 测试                     ← 2 agent 并行（后端 + 前端）
Phase 8: 收尾                     ← 手动完成
```

**Agent 派发规则**：

| 规则 | 说明 |
|------|------|
| [P] 标记的文件组可并行派发 | 无文件重叠 = 无冲突 |
| 紧密耦合的模块不拆分 | SecurityConfig + JwtAuthFilter + AuthService 必须同一 agent |
| 每批必须 commit | 编译验证 + `git commit` 是 agent 的"完成"定义 |
| 不接受"文件创建了但没编译" | 这是 BLOCKED 状态，需补完 |
| agent 无法执行 bash 时 | 主 session 接管编译和提交 |

**Agent 能力边界**：

AI agent 在沙箱中可能无法执行：
- `./gradlew`（Gradle wrapper 权限问题）
- `pnpm install`（网络或沙箱限制）
- `brew install`（系统级包管理）
- `git push`（远端认证）

主 session 应负责环境准备和最终验证，agent 负责代码生成和文件创建。

## 接口契约

### brainstorming → speckit-specify

design doc 中 `## 功能描述` 章节的格式规范：

```markdown
## 功能描述（供 speckit-specify 使用）

> 此章节是纯 WHAT 摘要，不含技术实现细节，专门作为 speckit 工具链的输入接口。

<2-3 句话的自然语言描述，覆盖：谁、能做什么、关键约束>
```

> ⚠️ 章节标题必须是 `## 功能描述（供 speckit-specify 使用）`，不能修改。这是 brainstorming 和 speckit 之间的接口契约。

### design doc → speckit-plan

speckit-plan 执行时，AI 应读取 design doc 中 `## 功能描述` 标题之前的所有章节获取技术决策上下文，避免重复讨论已确定的方案。

## 不做的事

| 只用一个 | 理由 |
|----------|------|
| brainstorming > speckit-clarify | brainstorming 已充分澄清，clarify 冗余 |
| speckit-plan > writing-plans | speckit-plan 与 spec/tasks 联动，artifact 一致性更好 |
| speckit-analyze > speckit-checklist | analyze 覆盖更全面：跨 artifact 一致性 + 宪法对齐 + 覆盖率 |
| superpowers verification > speckit-checklist | 功能重叠，选更严格的 |

## 必须做但文档没写的事

| 易遗漏 | 为什么重要 |
|--------|-----------|
| `speckit-analyze` 后的修复 | 发现的 data-model 缺口（如 sys_refresh_token 表）不修复会导致实施时逻辑不完整 |
| 环境 JDK 验证 | JDK 8 无法编译 Spring Boot 3.x，Lombok 会报 `NoSuchFieldException: TypeTag` |
| `./gradlew compileJava` 每次提交前 | 积压的编译错误在最后一起修 = 痛苦 |
| `.env` 支持 | 项目需要 DB 连接但密码不能进 git——`.env` + `DotenvConfig` 一次性配好 |

## 反模式

- 跳过 brainstorming 直接 speckit-specify → 需求未充分探索，spec 可能偏离意图
- speckit-specify 输入整个 design doc 文件 → 技术细节渗入 spec，违反"纯 WHAT"
- speckit-plan 不读 design doc → 重复讨论已确定的技术决策
- brainstorming 后跳过 speckit 直接写代码 → 缺少结构化 spec/plan/tasks，缺乏可追溯性
- 执行阶段不用 TDD → 违反宪法测试规范
- tasks.md 的阶段划分僵硬执行 → 阶段重叠是正常的，login 和 refresh 本来就是同一个 Service
- 编译不过继续加代码 → 问题复合，修复成本指数增长
