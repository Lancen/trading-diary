# ADR-0001: 指数日线独立表存储

## 状态

已接受（修订2）

## 日期

2026-05-24

## 语境

项目需要存储三类指数的日线数据：
- **宽基指数**（MARKET）：上证指数、沪深300、中证500 等，来源 `stock_zh_index_daily`
- **行业指数**（INDUSTRY）：银行、半导体 等 ~446 个，来源 `stock_board_industry_index_ths`
- **概念指数**（CONCEPT）：ChatGPT、新能源车 等 ~400+ 个，来源 `stock_board_concept_index_ths`

现有 `index_daily` 表是项目早期创建的空表，尚未接入采集管线。将新数据写入该表会影响采集状态追踪的清晰性——采集Hub按数据类型跟踪状态，表与采集类型应一一对应。

三类指数标识体系不同：宽基有标准交易所代码（`sh000001`），行业/概念用 `sector_code` 标识（关联 `industry.code`/`concept.code`）。数量级差异大（宽基8 vs 行业446 vs 概念400+）。

## 决策

三类指数各用独立表存储，`index_daily` 保持原样不动：
- **`market_index_daily`**：新增，存宽基指数日线，`index_code` 为标准交易所代码
- **`sector_index_daily`**：新增，存行业/概念指数日线，`sector_type` 区分 INDUSTRY/CONCEPT，`sector_code` 为板块编码（关联 `industry.code`/`concept.code`）
- **`index_daily`**：保持不变，不写入新数据

采集拆为 3 个独立类型：`MARKET_INDEX_DAILY`、`INDUSTRY_INDEX_DAILY`、`CONCEPT_INDEX_DAILY`，各自独立触发、互不影响。

## 理由

### 选择三表独立存储的原因

1. **采集状态隔离**：采集Hub按数据类型追踪采集状态，表与采集类型一一对应，避免共享表导致状态混淆
2. **标识体系不同**：宽基指数有标准交易所代码（`sh000001`），行业/概念指数用 `sector_code` 标识（关联 `industry.code`/`concept.code`），混入会污染字段语义
3. **数量级差异**：宽基 5-8 个 vs 行业 446 个 vs 概念 400+ 个，混入后宽基查询必须带过滤条件
4. **查询隔离**：宽基指数查询（大盘概览）、行业查询、概念查询是不同场景，分表避免互相干扰
5. **前端组件可复用**：K线+两融叠加图组件接收统一的 OHLCV 数据格式，不关心数据来自哪张表，分表不影响组件复用
6. **`index_daily` 不动**：避免修改已有表结构影响现有代码

### 选择行业/概念合表（而非三表各一）的原因

1. **数据结构完全一致**：行业和概念指数返回相同的 7 字段，无结构差异
2. **查询模式相同**：都是"按编码查K线+板块两融"，代码逻辑可复用
3. **`sector_type` 区分即可**：行业和概念编码体系独立，`sector_type` + `sector_code` 组合唯一

### 选择 3 个独立采集类型的原因

1. **来源接口不同**：三个 AKShare 接口参数格式不同，无法统一调用
2. **失败隔离**：行业接口失败不影响宽基采集
3. **频率灵活**：宽基每日必采，行业/概念可按需降低频率

### 被否决的方案

| 方案 | 否决理由 |
|------|---------|
| 统一表 `index_daily` + `index_type` | `index_code` 语义被污染，唯一键需改造，宽基查询必须带过滤条件，行业数据占99%干扰主查询 |
| `index_daily` 存宽基 + `sector_index_daily` 存行业/概念 | 共享 `index_daily` 会影响采集状态追踪的清晰性，表与采集类型应一一对应 |
| 三张表（`index_daily` / `industry_index_daily` / `concept_index_daily`） | 行业和概念数据结构、查询模式完全一致，分两表无必要，多一张表多一份维护 |

## 后果

- `index_daily` 表保持不变，不写入新数据
- 新增 `market_index_daily` 表，结构与 `index_daily` 类似但独立
- 新增 `sector_index_daily` 表，含 `sector_type` + `sector_code` 字段
- 前端图表组件需根据数据来源选择查询不同API，但组件内部渲染逻辑统一
- 未来新增板块类型（如申万行业指数）只需新增 `sector_type` 枚举值
