import { formatTemporalValueIST } from './formatters.js';

/**
 * Download browser-generated content as a file.
 * CSV cells are quoted and formula-like values are prefixed to avoid spreadsheet execution.
 */
export function downloadRows(rows, filename, format = 'csv') {
  const displayRows = format === 'json' ? rows : rows.map(row => Object.fromEntries(
    Object.entries(row).map(([key, value]) => [key, formatTemporalValueIST(key, value)]),
  ));
  const csvCell = value => {
    const text = String(value ?? '');
    const safeText = /^[\s]*[=+@-]/.test(text) ? `'${text}` : text;
    return `"${safeText.replaceAll('"', '""')}"`;
  };
  const body = format === 'json'
    ? JSON.stringify(displayRows, null, 2)
    : [
        Object.keys(displayRows[0] ?? {}).map(csvCell).join(','),
        ...displayRows.map(row => Object.values(row).map(csvCell).join(',')),
      ].join('\n');
  const url = URL.createObjectURL(new Blob([body], { type: format === 'json' ? 'application/json' : 'text/csv' }));
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
}
