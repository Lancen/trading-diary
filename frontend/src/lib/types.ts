/**
 * 前端集中类型定义，从各页面内联类型抽取合并
 */

// ============================================================
// 板块列表 — 概念/行业共用（原 concepts/page.tsx / industries/page.tsx 的 Item）
// ============================================================

/** 板块列表项（概念/行业通用） */
export interface SectorItem {
  code: string;
  name: string;
  stockCount: number;
  marginBalance: number;
  marginChange: number;
  shortBalance: number;
  shortChange: number;
  volumePct: number | null;
  snapDate: string | null;
  pinned: boolean;
  pinOrder: number | null;
}

// ============================================================
// 板块排序字段 — 概念/行业列表页共用
// ============================================================

export type SectorSortField =
  | "stockCount"
  | "marginBalance"
  | "marginChange"
  | "shortBalance"
  | "shortChange"
  | "volumePct";

// ============================================================
// 板块详情 — 概念/行业详情页共用
// ============================================================

/** 板块指数日线数据 */
export interface SectorIndexDaily {
  tradeDate: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
  amount: number;
  changePct: number;
  volumePct: number | null;
}

/** 板块两融总量日线数据 */
export interface SectorMarginDaily {
  tradeDate: string;
  marginBalance: number;
  shortBalance: number;
  totalBalance: number;
  marginBalanceChange: number | null;
  shortBalanceChange: number | null;
  totalBalanceChange: number | null;
}

/** 板块详情页成分股 */
export interface SectorStockItem {
  stockCode: string;
  stockName: string;
}

// ============================================================
// 板块排名 — rankings/page.tsx
// ============================================================

export interface RankingItem {
  sectorType: string;
  sectorCode: string;
  sectorName: string;
  tradeDate: string;
  amount: number | null;
  amountChange: number | null;
  changePct: number | null;
  changePctChange: number | null;
  volumePct: number | null;
}

export type SectorType = "INDUSTRY" | "CONCEPT";

export type RankingSortField = "amount" | "amountChange" | "changePct" | "changePctChange";

// ============================================================
// 股票列表 — stocks/page.tsx
// ============================================================

export interface StockListItem {
  stockCode: string;
  stockName: string;
  industry: string | null;
  concepts: string | null;
  close: number;
  changePct: number;
  volume: number;
  marginBalance: number;
  marginChange: number;
  shortBalance: number;
  shortChange: number;
  tradeDate: string;
}

export type StockSortField =
  | "changePct"
  | "volume"
  | "marginBalance"
  | "marginChange"
  | "shortBalance"
  | "shortChange";

// ============================================================
// 股票详情 — stocks/[code]/page.tsx
// ============================================================

/** 股票详情页 K 线原始数据 */
export interface StockKline {
  tradeDate: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

/** 股票详情页两融原始数据 */
export interface StockMargin {
  tradeDate: string;
  marginBalance: number;
  marginChange: number;
  shortBalance: number;
  shortChange: number;
}

/** 股票详情页完整数据 */
export interface StockDetailData {
  stockCode: string;
  stockName: string;
  industry: string;
  concepts: string[];
  latestQuote: {
    open: number;
    high: number;
    low: number;
    close: number;
    volume: number;
    changePct: number;
  };
  latestMargin: {
    marginBalance: number;
    marginBuy: number;
    shortBalance: number;
    totalBalance: number;
  } | null;
  dailyKlines: StockKline[];
  dailyMargins: StockMargin[];
}

// ============================================================
// K 线+两融叠加图 — KlineMarginOverlay 组件
// ============================================================

/** K 线数据点（图表组件输入） */
export interface KlinePoint {
  tradeDate: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

/** 两融数据点（图表组件输入） */
export interface MarginPoint {
  tradeDate: string;
  marginBalance?: number;
  shortBalance?: number;
}

// ============================================================
// 指数分析 — index-analysis/page.tsx
// ============================================================

/** 指数最新行情 */
export interface IndexQuote {
  indexCode: string;
  tradeDate: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
  changePct: number;
}

/** 宽基指数日线原始数据 */
export interface MarketIndexDaily {
  indexCode: string;
  tradeDate: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
  amount: number;
  changePct: number;
}

/** 两融总量（宏观）数据 */
export interface MacroMargin {
  tradeDate: string;
  marginBalance: number;
  shortBalance: number;
}

// ============================================================
// 两融统计 — margin-stats/page.tsx
// ============================================================

/** 两融总量汇总 */
export interface MarginSummary {
  totalMarginBalance: number;
  totalShortBalance: number;
  totalBalance: number;
  stockCount: number;
  tradeDate: string;
}

// ============================================================
// 数据采集 — collection/page.tsx + collection/[dataType]/page.tsx
// ============================================================

/** 采集任务状态 */
export interface JobStatus {
  status: string | null;
  startedAt: string | null;
  completedAt: string | null;
  recordCount: number | null;
  errorMsg: string | null;
}

/** 采集类型状态概览 */
export interface CollectionStatus {
  dataType: string;
  dataTypeLabel: string;
  lastFetch: JobStatus | null;
  lastCleanse: JobStatus | null;
  lastDataDate: string | null;
}

/** 采集类型简要状态（[dataType] 页面用） */
export interface StatusItem {
  dataType: string;
  lastFetch: JobStatus | null;
  lastDataDate: string | null;
}

/** Cookie 状态 */
export interface CookieStatus {
  hasCookie: boolean;
  cookiePreview: string;
  updatedAt: string | null;
}

/** 交易日历某天 */
export interface CalendarDay {
  date: string;
  tradingDay: boolean;
  hasData: boolean;
  status: "COLLECTED" | "MISSING" | "NON_TRADING";
}

/** 采集日志 */
export interface CollectionLog {
  id: number;
  dataType: string;
  jobType: string;
  status: string;
  recordCount: number | null;
  errorMsg: string | null;
  requestUrl: string | null;
  requestParams: string | null;
  remark: string | null;
  startedAt: string | null;
  completedAt: string | null;
  tradeDate: string | null;
}

// ============================================================
// 成分股管理 — collection/constituents/page.tsx
// ============================================================

/** 成分股文件信息 */
export interface ConstituentFile {
  filename: string;
  fetchedDate: string | null;
  industryCount: number;
  conceptCount: number;
  totalRelations: number;
  imported: boolean;
}

/** 成分股导入结果 */
export interface ImportResult {
  industryRelations: number;
  conceptRelations: number;
}

// ============================================================
// 两融数据完整性 — collection/margin/page.tsx
// ============================================================

/** 周级别数据缺口 */
export interface WeekGap {
  weekStart: string;
  weekEnd: string;
  expectedDays: number;
  collectedDays: number;
  missingDates: string[];
  status: string;
}

/** 缺口报告 */
export interface GapReport {
  weeks: WeekGap[];
  totalWeeks: number;
  completeWeeks: number;
  partialWeeks: number;
  missingWeeks: number;
}

/** 补采表单 */
export interface BackfillForm {
  dataType: string;
  startDate: string;
  endDate: string;
}

// ============================================================
// 板块两融差额 — 概念/行业详情页的 marginDiff 计算结果
// ============================================================

/** 板块两融差额 */
export interface SectorMarginDiff {
  marginBalance: number;
  shortBalance: number;
  totalBalance: number;
  startDate: string;
  endDate: string;
}