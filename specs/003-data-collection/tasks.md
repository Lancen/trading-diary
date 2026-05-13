# 任务列表：数据采集层

**输入**: 设计文档来自 `specs/003-data-collection/`
**前置条件**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**组织方式**: 按用户故事分组，支持独立实施和测试。

## 格式: `[ID] [P?] [Story] 描述`

- **[P]**: 可并行（不同文件，无依赖）
- **[Story]**: 所属用户故事（US1/US2/US3/US4）
- 描述使用中文，代码标识符用 `` ` `` 包裹（如 `` `StockInfo.java` ``）

## 路径约定

- **后端**: `src/main/java/com/tradingdiary/`
- **前端**: `frontend/src/`
- **迁移**: `src/main/resources/db/migration/`
- **测试**: `src/test/java/com/tradingdiary/`

---

## 阶段 1：搭建（共享基础设施）

**目的**: 项目初始化 — Flyway 迁移、`entity`/`mapper` 脚手架

- [ ] T001 创建 Flyway 迁移 `V3__collection_schema.sql`，含全部 12 张表（`data_collection_log`、`raw_data`、`trade_calendar`、`stock_info`、`stock_daily`、`industry`、`stock_industry`、`concept`、`stock_concept`、`classification_change_log`、`margin_stock`、`margin_daily`），路径 `src/main/resources/db/migration/V3__collection_schema.sql`
- [ ] T002 [P] 创建 `DataCollectionLog` 实体 `src/main/java/com/tradingdiary/entity/DataCollectionLog.java`
- [ ] T003 [P] 创建 `RawData` 实体 `src/main/java/com/tradingdiary/entity/RawData.java`
- [ ] T004 [P] 创建 `TradeCalendar` 实体 `src/main/java/com/tradingdiary/entity/TradeCalendar.java`
- [ ] T005 [P] 创建 `StockInfo` 实体 `src/main/java/com/tradingdiary/entity/StockInfo.java`
- [ ] T006 [P] 创建 `StockDaily` 实体 `src/main/java/com/tradingdiary/entity/StockDaily.java`
- [ ] T007 [P] 创建 `Industry` 实体 `src/main/java/com/tradingdiary/entity/Industry.java`
- [ ] T008 [P] 创建 `StockIndustry` 实体 `src/main/java/com/tradingdiary/entity/StockIndustry.java`
- [ ] T009 [P] 创建 `Concept` 实体 `src/main/java/com/tradingdiary/entity/Concept.java`
- [ ] T010 [P] 创建 `StockConcept` 实体 `src/main/java/com/tradingdiary/entity/StockConcept.java`
- [ ] T011 [P] 创建 `ClassificationChangeLog` 实体 `src/main/java/com/tradingdiary/entity/ClassificationChangeLog.java`
- [ ] T012 [P] 创建 `MarginStock` 实体 `src/main/java/com/tradingdiary/entity/MarginStock.java`
- [ ] T013 [P] 创建 `MarginDaily` 实体 `src/main/java/com/tradingdiary/entity/MarginDaily.java`
- [ ] T014 [P] 创建 `DataCollectionLogMapper` 接口 `src/main/java/com/tradingdiary/mapper/DataCollectionLogMapper.java`
- [ ] T015 [P] 创建 `RawDataMapper` 接口 `src/main/java/com/tradingdiary/mapper/RawDataMapper.java`
- [ ] T016 [P] 创建 `MarginDailyMapper` 接口 `src/main/java/com/tradingdiary/mapper/MarginDailyMapper.java`
- [ ] T017 [P] 创建 `StockInfoMapper` 接口 `src/main/java/com/tradingdiary/mapper/StockInfoMapper.java`
- [ ] T018 [P] 创建其余 Mapper 接口：`TradeCalendarMapper`、`StockDailyMapper`、`IndustryMapper`、`StockIndustryMapper`、`ConceptMapper`、`StockConceptMapper`、`ClassificationChangeLogMapper`、`MarginStockMapper`，路径 `src/main/java/com/tradingdiary/mapper/`
- [ ] T019 验证 Flyway 迁移成功，所有表正确创建：`./gradlew bootRun --args='--spring.profiles.active=dev'`

**检查点**: 12 张表全部就位，`entity` + `mapper` 可用

---

## 阶段 2：基础设施（阻塞性前置）

**目的**: `AKToolsClient` + `CollectionOrchestrator` — 所有用户故事的前置依赖

**⚠️ 关键**: 此阶段完成前不能开始任何用户故事

- [ ] T020 实现 `AKToolsClient`，使用 `RestClient` 封装全部 9 个 AKTools HTTP 端点的调用方法（`fetchStockSpot`、`fetchStockDaily`、`fetchIndustryNames`、`fetchIndustryCons`、`fetchConceptNames`、`fetchConceptCons`、`fetchTradeCalendar`、`fetchMarginDetailSse`、`fetchMarginDetailSzse`），路径 `src/main/java/com/tradingdiary/collection/client/AKToolsClient.java`
- [ ] T021 [P] 实现 `TradeCalendarService`（全量拉取 + 增量同步），路径 `src/main/java/com/tradingdiary/service/collection/TradeCalendarService.java`
- [ ] T022 [P] 创建 `CollectionStatusVO`（按 contracts 定义的响应模型），路径 `src/main/java/com/tradingdiary/collection/model/CollectionStatusVO.java`
- [ ] T023 [P] 创建 `GapReportVO`（按 contracts 定义的缺口响应模型），路径 `src/main/java/com/tradingdiary/collection/model/GapReportVO.java`
- [ ] T024 实现 `CollectionOrchestrator`，含 FETCH → CLEANSE 生命周期编排、per-key 并发锁（`ConcurrentHashMap<String, ReentrantLock>`）、3 次重试（间隔 2s/4s/8s），路径 `src/main/java/com/tradingdiary/collection/orchestrator/CollectionOrchestrator.java`
- [ ] T025 验证 `AKToolsClient` 连通性：执行 `TradeCalendarService.fetch`；若 AKTools 不可用，错误信息必须明确指出 "AKTools Docker 可能未运行，请检查 `docker ps`"（禁止静默失败）；成功时检查 `data_collection_log` 和 `raw_data` 表写入

**检查点**: AKTools 连通确认，编排流水线就绪

---

## 阶段 3：用户故事 1 — 定时自动采集数据 (Priority: P1) 🎯 MVP

**目标**: 系统每个交易日自动采集 6 类数据，16:00-18:00 全流程 30 分钟内完成。

**独立测试**: 配置两个交易日，收盘后观察所有股票/两融/行业/概念数据自动入库，无需人工干预。

### 用户故事 1 实现

- [ ] T026 [P] [US1] 实现 `StockInfoCleanseService`：解析 `stock_zh_a_spot_em` 返回的 JSON → `INSERT ON DUPLICATE KEY UPDATE` 写入 `stock_info` 表，路径 `src/main/java/com/tradingdiary/service/collection/StockInfoCleanseService.java`
- [ ] T027 [P] [US1] 实现 `StockDailyCleanseService`：从 `stock_zh_a_spot_em` JSON 中提取 OHLC 数据 → 写入 `stock_daily` 表，路径 `src/main/java/com/tradingdiary/service/collection/StockDailyCleanseService.java`
- [ ] T028 [P] [US1] 实现 `IndustryCleanseService`：解析行业板块名称 JSON + 遍历行业成分股 → 批量写入，路径 `src/main/java/com/tradingdiary/service/collection/IndustryCleanseService.java`
- [ ] T029 [P] [US1] 实现 `ConceptCleanseService`：解析概念板块名称 JSON + 遍历概念成分股 → 批量写入，路径 `src/main/java/com/tradingdiary/service/collection/ConceptCleanseService.java`
- [ ] T030 [US1] 实现 `MarginCleanseService`：解析 `stock_margin_detail_sse` / `stock_margin_detail_szse` JSON → 写入 `margin_daily` 表 + 提取 `margin_stock` 标的，路径 `src/main/java/com/tradingdiary/service/collection/MarginCleanseService.java`
- [ ] T031 [US1] 在 `CollectionOrchestrator.orchestrate()` 中建立 `data_type` 枚举到各 `CleanseService` 的路由映射，路径 `src/main/java/com/tradingdiary/collection/orchestrator/CollectionOrchestrator.java`
- [ ] T032 [US1] 实现 `CollectionScheduler`：`@Scheduled` 定时任务（16:00 股票快照、17:00 行业/概念、18:00 两融数据），`@Profile("!test")`，执行前检查 `trade_calendar` 判断是否为交易日（法定假日自动跳过），路径 `src/main/java/com/tradingdiary/collection/scheduler/CollectionScheduler.java`
- [ ] T033 [US1] 在 `AKToolsClient` 中添加 200ms 限速器和 4 线程异步执行器，用于批量 `stock_daily` 调用控制，路径 `src/main/java/com/tradingdiary/collection/client/AKToolsClient.java`
- [ ] T034 [US1] 实现 `raw_data` 月度归档任务（`@Scheduled` 每月 1 号凌晨，GZIP 压缩为 JSON Lines 存入项目根目录 `backups/raw_data/YYYY-MM.json.gz`，路径可通过 `app.collection.archive-path` 配置），路径 `src/main/java/com/tradingdiary/collection/scheduler/CollectionScheduler.java`
- [ ] T035 [US1] 端到端验证：模拟交易日 → 观察全部 6 类数据自动采集、FETCH + CLEANSE 日志完整、`margin_stock` 自动填充

**检查点**: US1 完整可用 — 每日自动采集无需人工干预

---

## 阶段 4：用户故事 2 — 查看采集状态与数据完整性 (Priority: P2)

**目标**: 管理员在后台查看所有数据类的采集状态卡片和两融数据按周完整性。

**独立测试**: 运行一次成功和一次失败采集，打开后台页面验证状态卡片正确显示。删除两天两融数据，验证缺口检测正确标记。

### 用户故事 2 实现

- [ ] T036 [US2] 实现 `GapDetectionService`：对比 `trade_calendar` 与 `margin_daily` 的 `trade_date` → 返回缺失日期列表并按周分组，路径 `src/main/java/com/tradingdiary/service/GapDetectionService.java`
- [ ] T037 [US2] 实现 `CollectionController` `GET /api/v1/admin/collection/status`：查询每个 `data_type` 的最新 FETCH + CLEANSE 日志 → 构建 `CollectionStatusVO` 列表，路径 `src/main/java/com/tradingdiary/collection/controller/CollectionController.java`
- [ ] T038 [US2] 实现 `CollectionController` `GET /api/v1/admin/collection/logs`：按 `data_type` 分页返回最近采集日志，路径 `src/main/java/com/tradingdiary/collection/controller/CollectionController.java`
- [ ] T039 [US2] 实现 `CollectionController` `GET /api/v1/admin/collection/gaps`：调用 `GapDetectionService` → 按周聚合 → 返回 `GapReportVO`，路径 `src/main/java/com/tradingdiary/collection/controller/CollectionController.java`
- [ ] T040 [US2] 为所有 `CollectionController` 端点添加 `@PreAuthorize("hasRole('ADMIN')")`，路径 `src/main/java/com/tradingdiary/collection/controller/CollectionController.java`
- [ ] T041 [US2] 创建前端采集状态页面：展示数据类状态卡片（FETCH + CLEANSE 状态、时间戳、记录数、错误信息，点击展开最近 5 条日志），路径 `frontend/src/app/(dashboard)/admin/collection/page.tsx`
- [ ] T042 [US2] 创建前端两融完整性页面：按周表格（周范围、交易所、应采/已采/缺失日期、状态标记，日期筛选器，刷新缺口按钮），路径 `frontend/src/app/(dashboard)/admin/collection/margin/page.tsx`
- [ ] T043 [US2] 在侧边栏添加"数据采集"导航菜单项，子项"采集状态"和"两融完整性"，路径 `frontend/src/components/layout/`
- [ ] T044 [US2] 端到端验证：打开采集状态页面 → 看到数据类状态卡片（共 9 个枚举类型：`STOCK_INFO`、`STOCK_DAILY`、`TRADE_CALENDAR`、`INDUSTRY_NAME`、`INDUSTRY_CONS`、`CONCEPT_NAME`、`CONCEPT_CONS`、`MARGIN_DAILY_SSE`、`MARGIN_DAILY_SZSE`）状态正确；打开两融完整性页面 → 选择日期范围 → 看到按周表格和缺口检测结果

**检查点**: US2 完整可用 — 管理员可在后台监控采集健康和数据完整性

---

## 阶段 5：用户故事 3 — 手动触发与补采 (Priority: P3)

**目标**: 管理员可手动触发任意数据类的采集，按日期范围补采缺失数据，按周分片支持断点续传。

**独立测试**: 对"股票基础信息"点手动触发 → 验证即时启动采集并更新状态。在两融页面选日期范围 → 点补采 → 观察按周分片创建和缺口消除。

### 用户故事 3 实现

- [ ] T045 [US3] 实现 `CollectionController` `POST /api/v1/admin/collection/trigger/{dataType}`：创建编排任务，返回"已启动"或 409"已在执行中"，路径 `src/main/java/com/tradingdiary/collection/controller/CollectionController.java`
- [ ] T046 [US3] 实现 `CollectionController` `POST /api/v1/admin/collection/backfill`：解析日期范围 → 按周分组 → 队列化周分片（含 `week_start`/`week_end`），路径 `src/main/java/com/tradingdiary/collection/controller/CollectionController.java`
- [ ] T047 [US3] 实现 `CollectionOrchestrator.backfillMarginByWeek()`：遍历周分片，逐日调用 `orchestrate()`，已完成周分片不重复执行（断点续传），路径 `src/main/java/com/tradingdiary/collection/orchestrator/CollectionOrchestrator.java`
- [ ] T048 [US3] 在采集状态页面添加 [重新采集] 按钮处理逻辑（调用 `POST /trigger/{dataType}`），路径 `frontend/src/app/(dashboard)/admin/collection/page.tsx`
- [ ] T049 [US3] 在两融完整性页面添加补采弹窗（数据类型下拉、交易所下拉、日期范围选择器、提交按钮调用 `POST /backfill`），路径 `frontend/src/app/(dashboard)/admin/collection/margin/page.tsx`
- [ ] T050 [US3] 在每行不完整周记录上添加 [补采缺失] 按钮（对该周缺失日期调用 `POST /backfill`），路径 `frontend/src/app/(dashboard)/admin/collection/margin/page.tsx`
- [ ] T051 [US3] 端到端验证：手动触发 → 状态实时更新；补采 → 周分片创建 → 缺口消除 → 完整性页面显示"完整"

**检查点**: US3 完整可用 — 管理员可手动触发和补采任意数据类型

---

## 阶段 6：用户故事 4 — 行业与概念分类变更追踪 (Priority: P3)

**目标**: 系统每日检测股票行业/概念分类变化，记录 ADD/REMOVE 事件到变更日志。

**独立测试**: 修改 `stock_industry` 数据模拟某股从"电子"移入"计算机"，执行 CLEANSE，验证 `classification_change_log` 记录 REMOVE 和 ADD 两条事件。

### 用户故事 4 实现

- [ ] T052 [US4] 在 `IndustryCleanseService` 中实现变化检测逻辑：对比当天 `stock_board_industry_cons_em` 结果与数据库当前记录 → 新增的 `INSERT` + 消失的 `DELETE`（或软删除）→ 变化事件写入 `classification_change_log`（`action=ADD/REMOVE`），路径 `src/main/java/com/tradingdiary/service/collection/IndustryCleanseService.java`
- [ ] T053 [P] [US4] 在 `ConceptCleanseService` 中实现相同变化检测逻辑（行业分类用单分类模式，概念用多分类模式），路径 `src/main/java/com/tradingdiary/service/collection/ConceptCleanseService.java`
- [ ] T054 [US4] 为 `ClassificationChangeLogMapper` 添加查询方法（`selectByStockAndType`、`selectByDateRange`），路径 `src/main/java/com/tradingdiary/mapper/ClassificationChangeLogMapper.java`
- [ ] T055 [US4] 编写行业变化检测单元测试：模拟旧 vs 新成分股数据，验证 ADD/REMOVE 事件和 `stock_industry` 最终状态正确，路径 `src/test/java/com/tradingdiary/service/collection/IndustryCleanseServiceTest.java`
- [ ] T056 [P] [US4] 编写概念变化检测单元测试：模拟旧 vs 新成分股数据，验证 ADD/REMOVE 事件，路径 `src/test/java/com/tradingdiary/service/collection/ConceptCleanseServiceTest.java`
- [ ] T057 [US4] 端到端验证：连续执行两次采集（中间模拟行业变化）→ 验证 `classification_change_log` 记录正确

**检查点**: US4 完整可用 — 分类变化自动检测并记录

---

## 阶段 7：收尾与横切关注点

**目的**: 集成测试、前端收尾、端到端验证、性能验证

- [ ] T058 [P] 编写 `CollectionController` MockMvc 集成测试：`GET /api/v1/admin/collection/status`，路径 `src/test/java/com/tradingdiary/collection/controller/CollectionControllerTest.java`
- [ ] T059 [P] 编写 `CollectionController` MockMvc 集成测试：`POST /api/v1/admin/collection/trigger`，路径 `src/test/java/com/tradingdiary/collection/controller/CollectionControllerTest.java`
- [ ] T060 编写 Playwright E2E 测试：打开采集状态页面，验证状态卡片展示，路径 `e2e/collection-status.spec.ts`
- [ ] T061 [P] 编写 Playwright E2E 测试：打开两融完整性页面，验证按周表格和缺口检测，路径 `e2e/collection-margin-gaps.spec.ts`
- [ ] T062 [P] 验证 SC-002（重试 ≤ 30s）：Mock AKTools 连续 3 次失败，断言总耗时 ≤ 30s（2s + 4s + 8s = 14s × 3 次上限），路径 `src/test/java/com/tradingdiary/collection/client/AKToolsClientTest.java`
- [ ] T063 [P] 验证 SC-007（回填 ≤ 2h）：测量单周分片（~5 个交易日 × 2 交易所）耗时，外推全历史窗口确认单周耗时 × 总分片数 ≤ 2h
- [ ] T064 执行 `quickstart.md` 验证：确认所有命令正常执行
- [ ] T065 代码审查清理：检查所有类是否有正确的 `@Component`/`@Service`/`@Repository` 注解，清理多余注释和 import
- [ ] T066 安全检查：验证所有 `CollectionController` 端点需 `ADMIN` 角色，错误响应不含敏感信息

---

## 依赖与执行顺序

### 阶段依赖

- **搭建（阶段 1）**: 无依赖 — 可立即开始
- **基础设施（阶段 2）**: 依赖 T019 — **阻塞所有用户故事**
- **用户故事 1 (阶段 3)**: 依赖基础设施 — MVP
- **用户故事 2 (阶段 4)**: 依赖基础设施 — 可与 US1 并行
- **用户故事 3 (阶段 5)**: 依赖 US2（共用 `CollectionController` 和前端页面）— US2 的 controller/frontend 创建后可开始
- **用户故事 4 (阶段 6)**: 依赖 US1（扩展 `IndustryCleanseService` 和 `ConceptCleanseService`）— US1 的 cleanse service 创建后可开始
- **收尾（阶段 7）**: 依赖所有需要的故事已完成

### 用户故事依赖

- **US1 (P1)**: 基础设施完成后即可开始 — 为 US3（复用 scheduler + cleanse）和 US4（扩展 cleanse service）提供基础
- **US2 (P2)**: 基础设施完成后即可开始 — 可独立于 US1 测试（用手工插入的日志条目）
- **US3 (P3)**: 扩展 US2 的 controller + 前端 — 需等待 T037-T039、T041-T042 存在
- **US4 (P3)**: 扩展 US1 的 cleanse service — 需等待 T028-T029 存在

### 并行机会

- **阶段 1**: T002-T018（全部 entity + mapper）可并行
- **阶段 2**: T021-T023 可与 T020 并行
- **阶段 3**: T026-T029（4 个 cleanse service）可并行
- **阶段 4**: T037-T039（3 个 controller 端点）+ T041-T042（2 个前端页面）可在阶段内并行
- **阶段 5**: US2 的 controller/frontend 就绪后，T045-T048 可重叠
- **阶段 6**: T052 和 T053 可并行；T055 和 T056 可并行
- **阶段 7**: 全部测试（T058-T063）可并行
- **US1 和 US2** 在基础设施完成后可完全并行开发

---

## 并行示例：用户故事 1

```bash
# 4 个 cleanse service 同时启动：
Task: "实现 StockInfoCleanseService 路径 src/main/java/com/tradingdiary/service/collection/StockInfoCleanseService.java"
Task: "实现 StockDailyCleanseService 路径 src/main/java/com/tradingdiary/service/collection/StockDailyCleanseService.java"
Task: "实现 IndustryCleanseService 路径 src/main/java/com/tradingdiary/service/collection/IndustryCleanseService.java"
Task: "实现 ConceptCleanseService 路径 src/main/java/com/tradingdiary/service/collection/ConceptCleanseService.java"
```

---

## 实施策略

### MVP 优先（仅用户故事 1）

1. 完成阶段 1：搭建 (T001-T019)
2. 完成阶段 2：基础设施 (T020-T025)
3. 完成阶段 3：用户故事 1 (T026-T035)
4. **停止并验证**: 端到端确认每日自动采集正常
5. 可部署/演示 — **数据已流动，这是可工作的系统**

### 增量交付

1. 搭建 + 基础设施 → 基础就绪
2. 加入 US1 → 自动采集运行 → 部署/演示 (MVP!)
3. 加入 US2 → 管理后台可见 → 部署/演示
4. 加入 US3 → 手动控制就绪 → 部署/演示
5. 加入 US4 → 分类追踪 → 部署/演示
6. 每个故事不破坏之前的故事独立增值

### 并行团队策略

多人协作：

1. 团队共同完成搭建 + 基础设施
2. 基础设施完成后：
   - 开发者 A：用户故事 1（后端采集流水线）
   - 开发者 B：用户故事 2（后端 API + 前端页面）
3. US1 + US2 完成后：
   - 开发者 A：用户故事 4（扩展 cleanse service）
   - 开发者 B：用户故事 3（手动触发 + 补采）
4. 双方汇合到阶段 7（测试 + 收尾）

---

## 附注

- `[P]` 任务 = 不同文件，无依赖，可并行
- `[Story]` 标签将任务映射到具体用户故事，支持可追溯
- 每个用户故事可独立完成和测试
- 每个任务或逻辑组完成后提交
- 可在任意检查点停下来独立验证故事
- US1 和 US2 在基础设施完成后真正可并行（US2 可在 US1 运行前用手造日志测试）
- US3 扩展 US2 的 controller/frontend，需等待文件创建
- US4 扩展 US1 的 cleanse service，需等待文件创建
