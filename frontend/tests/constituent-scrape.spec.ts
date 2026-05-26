import { test, expect } from '@playwright/test';

const BASE = 'http://localhost:3000';

test.describe('成分股抓取功能', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto(`${BASE}/admin`);
    await page.waitForTimeout(2000);
  });

  test('行业详情页 - 显示抓取成分股按钮', async ({ page }) => {
    await page.goto(`${BASE}/admin/industries/881121`);
    await page.waitForTimeout(3000);

    const scrapeBtn = page.getByText('抓取成分股');
    await expect(scrapeBtn).toBeVisible();

    await page.screenshot({ path: 'test-results/industry-detail.png', fullPage: true });
  });

  test('概念详情页 - 显示抓取成分股按钮', async ({ page }) => {
    await page.goto(`${BASE}/admin/concepts/308614`);
    await page.waitForTimeout(3000);

    const scrapeBtn = page.getByText('抓取成分股');
    await expect(scrapeBtn).toBeVisible();

    await page.screenshot({ path: 'test-results/concept-detail.png', fullPage: true });
  });

  test('行业详情页 - 点击抓取按钮', async ({ page }) => {
    await page.goto(`${BASE}/admin/industries/881121`);
    await page.waitForTimeout(3000);

    const scrapeBtn = page.getByText('抓取成分股');
    await scrapeBtn.click();

    await expect(page.getByText('抓取中...')).toBeVisible();

    await page.waitForTimeout(60000);

    await page.screenshot({ path: 'test-results/industry-scrape-result.png', fullPage: true });
  });
});
