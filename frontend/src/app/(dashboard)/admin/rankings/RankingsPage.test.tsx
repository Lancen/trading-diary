import { describe, it, expect, beforeEach, vi } from "vitest";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom/vitest";

const { mockGet, mockPush } = vi.hoisted(() => ({
  mockGet: vi.fn(),
  mockPush: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush, replace: vi.fn(), back: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
  usePathname: () => "/admin/rankings",
}));

vi.mock("@/lib/api", () => ({
  default: { get: (...args: unknown[]) => mockGet(...args) },
}));

import RankingsPage from "./page";

const mockRankingData = [
  {
    sectorType: "INDUSTRY",
    sectorCode: "BK0001",
    sectorName: "银行",
    tradeDate: "2025-01-15",
    amount: 5e9,
    amountChange: 1e9,
    changePct: 2.5,
    changePctChange: 0.8,
    volumePct: 3.21,
  },
  {
    sectorType: "INDUSTRY",
    sectorCode: "BK0002",
    sectorName: "证券",
    tradeDate: "2025-01-15",
    amount: 3e9,
    amountChange: -5e8,
    changePct: -1.2,
    changePctChange: -0.5,
    volumePct: 1.98,
  },
];

function getSearchParams(call: unknown[]): Record<string, string> {
  return (call[1] as { searchParams: Record<string, string> }).searchParams;
}

describe("RankingsPage", () => {
  beforeEach(() => {
    mockGet.mockReset();
    mockPush.mockReset();
  });

  it("渲染排行榜主 Tab", async () => {
    mockGet.mockReturnValue({
      json: () => Promise.resolve({ code: 200, data: mockRankingData }),
    });

    render(<RankingsPage />);

    await waitFor(() => {
      expect(screen.getByText("成交额排行")).toBeInTheDocument();
      expect(screen.getByText("成交额环比排行")).toBeInTheDocument();
      expect(screen.getByText("涨幅排行")).toBeInTheDocument();
      expect(screen.getByText("涨幅环比排行")).toBeInTheDocument();
    });
  });

  it("渲染行业/概念子 Tab", async () => {
    mockGet.mockReturnValue({
      json: () => Promise.resolve({ code: 200, data: mockRankingData }),
    });

    render(<RankingsPage />);

    await waitFor(() => {
      expect(screen.getByText("行业排名")).toBeInTheDocument();
      expect(screen.getByText("概念排名")).toBeInTheDocument();
    });
  });

  it("主 Tab 切换时设置正确的 sortBy 参数", async () => {
    mockGet.mockReturnValue({
      json: () => Promise.resolve({ code: 200, data: mockRankingData }),
    });

    render(<RankingsPage />);

    await waitFor(() => {
      expect(screen.getByText("成交额排行")).toBeInTheDocument();
    });

    expect(mockGet).toHaveBeenCalledTimes(1);
    expect(getSearchParams(mockGet.mock.calls[0]).sortBy).toBe("amount");

    fireEvent.click(screen.getByText("成交额环比排行"));
    await waitFor(() => {
      expect(getSearchParams(mockGet.mock.calls[mockGet.mock.calls.length - 1]).sortBy).toBe("amountChange");
    });

    fireEvent.click(screen.getByText("涨幅排行"));
    await waitFor(() => {
      expect(getSearchParams(mockGet.mock.calls[mockGet.mock.calls.length - 1]).sortBy).toBe("changePct");
    });

    fireEvent.click(screen.getByText("涨幅环比排行"));
    await waitFor(() => {
      expect(getSearchParams(mockGet.mock.calls[mockGet.mock.calls.length - 1]).sortBy).toBe("changePctChange");
    });
  });

  it("不渲染排序下拉框和升降序按钮", async () => {
    mockGet.mockReturnValue({
      json: () => Promise.resolve({ code: 200, data: mockRankingData }),
    });

    render(<RankingsPage />);

    await waitFor(() => {
      expect(screen.getByText("成交额排行")).toBeInTheDocument();
    });

    expect(screen.queryByRole("combobox")).not.toBeInTheDocument();
    expect(screen.queryByText("↓ 降序")).not.toBeInTheDocument();
    expect(screen.queryByText("↑ 升序")).not.toBeInTheDocument();
  });

  it("表头点击可切换排序方向", async () => {
    mockGet.mockReturnValue({
      json: () => Promise.resolve({ code: 200, data: mockRankingData }),
    });

    render(<RankingsPage />);

    await waitFor(() => {
      expect(screen.getByText("银行")).toBeInTheDocument();
    });

    const headers = screen.getAllByText(/成交额/);
    const amountHeader = headers.find((el) => el.closest("th"))!;
    fireEvent.click(amountHeader);

    await waitFor(() => {
      expect(getSearchParams(mockGet.mock.calls[mockGet.mock.calls.length - 1]).sortDir).toBe("asc");
    });
  });
});
