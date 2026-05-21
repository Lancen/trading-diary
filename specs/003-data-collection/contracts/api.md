# API 契约

所有接口前缀：`/api/v1/admin`，需要 ADMIN 角色。

## StockDataController — `/api/v1/admin/stocks`

### GET /stocks/list
股票列表（含 OHLCV + 两融 + 排序）

**Query Params:**
- `keyword` (可选) — 股票代码/名称模糊搜索
- `industry` (可选) — 行业名称筛选
- `concept` (可选) — 概念名称筛选
- `tradeDate` (可选) — 日期，不传=最新
- `sortBy` (可选) — 排序列: changePct/volume/marginBalance/marginChange/shortBalance/shortChange
- `sortDir` (可选) — asc/desc，默认 desc
- `page` (默认 1)
- `size` (默认 50)

**Response:**
```json
{
  "code": 200,
  "data": {
    "records": [{
      "stockCode": "000001",
      "stockName": "平安银行",
      "industry": "银行",
      "concepts": "MSCI,沪深300",
      "close": 12.80,
      "changePct": 2.40,
      "volume": 52340100,
      "marginBalance": 3258000000.00,
      "marginChange": 123000000.00,
      "shortBalance": 15000000.00,
      "shortChange": -5000000.00,
      "tradeDate": "2026-05-20"
    }],
    "total": 4821
  }
}
```

### GET /stocks/{code}
单股票详情

**Path Params:** `code` — 股票代码
**Query Params:** `startDate` (可选), `endDate` (可选) — 日线范围

**Response:**
```json
{
  "code": 200,
  "data": {
    "stockCode": "000001",
    "stockName": "平安银行",
    "industry": "银行",
    "concepts": ["MSCI", "沪深300", "破净股"],
    "latestQuote": { "open": 12.50, "high": 12.95, "low": 12.40, "close": 12.80, "volume": 52340100, "changePct": 2.40 },
    "latestMargin": { "marginBalance": 32.58e8, "marginBuy": 1.23e8, "shortBalance": 0.15e8, "totalBalance": 32.73e8 },
    "dailyKlines": [{ "tradeDate": "2026-05-20", "open": 12.50, "high": 12.95, "low": 12.40, "close": 12.80, "volume": 52340100 }],
    "dailyMargins": [{ "tradeDate": "2026-05-20", "marginBalance": 32.58e8, "marginChange": 1.23e8, "shortBalance": 0.15e8, "shortChange": -0.05e8 }]
  }
}
```

### GET /stocks/{code}/calendar
股票交易日历

**Query Params:** `yearMonth` — YYYY-MM 格式

**Response:**
```json
{
  "code": 200,
  "data": {
    "yearMonth": "2026-05",
    "days": [
      { "date": "2026-05-01", "isTradeDay": false, "hasData": false, "status": "NON_TRADING" },
      { "date": "2026-05-05", "isTradeDay": true, "hasData": true, "status": "COLLECTED" },
      { "date": "2026-05-06", "isTradeDay": true, "hasData": false, "status": "MISSING" }
    ]
  }
}
```

## MarketDataController — `/api/v1/admin/market`

### GET /market/concepts
概念列表（含两融聚合）

**Query Params:** `keyword` (可选), `tradeDate` (可选), `sortBy`, `sortDir`, `page`, `size`

**Response:**
```json
{
  "code": 200,
  "data": {
    "records": [{
      "conceptCode": "GN001",
      "conceptName": "MSCI",
      "stockCount": 268,
      "marginBalance": 68050000000.00,
      "marginChange": 1520000000.00,
      "shortBalance": 4580000000.00,
      "shortChange": -310000000.00,
      "snapDate": "2026-05-18"
    }],
    "total": 312
  }
}
```

### GET /market/industries
行业列表（同概念结构）

## MarginStatsController — `/api/v1/admin/margin-stats`

### GET /margin-stats/summary
融资统计总量

**Query Params:** `tradeDate`, `exchange` (可选)

**Response:**
```json
{
  "code": 200,
  "data": {
    "totalMarginBalance": 128560000000.00,
    "totalShortBalance": 8632000000.00,
    "totalBalance": 137192000000.00,
    "stockCount": 1826,
    "tradeDate": "2026-05-20"
  }
}
```

### GET /margin-stats/industries
行业融资列表（同 MarketDataController 行业结构，按 tradeDate 过滤）

### GET /margin-stats/concepts
概念融资列表

### GET /margin-stats/market/history
大盘历史（二期）

### GET /margin-stats/industry/{code}/history
行业融资历史（二期）

### GET /margin-stats/concept/{code}/history
概念融资历史（二期）
