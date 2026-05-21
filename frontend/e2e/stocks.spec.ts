import { test, expect } from "@playwright/test";

test.describe("股票列表页", () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem("accessToken", "fake-token");
      localStorage.setItem("refreshToken", "fake-refresh");
    });
    await page.route("**/api/v1/auth/me", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          code: 200,
          data: { id: 1, username: "admin", nickname: "管理员", roles: ["ADMIN"] },
        }),
      })
    );
  });

  const mockStockList = {
    code: 200,
    data: {
      records: [
        { stockCode: "000001", stockName: "平安银行", industry: "银行", concepts: "MSCI,HS300", close: 12.50, changePct: 2.35, volume: 50000000, marginBalance: 15000000000, marginChange: 200000000, shortBalance: 500000000, shortChange: -10000000, tradeDate: "2026-05-20" },
        { stockCode: "000002", stockName: "万科A", industry: "房地产", concepts: "MSCI,HS300", close: 8.20, changePct: -1.50, volume: 30000000, marginBalance: 8000000000, marginChange: -50000000, shortBalance: 300000000, shortChange: 2000000, tradeDate: "2026-05-20" },
      ],
      total: 2,
    },
  };

  test("页面加载并显示股票列表数据", async ({ page }) => {
    await page.route("**/api/v1/admin/stocks/list*", (route) =>
      route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(mockStockList) })
    );

    await page.goto("/admin/stocks");
    await page.waitForLoadState("networkidle");

    await expect(page.locator("h1")).toContainText("股票数据");
    await expect(page.locator("text=平安银行")).toBeVisible();
    await expect(page.locator("text=000001").first()).toBeVisible();
    await expect(page.locator("text=万科A")).toBeVisible();
    await expect(page.getByText("+2.35%")).toBeVisible();
    await expect(page.getByText("-1.50%")).toBeVisible();
  });

  test("点击股票代码跳转到详情页", async ({ page }) => {
    await page.route("**/api/v1/admin/stocks/list*", (route) =>
      route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(mockStockList) })
    );
    // 也需要 mock 详情页 API（跳转后会加载）
    await page.route("**/api/v1/admin/stocks/000001*", (route) =>
      route.fulfill({
        status: 200, contentType: "application/json",
        body: JSON.stringify({ code: 200, data: { stockCode: "000001", stockName: "平安银行", industry: "银行", concepts: [], latestQuote: null, latestMargin: null, dailyKlines: [], dailyMargins: [] } }),
      })
    );

    await page.goto("/admin/stocks");
    await page.waitForLoadState("networkidle");

    await page.click("text=000001");
    await page.waitForURL("**/admin/stocks/000001");
    await expect(page).toHaveURL(/\/admin\/stocks\/000001/);
  });

  test("搜索筛选功能", async ({ page }) => {
    let capturedParams: URLSearchParams | null = null;
    await page.route("**/api/v1/admin/stocks/list*", (route) => {
      capturedParams = new URL(route.request().url()).searchParams;
      return route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(mockStockList) });
    });

    await page.goto("/admin/stocks");
    await page.waitForLoadState("networkidle");

    // 输入关键字搜索
    await page.getByPlaceholder("输入搜索").fill("平安");
    await page.click("text=查询");

    expect(capturedParams?.get("keyword")).toBe("平安");
  });
});

test.describe("股票详情页", () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem("accessToken", "fake-token");
      localStorage.setItem("refreshToken", "fake-refresh");
    });
    await page.route("**/api/v1/auth/me", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          code: 200,
          data: { id: 1, username: "admin", nickname: "管理员", roles: ["ADMIN"] },
        }),
      })
    );
  });

  const mockDetail = {
    code: 200,
    data: {
      stockCode: "000001",
      stockName: "平安银行",
      industry: "银行",
      concepts: ["MSCI中国", "沪深300"],
      latestQuote: { open: 12.00, high: 12.80, low: 11.90, close: 12.50, volume: 50000000, changePct: 2.35 },
      latestMargin: { marginBalance: 15000000000, marginBuy: 200000000, shortBalance: 500000000, totalBalance: 15500000000 },
      dailyKlines: [
        { tradeDate: "2026-05-19", open: 12.10, high: 12.30, low: 12.00, close: 12.20, volume: 40000000 },
        { tradeDate: "2026-05-20", open: 12.20, high: 12.80, low: 11.90, close: 12.50, volume: 50000000 },
      ],
      dailyMargins: [
        { tradeDate: "2026-05-19", marginBalance: 14800000000, marginChange: 0, shortBalance: 510000000, shortChange: 0 },
        { tradeDate: "2026-05-20", marginBalance: 15000000000, marginChange: 200000000, shortBalance: 500000000, shortChange: -10000000 },
      ],
    },
  };

  test("页面加载并显示行情摘要和K线图", async ({ page }) => {
    await page.route("**/api/v1/admin/stocks/000001*", (route) =>
      route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(mockDetail) })
    );

    await page.goto("/admin/stocks/000001");
    await page.waitForLoadState("networkidle");

    // 标题含代码+名称
    await expect(page.locator("h1")).toContainText("平安银行");
    await expect(page.locator("h1")).toContainText("000001");

    // 概念标签
    await expect(page.locator("text=MSCI中国")).toBeVisible();
    await expect(page.locator("text=沪深300")).toBeVisible();

    // 行情摘要（close=12.5, changePct=2.35%）
    await expect(page.getByText("12.5", { exact: true })).toBeVisible();
    await expect(page.getByText("2.35%", { exact: true })).toBeVisible();

    // K线图组件
    await expect(page.locator("text=K线图")).toBeVisible();
    await expect(page.locator("text=日K")).toBeVisible();
    await expect(page.locator("text=周K")).toBeVisible();
    await expect(page.locator("text=月K")).toBeVisible();

    // 快捷区间
    await expect(page.locator("text=1月")).toBeVisible();
    await expect(page.locator("text=3月")).toBeVisible();

    // 两融数据
    await expect(page.locator("text=150.00亿")).toBeVisible();
  });

  test("日线明细表展开收起", async ({ page }) => {
    await page.route("**/api/v1/admin/stocks/000001*", (route) =>
      route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(mockDetail) })
    );

    await page.goto("/admin/stocks/000001");
    await page.waitForLoadState("networkidle");

    // 展开日线明细
    await page.click("text=展开日线明细");
    await expect(page.getByText("收起日线明细")).toBeVisible();

    // 表格显示
    await expect(page.locator("text=2026-05-20")).toBeVisible();
    await expect(page.locator("text=2026-05-19")).toBeVisible();
  });
});
