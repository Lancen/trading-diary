
# trading-diary

A 股全市场数据采集与管理平台 —— 覆盖股票行情、日线、两融、行业/概念板块，支持历史补采和交易日历监控。

**当前阶段：Phase 1（业务开发）**

## 技术栈

| 层 | 选型 |
|------|------|
| 语言 | Java 17+ |
| 框架 | Spring Boot 3.2.x |
| ORM | MyBatis-Plus 3.5+ |
| 数据库 | MySQL 8.0 |
| 构建 | Gradle |
| 前端 | Next.js 14+ / TypeScript / TailwindCSS / shadcn/ui |

完整技术栈见 [CLAUDE.md](CLAUDE.md)。

## 快速开始

```bash
# 环境检查
scripts/check-env.sh

# AKTools（行情数据源）
pip3 install aktools
python3 -m aktools --host 127.0.0.1 --port 8081 &

# 后端（必须激活 dev profile，否则 JWT 配置不加载）
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun

# 前端
cd frontend && pnpm install && pnpm dev
```

验证：

```bash
curl http://localhost:8081/version          # AKTools 健康
curl http://localhost:8080/actuator/health  # 后端健康
curl http://localhost:3000                   # 前端健康
```

## 数据采集：首次启动完整流程

> 管理后台 `http://localhost:3000/admin/collection` 提供可视化采集界面，以下用 API 示例说明执行顺序。

### 前置依赖

```
交易日历 ──→ 股票行情 ──→ 日线行情（复用股票行情的 API 返回）
                │
                ├──→ 行业/概念名称 ──→ 行业/概念成分股（Playwright 抓取 → 后台导入）
                │
                └──→ 两融明细(沪/深) + 两融总量(沪/深)
```

**关键规则**：`STOCK_DAILY` 不独立采集，它复用 `STOCK_INFO` 的同一份 API 返回数据。先采 STOCK_INFO，STOCK_DAILY 自动联动入库。

### 第 1 步：交易日历

所有日级采集都依赖交易日历判断数据完整性。先拉取 A 股历史交易日（1990 年至今），后续增量更新。

```bash
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/TRADE_CALENDAR
```

### 第 2 步：股票行情（含日线）

全市场实时行情快照（代码、名称、价格、涨跌幅、PE/PB/市值等 15 字段），数据源新浪 `stock_zh_a_spot`。
**同一份 API 返回同时写入 `stock_info` 和 `stock_daily` 两张表**。

```bash
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/STOCK_INFO
```

> **历史日线回填**：`stock_daily` 支持按个股 + 日期范围补采（数据源腾讯 `stock_zh_a_hist_tx`）。管理后台 → 股票行情详情页 → "历史补采" → 选择日期范围。

### 第 3 步：行业 + 概念板块

```bash
# 同花顺行业分类（~90 个）
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/INDUSTRY_NAME

# 同花顺概念分类（~370 个）
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/CONCEPT_NAME
```

### 第 4 步：行业/概念成分股

数据源为同花顺页面，需通过 Playwright 抓取。

```bash
# 抓取全部行业+概念成分股，保存到 data/constituents/constituents-YYYY-MM-DD.json
# 耗时约 20 分钟
python3 scripts/scrape_ths_constituents.py

# 调试/快速验证：仅抓取 5 个概念
python3 scripts/scrape_ths_constituents.py --concept --limit 5
```

抓取完成后，管理后台 → 数据采集 Hub → 底部"成分股数据"卡片 → 点击进入管理页 → **导入**。每月执行一次即可。

### 第 5 步：两融数据

```bash
# 上交所个股两融明细
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/MARGIN_DAILY_SSE

# 深交所个股两融明细
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/MARGIN_DAILY_SZSE

# 上交所两融总量
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/MARGIN_MACRO_SSE

# 深交所两融总量
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/MARGIN_MACRO_SZSE
```

两融数据支持按日期补采（个股级 + 全市场级），通过管理后台 → 各采集详情页 → "触发采集"。

### 采集顺序总结

```
(1) 交易日历  ──→  (2) 股票行情（自动联动日线）  ──→  (3) 行业 + 概念名称
                                                       ↓
                                                (4) 行业/概念成分股（Playwright → 导入）
                                                       ↓
                                                (5) 两融明细 + 两融总量（沪/深各 2 个）
```

### 日常增量采集

全部通过管理后台 `http://localhost:3000/admin/collection` 操作：

