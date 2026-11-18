import { useEffect, useMemo, useState } from 'react';
import { apiRequest } from '../../lib/apiClient.js';
import { Button, Card, Dialog, Input, Select } from '../../components/ui/index.js';
import { formatDateIST, formatDateTimeIST, todayISO } from '../../lib/formatters.js';

export function RecoveryPage({ clinicId, data, roleId, onRefresh, onNotify }) {
  const [saving, setSaving] = useState('');
  const [query, setQuery] = useState('');
  const [deliveryHistory, setDeliveryHistory] = useState([]);
  const [reminder, setReminder] = useState(null);
  const [channel, setChannel] = useState('WHATSAPP');
  const [destination, setDestination] = useState('');
  const [evidenceReference, setEvidenceReference] = useState('');
  const [consentError, setConsentError] = useState('');
  const [consentBusy, setConsentBusy] = useState(false);
  const canManageConsent = roleId === 'admin' || roleId === 'doctor';
  const patients = data.patients ?? [];
  const follows = data.followups ?? [];
  const appointments = data.appointments ?? [];
  async function refreshDeliveryHistory() { try { setDeliveryHistory(await apiRequest(`/api/${clinicId}/whatsapp/notifications`)); } catch { setDeliveryHistory([]); } }
  useEffect(() => { refreshDeliveryHistory(); }, [clinicId]);
  const rows = useMemo(() => {
    const followupRows = follows.filter(row => row.returnStatus !== 'Returned' && row.appointmentStatus !== 'Booked' && String(row.dueDate ?? '') <= todayISO()).map(row => ({ id: row.id, patient: row.patient, dueDate: row.dueDate, kind: 'Follow-up due', source: 'followups', row }));
    const apptRows = appointments.filter(row => ['No-show', 'Missed'].includes(row.status) || (row.date < todayISO() && !['Completed', 'Checked in', 'Cancelled'].includes(row.status))).map(row => ({ id: row.id, patient: row.patient, dueDate: row.date, kind: ['No-show', 'Missed'].includes(row.status) ? 'Missed appointment' : 'Past appointment outcome missing', source: 'appointments', row }));
    const q = query.trim().toLowerCase();
    return [...followupRows, ...apptRows].filter(item => !q || `${item.patient} ${item.id} ${item.kind}`.toLowerCase().includes(q)).sort((a, b) => String(a.dueDate).localeCompare(String(b.dueDate)));
  }, [follows, appointments, query]);

  async function update(item, patch) {
    setSaving(item.id);
    try {
      await apiRequest(`/api/${clinicId}/${item.source}/${encodeURIComponent(item.id)}`, { method: 'PUT', body: JSON.stringify({ ...item.row, ...patch }) });
      onRefresh();
      onNotify('Patient recovery record updated');
    } catch (error) { onNotify(error.message); } finally { setSaving(''); }
  }

  function openReminder(item) {
    const person = patients.find(patient => patient.name === item.patient);
    setReminder(item);
    setChannel('WHATSAPP');
    setDestination(person?.phone ?? '');
    setEvidenceReference('');
    setConsentError('');
  }

  async function recordExistingConsent() {
    const recipient = destination.trim();
    const evidence = evidenceReference.trim();
    if (!recipient || !evidence) {
      setConsentError('Enter the recipient and the reference for consent already recorded by your clinic.');
      return;
    }
    setConsentBusy(true);
    setConsentError('');
    try {
      const path = channel === 'WHATSAPP' ? 'consents' : 'notifications/consents';
      const body = channel === 'WHATSAPP'
        ? { phoneNumber: recipient, evidenceReference: evidence }
        : { channel, destination: recipient, action: 'GRANTED', evidenceReference: evidence };
      await apiRequest(`/api/${clinicId}/whatsapp/${path}`, { method: 'POST', body: JSON.stringify(body) });
      onNotify('Existing consent evidence reference recorded');
    } catch (error) {
      setConsentError(error.message);
    } finally {
      setConsentBusy(false);
    }
  }

  async function sendReminder(event) {
    event.preventDefault();
    if (!reminder) return;
    const item = reminder;
    const recipient = destination.trim();
    const evidence = evidenceReference.trim();
    if (!recipient || !evidence) return onNotify('Recipient and existing consent evidence reference are required');
    setSaving(`message-${item.id}`);
    const message = `Hello ${item.patient}, this is your ${item.kind.toLowerCase()} reminder from your clinic. Please contact us to schedule your visit.`;
    try {
      const result = await apiRequest(`/api/${clinicId}/whatsapp/notifications`, { method: 'POST', body: JSON.stringify({ channel, destination: recipient, message, consentConfirmed: true }) });
      await refreshDeliveryHistory();
      setReminder(null);
      onNotify(`${channel} provider status: ${result.status}`);
    } catch (error) { onNotify(error.message); }
    finally { setSaving(''); }
  }

  return <>
    <div className="page-heading"><div><h1>Patient recovery</h1><p>Overdue follow-ups, missed visits and contact outcomes</p></div><Input aria-label="Search recovery queue" placeholder="Search patient" value={query} onChange={event => setQuery(event.target.value)} style={{ maxWidth: 260 }} /></div>
    <div className="stats-grid"><Card className="stat-card"><div className="stat-top">Action needed</div><div className="stat-value">{rows.length}</div></Card><Card className="stat-card"><div className="stat-top">Today due</div><div className="stat-value">{rows.filter(row => row.dueDate <= todayISO()).length}</div></Card><Card className="stat-card"><div className="stat-top">Returned</div><div className="stat-value">{follows.filter(row => row.returnStatus === 'Returned').length}</div></Card></div>
    {deliveryHistory.length > 0 && <Card style={{ padding: 18, marginTop: 14 }}><h3>Recent SMS and email provider results</h3><p>ACCEPTED means the provider accepted the request; it does not confirm delivery to the patient.</p>{deliveryHistory.slice(0, 8).map(item => <div key={item.id} className="setting-row"><div><b>{item.channel} · #{item.id}</b><small>{item.createdAt ? formatDateTimeIST(item.createdAt) : ''}</small></div><span>{item.status}</span></div>)}</Card>}
    <div className="dashboard-grid" style={{ marginTop: 18 }}>{rows.map(item => <Card key={`${item.source}-${item.id}`} style={{ padding: 18 }}><h3>{item.patient || 'Unknown patient'}</h3><p>{item.kind} · {formatDateIST(item.dueDate) || 'Date not set'} · {item.id}</p>{item.source === 'followups' && <><label>Contact outcome <Select value={item.row.contactStatus || 'Not contacted'} onChange={event => update(item, { contactStatus: event.target.value })}><option>Not contacted</option><option>Contacted</option><option>Reached</option><option>No answer</option></Select></label><div style={{ height: 9 }} /><label>Return status <Select value={item.row.returnStatus || 'Not returned'} onChange={event => update(item, { returnStatus: event.target.value, appointmentStatus: event.target.value === 'Returned' ? 'Booked' : item.row.appointmentStatus })}><option>Not returned</option><option>Returned</option></Select></label></>}<div style={{ display: 'flex', gap: 8, marginTop: 14 }}><Button disabled={Boolean(saving)} onClick={() => openReminder(item)}>Send reminder</Button>{item.source === 'appointments' && <Button variant="outline" disabled={Boolean(saving)} onClick={() => update(item, { status: 'Follow-up needed' })}>Add to follow-up</Button>}</div></Card>)}{!rows.length && <Card style={{ padding: 22 }}>No overdue recovery items. Missed appointments and due follow-ups will appear here.</Card>}</div>
    <Dialog open={Boolean(reminder)} onOpenChange={open => { if (!open && !saving) setReminder(null); }} title="Send patient reminder" description="Send only when the clinic has already recorded active consent for this recipient and channel.">
      {reminder && <form onSubmit={sendReminder} style={{ display: 'grid', gap: 12 }}>
        <p>Reminder for {reminder.patient || 'Unknown patient'} ({reminder.kind}). Use only contact details and consent evidence already recorded by your clinic.</p>
        <label>Channel<Select value={channel} onChange={event => {
          const nextChannel = event.target.value;
          const patient = patients.find(person => person.name === reminder.patient);
          setChannel(nextChannel);
          setDestination(nextChannel === 'EMAIL' ? patient?.email ?? '' : patient?.phone ?? '');
          setEvidenceReference('');
          setConsentError('');
        }}><option value="WHATSAPP">WhatsApp</option><option value="SMS">SMS</option><option value="EMAIL">Email</option></Select></label>
        <label>{channel === 'EMAIL' ? 'Email address' : channel === 'WHATSAPP' ? 'Phone number in international format' : 'Phone number'}<Input required type={channel === 'EMAIL' ? 'email' : 'tel'} placeholder={channel === 'WHATSAPP' ? '+919876543210' : ''} value={destination} onChange={event => { setDestination(event.target.value); setConsentError(''); }} maxLength={200} />{channel === 'WHATSAPP' && <small>WhatsApp consent records use the full number with country code, such as +919876543210.</small>}</label>
        <label>Existing consent evidence reference<Input required value={evidenceReference} onChange={event => { setEvidenceReference(event.target.value); setConsentError(''); }} maxLength={300} /><small>Sending does not create consent. The reference must point to consent already collected by your clinic.</small></label>
        {consentError && <p role="alert">{consentError}</p>}
        {canManageConsent && <Button type="button" variant="outline" disabled={consentBusy || Boolean(saving)} onClick={recordExistingConsent}>{consentBusy ? 'Recording reference…' : 'Record existing consent reference'}</Button>}
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}><Button type="button" variant="outline" disabled={Boolean(saving) || consentBusy} onClick={() => setReminder(null)}>Cancel</Button><Button type="submit" disabled={Boolean(saving) || consentBusy}>{saving ? 'Sending…' : 'Send reminder'}</Button></div>
      </form>}
    </Dialog>
  </>;
}
