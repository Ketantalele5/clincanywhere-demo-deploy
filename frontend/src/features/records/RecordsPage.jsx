import { useEffect, useRef, useState } from 'react';
import { Download, Edit2, Plus, Search, Trash2 } from 'lucide-react';
import { Alert, Button, Card, Dialog, Input, Label, Select, Spinner, Table, TableBody, TableCell, TableHead, TableHeader, TableRow, Textarea } from '../../components/ui/index.js';
import { PersonCell, StatusBadge } from '../../components/shared/RecordDisplay.jsx';
import { roleWrites, schemas, titles } from '../../lib/appConfig.js';
import { useLanguage } from '../../lib/i18n.js';
import { apiRequest } from '../../lib/apiClient.js';
import { formatDateTimePartsIST, formatTemporalValueIST } from '../../lib/formatters.js';

function formatRecordValue(field, record) {
  if (field === 'dateTime') return formatDateTimePartsIST(record.date, record.time) || '—';
  return formatTemporalValueIST(field, record[field]) ?? '—';
}

export function RecordsPage({ category, records, search, onSearchChange, onSearchRecords, hasMore, onLoadMore, roleId, onAdd, onEdit, onDelete, onExport }) {
  const { t } = useLanguage();
  const schema = schemas[category];
  const canWrite = roleId === 'admin' || (roleWrites[roleId] ?? []).includes(category);
  const filtered = onSearchRecords
    ? records
    : records.filter(record => Object.values(record).join(' ').toLowerCase().includes(search.toLowerCase()));
  const timer = useRef(null);
  useEffect(() => {
    if (!onSearchRecords) return undefined;
    window.clearTimeout(timer.current);
    if (!search.trim()) return undefined;
    timer.current = window.setTimeout(() => {
      onSearchRecords(category, search).catch(() => {});
    }, 250);
    return () => window.clearTimeout(timer.current);
  }, [category, onSearchRecords, search]);
  return <><div className="page-heading"><div><h1>{t(category)||titles[category]}</h1><p>{t(category)||titles[category]} · {records.length}</p></div><div className="heading-actions"><Button variant="outline" onClick={() => onExport(records, `${category}.csv`)}><Download size={15} /> Export CSV</Button>{canWrite && <Button variant="primary" onClick={onAdd}><Plus size={16} /> {schema.title}</Button>}</div></div>
    {category === 'appointments' && <p className="muted">Telemedicine links open your clinic’s external video service. Configure and approve that provider separately; this app does not host calls or send links to patients.</p>}
    <Card className="data-card"><div className="table-toolbar"><span>{filtered.length} records loaded</span><label className="table-search"><Search size={15} /><Input aria-label={`Search ${titles[category]}`} value={search} onChange={event => onSearchChange(event.target.value)} placeholder="Search clinic records..." /></label></div><div className="table-scroll"><Table><TableHeader><TableRow>{schema.columns.map(([label]) => <TableHead key={label}>{label}</TableHead>)}<TableHead>Actions</TableHead></TableRow></TableHeader><TableBody>{filtered.map(record => <TableRow key={record.id}>{schema.columns.map(([, field, type]) => <TableCell key={field}>{type === 'person' ? <PersonCell name={record[field]} /> : field === 'videoLink' && record[field] ? <SafeMeetingLink href={record[field]} /> : field === 'status' || field.endsWith('Status') ? <StatusBadge value={record[field]} /> : formatRecordValue(field, record)}</TableCell>)}<TableCell><div className="row-actions">{canWrite && <><Button variant="ghost" aria-label="Edit" title="Edit" onClick={() => onEdit(record)}><Edit2 size={14} /></Button><Button variant="ghost" aria-label="Delete" title="Delete" onClick={() => onDelete(record)}><Trash2 size={14} /></Button></>}</div></TableCell></TableRow>)}</TableBody></Table>{!filtered.length && <div className="empty-inline" role="status">{records.length ? t('noRecordsMatch') : t('noRecordsYet')}</div>}</div>{hasMore && !search && <Button variant="outline" onClick={onLoadMore}>Load more records</Button>}</Card>
  </>;
}

