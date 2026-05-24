## 行为准则

### A1. 编码前先思考
**不要假设，不要隐藏困惑，暴露权衡。**
- 明确陈述假设，不确定就问。
- 存在多种理解时，全部列出——不要静默选择。
- 如果有更简单的方案，说出来。必要时反驳。
- 不清楚就停下来，指出困惑之处，然后问。

### A2. 简洁优先
**用最少的代码解决问题，不做推测性编码。**
- 不添加未被要求的功能。
- 不为单次使用的代码创建抽象。
- 不添加未被请求的"灵活性"或"可配置性"。
- 不为不可能发生的场景编写错误处理。
- 如果 200 行可以写成 50 行，就重写。

### A3. 精准修改
**只改必须改的，只清理自己弄乱的。**
- 不要"改进"相邻的代码、注释或格式。
- 不要重构没有问题的东西。
- 匹配现有风格，即使你会用不同方式。
- 发现无关的死代码，提出来——不要删除。
- 移除因你的变更而变得无用的 import/变量/函数。

### A4. 目标驱动执行
**定义成功标准，循环直到验证通过。**
将模糊任务转化为可验证的目标。多步骤工作时，先陈述简要计划。强成功标准让你可以独立循环；弱标准（"让它能跑"）需要不断确认。

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

## 技术栈

### 后端

| 领域 | 选型 |
|------|------|
| 语言 | Java 17+ (推荐 21 LTS) |
| 框架 | Spring Boot 3.3.x |
| ORM | MyBatis-Plus 3.5+ |
| 数据库 | MySQL 8.0+ |
| 构建工具 | Gradle |
| 测试 | JUnit 5 + Mockito + H2 + MockMvc |
| API 文档 | SpringDoc (Swagger/OpenAPI) |
| 数据库迁移 | Flyway |
| 安全 | Spring Security + JWT |
| 工具 | Lombok |
| 测试策略 | 新 API MUST 有集成测试，关键流程 MUST 有 E2E |

### 前端

| 领域 | 选型 |
|------|------|
| 框架 | Next.js 14+ (App Router) |
| 语言 | TypeScript |
| 样式 | TailwindCSS 3+ |
| 组件库 | shadcn/ui |
| 图表 | lightweight-charts (TradingView) |
| HTTP | ky |
| 状态 | zustand + TanStack Query |
| 单元测试 | Vitest + @testing-library/react |
| E2E | Playwright（`frontend/playwright.config.ts`，`frontend/e2e/`） |
| 构建工具 | pnpm |

## 编码规范速查

- 魔术数字 → 常量（批量大小/超时/重试等；0/1 判断除外）
- 用户可见文本 → 中文（API 路径/JSON key/日志除外）
- 代码注释 → 中文（技术术语可保留英文，如 `FETCH`、`JWT`、`API`）
- Controller public 方法 → `@Operation(summary=)`，Service public 方法 → Javadoc
- Service 必须接口+实现类（接口在 `service/`，实现类在 `service/impl/`）

## 代码规范引用

按需加载：数据库表规范、代码标准、API、安全、事务、日志、测试、部署、监控 → [technical-standards.md](docs/standards/technical-standards.md)

## 项目宪法

本项目遵循宪法（`specs/_governance/constitution.md` v0.1.0）作为最高准则。核心原则：
1. **约定优于配置** — 优先使用 Spring Boot 默认约定
2. **分层不可逆** — Controller → Service → Mapper，不可反向调用
3. **防御式编程** — 所有外部输入验证，所有异常处理
4. **渐进式优化** — 先正确，再优化，量化数据驱动

**当前阶段：Phase 1**（业务开发）。

## Agent 技能

### Issue 追踪
Issues 以本地 Markdown 文件形式存储在 `.scratch/` 目录下。详见 `docs/agents/issue-tracker.md`。

### 分诊标签
标准五标签体系：`needs-triage`、`needs-info`、`ready-for-agent`、`ready-for-human`、`wontfix`。详见 `docs/agents/triage-labels.md`。

### 领域文档
单上下文布局：仓库根目录一个 `CONTEXT.md` + `docs/adr/`。详见 `docs/agents/domain.md`。
