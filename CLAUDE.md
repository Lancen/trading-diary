## 行为准则

### A1. Think Before Coding
**Don't assume. Don't hide confusion. Surface tradeoffs.**
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them all — don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

### A2. Simplicity First
**Minimum code that solves the problem. Nothing speculative.**
- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If 200 lines could be 50, rewrite it.

### A3. Surgical Changes
**Touch only what you must. Clean up only your own mess.**
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it — don't delete it.
- Remove imports/variables/functions that YOUR changes made unused.

### A4. Goal-Driven Execution
**Define success criteria. Loop until verified.**
Transform vague tasks into verifiable goals. For multi-step work, state a brief plan. Strong success criteria let you loop independently; weak criteria ("make it work") require constant clarification.

### A5. 中英混合文本格式化
**所有包含代码标识符的中文描述，标识符必须用 `` ` `` 包裹。**
tasks.md、plan.md、spec.md 等文档中，Java 类名、方法名、文件路径、表名、字段名、枚举值、注解名一律用 `` ` `` 包裹。纯中文内容不加反引号。

### A6. Token 效率：减少 Agent 派发
机械任务（单文件、纯替换/注解/模板、无逻辑判断）→ Edit/Write 直接改。仅多文件协调、架构决策、审查时派发 Agent。

### A7. Token 效率：读文件先看结构
**需要了解文件内容时，先用 `smart_outline` 看结构，再精读目标区域。** 禁止一次性全量读取 200+ 行的大文件，除非确定需要全文。

### A8. Token 效率：长任务主动压缩
**跨多轮的复杂任务过半时，主动提示用户执行 `/compact`**，丢弃中间过程的冗余上下文。

## 协作准则 — 与用户的交互约束

### B1. 疑问输入安全阀
**用户输入含 `?` / `？` / `吗` / `呢` / `怎样` / `如何` → 只回答，不修改任何文件。**
用户输入以 `能不能` / `可以` / `是否` 开头 → 同理，只回答不修改。
收到用户输入后，在决定任何动作之前，先扫描上述标记。此规则优先级高于 B2（修改确认）和 A3（精准修改）——不论输入内容看起来多像"指出一个需要修复的问题"，只要有疑问标记，就是问题不是指令。

### B2. 修改代码前确认
**需确认**：新建/删除文件、修改业务逻辑、派发 Agent、不确定的变更。
**可直行**：typo/注释、按模式加字段/方法、用户明确指定的步骤、清理孤立 import。
不确定走确认。**"继续"** = 执行下一步，完成后汇报。

### B3. 精确暂存，禁止通配 commit
**禁止 `git add -A`、`git add .`、`git add *`、`git commit -a`。每次 commit 按文件路径逐一暂存。**
- `git add <具体文件>` 逐个暂存，每个文件都经过确认
- commit 前用 `git diff --stat` 检查变更范围
- 发现陌生文件（未创建、未讨论）立即排查来源
- agent 产出的文件必须逐项审查后再加入暂存

### B4. 实现代码必须经过测试
**新 Service/Controller 方法 → 必须有对应单元测试（JUnit + Mockito）。**
测试必须在实现提交之前编写并运行通过（RED → GREEN）。speckit 流程中的 TDD 纪律详见 [workflow 步骤 7](docs/superpowers/workflow-steps/step-07-execution.md)。

### B5. TDD 执行纪律
**所有功能开发严格遵循 TDD：垂直切片，一步一测。**
- 一个测试 → 一个实现 → 提交。禁止批量写全部测试再全部实现。
- `./gradlew test` 必须是绿的才能提交。新功能 RED→GREEN，修复直接 GREEN。
- 复杂 SQL MUST 写在 XML mapper 中（`src/main/resources/mapper/`），禁止 `@Select` 注解拼接。

### B6. 任务状态同步
**每完成一个 speckit tasks.md 中的任务，立即标记为 `[x]`。**
- 提交代码时连带更新 `specs/*/tasks.md` 中对应条目的 checkbox。
- 不依赖 git log 推断进度——tasks.md 是进度的唯一事实来源。
- 在 commit message 中引用 Task ID（如 `T003-T004: 实体更新`）。

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

## 编码规范速查

- 魔术数字 → 常量（批量大小/超时/重试等；0/1 判断除外）
- 用户可见文本 → 中文（API 路径/JSON key/日志除外）
- Controller public 方法 → `@Operation(summary=)`，Service public 方法 → Javadoc

## 代码规范引用

按需加载：数据库表规范、代码标准、API、安全、事务、日志、测试、部署、监控 → [technical-standards.md](docs/standards/technical-standards.md)

## 项目宪法

本项目遵循宪法（`specs/_governance/constitution.md` v0.1.0）作为最高准则。核心原则：
1. **约定优于配置** — 优先使用 Spring Boot 默认约定
2. **分层不可逆** — Controller → Service → Mapper，不可反向调用
3. **防御式编程** — 所有外部输入验证，所有异常处理
4. **渐进式优化** — 先正确，再优化，量化数据驱动

**当前阶段：Phase 1**（业务开发）。

## Agent skills

### Issue tracker
Issues are tracked as local markdown files under `.scratch/`. See `docs/agents/issue-tracker.md`.

### Triage labels
Standard five-label vocabulary: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs
Single-context layout: one `CONTEXT.md` + `docs/adr/` at the repo root. See `docs/agents/domain.md`.
