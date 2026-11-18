import { expect, test } from '@playwright/test';

const modules = [
  { nav: 'Patients', add: 'Register patient', name: 'Full name', value: token => `QA Patient ${token}` },
  { nav: 'Appointments', add: 'Book appointment', name: 'Department', value: token => `QA Department ${token}` },
  { nav: 'Doctors & Staff', add: 'Add doctor / staff', name: 'Full name', value: token => `Dr QA ${token}` },
  { nav: 'Medical Records', add: 'Create medical record', name: 'Diagnosis', value: token => `QA diagnosis ${token}` },
  { nav: 'Prescriptions', add: 'Create prescription', name: 'Medicines (clinician-approved)', value: token => `QA medication ${token}` },
  { nav: 'Follow-up & Reminders', add: 'Schedule follow-up', name: 'Follow-up date' },
  { nav: 'Instant Reminder', add: 'Create instant reminder', name: 'Patient name', value: token => `QA patient ${token}` },
  { nav: 'Laboratory', add: 'Add laboratory order', name: 'Test name', value: token => `QA lab test ${token}` },
  { nav: 'Pharmacy', add: 'Add pharmacy item', name: 'Medicine name', value: token => `QA medicine ${token}` },
  { nav: 'Beds & Wards', add: 'Add bed / ward', name: 'Ward', value: token => `QA ward ${token}` },
  { nav: 'Billing & Payments', add: 'Create invoice', name: 'Description', value: token => `QA invoice ${token}` },
  { nav: 'Inventory', add: 'Add inventory item', name: 'Item name', value: token => `QA item ${token}` },
];

async function signIn(page) {
  await page.goto('/');
  await page.getByRole('button', { name: /Enter workspace/ }).click();
  await expect(page.getByRole('heading', { name: 'Your clinic at a glance' })).toBeVisible();
  await expect(page.getByText('Could not load clinic data.')).toHaveCount(0);
}

async function completeNewRecordForm(page, module, token) {
  const fields = page.locator('.modal-body .field-label');
  for (let index = 0; index < await fields.count(); index += 1) {
    const field = fields.nth(index);
    const label = await field.evaluate(element => element.childNodes[0]?.textContent.trim() ?? '');
    const input = field.locator('input, select, textarea');
    const type = await input.getAttribute('type');
    const required = await input.evaluate(element => element.required);
    if (await input.evaluate(element => element.tagName === 'SELECT')) {
      if (required) {
        await input.selectOption({ index: 1 });
      }
    } else if (label === module.name) {
      await input.fill(module.value ? module.value(token) : '2026-12-15');
    } else if (type === 'date') {
      await input.fill('2026-12-15');
    } else if (type === 'time') {
      await input.fill('10:30');
    } else if (type === 'number') {
      await input.fill('3');
    } else if (type === 'email') {
      await input.fill(`qa-${token.toLowerCase()}@example.test`);
    } else if (type === 'tel') {
      await input.fill('9876501234');
    } else if (required) {
      await input.fill(`QA ${token}`);
    }
  }
}

test('create, edit, persist, search, and delete a record in every management module', async ({ page, request }) => {
  test.setTimeout(180_000);
  await signIn(page);

  for (const module of modules) {
    const token = `${Date.now()}${Math.floor(Math.random() * 1000)}`;
    await page.getByRole('button', { name: new RegExp(`^${module.nav}`) }).click();
    await expect(page.getByRole('heading', { name: module.nav, exact: true })).toBeVisible();
    await expect(page.getByText('Could not load clinic data.')).toHaveCount(0);
    const responsePromise = page.waitForResponse(response => response.request().method() === 'POST'
      && response.url().includes(`/api/clinic-a/${module.nav === 'Patients' ? 'patients' : categoryFor(module.nav)}`));
    await page.getByRole('button', { name: module.add, exact: true }).click();
    await completeNewRecordForm(page, module, token);
    await page.getByRole('button', { name: 'Add record', exact: true }).click();
    const createdResponse = await responsePromise;
    expect(createdResponse.status(), `${module.nav} create response`).toBe(201);
    const created = await createdResponse.json();
    const recordId = created.id;
    expect(recordId, `${module.nav} generated id`).toBeTruthy();
    const row = page.getByRole('row').filter({ hasText: recordId });
    await expect(row).toBeVisible();

    const updatePromise = page.waitForResponse(response => response.request().method() === 'PUT'
      && response.url().includes(`/api/clinic-a/${categoryFor(module.nav)}/${recordId}`));
    await row.getByRole('button', { name: 'Edit' }).click();
    const firstInput = page.locator('.modal-body input, .modal-body select, .modal-body textarea').first();
    if (await firstInput.evaluate(element => element.tagName === 'SELECT')) {
      await firstInput.selectOption({ index: 2 });
    } else {
      await firstInput.fill(module.value ? `${module.value(token)} edited` : '2026-12-22');
    }
    await page.getByRole('button', { name: 'Save changes' }).click();
    const updateResponse = await updatePromise;
    expect(updateResponse.status(), `${module.nav} update response`).toBe(200);
    await expect(row).toBeVisible();

    const persisted = await request.get(`http://127.0.0.1:8080/api/clinic-a/${categoryFor(module.nav)}/${recordId}`);
    expect(persisted.status(), `${module.nav} persistence lookup`).toBe(200);
    expect((await persisted.json()).id).toBe(recordId);

    const search = page.getByPlaceholder(`Search ${module.nav.toLowerCase()}…`);
    await search.fill(recordId);
    await expect(row).toBeVisible();
    await search.fill('no-such-record');
    await expect(page.getByText('No matching records found.')).toBeVisible();
    await search.fill('');

    page.once('dialog', dialog => dialog.accept());
    const deletePromise = page.waitForResponse(response => response.request().method() === 'DELETE'
      && response.url().includes(`/api/clinic-a/${categoryFor(module.nav)}/${recordId}`));
    await row.getByRole('button', { name: 'Delete' }).click();
    const deleteResponse = await deletePromise;
    expect(deleteResponse.status(), `${module.nav} delete response`).toBe(204);
    await expect(row).toHaveCount(0);
    const gone = await request.get(`http://127.0.0.1:8080/api/clinic-a/${categoryFor(module.nav)}/${recordId}`);
    expect(gone.status(), `${module.nav} delete persistence`).toBe(404);
  }
});

function categoryFor(label) {
  return {
    Patients: 'patients',
    Appointments: 'appointments',
    'Doctors & Staff': 'doctors',
    'Medical Records': 'records',
    Prescriptions: 'prescriptions',
    'Follow-up & Reminders': 'followups',
    'Instant Reminder': 'instantreminders',
    Laboratory: 'laboratory',
    Pharmacy: 'pharmacy',
    'Beds & Wards': 'beds',
    'Billing & Payments': 'billing',
    Inventory: 'inventory',
  }[label];
}
