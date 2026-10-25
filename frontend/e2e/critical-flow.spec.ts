import { test, expect } from "@playwright/test";

const TENANT_SLUG = "demo";
const ADMIN_USERNAME = "admin";
const ADMIN_PASSWORD = "admin";

test.describe("Vivent MVP critical flow", () => {
  test("login as admin and verify dashboard loads", async ({ page }) => {
    // Navigate to login
    await page.goto(`/${TENANT_SLUG}/login`);

    // Wait for tenant config to load
    await page.waitForSelector('input[name="username"]', { timeout: 15_000 });

    // Fill login form
    await page.fill('input[name="username"]', ADMIN_USERNAME);
    await page.fill('input[name="password"]', ADMIN_PASSWORD);

    // Submit
    await page.click('button[type="submit"]');

    // Verify redirect to dashboard
    await page.waitForURL("**/dashboard", { timeout: 15_000 });
    await expect(page).toHaveURL(/\/dashboard/);

    // Verify dashboard content
    await expect(page.locator("body")).toContainText(/Vivent|Dashboard|dashboard/i, { timeout: 10_000 });
  });

  test("login as admin and navigate to inventory", async ({ page }) => {
    await page.goto(`/${TENANT_SLUG}/login`);
    await page.waitForSelector('input[name="username"]', { timeout: 15_000 });
    await page.fill('input[name="username"]', ADMIN_USERNAME);
    await page.fill('input[name="password"]', ADMIN_PASSWORD);
    await page.click('button[type="submit"]');
    await page.waitForURL("**/dashboard", { timeout: 15_000 });

    // Navigate to inventory
    await page.goto(`/${TENANT_SLUG}/inventory`);
    await page.waitForLoadState("networkidle");
    await expect(page).toHaveURL(/\/inventory/);
  });

  test("login as admin and navigate to purchases", async ({ page }) => {
    await page.goto(`/${TENANT_SLUG}/login`);
    await page.waitForSelector('input[name="username"]', { timeout: 15_000 });
    await page.fill('input[name="username"]', ADMIN_USERNAME);
    await page.fill('input[name="password"]', ADMIN_PASSWORD);
    await page.click('button[type="submit"]');
    await page.waitForURL("**/dashboard", { timeout: 15_000 });

    // Navigate to purchases
    await page.goto(`/${TENANT_SLUG}/purchases`);
    await page.waitForLoadState("networkidle");
    await expect(page).toHaveURL(/\/purchases/);
  });

  test("login as admin and navigate to sales", async ({ page }) => {
    await page.goto(`/${TENANT_SLUG}/login`);
    await page.waitForSelector('input[name="username"]', { timeout: 15_000 });
    await page.fill('input[name="username"]', ADMIN_USERNAME);
    await page.fill('input[name="password"]', ADMIN_PASSWORD);
    await page.click('button[type="submit"]');
    await page.waitForURL("**/dashboard", { timeout: 15_000 });

    // Navigate to sales
    await page.goto(`/${TENANT_SLUG}/sales`);
    await page.waitForLoadState("networkidle");
    await expect(page).toHaveURL(/\/sales/);
  });

  test("login as admin and navigate to combos", async ({ page }) => {
    await page.goto(`/${TENANT_SLUG}/login`);
    await page.waitForSelector('input[name="username"]', { timeout: 15_000 });
    await page.fill('input[name="username"]', ADMIN_USERNAME);
    await page.fill('input[name="password"]', ADMIN_PASSWORD);
    await page.click('button[type="submit"]');
    await page.waitForURL("**/dashboard", { timeout: 15_000 });

    // Navigate to combos
    await page.goto(`/${TENANT_SLUG}/admin/catalog/combos`);
    await page.waitForLoadState("networkidle");
    await expect(page).toHaveURL(/\/admin\/catalog\/combos/);
  });

  test("login as admin and navigate to closures", async ({ page }) => {
    await page.goto(`/${TENANT_SLUG}/login`);
    await page.waitForSelector('input[name="username"]', { timeout: 15_000 });
    await page.fill('input[name="username"]', ADMIN_USERNAME);
    await page.fill('input[name="password"]', ADMIN_PASSWORD);
    await page.click('button[type="submit"]');
    await page.waitForURL("**/dashboard", { timeout: 15_000 });

    // Navigate to closures
    await page.goto(`/${TENANT_SLUG}/closures`);
    await page.waitForLoadState("networkidle");
    await expect(page).toHaveURL(/\/closures/);
  });

  test("invalid login is rejected", async ({ page }) => {
    await page.goto(`/${TENANT_SLUG}/login`);
    await page.waitForSelector('input[name="username"]', { timeout: 15_000 });
    await page.fill('input[name="username"]', "nonexistent");
    await page.fill('input[name="password"]', "wrongpassword");
    await page.click('button[type="submit"]');

    // Should stay on login page and show error
    await page.waitForTimeout(3_000);
    await expect(page).toHaveURL(/\/login/);
  });
});