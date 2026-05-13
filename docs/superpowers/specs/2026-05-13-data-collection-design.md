# 子项目 2：数据采集层 — 设计文档

**日期**: 2026-05-13 | **状态**: 待实施

## 背景

trading-diary 子项目 1（项目脚手架 + 用户认证）已完成。现进入子项目 2：从外部数据源全量采集股票基础信息、行业/概念板块分类、两融标的及每日两融交易数据，为后续股票查询和两融分析提供数据基础。

## 架构决策

| 决策 | 选择 | 依据 |
|------|------|------|
| 数据源 | 单一 Akshare（Tushare 124 积分不够，`stock_basic`/`margin_*` 全部需要 2000 积分） | 免费够用 |
| 集成方式 | AKTools 官方 Docker 镜像（`registry.cn-shanghai.aliyuncs.com/akfamily/aktools:latest`），Java HTTP 调用 | 官方方案，1000+ 接口全覆盖 |
| 采集模式 | FETCH（原始 JSON → `raw_data`）→ CLEANSE（解析 JSON → 写入业务表），两步各有状态日志 | 原始数据可追溯，失败可重新清洗 |
| 触发策略 | 定时自动 + 管理后台手动触发 + 按日期范围补采 | 全覆盖 |
| 失败处理 | 3 次重试（间隔 2s/4s/8s），管理后台红色标记，支持重采和补采 | |
| 历史回填 | 按周分片，`week_start`/`week_end` 记录到日志，基于交易日历做缺口检测 | 断点续传 |
| 行业/概念分类 | 统一用东方财富分类体系 | 同花顺概念成分股接口已失效 |
| 采集范围 | 全量 A 股（5000+），不限速只采两融标的 | 全量保证后续扩展性 |
| 股票信息 | 保留每日快照历史，唯一键 `(code, snapshot_date)` | 按行业/概念聚合需要当天 PE/市值做加权计算 |

## §1 数据库设计

### 采集日志表 `data_collection_log`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | PK 自增 |
| data_type | varchar(30) | `STOCK_INFO` / `STOCK_DAILY` / `TRADE_CALENDAR` / `INDUSTRY_NAME` / `INDUSTRY_CONS` / `CONCEPT_NAME` / `CONCEPT_CONS` / `MARGIN_DAILY_SSE` / `MARGIN_DAILY_SZSE` |
| job_type | varchar(10) | `FETCH`（原始采集）/ `CLEANSE`（清洗入库） |
| status | varchar(15) | `PENDING` → `RUNNING` → `SUCCESS` / `FAILED` |
| trade_date | date | 数据对应的交易日（两融数据需要） |
| week_start | date | 按周回填时，周的起始日期 |
| week_end | date | 按周回填时，周的结束日期 |
| record_count | int | 采集/清洗的记录数 |
| error_msg | text | 失败原因 |
| started_at | datetime | 开始时间 |
| completed_at | datetime | 完成时间 |
| created_at | datetime | 创建时间 |

日志表追加写入，无更新和软删除场景，不加 `updated_at` 和 `is_deleted`。

索引：`INDEX idx_log_type_date (data_type, trade_date)` — 状态查询。

### 原始数据表 `raw_data`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | PK 自增 |
| collection_log_id | bigint | FK → data_collection_log.id |
| data_type | varchar(30) | 同上枚举 |
| trade_date | date | 交易日 |
| source | varchar(20) | `AKSHARE` |
| raw_json | longtext | 原始 JSON |
| fetch_at | datetime | 采集时间 |
| created_at | datetime | 创建时间 |

索引：`INDEX idx_raw_log (collection_log_id)` — FK 追溯。

**清理策略**：原始数据清洗入库后价值降低。保留最近 30 天在 DB 中；超过 30 天的按月导出为 JSON 文件存储（如 `backups/raw_data/2026-04.json.gz`），然后物理删除 DB 记录。发生数据问题时可从文件恢复重洗。

