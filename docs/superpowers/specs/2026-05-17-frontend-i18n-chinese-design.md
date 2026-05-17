# 前端页面中文化

**日期**: 2026-05-17 | **状态**: 待实施

## 背景

现有前端页面中文字混用，采集管理页面状态枚举和多处标签为英文。用户要求所有页面内容统一为中文。

## 方案

直接替换硬编码英文字符串为中文，不引入 i18n 框架。

## 改动范围

### 1. `frontend/src/app/(dashboard)/layout.tsx` — 侧边栏和顶栏

| 当前 | 改为 |
|------|------|
| `Dashboard` | 控制台 |
| `Data Collection` | 数据采集 |
| `Collection Status` | 采集状态 |
| `Margin Completeness` | 两融完整性 |
| `Logout`（侧边栏 + 下拉菜单） | 退出登录 |

### 2. `frontend/src/app/(dashboard)/admin/collection/page.tsx` — 采集状态页

| 当前 | 改为 |
|------|------|
| `NOT_TRIGGERED` | 未触发 |
| `SUCCESS` | 成功 |
| `FAILED` | 失败 |
| `RUNNING` | 运行中 |
| `FETCH` | 采集 |
| `CLEANSE` | 清洗 |
| `Collection Status`（标题） | 采集状态 |
| `Refresh` | 刷新 |
| `Success` / `Failed` / `Running`（统计卡） | 成功 / 失败 / 运行中 |
| `Loading...` | 加载中... |
| `Last: ... \| N records` | 最近: ... \| N 条 |
| `Recent 5 Logs` | 最近 5 条日志 |
| `No logs` | 无日志 |

### 3. `frontend/src/app/(dashboard)/admin/collection/margin/page.tsx` — 两融完整性页

| 当前 | 改为 |
|------|------|
| `COMPLETE` / `PARTIAL` / `MISSING` | 完整 / 部分缺失 / 未采集 |
| `Margin Data Completeness`（标题） | 两融数据完整性 |
| `Start Date` / `End Date` | 开始日期 / 结束日期 |
| `Exchange` | 交易所 |
| `SSE` / `SZSE`（交易所下拉 label） | 上交所 / 深交所 |
| `Check Gaps` / `Checking...` | 检测缺口 / 检查中... |
| `Complete` / `Partial` / `Missing`（统计） | 完整 / 部分缺失 / 未采集 |
| `Week` / `Expected` / `Collected` / `Missing Dates` / `Status` / `Action` | 周 / 应采 / 已采 / 缺失日期 / 状态 / 操作 |
| `No trading days in range` | 此范围无交易日 |
| `MARGIN_DAILY_SSE` / `MARGIN_DAILY_SZSE`（下拉 label） | 上交所两融 / 深交所两融 |

### 不改的内容

- "Trading Diary" — 应用品牌名，保留英文
- 后端 API 路径和 JSON key
- URL 路由
- 已为中文的文本（登录页、Dashboard 页）
