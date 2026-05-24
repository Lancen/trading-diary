import { describe, it, expect, beforeEach, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom/vitest";

const { mockGet, mockBack } = vi.hoisted(() => ({
  mockGet: vi.fn(),
  mockBack: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  useParams: () => ({ code: "600000" }),
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), back: mockBack }),
  usePathname: () => "/admin/stocks/600000",
}));

vi.mock("lightweight-charts", () => ({
  createChart: vi.fn(() => ({
    addSeries: vi.fn(() => ({
      setData: vi.fn(),
      priceScale: vi.fn(() => ({ applyOptions: vi.fn() })),
    })),
    timeScale: vi.fn(() => ({ fitContent: vi.fn() })),
    remove: vi.fn(),
  })),
  CandlestickSeries: "CandlestickSeries",
  HistogramSeries: "HistogramSeries",
  LineSeries: "LineSeries",
  ColorType: { Solid: "Solid" },
  CrosshairMode: { Normal: 0 },
}));

vi.mock("@/lib/api", () => ({
  default: { get: (...args: unknown[]) => mockGet(...args) },
}));

import StockDetailPage from "./page";

const mockDetailData = {
  stockCode: "600000",
  stockName: "浦发银行",
  industry: "银行",
  concepts: ["金融", "上证50"],
  latestQuote: {
    open: 10.2,
    high: 10.8,
    low: 10.1,
    close: 10.5,
    volume: 50000000,
    changePct: 1.23,
  },
  latestMargin: {
    marginBalance: 2e9,
    marginBuy: 5e8,
    shortBalance: 3e8,
    totalBalance: 2.3e9,
  },
  dailyKlines: [
    {
      tradeDate: "2025-01-15",
      open: 10.2,
      high: 10.8,
      low: 10.1,
      close: 10.5,
      volume: 50000000,
    },
  ],
  dailyMargins: [
    {
      tradeDate: "2025-01-15",
      marginBalance: 2e9,
      marginChange: 1e8,
      shortBalance: 3e8,
      shortChange: -2e7,
    },
  ],
};

describe("StockDetailPage", () => {
  beforeEach(() => {
    mockGet.mockReset();
    mockBack.mockReset();
  });

  it("渲染股票详情信息", async () => {
    mockGet.mockReturnValue({
      json: () => Promise.resolve({ code: 200, data: mockDetailData }),
    });

    render(<StockDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("600000 浦发银行")).toBeInTheDocument();
    });
  });

  it("显示两融数据", async () => {
    mockGet.mockReturnValue({
      json: () => Promise.resolve({ code: 200, data: mockDetailData }),
    });

    render(<StockDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("两融数据（最新）")).toBeInTheDocument();
    });
    await waitFor(() => {
      expect(screen.getByText("20.00亿")).toBeInTheDocument();
    });
  });

  it("股票不存在时显示提示", async () => {
    mockGet.mockReturnValue({
      json: () => Promise.resolve({ code: 200, data: null }),
    });

    render(<StockDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("未找到该股票")).toBeInTheDocument();
    });
  });

  it("加载中显示加载状态", () => {
    mockGet.mockReturnValue({
      json: () => new Promise(() => {}),
    });

    render(<StockDetailPage />);
    expect(screen.getByText("加载中...")).toBeInTheDocument();
  });
});
