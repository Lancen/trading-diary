
# trading-diary

A 股全市场数据采集与管理平台 —— 覆盖股票行情、日线、两融、行业/概念板块指数，支持历史补采和交易日历监控。

**当前阶段：Phase 1（业务开发）**

## 技术栈

| 层 | 选型 |
|------|------|
| 语言 | Java 17+ |
| 框架 | Spring Boot 3.2.x |
| ORM | MyBatis-Plus 3.5+ |
| 数据库 | MySQL 8.0 |
| 构建 | Gradle |
| 前端 | Next.js 14+ / TypeScript / TailwindCSS / shadcn/ui |

完整技术栈见 [CLAUDE.md](CLAUDE.md)。

## 快速开始

```bash
# 环境检查
scripts/check-env.sh

# AKTools（行情数据源）
pip3 install aktools
python3 -m aktools --host 127.0.0.1 --port 8081 &

# 后端（必须激活 dev profile，否则 JWT 配置不加载）
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun

# 前端
cd frontend && pnpm install && pnpm dev
```

验证：

```bash
curl http://localhost:8081/version          # AKTools 健康
curl http://localhost:8080/actuator/health  # 后端健康
curl http://localhost:3000                   # 前端健康
```

## 数据采集架构

### 采集管线两阶段模型

所有 AKTools 数据类型遵循统一的 **FETCH → CLEANSE** 两阶段管线：

```
FETCH 阶段：调 AKTools API → 原始 JSON 存入 raw_data 表
CLEANSE 阶段：读 raw_data → 解析/转换 → 写入业务表
```

**核心规则**：
- FETCH 必须调 API 并入库 raw_data，CLEANSE 只从 raw_data 读取
- 已有成功的 FETCH 记录时，CLEANSE 可直接复用，无需重复调 API
- 板块指数K线（INDUSTRY_INDEX_DAILY / CONCEPT_INDEX_DAILY）走特殊路径：FETCH 阶段遍历所有板块逐个调 API，每板块一条 raw_data（含 sector_code 标识）

### 数据类型总览

#### 日级采集（每个交易日执行）

| 数据类型 | 标签 | 数据源 | 采集方式 | 目标表 |
|---------|------|--------|---------|--------|
| STOCK_INFO | 股票行情（含日线） | 新浪 `stock_zh_a_spot` | AKTools HTTP | stock_info + stock_daily |
| STOCK_DAILY | 日线行情 | 复用 STOCK_INFO 的 FETCH 数据 | 无独立 FETCH | stock_daily |
| MARGIN_DAILY_SSE | 两融明细(沪市) | 上交所 `stock_margin_detail_sse` | AKTools HTTP | margin_daily |
| MARGIN_DAILY_SZSE | 两融明细(深市) | 深交所 `stock_margin_detail_szse` | AKTools HTTP | margin_daily |
| MARGIN_MACRO_SSE | 两融总量(沪市) | 东方财富 `macro_china_market_margin_sh` | AKTools HTTP | margin_macro |
| MARGIN_MACRO_SZSE | 两融总量(深市) | 东方财富 `macro_china_market_margin_sz` | AKTools HTTP | margin_macro |
| MARKET_INDEX_DAILY | 宽基指数日线 | 东方财富 `stock_zh_index_daily_em` | AKTools HTTP | market_index_daily |
| INDUSTRY_INDEX_DAILY | 行业指数日线 | 同花顺 `stock_board_industry_index_ths` | AKTools HTTP（~90次调用） | sector_index_daily |
| CONCEPT_INDEX_DAILY | 概念指数日线 | 同花顺 `stock_board_concept_index_ths` | AKTools HTTP（~370次调用） | sector_index_daily |

#### 非日级采集（月级/不定期）

| 数据类型 | 标签 | 数据源 | 采集方式 | 频率 | 目标表 |
|---------|------|--------|---------|------|--------|
| TRADE_CALENDAR | 交易日历 | 新浪 `tool_trade_date_hist_sina` | AKTools HTTP | 不定期（首次全量） | trade_calendar |
| INDUSTRY_NAME | 行业板块分类 | 同花顺 `stock_board_industry_name_ths` | AKTools HTTP | 每月 | industry |
| CONCEPT_NAME | 概念板块分类 | 同花顺 `stock_board_concept_name_ths` | AKTools HTTP | 每月 | concept |
| INDUSTRY_CONS | 行业成分股 | 同花顺页面 | Playwright 抓取 → 后台导入 | 每月 | stock_industry |
| CONCEPT_CONS | 概念成分股 | 同花顺页面 | Playwright 抓取 → 后台导入 | 每月 | stock_concept |

### 依赖关系

```
交易日历 ──→ 股票行情（自动联动日线） ──→ 行业/概念名称 ──→ 行业/概念成分股（Playwright → 导入）
                │                                              │
                ├──→ 两融明细(沪/深)                            ├──→ 行业指数日线（依赖行业名称）
                ├──→ 两融总量(沪/深)                            └──→ 概念指数日线（依赖概念名称）
                └──→ 宽基指数日线
```

