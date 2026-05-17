
# trading-diary

交易日记应用 —— 记录和管理实盘交易。

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

# AKTools（行情数据源，需先 pip3 install aktools）
python3 -m aktools --host 127.0.0.1 --port 8081 &

# 后端
./gradlew bootRun --args='--spring.profiles.active=dev'

# 前端
cd frontend && pnpm install && pnpm dev
```

AKTools 提供 1000+ 行情数据接口，是数据采集模块的依赖。启动后验证：

```bash
curl http://localhost:8081/version
```

## 基础数据准备

数据采集模块依赖 AKTools（HTTP API）和 Playwright（同花顺页面抓取）。首次启动需按顺序初始化以下数据：

### 前置条件

- MySQL 8.0+ 运行中，已创建数据库
- `.env` 文件配置好数据库连接（`DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USER`、`DB_PASSWORD`）
- JDK 17+，`JAVA_HOME` 指向正确版本
- AKTools 已启动（`python3 -m aktools --host 127.0.0.1 --port 8081 &`）
- Playwright 已安装（`pip3 install playwright && python3 -m playwright install chromium`）
- 后端已启动（Flyway 会自动建表）

### 采集顺序

数据有依赖关系，首次采集必须按以下顺序执行：

```
(1) 交易日历  ──→  (2) 股票基础信息 + 日线行情  ──→  (3) 行业/概念名称
                                                     ↓
                                              (4) 行业/概念成分股
                                                     ↓
                                              (5) 两融交易明细
```

#### 1. 交易日历

```bash
# 全量拉取 A 股历史交易日（1990 年至今），后续增量更新
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/TRADE_CALENDAR
```

#### 2. 股票基础信息 + 日线行情

```bash
# 全 A 股实时行情快照（代码、名称、最新价、涨跌幅、OHLCV 等），数据源：新浪
# STOCK_DAILY 复用同一份数据，无需单独采集
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/STOCK_INFO

# 历史日线回填（全量 5000+ 只，耗时约 30 分钟），数据源：腾讯
# 通过管理后台 → 补采弹窗 → 选择 STOCK_DAILY → 设置日期范围
```

#### 3. 行业 / 概念板块名称

```bash
# 同花顺行业分类（~90 个）
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/INDUSTRY_NAME

# 同花顺概念分类（~370 个）
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/CONCEPT_NAME
```

#### 4. 行业 / 概念成分股

**数据源为同花顺页面，需通过 Playwright 抓取（非实时 API）。**

```bash
# 抓取全部行业+概念成分股，自动保存到 data/constituents/constituents-YYYY-MM-DD.json
# 耗时约 20 分钟
python3 scripts/scrape_ths_constituents.py

# 仅抓取概念（调试/快速验证）
python3 scripts/scrape_ths_constituents.py --concept --limit 5
```

抓取完成后，在管理后台 "采集状态" 页面（`/admin/collection`）底部的 "成分股数据" 区域：
- 可以看到已抓取的文件列表（采集日期、板块数、关系总数）
- 点击 **导入** 按钮将数据写入 `stock_industry` / `stock_concept` 表

每月执行一次抓取 + 导入即可。

#### 5. 两融交易明细

```bash
# 上交所个股两融明细（融资余额/买入额/偿还额、融券余量/卖出量/偿还量）
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/MARGIN_DAILY_SSE

# 深交所个股两融明细
curl -X POST http://localhost:8080/api/v1/admin/collection/trigger/MARGIN_DAILY_SZSE

# 历史两融回填（按周分片断点续传）
# 通过管理后台 → 两融完整性 → 补采弹窗 → 选择数据+交易所+日期范围
```

### 日常增量采集

当前阶段全部使用手动触发。后端管理后台 (`/admin/collection`) 提供一键采集按钮。

| 频率 | 任务 | 手动触发命令 |
|------|------|-------------|
| 每日 | 股票行情快照 | `POST /api/v1/admin/collection/trigger/STOCK_INFO` |
| 每日 | 行业/概念名称 | `POST .../trigger/INDUSTRY_NAME` + `CONCEPT_NAME` |
| 每日 | 两融明细 | `POST /api/v1/admin/collection/trigger/MARGIN_DAILY_SSE` 等 |
| 每月 | 成分股更新 | `python3 scripts/scrape_ths_constituents.py -o data/constituents.json` |

> 定时任务（`@Scheduled`）已在 `CollectionScheduler.java` 中注释暂停，后续需要自动运行时取消注释即可。

### 数据源对照

| 数据类型 | 数据源 | 采集方式 |
|---------|--------|---------|
| 交易日历 | 新浪 (`tool_trade_date_hist_sina`) | AKTools HTTP |
| 股票行情 | 新浪 (`stock_zh_a_spot`) | AKTools HTTP |
| 股票日线 | 腾讯 (`stock_zh_a_hist_tx`) | AKTools HTTP |
| 行业/概念名称 | 同花顺 (`stock_board_*_name_ths`) | AKTools HTTP |
| 行业/概念成分股 | 同花顺 (`q.10jqka.com.cn`) | Playwright 月级 → 后台导入 |
| 两融明细 | 上交所/深交所 (`stock_margin_detail_sse/szse`) | AKTools HTTP |

### 验证

```bash
# 查看各数据类采集状态
curl http://localhost:8080/api/v1/admin/collection/status

# 查看两融数据完整性
curl "http://localhost:8080/api/v1/admin/collection/gaps?start=2026-01-01&end=2026-05-17&exchange=SSE"

# 检查 AKTools 健康
curl http://localhost:8081/api/public/tool_trade_date_hist_sina | python3 -c "import sys,json; print(f'{len(json.load(sys.stdin))} 条交易日')"
```

## 项目结构

```
trading-diary/
├── src/main/java/com/tradingdiary/   # 后端
├── frontend/                          # 前端
├── specs/                             # 功能规范
│   └── _feature-status.md             # 当前 feature 进度
├── docs/
│   ├── standards/                     # 技术规范
│   └── superpowers/                   # 开发流程
├── scripts/                           # 工具脚本
└── e2e/                               # E2E 测试
```

## 文档

| 文档 | 说明 |
|------|------|
| [CLAUDE.md](CLAUDE.md) | AI 编码准则 |
| [项目宪法](specs/_governance/constitution.md) | 工程决策原则 |
| [技术规范](docs/standards/technical-standards.md) | 代码标准、数据库、API、安全等 |
| [开发流程](docs/superpowers/speckit-superpowers-workflow.md) | Speckit + Superpowers 混合流程 |
| [Feature 状态](specs/_feature-status.md) | 当前 feature 进度 |

## 工程原则

1. **约定优于配置** — 优先使用 Spring Boot 默认约定
2. **分层不可逆** — Controller → Service → Mapper
3. **防御式编程** — 所有外部输入验证
4. **渐进式优化** — 先正确，再优化
