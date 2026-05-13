# 任务列表：数据采集层

**输入**: 设计文档来自 `specs/003-data-collection/`
**前置条件**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**组织方式**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## 格式: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

- **Backend**: `src/main/java/com/tradingdiary/`
- **Frontend**: `frontend/src/`
- **Migrations**: `src/main/resources/db/migration/`
- **Tests**: `src/test/java/com/tradingdiary/`

---

## 阶段 1：搭建（共享基础设施）

**目的**: Project initialization — Flyway migration, entity/mapper scaffolding

- [ ] T001 Create Flyway migration V3__collection_schema.sql with all 12 tables (data_collection_log, raw_data, trade_calendar, stock_info, stock_daily, industry, stock_industry, concept, stock_concept, classification_change_log, margin_stock, margin_daily) in `src/main/resources/db/migration/V3__collection_schema.sql`
- [ ] T002 [P] Create DataCollectionLog entity in `src/main/java/com/tradingdiary/entity/DataCollectionLog.java`
- [ ] T003 [P] Create RawData entity in `src/main/java/com/tradingdiary/entity/RawData.java`
- [ ] T004 [P] Create TradeCalendar entity in `src/main/java/com/tradingdiary/entity/TradeCalendar.java`
- [ ] T005 [P] Create StockInfo entity in `src/main/java/com/tradingdiary/entity/StockInfo.java`
- [ ] T006 [P] Create StockDaily entity in `src/main/java/com/tradingdiary/entity/StockDaily.java`
- [ ] T007 [P] Create Industry entity in `src/main/java/com/tradingdiary/entity/Industry.java`
- [ ] T008 [P] Create StockIndustry entity in `src/main/java/com/tradingdiary/entity/StockIndustry.java`
- [ ] T009 [P] Create Concept entity in `src/main/java/com/tradingdiary/entity/Concept.java`
- [ ] T010 [P] Create StockConcept entity in `src/main/java/com/tradingdiary/entity/StockConcept.java`
- [ ] T011 [P] Create ClassificationChangeLog entity in `src/main/java/com/tradingdiary/entity/ClassificationChangeLog.java`
- [ ] T012 [P] Create MarginStock entity in `src/main/java/com/tradingdiary/entity/MarginStock.java`
- [ ] T013 [P] Create MarginDaily entity in `src/main/java/com/tradingdiary/entity/MarginDaily.java`
- [ ] T014 [P] Create DataCollectionLogMapper in `src/main/java/com/tradingdiary/mapper/DataCollectionLogMapper.java`
- [ ] T015 [P] Create RawDataMapper in `src/main/java/com/tradingdiary/mapper/RawDataMapper.java`
- [ ] T016 [P] Create MarginDailyMapper in `src/main/java/com/tradingdiary/mapper/MarginDailyMapper.java`
- [ ] T017 [P] Create StockInfoMapper in `src/main/java/com/tradingdiary/mapper/StockInfoMapper.java`
- [ ] T018 [P] Create remaining mappers (TradeCalendar, StockDaily, Industry, StockIndustry, Concept, StockConcept, ClassificationChangeLog, MarginStock) in `src/main/java/com/tradingdiary/mapper/`
- [ ] T019 Verify Flyway migration succeeds and all tables created correctly: `./gradlew bootRun --args='--spring.profiles.active=dev'`

**检查点**: 12 张表全部就位，entity + mapper 可用

---

## 阶段 2：基础设施（阻塞性前置）

**目的**: AKToolsClient + CollectionOrchestrator — MUST be complete before ANY user story

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T020 Implement AKToolsClient with RestClient and all 9 fetch methods in `src/main/java/com/tradingdiary/collection/client/AKToolsClient.java`
- [ ] T021 [P] Implement TradeCalendarService (fetch + incremental sync) in `src/main/java/com/tradingdiary/service/collection/TradeCalendarService.java`
- [ ] T022 [P] Create CollectionStatusVO (per-contract response model) in `src/main/java/com/tradingdiary/collection/model/CollectionStatusVO.java`
- [ ] T023 [P] Create GapReportVO (per-contract gap response model) in `src/main/java/com/tradingdiary/collection/model/GapReportVO.java`
- [ ] T024 Implement CollectionOrchestrator with FETCH→CLEANSE lifecycle, per-key concurrency lock, and 3-retry logic in `src/main/java/com/tradingdiary/collection/orchestrator/CollectionOrchestrator.java`
- [ ] T025 Verify AKToolsClient connectivity by running TradeCalendarService.fetch and inspect data_collection_log + raw_data tables

**检查点**: AKTools connectivity confirmed, orchestration pipeline ready

---

