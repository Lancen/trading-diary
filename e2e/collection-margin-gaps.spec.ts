import { test, expect } from "@playwright/test";

test.describe("Collection Margin Gaps Page", () => {
  test.beforeEach(async ({ page }) => {
    await page.context().clearCookies();
    await page.goto("http://localhost:3000/login");
    if (page.url().includes("/login")) {
      await page.getByRole("textbox", { name: "用户名" }).fill("admin");
      await page.getByRole("textbox", { name: "密码" }).fill("admin123");
      await page.getByRole("button", { name: "登录" }).click();
      await page.waitForURL("**/dashboard", { timeout: 10000 });
    }
  });

  test("should display margin gap report page", async ({ page }) => {
    await page.goto("http://localhost:3000/admin/collection/margin");
    await page.waitForLoadState("networkidle");

    await expect(page.getByRole("heading", { name: "Margin Data Completeness" })).toBeVisible({ timeout: 10000 });

    await expect(page.getByText("Start Date")).toBeVisible();
    await expect(page.getByText("End Date")).toBeVisible();
    await expect(page.getByText("Exchange")).toBeVisible();
    await expect(page.getByRole("button", { name: "Check Gaps" })).toBeVisible();
    await expect(page.getByRole("button", { name: "补采" })).toBeVisible();
  });

  test("should filter by date range", async ({ page }) => {
    await page.goto("http://localhost:3000/admin/collection/margin");
    await page.waitForLoadState("networkidle");

    const dateInputs = page.locator('input[type="date"]');
    await dateInputs.first().fill("2026-05-01");
    await dateInputs.nth(1).fill("2026-05-15");
    await page.getByRole("button", { name: "Check Gaps" }).click();
    await page.waitForTimeout(2000);

    await expect(page.getByRole("heading", { name: "Margin Data Completeness" })).toBeVisible();
  });

  test("should open backfill dialog", async ({ page }) => {
    await page.goto("http://localhost:3000/admin/collection/margin");
    await page.waitForLoadState("networkidle");

    await page.getByRole("button", { name: "补采" }).click();
    await page.waitForTimeout(500);

    await expect(page.getByRole("heading", { name: "补采数据" })).toBeVisible();
    await expect(page.getByRole("button", { name: "提交补采" })).toBeVisible();
    await expect(page.getByRole("button", { name: "取消" })).toBeVisible();
  });

  test("should close backfill dialog with cancel", async ({ page }) => {
    await page.goto("http://localhost:3000/admin/collection/margin");
    await page.waitForLoadState("networkidle");

    await page.getByRole("button", { name: "补采" }).click();
    await page.waitForTimeout(500);

    await page.getByRole("button", { name: "取消" }).click();
    await page.waitForTimeout(300);

    await expect(page.getByRole("heading", { name: "Margin Data Completeness" })).toBeVisible();
  });
});