### 交易日历表 `trade_calendar`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | PK 自增 |
| trade_date | date | 交易日 |
| is_trading_day | tinyint | 1=交易日 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间（增量同步时更新） |
| is_deleted | tinyint(1) | 软删除标记 |

唯一键 `(trade_date)`。

### 股票基础信息表 `stock_info`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | PK 自增 |
| code | varchar(10) | 股票代码 |
| name | varchar(20) | 股票名称 |
| latest_price | decimal(10,3) | 最新价 |
| change_pct | decimal(6,2) | 涨跌幅% |
| change_amount | decimal(8,3) | 涨跌额 |
| volume | bigint | 成交量（手） |
| amount | decimal(16,2) | 成交额 |
| turnover_rate | decimal(6,2) | 换手率% |
| volume_ratio | decimal(6,2) | 量比 |
| pe | decimal(10,3) | 市盈率（动态） |
| pb | decimal(10,3) | 市净率 |
| total_mv | decimal(14,2) | 总市值 |
| float_mv | decimal(14,2) | 流通市值 |
| snapshot_date | date | 快照日期 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |
| is_deleted | tinyint(1) | 软删除标记 |

唯一键 `(code, snapshot_date)`。索引：`INDEX idx_code (code)`，`INDEX idx_snapshot (snapshot_date)`。

### 股票日线表 `stock_daily`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | PK 自增 |
| stock_code | varchar(10) | 股票代码 |
| trade_date | date | 交易日 |
| open | decimal(10,3) | 开盘价 |
| high | decimal(10,3) | 最高价 |
| low | decimal(10,3) | 最低价 |
| close | decimal(10,3) | 收盘价（前复权 qfq） |
| volume | bigint | 成交量（手） |
| amount | decimal(16,2) | 成交额 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |
| is_deleted | tinyint(1) | 软删除标记 |

唯一键 `(stock_code, trade_date)`。索引：`INDEX idx_code_date (stock_code, trade_date)` — 个股历史查询。

### 行业板块表 `industry`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | PK 自增 |
| code | varchar(10) | 板块代码 `BK0740` |
| name | varchar(50) | 行业名称 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |
| is_deleted | tinyint(1) | 软删除标记 |

唯一键 `(code)`。

### 股票-行业关联表 `stock_industry`

只保留最新快照。每日 CLEANSE 与前一天对比，仅当有变化时才 INSERT 新行业或 DELETE 旧行业，并记录变更到 `classification_change_log`。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | PK 自增 |
| stock_code | varchar(10) | 股票代码 |
| industry_code | varchar(10) | 行业代码 |
| snap_date | date | 最近一次确认快照的日期 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |
| is_deleted | tinyint(1) | 软删除标记 |

唯一键 `(stock_code, industry_code)`（一票一行业，东方财富单行业分类）。索引：`INDEX idx_industry (industry_code)`。

### 概念板块表 `concept`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | PK 自增 |
| code | varchar(10) | 板块代码 |
| name | varchar(50) | 概念名称 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |
| is_deleted | tinyint(1) | 软删除标记 |

唯一键 `(code)`。

### 股票-概念关联表 `stock_concept`

同 `stock_industry`，只保留最新，变动时记录到 `classification_change_log`。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | PK 自增 |
| stock_code | varchar(10) | 股票代码 |
| concept_code | varchar(10) | 概念代码 |
| snap_date | date | 最近一次确认快照的日期 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |
| is_deleted | tinyint(1) | 软删除标记 |

唯一键 `(stock_code, concept_code)`（一票可多概念）。索引：`INDEX idx_concept (concept_code)`。

### 分类变更日志表 `classification_change_log`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | PK 自增 |
| stock_code | varchar(10) | 股票代码 |
| classification_type | varchar(10) | `INDUSTRY` / `CONCEPT` |
| sector_code | varchar(10) | 板块代码 |
| action | varchar(10) | `ADD`（新归入）/ `REMOVE`（移出） |
| snap_date | date | 变更发现日期 |
| created_at | datetime | 记录创建时间 |

