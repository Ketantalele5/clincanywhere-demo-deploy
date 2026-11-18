import { useCallback, useEffect, useState } from 'react';
import { Building2, Check, CirclePlus, Clock3, UserPlus, Users, Workflow } from 'lucide-react';
import { Alert, Badge, Button, Card, CardHeader, Input, Label, Spinner } from '../../components/ui/index.js';
import { apiRequest } from '../../lib/apiClient.js';

const statuses = ['NEW', 'CONTACTED', 'QUALIFIED', 'LOST'];
const emptyLead = { clinicName: '', contactName: '', email: '', phone: '', city: '', clinicType: '', message: '' };
const statusText = { NEW: 'New', CONTACTED: 'Contacted', QUALIFIED: 'Qualified', CONVERTED: 'Onboarded', LOST: 'Closed' };

export function PlatformOnboardingPage({ roleId, username, onNotify, onWorkspaceCreated }) {
  const owner = roleId === 'platform-owner';
  const [overview, setOverview] = useState(null);
  const [leads, setLeads] = useState([]);
  const [team, setTeam] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [showLeadForm, setShowLeadForm] = useState(false);

  const refresh = useCallback(async () => {
    setError('');
    try {
      const [summary, leadRows, teamRows] = await Promise.all([
        apiRequest('/api/platform/overview'), apiRequest('/api/platform/leads'),
        owner ? apiRequest('/api/platform/team') : Promise.resolve([]),
      ]);
      setOverview(summary); setLeads(leadRows); setTeam(teamRows);
    } catch (problem) { setError(problem.message || 'Could not load the platform pipeline.'); }
    finally { setLoading(false); }
  }, [owner]);

  useEffect(() => { refresh(); }, [refresh]);

  async function addLead(event) {
    event.preventDefault(); setBusy(true); setError('');
    const form = event.currentTarget;
    const values = Object.fromEntries(new FormData(form).entries());
    try {
      await apiRequest('/api/platform/leads', { method: 'POST', body: JSON.stringify(values) });
      form.reset(); setShowLeadForm(false); onNotify?.('Clinic prospect added to your pipeline'); await refresh();
    } catch (problem) { setError(problem.message); }
    finally { setBusy(false); }
  }

  async function updateLead(id, endpoint, body, successMessage) {
    setError('');
    try { await apiRequest(`/api/platform/leads/${id}/${endpoint}`, { method: endpoint === 'status' ? 'PATCH' : 'PUT', body: JSON.stringify(body) }); onNotify?.(successMessage); await refresh(); }
    catch (problem) { setError(problem.message); }
  }

  const metrics = owner ? [
    { label: 'Onboarded clinics', value: overview?.activeClinics ?? 0, icon: Building2, tone: 'mint' },
    { label: 'Open pipeline', value: overview?.openLeads ?? 0, icon: Workflow, tone: 'blue' },
    { label: 'Qualified clinics', value: overview?.qualifiedLeads ?? 0, icon: Check, tone: 'lilac' },
    { label: 'Onboarding team', value: overview?.onboardingTeamMembers ?? 0, icon: Users, tone: 'sand' },
  ] : [
    { label: 'My prospects', value: overview?.leads ?? 0, icon: Building2, tone: 'mint' },
    { label: 'Still in progress', value: overview?.openLeads ?? 0, icon: Workflow, tone: 'blue' },
    { label: 'Qualified', value: overview?.qualifiedLeads ?? 0, icon: Check, tone: 'lilac' },
    { label: 'Clinics onboarded', value: overview?.activeClinics ?? 0, icon: Users, tone: 'sand' },
  ];

  return <section className="platform-page">
    <div className="platform-page-heading"><div><span className="platform-eyebrow">CLINCYANYWHERE · PLATFORM</span><h1>{owner ? 'Clinic growth overview' : `Welcome, ${username}`}</h1><p>{owner ? 'Track clinic onboarding, your outreach pipeline and the people growing the platform.' : 'Add the clinics you discover and keep every follow-up in one place.'}</p></div><Button variant="primary" onClick={() => setShowLeadForm(value => !value)}><CirclePlus size={16}/>{showLeadForm ? 'Close lead form' : 'Add a clinic lead'}</Button></div>
    {overview && <div className="platform-metrics">{metrics.map(({ label, value, icon: Icon, tone }) => <Card className="platform-metric" key={label}><span className={`platform-metric-icon ${tone}`}><Icon size={17}/></span><small>{label}</small><b>{value}</b></Card>)}</div>}
    {error && <Alert variant="destructive">{error}</Alert>}
    {showLeadForm && <Card className="platform-lead-form-card"><CardHeader><div><h3>Record a clinic prospect</h3><p>{owner ? 'Capture an inbound enquiry or assign a clinic to a teammate.' : 'This lead will be assigned to your account so you can track it.'}</p></div></CardHeader><form className="platform-lead-form" onSubmit={addLead}><Label>Clinic name<Input name="clinicName" required maxLength="120" placeholder="e.g. Greenleaf Family Clinic"/></Label><Label>Contact person<Input name="contactName" required maxLength="120" placeholder="Clinic owner or manager"/></Label><Label>Work email<Input name="email" type="email" required maxLength="160" placeholder="name@clinic.com"/></Label><Label>Phone<Input name="phone" type="tel" maxLength="32" placeholder="+91"/></Label><Label>City<Input name="city" maxLength="80" placeholder="City"/></Label><Label>Clinic type<Input name="clinicType" maxLength="60" placeholder="Family clinic, diagnostics…"/></Label><Label className="platform-lead-message">Notes<textarea name="message" maxLength="1000" rows="3" placeholder="Needs, next follow-up, or conversation notes"/></Label><Button variant="primary" type="submit" disabled={busy}>{busy ? <Spinner/> : <><CirclePlus size={15}/>Save clinic lead</>}</Button></form></Card>}
    <Card className="platform-pipeline"><CardHeader><div><h3>Clinic pipeline</h3><p>{owner ? 'Website enquiries and prospects sourced by your team.' : 'Your assigned clinics and the next step for each one.'}</p></div><Badge>{leads.length} {leads.length === 1 ? 'clinic' : 'clinics'}</Badge></CardHeader>
      {loading ? <div className="platform-loading"><Spinner/> Loading your pipeline…</div> : leads.length ? <div className="platform-lead-list">{leads.map(lead => <LeadCard key={lead.id} lead={lead} owner={owner} team={team} onUpdate={updateLead} onRefresh={refresh} onWorkspaceCreated={onWorkspaceCreated} onNotify={onNotify}/>)}</div> : <div className="platform-empty"><span><Building2 size={21}/></span><b>No clinic leads yet</b><small>Add a clinic you found or wait for an enquiry from the website.</small><Button variant="outline" onClick={() => setShowLeadForm(true)}><CirclePlus size={15}/>Add your first lead</Button></div>}
    </Card>
  </section>;
}

