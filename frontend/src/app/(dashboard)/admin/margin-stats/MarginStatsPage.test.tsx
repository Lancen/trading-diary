import { describe, it, expect, beforeEach, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom/vitest";

const { mockGet } = vi.hoisted(() => ({
  mockGet: vi.fn(),
}));

vi.mock("@/lib/api", () => ({
  default: { get: (...args: unknown[]) => mockGet(...args) },
}));

import MarginStatsPage from "./page";

describe("MarginStatsPage", () => {
  beforeEach(() => {
    mockGet.mockReset();
  });

  it("渲染融资统计摘要", async () => {
    const summaryData = {
      totalMarginBalance: 1.5e12,
      totalShortBalance: 1.2e11,
      totalBalance: 1.62e12,
      stockCount: 2500,
      tradeDate: "2025-01-15",
    };
    mockGet.mockReturnValue({
      json: () => Promise.resolve({ code: 200, data: summaryData }),
    });

    render(<MarginStatsPage />);

    await waitFor(() => {
      expect(screen.getByText("融资统计")).toBeInTheDocument();
    });
    await waitFor(() => {
      expect(screen.getByText("15000.00亿")).toBeInTheDocument();
    });
    await waitFor(() => {
      expect(screen.getByText("1200.00亿")).toBeInTheDocument();
    });
  });

  it("显示日期选择器", () => {
    mockGet.mockReturnValue({
      json: () => new Promise(() => {}),
    });

    render(<MarginStatsPage />);

    const dateInput = document.querySelector('input[type="date"]');
    expect(dateInput).toBeInTheDocument();
  });

  it("空数据时显示无数据提示", async () => {
    mockGet.mockReturnValue({
      json: () => Promise.resolve({ code: 200, data: null }),
    });

    render(<MarginStatsPage />);

    await waitFor(() => {
      expect(screen.getByText("无数据")).toBeInTheDocument();
    });
  });

  it("加载中显示加载状态", () => {
    mockGet.mockReturnValue({
      json: () => new Promise(() => {}),
    });

    render(<MarginStatsPage />);
    expect(screen.getByText("加载中...")).toBeInTheDocument();
  });
});
