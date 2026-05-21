# 任务列表：数据采集页面 UI/UE 重构

**输入**: 设计文档来自 `/specs/003-data-collection/`
**前置条件**: plan.md, spec.md, research.md, data-model.md, contracts/api.md

**测试**: 要求单元测试（JUnit + Mockito），前端 E2E 测试

**组织方式**: 按用户故事分组，每故事可独立实现和测试

## 格式: `- [ ] [ID] [P?] [Story?] 描述 + 文件路径`

---

## 阶段 1：搭建（共享基础设施）

**目的**: 数据库迁移 + 实体层，所有故事的前置

- [x] T001 创建 Flyway V5 迁移 `src/main/resources/db/migration/V5__ui_redesign_schema.sql`，含：`data_collection_log` 加 `request_url VARCHAR(512)`、`request_params VARCHAR(512)`、`remark VARCHAR(256)`；`margin_daily` 加 `margin_change DECIMAL(16,2)`、`short_change DECIMAL(16,2)`；新建 `index_daily` 表（id/index_code/trade_date/open/high/low/close/volume/amount/change_pct + 审计字段 + uk_code_date）
- [ ] T002 运行 Flyway 迁移验证: `./gradlew flywayMigrate`，预期成功
- [x] T003 [P] 更新 `DataCollectionLog` 实体 `src/main/java/com/tradingdiary/entity/DataCollectionLog.java` 加 `requestUrl`、`requestParams`、`remark` 字段
- [x] T004 [P] 更新 `MarginDaily` 实体 `src/main/java/com/tradingdiary/entity/MarginDaily.java` 加 `marginChange`、`shortChange` 字段
- [x] T005 [P] 新建 `IndexDaily` 实体 `src/main/java/com/tradingdiary/entity/IndexDaily.java`，MyBatis-Plus 注解，字段对应 V5 建表
- [x] T006 [P] 新建 `IndexDailyMapper` `src/main/java/com/tradingdiary/mapper/IndexDailyMapper.java` 继承 `BaseMapper<IndexDaily>`
- [x] T007 [P] 新建 Collection 相关 VO/DTO：`src/main/java/com/tradingdiary/collection/model/StockListVO.java`、`src/main/java/com/tradingdiary/collection/model/StockDetailVO.java`、`src/main/java/com/tradingdiary/collection/model/ConceptIndustryVO.java`、`src/main/java/com/tradingdiary/collection/model/MarginSummaryVO.java`、`src/main/java/com/tradingdiary/collection/model/CalendarDayVO.java`
- [x] T008 提交: `git add` V5 迁移 + 3 实体 + 1 Mapper + 5 VO，commit

**检查点**: 数据库迁移通过，实体层就绪

---

## 阶段 2：基础设施（阻塞性前置）

**目的**: 后端 Service 层 + Controller 骨架，所有前端故事的前置

**⚠️ 关键**: 后续所有前端任务依赖此阶段 Controller API 就位

- [x] T009 改造 `MarginCleanseService.cleanse()` `src/main/java/com/tradingdiary/service/collection/MarginCleanseService.java`：清洗时查询上一交易日 margin_daily，计算 `marginChange = 当日.balance - 上日.balance`、`shortChange = 当日.shortBalance - 上日.shortBalance`，写入实体。无上日数据时设为 null
- [ ] T010  # 跳过: MyBatis-Plus LambdaQueryWrapper 不兼容纯 Mockito，需集成测试 写 `MarginCleanseService` 测试 `src/test/java/com/tradingdiary/collection/MarginCleanseServiceTest.java`：验证 change 计算正确、无上日数据时 change=null。RED→GREEN
- [x] T011 新建 `MarketDataService` `src/main/java/com/tradingdiary/service/MarketDataService.java`：提供概念列表聚合查询（concept JOIN stock_concept JOIN margin_daily GROUP BY + SUM）和行业列表聚合查询（同上用 stock_industry）
- [x] T012 新建 `StockDataService` `src/main/java/com/tradingdiary/service/StockDataService.java`：提供股票列表查询（stock_info JOIN stock_industry/stock_concept LEFT JOIN margin_daily，支持 keyword/日期/行业/概念筛选，sortBy/sortDir 排序，分页）和股票详情查询（stock_info + stock_daily 日线 + margin_daily 两融 + industry/concept 标签）
- [x] T013 新建 `CalendarService` `src/main/java/com/tradingdiary/service/CalendarService.java`：提供交易日历查询（trade_calendar 当月交易日 vs stock_daily DISTINCT trade_date 比对，返回 3 态）
- [x] T014 新建 `MarketDataController` `src/main/java/com/tradingdiary/collection/controller/MarketDataController.java`：`GET /api/v1/admin/market/concepts`、`GET /api/v1/admin/market/industries`
- [x] T015 新建 `StockDataController` `src/main/java/com/tradingdiary/collection/controller/StockDataController.java`：`GET /api/v1/admin/stocks/list`、`GET /api/v1/admin/stocks/{code}`、`GET /api/v1/admin/stocks/{code}/calendar`
- [x] T016 新建 `MarginStatsController` `src/main/java/com/tradingdiary/collection/controller/MarginStatsController.java`：`GET /api/v1/admin/margin-stats/summary`
- [x] T017 写 Controller 集成测试 `src/test/java/com/tradingdiary/controller/StockDataControllerTest.java`、`MarketDataControllerTest.java`、`MarginStatsControllerTest.java`：MockMvc 验证 API 响应格式和分页。RED→GREEN
- [x] T018 更新 `ConstituentImportService.listFiles()` `src/main/java/com/tradingdiary/service/collection/ConstituentImportService.java`：查询 stock_industry 表 snap_date=file.fetchedDate 有无记录，返回字段加 `imported: boolean`
- [x] T019 提交: `git add` 所有 Service + Controller + 测试，commit