function LeadCard({ lead, owner, team, onUpdate, onRefresh, onWorkspaceCreated, onNotify }) {
  const [converting, setConverting] = useState(false);
  const [busy, setBusy] = useState(false);
  const initialClinicId = lead.clinicName.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '').slice(0, 36) || 'new-clinic';
  const contactEmail = typeof lead.email === 'string' ? lead.email : '';
  const initialUsername = contactEmail.split('@')[0].replace(/[^A-Za-z0-9._-]/g, '').slice(0, 70) || 'clinicadmin';

  async function convert(event) {
    event.preventDefault(); setBusy(true);
    const form = event.currentTarget;
    const values = Object.fromEntries(new FormData(form).entries());
    try { await apiRequest(`/api/platform/leads/${lead.id}/convert`, { method: 'POST', body: JSON.stringify(values) }); setConverting(false); onNotify?.('Clinic workspace created and its admin account is ready'); await onRefresh(); await onWorkspaceCreated?.(); }
    catch (problem) { onNotify?.(problem.message); }
    finally { setBusy(false); }
  }

  return <article className="platform-lead">
    <div className="platform-lead-main"><span className="platform-lead-avatar">{lead.clinicName.slice(0, 1).toUpperCase()}</span><div className="platform-lead-title"><div><h4>{lead.clinicName}</h4><Badge variant={lead.status === 'CONVERTED' ? 'green' : lead.status === 'LOST' ? 'gray' : 'blue'}>{statusText[lead.status] || lead.status}</Badge></div><p>{lead.contactName} · <a href={`mailto:${lead.email}`}>{lead.email}</a>{lead.city ? ` · ${lead.city}` : ''}</p><small>{lead.source === 'WEBSITE' ? 'Website enquiry' : `Added by ${lead.createdBy || 'team'}`}{lead.assignedTo ? ` · Owner: ${lead.assignedTo}` : ''}</small></div>
      <div className="platform-lead-actions">
        {lead.status !== 'CONVERTED' && <select aria-label={`Update ${lead.clinicName} lead status`} value={lead.status} onChange={event => onUpdate(lead.id, 'status', { status: event.target.value }, 'Lead status updated')}>{statuses.map(status => <option value={status} key={status}>{statusText[status]}</option>)}</select>}
        {owner && lead.status !== 'CONVERTED' && team.length > 0 && <select aria-label={`Assign ${lead.clinicName} lead`} value={lead.assignedTo || ''} onChange={event => onUpdate(lead.id, 'assignment', { assignedTo: event.target.value }, 'Lead assigned to onboarding teammate')}><option value="" disabled>Assign to teammate</option>{team.map(member => <option value={member.username} key={member.username}>{member.displayName}</option>)}</select>}
        {lead.status === 'CONVERTED' ? <span className="platform-workspace-id"><Check size={13}/>{lead.workspaceId}</span> : lead.status !== 'LOST' && <Button variant="outline" onClick={() => setConverting(value => !value)}><Building2 size={14}/>{converting ? 'Cancel setup' : 'Set up clinic'}</Button>}
      </div>
    </div>
    {converting && <form className="platform-convert-form" onSubmit={convert}><div><b>Set up {lead.clinicName}</b><small>This creates its clinic workspace and first clinic admin.</small></div><Label>Workspace ID<Input name="clinicId" pattern="[A-Za-z0-9-]{2,40}" required maxLength="40" defaultValue={initialClinicId}/></Label><Label>Admin username<Input name="adminUsername" pattern="[A-Za-z0-9._-]+" minLength="3" maxLength="80" required defaultValue={initialUsername}/></Label><Label>Temporary password<Input name="adminPassword" type="password" minLength="8" maxLength="72" required autoComplete="new-password"/></Label><Button variant="primary" type="submit" disabled={busy}>{busy ? <Spinner/> : <><Check size={14}/>Create workspace</>}</Button></form>}
    {lead.message && <p className="platform-lead-notes"><Clock3 size={13}/>{lead.message}</p>}
  </article>;
}

