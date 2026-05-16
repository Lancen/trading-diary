# API 合约：CollectionController

**Base URL**: `/api/v1/admin/collection`
**认证**: Bearer JWT (需要 ADMIN 角色)
**Content-Type**: `application/json`

## 1. 获取采集状态

```
GET /api/v1/admin/collection/status
```

**Response** (200):
```json
{
  "code": 0,
  "data": [
    {
      "dataType": "STOCK_INFO",
      "dataTypeLabel": "股票基础信息",
      "lastFetch": {
        "status": "SUCCESS",
        "startedAt": "2026-05-13T16:00:05",
        "completedAt": "2026-05-13T16:02:30",
        "recordCount": 5362,
        "errorMsg": null
      },
      "lastCleanse": {
        "status": "SUCCESS",
        "startedAt": "2026-05-13T16:02:31",
        "completedAt": "2026-05-13T16:03:12",
        "recordCount": 5362,
        "errorMsg": null
      }
    }
  ],
  "message": "ok"
}
```

**状态枚举**: `SUCCESS` | `FAILED` | `RUNNING` | `NOT_TRIGGERED` (从未采集)

---

## 2. 获取最近日志

```
GET /api/v1/admin/collection/logs?dataType=STOCK_INFO&limit=10
```

**Parameters**:
- `dataType` (required): 数据类型枚举
- `limit` (optional, default 10): 返回条数

**Response** (200):
```json
{
  "code": 0,
  "data": [
    {
      "id": 123,
      "dataType": "STOCK_INFO",
      "jobType": "FETCH",
      "status": "SUCCESS",
      "tradeDate": "2026-05-13",
      "recordCount": 5362,
      "errorMsg": null,
      "startedAt": "2026-05-13T16:00:05",
      "completedAt": "2026-05-13T16:02:30"
    }
  ],
  "message": "ok"
}
```

---

## 3. 获取两融数据缺口

```
GET /api/v1/admin/collection/gaps?start=2024-01-01&end=2024-12-31&exchange=SSE
```

**Parameters**:
- `start` (required): 起始日期
- `end` (required): 结束日期
- `exchange` (required): SSE / SZSE

**Response** (200):
```json
{
  "code": 0,
  "data": {
    "weeks": [
      {
        "weekStart": "2024-01-01",
        "weekEnd": "2024-01-05",
        "expectedDays": 4,
        "collectedDays": 4,
        "missingDates": [],
        "status": "COMPLETE"
      },
      {
        "weekStart": "2024-01-08",
        "weekEnd": "2024-01-12",
        "expectedDays": 5,
        "collectedDays": 3,
        "missingDates": ["2024-01-11", "2024-01-12"],
        "status": "PARTIAL"
      }
    ],
    "totalWeeks": 52,
    "completeWeeks": 50,
    "partialWeeks": 2,
    "missingWeeks": 0
  },
  "message": "ok"
}
```

**status 枚举**: `COMPLETE` | `PARTIAL` | `MISSING`

---

## 4. 手动触发采集

```
POST /api/v1/admin/collection/trigger/{dataType}
```

**Path Parameters**:
- `dataType`: STOCK_INFO / STOCK_DAILY / INDUSTRY_NAME / INDUSTRY_CONS / CONCEPT_NAME / CONCEPT_CONS / MARGIN_DAILY_SSE / MARGIN_DAILY_SZSE / TRADE_CALENDAR

**Response** (200 - 触发成功):
```json
{
  "code": 0,
  "data": {
    "dataType": "STOCK_INFO",
    "message": "采集任务已启动"
  },
  "message": "ok"
}
```

**Response** (409 - 已在执行中):
```json
{
  "code": 40901,
  "data": null,
  "message": "该数据类型已在执行中，请等待当前任务完成"
}
```

---

## 5. 按日期范围补采

```
POST /api/v1/admin/collection/backfill
```

**Request Body**:
```json
{
  "dataType": "MARGIN_DAILY_SSE",
  "exchange": "SSE",
  "startDate": "2024-01-01",
  "endDate": "2024-01-15"
}
```

**Response** (200):
```json
{
  "code": 0,
  "data": {
    "dataType": "MARGIN_DAILY_SSE",
    "tradingDays": 10,
    "weeks": 3,
    "message": "已创建 3 个周分片任务，共 10 个交易日"
  },
  "message": "ok"
}
```

---

## 数据类型枚举

| 枚举值 | 中文名 |
|--------|--------|
| STOCK_INFO | 股票基础信息 |
| STOCK_DAILY | 股票日线行情 |
| TRADE_CALENDAR | 交易日历 |
| INDUSTRY_NAME | 行业板块列表 |
| INDUSTRY_CONS | 行业成分股 |
| CONCEPT_NAME | 概念板块列表 |
| CONCEPT_CONS | 概念成分股 |
| MARGIN_DAILY_SSE | 两融交易明细（上交所） |
| MARGIN_DAILY_SZSE | 两融交易明细（深交所） |
