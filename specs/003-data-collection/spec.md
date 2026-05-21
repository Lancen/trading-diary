# 数据采集页面 UI/UE 重构设计

## 概述

将原单一采集状态页面拆分为 Hub + 子页面架构，新增数据浏览分组。股票行情（含日线）和成分股管理从主页面分离为独立子页面，通过 Hub 页卡片跳转。

## 导航结构

侧边栏改为两个分组：

```
数据采集
├ 数据采集 (/admin/collection)
└ 两融完整性 (/admin/collection/margin)

数据浏览
├ 股票数据 (/admin/stocks)
├ 概念列表 (/admin/concepts)
├ 行业列表 (/admin/industries)
└ 融资统计 (/admin/margin-stats)
```

## 页面清单（12页）

### 1. 数据采集 Hub — `/admin/collection`

- 从原页面改造，9 张卡片（8 个采集类型 + 成分股数据），3 列统一网格布局
- 每卡片：名称 + 状态标签（正常/异常/采集中）+ 最新采集/清洗时间
- 全部操作按钮移除，仅保留"刷新"
- 点击"股票行情"卡片 → `/admin/collection/stocks`
- 点击"成分股数据"卡片 → `/admin/collection/constituents`

### 2. 股票行情详情 — `/admin/collection/stocks`

- 统一管线状态卡片：`采集 → 清洗` 两步，合并 STOCK_INFO + STOCK_DAILY 展示（清洗即入库，非独立步骤）
- 交易日历：月视图，绿底 ✓（已采集）、红底 ✗（交易日缺数据）、灰底（非交易日），默认当月，可翻月。点击红点日期可触发补采
- 操作："触发采集"（触发 STOCK_INFO 管线）、"历史补采"（仅 STOCK_DAILY，弹窗选日期范围）
- 采集日志表：日期、阶段（采集/清洗）、状态、地址、参数、记录数、备注。失败行红色高亮，备注显示错误详情。最近 20 条

### 3. 成分股管理 — `/admin/collection/constituents`

- 表格布局（非卡片）：文件名、采集日期、行业数、概念数、关系总数、状态、操作
- 状态判断：文件 `fetchedDate` 匹配 `stock_industry.snap_date` → 已导入，否则 → 待导入
- 已导入文件按钮置灰，待导入文件可点击"导入"
- 导入走 upsert 幂等逻辑

### 4. 股票数据 — `/admin/stocks`

- 筛选：股票代码/名称输入 + 行业下拉 + 概念下拉 + 日期（留空=最新）
- 列：代码、名称、行业、概念、收盘、涨跌幅、成交量、融资余额、融资变化、融券余额、融券变化、日期
- 涨跌幅、成交量、融资余额、融资变化、融券余额、融券变化 → 表头可排序（升/降切切换）
- 点击代码/名称 → `/admin/stocks/{code}`
- 行业/概念筛选可从概念列表页、行业列表页跳转带参数（`?industry=银行`、`?concept=MSCI`）

### 5. 股票详情 — `/admin/stocks/{code}`

- 所属行业（标签） + 所属概念（多标签）
- 行情摘要卡片：收盘/开盘/最高/最低/成交量/涨跌幅
- K线图（单图叠加）：
  - 日K/周K/月K 切换，快捷区间（1月/3月/6月/1年）+ 自定义日期范围，默认最近3个月日K
  - 叠加：成交量（半透明柱）+ 融资余额线 + 融券余额线 + 两融总余额线（虚线）
  - 双Y轴：左轴=价格，右轴=金额
  - 十字光标 hover 联动
- 日线明细表（默认收起可展开）：日期、OHLCV、涨跌幅
- 数据来源：`stock_daily` + `margin_daily` + `stock_industry` + `stock_concept`

### 6. 概念列表 — `/admin/concepts`

- 名称搜索
- 列：编码、名称、股票数、融资余额、融资变化、融券余额、融券变化、日期
- 数据为概念下所有成分股两融汇总
- 所有数值列可排序
- 点击股票数 → `/admin/stocks?concept=XXX`
- 点击两融数据 → `/admin/margin-stats/concept/XXX`
- 名称不可点击（仅展示）

### 7. 行业列表 — `/admin/industries`

- 结构与概念列表完全一致
- 点击股票数 → `/admin/stocks?industry=XXX`
- 点击两融数据 → `/admin/margin-stats/industry/XXX`

### 8. 融资统计 — `/admin/margin-stats`

- 筛选：日期 + 交易所
- 3 张总量卡片（融资余额/融券余额/两融总余额），可点击 → `/admin/margin-stats/market`
- Tab 切换：行业融资 / 概念融资
- 列表列：名称、融资余额、融资变化、融券余额、融券变化、股票数，均支持排序
- 点击行业/概念名 → 对应详情页

### 9. 大盘融资历史 — `/admin/margin-stats/market`