索引：`INDEX idx_stock_type_date (stock_code, classification_type, snap_date)` — 查询历史，确定某日某股的行业/概念归属。

### 两融标的信息表 `margin_stock`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | PK 自增 |
| stock_code | varchar(10) | 标的代码 |
| stock_name | varchar(20) | 标的名称 |
| exchange | varchar(4) | `SSE`（上交所）/ `SZSE`（深交所） |
| is_margin | tinyint | 是否融资标的 |
| is_short | tinyint | 是否融券标的 |
| snap_date | date | 快照日期 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |
| is_deleted | tinyint(1) | 软删除标记 |

唯一键 `(stock_code, snap_date)`。数据来源：从 `stock_margin_detail_sse/szse` CLEANSE 时提取出现过的所有 `stock_code`，同时建立两融标的列表。首次回填后，后续增量对比新增标的。

索引：`INDEX idx_exchange (exchange)`。

### 两融交易明细表 `margin_daily`（核心业务表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | PK 自增 |
| stock_code | varchar(10) | 标的代码 |
| trade_date | date | 交易日 |
| exchange | varchar(4) | `SSE` / `SZSE` |
| margin_balance | decimal(16,2) | 融资余额 |
| margin_buy | decimal(16,2) | 融资买入额 |
| margin_repay | decimal(16,2) | 融资偿还额 |
| short_balance | decimal(16,2) | 融券余额 |
| short_sell_vol | bigint | 融券卖出量 |
| short_repay_vol | bigint | 融券偿还量 |
| short_remain_vol | bigint | 融券余量 |
| total_balance | decimal(16,2) | 融资融券余额 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |
| is_deleted | tinyint(1) | 软删除标记 |

唯一键 `(stock_code, trade_date, exchange)`。

索引：
- `INDEX idx_trade_date_exchange (trade_date, exchange)` — 缺口检测
- `INDEX idx_stock_date (stock_code, trade_date)` — 个股两融历史查询

### Flyway 迁移

```text
V3__collection_schema.sql    # 以上 12 张表
```

---

## §2 后端设计

### 包结构（新增）

```
com.tradingdiary/
├── collection/                              # 新增包
│   ├── client/
│   │   └── AKToolsClient.java              # HTTP 客户端，统一封装对 AKTools Docker 的调用
│   ├── controller/
│   │   └── CollectionController.java       # 管理后台 REST API
│   ├── orchestrator/
│   │   └── CollectionOrchestrator.java      # 编排 FETCH → CLEANSE 流程
│   ├── scheduler/
│   │   └── CollectionScheduler.java         # @Scheduled 定时任务
│   └── model/
│       ├── CollectionStatusVO.java          # 采集状态 VO
│       └── GapReportVO.java                 # 缺口报告 VO
├── entity/   新增: DataCollectionLog, RawData, TradeCalendar, StockInfo,
│                StockDaily, Industry, StockIndustry, Concept,
│                StockConcept, ClassificationChangeLog, MarginStock, MarginDaily
├── mapper/   对应 Mapper 接口
└── service/
    ├── collection/                          # 各数据类的清洗逻辑
    │   ├── StockInfoCleanseService.java
    │   ├── StockDailyCleanseService.java
    │   ├── IndustryCleanseService.java
    │   ├── ConceptCleanseService.java
    │   ├── MarginCleanseService.java
    │   └── TradeCalendarService.java
    └── GapDetectionService.java
```

### 核心组件

#### AKToolsClient

统一封装对 AKTools Docker 的 HTTP 调用（建议 Spring RestClient）。所有请求走同一个 base URL（`http://aktools:8080`），返回 JSON 字符串写入 `raw_data`。

端点映射：

