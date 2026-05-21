import { test, expect } from "@playwright/test";

test.describe("融资统计汇总页", () => {
  test.beforeEach(async ({ page }) => {
    // Inject tokens before page scripts run
    await page.addInitScript(() => {
      localStorage.setItem("accessToken", "fake-token");
      localStorage.setItem("refreshToken", "fake-refresh");
    });
    // Mock auth API — return fake admin user
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

  test("页面加载并显示融资汇总数据", async ({ page }) => {
    await page.route("**/api/v1/admin/margin-stats/summary*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          code: 200,
          data: {
            totalMarginBalance: 150000000000,
            totalShortBalance: 5000000000,
            totalBalance: 155000000000,
            stockCount: 4200,
            tradeDate: "2026-05-20",
          },
        }),
      })
    );

    await page.goto("/admin/margin-stats");
    await page.waitForLoadState("networkidle");

    await expect(page.locator("h1")).toContainText("融资统计");
    await expect(page.locator("text=融资余额")).toBeVisible();
    await expect(page.locator("text=1500.00亿")).toBeVisible();
    await expect(page.locator("text=融券余额")).toBeVisible();
    await expect(page.getByText("50.00亿", { exact: true })).toBeVisible();
    await expect(page.locator("text=两融总额")).toBeVisible();
    await expect(page.getByText("1550.00亿", { exact: true })).toBeVisible();
    await expect(page.locator("text=标的数量")).toBeVisible();
    await expect(page.getByText(/4,?200/)).toBeVisible();
  });

  test("日期筛选功能", async ({ page }) => {
    let capturedDate: string | null = null;
    await page.route("**/api/v1/admin/margin-stats/summary*", (route) => {
      const url = new URL(route.request().url());
      capturedDate = url.searchParams.get("tradeDate");
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          code: 200,
          data: {
            totalMarginBalance: 140000000000,
            totalShortBalance: 4800000000,
            totalBalance: 144800000000,
            stockCount: 4198,
            tradeDate: "2026-05-19",
          },
        }),
      });
    });

    await page.goto("/admin/margin-stats");
    await page.waitForLoadState("networkidle");

    await page.fill('input[type="date"]', "2026-05-19");
    await page.click("text=查询");

    await expect(page.locator("text=1400.00亿")).toBeVisible();
    expect(capturedDate).toBe("2026-05-19");
  });
});
