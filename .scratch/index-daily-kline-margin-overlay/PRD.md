Status: ready-for-agent

# PRD: 指数日线数据采集与K线+两融叠加图

## Problem Statement

当前项目缺少以下核心能力：
1. 无法查看上证指数、深证成指、创业板指等宽基指数的日K走势
2. 无法查看行业/概念板块的指数日K走势
3. 无法将指数走势与两融数据叠加对比，判断杠杆资金与价格走势的联动关系
4. 行业/概念列表页只有单日快照数据，缺少历史K线+两融趋势图

## Solution

1. 扩展 `index_daily` 表，新增 `index_type` 字段，统一存储宽基/行业/概念三类指数日线数据
2. 新增3个独立采集类型，分别从AKTools获取宽基指数、行业指数、概念指数的日线OHLCV
3. 新增"指数分析"概览页，展示核心指数K线+全市场两融叠加图
4. 新增行业/概念详情页，展示板块指数K线+板块两融叠加图
5. 板块两融通过实时聚合 `margin_daily` + `stock_industry`/`stock_concept` 查询，不建物化表

## User Stories

### 数据采集

1. As an admin, I want to trigger MARKET_INDEX_DAILY collection, so that 上证指数/沪深300/中证500等宽基指数日线数据被采集入库
2. As an admin, I want to trigger INDUSTRY_INDEX_DAILY collection, so that 同花顺446个行业指数日线数据被采集入库
3. As an admin, I want to trigger CONCEPT_INDEX_DAILY collection, so that 同花顺400+概念指数日线数据被采集入库
4. As an admin, I want to see collection status for all 3 new index types on the collection hub page, so that I know whether data is up to date
5. As an admin, I want index daily data to be cleansed with change_pct calculated, so that 涨跌幅可直接查询无需实时计算

### 指数分析概览页

6. As a user, I want to visit /admin/index-analysis, so that I can see a dashboard of core market indices
7. As a user, I want to see 上证指数 K线+沪市两融总额 叠加图, so that I can observe the relationship between Shanghai index movement and margin trading
8. As a user, I want to see 深证成指 K线+深市两融总额 叠加图, so that I can observe the relationship between Shenzhen index movement and margin trading
9. As a user, I want to see 创业板指 K线+深市两融总额 叠加图, so that I can observe growth stock leverage trends
10. As a user, I want to see 沪深300 K线+两市两融总额 叠加图, so that I can observe the broad market leverage relationship
11. As a user, I want to switch date range (1m/3m/6m/1y) on the index analysis page, so that I can focus on different time periods
12. As a user, I want to see K线(左轴) + 成交量(底部) + 融资余额(右轴) + 融券余额(右轴) in the overlay chart, so that the chart is consistent with the stock detail page pattern

### 行业/概念详情页

13. As a user, I want to click an industry name on /admin/industries to navigate to /admin/industries/[name], so that I can see the industry detail page
14. As a user, I want to see the industry index K线 on the industry detail page, so that I can observe the industry's price trend
15. As a user, I want to see the industry index K线 + 板块两融 叠加图 on the industry detail page, so that I can observe leverage flow into/out of this industry
16. As a user, I want to click a concept name on /admin/concepts to navigate to /admin/concepts/[name], so that I can see the concept detail page
17. As a user, I want to see the concept index K线 + 板块两融 叠加图 on the concept detail page, so that I can observe leverage flow into/out of this concept
18. As a user, I want to switch date range and K-line period (daily/weekly/monthly) on industry/concept detail pages, so that I have flexible analysis views
19. As a user, I want to see the constituent stocks list on the industry/concept detail page, so that I know which stocks belong to this sector

### 板块两融

20. As a user, I want to see sector margin data (融资余额汇总/融券余额汇总) as a time series on the overlay chart, so that I can see how leverage in this sector changes over time
21. As a user, I want the sector margin to be calculated by aggregating all constituent stocks' margin_daily data, so that the data is accurate and consistent with individual stock data
22. As a user, I want the sector margin to use current industry/concept affiliation (snapshot), so that the query is simple and fast even if historical affiliation changes are not tracked

### 指数列表

23. As a user, I want to see a list of all tracked market indices with their latest values and change_pct on the index analysis page, so that I have a quick market overview
24. As a user, I want to see P1 indices (上证50, 科创50, 中证1000) in addition to the core 5, so that I have broader market coverage

## Implementation Decisions

### 数据模型

- **`index_daily` 表扩展**: 新增 `index_type VARCHAR(10) NOT NULL` 字段，取值 MARKET/INDUSTRY/CONCEPT。唯一键从 `(index_code, trade_date)` 扩展为 `(index_code, index_type, trade_date)`
- **`index_daily` 新增字段**: `amount DECIMAL(16,2)` (成交额), `change_pct DECIMAL(6,2)` (涨跌幅，清洗阶段计算)
- **不新增独立表**: 行业/概念指数日线统一存入 `index_daily`，不建 `industry_index_daily` 或 `concept_index_daily`（ADR-0001）
- **不建板块两融物化表**: 板块两融通过实时聚合查询，不建 `sector_margin_daily`（ADR-0002）

