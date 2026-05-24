# 沪深交易所官方数据源分析报告

> 分析日期: 2026-05-24
> 项目: trading-diary
> 目的: 评估上交所(SSE)和深交所(SZSE)官方数据对当前项目数据源的替代可行性，规划后续迭代数据需求

---

## 目录

1. [当前项目数据架构](#1-当前项目数据架构)
2. [上交所(SSE)可获取数据详析](#2-上交所sse可获取数据详析)
3. [深交所(SZSE)可获取数据详析](#3-深交所szse可获取数据详析)
4. [数据替代分析](#4-数据替代分析)
5. [后续迭代数据需求](#5-后续迭代数据需求)
6. [接口参考手册](#6-接口参考手册)
7. [实施路线图](#7-实施路线图)
8. [风险与注意事项](#8-风险与注意事项)

---

## 1. 当前项目数据架构

### 1.1 数据源总览

| 数据源 | 类型 | 基地址 | 用途 | 稳定性风险 |
|--------|------|--------|------|-----------|
| **Tushare Pro** | 付费API | `https://api.tushare.pro` | 全市场日线行情(OHLCV)，按日期批量拉取 | 需token+积分，有频率限制 |
| **AKTools (AKShare)** | 自部署API | `http://localhost:8081` | 实时行情、行业/概念板块、融资融券、交易日历 | 依赖东方财富/新浪/腾讯，部分接口已封 |
| **Playwright + 同花顺** | 爬虫 | — | 行业/概念成分股 | 反爬风险高，速度慢 |

### 1.2 AKTools 当前接口清单

| 方法 | AKTools路径 | 底层数据源 | 状态 | 对应项目表 |
|------|------------|-----------|------|-----------|
| `fetchStockSpot()` | `/api/public/stock_zh_a_spot` | 新浪 | ✅ 可用 | `stock_info` |
| `fetchStockDaily()` | `/api/public/stock_zh_a_hist_tx` | 腾讯 | ✅ 可用 | `stock_daily` |
| `fetchIndustryNames()` | `/api/public/stock_board_industry_name_ths` | 同花顺 | ✅ 可用 | `industry` |
| `fetchConceptNames()` | `/api/public/stock_board_concept_name_ths` | 同花顺 | ✅ 可用 | `concept` |
| `fetchIndustryCons()` | `/api/public/stock_board_industry_cons_em` | 东方财富 | ❌ 已封 | — (改用Playwright) |
| `fetchConceptCons()` | `/api/public/stock_board_concept_cons_em` | 东方财富 | ❌ 已封 | — (改用Playwright) |
| `fetchMacroMarginSh()` | `/api/public/macro_china_market_margin_sh` | — | ✅ 可用 | `margin_macro` |
| `fetchMacroMarginSz()` | `/api/public/macro_china_market_margin_sz` | — | ✅ 可用 | `margin_macro` |
| `fetchTradeCalendar()` | `/api/public/tool_trade_date_hist_sina` | 新浪 | ✅ 可用 | `trade_calendar` |
| `fetchMarginDetailSse()` | `/api/public/stock_margin_detail_sse` | — | ✅ 可用 | `margin_daily` |
| `fetchMarginDetailSzse()` | `/api/public/stock_margin_detail_szse` | — | ✅ 可用 | `margin_daily` |

### 1.3 Tushare 当前接口

| 方法 | API名称 | 参数 | 返回字段 | 对应项目表 |
|------|---------|------|---------|-----------|
| `fetchDaily()` | `daily` | `trade_date` | `ts_code,trade_date,open,high,low,close,pre_close,change,pct_chg,vol,amount` | `stock_daily` |

### 1.4 核心数据模型（12张表）

```
┌──────────────────────────────────────────────────────────────────┐
│                        数据采集层                                 │
├──────────────┬──────────────┬──────────────┬────────────────────┤
│  stock_info  │  stock_daily │  index_daily │   trade_calendar   │
│  股票快照     │  日线OHLCV   │  指数日线     │   交易日历          │
├──────────────┼──────────────┴──────────────┼────────────────────┤
│ margin_daily │         margin_macro        │   margin_stock     │
│ 两融个股明细  │       两融宏观数据           │  两融标的(已删除)   │
├──────────────┼──────────────┬──────────────┴────────────────────┤
│  industry    │ stock_industry│  concept / stock_concept         │
│  行业字典     │  行业关联     │  概念字典 / 概念关联              │
├──────────────┼──────────────┴──────────────────────────────────┤
│ raw_data     │  classification_change_log / data_collection_log │
│ 原始JSON     │  分类变更日志 / 采集日志                          │
└──────────────┴─────────────────────────────────────────────────┘
```

### 1.5 各表字段详情

#### stock_info (股票基本信息快照)

| 字段 | 类型 | 说明 | 当前数据源 |
|------|------|------|-----------|
| code | VARCHAR(10) | 股票代码 | AKTools `stock_zh_a_spot` |
| name | VARCHAR(20) | 股票名称 | AKTools `stock_zh_a_spot` |
| latest_price | DECIMAL(10,3) | 最新价 | AKTools `stock_zh_a_spot` |
| change_pct | DECIMAL(6,2) | 涨跌幅(%) | AKTools `stock_zh_a_spot` |
| change_amount | DECIMAL(8,3) | 涨跌额 | AKTools `stock_zh_a_spot` |
| volume | BIGINT | 成交量 | AKTools `stock_zh_a_spot` |
| amount | DECIMAL(16,2) | 成交额 | AKTools `stock_zh_a_spot` |
| turnover_rate | DECIMAL(6,2) | 换手率(%) | AKTools `stock_zh_a_spot` |
| volume_ratio | DECIMAL(6,2) | 量比 | AKTools `stock_zh_a_spot` |
| pe | DECIMAL(10,3) | 市盈率 | AKTools `stock_zh_a_spot` |
| pb | DECIMAL(10,3) | 市净率 | AKTools `stock_zh_a_spot` |
| total_mv | DECIMAL(14,2) | 总市值 | AKTools `stock_zh_a_spot` |
| float_mv | DECIMAL(14,2) | 流通市值 | AKTools `stock_zh_a_spot` |
| snapshot_date | DATE | 快照日期 | 系统 |

#### stock_daily (股票日线数据)

| 字段 | 类型 | 说明 | 当前数据源 |
|------|------|------|-----------|
| stock_code | VARCHAR(10) | 股票代码 | Tushare `daily` |
| trade_date | DATE | 交易日期 | Tushare `daily` |
| open | DECIMAL(10,3) | 开盘价 | Tushare `daily` |
| high | DECIMAL(10,3) | 最高价 | Tushare `daily` |
| low | DECIMAL(10,3) | 最低价 | Tushare `daily` |
| close | DECIMAL(10,3) | 收盘价 | Tushare `daily` |
| volume | BIGINT | 成交量 | Tushare `daily` |
| amount | DECIMAL(16,2) | 成交额 | Tushare `daily` |

#### index_daily (指数日线数据)

| 字段 | 类型 | 说明 | 当前数据源 |
|------|------|------|-----------|
| index_code | VARCHAR(10) | 指数代码(sh000001/sz399001) | AKTools `stock_zh_index_daily` |
| trade_date | DATE | 交易日期 | AKTools |
| open | DECIMAL(10,3) | 开盘价 | AKTools |
| high | DECIMAL(10,3) | 最高价 | AKTools |
| low | DECIMAL(10,3) | 最低价 | AKTools |
| close | DECIMAL(10,3) | 收盘价 | AKTools |
| volume | BIGINT | 成交量 | AKTools |
| amount | DECIMAL(16,2) | 成交额 | AKTools |
| change_pct | DECIMAL(6,2) | 涨跌幅(%) | AKTools |

#### margin_daily (融资融券每日明细)

| 字段 | 类型 | 说明 | 当前数据源 |
|------|------|------|-----------|
| stock_code | VARCHAR(10) | 股票代码 | AKTools `stock_margin_detail_sse/szse` |
| trade_date | DATE | 交易日期 | AKTools |
| exchange | VARCHAR(4) | SSE/SZSE | AKTools |
| margin_balance | DECIMAL(16,2) | 融资余额 | AKTools |
| margin_buy | DECIMAL(16,2) | 融资买入额 | AKTools |
| margin_repay | DECIMAL(16,2) | 融资偿还额 | AKTools |
| short_balance | DECIMAL(16,2) | 融券余额 | AKTools |
| short_sell_vol | BIGINT | 融券卖出量 | AKTools |
| short_repay_vol | BIGINT | 融券偿还量 | AKTools |
| short_remain_vol | BIGINT | 融券余量 | AKTools |
| total_balance | DECIMAL(16,2) | 两融总余额 | AKTools |
| margin_change | DECIMAL(16,2) | 融资余额环比变化 | CLEANSE阶段计算 |
| short_change | DECIMAL(16,2) | 融券余额环比变化 | CLEANSE阶段计算 |

#### margin_macro (融资融券宏观数据)

| 字段 | 类型 | 说明 | 当前数据源 |
|------|------|------|-----------|
| trade_date | DATE | 交易日期 | AKTools `macro_china_market_margin_sh/sz` |
| exchange | VARCHAR(4) | SSE/SZSE | AKTools |
| margin_buy | DECIMAL(24,2) | 融资买入额(元) | AKTools |
| margin_balance | DECIMAL(24,2) | 融资余额(元) | AKTools |
| short_sell_vol | BIGINT | 融券卖出量(股) | AKTools |
| short_remain_vol | BIGINT | 融券余量(股) | AKTools |
| short_balance | DECIMAL(24,2) | 融券余额(元) | AKTools |
| total_balance | DECIMAL(24,2) | 融资融券余额(元) | AKTools |

---

## 2. 上交所(SSE)可获取数据详析

### 2.1 数据访问方式概述

上交所**不提供开放API**，数据通过页面内嵌的JS异步请求获取JSON，核心特点：

- 所有数据接口域名: `query.sse.com.cn`
- **必须携带** `Referer: https://www.sse.com.cn/` 请求头，否则返回403
- 部分接口使用JSONP回调（`jsonCallBack=jsonpCallback`）
- 分页参数: `pageHelp.pageSize` / `pageHelp.pageNo` / `pageHelp.beginPage`
- 无官方API文档，接口参数可能随网站改版变化

### 2.2 数据清单

#### 2.2.1 每日成交概况

| 属性 | 值 |
|------|-----|
| 页面 | https://www.sse.com.cn/market/stockdata/overview/day/ |
| 接口 | `GET https://query.sse.com.cn/commonQuery.do` |
| 更新频率 | 每日 |
| 格式 | JSON |

**请求参数**:

| 参数 | 值 | 说明 |
|------|-----|------|
| jsonCallBack | jsonpCallback | JSONP回调 |
| isPagination | true | 是否分页 |
| sqlId | SSE_ZGPZ_YJLJLJXJ_L | 查询ID |
| tradeDate | 20260522 | 交易日期(YYYYMMDD) |
| pageHelp.pageSize | 10000 | 每页条数 |

**返回字段**:

| 字段 | 说明 | 对应项目表字段 |
|------|------|--------------|
| tradeDate | 交易日期 | — |
| listedNum | 挂牌数 | — |
| marketValue | 市价总值(亿元) | stock_info.total_mv (汇总) |
| negotiableValue | 流通市值(亿元) | stock_info.float_mv (汇总) |
| turnover | 成交金额(亿元) | stock_info.amount (汇总) |
| volume | 成交量(亿股) | stock_info.volume (汇总) |
| pe | 平均市盈率(倍) | stock_info.pe (汇总) |
| turnoverRate | 换手率(%) | stock_info.turnover_rate (汇总) |
| negotiableTurnoverRate | 流通换手率(%) | — |

**数据分类**: 按主板A / 主板B / 科创板 / 股票回购分别统计

**历史数据**: 2021-12-24之前的数据在 `index_his.shtml` 静态页面

**替代价值**: ⭐⭐ — 仅市场汇总级数据，无法替代个股级stock_info，但可用于市场总貌校验

---

#### 2.2.2 交易公开信息（龙虎榜）

| 属性 | 值 |
|------|-----|
| 页面 | https://www.sse.com.cn/disclosure/diclosure/public/dailydata/ |
| 接口 | `GET https://query.sse.com.cn/marketdata/publicQuery.do` |
| 更新频率 | 每日 |
| 格式 | JSON |

**请求参数**:

| 参数 | 值 | 说明 |
|------|-----|------|
| jsonCallBack | jsonpCallback | JSONP回调 |
| isPagination | true | 是否分页 |
| sqlId | SSE_DAILYTRADE_INFO_PLCC | 龙虎榜查询ID |
| tradeDate | 20260522 | 交易日期 |

**返回数据分类**:

| 类别 | sqlId后缀 | 说明 |
|------|----------|------|
| 涨幅偏离值≥7% | refType=11 | 日收盘价格涨幅偏离值达到7%的前五只证券 |
| 跌幅偏离值≥7% | refType=12 | 日收盘价格跌幅偏离值达到7%的前五只证券 |
| 振幅≥15% | refType=13 | 日价格振幅达到15%的前五只证券 |
| 换手率≥20% | refType=14 | 日换手率达到20%的前五只证券 |
| 无涨跌幅限制首日 | refType=15 | 首个交易日无价格涨跌幅限制的证券 |
| 连续3日涨幅偏离≥20% | refType=16 | 连续三个交易日内收盘价格涨幅偏离值累计达到20% |

**返回字段**:

| 字段 | 说明 |
|------|------|
| secCode | 证券代码 |
| secAbbr | 证券简称 |
| secType | 证券种类(A/B股) |
| deviation | 偏离值% |
| volume | 成交量(万股/万份) |
| amount | 成交金额(万元) |
| buyDeptName | 买入营业部名称 (TOP5) |
| buyAmt | 累计买入金额(万元) |
| sellDeptName | 卖出营业部名称 (TOP5) |
| sellAmt | 累计卖出金额(万元) |

**替代价值**: ⭐⭐⭐⭐⭐ — 全新数据，仅官方提供，短线交易核心参考

---

#### 2.2.3 融资融券数据

| 属性 | 值 |
|------|-----|
| 页面 | https://www.sse.com.cn/services/trading/margin/ |
| 接口 | `GET https://query.sse.com.cn/marketdata/margin/tradeMarginDetailQuery.do` |
| 更新频率 | 每日 |
| 格式 | JSON |

**请求参数**:

| 参数 | 值 | 说明 |
|------|-----|------|
| jsonCallBack | jsonpCallback | JSONP回调 |
| isPagination | true | 是否分页 |
| begin | 0 | 起始位置 |
| pageSize | 5000 | 每页条数 |
| tradeDate | 20260522 | 交易日期 |

**个股明细返回字段**:

| 字段 | 说明 | 对应项目表字段 |
|------|------|--------------|
| stockCode | 证券代码 | margin_daily.stock_code |
| stockName | 证券简称 | — |
| rzye | 融资余额(元) | margin_daily.margin_balance |
| rzmre | 融资买入额(元) | margin_daily.margin_buy |
| rzche | 融资偿还额(元) | margin_daily.margin_repay |
| rqye | 融券余额(元) | margin_daily.short_balance |
| rqmcl | 融券卖出量(股) | margin_daily.short_sell_vol |
| rqchl | 融券偿还量(股) | margin_daily.short_repay_vol |
| rqyl | 融券余量(股) | margin_daily.short_remain_vol |
| rzye | 融资融券余额(元) | margin_daily.total_balance |

**宏观汇总返回字段**:

| 字段 | 说明 | 对应项目表字段 |
|------|------|--------------|
| rzye | 融资余额(元) | margin_macro.margin_balance |
| rzmre | 融资买入额(元) | margin_macro.margin_buy |
| rqye | 融券余额(元) | margin_macro.short_balance |
| rqmcl | 融券卖出量(股) | margin_macro.short_sell_vol |
| rqyl | 融券余量(股) | margin_macro.short_remain_vol |
| rzye | 融资融券余额(元) | margin_macro.total_balance |

**替代价值**: ⭐⭐⭐⭐⭐ — 可完全替代AKTools `stock_margin_detail_sse` 和 `macro_china_market_margin_sh`

---

#### 2.2.4 融资融券标的调整公告

| 属性 | 值 |
|------|-----|
| 页面 | https://www.sse.com.cn/disclosure/magin/announcement/ |
| 接口 | HTML页面，需解析 |
| 更新频率 | 不定期(标的调整时发布) |
| 格式 | HTML |

**公告内容**: 标的证券新增/移除列表，含证券代码、简称、调整类型

**替代价值**: ⭐⭐⭐ — 可自动跟踪两融标的变更，替代手动维护

---

#### 2.2.5 上市公司列表

| 属性 | 值 |
|------|-----|
| 页面 | https://www.sse.com.cn/assortment/stock/list/name/ |
| 接口 | `GET https://query.sse.com.cn/commonQuery.do` |
| 更新频率 | 实时 |
| 格式 | JSON |

**返回字段**:

| 字段 | 说明 | 对应项目表字段 |
|------|------|--------------|
| secCode | 证券代码 | stock_info.code |
| secAbbr | 证券简称 | stock_info.name |
| listDate | 上市日期 | — |
| boardType | 板块类型(主板/科创板) | — |

**替代价值**: ⭐⭐ — 仅含代码/名称/上市日期，缺少PE/PB/市值等行情字段

---

#### 2.2.6 市场数据总貌

| 属性 | 值 |
|------|-----|
| 页面 | https://www.sse.com.cn/market/stockdata/statistic/ |
| 接口 | `GET https://query.sse.com.cn/commonQuery.do` |
| 更新频率 | 每日 |
| 格式 | JSON |

**返回字段**: 总貌/主板/科创板的挂牌数、市价总值、流通市值、成交金额、平均市盈率等

**替代价值**: ⭐⭐ — 市场级汇总，可用于大盘健康度指标

---

#### 2.2.7 主要指数行情

| 属性 | 值 |
|------|-----|
| 页面 | SSE首页 |
| 接口 | `GET https://query.sse.com.cn/commonQuery.do` |
| 更新频率 | 实时 |
| 格式 | JSON |

**覆盖指数**: 上证指数、科创综指、上证收益、上证50、科创50、上证180、沪深300

**替代价值**: ⭐⭐⭐ — 可部分替代AKTools指数数据，但仅限沪市指数

---

## 3. 深交所(SZSE)可获取数据详析

### 3.1 数据访问方式概述

深交所提供**标准REST API**，是最友好的官方数据源，核心特点：

- 统一接口: `GET https://www.szse.cn/api/report/ShowReport`
- 支持 **JSON** 和 **Excel(xlsx)** 两种格式
- 参数规范、清晰，通过 `CATALOGID` 区分数据类型
- 通过 `TABKEY` 选择同一页面下的不同Tab
- 无需特殊请求头（标准HTTP GET即可）
- 页面右上角提供Excel下载按钮，底层调用同一API

### 3.2 通用请求参数

| 参数 | 说明 | 示例 |
|------|------|------|
| SHOWTYPE | 返回格式 | `JSON` 或 `xlsx` |
| CATALOGID | 数据目录ID | `1803_after` |
| TABKEY | Tab键 | `tab1` / `tab2` |
| txtQueryDate | 查询日期 | `2025-04-21` |
| random | 随机数(防缓存) | `0.1151430633896432` |

### 3.3 数据清单

#### 3.3.1 股票基本概况

| 属性 | 值 |
|------|-----|
| 页面 | https://www.szse.cn/market/stock/indicator/index.html |
| CATALOGID | `1803_after` |
| TABKEY | `tab1` |
| 更新频率 | 每日 |
| 格式 | JSON / Excel |

**请求示例**:
```
GET https://www.szse.cn/api/report/ShowReport?SHOWTYPE=JSON&CATALOGID=1803_after&TABKEY=tab1&txtQueryDate=2025-04-21
```

**Excel下载**:
```
GET https://www.szse.cn/api/report/ShowReport?SHOWTYPE=xlsx&CATALOGID=1803_after&TABKEY=tab1&txtQueryDate=2025-04-21
```

**返回字段**:

| 字段 | 说明 | 对应项目表字段 |
|------|------|--------------|
| agdm | 公司代码 | stock_info.code |
| gsmc | 公司简称 | stock_info.name |
| ssrq | 上市日期 | — |
| zgb | 总股本(股) | — |
| ltgb | 流通股(股) | — |
| gszs | 总市值(元) | stock_info.total_mv |
| ltsz | 流通市值(元) | stock_info.float_mv |
| syl | 市盈率 | stock_info.pe |
| sjl | 市净率 | stock_info.pb |

**替代价值**: ⭐⭐⭐⭐ — 可替代深市stock_info中的PE/PB/市值字段，但缺少实时行情(价格/涨跌/换手率)

---

#### 3.3.2 股票日度成交概况

| 属性 | 值 |
|------|-----|
| 页面 | https://www.szse.cn/market/stock/situation/daily/index.html |
| 更新频率 | 每日 |
| 格式 | JSON / Excel |

**返回字段**: 日期、成交金额、成交量、成交笔数、平均市盈率等（市场汇总级）

**替代价值**: ⭐⭐ — 仅市场汇总，无法替代个股级stock_daily

---

#### 3.3.3 股票周度/月度/年度成交概况

| 属性 | 值 |
|------|-----|
| 页面 | https://www.szse.cn/market/stock/situation/weekly/index.html |
| 页面 | https://www.szse.cn/market/stock/situation/monthly/index.html |
| 页面 | https://www.szse.cn/market/stock/situation/annual/index.html |
| 格式 | JSON / Excel |

**替代价值**: ⭐⭐ — 市场汇总统计，可用于大盘分析

---

#### 3.3.4 市场总貌

| 属性 | 值 |
|------|-----|
| 页面 | https://www.szse.cn/market/overview/index.html |
| 更新频率 | 每日 |
| 格式 | JSON / Excel |

**返回字段**: 上市公司数、总市值、流通市值、成交金额、平均市盈率等

**替代价值**: ⭐⭐⭐ — 大盘健康度指标

---

#### 3.3.5 指数总览

| 属性 | 值 |
|------|-----|
| 页面 | https://www.szse.cn/market/exponent/pandect/index.html |
| 更新频率 | 每日 |
| 格式 | JSON |

**覆盖指数**: 深证成指、创业板指、深证100、深证综指等深市核心指数

**返回字段**: 指数代码、名称、最新价、涨跌幅、成交量、成交额

**替代价值**: ⭐⭐⭐ — 可替代深市指数日线数据(AKTools `stock_zh_index_daily`)

---

#### 3.3.6 指数与样本股

| 属性 | 值 |
|------|-----|
| 页面 | https://www.szse.cn/market/exponent/sample/index.html |
| 更新频率 | 定期调整(通常每半年) |
| 格式 | JSON / Excel |

**返回字段**: 指数代码、指数名称、样本股代码、样本股名称

**替代价值**: ⭐⭐⭐⭐ — 指数成分股追踪，调仓预警

---

#### 3.3.7 股票列表

| 属性 | 值 |
|------|-----|
| 页面 | https://www.szse.cn/market/product/stock/list/index.html |
| 更新频率 | 实时 |
| 格式 | JSON / Excel |

**返回字段**: 证券代码、证券简称、上市日期、板块类型

**替代价值**: ⭐⭐⭐ — 深市股票基础列表

---

#### 3.3.8 融资融券交易

| 属性 | 值 |
|------|-----|
| 页面 | https://www.szse.cn/disclosure/margin/margin/index.html |
| 更新频率 | 每日 |
| 格式 | JSON / Excel |

**宏观汇总字段**:

| 字段 | 说明 | 对应项目表字段 |
|------|------|--------------|
| rzrqye | 融资买入额(亿元) | margin_macro.margin_buy |
| rzye | 融资余额(亿元) | margin_macro.margin_balance |
| rqmcl | 融券卖出量(亿股) | margin_macro.short_sell_vol |
| rqyl | 融券余量(亿股) | margin_macro.short_remain_vol |
| rqye | 融券余额(亿元) | margin_macro.short_balance |
| rzye | 融资融券余额(亿元) | margin_macro.total_balance |

**个股明细字段**: 与SSE类似，含融资余额/买入额/偿还额、融券余额/卖出量/偿还量/余量

**替代价值**: ⭐⭐⭐⭐⭐ — 可完全替代AKTools `stock_margin_detail_szse` 和 `macro_china_market_margin_sz`

---

#### 3.3.9 转融通

| 属性 | 值 |
|------|-----|
| 页面 | https://www.szse.cn/disclosure/margin/lended/index.html |
| 更新频率 | 每日 |
| 格式 | JSON / Excel |

**替代价值**: ⭐⭐⭐ — 做空力量分析，新增数据

---

#### 3.3.10 深港通

| 属性 | 值 |
|------|-----|
| 页面 | https://www.szse.cn/szhk/szhktradeinfo/szdaily/index.html |
| 更新频率 | 每日 |
| 格式 | JSON |

**返回字段**: 深股通买入金额、卖出金额、成交净额等

**替代价值**: ⭐⭐⭐⭐ — 北向资金流向，重要市场情绪指标

---

#### 3.3.11 行业统计

| 属性 | 值 |
|------|-----|
| 页面 | https://www.szse.cn/market/stock/deal/index.html |
| 更新频率 | 每日 |
| 格式 | JSON / Excel |

**返回字段**: 行业代码、行业名称、公司数量、成交金额、成交笔数、涨跌家数等

**替代价值**: ⭐⭐⭐ — 行业轮动分析，但无个股-行业映射

---

#### 3.3.12 指标排名

| 属性 | 值 |
|------|-----|
| 页面 | https://www.szse.cn/market/stock/active/index.html |
| 更新频率 | 每日 |
| 格式 | JSON / Excel |

**排名类型**: 涨幅排名、跌幅排名、振幅排名、换手率排名、成交量排名、成交金额排名

**替代价值**: ⭐⭐⭐⭐ — 热门股筛选，市场热度追踪

---

#### 3.3.13 名称变更

| 属性 | 值 |
|------|-----|
| 页面 | https://www.szse.cn/market/stock/changename/index.html |
| 更新频率 | 不定期 |
| 格式 | JSON / Excel |

**返回字段**: 证券代码、原简称、新简称、变更日期

**替代价值**: ⭐⭐⭐ — 股票更名追踪

---

#### 3.3.14 暂停/终止上市

| 属性 | 值 |
|------|-----|
| 页面 | https://www.szse.cn/market/stock/suspend/index.html |
| 更新频率 | 不定期 |
| 格式 | JSON / Excel |

**替代价值**: ⭐⭐⭐ — 退市风险监控

---

#### 3.3.15 ETF规模

| 属性 | 值 |
|------|-----|
| 页面 | https://www.szse.cn/market/fund/volume/etf/index.html |
| 更新频率 | 每日 |
| 格式 | JSON / Excel |

**替代价值**: ⭐⭐⭐ — 基金资金流向分析

---

#### 3.3.16 LOF规模

| 属性 | 值 |
|------|-----|
| 页面 | https://www.szse.cn/market/fund/volume/lof/index.html |
| 更新频率 | 每日 |
| 格式 | JSON / Excel |

**替代价值**: ⭐⭐ — 基金资金流向

---

#### 3.3.17 债券成交概况

| 属性 | 值 |
|------|-----|
| 页面 | https://www.szse.cn/market/bond/daily/index.html |
| 更新频率 | 每日 |
| 格式 | JSON / Excel |

**替代价值**: ⭐⭐ — 固收市场监控

---

#### 3.3.18 期权成交概况

| 属性 | 值 |
|------|-----|
| 页面 | https://www.szse.cn/market/option/day/index.html |
| 更新频率 | 每日 |
| 格式 | JSON / Excel |

**替代价值**: ⭐⭐⭐ — 波动率参考，市场情绪指标

---

#### 3.3.19 统计月报

| 属性 | 值 |
|------|-----|
| 页面 | https://www.szse.cn/market/periodical/month/index.html |
| 更新频率 | 月度 |
| 格式 | PDF |

**替代价值**: ⭐⭐ — 深度研究，但需PDF解析

---

#### 3.3.20 统计年鉴

| 属性 | 值 |
|------|-----|
| 页面 | https://www.szse.cn/market/periodical/year/index.html |
| 更新频率 | 年度 |
| 格式 | PDF |

**替代价值**: ⭐⭐ — 宏观趋势分析

---

#### 3.3.21 交易日历

| 属性 | 值 |
|------|-----|
| 页面 | https://www.szse.cn/aboutus/calendar/index.html |
| 接口 | `GET https://www.szse.cn/api/report/ShowReport?SHOWTYPE=JSON&CATALOGID=1118&TABKEY=tab1&year=2026` |
| 更新频率 | 年度 |
| 格式 | JSON |

**返回字段**: 日期、是否交易日、节假日名称

**替代价值**: ⭐⭐⭐⭐⭐ — 可替代AKTools `tool_trade_date_hist_sina`

---

#### 3.3.22 股票质押回购

| 属性 | 值 |
|------|-----|
| 页面 | https://www.szse.cn/disclosure/innovate/stock/message/index.html (交易信息) |
| 页面 | https://www.szse.cn/disclosure/innovate/stock/rate/index.html (平均质押率) |
| 更新频率 | 每日 |
| 格式 | JSON / Excel |

**替代价值**: ⭐⭐⭐⭐ — 风险预警指标

---

#### 3.3.23 专题统计

| 属性 | 值 |
|------|-----|
| 页面 | https://www.szse.cn/market/subject/index.html |
| 更新频率 | 不定期 |
| 格式 | JSON / Excel / PDF |

**替代价值**: ⭐⭐ — 深度专题分析

---

## 4. 数据替代分析

### 4.1 可完全替代的数据

| 当前数据 | 当前来源 | 官方替代源 | 替代方式 | 数据完整性 | 实施难度 |
|---------|---------|-----------|---------|-----------|---------|
| 融资融券宏观(SSE) | AKTools `macro_china_market_margin_sh` | SSE JS接口 `/marketdata/margin/` | 直接HTTP请求+Referer头 | 100% | 中 |
| 融资融券宏观(SZSE) | AKTools `macro_china_market_margin_sz` | SZSE API `ShowReport` | 标准REST GET | 100% | 低 |
| 融资融券明细(SSE) | AKTools `stock_margin_detail_sse` | SSE JS接口 | 直接HTTP请求+Referer头 | 100% | 中 |
| 融资融券明细(SZSE) | AKTools `stock_margin_detail_szse` | SZSE API | 标准REST GET | 100% | 低 |
| 交易日历 | AKTools `tool_trade_date_hist_sina` | SZSE API `CATALOGID=1118` | 标准REST GET | 95%(仅深交所日历) | 低 |
| 深市股票基本概况 | AKTools `stock_zh_a_spot`(部分字段) | SZSE API `CATALOGID=1803_after` | 标准REST GET | 80%(PE/PB/市值) | 低 |
| 深市指数日线 | AKTools `stock_zh_index_daily` | SZSE API 指数总览 | 标准REST GET | 90% | 低 |

### 4.2 可部分替代的数据

| 当前数据 | 当前来源 | 官方部分替代 | 缺口说明 |
|---------|---------|------------|---------|
| 沪市股票基本信息 | AKTools `stock_zh_a_spot` | SSE上市公司列表 | SSE仅含代码/名称/上市日期，缺PE/PB/市值/行情 |
| 市场成交概况 | — | SSE/SZSE成交概况 | 仅市场汇总级，无个股级数据 |
| 行业统计 | — | SZSE行业统计 | 仅行业级汇总，无个股-行业映射关系 |

### 4.3 无法替代的数据

| 当前数据 | 当前来源 | 不可替代原因 |
|---------|---------|------------|
| 个股级日线OHLCV | Tushare `daily` | 交易所不提供免费个股级历史行情下载 |
| 股票实时行情快照 | AKTools `stock_zh_a_spot` | 交易所仅提供延迟行情，无个股实时快照 |
| 行业成分股映射 | Playwright同花顺 | 交易所不提供个股-行业归属关系 |
| 概念板块定义及成分股 | Playwright同花顺 | 交易所不提供概念板块定义 |
| 个股换手率/量比 | AKTools `stock_zh_a_spot` | 交易所不提供个股级换手率/量比 |

### 4.4 替代收益分析

| 替代项 | 消除的依赖 | 减少的风险 | 数据质量提升 |
|--------|-----------|-----------|------------|
| 融资融券→官方 | AKTools中间层 | 接口被封/数据延迟 | 官方一手数据，更权威 |
| 交易日历→官方 | 新浪源 | 新浪接口不稳定 | 官方权威，覆盖完整 |
| 深市股票概况→官方 | AKTools中间层 | 第三方数据不一致 | 官方PE/PB更准确 |

---

## 5. 后续迭代数据需求

### 5.1 P0 — 立即可接入（替代现有数据源）

| 数据 | 来源 | 接入方式 | 新增表/字段 | 价值 |
|------|------|---------|-----------|------|
| 融资融券(SSE) | SSE JS接口 | HTTP+Referer | 无(替代现有) | 消除AKTools依赖 |
| 融资融券(SZSE) | SZSE API | REST GET | 无(替代现有) | 消除AKTools依赖 |
| 交易日历 | SZSE API | REST GET | 无(替代现有) | 消除新浪源依赖 |
| 深市股票基本概况 | SZSE API | REST GET | 无(补充PE/PB) | 数据质量提升 |

### 5.2 P1 — 高价值新增（短期迭代）

| 数据 | 来源 | 接入方式 | 新增表/字段 | 价值 |
|------|------|---------|-----------|------|
| 龙虎榜(SSE) | SSE JS接口 | HTTP+Referer | 新表 `dragon_tiger_list` | 短线交易核心参考，识别主力资金动向 |
| 深港通资金流向 | SZSE API | REST GET | 新表 `szhk_flow` | 北向资金是重要市场情绪指标 |
| 沪港通资金流向 | SSE JS接口 | HTTP+Referer | 新表 `shhk_flow` | 同上 |
| 指标排名(深市) | SZSE API | REST GET | 新表 `stock_ranking` | 热门股筛选，市场热度追踪 |
| 融资融券标的调整 | SSE/SZSE公告 | HTML解析 | 新表 `margin_target_change` | 自动跟踪标的变更 |

**龙虎榜数据模型建议**:

```sql
CREATE TABLE dragon_tiger_list
(
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    trade_date      DATE          NOT NULL,
    stock_code      VARCHAR(10)   NOT NULL,
    stock_name      VARCHAR(20)   NOT NULL,
    exchange        VARCHAR(4)    NOT NULL COMMENT 'SSE/SZSE',
    reason_type     VARCHAR(4)    NOT NULL COMMENT '11=涨幅偏离 12=跌幅偏离 13=振幅 14=换手率 15=首日 16=连续3日',
    deviation       DECIMAL(8,2)  NULL COMMENT '偏离值(%)',
    volume          BIGINT        NULL COMMENT '成交量(万股)',
    amount          DECIMAL(16,2) NULL COMMENT '成交金额(万元)',
    buy_detail      JSON          NULL COMMENT '买入营业部TOP5 [{name,amount}]',
    sell_detail     JSON          NULL COMMENT '卖出营业部TOP5 [{name,amount}]',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT(1)    NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_date_code_reason (trade_date, stock_code, reason_type),
    INDEX idx_trade_date (trade_date),
    INDEX idx_stock_code (stock_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
```

**深港通资金流向数据模型建议**:

```sql
CREATE TABLE hkconnect_flow
(
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    trade_date      DATE          NOT NULL,
    exchange        VARCHAR(4)    NOT NULL COMMENT 'SSE/SZSE',
    direction       VARCHAR(4)    NOT NULL COMMENT 'NORTH/SOUTH',
    buy_amount      DECIMAL(16,2) NULL COMMENT '买入金额(亿元)',
    sell_amount     DECIMAL(16,2) NULL COMMENT '卖出金额(亿元)',
    net_amount      DECIMAL(16,2) NULL COMMENT '净买入金额(亿元)',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT(1)    NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_date_exchange_dir (trade_date, exchange, direction),
    INDEX idx_trade_date (trade_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
```

### 5.3 P2 — 中期迭代（增强分析能力）

| 数据 | 来源 | 接入方式 | 新增表/字段 | 价值 |
|------|------|---------|-----------|------|
| 指数成分股 | SZSE API | REST GET | 新表 `index_constituent` | 指数投资标的追踪，调仓预警 |
| ETF规模 | SZSE API | REST GET | 新表 `etf_scale` | 基金资金流向分析 |
| 行业统计汇总 | SZSE API | REST GET | 新表 `industry_stat` | 行业轮动分析 |
| 期权成交概况 | SZSE API | REST GET | 新表 `option_daily` | 波动率参考，市场情绪 |
| 债券成交概况 | SZSE API | REST GET | 新表 `bond_daily` | 固收市场监控 |
| 市场总貌(两市) | SSE+SZSE | REST GET | 新表 `market_overview` | 大盘整体健康度 |
| 名称变更 | SZSE API | REST GET | 新表 `stock_rename` | 股票更名追踪 |
| 暂停/终止上市 | SZSE API | REST GET | 新表 `stock_delist` | 退市风险监控 |

### 5.4 P3 — 长期迭代（高级功能）

| 数据 | 来源 | 接入方式 | 新增表/字段 | 价值 |
|------|------|---------|-----------|------|
| 股票质押数据 | SZSE API | REST GET | 新表 `stock_pledge` | 风险预警指标 |
| 转融通数据 | SZSE API | REST GET | 新表 `sec_lending` | 做空力量分析 |
| 统计月报/年鉴 | SZSE PDF | PDF解析 | — | 深度研究，宏观趋势 |
| 实时行情推送 | SSE行情接口 | WebSocket | — | 实时盯盘，条件预警 |
| 大宗交易 | SSE/SZSE | HTTP | 新表 `block_trade` | 大股东/机构动向 |

---

## 6. 接口参考手册

### 6.1 SSE 接口通用规范

**基础域名**: `https://query.sse.com.cn`

**必需请求头**:
```
Referer: https://www.sse.com.cn/
User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36
```

**通用参数**:

| 参数 | 说明 | 示例 |
|------|------|------|
| jsonCallBack | JSONP回调函数名 | `jsonpCallback` |
| isPagination | 是否分页 | `true` |
| pageHelp.pageSize | 每页条数 | `10000` |
| pageHelp.pageNo | 页码 | `1` |
| pageHelp.beginPage | 起始页 | `1` |
| _ | 时间戳(防缓存) | `1716364800000` |

**接口清单**:

| 数据 | 接口路径 | 关键参数 |
|------|---------|---------|
| 每日成交概况 | `/commonQuery.do` | `sqlId=SSE_ZGPZ_YJLJLJXJ_L&tradeDate=YYYYMMDD` |
| 龙虎榜 | `/marketdata/publicQuery.do` | `sqlId=SSE_DAILYTRADE_INFO_PLCC&tradeDate=YYYYMMDD` |
| 融资融券明细 | `/marketdata/margin/tradeMarginDetailQuery.do` | `tradeDate=YYYYMMDD&pageSize=5000` |
| 融资融券汇总 | `/marketdata/margin/tradeMarginQuery.do` | `tradeDate=YYYYMMDD` |
| 上市公司列表 | `/commonQuery.do` | `sqlId=SSE_ZGPZ_GPLB_L` |
| 市场数据总貌 | `/commonQuery.do` | `sqlId=SSE_ZGPZ_SJZM_L` |

**响应格式(JSONP)**:
```javascript
jsonpCallback({
    "pageHelp": {
        "total": 2355,
        "data": [
            { "secCode": "600000", "secAbbr": "浦发银行", ... }
        ]
    }
});
```

**解析方式**: 去除 `jsonpCallback(` 前缀和 `)` 后缀，得到标准JSON

---

### 6.2 SZSE 接口通用规范

**基础URL**: `https://www.szse.cn/api/report/ShowReport`

**请求方式**: `GET`

**无需特殊请求头**

**通用参数**:

| 参数 | 说明 | 示例 |
|------|------|------|
| SHOWTYPE | 返回格式 | `JSON` / `xlsx` |
| CATALOGID | 数据目录ID | `1803_after` |
| TABKEY | Tab键 | `tab1` |
| txtQueryDate | 查询日期 | `2025-04-21` |
| random | 随机数 | `0.1151430633896432` |

**CATALOGID 索引表**:

| CATALOGID | 数据 | 页面 |
|-----------|------|------|
| `1803_after` | 股票基本概况 | `/market/stock/indicator/` |
| `1118` | 交易日历 | `/aboutus/calendar/` |
| `1839` | 融资融券 | `/disclosure/margin/margin/` |
| `1742` | 市场总貌 | `/market/overview/` |
| `1750` | 指标排名 | `/market/stock/active/` |
| `1752` | 行业统计 | `/market/stock/deal/` |
| `1737` | 指数总览 | `/market/exponent/pandect/` |
| `1739` | 指数与样本股 | `/market/exponent/sample/` |
| `1763` | 股票列表 | `/market/product/stock/list/` |
| `1766` | ETF列表 | `/market/product/list/etfList/` |
| `1770` | 名称变更 | `/market/stock/changename/` |
| `1771` | 暂停/终止上市 | `/market/stock/suspend/` |
| `1782` | 股票质押交易信息 | `/disclosure/innovate/stock/message/` |
| `1783` | 平均质押率 | `/disclosure/innovate/stock/rate/` |
| `1787` | 转融通 | `/disclosure/margin/lended/` |

**响应格式(JSON)**:
```json
{
    "data": [
        {
            "agdm": "000001",
            "gsmc": "平安银行",
            "ssrq": "1991-04-03",
            ...
        }
    ],
    "pageInfo": {
        "totalPage": 10,
        "totalRecord": 2500,
        "pageNo": 1
    }
}
```

---

## 7. 实施路线图

### Phase 1: 数据源替代（P0）

**目标**: 消除融资融券和交易日历对AKTools的依赖

```
Week 1-2:
├── 新建 SzseApiClient (标准REST, 无需特殊头)
│   ├── fetchMarginDetail(date)
│   ├── fetchMarginMacro(date)
│   ├── fetchTradeCalendar(year)
│   └── fetchStockIndicator(date)
├── 新建 SseApiClient (需Referer头, JSONP解析)
│   ├── fetchMarginDetail(date)
│   └── fetchMarginMacro(date)
└── 修改 CollectionOrchestrator
    ├── 融资融券采集: AKTools → 官方Client
    └── 交易日历采集: AKTools → SZSE API
```

**验证**: 对比新旧数据源同一交易日数据，确保一致性

### Phase 2: 高价值新增（P1）

**目标**: 接入龙虎榜、深港通等独有数据

```
Week 3-5:
├── 龙虎榜采集
│   ├── SseApiClient.fetchDragonTiger(date)
│   ├── 新表 dragon_tiger_list
│   └── 清洗服务 + 前端展示
├── 深港通/沪港通
│   ├── SzseApiClient.fetchHkConnect(date)
│   ├── SseApiClient.fetchHkConnect(date)
│   ├── 新表 hkconnect_flow
│   └── 前端展示
└── 指标排名
    ├── SzseApiClient.fetchStockRanking(date)
    ├── 新表 stock_ranking
    └── 前端展示
```

### Phase 3: 分析增强（P2）

**目标**: 丰富分析维度

```
Week 6-8:
├── 指数成分股追踪
├── 行业统计汇总
├── ETF规模监控
└── 市场总貌大盘指标
```

### Phase 4: 高级功能（P3）

**目标**: 风险预警和深度分析

```
Week 9-12:
├── 股票质押风险预警
├── 转融通做空分析
├── 大宗交易追踪
└── 统计月报PDF解析
```

---

## 8. 风险与注意事项

### 8.1 SSE接口风险

| 风险 | 等级 | 缓解措施 |
|------|------|---------|
| 无官方文档，接口可能变动 | 高 | 建立接口监控，异常时降级到AKTools |
| 需Referer头，可能被反爬 | 中 | 设置合理请求间隔(≥200ms)，模拟浏览器行为 |
| JSONP格式需手动解析 | 低 | 统一封装JSONP解析工具方法 |
| 分页参数可能变化 | 中 | pageSize设大值(10000)，减少分页依赖 |

### 8.2 SZSE接口风险

| 风险 | 等级 | 缓解措施 |
|------|------|---------|
| CATALOGID可能随网站改版变化 | 中 | 建立CATALOGID配置表，便于快速更新 |
| 大数据量Excel下载可能超时 | 低 | 优先使用JSON格式，分页获取 |
| 频率限制 | 低 | 请求间隔≥200ms |

### 8.3 通用风险

| 风险 | 等级 | 缓解措施 |
|------|------|---------|
| 交易所网站改版导致接口失效 | 高 | 保留AKTools降级路径，双源切换 |
| IP被封 | 中 | 限流 + 异常检测 + 降级 |
| 数据格式不一致 | 中 | 清洗层统一字段映射 |
| 历史数据回填 | 中 | SZSE支持Excel下载历史数据，SSE需逐日请求 |

### 8.4 降级策略

```
官方数据源 ──(失败)──→ AKTools ──(失败)──→ 标记数据缺失，告警
     │                      │
     └──(重试3次)──────────┘
```

- 官方接口请求失败时，自动降级到AKTools
- 连续3次降级后触发告警
- 保留 `raw_data.source` 字段标记数据来源（OFFICIAL_SSE / OFFICIAL_SZSE / AKTOOLS / TUSHARE）

---

## 附录A: 数据源对比矩阵

| 数据需求 | SSE官方 | SZSE官方 | AKTools | Tushare | Playwright |
|---------|---------|---------|---------|---------|-----------|
| 个股日线OHLCV | ❌ | ❌ | ✅(腾讯) | ✅ | ❌ |
| 实时行情快照 | ⚠️(延迟) | ❌ | ✅(新浪) | ❌ | ❌ |
| 融资融券明细 | ✅ | ✅ | ✅ | ❌ | ❌ |
| 融资融券宏观 | ✅ | ✅ | ✅ | ❌ | ❌ |
| 交易日历 | ❌ | ✅ | ✅(新浪) | ❌ | ❌ |
| 股票基本概况 | ⚠️(沪市少) | ✅ | ✅ | ❌ | ❌ |
| 指数日线 | ⚠️(沪市) | ⚠️(深市) | ✅ | ✅ | ❌ |
| 龙虎榜 | ✅ | ✅ | ❌ | ❌ | ❌ |
| 深港通/沪港通 | ✅ | ✅ | ❌ | ❌ | ❌ |
| 行业成分股 | ❌ | ❌ | ❌ | ❌ | ✅(同花顺) |
| 概念成分股 | ❌ | ❌ | ❌ | ❌ | ✅(同花顺) |
| 行业统计 | ❌ | ✅ | ❌ | ❌ | ❌ |
| 指标排名 | ❌ | ✅ | ❌ | ❌ | ❌ |
| 指数成分股 | ❌ | ✅ | ❌ | ❌ | ❌ |
| 股票质押 | ❌ | ✅ | ❌ | ❌ | ❌ |
| ETF规模 | ❌ | ✅ | ❌ | ❌ | ❌ |
| 名称变更 | ❌ | ✅ | ❌ | ❌ | ❌ |
| 暂停/终止上市 | ❌ | ✅ | ❌ | ❌ | ❌ |
| 转融通 | ❌ | ✅ | ❌ | ❌ | ❌ |

> ✅ = 可提供 | ⚠️ = 部分提供 | ❌ = 不可提供

---

## 附录B: 推荐的混合数据架构

```
┌──────────────────────────────────────────────────────────────────────┐
│                      trading-diary 数据层                             │
├───────────────────┬──────────────────┬───────────────────────────────┤
│    官方数据源       │   第三方数据源     │        爬虫补充               │
│   (优先使用)       │   (必要保留)      │      (最小化使用)             │
├───────────────────┼──────────────────┼───────────────────────────────┤
│ SZSE API:         │ Tushare Pro:     │ Playwright + 同花顺:          │
│ · 股票基本概况     │ · 个股日线OHLCV   │ · 行业成分股映射              │
│ · 融资融券(明细+宏) │                  │ · 概念成分股映射              │
│ · 交易日历        │ AKTools:          │                              │
│ · 行业统计        │ · 实时行情快照     │                              │
│ · 指标排名        │ · 个股日线(腾讯源) │                              │
│ · 指数数据        │ · 行业/概念名称    │                              │
│ · 深港通          │                  │                              │
│ · 指数成分股      │                  │                              │
│ · 股票质押        │                  │                              │
│ · 名称变更/退市   │                  │                              │
│ · ETF规模        │                  │                              │
│                   │                  │                              │
│ SSE JS接口:       │                  │                              │
│ · 成交概况        │                  │                              │
│ · 融资融券(明细+宏)│                  │                              │
│ · 龙虎榜         │                  │                              │
│ · 沪港通         │                  │                              │
│ · 上市公司列表    │                  │                              │
│ · 市场数据总貌    │                  │                              │
└───────────────────┴──────────────────┴───────────────────────────────┘
```

**数据源选择优先级**: SZSE API > SSE JS > Tushare > AKTools > Playwright

**降级链路**: 官方 → AKTools → 标记缺失+告警

---

## 附录C: 行业分类体系对比与行业/概念指数数据获取

### C.1 国内主要行业分类体系对比

A股市场存在**多套并行的行业分类体系**，它们之间**不是一一对应关系**，理解差异对数据建模至关重要。

| 分类体系 | 发布方 | 一级行业数 | 二级行业数 | 三级行业数 | 用途 |
|---------|--------|-----------|-----------|-----------|------|
| **证监会行业分类** | 中国证监会 | 21 | 90 | — | 官方监管分类，上市公司年报披露标准 |
| **申万行业分类** | 申万宏源证券 | 31 | 134 | 346 | 量化研究主流标准，行业指数最完善 |
| **同花顺行业分类** | 同花顺 | ~110 | — | — | 细分最多(446个行业指数)，散户使用最广 |
| **国证行业分类** | 深圳证券信息公司 | 10 | 26 | 67 | 国证指数系列基础 |
| **中证行业分类** | 中证指数公司 | 11 | 35 | 98 | 中证指数系列基础(与GICS接轨) |
| **GICS** | MSCI/S&P | 11 | 24 | 69 | 全球标准，外资机构使用 |

### C.2 交易所行业分类 vs 同花顺行业分类：关键差异

**结论：不是一一对应关系，且差异显著。**

| 维度 | 交易所(证监会)行业分类 | 同花顺行业分类 |
|------|---------------------|--------------|
| **粒度** | 粗(21个一级/90个二级) | 细(446个行业指数) |
| **标准** | 官方监管标准，按主营业务 | 市场导向，按股价联动性+业务相似性 |
| **更新** | 年度(上市公司年报后调整) | 实时(根据市场热点动态调整) |
| **映射** | 每个上市公司有唯一证监会行业 | 每个上市公司有唯一同花顺一级行业，但同花顺二级行业更细 |
| **交叉** | 证监会1个二级行业 → 可能对应同花顺多个行业 | 同花顺1个行业 → 通常归属证监会1个一级行业 |
| **示例** | 证监会"制造业" → 同花顺拆分为"汽车零部件""半导体""消费电子"等数十个 | 同花顺"银行" ≈ 证监会"金融业-货币金融服务" |

**映射关系**: 多对多

```
证监会行业(粗)                    同花顺行业(细)
┌──────────────┐                ┌──────────────────┐
│ 制造业        │──── 1:N ────→│ 汽车零部件         │
│               │               │ 半导体            │
│               │               │ 消费电子           │
│               │               │ 光伏设备           │
│               │               │ ... (数十个)       │
└──────────────┘                └──────────────────┘

┌──────────────┐                ┌──────────────────┐
│ 金融业        │──── 1:N ────→│ 银行              │
│               │               │ 证券              │
│               │               │ 保险              │
│               │               │ 多元金融           │
└──────────────┘                └──────────────────┘
```

**对项目的影响**:
- 当前项目 `industry` 表使用的是**同花顺行业分类**（通过 `stock_board_industry_name_ths` 获取）
- 交易所(SZSE)的"行业统计"使用的是**国证/证监会行业分类**
- 两者**不可直接混用**，需要建立映射表或选择一套标准

### C.3 行业指数数据获取方案

行业指数是衡量行业整体走势的关键指标。以下是各行业指数体系的获取方式：

#### C.3.1 同花顺行业指数（当前项目使用）

**AKShare/AKTools 接口**:

| 接口 | 功能 | 参数 | 返回字段 | 状态 |
|------|------|------|---------|------|
| `stock_board_industry_name_ths` | 行业板块名称列表 | 无 | 板块代码、板块名称 | ✅ 可用 |
| `stock_board_industry_index_ths` | 行业板块指数历史行情 | symbol, start_date, end_date | 日期、开盘、收盘、最高、最低、成交量、成交额 | ✅ 可用 |
| `stock_board_industry_spot_ths` | 行业板块实时行情 | 无 | 名称、最新价、涨跌幅、领涨股等 | ✅ 可用 |
| `stock_board_industry_cons_ths` | 行业板块成分股 | symbol | 代码、名称 | ❌ 已封(需Playwright) |

**`stock_board_industry_index_ths` 详情**:

```python
import akshare as ak

df = ak.stock_board_industry_index_ths(
    symbol="银行",           # 行业名称(来自 stock_board_industry_name_ths)
    start_date="20240101",   # 开始日期
    end_date="20241231"      # 结束日期
)

# 返回字段:
# 日期       开盘      收盘      最高      最低      成交量      成交额
# 2024-01-02  3856.42  3872.15  3889.30  3841.28  12345678  1234567890.12
```

**AKTools HTTP 映射**:
```
GET /api/public/stock_board_industry_index_ths?symbol=银行&start_date=20240101&end_date=20241231
```

**数据特点**:
- 覆盖同花顺全部 ~446 个行业指数
- 日线级OHLCV数据，历史数据完整
- 指数基期和编制方法由同花顺定义
- **这是获取行业指数日线数据最便捷的免费途径**

#### C.3.2 申万行业指数（量化研究主流）

申万行业指数是量化研究最广泛使用的行业分类标准，有31个一级行业指数。

**获取方式**:

| 方式 | 接口/地址 | 返回字段 | 状态 | 说明 |
|------|---------|---------|------|------|
| AKShare `index_stock_info` + `stock_zh_index_daily` | 指数代码如 801010.XI | OHLCV | ✅ 可用 | 需知道申万指数代码 |
| 申万宏源官网 | `http://www.swsindex.com` | 指数列表+行情 | ⚠️ 需爬虫 | 官方源但无API |
| Tushare `index_daily` | 指数代码 | OHLCV | ✅ 可用 | 需积分≥120 |
| JoinQuant(聚宽) | `finance.SW1_DAILY_PRICE` | OHLCV | ✅ 可用 | 需注册，免费有限额 |

**申万一级行业指数代码表(部分)**:

| 指数代码 | 行业名称 | 指数代码 | 行业名称 |
|---------|---------|---------|---------|
| 801010 | 农林牧渔 | 801170 | 交通运输 |
| 801020 | 采掘 | 801180 | 房地产 |
| 801030 | 化工 | 801200 | 建筑装饰 |
| 801040 | 钢铁 | 801210 | 电气设备 |
| 801050 | 有色金属 | 801230 | 综合 |
| 801080 | 电子 | 801710 | 建筑材料 |
| 801110 | 家用电器 | 801720 | 通信 |
| 801120 | 食品饮料 | 801730 | 电力设备 |
| 801130 | 纺织服装 | 801740 | 国防军工 |
| 801140 | 轻工制造 | 801750 | 计算机 |
| 801150 | 医药生物 | 801760 | 传媒 |
| 801160 | 公用事业 | 801770 | 银行 |
| 801780 | 非银金融 | 801790 | 汽车 |
| 801880 | 商业贸易 | 801890 | 机械设备 |
| 801950 | 煤炭 | 801960 | 石油石化 |
| 801970 | 环保 | 801980 | 美容护理 |
| 801990 | 社会服务 | — | — |

**通过AKShare获取申万行业指数日线**:

```python
import akshare as ak

# 方法1: 通过指数代码直接获取
df = ak.stock_zh_index_daily(symbol="sh801010")  # 农林牧渔

# 方法2: 通过AKTools HTTP
# GET /api/public/stock_zh_index_daily?symbol=sh801010
```

#### C.3.3 中证行业指数

中证行业指数与GICS接轨，是外资机构最认可的分类标准。

**获取方式**:

| 方式 | 说明 | 状态 |
|------|------|------|
| 中证指数官网 `https://www.csindex.com.cn` | 提供指数行情下载 | ✅ 有API但需申请 |
| AKShare `stock_zh_index_daily` | 通过指数代码获取 | ✅ 可用 |
| Tushare `index_daily` | 通过指数代码获取 | ✅ 可用 |

**中证行业指数代码示例**: 
- 399967.XSHE — 中证军工
- 399986.XSHE — 中证银行
- 399997.XSHE — 中证白酒

#### C.3.4 交易所行业统计（无行业指数）

SZSE提供的"行业统计"(CATALOGID=1752)是**按行业汇总的成交数据**，不是行业指数：

| 字段 | 说明 |
|------|------|
| 行业代码 | 证监会/国证行业代码 |
| 行业名称 | 行业名称 |
| 公司数量 | 该行业上市公司数 |
| 成交金额 | 行业总成交额 |
| 涨跌家数 | 上涨/下跌/平盘家数 |

**注意**: 这不是行业指数(OHLCV)，无法用于走势分析，仅能做行业横向对比。

### C.4 同花顺概念指数数据获取

#### C.4.1 概念指数历史行情

**AKShare/AKTools 接口**:

| 接口 | 功能 | 参数 | 返回字段 | 状态 |
|------|------|------|---------|------|
| `stock_board_concept_name_ths` | 概念板块名称列表 | 无 | 板块代码、板块名称 | ✅ 可用 |
| `stock_board_concept_index_ths` | 概念板块指数历史行情 | symbol, start_date, end_date | 日期、开盘、收盘、最高、最低、成交量、成交额 | ✅ 可用 |
| `stock_board_concept_spot_ths` | 概念板块实时行情 | 无 | 名称、最新价、涨跌幅、领涨股等 | ✅ 可用 |
| `stock_board_concept_cons_ths` | 概念板块成分股 | symbol | 代码、名称 | ❌ 已封(需Playwright) |

**`stock_board_concept_index_ths` 详情**:

```python
import akshare as ak

df = ak.stock_board_concept_index_ths(
    symbol="阿里巴巴概念",    # 概念名称(来自 stock_board_concept_name_ths)
    start_date="20240101",
    end_date="20241231"
)

# 返回字段:
# 日期       开盘      收盘      最高      最低      成交量      成交额
```

**AKTools HTTP 映射**:
```
GET /api/public/stock_board_concept_index_ths?symbol=阿里巴巴概念&start_date=20240101&end_date=20241231
```

**⚠️ 重要区分**:
- `stock_board_industry_index_ths` — 行业指数（如"银行""半导体"）
- `stock_board_concept_index_ths` — 概念指数（如"阿里巴巴概念""ChatGPT"）
- 两者参数格式相同，但**不能混用**，概念名称传入行业接口会返回空数据

#### C.4.2 东方财富概念指数（备选）

| 接口 | 功能 | 状态 | 说明 |
|------|------|------|------|
| `stock_board_concept_name_em` | 概念板块名称列表 | ✅ 可用 | 东方财富分类，与同花顺不同 |
| `stock_board_concept_hist_em` | 概念板块历史行情 | ⚠️ 不稳定 | 东方财富接口频繁变动 |
| `stock_board_concept_spot_em` | 概念板块实时行情 | ✅ 可用 | — |

**注意**: 东方财富和同花顺的概念分类体系完全不同，不可混用。

### C.5 行业/概念指数数据模型建议

#### C.5.1 行业指数日线表

```sql
CREATE TABLE industry_index_daily
(
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    industry_code   VARCHAR(20)   NOT NULL COMMENT '行业编码(同花顺/申万)',
    industry_name   VARCHAR(50)   NOT NULL COMMENT '行业名称',
    classify_system VARCHAR(10)   NOT NULL COMMENT '分类体系: THS/SW/ZZ',
    trade_date      DATE          NOT NULL COMMENT '交易日期',
    open            DECIMAL(10,3) NULL COMMENT '开盘价',
    high            DECIMAL(10,3) NULL COMMENT '最高价',
    low             DECIMAL(10,3) NULL COMMENT '最低价',
    close           DECIMAL(10,3) NULL COMMENT '收盘价',
    volume          BIGINT        NULL COMMENT '成交量',
    amount          DECIMAL(16,2) NULL COMMENT '成交额',
    change_pct      DECIMAL(6,2)  NULL COMMENT '涨跌幅(%)',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT(1)    NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code_system_date (industry_code, classify_system, trade_date),
    INDEX idx_system_date (classify_system, trade_date),
    INDEX idx_industry_date (industry_code, trade_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT '行业指数日线表_存储行业板块指数的每日OHLCV行情';
```

#### C.5.2 概念指数日线表

```sql
CREATE TABLE concept_index_daily
(
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    concept_code    VARCHAR(20)   NOT NULL COMMENT '概念编码(同花顺)',
    concept_name    VARCHAR(50)   NOT NULL COMMENT '概念名称',
    trade_date      DATE          NOT NULL COMMENT '交易日期',
    open            DECIMAL(10,3) NULL COMMENT '开盘价',
    high            DECIMAL(10,3) NULL COMMENT '最高价',
    low             DECIMAL(10,3) NULL COMMENT '最低价',
    close           DECIMAL(10,3) NULL COMMENT '收盘价',
    volume          BIGINT        NULL COMMENT '成交量',
    amount          DECIMAL(16,2) NULL COMMENT '成交额',
    change_pct      DECIMAL(6,2)  NULL COMMENT '涨跌幅(%)',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT(1)    NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code_date (concept_code, trade_date),
    INDEX idx_concept_date (concept_code, trade_date),
    INDEX idx_trade_date (trade_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT '概念指数日线表_存储概念板块指数的每日OHLCV行情';
```

### C.6 行业/概念指数数据获取总结

| 数据需求 | 最佳获取方式 | 接口 | 数据质量 | 稳定性 |
|---------|------------|------|---------|--------|
| 同花顺行业指数日线 | AKTools | `stock_board_industry_index_ths` | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| 同花顺概念指数日线 | AKTools | `stock_board_concept_index_ths` | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| 申万行业指数日线 | AKTools/Tushare | `stock_zh_index_daily` + 指数代码 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 中证行业指数日线 | AKTools/Tushare | `stock_zh_index_daily` + 指数代码 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 行业成分股映射 | Playwright同花顺 | — | ⭐⭐⭐ | ⭐⭐ |
| 概念成分股映射 | Playwright同花顺 | — | ⭐⭐⭐ | ⭐⭐ |
| 交易所行业统计 | SZSE API | `CATALOGID=1752` | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |

**推荐策略**:
1. **行业指数日线**: 优先使用 AKTools `stock_board_industry_index_ths`（同花顺行业），补充申万行业指数通过 `stock_zh_index_daily`
2. **概念指数日线**: 使用 AKTools `stock_board_concept_index_ths`
3. **行业分类标准**: 项目统一使用同花顺行业分类（与现有 `industry` 表一致），申万分类作为补充
4. **交易所行业统计**: SZSE API 仅用于行业横向对比，不用于走势分析