| 方法 | AKTools HTTP 端点 | 返回 |
|------|-------------------|------|
| `fetchStockSpot()` | `GET /api/public/stock_zh_a_spot_em` | 全 A 股实时行情快照 |
| `fetchStockDaily(symbol, start, end)` | `GET /api/public/stock_zh_a_hist` | 单只股 OHLCV |
| `fetchIndustryNames()` | `GET /api/public/stock_board_industry_name_em` | 行业板块列表 |
| `fetchIndustryCons(symbol)` | `GET /api/public/stock_board_industry_cons_em` | 某行业成分股 |
| `fetchConceptNames()` | `GET /api/public/stock_board_concept_name_em` | 概念板块列表 |
| `fetchConceptCons(symbol)` | `GET /api/public/stock_board_concept_cons_em` | 某概念成分股 |
| `fetchTradeCalendar()` | `GET /api/public/tool_trade_date_hist_sina` | 交易日历全量 |
| `fetchMarginDetailSse(date)` | `GET /api/public/stock_margin_detail_sse` | 上交所个股两融明细 |
| `fetchMarginDetailSzse(date)` | `GET /api/public/stock_margin_detail_szse` | 深交所个股两融明细 |

内置 3 次重试（间隔 2s/4s/8s），超时 60s。

#### 并发控制

同一 `(data_type, trade_date)` 不允许并发执行。定时任务和手动触发可能冲突，在 `CollectionOrchestrator` 入口加锁：

```java
// 基于 ReentrantLock 的 per-key 锁，防止同 data_type + trade_date 并发
private final ConcurrentHashMap<String, Lock> locks = new ConcurrentHashMap<>();

orchestrate(dataType, tradeDate):
    key = dataType + "_" + (tradeDate != null ? tradeDate : "latest")
    lock = locks.computeIfAbsent(key, k -> new ReentrantLock())
    if (!lock.tryLock()) {
        return "已在执行中，跳过"
    }
    try { ... } finally { lock.unlock(); locks.remove(key); }
```

或使用 Spring Integration 的 `LockRegistry`。

#### CollectionOrchestrator

核心编排器，管理 FETCH → CLEANSE 完整生命周期：

```
orchestrate(dataType, tradeDate):
    0. 获取并发锁 (dataType, tradeDate)
    1. 创建 FETCH log (status=RUNNING)
    2. 调用对应 AKToolsClient.fetchXxx() → 写入 raw_data（关联 collection_log_id）
    3. 更新 FETCH log → SUCCESS 或 FAILED + error_msg
    4. 如果 FETCH 成功，自动触发 CLEANSE
    5. 创建 CLEANSE log (status=RUNNING)
    6. 读 raw_data → 调用对应 CleanseService → @Transactional 写入业务表（INSERT ON DUPLICATE KEY UPDATE）
    7. 更新 CLEANSE log → SUCCESS 或 FAILED + error_msg
    8. 释放并发锁
```

CLEANSE 在一个事务内，失败回滚。FETCH 失败不触发 CLEANSE。

#### raw_data 月度归档

定时任务（每月 1 号凌晨）：
```
1. 查询 raw_data WHERE fetch_at < now() - 30 days
2. 按 fetch_at 月份分组，每组装为一个 JSON Lines 文件
3. GZIP 压缩写入 backups/raw_data/YYYY-MM.json.gz
4. 删除 DB 中已归档的记录
```

#### 股票日线策略

| 场景 | 方式 | 说明 |
|------|------|------|
| 首次全量回填 | `stock_zh_a_hist(symbol, start, end)`，每只股一次调用，一次返回多年历史 OHLCV（前复权 qfq） | ~5000 次调用，无需循环日期 |
| 日常增量 | 从 `stock_zh_a_spot_em` 获取当日 OHLC（此接口返回最新价/开盘/最高/最低/昨收），写入 `stock_daily` | 1 次调用覆盖全市场，无需逐只遍历 |

不再每天遍历 5000 只调 `stock_zh_a_hist`。

#### 行业/概念成分股变化检测

每日 CLEANSE 时与前一天快照对比（`stock_industry` / `stock_concept` 存的是当前最新状态）：

1. 采集当天全量成分股（460 次板块调用，200ms 间隔）
2. 与数据库当前记录逐板块对比
3. 新出现的 `(stock_code, sector_code)` → INSERT + 写 `classification_change_log` (action=ADD)
4. 消失的 → DELETE（或软删除）+ 写 `classification_change_log` (action=REMOVE)
5. 无变化的跳过

