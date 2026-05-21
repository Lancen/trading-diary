# 采集管线

## 采集类型分级

| 类型 | 频率 | 数据表 | 是否需要交易日历 |
|------|------|--------|-----------------|
| STOCK_INFO | 日级 | stock_info + stock_daily（复用同一 API） | ✓ |
| STOCK_DAILY | 日级 | 复用 STOCK_INFO 的 FETCH 数据 | ✓ |
| MARGIN_DAILY_SSE/SZSE | 日级 | margin_daily | ✓ |
| MARGIN_MACRO_SSE/SZSE | 日级 | margin_macro | ✓ |
| TRADE_CALENDAR | 不定期 | trade_calendar | ✗（自身就是日历数据源） |
| INDUSTRY_NAME | 月级 | industry | ✗ |
| CONCEPT_NAME | 月级 | concept | ✗ |

**Why:** 日历组件需要按 dataType 查询对应表的数据覆盖度，月级快照不需要日历。

## 日历数据源路由

日历 API (`GET /api/v1/admin/stocks/calendar`) 按 `dataType` 参数路由到不同表：

- `STOCK_INFO` → stock_info + stock_daily 并集（复用同一 spot API）
- `MARGIN_DAILY_SSE` → margin_daily WHERE exchange='SSE'
- `MARGIN_DAILY_SZSE` → margin_daily WHERE exchange='SZSE'
- `MARGIN_MACRO_SSE` → margin_macro WHERE exchange='SSE'
- `MARGIN_MACRO_SZSE` → margin_macro WHERE exchange='SZSE'

**Why:** 之前所有类型共用 stock 表查询，导致两融类型日历显示错误。

## 表结构注意事项

- `margin_macro` 表**没有** `is_deleted` 字段（不同于其他表）
- `stock_info` 和 `stock_daily` 共享同一份 spot API 返回数据，完整性判断用并集

## 启动要求

后端必须激活 `dev` profile，否则 JWT 配置不加载：

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

**Why:** `application-dev.yml` 含 `app.jwt.*` 配置，default profile 无此配置导致登录 NPE。
