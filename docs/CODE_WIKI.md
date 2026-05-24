# Trading Diary — Code Wiki

> A 股全市场数据采集与管理平台，覆盖股票行情、日线、两融、行业/概念板块，支持历史补采和交易日历监控。

---

## 目录

1. [项目概览](#1-项目概览)
2. [技术栈与依赖](#2-技术栈与依赖)
3. [项目架构](#3-项目架构)
4. [后端模块详解](#4-后端模块详解)
   - 4.1 [应用入口与配置](#41-应用入口与配置)
   - 4.2 [认证与安全模块](#42-认证与安全模块)
   - 4.3 [数据采集模块 (collection)](#43-数据采集模块-collection)
   - 4.4 [业务服务层](#44-业务服务层)
   - 4.5 [数据访问层 (Mapper)](#45-数据访问层-mapper)
   - 4.6 [实体层 (Entity)](#46-实体层-entity)
   - 4.7 [视图对象 (VO / Model)](#47-视图对象-vo--model)
   - 4.8 [异常处理](#48-异常处理)
   - 4.9 [工具类](#49-工具类)
5. [前端模块详解](#5-前端模块详解)
   - 5.1 [技术选型](#51-技术选型)
   - 5.2 [目录结构](#52-目录结构)
   - 5.3 [路由与页面](#53-路由与页面)
   - 5.4 [状态管理与认证](#54-状态管理与认证)
   - 5.5 [API 通信层](#55-api-通信层)
6. [数据库设计](#6-数据库设计)
7. [模块间依赖关系](#7-模块间依赖关系)
8. [API 接口一览](#8-api-接口一览)
9. [数据采集流程](#9-数据采集流程)
10. [项目运行方式](#10-项目运行方式)
11. [测试体系](#11-测试体系)
12. [工程规范与约定](#12-工程规范与约定)

---

## 1. 项目概览

Trading Diary 是一个 A 股市场数据采集与管理平台，采用前后端分离架构：

- **后端**：Spring Boot 3.3 + MyBatis-Plus + MySQL，负责数据采集编排、清洗入库、REST API
- **前端**：Next.js 14 App Router + TypeScript + TailwindCSS + shadcn/ui，提供管理后台界面
- **外部数据源**：AKTools（封装东方财富/新浪/同花顺 API）、Tushare Pro（日线历史补采）、Playwright（同花顺成分股抓取）

### 核心业务流程

```
外部数据源 → AKTools/Tushare HTTP → 采集(FETCH) → 原始数据(raw_data) → 清洗(CLEANSE) → 业务表(stock_info/stock_daily/margin_daily/...)
```

### 当前阶段

Phase 1（业务开发），当前仅 ADMIN 角色使用，主要用途为数据采集状态监控 + 市场数据浏览。

---

## 2. 技术栈与依赖

### 后端

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 17+ |
| 框架 | Spring Boot | 3.3.5 |
| ORM | MyBatis-Plus | 3.5.9 |
| 安全 | Spring Security | (Boot BOM) |
| JWT | jjwt | 0.12.5 |
| 数据库 | MySQL | 8.0 |
| 数据库迁移 | Flyway | (Boot BOM) |
| API 文档 | SpringDoc OpenAPI | 2.5.0 |
| 构建 | Gradle | 8.x |
| 测试 | JUnit 5 + Spring Boot Test | (Boot BOM) |
| 内存数据库(测试) | H2 | (Boot BOM) |
| 简化代码 | Lombok | (Boot BOM) |

### 前端

| 类别 | 技术 | 版本 |
|------|------|------|
| 框架 | Next.js (App Router) | ^14.2.0 |
| 语言 | TypeScript | ^5.6.0 |
| UI 库 | shadcn/ui (Radix UI) | - |
| 样式 | TailwindCSS | ^3.4.0 |
| 状态管理 | Zustand | ^5.0.0 |
| 数据请求 | TanStack React Query | ^5.60.0 |
| HTTP 客户端 | ky | ^1.7.0 |
| K 线图 | lightweight-charts | ^5.2.0 |
| 图标 | lucide-react | ^0.460.0 |
| E2E 测试 | Playwright | ^1.49.0 |
| 单元测试 | Vitest + Testing Library | ^2.1.0 |

---

## 3. 项目架构

```
trading-diary/
├── src/main/java/com/tradingdiary/     # 后端 Java 源码
│   ├── Application.java                # Spring Boot 启动类
│   ├── collection/                     # 数据采集模块（核心业务）
│   │   ├── client/                     #   外部 API 客户端
│   │   ├── controller/                 #   采集相关 REST 控制器
│   │   ├── model/                      #   采集模块 VO
│   │   ├── orchestrator/               #   采集编排器（核心）
│   │   └── scheduler/                  #   定时调度器
│   ├── config/                         # Spring 配置类
│   ├── controller/                     # 通用 REST 控制器（Auth）
│   ├── entity/                         # 数据库实体（MyBatis-Plus）
│   ├── exception/                      # 异常定义与全局处理
│   ├── mapper/                         # MyBatis-Plus Mapper 接口
│   ├── model/                          # 通用 VO / Request / Response
│   ├── security/                       # JWT + Spring Security 组件
│   ├── service/                        # 业务服务接口与实现
│   │   ├── collection/                 #   采集相关服务
│   │   │   └── impl/                   #     采集服务实现
│   │   └── impl/                       #   通用服务实现
│   └── util/                           # 工具类
├── src/main/resources/
│   ├── application.yml                 # 主配置
│   ├── application-dev.yml             # 开发环境配置
│   ├── application-prod.yml            # 生产环境配置
│   ├── application-test.yml            # 测试环境配置
│   ├── db/migration/                   # Flyway 数据库迁移脚本
│   └── mapper/                         # MyBatis XML Mapper
├── src/test/java/                      # 后端测试
├── frontend/                           # 前端 Next.js 项目
│   ├── src/
│   │   ├── app/                        #   App Router 页面
│   │   ├── components/                 #   组件
│   │   ├── hooks/                      #   自定义 Hooks
│   │   └── lib/                        #   工具库
│   └── e2e/                            #   E2E 测试
├── scripts/                            # 工具脚本
│   ├── check-env.sh                    #   环境检查
│   └── scrape_ths_constituents.py      #   同花顺成分股抓取
├── docs/                               # 项目文档
├── specs/                              # 功能规范
└── build.gradle                        # Gradle 构建配置
```

### 分层架构

```
┌─────────────────────────────────────────────────┐
│                   Controller                     │  REST API 入口
├─────────────────────────────────────────────────┤
│                    Service                       │  业务逻辑
│  ┌───────────────────┐  ┌─────────────────────┐ │
│  │  通用服务          │  │  采集服务             │ │
│  │  AuthService       │  │  *CleanseService     │ │
│  │  StockDataService  │  │  CollectionQuery     │ │
│  │  MarketDataService │  │  MarginStats         │ │
│  │  CalendarService   │  │  ConstituentImport   │ │
│  │  GapDetection      │  │  TradeCalendar       │ │
│  └───────────────────┘  └─────────────────────┘ │
├─────────────────────────────────────────────────┤
│              Orchestrator (编排层)                │  采集→清洗 两阶段编排
├─────────────────────────────────────────────────┤
│               Client (外部调用)                   │  AKTools / Tushare
├─────────────────────────────────────────────────┤
│                 Mapper (DAO)                     │  MyBatis-Plus 数据访问
├─────────────────────────────────────────────────┤
│                  MySQL 8.0                       │  数据持久化
└─────────────────────────────────────────────────┘
```

---

## 4. 后端模块详解

### 4.1 应用入口与配置

#### [Application.java](file:///Users/lance/code/trading-diary/src/main/java/com/tradingdiary/Application.java)

Spring Boot 启动类，启用 `@EnableScheduling`（定时任务）。

#### 配置类

| 类 | 职责 |
|---|------|
| [SecurityConfig](file:///Users/lance/code/trading-diary/src/main/java/com/tradingdiary/config/SecurityConfig.java) | Spring Security 配置：无状态会话、JWT 过滤器链、CORS、路由权限 |
| [JwtConfig](file:///Users/lance/code/trading-diary/src/main/java/com/tradingdiary/config/JwtConfig.java) | JWT 参数配置（secret、access/refresh 过期时间），从 `app.jwt.*` 读取 |
| [AsyncConfig](file:///Users/lance/code/trading-diary/src/main/java/com/tradingdiary/config/AsyncConfig.java) | 异步任务线程池配置 |
| [MybatisPlusConfig](file:///Users/lance/code/trading-diary/src/main/java/com/tradingdiary/config/MybatisPlusConfig.java) | MyBatis-Plus 分页插件、自动填充 createdAt/updatedAt |
| [DotenvConfig](file:///Users/lance/code/trading-diary/src/main/java/com/tradingdiary/config/DotenvConfig.java) | `.env` 文件加载器，在 Spring 上下文初始化前以最高优先级注入环境变量 |
| [AdminInitializer](file:///Users/lance/code/trading-diary/src/main/java/com/tradingdiary/config/AdminInitializer.java) | 应用启动时检测 admin 用户占位密码并替换为 BCrypt 加密值 |

#### 配置文件

| 文件 | 用途 |
|------|------|
| `application.yml` | 主配置：数据源、MyBatis-Plus、Flyway、Jackson |
| `application-dev.yml` | 开发环境：JWT secret、Swagger 开启、自动登录 |
| `application-prod.yml` | 生产环境 |
| `application-test.yml` | 测试环境 |
| `.env` / `.env.example` | 环境变量：数据库密码、JWT secret、管理员密码 |

**关键约束**：后端必须 `SPRING_PROFILES_ACTIVE=dev` 启动，否则 JWT 配置为空导致登录 NPE。

---

### 4.2 认证与安全模块

#### 组件关系

```
请求 → AutoLoginFilter (dev only) → JwtAuthFilter → UsernamePasswordAuthenticationFilter → Controller
```

| 类 | 职责 |
|---|------|
| [JwtTokenProvider](file:///Users/lance/code/trading-diary/src/main/java/com/tradingdiary/security/JwtTokenProvider.java) | JWT 令牌签发、验证、解析。签发 access token（15min）和 refresh token（7天） |
| [JwtAuthFilter](file:///Users/lance/code/trading-diary/src/main/java/com/tradingdiary/security/JwtAuthFilter.java) | 从 Authorization header 提取 JWT，验证并设置 SecurityContext |
| [AutoLoginFilter](file:///Users/lance/code/trading-diary/src/main/java/com/tradingdiary/security/AutoLoginFilter.java) | 开发模式自动登录过滤器，`app.auto-login.enabled=true` 时生效 |
| [UserDetailsServiceImpl](file:///Users/lance/code/trading-diary/src/main/java/com/tradingdiary/security/UserDetailsServiceImpl.java) | Spring Security UserDetailsService 实现，从数据库加载用户 |
| [SecurityExceptionHandler](file:///Users/lance/code/trading-diary/src/main/java/com/tradingdiary/security/SecurityExceptionHandler.java) | 安全相关异常处理（401/403） |

#### 认证流程

1. 用户提交 `POST /api/v1/auth/login` → `AuthController.login()`
2. `AuthService.login()` 验证用户名密码，签发 JWT access + refresh token
3. 前端存储 token 到 localStorage，后续请求通过 `Authorization: Bearer <token>` 携带
4. access token 过期时，前端自动调用 `POST /api/v1/auth/refresh` 刷新
5. 开发模式下 `AutoLoginFilter` 自动注入 admin 认证信息

---

### 4.3 数据采集模块 (collection)

这是项目的核心模块，负责从外部数据源采集 A 股市场数据并清洗入库。

#### 模块结构

```
collection/
├── client/
│   ├── AKToolsClient.java        # AKTools HTTP 客户端（主数据源）
│   └── TushareClient.java        # Tushare Pro HTTP 客户端（日线补采）
├── controller/
│   ├── CollectionController.java # 采集管理 API
│   ├── StockDataController.java  # 股票数据查询 API
│   ├── MarginStatsController.java# 两融统计 API
│   └── MarketDataController.java # 概念/行业聚合 API
├── model/
│   ├── BackfillRequest.java      # 补采请求参数
│   ├── CalendarDayVO.java        # 日历日视图
│   ├── CollectionStatusVO.java   # 采集状态视图
│   ├── ConceptIndustryVO.java    # 概念/行业视图
│   ├── GapReportVO.java          # 数据缺口报告
│   ├── MarginSummaryVO.java      # 两融汇总视图
│   ├── StockDetailVO.java        # 股票详情视图
│   └── StockListVO.java          # 股票列表视图
├── orchestrator/
│   └── CollectionOrchestrator.java # 采集编排器（核心）
├── scheduler/
│   └── CollectionScheduler.java  # 定时调度器（已暂停）
└── CollectionConstants.java      # 采集模块常量
```

#### [CollectionOrchestrator](file:///Users/lance/code/trading-diary/src/main/java/com/tradingdiary/collection/orchestrator/CollectionOrchestrator.java) — 核心编排器

采集管线的核心，协调 FETCH（采集）和 CLEANSE（清洗）两阶段流程。

**关键方法**：

| 方法 | 说明 |
|------|------|
| `orchestrate(dataType, tradeDate)` | 编排执行采集+清洗流程，支持数据复用和并发锁 |
| `backfillMarginByWeek(request)` | 按周补采两融数据，自动跳过完整周 |
| `backfillStockDaily(start, end)` | 通过 Tushare 补采股票日线历史数据 |

**编排流程**：

```
orchestrate(dataType, tradeDate)
  │
  ├─ 1. 获取锁（ReentrantLock，按 dataType+tradeDate 粒度）
  │
  ├─ 2. STOCK_DAILY 特殊处理：复用 STOCK_INFO 的 FETCH 数据
  │
  ├─ 3. 检查已有 FETCH 日志，有则复用 raw_data 跳过重复采集
  │
  ├─ 4. executeFetch()
  │     ├─ 创建 FETCH 日志（RUNNING）
  │     ├─ fetchWithRetry() → 最多 3 次重试，指数退避
  │     ├─ 保存原始数据到 raw_data 表
  │     └─ 更新日志（SUCCESS/FAILED）
  │
  ├─ 5. executeCleanse()
  │     ├─ 创建 CLEANSE 日志（RUNNING）
  │     ├─ dispatchCleanse() → 按 dataType 分发到对应 CleanseService
  │     └─ 更新日志（SUCCESS/FAILED）
  │
  └─ 6. STOCK_INFO 完成后联动执行 STOCK_DAILY CLEANSE
```

**数据类型与分发**：

| dataType | FETCH 来源 | CLEANSE 服务 |
|----------|-----------|-------------|
| `TRADE_CALENDAR` | AKTools `tool_trade_date_hist_sina` | `TradeCalendarService.syncTradeCalendar()` |
| `STOCK_INFO` | AKTools `stock_zh_a_spot` | `StockInfoCleanseService.cleanse()` |
| `STOCK_DAILY` | 复用 STOCK_INFO FETCH | `StockDailyCleanseService.cleanse()` |
| `INDUSTRY_NAME` | AKTools `stock_board_industry_name_ths` | `IndustryCleanseService.cleanseNames()` |
| `CONCEPT_NAME` | AKTools `stock_board_concept_name_ths` | `ConceptCleanseService.cleanseNames()` |
| `INDUSTRY_CONS` | 空操作（Playwright 抓取） | 变化检测（日志记录） |
| `CONCEPT_CONS` | 空操作（Playwright 抓取） | 变化检测（日志记录） |
| `MARGIN_DAILY_SSE` | AKTools `stock_margin_detail_sse` | `MarginCleanseService.cleanse("SSE")` |
| `MARGIN_DAILY_SZSE` | AKTools `stock_margin_detail_szse` | `MarginCleanseService.cleanse("SZSE")` |
| `MARGIN_MACRO_SSE` | AKTools `macro_china_market_margin_sh` | `MarginMacroCleanseService.cleanse("SSE")` |
| `MARGIN_MACRO_SZSE` | AKTools `macro_china_market_margin_sz` | `MarginMacroCleanseService.cleanse("SZSE")` |

#### [AKToolsClient](file:///Users/lance/code/trading-diary/src/main/java/com/tradingdiary/collection/client/AKToolsClient.java) — AKTools API 客户端

封装 AKTools HTTP 接口调用，内置限流（200ms 最小间隔）。

**关键方法**：

| 方法 | 数据源 | 说明 |
|------|--------|------|
| `fetchStockSpot()` | 新浪 `stock_zh_a_spot` | 全市场实时行情 |
| `fetchStockDaily(symbol, start, end)` | 腾讯 `stock_zh_a_hist_tx` | 个股历史日线 |
| `fetchIndustryNames()` | 同花顺 `stock_board_industry_name_ths` | 行业分类名称 |
| `fetchConceptNames()` | 同花顺 `stock_board_concept_name_ths` | 概念分类名称 |
| `fetchTradeCalendar()` | 新浪 `tool_trade_date_hist_sina` | 交易日历 |
| `fetchMarginDetailSse(date)` | 上交所 `stock_margin_detail_sse` | 沪市两融明细 |
| `fetchMarginDetailSzse(date)` | 深交所 `stock_margin_detail_szse` | 深市两融明细 |
| `fetchMacroMarginSh()` | 上交所 `macro_china_market_margin_sh` | 沪市两融总量 |
| `fetchMacroMarginSz()` | 深交所 `macro_china_market_margin_sz` | 深市两融总量 |

> `fetchIndustryCons` / `fetchConceptCons` 系列方法已标记 `@Deprecated`，成分股数据改用 Playwright 抓取。

#### [TushareClient](file:///Users/lance/code/trading-diary/src/main/java/com/tradingdiary/collection/client/TushareClient.java) — Tushare Pro 客户端

| 方法 | 说明 |
|------|------|
| `fetchDaily(tradeDate)` | 按交易日拉取全市场 ~5500 只股票的 OHLCV 数据 |

#### [CollectionScheduler](file:///Users/lance/code/trading-diary/src/main/java/com/tradingdiary/collection/scheduler/CollectionScheduler.java) — 定时调度器

所有 `@Scheduled` 注解已注释，当前使用手动触发。包含以下定时任务：

| 任务 | 原定时间 | 说明 |
|------|---------|------|
| `collectStockData()` | 工作日 16:00 | 采集股票行情+日线 |
| `collectClassificationData()` | 工作日 17:00 | 采集行业/概念分类 |
| `collectMarginData()` | 工作日 18:00 | 采集两融数据 |
| `archiveOldRawData()` | 每月 1 日 03:00 | 归档 30 天前的 raw_data 为 GZIP 文件 |

#### 采集 Cleanse 服务

| 服务接口 | 实现 | 职责 |
|---------|------|------|
| `StockInfoCleanseService` | `StockInfoCleanseServiceImpl` | 清洗股票基本信息入库 `stock_info` |
| `StockDailyCleanseService` | `StockDailyCleanseServiceImpl` | 清洗日线 OHLCV 入库 `stock_daily`，支持 AKTools 和 Tushare 两种数据格式 |
| `IndustryCleanseService` | `IndustryCleanseServiceImpl` | 清洗行业分类名称入库 `industry` |
| `ConceptCleanseService` | `ConceptCleanseServiceImpl` | 清洗概念分类名称入库 `concept` |
| `MarginCleanseService` | `MarginCleanseServiceImpl` | 清洗个股两融明细入库 `margin_daily`，计算环比变化 |
| `MarginMacroCleanseService` | `MarginMacroCleanseServiceImpl` | 清洗全市场两融总量入库 `margin_macro` |
| `TradeCalendarService` | `TradeCalendarServiceImpl` | 同步交易日历到 `trade_calendar` |
| `ConstituentImportService` | `ConstituentImportServiceImpl` | 从 Playwright 抓取的 JSON 文件导入成分股到 `stock_industry` / `stock_concept` |
| `CollectionQueryService` | `CollectionQueryServiceImpl` | 查询采集状态、日志、交易日历 |
| `MarginStatsService` | `MarginStatsServiceImpl` | 两融数据汇总统计 |

---

### 4.4 业务服务层

| 接口 | 实现 | 职责 |
|------|------|------|
| [AuthService](file:///Users/lance/code/trading-diary/src/main/java/com/tradingdiary/service/AuthService.java) | `AuthServiceImpl` | 用户登录、令牌刷新、登出、用户信息查询 |
| [StockDataService](file:///Users/lance/code/trading-diary/src/main/java/com/tradingdiary/service/StockDataService.java) | `StockDataServiceImpl` | 股票列表分页查询、个股详情（日线+两融+行业概念） |
| [MarketDataService](file:///Users/lance/code/trading-diary/src/main/java/com/tradingdiary/service/MarketDataService.java) | `MarketDataServiceImpl` | 概念/行业板块列表查询（含两融聚合） |
| [CalendarService](file:///Users/lance/code/trading-diary/src/main/java/com/tradingdiary/service/CalendarService.java) | `CalendarServiceImpl` | 月度交易日历与数据采集状态 |
| [GapDetectionService](file:///Users/lance/code/trading-diary/src/main/java/com/tradingdiary/service/GapDetectionService.java) | `GapDetectionServiceImpl` | 两融数据缺口检测，按周分组报告 |

---

### 4.5 数据访问层 (Mapper)

所有 Mapper 继承 MyBatis-Plus `BaseMapper`，提供标准 CRUD。部分复杂查询使用 XML Mapper。

| Mapper | 对应表 | XML Mapper | 说明 |
|--------|--------|-----------|------|
| `StockInfoMapper` | `stock_info` | ✅ | 自定义分页+关联查询 |
| `StockDailyMapper` | `stock_daily` | - | - |
| `MarginDailyMapper` | `margin_daily` | ✅ | 自定义聚合查询 |
| `MarginMacroMapper` | `margin_macro` | ✅ | 自定义汇总查询 |
| `TradeCalendarMapper` | `trade_calendar` | ✅ | `selectTradingDays()` 按范围查交易日 |
| `DataCollectionLogMapper` | `data_collection_log` | ✅ | `selectLatestByDataTypeAndJobTypeAndTradeDate()` |
| `ConceptMapper` | `concept` | ✅ | - |
| `IndustryMapper` | `industry` | ✅ | - |
| `SysUserMapper` | `sys_user` | ✅ | `selectByUsername()` |
| `SysRoleMapper` | `sys_role` | - | - |
| `SysUserRoleMapper` | `sys_user_role` | - | - |
| `SysPermissionMapper` | `sys_permission` | - | - |
| `SysRefreshTokenMapper` | `sys_refresh_token` | - | - |
| `RawDataMapper` | `raw_data` | - | - |
| `StockConceptMapper` | `stock_concept` | - | - |
| `StockIndustryMapper` | `stock_industry` | - | - |
| `IndexDailyMapper` | `index_daily` | - | - |
| `ClassificationChangeLogMapper` | `classification_change_log` | - | - |

---

### 4.6 实体层 (Entity)

所有实体使用 Lombok `@Getter/@Setter`，MyBatis-Plus `@TableName` 注解。

#### 业务数据实体

| 实体 | 表名 | 核心字段 | 说明 |
|------|------|---------|------|
| `StockInfo` | `stock_info` | code, name, latestPrice, changePct, pe, pb, totalMv, snapshotDate | 股票行情快照 |
| `StockDaily` | `stock_daily` | stockCode, tradeDate, open, high, low, close, volume | 日线 OHLCV |
| `MarginDaily` | `margin_daily` | stockCode, tradeDate, exchange, marginBalance, shortBalance, totalBalance, marginChange | 个股两融明细 |
| `MarginMacro` | `margin_macro` | tradeDate, exchange, marginBuy, marginBalance, shortBalance, totalBalance | 全市场两融总量 |
| `TradeCalendar` | `trade_calendar` | tradeDate, isTradingDay | 交易日历 |
| `Industry` | `industry` | code, name | 行业分类 |
| `Concept` | `concept` | code, name | 概念分类 |
| `StockIndustry` | `stock_industry` | stockCode, industryCode, snapDate | 股票-行业关联 |
| `StockConcept` | `stock_concept` | stockCode, conceptCode, snapDate | 股票-概念关联 |
| `ClassificationChangeLog` | `classification_change_log` | stockCode, classificationType, sectorCode, action | 分类变更日志 |
| `RawData` | `raw_data` | collectionLogId, dataType, tradeDate, source, rawJson | 原始采集数据 |
| `DataCollectionLog` | `data_collection_log` | dataType, jobType, status, tradeDate, recordCount, errorMsg | 采集日志 |
| `IndexDaily` | `index_daily` | - | 指数日线（预留） |

#### 系统用户实体

| 实体 | 表名 | 说明 |
|------|------|------|
| `SysUser` | `sys_user` | 系统用户 |
| `SysRole` | `sys_role` | 角色（ADMIN/USER） |
| `SysUserRole` | `sys_user_role` | 用户-角色关联 |
| `SysPermission` | `sys_permission` | 权限定义 |
| `SysRefreshToken` | `sys_refresh_token` | 刷新令牌（SHA-256 哈希存储） |

**通用字段约定**：
- `id`：BIGINT AUTO_INCREMENT 主键
- `createdAt`：`@TableField(fill = FieldFill.INSERT)` 自动填充
- `updatedAt`：`@TableField(fill = FieldFill.INSERT_UPDATE)` 自动填充
- `isDeleted`：逻辑删除标记（`margin_macro` 表例外，无此字段）

---

### 4.7 视图对象 (VO / Model)

| 类 | 用途 |
|----|------|
| [ApiResponse\<T\>](file:///Users/lance/code/trading-diary/src/main/java/com/tradingdiary/model/ApiResponse.java) | 统一 API 响应封装：`{code, message, data, timestamp}` |
| `LoginRequest` | 登录请求：username + password |
| `TokenVO` | 令牌响应：accessToken + refreshToken + expiresIn |
| `UserInfoVO` | 用户信息：id, username, nickname, roles |
| `CollectionStatusVO` | 采集状态：dataType, lastJobType, lastStatus, lastTradeDate, recordCount |
| `GapReportVO` | 缺口报告：按周分组，含完整度统计和缺失日期 |
| `MarginSummaryVO` | 两融汇总：融资余额/融券余额/两融总额/标的数量 |
| `StockDetailVO` | 股票详情：基本信息 + 日线列表 + 两融数据 + 行业概念 |
| `StockListVO` | 股票列表项 |
| `CalendarDayVO` | 日历日：date, isTradingDay, hasData |
| `ConceptIndustryVO` | 概念/行业聚合视图 |
| `BackfillRequest` | 补采请求：dataType, startDate, endDate, exchange |

---

### 4.8 异常处理

```
BaseException (抽象基类, code + message)
  ├── BadRequestException      # 400 — 请求参数错误
  ├── UnauthorizedException    # 401 — 未授权
  └── NotFoundException        # 404 — 资源不存在
```

[GlobalExceptionHandler](file:///Users/lance/code/trading-diary/src/main/java/com/tradingdiary/exception/GlobalExceptionHandler.java) 通过 `@RestControllerAdvice` 统一捕获：
- `BaseException` → 根据 subtype 映射 HTTP 状态码
- `MethodArgumentNotValidException` → 400 + 字段错误信息
- `Exception` → 500 + 通用错误消息

---

### 4.9 工具类

#### [BatchSqlRunner](file:///Users/lance/code/trading-diary/src/main/java/com/tradingdiary/util/BatchSqlRunner.java)

基于 `JdbcTemplate` 的批量 SQL 执行器，通过反射自动生成 INSERT/UPDATE SQL。

**关键方法**：

| 方法 | 说明 |
|------|------|
| `batchInsert(entities, batchSize)` | 批量插入，默认每批 500 条 |
| `batchUpdate(entities, batchSize)` | 批量更新，使用 CASE-WHEN 语法 |

**设计要点**：
- 通过 `@TableName` 和 `@TableId` 注解自动推断表名和主键
- 自动填充 `createdAt` / `updatedAt`
- `EntityMeta` 缓存避免重复反射
- `camelToSnake()` 自动转换字段名为下划线命名

#### [CollectionConstants](file:///Users/lance/code/trading-diary/src/main/java/com/tradingdiary/collection/CollectionConstants.java)

| 常量 | 值 | 说明 |
|------|---|------|
| `DB_BATCH_SIZE` | 500 | 批量写入每批记录数 |
| `BACKFILL_ACCUMULATE_SIZE` | 50 | 补采时累计多少只股票后执行批量写入 |

---

## 5. 前端模块详解

### 5.1 技术选型

- **Next.js 14 App Router**：SSR + CSR 混合渲染
- **Zustand**：轻量状态管理（认证状态）
- **ky**：基于 fetch 的 HTTP 客户端，内置请求/响应拦截
- **shadcn/ui**：Radix UI + TailwindCSS 组件库
- **lightweight-charts**：TradingView K 线图库
- **TanStack React Query**：服务端状态管理

### 5.2 目录结构

```
frontend/src/
├── app/
│   ├── (dashboard)/              # 需认证的仪表盘布局组
│   │   ├── admin/
│   │   │   ├── collection/       #   数据采集 Hub + 详情
│   │   │   ├── concepts/         #   概念列表
│   │   │   ├── industries/       #   行业列表
│   │   │   ├── margin-stats/     #   融资统计
│   │   │   └── stocks/           #   股票数据 + 详情
│   │   ├── dashboard/            #   控制台首页
│   │   └── layout.tsx            #   仪表盘布局（侧边栏+顶栏）
│   ├── login/                    # 登录页
│   ├── globals.css               # 全局样式
│   ├── layout.tsx                # 根布局
│   ├── page.tsx                  # 首页（重定向）
│   └── providers.tsx             # Provider 组合
├── components/
│   ├── layout/
│   │   ├── AuthGuard.tsx         #   认证守卫组件
│   │   └── AuthGuard.test.tsx
│   ├── providers/
│   │   └── AuthProvider.tsx      #   认证状态初始化
│   └── ui/                       #   shadcn/ui 基础组件
│       ├── avatar.tsx
│       ├── button.tsx
│       ├── card.tsx
│       ├── dropdown-menu.tsx
│       ├── input.tsx
│       └── toast.tsx
├── hooks/
│   ├── useAuth.ts                #   认证状态 Hook (Zustand)
│   ├── useAuth.test.ts
│   └── use-toast.ts              #   Toast 通知 Hook
└── lib/
    ├── api.ts                    #   ky HTTP 客户端（含 JWT 自动刷新）
    ├── auth.ts                   #   认证 API 函数
    └── utils.ts                  #   工具函数
```

### 5.3 路由与页面

| 路径 | 页面 | 说明 |
|------|------|------|
| `/login` | 登录页 | 用户名密码登录 |
| `/dashboard` | 控制台 | 首页概览 |
| `/admin/collection` | 数据采集 Hub | 8 张采集类型卡片 + 成分股管理入口 |
| `/admin/collection/[dataType]` | 采集详情 | 管线状态 + 触发采集 + 交易日历 + 日志表 |
| `/admin/collection/constituents` | 成分股管理 | 文件列表 + 导入操作 |
| `/admin/collection/margin` | 两融完整性 | 两融数据缺口检测 |
| `/admin/stocks` | 股票数据 | 筛选/排序/分页浏览 |
| `/admin/stocks/[code]` | 股票详情 | K 线图 + 两融叠加 + 日线明细 |
| `/admin/concepts` | 概念列表 | 概念维度两融聚合 |
| `/admin/industries` | 行业列表 | 行业维度两融聚合 |
| `/admin/margin-stats` | 融资统计 | 全市场两融总量 |

### 5.4 状态管理与认证

#### [useAuth](file:///Users/lance/code/trading-diary/frontend/src/hooks/useAuth.ts) — Zustand Store

管理认证状态的核心 Hook：

| 状态 | 说明 |
|------|------|
| `accessToken` / `refreshToken` | JWT 令牌，持久化到 localStorage |
| `user` | 当前用户信息（UserInfoVO） |
| `isLoading` | 初始化加载状态 |
| `isDev` | 开发模式标记（`NEXT_PUBLIC_DEV_AUTO_LOGIN=true`） |

| 方法 | 说明 |
|------|------|
| `login(username, password)` | 登录并存储令牌 |
| `logout()` | 登出并清除令牌 |
| `refreshAuth()` | 刷新令牌 |
| `fetchUser()` | 获取当前用户信息 |

#### [AuthProvider](file:///Users/lance/code/trading-diary/frontend/src/components/providers/AuthProvider.tsx)

应用启动时初始化认证状态：
- 开发模式：直接调用 `fetchUser()`（后端 AutoLoginFilter 处理认证）
- 有令牌：通过 `fetchUser()` 验证令牌有效性
- 无令牌：跳过，等待用户登录

#### [AuthGuard](file:///Users/lance/code/trading-diary/frontend/src/components/layout/AuthGuard.tsx)

路由守卫组件，包裹 `(dashboard)` 布局组：
- 未认证且非开发模式 → 重定向到 `/login`
- 加载中 → 显示 spinner
- 已认证或开发模式 → 渲染子组件

### 5.5 API 通信层

#### [api.ts](file:///Users/lance/code/trading-diary/frontend/src/lib/api.ts) — ky HTTP 客户端

- `prefixUrl`：`NEXT_PUBLIC_API_BASE_URL` 或 `http://localhost:8080`
- `beforeRequest` hook：自动注入 `Authorization: Bearer <token>`
- `afterResponse` hook：401 时自动刷新令牌并重试原始请求
- 刷新失败时清除令牌并重定向到登录页

---

## 6. 数据库设计

### Flyway 迁移脚本

| 版本 | 文件 | 说明 |
|------|------|------|
| V1 | `V1__init_schema.sql` | 系统用户/角色/权限/刷新令牌表 |
| V2 | `V2__seed_admin.sql` | 初始化 admin 用户（密码占位符） |
| V3 | `V3__collection_schema.sql` | 采集日志/原始数据/交易日历/股票/行业/概念/两融表 |
| V4 | `V4__margin_macro.sql` | 两融总量表（margin_macro） |
| V5 | `V5__ui_redesign_schema.sql` | UI 重构相关表变更 |
| V6 | `V6__drop_margin_stock.sql` | 删除 margin_stock 表 |
| V7 | `V7__raw_data_unique.sql` | raw_data 唯一索引 |

### ER 关系图（核心表）

```
sys_user ──1:N──> sys_user_role ──N:1──> sys_role
                                                    │
                                              sys_role_permission ──N:1──> sys_permission

trade_calendar (独立)

data_collection_log ──1:1──> raw_data

stock_info ──1:N──> stock_daily (by code + date)
    │
    ├──N:N──> industry (via stock_industry)
    └──N:N──> concept  (via stock_concept)

margin_daily (个股两融, by stock_code + trade_date + exchange)
margin_macro (全市场两融, by trade_date + exchange)

classification_change_log (分类变更审计)
```

### 关键索引

| 表 | 索引 | 用途 |
|----|------|------|
| `stock_info` | `uk_code_date (code, snapshot_date)` | 防止重复快照 |
| `stock_daily` | `uk_code_date (stock_code, trade_date)` | 防止重复日线 |
| `margin_daily` | `uk_stock_date_exchange (stock_code, trade_date, exchange)` | 防止重复两融 |
| `trade_calendar` | `uk_trade_date (trade_date)` | 唯一交易日 |
| `data_collection_log` | `idx_log_type_date (data_type, trade_date)` | 采集日志查询 |

---

## 7. 模块间依赖关系

### 后端依赖图

```
Controller 层
  │
  ├── AuthController ──────> AuthService
  ├── CollectionController ─> CollectionQueryService + GapDetectionService + CollectionOrchestrator + ConstituentImportService
  ├── StockDataController ──> StockDataService + CalendarService
  ├── MarginStatsController > MarginStatsService
  └── MarketDataController ─> MarketDataService

Service 层
  │
  ├── CollectionOrchestrator ──> AKToolsClient + TushareClient + 所有 *CleanseService + 各 Mapper
  ├── AuthServiceImpl ─────────> SysUserMapper + SysRoleMapper + SysRefreshTokenMapper + JwtTokenProvider
  ├── StockDataServiceImpl ────> StockInfoMapper + StockDailyMapper + MarginDailyMapper
  ├── MarketDataServiceImpl ───> ConceptMapper + IndustryMapper + MarginDailyMapper
  ├── CalendarServiceImpl ─────> TradeCalendarMapper + DataCollectionLogMapper
  ├── GapDetectionServiceImpl ─> TradeCalendarMapper + DataCollectionLogMapper
  ├── *CleanseServiceImpl ─────> 对应 Mapper + BatchSqlRunner
  └── ConstituentImportServiceImpl > StockIndustryMapper + StockConceptMapper

基础设施
  │
  ├── SecurityConfig ──> JwtAuthFilter + AutoLoginFilter
  ├── JwtAuthFilter ───> JwtTokenProvider + UserDetailsServiceImpl
  ├── AdminInitializer > SysUserMapper + PasswordEncoder
  └── DotenvConfig ────> (独立，最早加载)
```

### 前端依赖图

```
页面组件
  │
  ├── (dashboard)/layout.tsx ──> AuthGuard + useAuth
  ├── login/page.tsx ──────────> useAuth
  ├── admin/collection/* ──────> api (ky)
  ├── admin/stocks/* ──────────> api (ky)
  └── ...

状态与认证
  │
  ├── AuthProvider ──> useAuth (Zustand)
  ├── AuthGuard ────> useAuth
  └── useAuth ──────> auth.ts (API 函数) + localStorage

API 通信
  │
  └── api.ts (ky) ──> localStorage (token) + /auth/refresh
```

---

## 8. API 接口一览

### 认证接口（无需认证）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/auth/login` | 用户登录 |
| POST | `/api/v1/auth/refresh` | 刷新令牌 |

### 认证接口（需认证）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/auth/logout` | 用户登出 |
| GET | `/api/v1/auth/me` | 获取当前用户信息 |

### 采集管理接口（需 ADMIN 角色）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/admin/collection/status` | 获取所有采集类型状态 |
| GET | `/api/v1/admin/collection/logs` | 获取采集日志（?dataType=&limit=） |
| GET | `/api/v1/admin/collection/gaps` | 检测数据缺口（?start=&end=&dataType=） |
| POST | `/api/v1/admin/collection/trigger/{dataType}` | 触发采集任务 |
| POST | `/api/v1/admin/collection/backfill` | 执行历史补采 |
| GET | `/api/v1/admin/collection/constituents/files` | 成分股文件列表 |
| POST | `/api/v1/admin/collection/constituents/import` | 导入成分股文件 |

### 股票数据接口（需 ADMIN 角色）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/admin/stocks/list` | 股票列表（含行情+两融） |
| GET | `/api/v1/admin/stocks/{code}` | 股票详情 |
| GET | `/api/v1/admin/stocks/calendar` | 交易日历 |

### 市场数据接口（需 ADMIN 角色）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/admin/market/concepts` | 概念列表（含两融聚合） |
| GET | `/api/v1/admin/market/industries` | 行业列表（含两融聚合） |
| GET | `/api/v1/admin/margin-stats/summary` | 两融统计总量 |

### 系统接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/actuator/health` | 健康检查（无需认证） |

---

## 9. 数据采集流程

### 首次启动完整流程

```
(1) 交易日历  ──→  (2) 股票行情（自动联动日线）  ──→  (3) 行业 + 概念名称
                                                       ↓
                                                (4) 行业/概念成分股（Playwright → 导入）
                                                       ↓
                                                (5) 两融明细 + 两融总量（沪/深各 2 个）
```

### 采集管线两阶段

```
FETCH 阶段                          CLEANSE 阶段
┌──────────────────┐               ┌──────────────────┐
│ 1. 创建 FETCH 日志 │               │ 1. 创建 CLEANSE 日志│
│ 2. 调用外部 API    │               │ 2. 读取 raw_data   │
│ 3. 保存 raw_data   │               │ 3. 解析+转换+计算   │
│ 4. 更新日志状态     │               │ 4. 批量写入业务表    │
└──────────────────┘               │ 5. 更新日志状态      │
                                   └──────────────────┘
```

### 日常增量采集

| 频率 | 采集类型 | 方式 |
|------|---------|------|
| 每日 | 股票行情 | 管理后台手动触发 |
| 每日 | 两融明细/总量 | 管理后台手动触发 |
| 每月 | 行业/概念名称 | 管理后台手动触发 |
| 每月 | 成分股 | `python3 scripts/scrape_ths_constituents.py` → 后台导入 |

### 数据完整性监控

管理后台每个采集详情页带交易日历（月视图，三态标记）：
- 🟢 绿色 = 已采集
- 🔴 红色 = 交易日缺数据（可点击触发补采）
- ⬜ 灰色 = 非交易日

---

## 10. 项目运行方式

### 环境要求

- Java 17+
- Node.js 18+ / pnpm
- MySQL 8.0
- Python 3（AKTools + 成分股抓取）

### 启动步骤

```bash
# 1. 环境检查
scripts/check-env.sh

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env 设置数据库密码、JWT secret 等

# 3. 启动 AKTools（行情数据源）
pip3 install aktools
python3 -m aktools --host 127.0.0.1 --port 8081 &

# 4. 启动后端（必须激活 dev profile）
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun

# 5. 启动前端
cd frontend && pnpm install && pnpm dev
```

### 验证

```bash
curl http://localhost:8081/version          # AKTools 健康
curl http://localhost:8080/actuator/health  # 后端健康
curl http://localhost:3000                   # 前端健康
```

### 构建与测试

```bash
# 后端
./gradlew build           # 构建
./gradlew test            # 运行测试

# 前端
cd frontend
pnpm build                # 构建
pnpm test                 # 单元测试 (Vitest)
pnpm exec playwright test # E2E 测试
```

### 端口分配

| 服务 | 端口 |
|------|------|
| 后端 API | 8080 |
| AKTools | 8081 |
| 前端 Dev Server | 3000 |
| Swagger UI | 8080/swagger-ui.html (dev only) |

---

## 11. 测试体系

### 后端测试

位于 `src/test/java/com/tradingdiary/`，使用 JUnit 5 + Spring Boot Test + H2 内存数据库。

| 测试类 | 测试目标 |
|--------|---------|
| `AuthControllerTest` | 认证接口集成测试 |
| `CollectionControllerTest` | 采集接口测试 |
| `StockDataControllerTest` | 股票数据接口测试 |
| `MarketDataControllerTest` | 市场数据接口测试 |
| `AKToolsClientTest` | AKTools 客户端单元测试 |
| `CollectionOrchestratorTest` | 采集编排器测试 |
| `JwtTokenProviderTest` | JWT 令牌提供者测试 |
| `AutoLoginFilterTest` | 自动登录过滤器测试 |
| `AuthServiceTest` | 认证服务测试 |
| `GapDetectionServiceTest` | 缺口检测测试 |
| `ConceptCleanseServiceTest` | 概念清洗测试 |
| `IndustryCleanseServiceTest` | 行业清洗测试 |
| `BatchSqlRunnerTest` | 批量 SQL 执行器测试 |

### 前端测试

| 类型 | 工具 | 位置 |
|------|------|------|
| 单元测试 | Vitest + Testing Library | `src/**/*.test.ts(x)` |
| E2E 测试 | Playwright | `e2e/*.spec.ts` |

E2E 测试覆盖：
- `collection-hub.spec.ts` — 数据采集 Hub 页面
- `margin-stats.spec.ts` — 融资统计页面
- `stocks.spec.ts` — 股票数据页面

---

## 12. 工程规范与约定

### 分层规则

- **分层不可逆**：Controller → Service → Mapper，禁止跨层调用
- Controller 只做参数校验和响应封装，不含业务逻辑
- Service 接口与实现分离，面向接口编程

### 代码约定

- 实体使用 Lombok `@Getter/@Setter`，不使用 `@Data`
- 逻辑删除：`is_deleted` 字段（`margin_macro` 表例外）
- API 响应统一使用 `ApiResponse<T>` 封装
- 采集日志通过 `DataCollectionLog` 追踪每步状态
- 批量写入使用 `BatchSqlRunner`（JDBC），不走 MyBatis-Plus 逐条插入

### 数据库约定

- 表名：snake_case
- 字段名：snake_case
- Java 字段：camelCase（MyBatis-Plus 自动映射）
- 所有表都有 `created_at`，大部分有 `updated_at` 和 `is_deleted`
- 主键：BIGINT AUTO_INCREMENT
- Flyway 管理数据库版本迁移

### 安全约定

- JWT 无状态认证，access token 15 分钟，refresh token 7 天
- 密码 BCrypt 加密（强度 12）
- refresh token 存储 SHA-256 哈希
- `.env` 文件 git-ignored，使用 `.env.example` 作为模板
- 开发模式支持自动登录（`AutoLoginFilter`）

### 采集类型分级

- **日级**（需交易日历）：STOCK_INFO, STOCK_DAILY, MARGIN_DAILY_*, MARGIN_MACRO_*
- **月级快照**（无需日历）：INDUSTRY_NAME, CONCEPT_NAME, INDUSTRY_CONS, CONCEPT_CONS

### 工程原则

1. **约定优于配置** — 优先使用 Spring Boot 默认约定
2. **分层不可逆** — Controller → Service → Mapper
3. **防御式编程** — 所有外部输入验证
4. **渐进式优化** — 先正确，再优化
