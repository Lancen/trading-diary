# ADR-0001: 指数日线统一表 vs 分表

## 状态

已接受

## 日期

2026-05-24

## 语境

项目需要存储三类指数的日线数据：
- **宽基指数**（MARKET）：上证指数、沪深300、中证500 等，来源 `stock_zh_index_daily`
- **行业指数**（INDUSTRY）：银行、半导体 等 ~446 个，来源 `stock_board_industry_index_ths`
- **概念指数**（CONCEPT）：ChatGPT、新能源车 等 ~400+ 个，来源 `stock_board_concept_index_ths`

三类指数的数据结构完全一致（日期、开盘、最高、最低、收盘、成交量、成交额），但来源接口不同、语义不同、数量级不同（宽基 5-8 个 vs 行业 446 个 vs 概念 400+ 个）。

现有 `index_daily` 表仅有 `index_code + trade_date + OHLCV`，无类型区分。

## 决策

所有指数日线统一存入 `index_daily` 表，新增 `index_type` 字段（MARKET/INDUSTRY/CONCEPT）区分。

采集拆为 3 个独立类型：`MARKET_INDEX_DAILY`、`INDUSTRY_INDEX_DAILY`、`CONCEPT_INDEX_DAILY`，各自独立触发、互不影响。

## 理由

### 选择统一表的原因

1. **数据结构完全一致**：三类指数返回相同的 7 字段（日期、OHLCV、成交量、成交额），无结构差异
2. **查询统一**：前端图表组件不关心指数类型，只需 `index_code + trade_date → OHLCV`，统一表避免按类型路由
3. **总量可控**：宽基 8 个 × 250天 + 行业 446 × 250天 + 概念 400 × 250天 ≈ 21万行/年，单表完全承载
4. **组件复用**：K线+两融叠加图组件可统一消费 `index_daily`，无需按类型写三套

### 选择 3 个独立采集类型的原因

1. **来源接口不同**：三个 AKShare 接口参数格式不同，无法统一调用
2. **失败隔离**：行业接口失败不影响宽基采集
3. **频率灵活**：宽基每日必采，行业/概念可按需降低频率

### 被否决的方案

| 方案 | 否决理由 |
|------|---------|
| 三张表（`index_daily` / `industry_index_daily` / `concept_index_daily`） | 查询需按类型路由，图表组件需写三套数据获取逻辑，维护成本高 |
| 两张表（宽基+行业统一，概念单独） | 人为拆分无明确边界，概念数量增长后同样面临管理问题 |

## 后果

- `index_daily` 表需新增 `index_type VARCHAR(10)` 字段和对应索引
- 查询时需注意 `index_type` 过滤，避免行业指数污染宽基查询
- 未来新增指数类型（如申万行业指数）只需新增 `index_type` 枚举值，无需建表
