# ADR-0003: 板块指数K线采集管线重构

## 状态

已接受

## 日期

2026-05-26

## 语境

板块指数日线（`sector_index_daily`）的采集管线存在以下问题：

1. **FETCH阶段返回空数组**：当前 `INDUSTRY_INDEX_DAILY` / `CONCEPT_INDEX_DAILY` 的 FETCH 阶段返回 `"[]"`，490次AKTools API调用全部放在CLEANSE阶段执行，违反了"FETCH必须调API并入库raw_data"的统一管线原则
2. **无前端触发入口**：采集Hub页面没有 `INDUSTRY_INDEX_DAILY` / `CONCEPT_INDEX_DAILY` 的卡片，无法手动触发
3. **无补采能力**：不支持历史数据补采，只能采集单日数据
4. **raw_data缺少板块标识**：490个板块的原始数据无法区分归属

## 决策

### 1. FETCH阶段执行490次API调用

FETCH阶段遍历所有行业/概念，逐个调用AKTools API获取板块指数数据，每板块一条raw_data记录。CLEANSE阶段只从raw_data读取并入库sector_index_daily表。

与项目其他AKTools数据类型保持统一流程：FETCH调API存raw_data → CLEANSE读raw_data入库。

### 2. raw_data表增加sector_code列

490条raw_data记录需要区分"这条是哪个板块的"。AKTools返回的JSON中没有板块编码，只有OHLCV数据，因此raw_data表新增`sector_code`字段（VARCHAR(20)，nullable），FETCH时写入板块编码。

### 3. 每个板块一次调用拉全范围

AKTools接口支持start_date/end_date参数，每个板块一次调用可拉取任意日期范围的数据。补采1年或3年历史，调用次数始终是490次（~2分钟），无需按交易日循环。

### 4. 手动触发 + 自动联动

- 采集Hub增加 `INDUSTRY_INDEX_DAILY` / `CONCEPT_INDEX_DAILY` 卡片，支持手动触发
- 补采API增加对应分支，支持日期范围补采
- 两融采集完成后可自动联动触发当日板块K线采集（增量场景）

### 5. 数据源统一为同花顺

板块指数K线使用 `stock_board_industry_index_ths` / `stock_board_concept_index_ths`（同花顺源），与成分股数据源保持一致，避免混用东方财富源导致指数计算方式不同。

### 6. 板块两融保持纯实时聚合

不建 `sector_margin_daily` 快照表。板块两融始终通过实时聚合 `margin_daily` + 当前归属计算，与ADR-0002立场一致。接受"历史快照用的是今天的成分归属"这个不精确性。

## 理由

### FETCH必须入库的原因

1. **管线统一**：所有AKTools数据类型遵循相同的FETCH→CLEANSE流程，降低认知负担
2. **数据可追溯**：raw_data保留原始API响应，CLEANSE失败重跑无需重新调API
3. **幂等性**：重跑CLEANSE是纯本地操作，不产生外部副作用

### 每板块一条raw_data的原因

1. **单行大小可控**：1个板块×365天×7字段 ≈ 几十KB，不会产生超大行
2. **失败隔离**：单个板块API失败不影响其他板块的raw_data存储
3. **CLEANSE友好**：逐条解析，内存可控

### 不建快照表的原因

1. **与ADR-0002一致**：当前归属快照下，快照和实时聚合结果一致，快照无语义增益
2. **优先功能**：快照方案增加复杂度但当前无实际收益，历史归属不精确性可接受
3. **可延后**：未来如需精确历史归属，可引入时序归属+物化表

## 后果

- `raw_data` 表需新增 `sector_code` 列（数据库迁移）
- FETCH阶段执行490次API调用，耗时约2分钟，需确保超时配置足够
- K线采集依赖 `industry` / `concept` 表有数据，为空时应报错提示先采名称
- 采集Hub页面增加2张卡片（行业指数日线、概念指数日线）
- 补采API增加 `INDUSTRY_INDEX_DAILY` / `CONCEPT_INDEX_DAILY` 分支
