# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

<!-- META: 本文不超过 250 行。新增规则前评估旧规则是否可退役。 -->

## Project

trading-diary — 交易日记应用。项目处于初始化阶段（2026-05-08 初始化），规范文件已建立，业务代码待开发。

## 技术栈

### 后端

| 领域 | 选型 |
|------|------|
| 语言 | Java 17+ (推荐 21 LTS) |
| 框架 | Spring Boot 3.2.x |
| ORM | MyBatis-Plus 3.5+ |
| 数据库 | MySQL 8.0+ |
| 构建工具 | Gradle |
| 测试 | JUnit 5 + Mockito + H2 + MockMvc + @MybatisPlusTest |
| API 文档 | SpringDoc (Swagger/OpenAPI) |
| 数据库迁移 | Flyway |
| 安全 | Spring Security + JWT |
| 测试策略 | 新 API MUST 有集成测试，关键流程 MUST 有 E2E |

### 前端

| 领域 | 选型 |
|------|------|
| 框架 | Next.js 14+ (App Router) |
| 语言 | TypeScript |
| 样式 | TailwindCSS 3+ |
| 组件库 | shadcn/ui |
| 图表 | lightweight-charts (TradingView) + Recharts |
| 表单 | react-hook-form + zod |
| HTTP | ky |
| 状态 | zustand + TanStack Query |
| 单元测试 | Vitest + @testing-library/react |
| E2E | Playwright（`playwright.config.ts`，`e2e/`） |
| PWA | next-pwa |
| 构建工具 | pnpm |

## 行为准则 — 编码过程约束

以下准则约束 AI 的编码行为。权衡：偏向谨慎而非速度，简单任务可自行判断。

### A1. 编码前先思考

**不假设，不隐藏困惑，揭示权衡。**

- 明确陈述假设。不确定时就问。
- 存在多种理解时全部呈现，不默默选择其一。
- 有更简单的方案就说出来，必要时据理力争。

### A2. 精准修改，局部简化

**默认只改目标代码。但当目标修改自然暴露大段可简化代码时（如涉及 50 行实为 10 行可表达），允许一并压缩。不确定时，优先精准修改。**

- 不顺手改相邻代码、不预加"灵活性"
- 不写不可能发生场景的错误处理
- 不重构没坏的东西
- 孤立 import/变量/函数必须清理
- 发现无关废弃代码 → 提出来确认，确认后清理

### A3. 目标驱动

把模糊指令转化为可验证结果。speckit 任务以 spec.md 的 AC-/SC- 为准；非 speckit 任务自定验收标准。

### A4. 中英混合文本格式化

**所有包含代码标识符的中文描述，标识符必须用 `` ` `` 包裹。**

tasks.md、plan.md、spec.md 等文档中，Java 类名、方法名、文件路径、表名、字段名、枚举值、注解名一律用 `` ` `` 包裹。纯中文内容不加反引号。

## 协作准则 — 与用户的交互约束

### B1. 疑问输入安全阀

**用户输入含 `?` / `？` / `吗` / `呢` / `怎样` / `如何` → 只回答，不修改任何文件。**

用户输入以 `能不能` / `可以` / `是否` 开头 → 同理，只回答不修改。

收到用户输入后，在决定任何动作之前，先扫描上述标记。此规则优先级高于 B2（修改确认）和 A2（精准修改）——不论输入内容看起来多像"指出一个需要修复的问题"，只要有疑问标记，就是问题不是指令。

### B2. 修改代码前确认

**需要用户确认再执行**：
- 新建/删除文件
- 修改业务逻辑
- 派发 Agent（Agent 会创建/编辑文件）
- 任何不确定的变更

**无需确认可直接执行**：
- typo、注释修改
- 按已有模式添加字段/方法（如给 Entity 加属性、给 Mapper 加已有查询模式的接口）
- 按用户明确指定的步骤执行（"把 A.java 的 foo 改成 bar"）
- 清理此前修改产生的孤立 import

不确定是否属于快速通道 → 走确认流程。**"继续"** = 执行当前讨论中已明确的下一步，完成后汇报 + 询问下一步。

### B3. 精确暂存，禁止通配 commit

**禁止 `git add -A`、`git add .`、`git add *`、`git commit -a`。每次 commit 按文件路径逐一暂存。**

- `git add <具体文件>` 逐个暂存，每个文件都经过确认
- commit 前用 `git diff --stat` 检查变更范围
- 发现陌生文件（未创建、未讨论）立即排查来源
- agent 产出的文件必须逐项审查后再加入暂存

### B4. 实现代码必须经过测试

**新 Service/Controller 方法 → 必须有对应单元测试（JUnit + Mockito）。**