export function RecordDialog({ modal, data, roleId, clinicId, onClose, onSave }) {
  const [saving, setSaving] = useState(false);
  const [validationError, setValidationError] = useState('');
  useEffect(() => { setSaving(false); }, [modal]);
  if (!modal) return null;
  const { category, record } = modal;
  const schema = schemas[category];
  async function submit(event) {
    event.preventDefault(); setSaving(true); setValidationError('');
    const formData = new FormData(event.currentTarget);
    const values = Object.fromEntries(formData.entries());
    for (const [, key, type] of schema.fields) if (type === 'number' && values[key] !== '') values[key] = Number(values[key]);
    try {
      if (category === 'appointments' && values.visitMode === 'Telemedicine') {
        if (!values.videoLink) throw new Error('Add the video visit URL supplied by your clinic’s approved provider.');
        const meeting = new URL(values.videoLink);
        if (meeting.protocol !== 'https:') throw new Error('Video visit links must use HTTPS.');
      }
      const nextRecord = { ...record, ...values, ...(record?.id ? { id: record.id } : {}) };
      if (category === 'billing' && !record) Object.assign(nextRecord, { paid: 0, status: 'Pending' });
      await onSave(category, nextRecord, !record);
    }
    catch (saveError) { setValidationError(saveError.message); }
    finally { setSaving(false); }
  }
  return <Dialog open={Boolean(modal)} onOpenChange={open => !open && onClose()} title={record ? `Edit ${category}` : schema.title} description={`Changes are saved to ${clinicIdLabel(data)}`} className="modal-card">
    <form onSubmit={submit}><div className="modal-body form-grid">{schema.fields.map(([key, label, type, required, options]) => <Field key={key} fieldKey={key} label={label} type={type} required={required} options={options} value={record?.[key]} data={data} clinicId={clinicId} />)}{validationError && <Alert variant="destructive" className="form-error">{validationError}</Alert>}</div><div className="modal-footer"><span className="muted">{record ? 'Update this record' : 'New record'}</span><div><Button variant="outline" onClick={onClose}>Cancel</Button><Button type="submit" variant="primary" disabled={saving}>{saving ? <Spinner /> : null}{record ? 'Save changes' : 'Add record'}</Button></div></div></form>
  </Dialog>;
}

function SafeMeetingLink({ href }) {
  try {
    const meeting = new URL(href);
    if (meeting.protocol !== 'https:') return <>Invalid video link</>;
    return <a href={meeting.toString()} target="_blank" rel="noopener noreferrer">Join video visit</a>;
  } catch { return <>Invalid video link</>; }
}
function clinicIdLabel() { return 'this clinic workspace'; }

function PatientField({ fieldKey, label, required, value, clinicId }) {
  const [selectedId, setSelectedId] = useState(value ?? '');
  const [query, setQuery] = useState('');
  const [matches, setMatches] = useState([]);
  useEffect(() => {
    let active = true;
    const timer = window.setTimeout(() => apiRequest(`/api/${clinicId}/patients?q=${encodeURIComponent(query)}&size=30`)
      .then(rows => { if (active) setMatches(rows); }).catch(() => { if (active) setMatches([]); }), 250);
    return () => { active = false; window.clearTimeout(timer); };
  }, [clinicId, query]);
  return <Label htmlFor={`field-${fieldKey}`}>{label}<Input value={query} onChange={event => { setQuery(event.target.value); setSelectedId(''); }} placeholder="Search patient by exact name or ID" />
    <Select id={`field-${fieldKey}`} name={fieldKey} required={Boolean(required)} value={selectedId} onChange={event => setSelectedId(event.target.value)}><option value="">Select patient</option>{matches.map(patient => <option value={patient.id} key={patient.id}>{patient.name ?? patient.id} · {patient.id}</option>)}</Select></Label>;
}
function Field({ fieldKey, label, type, required, options, value, data, clinicId }) {
  if (options === 'patients') return <PatientField fieldKey={fieldKey} label={label} required={required} value={value} clinicId={clinicId} />;
  const choices = Array.isArray(options) ? options : typeof options === 'string' ? (data[options] ?? []).map(item => item.name) : [];
  const common = { id: `field-${fieldKey}`, name: fieldKey, required: Boolean(required), defaultValue: value ?? '' };
  return <Label htmlFor={common.id} className={type === 'textarea' ? 'field-full' : ''}>{label}{type === 'select' ? <Select {...common}><option value="">Select {label.toLowerCase()}</option>{choices.map(option => <option value={option} key={option}>{option}</option>)}</Select> : type === 'textarea' ? <Textarea {...common} rows={3} /> : <Input {...common} type={type || 'text'} />}</Label>;
}