| 频率 | 采集类型 | 管理后台入口 |
|------|---------|-------------|
| 每日 | 股票行情 | Hub → 股票行情卡片 → 触发采集 |
| 每日 | 两融明细/总量 | Hub → 对应卡片 → 触发采集 |
| 每月 | 行业/概念名称 | Hub → 对应卡片 → 触发采集 |
| 每月 | 成分股抓取+导入 | `python3 scripts/scrape_ths_constituents.py` → 后台导入 |

> 定时任务（`CollectionScheduler.java`）已暂停，后续取消注释即可自动运行。

### 数据完整性监控

管理后台每个采集详情页都带**交易日历**（月视图，三态标记）：
- 🟢 绿色 = 已采集
- 🔴 红色 = 交易日缺数据（可点击触发补采）
- ⬜ 灰色 = 非交易日

## 管理后台页面

| 页面 | 路径 | 功能 |
|------|------|------|
| 数据采集 Hub | `/admin/collection` | 8 张采集类型卡片 + 成分股管理入口 |
| 采集详情 | `/admin/collection/[dataType]` | 管线状态 + 触发采集 + 交易日历 + 日志表 |
| 成分股管理 | `/admin/collection/constituents` | 文件列表 + 导入操作 |
| 股票数据 | `/admin/stocks` | 筛选/排序/分页浏览 |
| 股票详情 | `/admin/stocks/[code]` | K 线图 + 两融叠加 + 日线明细 |
| 概念列表 | `/admin/concepts` | 概念维度两融聚合 |
| 行业列表 | `/admin/industries` | 行业维度两融聚合 |
| 融资统计 | `/admin/margin-stats` | 全市场两融总量 |

## 数据源对照

| 数据类型 | 数据源 | 采集方式 | 频率 |
|---------|--------|---------|------|
| 交易日历 | 新浪 `tool_trade_date_hist_sina` | AKTools HTTP | 不定期 |
| 股票行情 | 新浪 `stock_zh_a_spot` | AKTools HTTP | 日级 |
| 股票日线 | 腾讯 `stock_zh_a_hist_tx` | AKTools HTTP（复用 STOCK_INFO 返回 + 历史补采） | 日级 |
| 行业/概念名称 | 同花顺 `stock_board_*_name_ths` | AKTools HTTP | 月级 |
| 行业/概念成分股 | 同花顺页面 `q.10jqka.com.cn` | Playwright 抓取 → 后台导入 | 月级 |
| 两融明细 | 上交所/深交所 `stock_margin_detail_sse/szse` | AKTools HTTP | 日级 |
| 两融总量 | 上交所/深交所 `macro_china_market_margin_sh/sz` | AKTools HTTP | 日级 |

## 验证

```bash
# 各采集类型状态
curl http://localhost:8080/api/v1/admin/collection/status

# 交易日历覆盖度（默认查股票行情，支持 ?dataType=MARGIN_DAILY_SSE 等）
curl "http://localhost:8080/api/v1/admin/stocks/calendar?year=2026&month=5"

# 两融数据完整性
curl "http://localhost:8080/api/v1/admin/collection/gaps?start=2026-01-01&end=2026-05-17&exchange=SSE"

# AKTools 交易日历数量
curl http://localhost:8081/api/public/tool_trade_date_hist_sina | python3 -c "import sys,json; print(f'{len(json.load(sys.stdin))} 条交易日')"
```

## 项目结构

```
trading-diary/
├── src/main/java/com/tradingdiary/   # 后端
│   └── collection/                    #   采集管线（client / orchestrator / scheduler / controller）
├── frontend/                          # 前端（Next.js 14 App Router）
│   └── e2e/                           #   E2E 测试（Playwright, 10 用例）
├── specs/                             # 功能规范
│   └── 003-data-collection/           #   数据采集 UI/UE 重构
├── docs/
│   ├── standards/                     # 技术规范
│   └── superpowers/                   # 开发流程
├── scripts/                           # 工具脚本
└── data/                              # 采集数据文件（constituents JSON）
```

## 文档

| 文档 | 说明 |
|------|------|
| [CLAUDE.md](CLAUDE.md) | AI 编码准则 |
| [CONTEXT.md](CONTEXT.md) | 领域词汇表 |
| [项目宪法](specs/_governance/constitution.md) | 工程决策原则 |
| [技术规范](docs/standards/technical-standards.md) | 代码标准、数据库、API、安全等 |
| [采集管线规则](.claude/rules/collection-pipeline.md) | 采集类型分级、日历路由、表注意事项 |
| [Feature 状态](specs/_feature-status.md) | 当前 feature 进度 |

## 工程原则

1. **约定优于配置** — 优先使用 Spring Boot 默认约定
2. **分层不可逆** — Controller → Service → Mapper
3. **防御式编程** — 所有外部输入验证
4. **渐进式优化** — 先正确，再优化
