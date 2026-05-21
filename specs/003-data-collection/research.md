# 研究文档

## 1. AKTools 指数日线 API

**Decision**: 使用 `/api/public/stock_zh_index_daily?symbol=sh000001` 采集上证指数
**Rationale**: AKShare 的 `stock_zh_index_daily` 返回上证/深证等指数的 OHLCV 数据，AKTools HTTP 映射为同路径
**Alternatives**: `index_zh_a_hist`（功能类似但 API 路径不同）
**Symbol 格式**: `sh000001`（上证）, `sz399001`（深证成指）

## 2. 融资变化字段计算

**Decision**: CLEANSE 阶段计算并写入 `margin_daily.margin_change` / `margin_daily.short_change`
**Rationale**: 数据入库后不变，写入时一次性计算比每次查询时 LAG() 更高效
**Implementation**: 查上一交易日 margin_daily 数据，`marginChange = 当日.balance - 上日.balance`
**Edge case**: 新股/新增两融标的无上一交易日数据 → change = null

## 3. 涨跌幅来源

**Decision**: 直接从 `stock_info.change_pct` 读取，不重复计算
**Rationale**: `stock_info` 表已存储腾讯 API 返回的涨跌幅，无需从 stock_daily 计算

## 4. MySQL 环比查询

**Decision**: 融资环比在 CLEANSE 时写入，聚合查询直接 SUM margin_change
**Rationale**: 预计算消除查询时窗口函数开销。概念/行业聚合只需 GROUP BY + SUM

## 5. lightweight-charts 多系列叠加

**Decision**: 使用 `CandlestickSeries` (K线) + `HistogramSeries` (成交量，半透明) + 2 条 `LineSeries` (融资余额/融券余额)，双 `PriceScale`
**Rationale**: lightweight-charts 3.8+ 原生支持多系列单图叠加，无需拆分面板
**API 参考**: `chart.addCandlestickSeries()` → `chart.addHistogramSeries({ priceScaleId: 'left' })` → `chart.addLineSeries({ priceScaleId: 'right' })`

## 6. 交易日历数据源

**Decision**: `trade_calendar` 表提供交易日列表，`stock_daily` 表 DISTINCT trade_date 提供有数据的日期。两者比对得缺失日期
**Rationale**: 两个表已在当前系统中，无需新数据源

## 7. IndexDaily 实体字段

**Decision**: 字段与 stock_daily 一致（OHLCV + amount + change_pct），用 `index_code` 替代 `stock_code`
**Rationale**: 指数和个股数据结构相同，但分表存储避免混淆