## 阶段 3：用户故事 1 - 定时自动采集数据 (Priority: P1) 🎯 MVP

**目标**: System auto-collects 6 data types on schedule. Daily 16:00-18:00 full collection completes within 30 min.

**独立测试**: Configure two trading days, after market close observe all stock/margin/industry/concept data auto-populated in DB without manual intervention.

### 用户故事 1 实现

- [ ] T026 [P] [US1] Implement StockInfoCleanseService (parse stock_zh_a_spot_em JSON → INSERT ON DUPLICATE KEY UPDATE stock_info) in `src/main/java/com/tradingdiary/service/collection/StockInfoCleanseService.java`
- [ ] T027 [P] [US1] Implement StockDailyCleanseService (extract OHLC from stock_zh_a_spot_em JSON → INSERT ON DUPLICATE KEY UPDATE stock_daily) in `src/main/java/com/tradingdiary/service/collection/StockDailyCleanseService.java`
- [ ] T028 [P] [US1] Implement IndustryCleanseService (parse industry names JSON + iterate industry cons → batch INSERT) in `src/main/java/com/tradingdiary/service/collection/IndustryCleanseService.java`
- [ ] T029 [P] [US1] Implement ConceptCleanseService (parse concept names JSON + iterate concept cons → batch INSERT) in `src/main/java/com/tradingdiary/service/collection/ConceptCleanseService.java`
- [ ] T030 [US1] Implement MarginCleanseService (parse stock_margin_detail_sse/szse JSON → INSERT margin_daily + extract margin_stock) in `src/main/java/com/tradingdiary/service/collection/MarginCleanseService.java`
- [ ] T031 [US1] Wire AKToolsClient.fetchXxx() → CleanseService mapping in CollectionOrchestrator.orchestrate() per data_type enum in `src/main/java/com/tradingdiary/collection/orchestrator/CollectionOrchestrator.java`
- [ ] T032 [US1] Implement CollectionScheduler with @Scheduled cron tasks (16:00 stock spot, 17:00 industry/concept, 18:00 margin daily) and @Profile("!test") in `src/main/java/com/tradingdiary/collection/scheduler/CollectionScheduler.java`
- [ ] T033 [US1] Add 200ms rate limiter and 4-thread async executor for bulk stock_daily calls in `src/main/java/com/tradingdiary/collection/client/AKToolsClient.java`
- [ ] T034 [US1] Implement raw_data monthly archive job (@Scheduled 1st day of month, GZIP JSON Lines to backups/raw_data/) in `src/main/java/com/tradingdiary/collection/scheduler/CollectionScheduler.java`
- [ ] T035 [US1] Verify end-to-end: simulate trading day → observe all 6 data types collected, FETCH+CLEANSE logs recorded, margin_stock auto-populated

**检查点**: US1 fully functional — daily auto collection runs without manual intervention

---

## 阶段 4：用户故事 2 - 查看采集状态与数据完整性 (Priority: P2)

**目标**: Admin views collection status cards for all data types and margin data completeness by week on management console.

**独立测试**: Run a successful and a failed collection, open backend page, verify status cards show correct state. Delete 2 days of margin data, verify gap detection shows missing dates.

### 用户故事 2 实现

- [ ] T036 [US2] Implement GapDetectionService (compare trade_calendar vs margin_daily, return missing trade_dates grouped by week) in `src/main/java/com/tradingdiary/service/GapDetectionService.java`
- [ ] T037 [US2] Implement CollectionController GET /api/v1/admin/collection/status (query latest FETCH+CLEANSE per data_type, build CollectionStatusVO list) in `src/main/java/com/tradingdiary/collection/controller/CollectionController.java`
- [ ] T038 [US2] Implement CollectionController GET /api/v1/admin/collection/logs (paginated recent logs per data_type) in `src/main/java/com/tradingdiary/collection/controller/CollectionController.java`
- [ ] T039 [US2] Implement CollectionController GET /api/v1/admin/collection/gaps (invoke GapDetectionService, group by week, return GapReportVO) in `src/main/java/com/tradingdiary/collection/controller/CollectionController.java`
- [ ] T040 [US2] Add @PreAuthorize("hasRole('ADMIN')") to all CollectionController endpoints in `src/main/java/com/tradingdiary/collection/controller/CollectionController.java`
- [ ] T041 [US2] Create frontend collection status page with status cards (FETCH + CLEANSE states, timestamps, record counts, error messages, expand for recent logs) in `frontend/src/app/(dashboard)/admin/collection/page.tsx`
- [ ] T042 [US2] Create frontend margin completeness page with week table (week range, exchange, expected/collected/missing, status badges, date filter, refresh gap button) in `frontend/src/app/(dashboard)/admin/collection/margin/page.tsx`
- [ ] T043 [US2] Add "数据采集" nav menu item with sub-items ("采集状态", "两融完整性") to sidebar layout in `frontend/src/components/layout/`
- [ ] T044 [US2] Verify end-to-end: open collection status page → see all 9 data type cards with correct statuses; open margin completeness page → select date range → see week table with gap detection results

