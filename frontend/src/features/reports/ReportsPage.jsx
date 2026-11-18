import { Activity, CalendarDays, CreditCard, FileText, Users } from 'lucide-react';
import { Button, Card, CardHeader } from '../../components/ui/index.js';
import { schemas, titles } from '../../lib/appConfig.js';
import { useLanguage } from '../../lib/i18n.js';
import { Metric } from '../../components/shared/RecordDisplay.jsx';

export function ReportsPage({ data, onExport }) {
  const { lang } = useLanguage();
  const hasPatients = Array.isArray(data.patients);
  const hasAppointments = Array.isArray(data.appointments);
  const hasBilling = Array.isArray(data.billing);
  const hasPharmacy = Array.isArray(data.pharmacy);
  const patients = data.patients ?? [];
  const bills = data.billing ?? [];
  const paid = bills.reduce((sum, item) => sum + Number(item.paid || 0), 0);
  const outstanding = bills.reduce((sum, item) => sum + Math.max(Number(item.amount || 0) - Number(item.paid || 0), 0), 0);
  const appointments = data.appointments ?? [];
  const missed = appointments.filter(item => ['No-show', 'Missed', 'Cancelled'].includes(item.status));
  const unpaid = bills.filter(item => Number(item.amount || 0) > Number(item.paid || 0));
  const underpaid = bills.filter(item => item.status === 'Paid' && Number(item.paid || 0) < Number(item.amount || 0));
  const returnRate = appointments.length
    ? Math.round((appointments.filter(item => ['Completed', 'Checked in'].includes(item.status)).length / appointments.length) * 100)
    : 0;
  const lowStock = (data.pharmacy ?? []).filter(item => Number(item.stock || 0) <= Number(item.reorder || 0));
  const availableModules = Object.keys(data).filter(category => schemas[category]);

  return <section className="reports-page">
    <div className="page-heading">
      <div>
        <h1>{lang === 'mr' ? 'अहवाल आणि क्लिनिक विश्लेषण' : lang === 'hi' ? 'रिपोर्ट और क्लिनिक विश्लेषण' : 'Reports & Clinic Analytics'}</h1>
        <p>Collection, outstanding balances and visit activity from this clinic’s records.</p>
      </div>
      <Button variant="outline" onClick={() => onExport(data, 'clincanywhere-report.json', 'json')}>
        <FileText size={15} /> Export report
      </Button>
    </div>

    <div className="stats-grid report-stats">
      <Metric icon={Users} label="Total patients" value={hasPatients ? patients.length : '—'} />
      <Metric icon={CreditCard} label="Payments received" value={hasBilling ? `₹${paid.toLocaleString('en-IN')}` : '—'} />
      <Metric icon={Activity} label="Outstanding" value={hasBilling ? `₹${outstanding.toLocaleString('en-IN')}` : '—'} />
      <Metric icon={CalendarDays} label="Appointments" value={hasAppointments ? appointments.length : '—'} />
    </div>

    <Card className="report-insights">
      <div>
        <h2>Clinic operations at a glance</h2>
        <p>Review visit outcomes, outstanding invoices and inventory that may need attention.</p>
      </div>
      <div className="report-checks">
        <div className="report-check"><b>{hasAppointments ? missed.length : '—'}</b><span>Missed or cancelled visits</span></div>
        <div className="report-check"><b>{hasBilling ? unpaid.length : '—'}</b><span>Invoices with a balance</span></div>
        <div className="report-check"><b>{hasBilling ? underpaid.length : '—'}</b><span>Invoices marked paid below total</span></div>
        <div className="report-check"><b>{hasAppointments ? `${returnRate}%` : '—'}</b><span>Visit completion rate</span></div>
      </div>
      {hasBilling && <p>Uncollected invoice balance <b>₹{outstanding.toLocaleString('en-IN')}</b> · not net profit.</p>}
      {lowStock.length > 0 && <div>
        <h3>Pharmacy items at or below reorder level</h3>
        <div className="report-low-stock">{lowStock.map(item => <span key={item.id}>{item.name}: {item.stock} on hand · reorder at {item.reorder}</span>)}</div>
      </div>}
      {hasPharmacy && lowStock.length === 0 && <p>No pharmacy items are currently at or below their reorder level.</p>}
      {!hasPharmacy && <p>Pharmacy stock data is not available for your role.</p>}
    </Card>

    <Card className="report-modules">
      <CardHeader><div><h3>Loaded module records</h3><p>Counts reflect the data currently available to your account.</p></div></CardHeader>
      {availableModules.length
        ? <div className="module-counts">{availableModules.map(category => <div key={category}><span>{titles[category]}</span><b>{data[category]?.length ?? 0}</b></div>)}</div>
        : <div className="empty-inline">No module data is available for this account.</div>}
    </Card>
  </section>;
}
