# 003-data-collection 变更记录

## 2026-05-17：数据源紧急迁移

**触发原因**：东方财富 `push2.eastmoney.com` API 全面封锁（错误码 `rc:102`），所有 `_em` 后缀的 akshare 接口失效。采集模块无法获取股票行情、日线、板块分类及成分股数据。

**变更类型**：运维适配（非新 feature，未走 speckit 流程）

**变更人**：liangcaizhan + Claude Code

### 数据源迁移

| 数据类型 | 原数据源 | 新数据源 | 采集方式 |
|---------|---------|---------|---------|
| 股票行情 | 东方财富 `stock_zh_a_spot_em` | 新浪 `stock_zh_a_spot` | AKTools HTTP |
| 股票日线 | 东方财富 `stock_zh_a_hist` | 腾讯 `stock_zh_a_hist_tx` | AKTools HTTP |
| 行业名称 | 东方财富 `stock_board_industry_name_em` | 同花顺 `stock_board_industry_name_ths` | AKTools HTTP |
| 概念名称 | 东方财富 `stock_board_concept_name_em` | 同花顺 `stock_board_concept_name_ths` | AKTools HTTP |
| 行业成分股 | 东方财富 `stock_board_industry_cons_em` | 同花顺 `q.10jqka.com.cn/thshy/detail/` | Playwright 月级抓取 |
| 概念成分股 | 东方财富 `stock_board_concept_cons_em` | 同花顺 `q.10jqka.com.cn/gn/detail/` | Playwright 月级抓取 |
| 两融明细 | 上交所/深交所 | 不变 | AKTools HTTP |
| 交易日历 | 新浪 | 不变 | AKTools HTTP |

### 代码变更

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `AKToolsClient.java` | 修改 | 切换 4 个 API 端点；`fetchIndustryCons`/`fetchConceptCons` 标记 `@Deprecated` |
| `StockDailyCleanseService.java` | 修改 | 适配腾讯日线 API 英文字段名（`date/open/close/high/low`） |
| `IndustryCleanseService.java` | 修改 | 字段名从 `板块代码/板块名称` 改为 `code/name` |
| `ConceptCleanseService.java` | 修改 | 同上 |
| `CollectionOrchestrator.java` | 修改 | 成分股清洗改为依赖 Playwright 预导入；所有返回消息中文化 |
| `CollectionController.java` | 修改 | API 校验错误消息中文化；新增成分股文件管理接口 |
| `CollectionScheduler.java` | 修改 | 全部 `@Scheduled` 注释暂停 |
| `ConstituentImportService.java` | 新增 | 读取 Playwright JSON 导入 `stock_industry`/`stock_concept` |
| `scrape_ths_constituents.py` | 新增 | Playwright 抓取同花顺行业+概念成分股 |
| `collection/page.tsx` | 修改 | 中文化 + 成分股文件列表 + 导入按钮 |
| `layout.tsx` | 修改 | 侧边栏中文化 |
| `margin/page.tsx` | 修改 | 两融完整性页中文化 |

### 影响评估

**功能影响**：
- 新浪 `stock_zh_a_spot` 不返回 PE/PB/总市值/流通市值/换手率/量比，`stock_info` 表这些字段为 NULL
- 成分股不再每日自动更新，改为月级手动执行 Playwright 脚本 + 管理后台导入
- 行业/概念分类体系从东方财富切换为同花顺（代码格式变化：`BK0740` → `881121`）
- 全部定时任务暂停，改为管理后台手动触发

**性能影响**：
- AKTools HTTP 接口调用量不变（~6 个接口）
- Playwright 抓取：~90 行业 × 1-18 页 + ~362 概念 × 1-5 页，预估 20 分钟

### 回滚方案

若需恢复东方财富数据源：
1. 确认东方财富 API 已解封
2. 还原 `AKToolsClient.java` 中的 API 端点（`git revert` 相关 commit）
3. 还原 CleanseService 字段名
4. 取消 `@Scheduled` 注释恢复定时任务

---

## 2026-05-17：采集流程优化

**触发原因**：全量采集 5516 条记录耗时过长，STOCK_INFO/STOCK_DAILY 重复调用同一 API。

**变更类型**：性能优化

### 变更内容

| 项 | 原方案 | 新方案 |
|----|--------|--------|
| 股票行情+日线触发 | 两个独立按钮，各自 FETCH | 合并为"股票行情（含日线）"，一次 FETCH → 两张表 CLEANSE |
| 采集执行方式 | 同步 HTTP 阻塞等待（2-5 分钟） | 异步后台线程，API 立即返回"任务已提交" |
| STOCK_INFO CLEANSE | 5516 条逐条 SQL | 分类后按 500 条分批写入 |
| FETCH 复用 | 每次触发重新调 API | CLEANSE 失败后重试不重复 FETCH，复用已有 raw_data |
| 行业/概念成分股触发 | 按钮可用但空转 | 按钮移除，数据只通过 Playwright 导入 |
| 两融 CLEANSE 事务 | 两张表各自提交 | `@Transactional` 原子写入 |

### 影响

- 前端采集按钮从 9 个减为 6 个
- STOCK_INFO 触发后自动联动 STOCK_DAILY，无需单独操作
- 异步执行解决 HTTP 超时导致的"采集成功但显示失败"问题