**检查点**: 后端 API 全部就位，可用 curl 测试

---

## 阶段 3：用户故事 1 — 侧边栏重构 + Hub 页改造 (P1) 🎯 MVP

**目标**: 改造现有采集状态页为纯展示 Hub，侧边栏新增"数据浏览"分组

**独立测试**: 打开 /admin/collection，看到 9 张卡片纯展示；侧边栏有两个分组，点击可导航

- [x] T020 [US1] 重构侧边栏 `frontend/src/app/(dashboard)/layout.tsx`：分组名"采集状态"→"基础数据管理"，条目名"采集状态"→"数据采集"；新增"数据浏览"分组含 4 个条目（股票数据/概念列表/行业列表/融资统计）；高亮逻辑用 startsWith 排除 `/admin/collection/margin`
- [x] T021 [US1] 改造 Hub 页 `frontend/src/app/(dashboard)/admin/collection/page.tsx`：9 张卡片 3 列网格统一布局（8 采集类型 + 成分股），每卡片显示名称+状态标签+最新时间；移除全部操作按钮和日志展开，仅保留"刷新"；点击"股票行情"卡片 → `/admin/collection/stocks`；点击"成分股数据"卡片 → `/admin/collection/constituents`
- [x] T022 [US1] E2E：Playwright 验证 Hub 页卡片渲染、点击跳转 `e2e/collection-hub.spec.ts`
- [x] T023 [US1] 提交: `git add` layout.tsx + page.tsx + e2e，commit

**检查点**: Hub 页纯展示，侧边栏新分组可见，卡片跳转正常

---

## 阶段 4：用户故事 2 — 股票行情详情页 (P1)

**目标**: 管线状态 + 交易日历 + 采集日志的独立页面

**独立测试**: 从 Hub 点击"股票行情"卡片 → 看到管线状态图、交易日历、操作按钮、日志表

- [x] T024 [US2] 新建 `frontend/src/app/(dashboard)/admin/collection/stocks/page.tsx`：面包屑返回、采集→清洗两步管线状态卡片、交易日历（月视图，3 色标记，默认当月，可翻月）、操作按钮（触发采集/历史补采）、日志表（日期/阶段/状态/地址/参数/记录数/备注，失败红底，最近 20 条）
- [x] T025 [US2] 提交: `git add` page.tsx，commit

**检查点**: 股票行情详情页数据展示、日历交互、日志查看正常

---

## 阶段 5：用户故事 3 — 成分股管理页 (P1)

**目标**: 文件列表 + 导入状态 + 导入操作

**独立测试**: 从 Hub 点击"成分股数据"卡片 → 看到文件列表，已导入/待导入状态正确

- [x] T026 [US3] 新建 `frontend/src/app/(dashboard)/admin/collection/constituents/page.tsx`：面包屑返回、表格列表（文件名/采集日期/行业数/概念数/关系总数/状态/操作）、已导入置灰、待导入可点击导入
- [x] T027 [US3] 提交: `git add` page.tsx，commit

**检查点**: 成分股文件列表展示正确，导入按钮可用

---

## 阶段 6：用户故事 4 — 股票列表 + 股票详情 (P1)

**目标**: 可筛选/排序的股票数据列表 + 单股票 K线叠加两融详情

