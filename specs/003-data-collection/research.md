# 技术研究报告：数据采集层

**Feature**: 003-data-collection  
**Date**: 2026-05-13

## 研究项

### R1: 外部数据源选择

**Decision**: 单一使用 Akshare，不接入 Tushare

**Rationale**: Tushare 账户仅 124 积分，`stock_basic`（股票基础信息）、`margin`（两融汇总）、`margin_detail`（两融明细）、`margin_secs`（两融标的）全部需要 2000 积分。Akshare 免费覆盖全部所需数据。

**Alternatives considered**: Akshare + Tushare 双源。否决原因：Tushare 积分门槛无法满足。

---

### R2: Python-Java 桥接方案

**Decision**: AKTools 官方 Docker 镜像 (`registry.cn-shanghai.aliyuncs.com/akfamily/aktools:1.8.95`)，Java 通过 HTTP 调用

**Rationale**: AKTools 基于 FastAPI，将 Akshare 所有函数自动映射为 REST 端点（`/api/public/{function_name}?param=value`）。Docker 一键部署，无需自建 Python 服务。

**Alternatives considered**:
- 自建 Python FastAPI 桥接：增加维护成本，AKTools 已满足需求
- Java 直调东方财富/交易所 API：需要自己逆向维护，数据源变动易断裂
- AkShare-One (LLM 专用简化接口)：功能覆盖面不如 AKTools 完整

---

### R3: 采集架构模式

**Decision**: FETCH（原始 JSON → raw_data）→ CLEANSE（解析 → 清洗 → 业务表）两步流水线

**Rationale**: 两步分离使得原始数据可追溯、失败可单独重试清洗步骤、支持未来数据重洗。两步各有独立的 `data_collection_log` 状态记录。

**Alternatives considered**:
- 直写模式（FETCH → 直接写入业务表）：简单但失败时无法重洗，数据质量问题难以排查
- 流式处理（边采边洗）：代码复杂度高，不适合批量采集场景

---

### R4: 历史回填策略

**Decision**: 按周分片回填，基于交易日历做缺口检测，支持断点续传

**Rationale**: 2010 年至今约 3900 个交易日，按周分片（每片 ~5 个交易日）产生约 780 个周片。每片独立记录到 `data_collection_log`（含 `week_start`/`week_end`），中断后已完成周片不会重复执行。

**Alternatives considered**:
- 按日逐条回填：进度粒度最细但日志量巨大
- 按月分片：粒度太粗，中断回退成本高

---

### R5: 并发控制方案

**Decision**: 基于 `ConcurrentHashMap<String, ReentrantLock>` 的 per-key 内存锁

**Rationale**: 同一 `(data_type, trade_date)` 不允许并发执行。定时任务与手动触发可能冲突，`tryLock()` 非阻塞获取，已执行中则返回提示。内存锁足够（单实例部署，不涉及分布式锁）。

**Alternatives considered**:
- 数据库悲观锁（`SELECT ... FOR UPDATE`）：可靠但增加 DB 压力
- Redis 分布式锁：当前阶段过度设计，无分布式部署需求

---

### R6: raw_data 归档策略

**Decision**: 保留 30 天在 DB，超期按月 GZIP 导出为 JSON Lines 文件，然后物理删除

**Rationale**: `stock_zh_a_spot_em` 单次约 5MB JSON，日均 ~20MB，年增长 ~7GB。保留 30 天窗口满足绝大部分排查需求，归档文件可永久保留。

**Alternatives considered**:
- DB 永久保留：存储膨胀，查询变慢
- 无归档直接删除：数据问题无法回溯原始终态

---

### R7: 行业/概念分类变化检测

**Decision**: 每日 CLEANSE 时与数据库当前状态对比，仅写入差异，变化记录到 `classification_change_log`

**Rationale**: 行业分类变动频率低（月度/季度），概念变动频率较高但仍远低于每日。全量每日写入会产生大量冗余数据。差异检测 + 变更日志实现了高效存储和历史可追溯。

**Alternatives considered**:
- 每日全量快照：简单但每天产生 5000+ 行冗余数据
- 只保留最新无历史：丢失分类变化历史，后续聚合分析无法确定历史归属

---

### R8: 股票日线增量采集

**Decision**: 首次全量回填用 `stock_zh_a_hist(symbol, start, end)` 一次性获取每只股全部历史；日常增量从 `stock_zh_a_spot_em` 提取当日 OHLC

**Rationale**: `stock_zh_a_hist` 单次调用返回多年数据，但需要遍历 5000+ 只股票。日常增量如果继续逐只调 `stock_zh_a_hist`，5000 次调用只产出一行/只——极端低效。改用 `stock_zh_a_spot_em` 一次调用获取全市场当日快照，其中包含 OHLC 数据。

**Alternatives considered**: 每日全量调用 `stock_zh_a_hist`。否决原因：5000 次 HTTP 调用产出 5000 行，ROI 极低。

---

### R9: 两融标的来源验证

**Decision**: 从 `stock_margin_detail_sse/szse` CLEANSE 时提取出现过的股票代码

**Rationale**: `stock_zh_a_spot_em` 的 23 个字段中不含两融标记。交易所每日公布的个股两融明细天然限定了两融标的范围——只有两融标的数据才会出现。CLEANSE 时收集所有出现过的新 `stock_code` 即可建立标的列表。

**Alternatives considered**:
- 期待 AKShare 有单独的两融标的接口：已验证不存在
- 从 stock_info 关联判断：无可用字段