测试必须在实现提交之前编写并运行通过（RED → GREEN）。speckit 流程中的 TDD 纪律详见 [workflow 步骤 7](docs/superpowers/workflow-steps/step-07-execution.md)。

## 流程准则 — 多步协作的门禁

### C1. 开发流程强制入口

**所有 speckit + superpowers 混合开发流程 MUST 遵循 [workflow 文档](docs/superpowers/speckit-superpowers-workflow.md)。**

收到开发指令后，匹配 workflow 步骤速查表确定当前步骤，按需加载对应步骤的详细文件。执行前按速查表"前置产物"列检查文件是否存在，缺失时警告但不阻断——由人决定是否继续。workflow 文档中定义的接口契约、审查门禁、TDD 纪律为强制规则，不可跳过。

### C2. 审查门禁

**每批实现完成后、进入下一批前，MUST 在对话中展示审查门禁状态：**

```
上一批完成自检：
  Spec 合规审查：✅ 已通过 / ❌ 未执行
  代码质量审查：✅ 已通过 / ❌ 未执行
  审查问题修复：✅ 已修复 / ❌ 未修复 / — 无问题
```

任何一项为 ❌ → BLOCKED → 补完审查 → 才能问"继续？"。

**审查分级**：
| 等级 | 适用 | 审查次数 |
|------|------|---------|
| Level 1（轻量） | 纯实体/Mapper/VO/DTO/枚举/常量 | 1 次审查（spec + 质量合并） |
| Level 2（标准） | 含 Service/Controller/安全/业务逻辑 | 2 次审查（先 spec 后质量） |

例外：Level 1 批包含安全相关配置（SecurityConfig 等）→ 升级到 Level 2。

审查结论必须来自独立 subagent。审查 agent 的 prompt MUST 包含 design doc 路径。审查发现的问题必须在进入下一批前修复并重新审查。

### C3. 派发 agent 前验证环境

**派发任何实现 agent 之前，MUST 运行 `scripts/check-env.sh`，确认全部通过。**

任何一项不满足 → 修复环境 → 再派发。不在不可用的环境上加代码。

## 代码规范引用

以下规范位于外部文档，按需加载：
- **数据库表规范**（`id`/`created_at`/`updated_at`/`is_deleted`、日志表例外等）：[technical-standards.md §2.1](docs/standards/technical-standards.md)
- **代码标准、API、安全、事务、日志、测试、部署、监控**：[technical-standards.md](docs/standards/technical-standards.md)
- **完整 TDD 纪律、审查流程、Agent 派发规则**：[workflow 步骤 7](docs/superpowers/workflow-steps/step-07-execution.md)

## 项目结构

```text
# 后端（详细结构见 docs/standards/technical-standards.md §1.1）
src/main/java/com/tradingdiary/
├── config/     controller/     service/       mapper/
├── entity/     model/vo/       model/request/ exception/
└── aspect/     util/

# 前端
frontend/
├── src/
│   ├── app/         # Next.js App Router（页面路由）
│   ├── components/  # 通用组件
│   ├── lib/         # 工具函数、API 调用（ky 封装）
│   └── hooks/       # 自定义 hooks
├── public/          # 静态资源
└── package.json

# E2E 测试（项目根目录）
e2e/
└── *.spec.ts        # Playwright 用例
playwright.config.ts
```

## 常用命令

```bash
# 构建项目
./gradlew build

# 运行测试
./gradlew test

# 启动应用（开发环境）
./gradlew bootRun --args='--spring.profiles.active=dev'

# 代码检查
./gradlew check

# 仅编译
./gradlew compileJava

# 前端（frontend/ 目录下）
pnpm install        # 安装依赖
pnpm dev            # 启动开发服务器
pnpm build          # 生产构建
pnpm lint           # 代码检查
pnpm test           # 运行 Vitest

# E2E 测试（项目根目录）
npx playwright test             # 运行所有 E2E
npx playwright test --ui        # Playwright UI 模式
npx playwright test --headed    # 有头模式调试
```

## 项目宪法

本项目遵循宪法（`specs/_governance/constitution.md` v0.1.0）作为最高准则。核心原则：

1. **约定优于配置** — 优先使用 Spring Boot 默认约定
2. **分层不可逆** — Controller → Service → Mapper，不可反向调用
3. **防御式编程** — 所有外部输入验证，所有异常处理
4. **渐进式优化** — 先正确，再优化，量化数据驱动

**当前阶段：Phase 1**（业务开发）。宪法合规检查清单见宪法 Governance 章节。

技术实现规范（代码标准、数据库、API、安全、事务、日志、测试、部署、监控）见 `docs/standards/technical-standards.md`。

当前 feature 状态见 `specs/_feature-status.md`。
