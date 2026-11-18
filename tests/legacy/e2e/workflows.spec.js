import { expect, test } from '@playwright/test';

async function signIn(page, role = 'admin') {
  await page.goto('/');
  if (role !== 'admin') await page.getByRole('button', { name: new RegExp(role, 'i') }).click();
  await page.getByRole('button', { name: /Enter workspace/ }).click();
  await expect(page.getByRole('heading', { name: 'Your clinic at a glance' })).toBeVisible();
  await expect(page.getByText('Could not load clinic data.')).toHaveCount(0);
}

test('prescription review, pharmacy handoff, and dispensing advance correctly', async ({ page }) => {
  await signIn(page);
  await page.getByRole('button', { name: /^Prescriptions/ }).click();
  const row = page.getByRole('row').filter({ hasText: 'RX-402' });
  await expect(row).toContainText('Draft');
  page.once('dialog', dialog => dialog.accept());
  await row.getByRole('button', { name: 'Finalize' }).click();
  await expect(row).toContainText('Finalized');
  await expect(row).toContainText('New');
  await row.getByRole('button', { name: 'Advance pharmacy' }).click();
  await expect(row).toContainText('Processing');
  await row.getByRole('button', { name: 'WhatsApp preview' }).click();
  await expect(page.getByRole('dialog')).toContainText('No external message will be sent');
  await page.getByRole('button', { name: 'Mark as sent (demo)' }).click();
  await expect(page.getByText('Demo status updated — no external message was sent.')).toBeVisible();
  await page.getByRole('button', { name: /^Log out/ }).click();
  await page.getByRole('button', { name: /Pharmacist/ }).click();
  await page.getByRole('button', { name: /Enter workspace/ }).click();
  await page.getByRole('button', { name: /^Prescriptions/ }).click();
  const pharmacistRow = page.getByRole('row').filter({ hasText: 'RX-402' });
  await pharmacistRow.getByRole('button', { name: 'Advance pharmacy' }).click();
  await expect(pharmacistRow).toContainText('Ready');
});

test('follow-up booking and instant reminder previews are non-delivery actions', async ({ page }) => {
  await signIn(page);
  await page.getByRole('button', { name: /^Follow-up & Reminders/ }).click();
  const followup = page.getByRole('row').filter({ hasText: 'FU-501' });
  await followup.getByRole('button', { name: 'WhatsApp preview' }).click();
  await expect(page.getByRole('dialog')).toContainText('This is a preview. No external message will be sent.');
  await page.getByRole('dialog').getByRole('button', { name: 'Close', exact: true }).last().click();
  await followup.getByRole('button', { name: 'Mark booked' }).click();
  await expect(followup).toContainText('Booked');

  await page.getByRole('button', { name: /^Instant Reminder/ }).click();
  const reminder = page.getByRole('row').filter({ hasText: 'IR-1001' });
  await reminder.getByRole('button', { name: 'Preview' }).click();
  await expect(page.getByRole('dialog')).toContainText('No external message will be sent');
  await page.getByRole('button', { name: 'Mark as sent (demo)' }).click();
  await expect(page.getByText('Demo status updated — no external message was sent.')).toBeVisible();
});

test('bed status cycling and invoice payment collection update persisted records', async ({ page, request }) => {
  await signIn(page);
  await page.getByRole('button', { name: /^Beds & Wards/ }).click();
  const bed = page.getByRole('row').filter({ hasText: 'BED-01' });
  await bed.getByRole('button', { name: 'Change status' }).click();
  await expect(bed).toContainText('Occupied');
  let record = await request.get('http://127.0.0.1:8080/api/clinic-a/beds/BED-01');
  expect((await record.json()).status).toBe('Occupied');

  await page.getByRole('button', { name: /^Billing & Payments/ }).click();
  const invoice = page.getByRole('row').filter({ hasText: 'INV-801' });
  await invoice.getByRole('button', { name: 'Collect payment' }).click();
  await expect(invoice).toContainText('Paid');
  record = await request.get('http://127.0.0.1:8080/api/clinic-a/billing/INV-801');
  const invoiceData = await record.json();
  expect(invoiceData.status).toBe('Paid');
  expect(invoiceData.paid).toBe(invoiceData.amount);
});

test('settings persist clinic edits and restore just that clinic sample data', async ({ page, request }) => {
  await signIn(page);
  await page.getByRole('button', { name: /^Settings/ }).click();
  await expect(page.getByRole('heading', { name: 'Settings', exact: true })).toBeVisible();
  await expect(page.getByText('Persistent H2 storage')).toBeVisible();
  await page.getByLabel('Clinic / hospital name').fill('ClincyAnywhere Test Workspace');
  await page.getByRole('button', { name: 'Save clinic profile' }).click();
  await expect(page.getByRole('combobox', { name: 'Clinic workspace' })).toContainText('ClincyAnywhere Test Workspace');
  const profile = await request.get('http://127.0.0.1:8080/api/clinic-a/settings');
  expect((await profile.json())[0].name).toBe('ClincyAnywhere Test Workspace');

  page.once('dialog', dialog => dialog.accept());
  await page.getByRole('button', { name: 'Reset' }).click();
  await expect(page.getByText('This clinic has been restored to the original sample dataset.')).toBeVisible();
  const restored = await request.get('http://127.0.0.1:8080/api/clinics');
  expect((await restored.json()).find(clinic => clinic.id === 'clinic-a').name).toBe('ClincyAnywhere Health Centre');
  const patientRows = await request.get('http://127.0.0.1:8080/api/clinic-a/patients');
  expect((await patientRows.json()).length).toBe(6);
});

test('CSV and JSON exports download from records, reports, and settings', async ({ page }) => {
  await signIn(page);
  await page.getByRole('button', { name: /^Patients/ }).click();
  const csvDownload = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Export CSV' }).click();
  expect((await csvDownload).suggestedFilename()).toBe('patients.csv');

  await page.getByRole('button', { name: /^Reports & Analytics/ }).click();
  const reportDownload = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Export report' }).click();
  expect((await reportDownload).suggestedFilename()).toBe('clincanywhere-report.json');

  await page.getByRole('button', { name: /^Settings/ }).click();
  const settingsDownload = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Export JSON' }).click();
  expect((await settingsDownload).suggestedFilename()).toBe('clincanywhere-demo-data.json');
});
