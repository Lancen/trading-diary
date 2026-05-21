import { test, expect } from "@playwright/test";

test.describe("数据采集 Hub 页", () => {
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

  test("页面加载并显示采集状态卡片", async ({ page }) => {
    await page.route("**/api/v1/admin/collection/status", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          code: 200,
          data: [
            { dataType: "STOCK_INFO", dataTypeLabel: "股票行情", lastFetch: { status: "SUCCESS", completedAt: "2026-05-21T10:00:00", recordCount: 4800, errorMsg: null }, lastCleanse: { status: "SUCCESS", completedAt: "2026-05-21T10:05:00", recordCount: 4800, errorMsg: null } },
            { dataType: "MARGIN_DAILY", dataTypeLabel: "融资日数据", lastFetch: { status: "SUCCESS", completedAt: "2026-05-21T09:00:00", recordCount: 4200, errorMsg: null }, lastCleanse: null },
            { dataType: "STOCK_DAILY", dataTypeLabel: "日线数据", lastFetch: null, lastCleanse: null },
            { dataType: "INDEX_DAILY", dataTypeLabel: "指数日线", lastFetch: { status: "FAILED", completedAt: "2026-05-20T15:00:00", recordCount: 0, errorMsg: "网络超时" }, lastCleanse: null },
            { dataType: "CONCEPT", dataTypeLabel: "概念板块", lastFetch: { status: "SUCCESS", completedAt: "2026-05-21T08:00:00", recordCount: 350, errorMsg: null }, lastCleanse: null },
            { dataType: "INDUSTRY", dataTypeLabel: "行业板块", lastFetch: { status: "SUCCESS", completedAt: "2026-05-21T08:00:00", recordCount: 90, errorMsg: null }, lastCleanse: null },
            { dataType: "TRADE_CALENDAR", dataTypeLabel: "交易日历", lastFetch: { status: "SUCCESS", completedAt: "2026-04-01T00:00:00", recordCount: 245, errorMsg: null }, lastCleanse: null },
            { dataType: "MARGIN_TOTAL", dataTypeLabel: "融资总量", lastFetch: { status: "SUCCESS", completedAt: "2026-05-21T09:00:00", recordCount: 1, errorMsg: null }, lastCleanse: null },
          ],
        }),
      })
    );
    await page.route("**/api/v1/admin/collection/constituents/files", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          code: 200,
          data: [
            { fileName: "2026-05-20.json", fetchedDate: "2026-05-20", industryCount: 90, conceptCount: 350, totalRelations: 5400, imported: true },
            { fileName: "2026-05-19.json", fetchedDate: "2026-05-19", industryCount: 90, conceptCount: 348, totalRelations: 5380, imported: false },
          ],
        }),
      })
    );

    await page.goto("/admin/collection");
    await page.waitForLoadState("networkidle");

    // 页面标题
    await expect(page.locator("h1")).toContainText("数据采集");

    // 汇总统计：格式 "正常 N · 异常 M"
    await expect(page.getByText(/正常 \d+ · 异常 \d+/)).toBeVisible();

    // 卡片
    await expect(page.locator("text=股票行情")).toBeVisible();
    await expect(page.locator("text=成分股数据")).toBeVisible();

    // 状态徽章（正常和异常都有）
    await expect(page.getByText("正常").first()).toBeVisible();
  });

  test("点击股票行情卡片跳转到详情页", async ({ page }) => {
    await page.route("**/api/v1/admin/collection/status", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          code: 200,
          data: [
            { dataType: "STOCK_INFO", dataTypeLabel: "股票行情", lastFetch: { status: "SUCCESS", completedAt: "2026-05-21T10:00:00", recordCount: 4800, errorMsg: null }, lastCleanse: { status: "SUCCESS", completedAt: "2026-05-21T10:05:00", recordCount: 4800, errorMsg: null } },
          ],
        }),
      })
    );
    await page.route("**/api/v1/admin/collection/constituents/files", (route) =>
      route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ code: 200, data: [] }) })
    );

    await page.goto("/admin/collection");
    await page.waitForLoadState("networkidle");

    await page.click("text=股票行情");
    await page.waitForURL("**/admin/collection/stocks");

    await expect(page).toHaveURL(/\/admin\/collection\/stocks/);
  });

  test("点击成分股卡片跳转到管理页", async ({ page }) => {
    await page.route("**/api/v1/admin/collection/status", (route) =>
      route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ code: 200, data: [] }) })
    );
    await page.route("**/api/v1/admin/collection/constituents/files", (route) =>
      route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ code: 200, data: [] }) })
    );

    await page.goto("/admin/collection");
    await page.waitForLoadState("networkidle");

    await page.click("text=成分股数据");
    await page.waitForURL("**/admin/collection/constituents");

    await expect(page).toHaveURL(/\/admin\/collection\/constituents/);
  });
});
