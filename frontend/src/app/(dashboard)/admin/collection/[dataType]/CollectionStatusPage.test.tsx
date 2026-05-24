import { describe, it, expect, beforeEach, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom/vitest";

const { mockGet } = vi.hoisted(() => ({
  mockGet: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  useParams: () => ({ dataType: "STOCK_INFO" }),
  usePathname: () => "/admin/collection/STOCK_INFO",
  useRouter: () => ({ replace: vi.fn(), push: vi.fn(), back: vi.fn() }),
}));

vi.mock("@/hooks/use-toast", () => ({
  useToast: () => ({ toast: vi.fn() }),
}));

vi.mock("@/lib/api", () => ({
  default: { get: (...args: unknown[]) => mockGet(...args), post: vi.fn() },
}));

import CollectionDetailPage from "./page";

describe("CollectionDetailPage", () => {
  beforeEach(() => {
    mockGet.mockReset();
  });

  it("渲染加载状态", () => {
    mockGet.mockReturnValue({
      json: () => new Promise(() => {}),
    });
    render(<CollectionDetailPage />);
    expect(screen.getByText("加载中...")).toBeInTheDocument();
  });

  it("渲染采集状态项", async () => {
    const statusData = [
      {
        dataType: "STOCK_INFO",
        lastFetch: {
          status: "SUCCESS",
          startedAt: "2025-01-01T10:00:00",
          completedAt: "2025-01-01T10:05:00",
          recordCount: 100,
          errorMsg: null,
        },
        lastDataDate: "2025-01-01",
      },
    ];
    const logsData = [
      {
        id: 1,
        dataType: "STOCK_INFO",
        jobType: "FETCH",
        status: "SUCCESS",
        recordCount: 100,
        errorMsg: null,
        requestUrl: "/api/test",
        requestParams: null,
        remark: null,
        startedAt: "2025-01-01T10:00:00",
        completedAt: "2025-01-01T10:05:00",
        tradeDate: "2025-01-01",
      },
    ];

    mockGet.mockImplementation((url: string) => {
      if (url.includes("status")) {
        return { json: () => Promise.resolve({ code: 200, data: statusData }) };
      }
      return { json: () => Promise.resolve({ code: 200, data: logsData }) };
    });

    render(<CollectionDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("股票行情")).toBeInTheDocument();
    });
    await waitFor(() => {
      expect(screen.getByText("成功")).toBeInTheDocument();
    });
  });

  it("API 错误时仍渲染页面", async () => {
    mockGet.mockRejectedValue(new Error("网络错误"));

    render(<CollectionDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("股票行情")).toBeInTheDocument();
    });
  });
});
