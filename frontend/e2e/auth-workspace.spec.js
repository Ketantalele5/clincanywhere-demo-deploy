import { expect, test } from '@playwright/test';

async function openSignIn(page) {
  await page.goto('/');
  await page.getByRole('button', { name: /Clinic sign in/i }).first().click();
  await expect(page.getByLabel('Username')).toBeVisible();
}

test('marketing site opens accessible clinic sign in', async ({ page }) => {
  await openSignIn(page);
  await expect(page.locator('#password')).toBeVisible();
  await expect(page.getByRole('button', { name: /Sign in to ClincyAnywhere/i })).toBeVisible();
  await expect(page.locator('#demoRole option')).toHaveCount(7);
  await page.locator('#demoRole').selectOption('billing');
  await expect(page.getByLabel('Username')).toHaveValue('billing');
  await expect(page.locator('#password')).toHaveValue('demo123');
  await page.getByRole('button', { name: 'Platform team', exact: true }).click();
  await expect(page.locator('#demoRole option')).toHaveCount(2);
  await page.locator('#demoRole').selectOption('onboarding');
  await expect(page.getByLabel('Username')).toHaveValue('onboarder');
});

test('admin can sign in and load the H2 clinic workspace', async ({ page }) => {
  await openSignIn(page);
  await page.locator('#demoRole').selectOption('admin');
  await page.getByLabel('Username').fill('admin');
  await page.locator('#password').fill('demo123');
  await page.getByRole('button', { name: /Sign in to ClincyAnywhere/i }).click();

  await expect(page.getByRole('heading', { name: /^Welcome back,/ })).toBeVisible();
  await expect(page.getByText('Could not load clinic data.')).toHaveCount(0);
  await page.getByRole('button', { name: /Reports & Analytics/i }).click();
  await expect(page.getByRole('heading', { name: 'Reports & Clinic Analytics' })).toBeVisible();
  await expect(page.locator('.report-stats')).not.toContainText('—');
  await page.setViewportSize({ width: 390, height: 844 });
  await expect.poll(() => page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(390);
  const reportWidth = await page.evaluate(() => ({
    viewport: document.documentElement.clientWidth,
    document: document.documentElement.scrollWidth,
  }));
  expect(reportWidth.document).toBeLessThanOrEqual(reportWidth.viewport);
  await page.setViewportSize({ width: 1280, height: 800 });
  await page.getByRole('button', { name: /Patient support/i }).click();
  await expect(page.getByRole('heading', { name: 'New support request' })).toBeVisible();
  await expect(page.locator('#support-patient')).toBeVisible();
  await page.getByRole('button', { name: /^Patients/ }).click();
  await expect(page.getByText(/\d+ records/)).toBeVisible();
});

test('invalid sign in stays on the login page with useful guidance', async ({ page }) => {
  await openSignIn(page);
  await page.getByLabel('Username').fill('missing-user');
  await page.locator('#password').fill('wrong-password');
  await page.getByRole('button', { name: /Sign in to ClincyAnywhere/i }).click();
  await expect(page.getByText(/Sign-in failed/i)).toBeVisible();
  await expect(page.getByLabel('Username')).toHaveValue('missing-user');
});

for (const role of [
  { id: 'lab', username: 'labtech', page: 'Laboratory' },
  { id: 'billing', username: 'billing', page: 'Billing & Payments' },
  { id: 'inventory', username: 'store', page: 'inventory' },
]) {
  test(`${role.id} can load only the authorized clinic workspace`, async ({ page }) => {
    await openSignIn(page);
    await page.locator('#demoRole').selectOption(role.id);
    await page.getByLabel('Username').fill(role.username);
    await page.locator('#password').fill('demo123');
    await page.getByRole('button', { name: /Sign in to ClincyAnywhere/i }).click();

    await expect(page.getByRole('heading', { name: /^Welcome back,/ })).toBeVisible();
    await expect(page.getByText('Could not load clinic data.')).toHaveCount(0);
    await expect(page.getByRole('navigation', { name: 'Main navigation' }).getByRole('button', { name: new RegExp(role.page, 'i') })).toBeVisible();
    if (role.id === 'billing') {
      await page.getByRole('button', { name: /Billing & Payments/i }).click();
      await expect(page.getByRole('heading', { name: 'Billing & Payments' })).toBeVisible();
      await expect(page.getByText(/\d+ invoices loaded/)).toBeVisible();
    }
  });
}

test('PWA manifest and offline shell assets are available', async ({ page, request }) => {
  await page.goto('/');
  await expect(page.locator('link[rel="manifest"]')).toHaveAttribute('href', '/manifest.webmanifest');

  const manifestResponse = await request.get('/manifest.webmanifest');
  expect(manifestResponse.ok()).toBeTruthy();
  const manifest = await manifestResponse.json();
  expect(manifest.display).toBe('standalone');
  expect(manifest.start_url).toBe('/');
  expect(manifest.icons.map(icon => icon.sizes)).toEqual(expect.arrayContaining(['192x192', '512x512']));

  const serviceWorkerResponse = await request.get('/service-worker.js');
  expect(serviceWorkerResponse.ok()).toBeTruthy();
  const serviceWorker = await serviceWorkerResponse.text();
  expect(serviceWorker).toContain("url.pathname.startsWith('/api/')");
  expect(serviceWorker).toContain("url.pathname.startsWith('/ws')");
  if (process.env.PLAYWRIGHT_EXPECT_SERVICE_WORKER === '1') {
    const registration = await page.evaluate(async () => {
      const active = await navigator.serviceWorker.ready;
      return { state: active.active?.state, scope: active.scope };
    });
    expect(registration.state).toBe('activated');
    expect(registration.scope).toBe(new URL('/', page.url()).href);
  }
  for (const icon of manifest.icons) {
    expect((await request.get(icon.src)).ok()).toBeTruthy();
  }
});
