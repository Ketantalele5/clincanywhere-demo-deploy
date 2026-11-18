import { expect, test } from '@playwright/test';

const roleCases = [
  ['Administrator', ['Patients', 'Appointments', 'Doctors & Staff', 'Medical Records', 'Prescriptions', 'Follow-up & Reminders', 'Instant Reminder', 'Laboratory', 'Pharmacy', 'Beds & Wards', 'Billing & Payments', 'Inventory', 'Reports & Analytics', 'Settings']],
  ['Receptionist', ['Patients', 'Appointments', 'Doctors & Staff', 'Billing & Payments', 'Follow-up & Reminders', 'Instant Reminder']],
  ['Doctor', ['Patients', 'Appointments', 'Doctors & Staff', 'Medical Records', 'Prescriptions', 'Follow-up & Reminders', 'Instant Reminder', 'Laboratory']],
  ['Laboratory Technician', ['Patients', 'Medical Records', 'Laboratory']],
  ['Pharmacist', ['Patients', 'Prescriptions', 'Pharmacy']],
  ['Billing Executive', ['Patients', 'Appointments', 'Billing & Payments', 'Reports & Analytics']],
  ['Inventory Manager', ['Pharmacy', 'Inventory', 'Reports & Analytics']],
];

test('validates demo login and rejects incorrect credentials', async ({ page }) => {
  await page.goto('/');
  await page.getByLabel('Username').fill('not-a-demo-user');
  await page.getByRole('button', { name: /Enter workspace/ }).click();
  await expect(page.getByText(/Use the demo username shown/)).toBeVisible();
  await page.getByRole('button', { name: 'DR Doctor' }).click();
  await expect(page.getByLabel('Username')).toHaveValue('doctor');
  await expect(page.getByLabel('Password')).toHaveValue('demo123');
  await page.getByRole('button', { name: /Enter workspace/ }).click();
  await expect(page.getByRole('heading', { name: 'Your clinic at a glance' })).toBeVisible();
  await expect(page.getByText('Could not load clinic data.')).toHaveCount(0);
});

test('clinic loading errors are visible and retry recovers when the API returns', async ({ page }) => {
  await page.route('**/api/clinic-a/patients', route => route.abort('failed'));
  await page.goto('/');
  await page.getByRole('button', { name: /Enter workspace/ }).click();
  await expect(page.getByText('Could not load clinic data.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Retry' })).toBeVisible();

  await page.unroute('**/api/clinic-a/patients');
  await page.getByRole('button', { name: 'Retry' }).click();
  await expect(page.getByText('Could not load clinic data.')).toHaveCount(0);
  await page.getByRole('button', { name: /^Patients/ }).click();
  await expect(page.getByText('6 records')).toBeVisible();
});

test('every demo role sees its authorized navigation and dashboard', async ({ page }) => {
  await page.goto('/');
  for (const [role, allowed] of roleCases) {
    if (role !== 'Administrator') {
      await page.getByRole('button', { name: 'Log out' }).click();
      await expect(page.getByRole('heading', { name: 'Sign in to your workspace' })).toBeVisible();
    }
    await page.getByRole('button', { name: new RegExp(role) }).click();
    await page.getByRole('button', { name: /Enter workspace/ }).click();
    await expect(page.getByRole('heading', { name: 'Your clinic at a glance' })).toBeVisible();
    await expect(page.getByText('Could not load clinic data.')).toHaveCount(0);
    for (const label of allowed) {
      await expect(page.getByRole('button', { name: new RegExp(`^${label}`) }).first()).toBeVisible();
    }
    const forbidden = role === 'Administrator' ? [] : roleCases
      .flatMap(([, labels]) => labels)
      .filter(label => !allowed.includes(label));
    for (const label of [...new Set(forbidden)]) {
      await expect(page.getByRole('button', { name: new RegExp(`^${label}`) })).toHaveCount(0);
    }
  }
});

test('administrator can navigate every module and switch isolated clinic workspaces', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('button', { name: /Enter workspace/ }).click();
  const modules = [
    ['Patients', 'Patients'], ['Appointments', 'Appointments'], ['Doctors & Staff', 'Doctors & Staff'],
    ['Medical Records', 'Medical Records'], ['Prescriptions', 'Prescriptions'],
    ['Follow-up & Reminders', 'Follow-up & Reminders'], ['Instant Reminder', 'Instant Reminder'],
    ['Laboratory', 'Laboratory'], ['Pharmacy', 'Pharmacy'], ['Beds & Wards', 'Beds & Wards'],
    ['Billing & Payments', 'Billing & Payments'], ['Inventory', 'Inventory'],
    ['Reports & Analytics', 'Reports & Analytics'], ['Settings', 'Settings'],
  ];
  for (const [buttonName, heading] of modules) {
    await page.getByRole('button', { name: new RegExp(`^${buttonName}`) }).click();
    await expect(page.getByRole('heading', { name: heading, exact: true })).toBeVisible();
    await expect(page.getByText('Could not load clinic data.')).toHaveCount(0);
  }
  await page.getByRole('button', { name: 'Overview' }).click();
  await page.getByLabel('Clinic workspace').selectOption('clinic-b');
  await expect(page.getByText('Sunrise Family Clinic', { exact: true }).last()).toBeVisible();
  await expect(page.getByText('Could not load clinic data.')).toHaveCount(0);
  await page.getByRole('button', { name: /^Patients/ }).click();
  await expect(page.getByText('6 records')).toBeVisible();
  await page.getByLabel('Clinic workspace').selectOption('clinic-a');
  await expect(page.getByText('6 records')).toBeVisible();
  await page.getByRole('button', { name: 'Notifications' }).click();
  await expect(page.getByRole('heading', { name: 'Follow-up & Reminders', exact: true })).toBeVisible();
});
