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

以下准则面向 AI（Claude Code），与宪法（`.specify/memory/constitution.md` v0.0.1）的工程决策原则并行。AI 准则约束编码过程，宪法约束系统设计。**权衡：偏向谨慎而非速度，简单任务可自行判断。**

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

本项目遵循宪法（`.specify/memory/constitution.md` v0.0.1）作为最高准则。核心原则：

1. **约定优于配置** — 优先使用 Spring Boot 默认约定
2. **分层不可逆** — Controller → Service → Mapper，不可反向调用
3. **防御式编程** — 所有外部输入验证，所有异常处理
4. **渐进式优化** — 先正确，再优化，量化数据驱动

**当前阶段：Phase 0**（项目初始化）。宪法合规检查清单见宪法 Governance 章节。

技术实现规范（代码标准、数据库、API、安全、事务、日志、测试、部署、监控）见 `docs/standards/technical-standards.md`。

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
<!-- SPECKIT END -->
