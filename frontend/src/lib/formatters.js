const IST_TIME_ZONE = 'Asia/Kolkata';
const IST_OFFSET_MINUTES = 330;
const DATE_ONLY_FIELDS = new Set(['date', 'dueDate', 'followupDate', 'expiry', 'joined', 'startDate', 'endDate', 'appointmentDate', 'birthDate', 'dateOfBirth']);
const DATE_TIME_FIELDS = new Set(['createdAt', 'updatedAt', 'sentAt', 'dispensedAt', 'finalizedAt', 'expiresAt', 'occurredAt', 'receivedAt', 'processedAt', 'nextAttemptAt', 'timestamp']);

function parseDate(value) {
  if (value instanceof Date) return value;
  if (typeof value !== 'string' || !value) return null;
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) return new Date(`${value}T00:00:00+05:30`);
  const hasOffset = /(?:[zZ]|[+-]\d{2}:?\d{2})$/.test(value);
  return new Date(/^\d{4}-\d{2}-\d{2}T/.test(value) && !hasOffset ? `${value}+05:30` : value);
}

function isValidDate(value) {
  return value instanceof Date && !Number.isNaN(value.getTime());
}

function getISTParts(value, fields) {
  return new Intl.DateTimeFormat('en-IN', { timeZone: IST_TIME_ZONE, ...fields })
    .formatToParts(value)
    .reduce((parts, { type, value: part }) => ({ ...parts, [type]: part }), {});
}

export function statusClass(value) {
  const text = String(value ?? '').toLowerCase();
  if (['active', 'available', 'paid', 'dispensed', 'ready', 'completed', 'booked'].some(part => text.includes(part))) return 'green';
  if (['waiting', 'pending', 'follow-up', 'scheduled', 'new', 'ordered', 'draft'].some(part => text.includes(part))) return 'orange';
  if (['cancel', 'surgery', 'urgent', 'maintenance'].some(part => text.includes(part))) return 'red';
  if (['confirmed', 'processing', 'checked', 'reviewed', 'collected'].some(part => text.includes(part))) return 'blue';
  return 'gray';
}

export function initials(name = '') {
  return name.split(/\\s+/).filter(Boolean).slice(0, 2).map(part => part[0]).join('').toUpperCase() || 'CP';
}

export function formatDateIST(value, options = {}) {
  if (value == null || value === '') return '';
  const date = parseDate(value);
  if (!isValidDate(date)) return String(value);
  return new Intl.DateTimeFormat('en-IN', {
    timeZone: IST_TIME_ZONE,
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    ...options,
  }).format(date);
}

export function formatDateTimeIST(value) {
  if (value == null || value === '') return '';
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value)) return formatDateIST(value);
  const date = parseDate(value);
  if (!isValidDate(date)) return String(value);
  const formatted = new Intl.DateTimeFormat('en-IN', {
    timeZone: IST_TIME_ZONE,
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  }).format(date);
  return `${formatted} IST`;
}

export function formatDateTimePartsIST(date, time) {
  if (!date) return time ? `${time} IST` : '';
  if (!time) return formatDateIST(date);
  const parsed = parseDate(`${date}T${time.length === 5 ? `${time}:00` : time}+05:30`);
  return isValidDate(parsed) ? formatDateTimeIST(parsed) : `${formatDateIST(date)}, ${time} IST`;
}

export function formatTemporalValueIST(field, value) {
  if (value == null || value === '') return value;
  if (DATE_ONLY_FIELDS.has(field)) return formatDateIST(value);
  if (DATE_TIME_FIELDS.has(field)) return formatDateTimeIST(value);
  if (field === 'time' && /^\d{1,2}:\d{2}$/.test(String(value))) return `${value} IST`;
  return value;
}

export function dateTimeLocalToISOStringIST(value) {
  const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})$/.exec(value ?? '');
  if (!match) return '';
  const [, year, month, day, hour, minute] = match.map(Number);
  const calendarDate = new Date(Date.UTC(year, month - 1, day));
  if (calendarDate.getUTCFullYear() !== year || calendarDate.getUTCMonth() !== month - 1 || calendarDate.getUTCDate() !== day || hour > 23 || minute > 59) return '';
  return new Date(Date.UTC(year, month - 1, day, hour, minute) - IST_OFFSET_MINUTES * 60_000).toISOString();
}

export function todayISO() {
  const parts = getISTParts(new Date(), { year: 'numeric', month: '2-digit', day: '2-digit' });
  return `${parts.year}-${parts.month}-${parts.day}`;
}

export function addDaysToISODate(value, days = 0) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value ?? '');
  if (!match) return '';
  const [, year, month, day] = match.map(Number);
  const date = new Date(Date.UTC(year, month - 1, day + days));
  return `${date.getUTCFullYear()}-${String(date.getUTCMonth() + 1).padStart(2, '0')}-${String(date.getUTCDate()).padStart(2, '0')}`;
}

export function nowISOStringIST(value = new Date()) {
  const date = parseDate(value);
  if (!isValidDate(date)) return '';
  const parts = getISTParts(date, {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23',
  });
  return `${parts.year}-${parts.month}-${parts.day}T${parts.hour}:${parts.minute}:${parts.second}+05:30`;
}