**独立测试**: 侧边栏→股票数据→筛选/排序→点击股票→K线图显示，两融叠加，行业概念展示

- [x] T028 [US4] 新建股票列表页 `frontend/src/app/(dashboard)/admin/stocks/page.tsx`：筛选栏（代码/名称输入+行业下拉+概念下拉+日期）、表头可排序（涨跌幅/成交量/融资余额/融资变化/融券余额/融券变化）、列（代码/名称/行业/概念/收盘/涨跌幅/成交量/融资余额/融资变化/融券余额/融券变化/日期）、点击代码/名称跳转详情
- [x] T029 [US4] 新建股票详情页 `frontend/src/app/(dashboard)/admin/stocks/[code]/page.tsx`：面包屑返回、行业+概念标签、行情摘要卡片、K线图（lightweight-charts 单图叠加：CandlestickSeries + HistogramSeries 成交量半透明 + 2 LineSeries 融资/融券线，双 PriceScale，日K/周K/月K 切换，快捷区间 1月/3月/6月/1年+自定义日期，默认最近3个月日K，无两融数据断开）、日线明细表默认收起可展开
- [x] T030 [US4] E2E：Playwright 验证股票列表筛选/排序、详情页 K线渲染 `e2e/stocks.spec.ts`
- [x] T031 [US4] 提交: `git add` 两个 page.tsx + e2e，commit

**检查点**: 股票列表筛选排序正常，详情页 K线+两融叠加渲染正确

---

## 阶段 7：用户故事 5 — 概念列表 + 行业列表 (P2)

**目标**: 概念/行业维度两融聚合数据浏览，支持跳转

**独立测试**: 侧边栏→概念列表→点击股票数→股票列表带筛选；点击两融数据→融资统计详情（二期）

- [x] T032 [US5] 新建概念列表页 `frontend/src/app/(dashboard)/admin/concepts/page.tsx`：名称搜索、表（编码/名称/股票数/融资余额/融资变化/融券余额/融券变化/日期）、所有数值列可排序、点击股票数→`/admin/stocks?concept=XXX`、点击两融数据→`/admin/margin-stats/concept/XXX`（二期）、名称不可点击
- [x] T033 [US5] 新建行业列表页 `frontend/src/app/(dashboard)/admin/industries/page.tsx`：同概念列表结构、点击股票数→`/admin/stocks?industry=XXX`、点击两融数据→`/admin/margin-stats/industry/XXX`
- [x] T034 [US5] 提交: `git add` 两个 page.tsx，commit

**检查点**: 概念/行业列表数据展示和跳转正常

---

## 阶段 8：收尾与横切关注点

**目的**: 验证、文档更新

- [x] T035 运行完整后端测试: `./gradlew test`，验证全部通过
- [x] T036 运行前端 E2E: `cd frontend && pnpm exec playwright test`，验证全部通过（10/10）
- [ ] T037 运行 quickstart.md 验证流程（迁移+API+前端页面）

---

## 依赖与执行顺序

### 阶段依赖

- **搭建（阶段 1）**: 无依赖，立即开始
- **基础设施（阶段 2）**: 依赖阶段 1 完成 —— 阻塞所有用户故事
- **US1（阶段 3）**: 依赖阶段 2 完成
- **US2（阶段 4）**: 依赖阶段 2 完成，与 US1 无关
- **US3（阶段 5）**: 依赖阶段 2 + T018（ConstituentImportService 改造）
- **US4（阶段 6）**: 依赖阶段 2 完成，与 US1/US2/US3 无关
- **US5（阶段 7）**: 依赖阶段 2 完成，与 US4 共用 /admin/stocks 跳转（但可独立测试）
- **收尾（阶段 8）**: 依赖所有故事完成

### 并行机会

- 阶段 1 内 T003/T004/T005/T006/T007 可并行（不同文件）
- 阶段 2 内 T011/T012/T013 可并行（不同 Service），T014/T015/T016 可并行（不同 Controller）
- 阶段 3-7 各用户故事之间可并行（不同前端页面文件）

---

## 实施策略

### MVP 优先（US1 + US2）

1. 阶段 1 + 2 完成 → 后端就位
2. 阶段 3 (US1: Hub) + 阶段 4 (US2: Stocks detail) → 股票行情详情可操作
3. STOP → 验证 → 可交付

### 增量交付

1. Foundation 完成
2. + US1 (Hub) → 验证
3. + US2 (股票行情) → 验证
4. + US3 (成分股) → 验证
5. + US4 (股票列表+详情) → 验证
6. + US5 (概念+行业) → 验证
