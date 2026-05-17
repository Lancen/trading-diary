# 前端页面中文化 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 layout.tsx、collection/page.tsx、margin/page.tsx 中的英文字符串替换为中文

**Architecture:** 直接替换硬编码字符串，不引入 i18n 框架

**Tech Stack:** Next.js 14 / TypeScript / React

---

### Task 1: 侧边栏和顶栏中文化

**Files:**
- Modify: `frontend/src/app/(dashboard)/layout.tsx`

- [ ] **Step 1: 替换侧边栏导航文本**

修改 `frontend/src/app/(dashboard)/layout.tsx`：

```tsx
// 第 60 行: Dashboard → 控制台
Dashboard

// 改为:
控制台
```

```tsx
// 第 64 行: Data Collection → 数据采集
Data Collection

// 改为:
数据采集
```

```tsx
// 第 75 行: Collection Status → 采集状态
Collection Status

// 改为:
采集状态
```

```tsx
// 第 85 行: Margin Completeness → 两融完整性
Margin Completeness

// 改为:
两融完整性
```

```tsx
// 第 95 行和第 116 行: Logout → 退出登录
Logout

// 改为（两处都改）:
退出登录
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/app/\(dashboard\)/layout.tsx
git commit -m "侧边栏和顶栏文本中文化"
```

---

### Task 2: 采集状态页中文化

**Files:**
- Modify: `frontend/src/app/(dashboard)/admin/collection/page.tsx`

- [ ] **Step 1: 替换 StatusIcon 组件中的状态枚举**

```tsx
// 第 34 行: NOT_TRIGGERED → 未触发
if (!status) return <span className="text-gray-400">NOT_TRIGGERED</span>;

// 改为:
if (!status) return <span className="text-gray-400">未触发</span>;
```

```tsx
// 第 35 行: SUCCESS → 成功
if (status === "SUCCESS") return <span className="text-green-600">SUCCESS</span>;

// 改为:
if (status === "SUCCESS") return <span className="text-green-600">成功</span>;
```

```tsx
// 第 36 行: FAILED → 失败
if (status === "FAILED") return <span className="text-red-600">FAILED</span>;

// 改为:
if (status === "FAILED") return <span className="text-red-600">失败</span>;
```

```tsx
// 第 37 行: RUNNING → 运行中
if (status === "RUNNING") return <span className="text-blue-600">RUNNING</span>;

// 改为:
if (status === "RUNNING") return <span className="text-blue-600">运行中</span>;
```

- [ ] **Step 2: 替换页面标题和按钮**

```tsx
// 第 112 行: Collection Status → 采集状态
<h1 className="text-2xl font-bold">Collection Status</h1>

// 改为:
<h1 className="text-2xl font-bold">采集状态</h1>
```

```tsx
// 第 113 行: Refresh → 刷新
<button onClick={fetchStatus} ...>Refresh</button>

// 改为:
<button onClick={fetchStatus} ...>刷新</button>
```

- [ ] **Step 3: 替换统计卡片文本**

```tsx
// 第 116-118 行
<div ...><div className="text-sm text-gray-600">Success</div>...</div>
<div ...><div className="text-sm text-gray-600">Failed</div>...</div>
<div ...><div className="text-sm text-gray-600">Running</div>...</div>

// 改为:
<div ...><div className="text-sm text-gray-600">成功</div>...</div>
<div ...><div className="text-sm text-gray-600">失败</div>...</div>
<div ...><div className="text-sm text-gray-600">运行中</div>...</div>
```

- [ ] **Step 4: 替换加载状态和日志区域文本**

```tsx
// 第 120 行: Loading... → 加载中...
{loading ? <div className="py-8 text-center text-gray-500">Loading...</div> : (

// 改为:
{loading ? <div className="py-8 text-center text-gray-500">加载中...</div> : (
```

```tsx
// 第 131 行: FETCH → 采集
<div><span className="text-gray-500">FETCH: </span>...

// 改为:
<div><span className="text-gray-500">采集: </span>...
```

```tsx
// 第 132 行: CLEANSE → 清洗
<div><span className="text-gray-500">CLEANSE: </span>...

// 改为:
<div><span className="text-gray-500">清洗: </span>...
```

```tsx
// 第 133 行: Last: ... | N records → 最近: ... | N 条
<div className="text-xs text-gray-400">Last: {formatTime(item.lastFetch.completedAt)} | {item.lastFetch.recordCount ?? 0} records</div>

// 改为:
<div className="text-xs text-gray-400">最近: {formatTime(item.lastFetch.completedAt)} | {item.lastFetch.recordCount ?? 0} 条</div>
```

```tsx
// 第 148 行: Recent 5 Logs → 最近 5 条日志
<h4 className="mb-2 text-sm font-medium">Recent 5 Logs</h4>

// 改为:
<h4 className="mb-2 text-sm font-medium">最近 5 条日志</h4>
```

