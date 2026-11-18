import { useEffect, useState } from 'react';
import { CheckCircle2, LifeBuoy, MessageSquare, RefreshCw, Send } from 'lucide-react';
import { Alert, Badge, Button, Card, CardHeader, Input, Label, Select, Spinner, Textarea } from '../../components/ui/index.js';
import { apiRequest } from '../../lib/apiClient.js';
import { formatDateTimeIST } from '../../lib/formatters.js';

export function SupportPage({ clinicId, roleId, data, onNotify }) {
  const [items, setItems] = useState([]);
  const [busy, setBusy] = useState(false);
  const [busyTicket, setBusyTicket] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const staff = roleId !== 'patient';

  async function loadTickets() {
    setLoading(true);
    setError('');
    try {
      setItems(await apiRequest(`/api/${clinicId}/support`));
    } catch (problem) {
      setError(problem.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    apiRequest(`/api/${clinicId}/support`)
      .then(rows => { if (active) setItems(rows); })
      .catch(problem => { if (active) setError(problem.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [clinicId]);

  async function submit(event) {
    event.preventDefault();
    const form = event.currentTarget;
    setBusy(true);
    setError('');
    const values = Object.fromEntries(new FormData(form).entries());
    try {
      await apiRequest(`/api/${clinicId}/support`, {
        method: 'POST',
        body: JSON.stringify(values),
      });
      form.reset();
      await loadTickets();
      onNotify?.('Support request submitted');
    } catch (problem) {
      setError(problem.message);
    } finally {
      setBusy(false);
    }
  }

  async function update(ticket, status) {
    setBusyTicket(ticket.id);
    setError('');
    try {
      await apiRequest(`/api/${clinicId}/support/${ticket.id}`, {
        method: 'PUT',
        body: JSON.stringify({ status, assignedTo: ticket.assignedTo ?? '' }),
      });
      await loadTickets();
      onNotify?.(status === 'RESOLVED' ? 'Support request resolved' : 'Support request updated');
    } catch (problem) {
      setError(problem.message);
    } finally {
      setBusyTicket(null);
    }
  }

  return <section className="support-page">
    <div>
      <div className="page-heading">
        <div><h1>Patient support</h1><p>Questions, service issues and feedback for this clinic.</p></div>
      </div>
      <Card className="support-form-card">
        <CardHeader><div><h3><LifeBuoy size={17} /> New support request</h3><p>Share a question or concern with your clinic team.</p></div></CardHeader>
        <form className="support-form" onSubmit={submit}>
          {staff && <Label htmlFor="support-patient">Patient
            <Select id="support-patient" name="patientId" required>
              <option value="">Select a patient</option>
              {(data.patients ?? []).map(patient => <option key={patient.id} value={patient.id}>{patient.name} · {patient.id}</option>)}
            </Select>
          </Label>}
          <Label htmlFor="support-subject">Subject
            <Input id="support-subject" name="subject" required maxLength={160} placeholder="What do you need help with?" />
          </Label>
          <Label htmlFor="support-message">Details
            <Textarea id="support-message" name="message" required maxLength={2000} rows={5} placeholder="Describe your question or feedback." />
          </Label>
          {error && <Alert variant="destructive" role="alert">{error}</Alert>}
          <Button variant="primary" type="submit" disabled={busy}>
            {busy ? <Spinner /> : <Send size={15} />}
            {busy ? 'Sending request…' : 'Submit request'}
          </Button>
        </form>
      </Card>
    </div>

    <Card className="support-list-card">
      <CardHeader>
        <div><h3><MessageSquare size={17} /> Requests</h3><p>{items.length} {items.length === 1 ? 'request' : 'requests'} in this clinic.</p></div>
        <Button variant="outline" aria-label="Refresh support requests" onClick={loadTickets} disabled={loading}>
          {loading ? <Spinner /> : <RefreshCw size={15} />} Refresh
        </Button>
      </CardHeader>
      {loading ? <div className="loading-line"><Spinner /> Loading support requests…</div>
        : error ? <div className="support-list"><Alert variant="destructive" role="alert">{error}</Alert><Button variant="outline" onClick={loadTickets}>Try again</Button></div>
          : items.length ? <div className="support-list">{items.map(ticket => <article className="support-ticket" key={ticket.id}>
            <div className="support-ticket-header"><div><h3>{ticket.subject}</h3><div className="support-ticket-meta"><span>{ticket.patientId ? `Patient ${ticket.patientId}` : 'Patient request'}</span><span>{formatDateTimeIST(ticket.createdAt)}</span></div></div>
              <Badge variant={ticket.status === 'RESOLVED' ? 'green' : ticket.status === 'IN_PROGRESS' ? 'blue' : 'gray'}>{ticket.status.replaceAll('_', ' ')}</Badge>
            </div>
            <p>{ticket.message}</p>
            {ticket.assignedTo && <div className="support-ticket-meta">Assigned to {ticket.assignedTo}</div>}
            {staff && ticket.status !== 'RESOLVED' && <div className="support-ticket-actions">
              {ticket.status !== 'IN_PROGRESS' && <Button variant="outline" disabled={busyTicket === ticket.id} onClick={() => update(ticket, 'IN_PROGRESS')}>{busyTicket === ticket.id ? <Spinner /> : null}Mark in progress</Button>}
              <Button variant="secondary" disabled={busyTicket === ticket.id} onClick={() => update(ticket, 'RESOLVED')}>
                {busyTicket === ticket.id ? <Spinner /> : <CheckCircle2 size={15} />} Resolve
              </Button>
            </div>}
          </article>)}</div>
            : <div className="empty-inline"><div><b>No support requests yet</b><p>New questions and feedback for this clinic will appear here.</p></div></div>}
    </Card>
  </section>;
}