### 采集层

- **3个独立采集类型**: `MARKET_INDEX_DAILY`, `INDUSTRY_INDEX_DAILY`, `CONCEPT_INDEX_DAILY`，各自独立触发
- **宽基指数数据源**: AKTools `stock_zh_index_daily`，按指数代码逐一拉取
- **行业指数数据源**: AKTools `stock_board_industry_index_ths`，按行业名称逐一拉取
- **概念指数数据源**: AKTools `stock_board_concept_index_ths`，按概念名称逐一拉取
- **核心宽基指数清单(P0)**: sh000001(上证指数), sz399001(深证成指), sz399006(创业板指), sh000300(沪深300), sh000905(中证500)
- **补充宽基指数清单(P1)**: sh000016(上证50), sh000688(科创50), sh000852(中证1000)
- **行业/概念名称来源**: 从现有 `industry` / `concept` 表读取，遍历采集

### 清洗层

- **change_pct 计算**: `(今日close - 昨日close) / 昨日close * 100`，首日无前值则为null
- **行业/概念指数代码生成**: 需要建立行业/概念名称到指数代码的映射关系（同花顺行业指数无标准代码，需自行生成或使用名称作为标识）

### API层

- **指数日线查询**: `GET /api/v1/admin/index-daily?indexCode=sh000001&indexType=MARKET&startDate=...&endDate=...`
- **指数列表查询**: `GET /api/v1/admin/index-list` 返回所有已采集指数的最新行情
- **板块两融聚合查询**: `GET /api/v1/admin/sector-margin?sectorType=INDUSTRY&sectorName=银行&startDate=...&endDate=...` 实时聚合 margin_daily + stock_industry
- **行业/概念详情**: `GET /api/v1/admin/industries/{name}` 和 `GET /api/v1/admin/concepts/{name}` 扩展返回指数日线+板块两融数据

### 前端

- **指数分析页**: `/admin/index-analysis`，展示5-8个核心指数的K线+两融叠加图
- **行业详情页**: `/admin/industries/[name]`，展示行业指数K线+板块两融叠加图+成分股列表
- **概念详情页**: `/admin/concepts/[name]`，展示概念指数K线+板块两融叠加图+成分股列表
- **图表模式**: 统一双轴叠加 — K线(左轴) + 成交量(底部) + 融资余额(右轴,绝对值) + 融券余额(右轴,绝对值)，与个股详情页一致
- **图表组件**: 复用 `lightweight-charts`，提取可复用的 KlineMarginOverlay 组件

### 板块两融聚合

- **聚合SQL**: JOIN `margin_daily` + `stock_industry`/`stock_concept`，按 `trade_date` + `sector_name` GROUP BY SUM
- **归属快照**: 使用当前 `stock_industry`/`stock_concept` 的归属关系，不追踪历史变更
- **性能**: 5000股 × 100天 ≈ 50万行，MySQL聚合毫秒级，无需物化

## Testing Decisions

- **好的测试标准**: 只测试外部行为（API返回的数据是否正确），不测试内部实现（SQL怎么写、聚合怎么算）
- **需测试的模块**:
  - 指数日线采集+清洗: 验证OHLCV数据入库、change_pct计算正确
  - 板块两融聚合查询: 验证聚合结果与手动SUM一致
  - 指数日线API: 验证按indexType过滤正确、日期范围过滤正确
  - 行业/概念详情API: 验证返回的指数日线和板块两融数据匹配
- **测试先例**: 参考现有 `MarginCleanseServiceImpl` 的测试模式（验证清洗后字段计算正确）

## Out of Scope

- **全市场日交易量**: 不建 `market_overview_daily` 表，当前目标不需要
- **行业归属时序追踪**: `stock_industry` 保持快照型，不加 `effective_date`
- **板块两融物化表**: 不建 `sector_margin_daily`，实时聚合
- **归一化涨跌幅图表**: 不实现，统一使用双轴叠加
- **龙虎榜/深港通等新增数据**: 属于后续迭代，不在本次范围
- **SSE/SZSE官方数据源替代**: 属于数据源优化，不在本次范围
- **申万行业指数**: 可作为后续补充，本次只做同花顺行业/概念指数

## Further Notes

- 相关ADR: [ADR-0001](../docs/adr/0001-unified-index-daily-table.md) 指数日线统一表, [ADR-0002](../docs/adr/0002-sector-margin-realtime-aggregation.md) 板块两融实时聚合
- 相关文档: [exchange-data-source-analysis.md](../docs/exchange-data-source-analysis.md) 沪深交易所数据源分析
- 行业/概念指数无标准代码体系，需在清洗阶段建立名称→代码的映射。建议使用 `ths_ind_{序号}` / `ths_cpt_{序号}` 格式生成，或直接使用行业/概念名称的hash作为代码
- 行业指数约446个，概念指数约400+个，全量采集可能耗时较长。建议行业/概念指数采集支持增量模式（只采集有新交易日的数据）