export function PlatformTeamPage({ onNotify }) {
  const [team, setTeam] = useState([]); const [error, setError] = useState(''); const [busy, setBusy] = useState(false);
  const refresh = useCallback(async () => { try { setTeam(await apiRequest('/api/platform/team')); setError(''); } catch (problem) { setError(problem.message); } }, []);
  useEffect(() => { refresh(); }, [refresh]);
  async function createMember(event) { event.preventDefault(); setBusy(true); setError(''); const form = event.currentTarget; const values = Object.fromEntries(new FormData(form).entries()); try { await apiRequest('/api/platform/team', { method: 'POST', body: JSON.stringify(values) }); form.reset(); await refresh(); onNotify?.('Onboarding teammate account created'); } catch (problem) { setError(problem.message); } finally { setBusy(false); } }
  return <section className="platform-page"><div className="platform-page-heading"><div><span className="platform-eyebrow">PEOPLE · PLATFORM</span><h1>Onboarding team</h1><p>Create accounts for the people finding clinics and following up with them.</p></div></div>
    <Card className="platform-team-card"><CardHeader><div><h3>Add a teammate</h3><p>This account can work on clinic leads but cannot access any clinic's patient or staff data.</p></div></CardHeader><form className="platform-lead-form" onSubmit={createMember}><Label>Full name<Input name="displayName" required maxLength="120"/></Label><Label>Username<Input name="username" required minLength="3" maxLength="80" pattern="[A-Za-z0-9._-]+" placeholder="e.g. anita.shah"/></Label><Label>Work email<Input name="email" type="email" maxLength="160"/></Label><Label>Phone<Input name="phone" type="tel" maxLength="32"/></Label><Label>Temporary password<Input name="password" type="password" required minLength="8" maxLength="72" autoComplete="new-password"/></Label><div className="platform-team-submit"><Button variant="primary" type="submit" disabled={busy}>{busy ? <Spinner/> : <><UserPlus size={15}/>Create onboarding login</>}</Button><small>Share the temporary password with the teammate securely.</small></div>{error && <Alert variant="destructive">{error}</Alert>}</form></Card>
    <Card className="platform-team-list-card"><CardHeader><div><h3>Your onboarding team</h3><p>{team.length} team {team.length === 1 ? 'member' : 'members'} with platform lead access.</p></div></CardHeader><div className="platform-team-list">{team.length ? team.map(member => <div className="platform-team-member" key={member.username}><span>{member.displayName.split(/\s+/).map(part => part[0]).join('').slice(0, 2).toUpperCase()}</span><div><b>{member.displayName}</b><small>{member.username}{member.email ? ` · ${member.email}` : ''}</small><small>{member.leadsInPipeline} leads · {member.clinicsOnboarded} clinics onboarded</small></div><Badge>Onboarding</Badge></div>) : <div className="platform-empty-small">No onboarding teammates yet. Add the first person above.</div>}</div></Card>
  </section>;
}
