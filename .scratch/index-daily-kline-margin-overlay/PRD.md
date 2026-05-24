Status: ready-for-agent

# PRD: 指数日线数据采集与K线+两融叠加图

## 问题陈述

当前项目缺少以下核心能力：
1. 无法查看上证指数、深证成指、创业板指等宽基指数的日K走势
2. 无法查看行业/概念板块的指数日K走势
3. 无法将指数走势与两融数据叠加对比，判断杠杆资金与价格走势的联动关系
4. 行业/概念列表页只有单日快照数据，缺少历史K线+两融趋势图

## 解决方案

1. `index_daily` 表保持不变，仅存宽基指数；新增 `sector_index_daily` 表存行业/概念指数日线数据
2. 新增3个独立采集类型，分别从AKTools获取宽基指数、行业指数、概念指数的日线OHLCV
3. 新增"指数分析"概览页，展示核心指数K线+全市场两融叠加图
4. 新增行业/概念详情页，展示板块指数K线+板块两融叠加图
5. 板块两融通过实时聚合 `margin_daily` + `stock_industry`/`stock_concept` 查询，不建物化表

## 用户故事

### 数据采集

1. 作为管理员，我希望触发 MARKET_INDEX_DAILY 采集，以便上证指数/沪深300/中证500等宽基指数日线数据被采集入库
2. 作为管理员，我希望触发 INDUSTRY_INDEX_DAILY 采集，以便同花顺446个行业指数日线数据被采集入库
3. 作为管理员，我希望触发 CONCEPT_INDEX_DAILY 采集，以便同花顺400+概念指数日线数据被采集入库
4. 作为管理员，我希望在采集总览页看到3个新指数采集类型的状态，以便了解数据是否最新
5. 作为管理员，我希望指数日线数据在清洗阶段计算 change_pct，以便涨跌幅可直接查询无需实时计算

### 指数分析概览页

6. 作为用户，我希望访问 /admin/index-analysis 页面，以便查看核心指数的大盘概览
7. 作为用户，我希望看到上证指数K线+沪市两融总额叠加图，以便观察沪市走势与杠杆资金的关系
8. 作为用户，我希望看到深证成指K线+深市两融总额叠加图，以便观察深市走势与杠杆资金的关系
9. 作为用户，我希望看到创业板指K线+深市两融总额叠加图，以便观察成长股杠杆趋势
10. 作为用户，我希望看到沪深300K线+两市两融总额叠加图，以便观察全市场杠杆关系
11. 作为用户，我希望在指数分析页切换日期范围（1月/3月/6月/1年），以便聚焦不同时间段
12. 作为用户，我希望叠加图展示K线(左轴)+成交量(底部)+融资余额(右轴)+融券余额(右轴)，以便与个股详情页图表模式一致

### 行业/概念详情页

13. 作为用户，我希望在行业列表页点击行业名称跳转到 /admin/industries/[name] 行业详情页
14. 作为用户，我希望在行业详情页看到行业指数K线，以便观察该行业的价格走势
15. 作为用户，我希望在行业详情页看到行业指数K线+板块两融叠加图，以便观察该行业的杠杆资金进出
16. 作为用户，我希望在概念列表页点击概念名称跳转到 /admin/concepts/[name] 概念详情页
17. 作为用户，我希望在概念详情页看到概念指数K线+板块两融叠加图，以便观察该概念的杠杆资金进出
18. 作为用户，我希望在行业/概念详情页切换日期范围和K线周期（日K/周K/月K），以便灵活分析
19. 作为用户，我希望在行业/概念详情页看到成分股列表，以便了解该板块包含哪些股票

### 板块两融

20. 作为用户，我希望在叠加图上看到板块两融数据（融资余额汇总/融券余额汇总）的时间序列，以便观察板块杠杆随时间的变化
21. 作为用户，我希望板块两融通过聚合所有成分股的 margin_daily 数据计算，以便数据准确且与个股数据一致
22. 作为用户，我希望板块两融使用当前的行业/概念归属快照计算，以便查询简单快速

### 指数列表

23. 作为用户，我希望在指数分析页看到所有已跟踪指数的最新值和涨跌幅列表，以便快速了解市场全貌
24. 作为用户，我希望在核心5个指数之外还能看到上证50、科创50、中证1000，以便覆盖更广的市场维度

## 实现决策

### 数据模型