绝大多数交易日变化量为 0 或极少，不会产生冗余数据。

#### 全量采集限速

行业/概念成分股调用量大（~460 个板块）：

用限速器（200ms 间隔）+ 线程池（如 4 线程并行），预估 460 × 200ms / 4 ≈ 23 秒完成。

#### 采集顺序

```
1. trade_calendar → 2. stock_info → stock_daily（从快照提 OHLC）
                                   ↓
    3. industry → stock_industry（与 concept 并行）
    4. concept → stock_concept
    5. margin_daily → margin_stock（从两融明细提取标的）
                  ← 依赖 trade_calendar
```

#### 历史回填

```java
// 两融历史回填，按周分片
backfillMarginByWeek(startDate, endDate):
    tradeDates = tradeCalendarMapper.selectBetween(startDate, endDate)
    weeks = groupByWeek(tradeDates)
    for week in weeks:
        for date in week:
            orchestrator.orchestrate(MARGIN_DAILY_SSE, date, weekStart, weekEnd)
            orchestrator.orchestrate(MARGIN_DAILY_SZSE, date, weekStart, weekEnd)
```

每个 `data_collection_log` 记录带 `week_start`/`week_end`。当日采集（18:00 定时任务）的周字段为 null。

#### 缺口检测

```java
getGaps(startDate, endDate, exchange):
    expected = tradeCalendarMapper.selectBetween(startDate, endDate)
    actual = marginDailyMapper.selectDistinctTradeDates(startDate, endDate, exchange)
    return expected - actual
```

按周聚合，前端按周展示。

### REST API

```
GET  /api/v1/admin/collection/status                          # 各数据类最新采集状态
GET  /api/v1/admin/collection/logs?dataType=&limit=            # 最近 N 条日志
GET  /api/v1/admin/collection/gaps?start=&end=&exchange=       # 两融缺口
POST /api/v1/admin/collection/trigger/{dataType}               # 手动触发单类采集
POST /api/v1/admin/collection/backfill                         # 按日期范围补采
     Body: { dataType, exchange, startDate, endDate }
```

所有接口需 ADMIN 角色。

### 定时调度

```java
@Scheduled(cron = "0 0 16 * * MON-FRI")      // 交易日 16:00
collectStockSpot()                             // 写入 stock_info + 提取 OHLC 到 stock_daily

@Scheduled(cron = "0 0 17 * * MON-FRI")      // 交易日 17:00
collectIndustryAndConcept()                    // 行业/概念板块定义 + 成分股全量 + 变化检测

@Scheduled(cron = "0 0 18 * * MON-FRI")      // 交易日 18:00
collectMarginDaily()                           // 当日两融数据 SSE + SZSE（CLEANSE 同时更新 margin_stock）

@Scheduled(cron = "0 0 3 1 * *")             // 每月 1 号凌晨 3:00
archiveRawData()                               // 归档 30 天前的 raw_data 到文件
```

均带 `@Profile("!test")`，dev/prod 生效。

---

## §3 AKTools Docker 管理

### 启动

```bash
docker run -d \
  --name aktools \
  --restart unless-stopped \
  -p 8081:8080 \
  registry.cn-shanghai.aliyuncs.com/akfamily/aktools:1.8.95
```

- 镜像版本锁定（不设 `latest`），避免自动升级导致接口变化
- `--restart unless-stopped` 确保进程异常退出后自动重启
- 映射到 host 8081 端口，避免与后端 8080 冲突

### 健康检查

Java 端在每次调用前可先访问 `GET /api/public/tool_trade_date_hist_sina`（最轻量接口）验证 AKTools 在线状态。连续失败时可输出明确日志："AKTools Docker may be down, check `docker ps`"。

### 升级流程

