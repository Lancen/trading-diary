# CONTEXT — 领域词汇表

## 核心概念

| 术语 | 定义 | 别称 |
|------|------|------|
| **采集** | 从外部 API（东方财富、新浪、同花顺）拉取原始数据 | Fetch、拉取 |
| **清洗** | 原始数据入库 + 计算衍生字段（环比变化等） | Cleanse、入库 |
| **采集管线** | 采集 → 清洗 两步流程，`DataCollectionLog` 追踪每步状态 | Collection Pipeline |
| **股票行情** | `StockInfo`（基本信息）+ `StockDaily`（日线OHLCV）的统称 | Stock Quote |
| **两融** | 融资融券的统称。覆盖个股级（`MarginDaily`）、全市场级（`MarginMacro`）、板块级（聚合查询） | 融资融券、Margin |
| **融资余额** | 投资者融资买入未偿还的金额 | `margin_balance` |
| **融券余额** | 投资者融券卖出未偿还的金额 | `short_balance` |
| **两融总额** | 融资余额 + 融券余额 | `total_balance` |
| **全市场两融** | 沪/深交易所整体的两融数据，存于 `MarginMacro` 表。按交易所(SSE/SZSE)分列 | Market Margin、`margin_macro` |
| **板块两融** | 某行业/概念下所有成分股两融余额的汇总值。无独立表，实时聚合 `MarginDaily` + `StockIndustry`/`StockConcept` 查询。使用当前归属快照，不追踪历史归属变更 | Sector Margin |
| **两融环比** | 本交易日两融余额较上一交易日的变化值 | `margin_change`、`short_change` |
| **成分股** | 行业/概念的成分股关系数据。来源：同花顺快照，每月导入。写入 `stock_industry`、`stock_concept` 表 | Constituent |
| **概念** | 概念板块分类（如 MSCI中国、沪深300）。与"行业"处理逻辑相同，仅分类维度不同 | Concept |
| **行业** | 行业板块分类（如 银行、房地产）。与"概念"处理逻辑相同，仅分类维度不同 | Industry |
| **交易日** | A股实际开市交易日期。交易日历三态：已采集✓ / 交易日缺数据✗ / 非交易日 | Trade Date |
| **日线** | 单日 OHLCV（开盘/最高/最低/收盘/成交量）数据 | Daily Kline |
| **指数日线** | 宽基指数（上证指数、沪深300等）日线数据，存于 `market_index_daily` 表，`index_code` 为标准交易所代码（如 sh000001）。`change_pct` 在清洗阶段计算。`index_daily` 表保持原样不动 | Market Index Daily Kline |
| **板块指数日线** | 行业/概念指数日线数据，存于 `sector_index_daily` 表，`sector_type` 区分 INDUSTRY/CONCEPT，`sector_code` 为板块编码（关联 `industry.code`/`concept.code`）。与指数日线分表存储，因标识体系不同（标准代码 vs sector_code）且数量级差异大（8 vs 446 vs 400+）。采集拆为3个独立类型：`MARKET_INDEX_DAILY`、`INDUSTRY_INDEX_DAILY`、`CONCEPT_INDEX_DAILY` | Sector Index Daily Kline |
| **K线+两融叠加图** | 统一图表模式：K线(左轴) + 成交量(底部) + 融资余额(右轴,绝对值) + 融券余额(右轴,绝对值)。个股/指数/行业/概念均使用此模式，`lightweight-charts` 双轴叠加实现 | Dual-Axis Overlay |
| **周K** | 日线按 ISO 周聚合：开盘=周一开盘，收盘=周五收盘，最高/最低=周内极值，成交量=周总和 | Weekly Kline |
| **月K** | 日线按自然月聚合：开盘=首日开盘，收盘=末日收盘，最高/最低=月内极值，成交量=月总和 | Monthly Kline |

## 系统角色

| 术语 | 定义 |
|------|------|
| **数据采集 Hub** | `/admin/collection` — 总览页。9 张卡片（8 个采集类型 + 成分股数据），纯展示采集状态 |
| **数据浏览** | 侧边栏分组名。含股票数据、概念列表、行业列表、融资统计 4 个子页面 |
| **指数分析** | `/admin/index-analysis` — 大盘概览页。展示多个核心指数K线+全市场两融叠加图 |
| **行业详情** | `/admin/industries/[name]` — 单个行业K线+板块两融叠加图（新增） |
| **概念详情** | `/admin/concepts/[name]` — 单个概念K线+板块两融叠加图（新增） |
| **融资统计** | `/admin/margin-stats` — 全市场两融总量汇总（融资余额/融券余额/两融总额/标的数量） |
| **管线状态** | 采集/清洗步骤的成功/失败/未触发状态，用 `StatusBadge` 组件展示 |

## 外部系统

| 系统 | 用途 | 频率 |
|------|------|------|
| **东方财富 API** | 股票行情、两融数据、交易日历 | 每日 |
| **新浪 API** | 股票日线数据 | 按需 |
| **同花顺** | 行业/概念成分股快照 | 每月 |

## 系统约束

- **启动 Profile**：后端必须 `SPRING_PROFILES_ACTIVE=dev`，否则 JWT 配置为空导致登录 NPE
- **margin_macro 表**：无 `is_deleted` 字段（与其他表不同）
- **采集类型分两级**：日级（需交易日历）+ 月级快照（无需日历），详见 `.claude/rules/collection-pipeline.md`

## 使用者

当前个人使用（ADMIN 角色），后续可能开放他人。主要用途：数据采集状态监控 + 市场数据浏览。