**检查点**: US2 fully functional — admin can monitor collection health and data completeness from management console

---

## 阶段 5：用户故事 3 - 手动触发与补采 (Priority: P3)

**目标**: Admin can manually trigger collection for any data type and backfill missing margin data by date range with week-based sharding.

**独立测试**: Click manual trigger on "股票基础信息" → verify immediate collection starts with status update. Select date range on margin page → click backfill → observe weekly shards created and gaps resolved.

### 用户故事 3 实现

- [ ] T045 [US3] Implement CollectionController POST /api/v1/admin/collection/trigger/{dataType} (create orchestrator task, return "已启动" or 409 if already running) in `src/main/java/com/tradingdiary/collection/controller/CollectionController.java`
- [ ] T046 [US3] Implement CollectionController POST /api/v1/admin/collection/backfill (parse date range, group by week, queue weekly shards with week_start/week_end in data_collection_log) in `src/main/java/com/tradingdiary/collection/controller/CollectionController.java`
- [ ] T047 [US3] Implement backfillMarginByWeek() in CollectionOrchestrator (iterate weekly shards, call orchestrate for each date in week, support resume on interrupted shards) in `src/main/java/com/tradingdiary/collection/orchestrator/CollectionOrchestrator.java`
- [ ] T048 [US3] Add [重新采集] button handler on collection status page (POST /trigger/{dataType}) in `frontend/src/app/(dashboard)/admin/collection/page.tsx`
- [ ] T049 [US3] Add backfill dialog on margin completeness page (data type dropdown, exchange dropdown, date range picker, submit button calling POST /backfill) in `frontend/src/app/(dashboard)/admin/collection/margin/page.tsx`
- [ ] T050 [US3] Add [补采缺失] button on each incomplete week row (calls POST /backfill for that week's missing dates) in `frontend/src/app/(dashboard)/admin/collection/margin/page.tsx`
- [ ] T051 [US3] Verify end-to-end: manual trigger → status updates in realtime; backfill → week shards created → gaps resolved → completeness page shows COMPLETE

**检查点**: US3 fully functional — admin can manually trigger and backfill any data type

---

## 阶段 6：用户故事 4 - 行业与概念分类变更追踪 (Priority: P3)

**目标**: System detects stock industry/concept classification changes daily, records ADD/REMOVE events in change log.

**独立测试**: Modify stock_industry data to simulate a stock moving from "电子" to "计算机", run CLEANSE, verify classification_change_log records both REMOVE and ADD entries.

### 用户故事 4 实现

- [ ] T052 [US4] Implement change detection logic in IndustryCleanseService (compare today's stock_industry_cons vs current DB, INSERT new, DELETE removed, log ADD/REMOVE to classification_change_log) in `src/main/java/com/tradingdiary/service/collection/IndustryCleanseService.java`
- [ ] T053 [P] [US4] Implement change detection logic in ConceptCleanseService (same comparison logic as industry but for stock_concept) in `src/main/java/com/tradingdiary/service/collection/ConceptCleanseService.java`
- [ ] T054 [US4] Add ClassificationChangeLogMapper query methods (selectByStockAndType, selectByDateRange) in `src/main/java/com/tradingdiary/mapper/ClassificationChangeLogMapper.java`
- [ ] T055 [US4] Write unit test for industry change detection: simulate old vs new cons, verify ADD/REMOVE events and stock_industry state correctness in `src/test/java/com/tradingdiary/service/collection/IndustryCleanseServiceTest.java`
- [ ] T056 [P] [US4] Write unit test for concept change detection: simulate old vs new cons, verify ADD/REMOVE events in `src/test/java/com/tradingdiary/service/collection/ConceptCleanseServiceTest.java`
- [ ] T057 [US4] Verify end-to-end: run two consecutive collection runs with a simulated industry change between them → verify classification_change_log has correct entries

**检查点**: US4 fully functional — classification changes automatically detected and logged

---

## 阶段 7：收尾与横切关注点

**目的**: Integration tests, frontend polish, end-to-end validation

- [ ] T058 [P] Write MockMvc integration test for GET /api/v1/admin/collection/status in `src/test/java/com/tradingdiary/collection/controller/CollectionControllerTest.java`
- [ ] T059 [P] Write MockMvc integration test for POST /api/v1/admin/collection/trigger in `src/test/java/com/tradingdiary/collection/controller/CollectionControllerTest.java`
- [ ] T060 Write Playwright E2E test: open collection status page, verify cards display in `e2e/collection-status.spec.ts`
- [ ] T061 [P] Write Playwright E2E test: open margin completeness page, verify week table and gap detection in `e2e/collection-margin-gaps.spec.ts`
- [ ] T062 Run quickstart.md validation: verify all commands execute successfully
- [ ] T063 Code review cleanup: verify all classes have proper @Component/@Service/@Repository annotations, consistent JavaDoc removal, import organization
- [ ] T064 Security check: verify all CollectionController endpoints require ADMIN role, no sensitive data in error responses

---

## 依赖与执行顺序

### 阶段依赖

- **搭建（阶段 1）**: No dependencies — can start immediately
- **基础设施（阶段 2）**: Depends on Setup completion (T019) — BLOCKS all user stories
- **用户故事 1 (阶段 3)**: Depends on Foundational — MVP
- **用户故事 2 (阶段 4)**: Depends on Foundational — Can run in parallel with US1
- **用户故事 3 (阶段 5)**: Depends on US2 (uses same controller + frontend pages) — Can start after US2 controller/frontend are in place
- **用户故事 4 (阶段 6)**: Depends on US1 (extend IndustryCleanseService and ConceptCleanseService from US1) — Can start after US1 cleanse services exist
- **收尾（阶段 7）**: Depends on all desired user stories being complete

### 用户故事依赖

- **用户故事 1 (P1)**: Can start after Foundational — No dependencies on other stories. Foundation for US3 (reuses scheduler+cleanse) and US4 (extends cleanse services)
- **用户故事 2 (P2)**: Can start after Foundational — No dependencies on US1 (reads data_collection_log written by US1, but can be tested independently with manually inserted log entries)
- **用户故事 3 (P3)**: Extends US2 controller + frontend — Should start after T037-T039, T041-T042 exist
- **用户故事 4 (P3)**: Extends US1 cleanse services — Should start after T028-T029 exist

### 并行机会

- **Phase 1**: T002-T018 (all entities + mappers) can run in parallel
- **Phase 2**: T021-T023 can run in parallel with T020
- **Phase 3**: T026-T029 (4 cleanse services) can run in parallel
- **Phase 4**: T037-T039 (3 controller endpoints) + T041-T042 (2 frontend pages) can run in parallel within phase
- **Phase 5**: After US2 controller/frontend exist, T045-T048 can overlap
- **Phase 6**: T052 and T053 can run in parallel; T055 and T056 can run in parallel
- **Phase 7**: All tests (T058-T061) can run in parallel
- **US1 and US2** can be developed in parallel after Foundational phase

---

## 并行示例：用户故事 1

```bash
# Launch all 4 cleanse services together:
Task: "Implement StockInfoCleanseService in src/main/java/com/tradingdiary/service/collection/StockInfoCleanseService.java"
Task: "Implement StockDailyCleanseService in src/main/java/com/tradingdiary/service/collection/StockDailyCleanseService.java"
Task: "Implement IndustryCleanseService in src/main/java/com/tradingdiary/service/collection/IndustryCleanseService.java"
Task: "Implement ConceptCleanseService in src/main/java/com/tradingdiary/service/collection/ConceptCleanseService.java"
```

---

## 实施策略

### MVP 优先（仅用户故事 1）

1. Complete Phase 1: Setup (T001-T019)
2. Complete Phase 2: Foundational (T020-T025)
3. Complete Phase 3: User Story 1 (T026-T035)
4. **STOP and VALIDATE**: Verify daily auto-collection works end-to-end
5. Deploy/demo if ready — **data is flowing, this is a working system**

### 增量交付

1. Setup + Foundational → Infrastructure ready
2. Add US1 → Auto-collection running → Deploy/Demo (MVP!)
3. Add US2 → Management console visible → Deploy/Demo
4. Add US3 → Manual control → Deploy/Demo
5. Add US4 → Classification tracking → Deploy/Demo
6. Each story adds value without breaking previous stories

### 并行团队策略

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (backend collection pipeline)
   - Developer B: User Story 2 (backend API + frontend pages)
3. After US1 + US2:
   - Developer A: User Story 4 (extend cleanse services)
   - Developer B: User Story 3 (manual trigger + backfill)
4. Both converge on Phase 7 (tests + polish)

---

## 附注

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- US1 and US2 can truly start in parallel after Foundational (US2 can test with fabricated collection logs before US1 runs)
- US3 extends US2's controller/frontend — cannot begin until those files exist
- US4 extends US1's cleanse services — cannot begin until those files exist