**关键规则**：
- `STOCK_DAILY` 不独立采集，复用 `STOCK_INFO` 的同一份 API 返回数据
- `INDUSTRY_INDEX_DAILY` / `CONCEPT_INDEX_DAILY` 依赖 `industry` / `concept` 表有数据（需先采名称）
- 板块两融数据不单独存储，通过实时聚合 `margin_daily` + `stock_industry`/`stock_concept` 计算

## 数据采集：首次启动完整流程

> 管理后台 `http://localhost:3000/admin/collection` 提供可视化采集界面，以下用 API 示例说明执行顺序。

### 第 1 步：交易日历

所有日级采集都依赖交易日历判断数据完整性。先拉取 A 股历史交易日（1990 年至今），后续增量更新。

```bash
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/TRADE_CALENDAR
```

### 第 2 步：股票行情（含日线）

全市场实时行情快照（代码、名称、价格、涨跌幅、PE/PB/市值等 15 字段），数据源新浪 `stock_zh_a_spot`。
**同一份 API 返回同时写入 `stock_info` 和 `stock_daily` 两张表**。

```bash
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/STOCK_INFO
```

> **历史日线回填**：`stock_daily` 支持按个股 + 日期范围补采（数据源腾讯 `stock_zh_a_hist_tx`）。管理后台 → 股票行情详情页 → "历史补采" → 选择日期范围。

### 第 3 步：行业 + 概念板块名称

```bash
# 同花顺行业分类（~90 个）
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/INDUSTRY_NAME

# 同花顺概念分类（~370 个）
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/CONCEPT_NAME
```

### 第 4 步：行业/概念成分股

数据源为同花顺页面，需通过 Playwright 抓取。**需要配置同花顺 Cookie**（超过5页数据需要登录）。

```bash
# 抓取全部行业+概念成分股，保存到 data/constituents/constituents-YYYY-MM-DD.json
# 耗时约 20 分钟
python3 scripts/scrape_ths_constituents.py

# 调试/快速验证：仅抓取 5 个概念
python3 scripts/scrape_ths_constituents.py --concept --limit 5
```

抓取完成后，管理后台 → 数据采集 Hub → 底部"成分股数据"卡片 → 点击进入管理页 → **导入**。

**Cookie 配置**：管理后台 → 数据采集 Hub → "同花顺 Cookie 配置"区域 → 粘贴完整 Cookie → 保存。获取方法：登录同花顺 → F12 → Application → Cookies → q.10jqka.com.cn → 复制全部 Cookie。

**重新采集行为**：已有关联个股的行业/概念重新采集时，不在新列表中的旧记录会被软删除（`is_deleted=true`），新增的记录插入，已有的记录更新 `snap_date`。

### 第 5 步：两融数据

```bash
# 上交所个股两融明细
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/MARGIN_DAILY_SSE

# 深交所个股两融明细
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/MARGIN_DAILY_SZSE

# 上交所两融总量
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/MARGIN_MACRO_SSE

# 深交所两融总量
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/MARGIN_MACRO_SZSE
```

两融数据支持按日期补采（个股级 + 全市场级），通过管理后台 → 各采集详情页 → "触发采集"。

### 第 6 步：板块指数K线

行业/概念指数的 OHLCV 日线数据，数据源同花顺。每个板块一次 API 调用可拉取任意日期范围的历史数据。

```bash
# 行业指数日线（~90 个行业，约 2 分钟）
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/INDUSTRY_INDEX_DAILY

# 概念指数日线（~370 个概念，约 2 分钟）
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/CONCEPT_INDEX_DAILY
```

**历史补采**：管理后台 → 行业/概念指数日线详情页 → "历史补采" → 选择日期范围。每个板块一次调用拉全范围，补采1年或3年历史都是 ~490 次调用，约 2 分钟完成。

### 首次启动采集顺序总结

```
(1) 交易日历  ──→  (2) 股票行情（自动联动日线）  ──→  (3) 行业 + 概念名称
                                                       ↓
                                                (4) 行业/概念成分股（Playwright → 导入）
                                                       ↓
                                                (5) 两融明细 + 两融总量（沪/深各 2 个）
                                                       ↓
                                                (6) 行业指数日线 + 概念指数日线
```

## 日常增量采集

全部通过管理后台 `http://localhost:3000/admin/collection` 操作：

### 一键采集

管理后台 → 数据采集 → **每日采集** Tab → ⚡ 一键采集按钮，按依赖顺序依次执行全部日级采集任务（共9个）。

**特性**：
- **幂等**：FETCH 阶段已有成功记录时复用 raw_data，CLEANSE 阶段用软删除+覆盖不会产生重复数据
- **断点续**：已完成的步骤（FETCH + CLEANSE 均成功）自动跳过，从中断处继续执行
- **按顺序执行**：交易日历 → 股票行情 → 两融明细 → 两融总量 → 宽基指数 → 板块指数

```bash
# API 方式
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger-daily
```

### 手动采集