```tsx
// 第 149 行: Loading... / No logs → 加载中... / 无日志
{logsLoading ? <div className="text-xs text-gray-400">Loading...</div> : logs.length === 0 ? <div className="text-xs text-gray-400">No logs</div> : (

// 改为:
{logsLoading ? <div className="text-xs text-gray-400">加载中...</div> : logs.length === 0 ? <div className="text-xs text-gray-400">无日志</div> : (
```

- [ ] **Step 5: 提交**

```bash
git add frontend/src/app/\(dashboard\)/admin/collection/page.tsx
git commit -m "采集状态页文本中文化"
```

---

### Task 3: 两融完整性页中文化

**Files:**
- Modify: `frontend/src/app/(dashboard)/admin/collection/margin/page.tsx`

- [ ] **Step 1: 替换状态枚举**

```tsx
// 第 25-27 行: statusEmoji 函数
function statusEmoji(status: string): string {
  if (status === "COMPLETE") return "COMPLETE";
  if (status === "PARTIAL") return "PARTIAL";
  if (status === "MISSING") return "MISSING";
  return status;
}

// 改为:
function statusEmoji(status: string): string {
  if (status === "COMPLETE") return "完整";
  if (status === "PARTIAL") return "部分缺失";
  if (status === "MISSING") return "未采集";
  return status;
}
```

- [ ] **Step 2: 替换页面标题和筛选区**

```tsx
// 第 202 行: Margin Data Completeness → 两融数据完整性
<h1 className="text-2xl font-bold">Margin Data Completeness</h1>

// 改为:
<h1 className="text-2xl font-bold">两融数据完整性</h1>
```

```tsx
// 第 213 行: Start Date → 开始日期
<label ...>Start Date</label>

// 改为:
<label ...>开始日期</label>
```

```tsx
// 第 217 行: End Date → 结束日期
<label ...>End Date</label>

// 改为:
<label ...>结束日期</label>
```

```tsx
// 第 221 行: Exchange → 交易所
<label ...>Exchange</label>

// 改为:
<label ...>交易所</label>
```

```tsx
// 第 223-224 行: SSE / SZSE → 上交所 / 深交所
<option value="SSE">SSE</option>
<option value="SZSE">SZSE</option>

// 改为:
<option value="SSE">上交所</option>
<option value="SZSE">深交所</option>
```

- [ ] **Step 3: 替换检测按钮**

```tsx
// 第 228 行: Checking... / Check Gaps → 检查中... / 检测缺口
{loading ? "Checking..." : "Check Gaps"}

// 改为:
{loading ? "检查中..." : "检测缺口"}
```

- [ ] **Step 4: 替换统计卡片**

```tsx
// 第 235-237 行
<div ...>Complete: {report.completeWeeks}</div>
<div ...>Partial: {report.partialWeeks}</div>
<div ...>Missing: {report.missingWeeks}</div>

// 改为:
<div ...>完整: {report.completeWeeks}</div>
<div ...>部分缺失: {report.partialWeeks}</div>
<div ...>未采集: {report.missingWeeks}</div>
```

- [ ] **Step 5: 替换表格表头**

```tsx
// 第 244-250 行
<th ...>Week</th>
<th ...>Exchange</th>
<th ...>Expected</th>
<th ...>Collected</th>
<th ...>Missing Dates</th>
<th ...>Status</th>
<th ...>Action</th>

// 改为:
<th ...>周</th>
<th ...>交易所</th>
<th ...>应采</th>
<th ...>已采</th>
<th ...>缺失日期</th>
<th ...>状态</th>
<th ...>操作</th>
```

- [ ] **Step 6: 替换空数据提示**

```tsx
// 第 283 行
<td colSpan={7} ...>No trading days in range</td>

// 改为:
<td colSpan={7} ...>此范围无交易日</td>
```

- [ ] **Step 7: 替换补采弹窗下拉选项**

```tsx
// 第 101-102 行
<option value="MARGIN_DAILY_SSE">MARGIN_DAILY_SSE</option>
<option value="MARGIN_DAILY_SZSE">MARGIN_DAILY_SZSE</option>

// 改为:
<option value="MARGIN_DAILY_SSE">上交所两融</option>
<option value="MARGIN_DAILY_SZSE">深交所两融</option>
```

```tsx
// 第 113-114 行: SSE / SZSE → 上交所 / 深交所
<option value="SSE">SSE</option>
<option value="SZSE">SZSE</option>

// 改为:
<option value="SSE">上交所</option>
<option value="SZSE">深交所</option>
```

- [ ] **Step 8: 提交**

```bash
git add frontend/src/app/\(dashboard\)/admin/collection/margin/page.tsx
git commit -m "两融完整性页文本中文化"
```

---

### Task 4: 验证

- [ ] **Step 1: 编译检查**

```bash
cd frontend && pnpm build
```
Expected: BUILD SUCCESSFUL，无 TypeScript 错误

- [ ] **Step 2: 运行 lint**

```bash
cd frontend && pnpm lint
```
Expected: PASS，无新增 lint 错误
