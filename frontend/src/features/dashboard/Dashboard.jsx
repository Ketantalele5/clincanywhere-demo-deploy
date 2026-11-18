import { useEffect, useState } from 'react';
import {
  Activity,
  ArrowRight,
  Bell,
  CalendarDays,
  CreditCard,
  FlaskConical,
  Package,
  Pill,
  Plus,
  ShieldCheck,
  Users,
} from 'lucide-react';
import { Button, Card, CardHeader, Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../../components/ui/index.js';
import { PersonCell, StatusBadge } from '../../components/shared/RecordDisplay.jsx';
import { todayISO } from '../../lib/formatters.js';
import { apiRequest } from '../../lib/apiClient.js';
import { useLanguage } from '../../lib/i18n.js';

export function Dashboard({ data, clinic, allowedPages, onNavigate, username, role }) {
  const { lang, t } = useLanguage();
  const [accountProfile, setAccountProfile] = useState(null);

  useEffect(() => {
    let active = true;
    apiRequest('/api/account/profile')
      .then(profile => { if (active) setAccountProfile(profile); })
      .catch(() => {});
    return () => { active = false; };
  }, [username]);

  const signedInName = accountProfile?.displayName?.trim() || username || 'Clinic user';
  const firstName = signedInName.split(/\s+/)[0];
  const signedInInitials = signedInName.split(/\s+/).map(part => part[0]).join('').slice(0, 2).toUpperCase();
  const dateLocale = { en: 'en-IN', hi: 'hi-IN', mr: 'mr-IN' }[lang] ?? 'en-IN';
  const formatDashboardDate = (value, options) => {
    const date = value instanceof Date ? value : new Date(`${value}T12:00:00+05:30`);
    return new Intl.DateTimeFormat(dateLocale, { timeZone: 'Asia/Kolkata', ...options }).format(date);
  };
  const patients = data.patients ?? [];
  const appointments = data.appointments ?? [];
  const today = todayISO();
  const todayAppointments = appointments.filter(item => item.date === today);
  const invoices = data.billing ?? [];
  const openInvoices = invoices.filter(item => item.status !== 'Paid');
  const outstanding = openInvoices.reduce((sum, item) => sum + Math.max(Number(item.amount || 0) - Number(item.paid || 0), 0), 0);
  const prescriptionQueue = (data.prescriptions ?? []).filter(item => ['New', 'Processing', 'Ready'].includes(item.pharmacyStatus));
  const canSee = page => allowedPages.includes(page);

  const stats = [
    ...(canSee('appointments') ? [{ label: t('dashboardTodayAppointments'), value: todayAppointments.length, note: t('dashboardTotalScheduled', { count: appointments.length }), icon: CalendarDays, color: 'stat-blue' }] : []),
    ...(canSee('patients') ? [{ label: t('dashboardRegisteredPatients'), value: patients.length, note: t('dashboardThisClinic'), icon: Users, color: 'stat-teal' }] : []),
    ...(canSee('billing') ? [{ label: t('dashboardOutstandingBalance'), value: new Intl.NumberFormat(dateLocale, { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(outstanding), note: t('dashboardOpenInvoices', { count: openInvoices.length }), icon: CreditCard, color: 'stat-purple' }] : []),
    ...(canSee('prescriptions') ? [{ label: t('dashboardPrescriptionQueue'), value: prescriptionQueue.length, note: t('dashboardAwaitingPharmacy'), icon: Pill, color: 'stat-orange' }] : []),
    ...(canSee('doctors') ? [{ label: t('dashboardCareTeam'), value: data.doctors?.length ?? 0, note: t('dashboardStaffProfiles'), icon: Activity, color: 'stat-purple' }] : []),
    ...(canSee('laboratory') ? [{ label: t('dashboardLaboratoryQueue'), value: data.laboratory?.length ?? 0, note: t('dashboardOrdersOnFile'), icon: FlaskConical, color: 'stat-blue' }] : []),
    ...(canSee('pharmacy') ? [{ label: t('dashboardPharmacyItems'), value: data.pharmacy?.length ?? 0, note: t('dashboardCatalogueItems'), icon: Package, color: 'stat-orange' }] : []),
    ...(canSee('inventory') ? [{ label: t('dashboardInventoryItems'), value: data.inventory?.length ?? 0, note: t('dashboardTrackedItems'), icon: Package, color: 'stat-teal' }] : []),
  ].slice(0, 4);

  const upcomingAppointments = appointments
    .filter(item => item.date >= today && !['completed', 'cancelled', 'no-show'].includes(String(item.status ?? '').toLowerCase()))
    .sort((a, b) => `${a.date} ${a.time ?? ''}`.localeCompare(`${b.date} ${b.time ?? ''}`))
    .slice(0, 5);
  const recentPatients = [...patients].slice(-5).reverse();
  const quickLinks = [
    ['appointments', CalendarDays, t('dashboardAppointmentsToday'), todayAppointments.length],
    ['patients', Users, t('dashboardPatientDirectory'), patients.length],
    ['laboratory', FlaskConical, t('dashboardLaboratoryOrders'), data.laboratory?.length],
    ['pharmacy', Pill, t('dashboardPharmacyCatalogue'), data.pharmacy?.length],
    ['billing', CreditCard, t('dashboardOpenInvoicesAction'), openInvoices.length],
    ['inventory', Package, t('dashboardInventoryAction'), data.inventory?.length],
    ['followups', Bell, t('dashboardFollowUps'), data.followups?.length],
    ['recovery', Activity, t('recovery'), data.followups?.filter(item => item.returnStatus !== 'Returned').length],
  ].filter(([id]) => canSee(id));

  return <>
    <section className="dashboard-intro" aria-labelledby="dashboard-title">
      <div className="dashboard-intro-copy">
        <div className="dashboard-date"><CalendarDays size={15} />{formatDashboardDate(new Date(), { weekday: 'long', day: 'numeric', month: 'long' })}</div>
        <h1 id="dashboard-title">{t('dashboardWelcomeBack')}, {firstName}</h1>
        <p>{t('dashboardLatestFrom')} <b>{clinic.name}</b>. {t('dashboardTeamWork')}</p>
        <div className="dashboard-account-info">
          <span className="dashboard-account-avatar">{signedInInitials || role?.initials || 'CA'}</span>
          <span><small>{t('dashboardSignedInAs')}</small><b>{signedInName}</b></span>
          <i />
          <strong>{role?.label || t('dashboardClinicTeam')}</strong>
        </div>
      </div>
      {canSee('appointments') && <Button variant="primary" onClick={() => onNavigate('appointments')}><Plus size={16} /> {t('dashboardBookAppointment')}</Button>}
    </section>

    {!!stats.length && <div className="stats-grid dashboard-stats">{stats.map(({ label, value, note, icon: Icon, color }) => <Card className="stat-card dashboard-stat-card" key={label}>
      <div className="stat-top"><span>{label}</span><i className={color}><Icon size={18} /></i></div>
      <div className="stat-value">{value}</div>
      <div className="stat-foot"><span>{t('dashboardCurrent')}</span><small>{note}</small></div>
    </Card>)}</div>}

    <div className="dashboard-grid lower-grid dashboard-workspace-grid">
      {canSee('patients') && <Card className="dashboard-patients-card">
        <CardHeader>
          <div><h3>{t('dashboardRecentlyRegistered')}</h3><p>{t('dashboardLatestPatientRecords')}</p></div>
          <Button variant="link" onClick={() => onNavigate('patients')}>{t('dashboardViewAll')} <ArrowRight size={14} /></Button>
        </CardHeader>
        <div className="table-scroll"><Table>
          <TableHeader><TableRow><TableHead>{t('dashboardPatient')}</TableHead><TableHead>{t('dashboardPatientId')}</TableHead><TableHead>{t('dashboardCondition')}</TableHead><TableHead>{t('dashboardStatus')}</TableHead></TableRow></TableHeader>
          <TableBody>{recentPatients.map(item => <TableRow key={item.id}>
            <TableCell><PersonCell name={item.name} /></TableCell>
            <TableCell className="muted">{item.id}</TableCell>
            <TableCell>{item.condition || '—'}</TableCell>
            <TableCell><StatusBadge value={item.status} /></TableCell>
          </TableRow>)}</TableBody>
        </Table>{!recentPatients.length && <div className="empty-inline">{t('dashboardNoPatientRecords')} {t('dashboardNewRegistrations')}</div>}</div>
      </Card>}

      <div className="dashboard-side-stack">
        {canSee('appointments') && <Card className="dashboard-agenda-card">
          <CardHeader>
            <div><h3>{t('dashboardUpcomingVisits')}</h3><p>{t('dashboardAppointmentsGlance')}</p></div>
            <Button variant="link" onClick={() => onNavigate('appointments')}>{t('dashboardViewSchedule')} <ArrowRight size={14} /></Button>
          </CardHeader>
          {upcomingAppointments.length ? <div className="dashboard-agenda-list">
            {upcomingAppointments.map(item => <article className="dashboard-agenda-item" key={item.id}>
              <div className="dashboard-agenda-date">{formatDashboardDate(item.date, { weekday: 'short', day: 'numeric', month: 'short' })}</div>
              <div className="dashboard-agenda-details">
                <b>{item.patient || t('dashboardPatientVisit')}</b>
                <span>{item.department || item.doctor || t('dashboardClinicAppointment')}</span>
              </div>
              <div className="dashboard-agenda-meta">
                <span className="dashboard-agenda-time">{item.time ? `${item.time} IST` : t('dashboardTimePending')}</span>
                <StatusBadge value={item.status || t('dashboardScheduled')} />
              </div>
            </article>)}
          </div> : <div className="dashboard-agenda-empty"><span><CalendarDays size={19} /></span><div><b>{t('dashboardNoUpcomingVisits')}</b><p>{t('dashboardVisitsAppear')}</p></div></div>}
        </Card>}

        <Card className="operations-card dashboard-operations-card">
          <CardHeader><div><h3>{t('dashboardQuickAccess')}</h3><p>{t('dashboardTeamWorkflow')}</p></div></CardHeader>
          {quickLinks.length ? <div className="quick-links">{quickLinks.map(([id, Icon, label, count]) => <button type="button" className="quick-link" key={id} onClick={() => onNavigate(id)}>
            <span className="quick-icon"><Icon size={16} /></span><span>{label}</span><b>{count ?? '—'}</b>
          </button>)}</div> : <div className="empty-inline">{t('dashboardShortcutsAppear')}</div>}
        </Card>
      </div>
    </div>

    <div className="prototype-notice"><ShieldCheck size={17} /><div><b>{t('dashboardClinicWorkspace')}</b><span>{t('dashboardPrivacyWorkflows')}</span></div></div>
  </>;
}
