# 数据采集页面 UI/UE 重构 — 实施计划

**分支**: `001-collection-ui-redesign` | **日期**: 2026-05-20 | **规范**: [spec.md](./spec.md)
**输入**: 功能规范来自 `/specs/003-data-collection/spec.md`

## 概述

将原单一采集状态页面拆分为 Hub + 子页面架构。新增"数据浏览"侧边栏分组（4 页）。一期实现：后端聚合 API + 7 个前端页面（Hub 改造、股票行情详情、成分股管理、股票列表/详情、概念列表、行业列表）。

**技术方案**: 后端新增 3 个 Controller（StockDataController、MarketDataController、MarginStatsController），Flyway V5 迁移（日志加字段 + margin_daily 加 change + index_daily 建表）。前端 Next.js 14 App Router，lightweight-charts 图表。

## 技术上下文

**Language/Version**: Java 17 + TypeScript (Next.js 14)
**Primary Dependencies**: Spring Boot 3.2, MyBatis-Plus 3.5, AKTools HTTP API, lightweight-charts, TailwindCSS
**Storage**: MySQL 8.0 (Flyway 迁移)
**Testing**: JUnit 5 + Mockito + H2 + MockMvc + @MybatisPlusTest / Vitest + Playwright
**Target Platform**: Web (Linux server + browser)
**Project Type**: web-service (backend API + frontend SPA)
**Performance Goals**: 聚合查询 < 500ms（需索引优化）
**Constraints**: 一期不包含 INDEX_DAILY 采集和融资详情页
**Scale/Scope**: ~12 张新表/修改表, 4 个新 Controller, 7 个新前端页面, 1 个 Flyway 迁移

## 宪法检查

*门禁: 阶段 0 研究前通过。阶段 1 设计后重新检查。*

Phase 1 门禁全部通过：

- [x] 包路径在 `com.tradingdiary` 下
- [x] Controller 未直接注入 Mapper（分层不可逆）
- [x] API 响应使用 `ApiResponse<T>` 包装
- [x] 金额字段使用 `BigDecimal`
- [x] `BigDecimal` 除法使用 `divide(value, scale, RoundingMode.HALF_UP)`
- [x] 新表 `index_daily` 含 `id`、`created_at`、`updated_at`、`is_deleted`
- [x] 新表 `index_daily` 不含 `user_id`（合理例外：公共市场数据）
- [x] 时间 UTC 存储
- [x] JWT + Spring Security（已就位，新 Controller 使用 `@PreAuthorize("hasRole('ADMIN')")`）
- [x] 命名符合约定
- [x] 输入验证：Controller 参数手动校验
- [x] 事务管理：Service 方法 `@Transactional`

无违规。

## 项目结构

### 文档（本功能）

```text
specs/003-data-collection/
├── plan.md              # 本文件
├── research.md          # 阶段 0 输出
├── data-model.md        # 阶段 1 输出
├── quickstart.md        # 阶段 1 输出
├── contracts/           # 阶段 1 输出（API 契约）
└── tasks.md             # 阶段 2 输出 (/speckit-tasks)
```

### 源代码

```text
# 后端
src/main/java/com/tradingdiary/
├── collection/
│   ├── controller/CollectionController.java    # 不改（日志字段自动映射）
│   ├── controller/StockDataController.java     # 新增：股票数据 API
│   ├── controller/MarketDataController.java    # 新增：概念/行业列表 API
│   ├── controller/MarginStatsController.java   # 新增：融资统计 API
│   ├── client/AKToolsClient.java               # 新增指数采集方法
│   ├── orchestrator/CollectionOrchestrator.java # 新增 INDEX_DAILY case
│   ├── model/                                   # 新增 VO/DTO
│   └── scheduler/CollectionScheduler.java       # 新增 INDEX_DAILY 调度
├── entity/
│   ├── DataCollectionLog.java                  # 加字段
│   ├── MarginDaily.java                         # 加字段
│   └── IndexDaily.java                          # 新增
├── mapper/
│   ├── DataCollectionLogMapper.java             # 不改（字段自动映射）
│   ├── MarginDailyMapper.java                   # 不改
│   └── IndexDailyMapper.java                    # 新增
└── service/collection/
    ├── MarginCleanseService.java                # 加 change 计算
    └── IndexCleanseService.java                 # 新增

src/main/resources/db/migration/
└── V5__ui_redesign_schema.sql                   # 新增迁移

src/test/java/com/tradingdiary/
├── controller/
│   ├── StockDataControllerTest.java
│   ├── MarketDataControllerTest.java
│   └── MarginStatsControllerTest.java
└── collection/
    └── IndexCleanseServiceTest.java

# 前端
frontend/src/app/(dashboard)/
├── layout.tsx                                    # 侧边栏重构
├── admin/
│   ├── collection/
│   │   ├── page.tsx                              # Hub 页改造
│   │   ├── stocks/page.tsx                       # 新：股票行情详情
│   │   ├── constituents/page.tsx                 # 新：成分股管理
│   │   └── margin/page.tsx                       # 不变
│   ├── stocks/
│   │   ├── page.tsx                              # 新：股票列表
│   │   └── [code]/page.tsx                       # 新：股票详情
│   ├── concepts/page.tsx                         # 新：概念列表
│   ├── industries/page.tsx                       # 新：行业列表
│   └── margin-stats/
│       ├── page.tsx                              # 二期
│       ├── market/page.tsx                       # 二期
│       ├── industry/[code]/page.tsx              # 二期
│       └── concept/[code]/page.tsx               # 二期
```

## 复杂度追踪

> 无违规。
