// --- 拥挤度 ---
export interface CrowdednessDaily {
  tradeDate: string;
  crowdedness: number;
  totalAmount: number;
  topAmount: number;
  totalStocks: number;
  topStocks: number;
}

// --- 板块排名 ---
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

// --- 股票列表 ---
export interface StockItem {
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

// --- 股票详情 ---
export interface Kline {
  tradeDate: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

export interface Margin {
  tradeDate: string;
  marginBalance: number;
  marginChange: number;
  shortBalance: number;
  shortChange: number;
}

export interface DetailData {
  stockCode: string;
  stockName: string;
  industry: string;
  concepts: string[];
  latestQuote: { open: number; high: number; low: number; close: number; volume: number; changePct: number };
  latestMargin: { marginBalance: number; marginBuy: number; shortBalance: number; totalBalance: number } | null;
  dailyKlines: Kline[];
  dailyMargins: Margin[];
}

// --- 融资统计 ---
export interface MarginSummary {
  totalMarginBalance: number;
  totalShortBalance: number;
  totalBalance: number;
  stockCount: number;
  tradeDate: string;
}

// --- 指数分析 ---
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

export interface MacroMargin {
  tradeDate: string;
  marginBalance: number;
  shortBalance: number;
}

// --- 板块列表（行业/概念） ---
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

// --- 板块指数日线 ---
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

// --- 板块两融日线 ---
export interface SectorMarginDaily {
  tradeDate: string;
  marginBalance: number;
  shortBalance: number;
  totalBalance: number;
  marginBalanceChange: number | null;
  shortBalanceChange: number | null;
  totalBalanceChange: number | null;
}

// --- 成分股 ---
export interface ConstituentStockItem {
  stockCode: string;
  stockName: string;
}

// --- 采集状态 ---
export interface JobStatus {
  status: string;
  completedAt: string | null;
  recordCount: number | null;
  errorMsg: string | null;
}

export interface CollectionStatus {
  dataType: string;
  dataTypeLabel: string;
  lastFetch: JobStatus | null;
  lastCleanse: JobStatus | null;
  lastDataDate: string | null;
}

export interface CookieStatus {
  hasCookie: boolean;
  cookiePreview: string;
  updatedAt: string | null;
}

// --- API 响应 ---
export interface ApiResponse<T> {
  code: number;
  data: T;
  message?: string;
  timestamp?: string;
}

export interface PaginatedData<T> {
  records: T[];
  total: number;
}
