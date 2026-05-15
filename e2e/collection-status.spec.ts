import { test, expect } from "@playwright/test";

test.describe("Collection Status Page", () => {
  test.beforeEach(async ({ page }) => {
    await page.context().clearCookies();
    await page.goto("http://localhost:3000/login");
    // In case already logged in (redirected to /dashboard)
    if (page.url().includes("/login")) {
      await page.getByRole("textbox", { name: "用户名" }).fill("admin");
      await page.getByRole("textbox", { name: "密码" }).fill("admin123");
      await page.getByRole("button", { name: "登录" }).click();
      await page.waitForURL("**/dashboard", { timeout: 10000 });
    }
  });

  test("should display collection status page with 9 data type cards", async ({ page }) => {
    await page.goto("http://localhost:3000/admin/collection");
    await page.waitForLoadState("networkidle");

    await expect(page.getByRole("heading", { name: "Collection Status" })).toBeVisible();

    await expect(page.getByText("Success")).toBeVisible();
    await expect(page.getByText("Failed", { exact: true })).toBeVisible();
    await expect(page.getByText("Running")).toBeVisible();

    const labels = ["股票基础信息", "股票日线行情", "交易日历",
      "行业板块分类", "行业成分股", "概念板块分类",
      "概念成分股", "两融明细(沪市)", "两融明细(深市)"];
    for (const label of labels) {
      await expect(page.getByRole("heading", { name: label })).toBeVisible();
    }
  });

  test("should refresh on button click", async ({ page }) => {
    await page.goto("http://localhost:3000/admin/collection");
    await page.waitForLoadState("networkidle");

    await page.getByRole("button", { name: "Refresh" }).click();
    await page.waitForTimeout(1000);
    await expect(page.getByRole("heading", { name: "Collection Status" })).toBeVisible();
  });

  test("should expand logs when card clicked", async ({ page }) => {
    await page.goto("http://localhost:3000/admin/collection");
    await page.waitForLoadState("networkidle");

    await page.getByRole("heading", { name: "股票基础信息" }).click();
    await page.waitForTimeout(500);

    await expect(page.getByText("Recent 5 Logs")).toBeVisible();
    await expect(page.getByText("FETCH:").first()).toBeVisible();
  });

  test("should trigger collection and stay on page", async ({ page }) => {
    await page.goto("http://localhost:3000/admin/collection");
    await page.waitForLoadState("networkidle");

    await page.getByRole("button", { name: "重新采集" }).first().click();
    await page.waitForTimeout(3000);

    await expect(page.getByRole("heading", { name: "Collection Status" })).toBeVisible();
  });
});