- 融资趋势图：融资余额 + 融券余额 + 总余额，叠加上证指数/深证成指（右轴）
- 快捷区间（周/月/3月/年）+ 自定义日期范围，默认最近3个月
- 历史表：日期、融资余额、融资变化、融券余额、融券变化、总余额

### 10. 行业融资详情 — `/admin/margin-stats/industry/{name}`

- 图表：该行业融资+融券趋势（暂不叠加行业指数，后续补充）
- 历史表：同大盘结构，数据范围为该行业
- 快捷区间同上

### 11. 概念融资详情 — `/admin/margin-stats/concept/{name}`

- 图表：该概念融资+融券趋势
- 历史表：同大盘结构，数据范围为该概念

### 12. 两融完整性 — `/admin/collection/margin`

- 现有页面，不做改动

## 后端变更

### 新增数据采集类型：指数日线

```java
INDEX_DAILY: 指数日线数据
```

覆盖：上证指数 (000001.SH)、深证成指 (399001.SZ)

采集 API：复用 `stock_zh_a_hist_tx`，symbol 使用指数代码。行业指数后续补充。

### 成分股导入状态查询

`ConstituentImportService.listFiles()` 增加 DB 查询：
- 对每个文件，查 `stock_industry` 表 `snap_date = fetchedDate` 是否有记录
- 返回字段增加 `imported: boolean`

### 融资聚合查询

各页面需要的新增聚合查询（按行业/概念 GROUP BY + SUM margin_daily 数据 + 环比）。

### 采集日志增加字段

`DataCollectionLog` 实体增加：`requestUrl`（地址）、`requestParams`（参数）、`remark`（备注）

## 设计决策

1. **实现顺序**：后端 API → 前端页面
2. **日志字段**：所有外部接口调用均记录 `requestUrl`、`requestParams`；`remark` 和 `errorMsg` 分开（前者描述性备注，后者异常详情）
3. **指数存储**：新建 `index_daily` 表，不复用 `stock_daily`
4. **路由编码**：行业/概念详情 URL 用编码（如 `/admin/margin-stats/concept/GN001`），不用中文名
5. **概念融资聚合**：接受双重计数（一只股票可属多个概念，互斥分类允许重复计入）
6. **交易日历逻辑**：`stock_daily` 当月 DISTINCT trade_date 与 `trade_calendar` 比对，三状态：已采集/缺失/非交易日
7. **周K/月K 融资值**：取周期最后一个交易日的余额值
8. **环比基准**：较上一个交易日（非字面"昨日"）
9. **融资变化预计算**：CLEANSE 阶段计算 margin_change/short_change 写入 margin_daily，不在查询时实时计算
10. **K线两融叠加**：无两融数据的天断开（null=不绘制），不显示为 0
11. **涨跌幅**：复用 `stock_info.change_pct`（已预计算，不重复算）
12. **侧边栏**：分组改名"基础数据管理"，条目名"数据采集"；子路由高亮用 startsWith 排除 margin
13. **Controller 拆分**：CollectionController（不变）+ StockDataController + MarginStatsController + MarketDataController
14. **Flyway**：V5 统一迁移（日志加字段 + margin_daily 加字段 + index_daily 建表）
15. **分期实施**：
   - 一期：后端 API + Hub 改造 + 股票行情详情 + 股票数据/详情 + 成分股管理
   - 二期：概念列表 + 行业列表 + 融资统计 + 详情页 + INDEX_DAILY

## 前端技术要点

- 图表：lightweight-charts（项目已有）
- K线图 + 两融线叠加 → 使用 `HistogramSeries` + `LineSeries`，双 `PriceScale`
- Tab 切换：`useState` 管理当前 tab，条件渲染表格
- 排序：点击表头切换 asc/desc，通过 `searchParams` 或 `useState` 管理

## 路由汇总

| 路由 | 页面 | 入口 |
|------|------|------|
| `/admin/collection` | 数据采集 Hub | 侧边栏 |
| `/admin/collection/stocks` | 股票行情详情 | Hub 卡片 |
| `/admin/collection/constituents` | 成分股管理 | Hub 卡片 |
| `/admin/collection/margin` | 两融完整性 | 侧边栏（不变） |
| `/admin/stocks` | 股票数据 | 侧边栏 |
| `/admin/stocks/{code}` | 股票详情 | 股票列表点击 |
| `/admin/concepts` | 概念列表 | 侧边栏 |
| `/admin/industries` | 行业列表 | 侧边栏 |
| `/admin/margin-stats` | 融资统计 | 侧边栏 |
| `/admin/margin-stats/market` | 大盘融资历史 | 总量卡片点击 |
| `/admin/margin-stats/industry/{code}` | 行业融资详情 | 行业名点击 |
| `/admin/margin-stats/concept/{code}` | 概念融资详情 | 概念名点击 |