```
1. 在 dev 环境拉取新镜像并启动
2. 验证关键接口（stock_zh_a_spot_em / stock_margin_detail_sse）
3. 确认字段兼容后更新 prod
4. 保留旧镜像 tag 以便回滚
```

---

## §4 前端设计

### 页面路由

```
frontend/src/app/(dashboard)/
├── admin/
│   └── collection/
│       ├── page.tsx              # 数据采集总览（各数据类状态卡片）
│       └── margin/
│           └── page.tsx          # 两融数据完整性（按周视图）
```

### 采集总览页

顶部统计条：运行中 / 成功 / 失败 计数。

下方 9 个状态卡片（对应 9 类数据），每个展示：

- 最后采集状态（✅ 成功 / 🔴 失败 / ⬜ 未触发）
- FETCH + CLEANSE 各自状态
- 最后采集时间、记录数
- 失败时显示 error_msg
- 点击展开最近 5 条日志
- [重新采集] 按钮

### 两融完整性页

按周表格：

```
周范围 | 交易所 | 应采 | 已采 | 缺口 | 状态 | 操作
```

- ✅ 完整 / ⚠️ 缺 N 天（显示具体日期）/ ❌ 未采集
- 顶部日期范围筛选 + [重新检测缺口]
- [补采缺失] 按钮对缺口日期发起补采

### 补采弹窗

下拉选择数据类型 + 交易所，日期范围选择器，[开始补采] 按钮。

### 导航

左侧栏新增"数据采集"菜单项，子项"采集状态"和"两融完整性"。

---

## §5 不在此范围

- 用户注册/密码管理
- 股票查询功能（子项目 3）
- 两融分析/标签管理（子项目 4）
- Docker 部署 / CI/CD
- 告警通知（邮件/站内信预留接口，Phase 2 实现）

---

## §6 风险与注意事项

| 风险 | 应对 |
|------|------|
| AKTools 单日接口调用量大（~1000 次，含成分股 460 次） | 限速 200ms 间隔 + 4 线程池并行 |
| 交易所接口偶尔限流/失效 | 3 次重试 + 失败记录 + 手动补采 |
| AKTools Docker 版本升级接口变化 | 锁定镜像版本，升级前先在 dev 验证 |
| 第一次历史全量回填耗时长 | stock_daily 首次全量 (~5000 次调用，每只股返回多年数据)；两融按周分片回填 (~5800 次调用 / 两个交易所）。断点续传，优先回填最近数据再往前回溯 |
| 定时任务与手动触发并发 | per-key ReentrantLock 防重入 |
| AKTools Docker 不可用 | 连续失败时日志明确提示，管理后台红色标记 |
| raw_data 存储膨胀 | 30 天保留 + 月度 GZIP 归档 |
| stock_industry/concept 每日全量写入冗余 | 变化检测 + 仅写入差异 + change_log |
| ~~两融标记字段名未知~~ | 已修正：`stock_zh_a_spot_em` **不含两融标记**。`margin_stock` 改为从 `margin_detail` CLEANSE 时提取标的列表 |

---

## 功能描述（供 speckit-specify 使用）

> 此章节是纯 WHAT 摘要，不含技术实现细节，专门作为 speckit 工具链的输入接口。

管理员可查看各数据类的采集状态（成功/失败/运行中），手动触发采集或对缺失数据发起补采。系统定时自动从外部数据源采集以下数据：A 股全量股票基础信息（代码、名称、最新价、涨跌幅、PE/PB/总市值/流通市值等，保留每日快照）、股票日线行情（开盘/最高/最低/收盘/成交量/成交额）、东方财富行业板块分类及各行业成分股、东方财富概念板块分类及各概念成分股、A 股交易日历、两融标的列表（融资标的/融券标的/两者）、每日两融交易明细（融资余额/融资买入额/融资偿还额/融券余额/融券卖出量/融券余量等，按股票按交易日）。历史数据按周回填，支持断点续传。系统自动检测两融数据缺口，管理员可按周查看数据完整性并补采缺失交易日。所有采集任务失败时自动重试三次，失败信息在管理后台可见。
