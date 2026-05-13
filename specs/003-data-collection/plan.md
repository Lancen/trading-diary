# 实施计划：数据采集层

**分支**: `003-data-collection` | **日期**: 2026-05-13 | **规范**: [spec.md](./spec.md)
**输入**: 功能规范来自 `specs/003-data-collection/spec.md`
**技术上下文**: 来自 `docs/superpowers/specs/2026-05-13-data-collection-design.md`

## 概述

构建全量 A 股数据采集系统，通过 AKTools Docker HTTP API 从 Akshare 获取 6 类数据（股票基础信息、日线行情、行业板块、概念板块、交易日历、两融交易明细），采用 FETCH（原始 JSON 暂存）→ CLEANSE（解析清洗写入）两步流程，支持定时自动采集、管理后台手动触发和按周历史回填。

## 技术上下文

**Language/Version**: Java 17+, TypeScript (Next.js 14+)
**Primary Dependencies**: Spring Boot 3.3.5, MyBatis-Plus 3.5.9, Flyway 9.22.3, Spring Security 6.3.x, Spring RestClient
**Storage**: MySQL 8.0+ (dev/prod), H2 (test only)
**Testing**: JUnit 5 + Mockito + H2 + MockMvc, Playwright (E2E)
**Target Platform**: Linux server (AKTools Docker + Spring Boot + Next.js)
**Project Type**: web-service (backend monolith + frontend SPA)
**Performance Goals**: Daily full collection (6 data types) completes within 30 minutes; initial historical backfill (2014-present) within 2 hours
**Constraints**: AKTools Docker must be running; 200ms rate limit between API calls; 4-thread parallel for bulk calls
**Scale/Scope**: ~5000 A-stocks × daily snapshot, ~2000 margin stocks, ~60 industries, ~400 concepts, ~2900 historical trading days

## 宪法检查

*门禁: 必须在阶段 0 研究前通过。阶段 1 设计后重新检查。*

### Phase 0 合规检查

| # | 检查项 | 状态 | 说明 |
|---|--------|------|------|
| 1 | 包路径在 `com.tradingdiary` 下 | ✅ | 新增 `collection/` 子包，不新增顶层包 |
| 2 | Controller 未直接注入 Mapper | ✅ | CollectionController → CollectionOrchestrator → Mapper |
| 3 | Controller 方法未包含业务逻辑 | ✅ | Controller 仅参数绑定 + 委托编排器 |
| 4 | API 响应使用 `ApiResponse<T>` | ✅ | 复用现有 `ApiResponse` 类 |
| 5 | 金额字段使用 `BigDecimal` | ✅ | 所有两融金额字段 `decimal(16,2)` |
| 6 | 数据库表含标准字段 | ✅ | CLAUDE.md §5 规则已覆盖，12 张新表全部含 `id`、`created_at`、`updated_at` |
| 7 | 时间字段 UTC 存储 | ✅ | `started_at`、`completed_at` 等遵循现有规范 |
| 8 | 认证方案 JWT + Spring Security | ✅ | `CollectionController` 需 ADMIN 角色 |
| 9 | 异常继承 `BaseException` | ✅ | 复用现有异常体系 |
| 10 | 命名符合约定 | ✅ | 遵循项目现有命名规范 |

**门禁结果**: 通过 ✅

## 项目结构

### 文档（本功能）

```text
specs/003-data-collection/
├── plan.md              # 本文件
├── research.md          # 阶段 0 输出
├── data-model.md        # 阶段 1 输出
├── quickstart.md        # 阶段 1 输出
├── contracts/           # 阶段 1 输出
└── tasks.md             # 阶段 2 输出 (/speckit-tasks)
```

### 源代码（仓库根目录）

```text
# 后端新增
src/main/java/com/tradingdiary/
├── collection/
│   ├── client/
│   │   └── AKToolsClient.java
│   ├── controller/
│   │   └── CollectionController.java
│   ├── orchestrator/
│   │   └── CollectionOrchestrator.java
│   ├── scheduler/
│   │   └── CollectionScheduler.java
│   └── model/
│       ├── CollectionStatusVO.java
│       └── GapReportVO.java
├── entity/
│   ├── DataCollectionLog.java
│   ├── RawData.java
│   ├── StockInfo.java
│   ├── StockDaily.java
│   ├── TradeCalendar.java
│   ├── Industry.java
│   ├── StockIndustry.java
│   ├── Concept.java
│   ├── StockConcept.java
│   ├── ClassificationChangeLog.java
│   ├── MarginStock.java
│   └── MarginDaily.java
├── mapper/  (对应 Mapper 接口)
└── service/
    ├── collection/
    │   ├── StockInfoCleanseService.java
    │   ├── StockDailyCleanseService.java
    │   ├── IndustryCleanseService.java
    │   ├── ConceptCleanseService.java
    │   ├── MarginCleanseService.java
    │   └── TradeCalendarService.java
    └── GapDetectionService.java

src/main/resources/db/migration/
└── V3__collection_schema.sql

# 前端新增
frontend/src/app/(dashboard)/admin/collection/
├── page.tsx
└── margin/
    └── page.tsx
```

## 复杂度追踪

*本功能无宪法违规需要解释。*
