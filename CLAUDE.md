# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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

## 行为准则

以下准则面向 AI（Claude Code），与宪法（`specs/_governance/constitution.md` v0.1.0）的工程决策原则并行。AI 准则约束编码过程，宪法约束系统设计。**权衡：偏向谨慎而非速度，简单任务可自行判断。**

### 1. 编码前先思考

**不假设，不隐藏困惑，揭示权衡。**

- 明确陈述假设。不确定时就问。
- 存在多种理解时全部呈现，不默默选择其一。
- 有更简单的方案就说出来，必要时据理力争。
- 有不明确的地方就停下来，说出困惑之处。

### 2. 简洁优先

**用最少代码解决问题，不做猜测性设计。**

- 不添加超出需求的功能，不为一次性代码创建抽象。
- 不添加未经请求的"灵活性"或"可配置性"。
- 不为不可能发生的场景编写错误处理。
- 写多了就重写：200 行能压缩到 50 行就立刻做。

自问："资深工程师会觉得这过于复杂吗？" 如果是，简化。

### 3. 精准修改

**只改必须改的，增量不携带无关变更。**

- 不"顺手改进"相邻代码、注释或格式。
- 不重构没坏的东西。
- 保持现有风格，即使不符合个人偏好。
- 发现无关的废弃代码时提出来，确认后再清理（与"简洁优先"不矛盾——废弃代码确实要删，但不混入当前 PR，除非关联极强）。
- 因修改产生的孤立 import/变量/函数必须清理。

检验标准：每一行修改都应能直接追溯到用户需求。

**边界**：废弃代码清理本身是简洁优先的正确实践。本条约束的是"顺手把相邻方法也重写了"这类偏离目标的改造。纯粹的删无用代码是好行为。

### 4. 目标驱动执行

**定义成功标准，循环直到验证通过。**

将任务转化为可验证的目标：

| 模糊指令 | 可验证目标 |
|----------|-----------|
| "添加验证" | 为无效输入编写测试，然后使其通过 |
| "修复 bug" | 编写复现测试，然后使其通过 |
| "重构 X" | 确保重构前后测试都通过 |

多步骤任务先简述计划，每步附带验证方式。强成功标准让你独立循环，弱标准则需不断澄清。

### 5. 数据库表标准字段

**所有表必须包含 `id`、`created_at`、`updated_at`。`is_deleted` 按业务需要判断。**

- design doc 编写完毕后自检每个表的字段是否齐全，不依赖审查发现。
- 纯日志表（如 `data_collection_log`）可只含 `created_at`，不需要 `updated_at` 和 `is_deleted`。
- 原始数据暂存表（如 `raw_data`）同理，按实际场景判断。

检验标准：`V{n}__*.sql` 中每张表都含 `created_at` 列。

### 6. 中英混合文本格式化

**所有包含代码标识符的中文描述，标识符必须用 `` ` `` 包裹。**

- tasks.md、plan.md、spec.md 等文档中，Java 类名、方法名、文件路径、表名、字段名、枚举值、注解名一律用 `` ` `` 包裹
- 示例：`StockInfoCleanseService`、`data_collection_log`、`GET /api/v1/admin/collection/status`、`@PreAuthorize`
- 纯中文内容不加反引号

**为什么**: Markdown 渲染后反引号内容显示为等宽字体，视觉上清晰区分代码标识符和自然语言，大幅提升可读性。

检验标准：任何中文文档中出现的英文代码标识符都能在 Markdown 渲染后以等宽字体展示。

### 7. 开发流程强制入口

**所有 speckit + superpowers 混合开发流程 MUST 遵循 `docs/superpowers/speckit-superpowers-workflow.md`。**

- 执行任何 speckit 或 superpowers 步骤前，必须确认当前步骤与 workflow 文档定义一致
- workflow 文档中定义的接口契约、审查门禁、TDD 纪律为强制规则，不可跳过或自行裁量
- 换 session 后第一条开发指令，必须先读取 workflow 文档再行动

**为什么**: workflow 文档是 speckit + superpowers 混合流程的唯一权威。没有它，AI 会退回各自独立的默认行为——speckit 不走审查门禁，superpowers 不走任务拆解，TDD 纪律丢失。

检验标准：任何 speckit/superpowers 命令执行前，AI 能说出当前在 workflow 的哪一个步骤、该步骤的产出物和下一个步骤。

### 8. 修改代码前确认

**所有对代码文件的修改（Edit/Write/Agent 派发）必须先获得用户确认再执行。**

- 分析问题、提出方案时充分展示将要修改的内容
- 用户说"改"或"修改"后再动手
- 讨论、提问、展示方案阶段不需要确认——确认只针对代码修改行为本身
- **"继续"只执行当前讨论中已明确的下一步**，完成后汇报结果 + 询问下一步。"继续"不授权连续多步自动执行；每一步完成后回确认点
- **Agent 派发等同于代码修改**——Agent 会创建/编辑文件，派发前同样需要确认

**为什么**: 避免 AI 在自己认为对的方向上默默修改，等用户发现问题时已积累大量变更。

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

**当前阶段：Phase 0**（项目初始化）。宪法合规检查清单见宪法 Governance 章节。

技术实现规范（代码标准、数据库、API、安全、事务、日志、测试、部署、监控）见 `docs/standards/technical-standards.md`。

<!-- SPECKIT START -->
**Current feature**: [003-data-collection](specs/003-data-collection/)
- Spec: [spec.md](specs/003-data-collection/spec.md)
- Plan: [plan.md](specs/003-data-collection/plan.md)
- Tasks: (pending `/speckit-tasks`)
<!-- SPECKIT END -->
