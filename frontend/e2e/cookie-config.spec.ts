import { test, expect } from "@playwright/test";

test.describe("Cookie 配置功能测试", () => {
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
    await page.route("**/api/v1/admin/collection/status", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          code: 200,
          data: [
            { dataType: "STOCK_INFO", dataTypeLabel: "股票行情", lastFetch: { status: "SUCCESS", completedAt: "2026-05-21T10:00:00", recordCount: 4800, errorMsg: null }, lastCleanse: { status: "SUCCESS", completedAt: "2026-05-21T10:05:00", recordCount: 4800, errorMsg: null } },
            { dataType: "TRADE_CALENDAR", dataTypeLabel: "交易日历", lastFetch: { status: "SUCCESS", completedAt: "2026-05-21T08:00:00", recordCount: 245, errorMsg: null }, lastCleanse: null },
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
            { filename: "2026-05-20.json", fetchedDate: "2026-05-20", industryCount: 90, conceptCount: 350, totalRelations: 5400, imported: true },
          ],
        }),
      })
    );
  });

  test("Smoke Test: 页面加载与网络请求", async ({ page }) => {
    await page.route("**/api/v1/admin/collection/config/cookie", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          code: 200,
          data: { hasCookie: false, cookiePreview: "", updatedAt: null },
        }),
      })
    );

    await page.goto("/admin/collection");
    await page.waitForLoadState("networkidle");

    await expect(page).toHaveURL(/\/admin\/collection/);

    const consoleErrors: string[] = [];
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        consoleErrors.push(msg.text());
      }
    });

    await expect(page.locator("h1", { hasText: "数据采集" })).toBeVisible();

    const criticalErrors = consoleErrors.filter(
      (e) => !e.includes("analytics") && !e.includes("third-party")
    );
    expect(criticalErrors.length).toBe(0);
  });

  test("Visual: Cookie 配置区域 UI 显示", async ({ page }) => {
    await page.route("**/api/v1/admin/collection/config/cookie", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          code: 200,
          data: { hasCookie: false, cookiePreview: "", updatedAt: null },
        }),
      })
    );

    await page.goto("/admin/collection");
    await page.waitForLoadState("networkidle");

    await expect(page.locator("h2", { hasText: "同花顺 Cookie 配置" })).toBeVisible();

    const cookieSection = page.locator("div.rounded-lg.border.bg-white").filter({ has: page.locator("h2", { hasText: "同花顺 Cookie 配置" }) });
    const statusBadge = cookieSection.locator("span.rounded-full.bg-yellow-100");
    await expect(statusBadge).toContainText("未配置");

    await expect(cookieSection.locator("textarea")).toBeVisible();
    await expect(cookieSection.locator("textarea")).toHaveAttribute("placeholder", "粘贴完整的Cookie字符串...");

    await expect(cookieSection.locator("button", { hasText: "保存" })).toBeVisible();
    await expect(cookieSection.locator("button", { hasText: "保存" })).toBeDisabled();

    await expect(cookieSection.locator("text=获取方法：登录同花顺")).toBeVisible();
  });

  test("Interaction: 查看当前 Cookie 状态（未配置）", async ({ page }) => {
    await page.route("**/api/v1/admin/collection/config/cookie", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          code: 200,
          data: { hasCookie: false, cookiePreview: "", updatedAt: null },
        }),
      })
    );

    await page.goto("/admin/collection");
    await page.waitForLoadState("networkidle");

    const cookieSection = page.locator("div.rounded-lg.border.bg-white").filter({ has: page.locator("h2", { hasText: "同花顺 Cookie 配置" }) });
    const statusBadge = cookieSection.locator("span.rounded-full.bg-yellow-100");
    await expect(statusBadge).toContainText("未配置");

    const infoText = cookieSection.locator("div.text-sm.text-gray-500").nth(0);
    await expect(infoText).toContainText("未配置Cookie时");
  });

  test("Interaction: 查看当前 Cookie 状态（已配置）", async ({ page }) => {
    await page.route("**/api/v1/admin/collection/config/cookie", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          code: 200,
          data: { hasCookie: true, cookiePreview: "v=...", updatedAt: "2026-05-21T10:00:00" },
        }),
      })
    );

    await page.goto("/admin/collection");
    await page.waitForLoadState("networkidle");

    const cookieSection = page.locator("div.rounded-lg.border.bg-white").filter({ has: page.locator("h2", { hasText: "同花顺 Cookie 配置" }) });
    const statusBadge = cookieSection.locator("span.rounded-full.bg-green-100");
    await expect(statusBadge).toContainText("已配置");

    const infoText = cookieSection.locator("div.text-sm.text-gray-500").nth(0);
    await expect(infoText).toContainText("当前:");
    await expect(infoText).toContainText("更新时间:");

    const clearButton = cookieSection.locator("button", { hasText: "清除" });
    await expect(clearButton).toBeVisible();
    await expect(clearButton).toBeEnabled();
  });

  test("Interaction: 输入并保存 Cookie", async ({ page }) => {
    let savedCookie = "";
    await page.route("**/api/v1/admin/collection/config/cookie", (route) => {
      if (route.request().method() === "GET") {
        const hasCookie = savedCookie !== "";
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            code: 200,
            data: {
              hasCookie,
              cookiePreview: hasCookie ? savedCookie.substring(0, 20) + "..." : "",
              updatedAt: hasCookie ? "2026-05-26T10:00:00" : null,
            },
          }),
        });
      } else {
        const body = route.request().postDataJSON();
        savedCookie = body.cookie || "";
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ code: 200, data: null }),
        });
      }
    });

    await page.goto("/admin/collection");
    await page.waitForLoadState("networkidle");

    const cookieSection = page.locator("div.rounded-lg.border.bg-white").filter({ has: page.locator("h2", { hasText: "同花顺 Cookie 配置" }) });
    const statusBadge = cookieSection.locator("span.rounded-full.bg-yellow-100");
    await expect(statusBadge).toContainText("未配置");

    const textarea = cookieSection.locator("textarea");
    await textarea.fill("test_cookie_string_for_10jqka");

    const saveButton = cookieSection.locator("button", { hasText: "保存" });
    await expect(saveButton).toBeEnabled();
    await saveButton.click();

    await page.waitForTimeout(500);

    await page.route("**/api/v1/admin/collection/config/cookie", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          code: 200,
          data: { hasCookie: true, cookiePreview: "test_cookie_string...", updatedAt: "2026-05-26T10:00:00" },
        }),
      })
    );

    await page.reload();
    await page.waitForLoadState("networkidle");

    const newCookieSection = page.locator("div.rounded-lg.border.bg-white").filter({ has: page.locator("h2", { hasText: "同花顺 Cookie 配置" }) });
    const newStatusBadge = newCookieSection.locator("span.rounded-full.bg-green-100");
    await expect(newStatusBadge).toContainText("已配置");
  });

  test("Interaction: 清除 Cookie", async ({ page }) => {
    let hasCookie = true;
    await page.route("**/api/v1/admin/collection/config/cookie", (route) => {
      if (route.request().method() === "GET") {
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            code: 200,
            data: {
              hasCookie,
              cookiePreview: hasCookie ? "v=..." : "",
              updatedAt: hasCookie ? "2026-05-26T10:00:00" : null,
            },
          }),
        });
      } else {
        const body = route.request().postDataJSON();
        if (body.cookie === "") {
          hasCookie = false;
        }
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ code: 200, data: null }),
        });
      }
    });

    await page.goto("/admin/collection");
    await page.waitForLoadState("networkidle");

    const cookieSection = page.locator("div.rounded-lg.border.bg-white").filter({ has: page.locator("h2", { hasText: "同花顺 Cookie 配置" }) });
    const statusBadge = cookieSection.locator("span.rounded-full.bg-green-100");
    await expect(statusBadge).toContainText("已配置");

    const clearButton = cookieSection.locator("button", { hasText: "清除" });
    await expect(clearButton).toBeVisible();
    await clearButton.click();

    await page.waitForTimeout(500);

    await page.route("**/api/v1/admin/collection/config/cookie", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          code: 200,
          data: { hasCookie: false, cookiePreview: "", updatedAt: null },
        }),
      })
    );

    await page.reload();
    await page.waitForLoadState("networkidle");

    const newCookieSection = page.locator("div.rounded-lg.border.bg-white").filter({ has: page.locator("h2", { hasText: "同花顺 Cookie 配置" }) });
    const newStatusBadge = newCookieSection.locator("span.rounded-full.bg-yellow-100");
    await expect(newStatusBadge).toContainText("未配置");
  });

  test("Interaction: 保存按钮禁用状态", async ({ page }) => {
    await page.route("**/api/v1/admin/collection/config/cookie", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          code: 200,
          data: { hasCookie: false, cookiePreview: "", updatedAt: null },
        }),
      })
    );

    await page.goto("/admin/collection");
    await page.waitForLoadState("networkidle");

    const cookieSection = page.locator("div.rounded-lg.border.bg-white").filter({ has: page.locator("h2", { hasText: "同花顺 Cookie 配置" }) });
    const saveButton = cookieSection.locator("button", { hasText: "保存" });
    await expect(saveButton).toBeDisabled();

    const textarea = cookieSection.locator("textarea");
    await textarea.fill("   ");
    await expect(saveButton).toBeDisabled();

    await textarea.fill("valid_cookie_value");
    await expect(saveButton).toBeEnabled();

    await textarea.clear();
    await expect(saveButton).toBeDisabled();
  });

  test("Interaction: 保存中状态显示", async ({ page }) => {
    await page.route("**/api/v1/admin/collection/config/cookie", (route) => {
      if (route.request().method() === "GET") {
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            code: 200,
            data: { hasCookie: false, cookiePreview: "", updatedAt: null },
          }),
        });
      } else {
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ code: 200, data: null }),
        });
      }
    });

    await page.goto("/admin/collection");
    await page.waitForLoadState("networkidle");

    const cookieSection = page.locator("div.rounded-lg.border.bg-white").filter({ has: page.locator("h2", { hasText: "同花顺 Cookie 配置" }) });
    const textarea = cookieSection.locator("textarea");
    await textarea.fill("test_cookie");

    const saveButton = cookieSection.locator("button", { hasText: "保存" });
    await saveButton.click();

    await expect(cookieSection.locator("button", { hasText: "保存中..." })).toBeVisible({ timeout: 1000 });

    await page.waitForTimeout(1000);
    await expect(cookieSection.locator("button", { hasText: "保存" })).toBeVisible();
  });
});