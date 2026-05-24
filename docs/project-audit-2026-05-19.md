# Trading Diary 项目审计报告

> 审计日期：2026-05-19
> 审计范围：后端 Java / 前端 Next.js / 构建配置 / 文档一致性
> 最后更新：2026-05-19（标记修复状态）

---

## 一、项目概览

| 项 | 值 |
|---|---|
| 项目名 | trading-diary — 交易日记 |
| 后端 | Java 17 / Spring Boot 3.3.5 / MyBatis-Plus 3.5.9 / MySQL 8 / Gradle |
| 前端 | Next.js 14 / TypeScript / TailwindCSS / shadcn/ui / pnpm |
| 数据库迁移 | Flyway（V1–V7） |
| 安全 | Spring Security + JWT |
| 测试 | JUnit 5 + Mockito + H2 / Vitest + Testing Library / Playwright |
| 当前阶段 | Phase 1（业务开发） |

---

## 二、严重问题（🔴 必须修复）

### 2.1 构建环境不可用 — Java 版本不匹配 ✅ 已修复

**修复方式**：创建 `gradle.properties`，设置 `org.gradle.java.home` 指向 Java 17（`/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`）。

---

### 2.2 Flyway 版本与 Spring Boot 不兼容 ✅ 已修复

**修复方式**：移除 `build.gradle` 中 Flyway 显式版本号 `9.22.3`，由 Spring Boot BOM 自动管理（10.x）。

---

### 2.3 Controller 直接注入 Mapper — 违反宪法核心原则 ✅ 已修复

**修复方式**：
- 新建 `CollectionQueryService` + `CollectionQueryServiceImpl`，封装 `CollectionController` 的 7 个 Mapper 调用
- 新建 `MarginStatsService` + `MarginStatsServiceImpl`，封装 `MarginStatsController` 的 `MarginDailyMapper` 调用
- `AuthService` 新增 `getUserIdByUsername()` 方法，替代 `AuthController` 中的 `SysUserMapper` 直接调用
- 所有 Controller 不再直接注入 Mapper

---

### 2.4 `@Select` 注解泛滥 — 违反项目编码规范 ✅ 已修复

**修复方式**：23 处 `@Select` 注解全部迁移到 XML mapper 文件（新建 5 个 XML，更新 3 个已有 XML）。

---

### 2.5 前端 API 基地址硬编码 ✅ 已修复

**修复方式**：`api.ts` 使用 `process.env.NEXT_PUBLIC_API_BASE_URL` 环境变量，新建 `.env.example` 模板。

---

## 三、中等问题（🟡 建议修复）

### 3.1 前端测试覆盖率极低 ⏳ 待修复

**现状**：前端仅有 2 个测试文件（`AuthGuard.test.tsx`、`useAuth.test.ts`），覆盖 40+ 个组件中的 2 个，覆盖率约 5%。

**缺失测试的关键组件**：
- 所有页面组件（`admin/collection/`, `stocks/`, `margin-stats/` 等）
- `AuthProvider` 组件
- `api.ts` 中的 API 调用函数
- 自定义 hooks（`use-toast.ts`）

**修复**：按优先级补充测试 — 先覆盖 API 层和 Auth 流程，再覆盖核心页面。

---

### 3.2 后端测试覆盖不完整 ⏳ 待修复

**现状**：以下 Service/Controller 缺少对应测试：

| 缺失测试的类 | 类型 |
|---|---|
| `MarginCleanseService` | Service |
| `MarginMacroCleanseService` | Service |
| `StockDailyCleanseService` | Service |
| `StockInfoCleanseService` | Service |
| `ConstituentImportService` | Service |
| `TradeCalendarService` | Service |
| `CalendarService` | Service |
| `MarketDataService` | Service |
| `StockDataServiceImpl` | Service |
| `CollectionScheduler` | Scheduler |
| `MarginStatsController` | Controller |
| `StockDataController`（collection 包下） | Controller |

---

### 3.3 重复的 Playwright 配置和 E2E 测试 ✅ 已修复

**修复方式**：删除根目录 `e2e/` 和 `playwright.config.ts`，统一到 `frontend/` 下。

---

### 3.4 CLAUDE.md 技术栈声明与实际不符 ✅ 已修复

**修复方式**：更新 CLAUDE.md 技术栈表 — Spring Boot 3.2.x→3.3.x，移除 Recharts/react-hook-form/zod/next-pwa/@MybatisPlusTest，新增 Lombok，E2E 路径更新。

---

### 3.5 前端缺少 ESLint 配置 ✅ 已修复

**修复方式**：创建 `frontend/.eslintrc.json`（next/core-web-vitals + next/typescript），安装 eslint@8 + eslint-config-next@14。`pnpm lint` 可正常运行。

---

### 3.6 文档断链 ✅ 已修复

**修复方式**：`constitution.md` 删除不存在的 `docs/architecture/` 引用；`domain.md` 标注 `docs/adr/` 为 lazy 创建。

---

### 3.7 宪法文档路径引用不一致 ✅ 已修复

**修复方式**：`technical-standards.md` 宪法路径从 `.specify/memory/constitution.md` 修正为 `specs/_governance/constitution.md`。

---

### 3.8 Service 方法缺少 Javadoc ✅ 已修复

**修复方式**：12 个 Service 接口的 24 个 public 方法补充了中文 Javadoc（含 `@param` 和 `@return`）。

---

## 四、低优先级问题（🟢 可后续处理）

### 4.1 根目录存在垃圾文件 ✅ 已修复

**修复方式**：删除 10 个垃圾文件（日志、Playwright 快照、QA 截图、agent 文件）。

---

### 4.2 `.gitignore` 可补充 ✅ 已修复

**修复方式**：新增 7 条模式（`*.log.*`、`*.jar`、`.env.production`/`.env.staging`、`coverage/`/`.nyc_output/`、`qa-*.png`、`page-*.yml`）。

---

### 4.3 `.scratch/` 目录不存在 ✅ 已修复

**修复方式**：创建 `.scratch/.gitkeep`。

---

### 4.4 Feature 状态可能过时 ✅ 已修复

**修复方式**：`_feature-status.md` 添加活跃 spec 交叉引用。

---

### 4.5 前端 `any` 类型使用 ⏳ 待修复

**现状**：ESLint 检测到 9 处 `no-explicit-any` 警告。

**修复**：逐步替换 `any` 为具体类型定义。

---

## 五、问题统计

| 严重程度 | 总数 | 已修复 | 待修复 |
|---|---|---|---|
| 🔴 严重 | 5 | 5 | 0 |
| 🟡 中等 | 8 | 6 | 2（3.1 前端测试、3.2 后端测试） |
| 🟢 低 | 5 | 4 | 1（4.5 any 类型） |
| **合计** | **18** | **15** | **3** |

---

## 六、剩余待修复项

| 编号 | 问题 | 优先级 | 说明 |
|---|---|---|---|
| 3.1 | 前端测试覆盖率极低 | 中 | 需逐步补充，非一次性可完成 |
| 3.2 | 后端测试覆盖不完整 | 中 | 12 个 Service/Controller 缺测试，需逐步补充 |
| 4.5 | 前端 `any` 类型 | 低 | 9 处警告，逐步替换 |