- **`index_daily` 表保持不变**: 仅存宽基指数，`index_code` 继续使用标准交易所代码（如 sh000001），唯一键 `(index_code, trade_date)` 不变
- **新增 `sector_index_daily` 表**: 存行业/概念指数日线数据，含 `sector_type`（INDUSTRY/CONCEPT）+ `sector_name`（板块名称）字段，唯一键 `(sector_type, sector_name, trade_date)`
- **分表原因**: 宽基指数有标准交易所代码，行业/概念指数无标准代码只能用名称标识；数量级差异大（8 vs 850+）；混入会污染 `index_code` 语义且宽基查询必须带过滤条件（ADR-0001）
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
- **板块指数标识**: `sector_index_daily` 使用 `sector_name` 作为主要标识，无需生成伪代码

### API层

- **指数日线查询**: `GET /api/v1/admin/index-daily?indexCode=sh000001&startDate=...&endDate=...`
- **板块指数日线查询**: `GET /api/v1/admin/sector-index-daily?sectorType=INDUSTRY&sectorName=银行&startDate=...&endDate=...`
- **指数列表查询**: `GET /api/v1/admin/index-list` 返回所有已采集宽基指数的最新行情
- **板块两融聚合查询**: `GET /api/v1/admin/sector-margin?sectorType=INDUSTRY&sectorName=银行&startDate=...&endDate=...` 实时聚合 margin_daily + stock_industry
- **行业/概念详情**: `GET /api/v1/admin/industries/{name}` 和 `GET /api/v1/admin/concepts/{name}` 扩展返回板块指数日线+板块两融数据

### 前端

- **指数分析页**: `/admin/index-analysis`，展示5-8个核心指数的K线+两融叠加图
- **行业详情页**: `/admin/industries/[name]`，展示行业指数K线+板块两融叠加图+成分股列表
- **概念详情页**: `/admin/concepts/[name]`，展示概念指数K线+板块两融叠加图+成分股列表
- **图表模式**: 统一双轴叠加 — K线(左轴) + 成交量(底部) + 融资余额(右轴,绝对值) + 融券余额(右轴,绝对值)，与个股详情页一致
- **图表组件**: 复用 `lightweight-charts`，提取可复用的 KlineMarginOverlay 组件，接收统一的OHLCV数据格式，不关心数据来自 `index_daily` 还是 `sector_index_daily`

### 板块两融聚合

- **聚合方式**: JOIN `margin_daily` + `stock_industry`/`stock_concept`，按 `trade_date` + `sector_name` GROUP BY SUM
- **归属快照**: 使用当前 `stock_industry`/`stock_concept` 的归属关系，不追踪历史变更
- **性能**: 5000股 × 100天 ≈ 50万行，MySQL聚合毫秒级，无需物化

## 测试决策

- **测试标准**: 只测试外部行为（API返回的数据是否正确），不测试内部实现（SQL怎么写、聚合怎么算）
- **需测试的模块**:
  - 指数日线采集+清洗: 验证OHLCV数据入库、change_pct计算正确
  - 板块指数日线采集+清洗: 验证行业/概念指数数据入库、sector_type区分正确
  - 板块两融聚合查询: 验证聚合结果与手动SUM一致
  - 指数日线API: 验证日期范围过滤正确
  - 板块指数日线API: 验证按sectorType和sectorName过滤正确
  - 行业/概念详情API: 验证返回的板块指数日线和板块两融数据匹配
- **测试先例**: 参考现有 `MarginCleanseServiceImpl` 的测试模式（验证清洗后字段计算正确）

## 范围外

- **全市场日交易量**: 不建 `market_overview_daily` 表，当前目标不需要
- **行业归属时序追踪**: `stock_industry` 保持快照型，不加 `effective_date`
- **板块两融物化表**: 不建 `sector_margin_daily`，实时聚合
- **归一化涨跌幅图表**: 不实现，统一使用双轴叠加
- **龙虎榜/深港通等新增数据**: 属于后续迭代，不在本次范围
- **SSE/SZSE官方数据源替代**: 属于数据源优化，不在本次范围
- **申万行业指数**: 可作为后续补充，本次只做同花顺行业/概念指数

## 补充说明

- 相关ADR: [ADR-0001](../docs/adr/0001-unified-index-daily-table.md) 指数日线分表存储, [ADR-0002](../docs/adr/0002-sector-margin-realtime-aggregation.md) 板块两融实时聚合
- 相关文档: [exchange-data-source-analysis.md](../docs/exchange-data-source-analysis.md) 沪深交易所数据源分析
- 行业指数约446个，概念指数约400+个，全量采集可能耗时较长。建议行业/概念指数采集支持增量模式（只采集有新交易日的数据）
