import { describe, it, expect, beforeEach, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom/vitest";

const { mockGet, mockPush } = vi.hoisted(() => ({
  mockGet: vi.fn(),
  mockPush: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush, replace: vi.fn(), back: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
  usePathname: () => "/admin/stocks",
}));

vi.mock("@/lib/api", () => ({
  default: { get: (...args: unknown[]) => mockGet(...args) },
}));

import StocksPage from "./page";

describe("StocksPage", () => {
  const mockStocksData = {
    records: [
      {
        stockCode: "600000",
        stockName: "浦发银行",
        industry: "银行",
        concepts: "金融",
        close: 10.5,
        changePct: 1.23,
        volume: 50000000,
        marginBalance: 2e9,
        marginChange: 1e8,
        shortBalance: 5e8,
        shortChange: -2e7,
        tradeDate: "2025-01-15",
      },
    ],
    total: 150,
  };

  beforeEach(() => {
    mockGet.mockReset();
    mockPush.mockReset();
  });

  it("渲染股票列表", async () => {
    mockGet.mockReturnValue({
      json: () =>
        Promise.resolve({ code: 200, data: mockStocksData }),
    });

    render(<StocksPage />);

    await waitFor(() => {
      expect(screen.getByText("浦发银行")).toBeInTheDocument();
    });
    await waitFor(() => {
      expect(screen.getByText("600000")).toBeInTheDocument();
    });
  });

  it("显示分页控件", async () => {
    mockGet.mockReturnValue({
      json: () =>
        Promise.resolve({ code: 200, data: mockStocksData }),
    });

    render(<StocksPage />);

    await waitFor(() => {
      expect(screen.getByText("上一页")).toBeInTheDocument();
      expect(screen.getByText("下一页")).toBeInTheDocument();
    });
  });

  it("显示搜索和筛选输入框", async () => {
    mockGet.mockReturnValue({
      json: () =>
        Promise.resolve({ code: 200, data: mockStocksData }),
    });

    render(<StocksPage />);

    await waitFor(() => {
      const inputs = screen.getAllByRole("textbox");
      expect(inputs.length).toBeGreaterThanOrEqual(3);
    });
  });

  it("加载中显示加载状态", () => {
    mockGet.mockReturnValue({
      json: () => new Promise(() => {}),
    });

    render(<StocksPage />);
    expect(screen.getByText("加载中...")).toBeInTheDocument();
  });

  it("空数据时显示无数据提示", async () => {
    mockGet.mockReturnValue({
      json: () =>
        Promise.resolve({ code: 200, data: { records: [], total: 0 } }),
    });

    render(<StocksPage />);

    await waitFor(() => {
      expect(screen.getByText("无数据")).toBeInTheDocument();
    });
  });
});
