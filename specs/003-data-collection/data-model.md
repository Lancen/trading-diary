# 数据模型

## 新增实体

### IndexDaily

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 主键 |
| index_code | VARCHAR(10) NOT NULL | 指数代码 (sh000001/sz399001) |
| trade_date | DATE NOT NULL | 交易日 |
| open | DECIMAL(10,3) | 开盘 |
| high | DECIMAL(10,3) | 最高 |
| low | DECIMAL(10,3) | 最低 |
| close | DECIMAL(10,3) | 收盘 |
| volume | BIGINT | 成交量 |
| amount | DECIMAL(16,2) | 成交额 |
| change_pct | DECIMAL(6,2) | 涨跌幅(%) |
| created_at | DATETIME DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME ON UPDATE | 更新时间 |
| is_deleted | TINYINT(1) DEFAULT 0 | 软删除 |

**索引**: UNIQUE KEY `uk_code_date` (index_code, trade_date)

## 修改实体

### DataCollectionLog（加 3 字段）

| 新字段 | 类型 | 说明 |
|--------|------|------|
| request_url | VARCHAR(512) | 外部 API 请求地址 |
| request_params | VARCHAR(512) | 请求参数 |
| remark | VARCHAR(256) | 备注（正常描述或补充信息） |

原有 `error_msg` 保留（仅存异常信息）。

### MarginDaily（加 2 字段）

| 新字段 | 类型 | 说明 |
|--------|------|------|
| margin_change | DECIMAL(16,2) | 融资余额较上一交易日变化 |
| short_change | DECIMAL(16,2) | 融券余额较上一交易日变化 |

## 现有实体（仅引用，不改）

### StockInfo
`code, name, latest_price, change_pct, change_amount, volume, amount, turnover_rate, volume_ratio, pe, pb, total_mv, float_mv, snapshot_date`

### StockDaily
`stock_code, trade_date, open, high, low, close, volume, amount`

### MarginDaily (existing fields)
`stock_code, trade_date, exchange, margin_balance, margin_buy, margin_repay, short_balance, short_sell_vol, short_repay_vol, short_remain_vol, total_balance`

### StockIndustry
`stock_code, industry_code, snap_date`

### StockConcept
`stock_code, concept_code, snap_date`

### TradeCalendar
`trade_date, is_trading_day`

## 关系

```
IndexDaily -- index_code + trade_date (唯一)
MarginDaily -- stock_code + trade_date + exchange (唯一)
DataCollectionLog -- dataType + jobType + tradeDate

StockInfo -- code + snapshot_date (唯一)
StockDaily -- stock_code + trade_date (唯一)

StockIndustry -- stock_code → Industry.code, snap_date
StockConcept -- stock_code → Concept.code, snap_date

Concept 融资聚合: MarginDaily.stock_code → StockConcept.stock_code → Concept
Industry 融资聚合: MarginDaily.stock_code → StockIndustry.stock_code → Industry
```