| 频率 | 采集类型 | 管理后台入口 | 说明 |
|------|---------|-------------|------|
| 每个交易日 16:00 | 股票行情 | 每日采集 → 股票行情卡片 → 触发采集 | 自动联动日线入库 |
| 每个交易日 17:00 | 两融明细/总量 | 每日采集 → 对应卡片 → 触发采集 | 沪深各2个，共4个类型 |
| 每个交易日 | 宽基指数日线 | 每日采集 → 宽基指数日线卡片 → 触发采集 | 沪深300、中证500等 |
| 每个交易日 | 行业/概念指数日线 | 每日采集 → 对应卡片 → 触发采集 | ~490个板块，约2分钟 |
| 每月 | 行业/概念名称 | 固定采集 → 对应卡片 → 触发采集 | 板块分类变动较少 |
| 每月 | 成分股抓取+导入 | `python3 scripts/scrape_ths_constituents.py` → 后台导入 | 需配置Cookie |

> 定时任务（`CollectionScheduler.java`）已暂停（`@Scheduled` 已注释），当前阶段使用手动触发。恢复时取消注释即可自动运行，默认调度：工作日 16:00 股票行情、17:00 板块分类、18:00 两融数据。

## 数据完整性监控

管理后台每个采集详情页都带**交易日历**（月视图，三态标记）：
- 🟢 绿色 = 已采集
- 🔴 红色 = 交易日缺数据（可点击触发补采）
- ⬜ 灰色 = 非交易日

## 管理后台页面

| 页面 | 路径 | 功能 |
|------|------|------|
| 数据采集 | `/admin/collection` | 每日采集（流程图+一键采集）/ 固定采集 + Cookie 配置 + 成分股管理入口 |
| 采集详情 | `/admin/collection/[dataType]` | 管线状态 + 触发采集 + 交易日历 + 日志表 + 历史补采 |
| 成分股管理 | `/admin/collection/constituents` | 文件列表 + 导入操作 |
| 板块排名 | `/admin/rankings` | 行业/概念双Tab + 成交额/涨幅/环比排序 + 时间筛选 + 区间变动 |
| 股票数据 | `/admin/stocks` | 筛选/排序/分页浏览 |
| 股票详情 | `/admin/stocks/[code]` | K 线图 + 两融叠加 + 日线明细 |
| 行业列表 | `/admin/industries` | 行业维度两融聚合 + 采集 + 置顶排序 |
| 行业详情 | `/admin/industries/[code]` | K 线图 + 两融叠加 + 关联度 + 成分股列表 |
| 概念列表 | `/admin/concepts` | 概念维度两融聚合 + 采集 + 置顶排序 |
| 概念详情 | `/admin/concepts/[code]` | K 线图 + 两融叠加 + 关联度 + 成分股列表 |
| 融资统计 | `/admin/margin-stats` | 全市场两融总量 |

## 验证

```bash
# 各采集类型状态
curl http://localhost:8080/api/v1/admin/collection/status

# 交易日历覆盖度（默认查股票行情，支持 ?dataType=MARGIN_DAILY_SSE 等）
curl "http://localhost:8080/api/v1/admin/stocks/calendar?year=2026&month=5"

# 两融数据完整性
curl "http://localhost:8080/api/v1/admin/collection/gaps?start=2026-01-01&end=2026-05-17&exchange=SSE"

# AKTools 交易日历数量
curl http://localhost:8081/api/public/tool_trade_date_hist_sina | python3 -c "import sys,json; print(f'{len(json.load(sys.stdin))} 条交易日')"
```

## 项目结构

```
trading-diary/
├── src/main/java/com/tradingdiary/   # 后端
│   └── collection/                    #   采集管线（client / orchestrator / scheduler / controller）
├── frontend/                          # 前端（Next.js 14 App Router）
│   └── e2e/                           #   E2E 测试（Playwright, 10 用例）
├── specs/                             # 功能规范
│   └── 003-data-collection/           #   数据采集 UI/UE 重构
├── docs/
│   ├── standards/                     # 技术规范
│   ├── adr/                           # 架构决策记录
│   └── superpowers/                   # 开发流程
├── scripts/                           # 工具脚本
└── data/                              # 采集数据文件（constituents JSON）
```

## 文档

| 文档 | 说明 |
|------|------|
| [CLAUDE.md](CLAUDE.md) | AI 编码准则 |
| [CONTEXT.md](CONTEXT.md) | 领域词汇表 |
| [项目宪法](specs/_governance/constitution.md) | 工程决策原则 |
| [技术规范](docs/standards/technical-standards.md) | 代码标准、数据库、API、安全等 |
| [采集管线规则](.claude/rules/collection-pipeline.md) | 采集类型分级、日历路由、表注意事项 |
| [ADR-0002](docs/adr/0002-sector-margin-realtime-aggregation.md) | 板块两融实时聚合决策 |
| [ADR-0003](docs/adr/0003-sector-index-daily-pipeline-refactor.md) | 板块指数K线采集管线重构 |
| [Feature 状态](specs/_feature-status.md) | 当前 feature 进度 |

## 工程原则

1. **约定优于配置** — 优先使用 Spring Boot 默认约定
2. **分层不可逆** — Controller → Service → Mapper
3. **防御式编程** — 所有外部输入验证
4. **渐进式优化** — 先正确，再优化
