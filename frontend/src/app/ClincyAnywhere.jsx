import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  BriefcaseMedical, Search, Bell, ShieldCheck, HeartPulse, ChevronDown, Menu, LogOut, UserRound, Settings2,
  
  
  MessageCircle, Send, RefreshCw, PanelLeftClose, PanelLeftOpen,
} from 'lucide-react';
import { apiRequest, clearAccessToken, getAccessToken, setAccessToken, setAuthenticatedUsername } from '../lib/apiClient.js';
import { downloadRows } from '../lib/downloadFile.js';
import { clinics as demoClinics, dataCategoriesByRole, demoModeEnabled, navGroups, roleIdsByAuthority, roles, schemas, titles, useLocalizedTitles } from '../lib/appConfig.js';
import { useLanguage } from '../lib/i18n.js';
import LoginPage from '../features/auth/LoginPage.jsx';
import { Dashboard } from '../features/dashboard/Dashboard.jsx';
import { RecordDialog, RecordsPage } from '../features/records/RecordsPage.jsx';
import { BillingPage } from '../features/billing/BillingPage.jsx';
import { MessagesPage } from '../features/messaging/MessagesPage.jsx';
import { ReportsPage } from '../features/reports/ReportsPage.jsx';
import { ProfilePage, SettingsPage } from '../features/settings/SettingsPage.jsx';
import { PlatformOnboardingPage, PlatformTeamPage } from '../features/platform/PlatformOnboardingPage.jsx';
import { useMessages, useNotifications } from '../hooks/useRealtime.js';
import MarketingSite from '../features/marketing/MarketingSite.jsx';
import { RecoveryPage } from '../features/dashboard/RecoveryPage.jsx';
import { VoiceAssistantPage } from '../features/dashboard/VoiceAssistantPage.jsx';
import { PharmacyPage } from '../features/dashboard/PharmacyPage.jsx';
import { OfflineBanner } from '../components/OfflineBanner.jsx';
import { DigitalHealthPage } from '../features/settings/DigitalHealthPage.jsx';
import { SupportPage } from '../features/messaging/SupportPage.jsx';
import { ParityPage } from '../features/settings/ParityPage.jsx';
import { useToast } from '../components/ui/ToastProvider.jsx';
import { Alert, Button, Input, ScrollArea, Select, Sidebar, SidebarProvider, SidebarTrigger, Spinner } from '../components/ui/index.js';

const demoCredentials = Object.fromEntries(Object.entries(roles).map(([id, role]) => [id, { username: role.username, password: 'demo123' }]));

export default function ClincyAnywhere() {
  const { lang, setLanguage, t } = useLanguage();
  const localizedTitles = useLocalizedTitles();
  const { toast } = useToast();
  const [clinicId, setClinicId] = useState(demoClinics[0].id);
  const [clinicList, setClinicList] = useState([]);
  const [roleId, setRoleId] = useState('');
  const [username, setUsername] = useState('');
  const [page, setPage] = useState('dashboard');
  const [accountMenuOpen, setAccountMenuOpen] = useState(false);
  const accountMenuRef = useRef(null);
  const [data, setData] = useState({});
  const [searchResults, setSearchResults] = useState({});
  const [loading, setLoading] = useState(false);
  const [clinicsReady, setClinicsReady] = useState(false);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [modal, setModal] = useState(null);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(() => {
    try { return localStorage.getItem('clinic-sidebar-collapsed') === 'true'; }
    catch { return false; }
  });
  const [notifications, setNotifications] = useState([]);
  const [notificationsEnabled, setNotificationsEnabled] = useState(() => {
    try { return localStorage.getItem('clinic-notifications-enabled') !== 'false'; }
    catch { return true; }
  });
  const [messageContacts, setMessageContacts] = useState([]);
  const [peer, setPeer] = useState('');
  const [messages, setMessages] = useState([]);
  const [draft, setDraft] = useState('');
  const [loginError, setLoginError] = useState('');
  const [registering, setRegistering] = useState(false);
  const [loginUsername, setLoginUsername] = useState(demoModeEnabled ? roles.admin.username : '');
  const [loginPassword, setLoginPassword] = useState(demoModeEnabled ? 'demo123' : '');
  const [invitationToken, setInvitationToken] = useState('');
  const [registrationProfile, setRegistrationProfile] = useState({ displayName: '', email: '', phone: '' });
  const [busy, setBusy] = useState(false);
  const [dataRevision, setDataRevision] = useState(0);
  const dataRevisionRef = useRef(0);
  const [loadedCategories, setLoadedCategories] = useState([]);
  const [recordPages, setRecordPages] = useState({});
  const searchSequence = useRef({});
  const [showSignIn, setShowSignIn] = useState(false);
  const [loginPortal, setLoginPortal] = useState('hospital');

  useEffect(() => {
    try { localStorage.setItem('clinic-sidebar-collapsed', String(sidebarCollapsed)); }
    catch { /* Sidebar preference remains usable if browser storage is unavailable. */ }
  }, [sidebarCollapsed]);

  useEffect(() => {
    if (!mobileOpen) return undefined;
    const previousOverflow = document.body.style.overflow;
    const closeOnEscape = event => { if (event.key === 'Escape') setMobileOpen(false); };
    const closeOnDesktop = () => { if (window.matchMedia('(min-width: 841px)').matches) setMobileOpen(false); };
    if (window.matchMedia('(max-width: 840px)').matches) document.body.style.overflow = 'hidden';
    document.addEventListener('keydown', closeOnEscape);
    window.addEventListener('resize', closeOnDesktop);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener('keydown', closeOnEscape);
      window.removeEventListener('resize', closeOnDesktop);
    };
  }, [mobileOpen]);

  useEffect(() => {
    if (!accountMenuOpen) return undefined;
    const closeOnOutsideClick = event => { if (!accountMenuRef.current?.contains(event.target)) setAccountMenuOpen(false); };
    const closeOnEscape = event => { if (event.key === 'Escape') setAccountMenuOpen(false); };
    document.addEventListener('pointerdown', closeOnOutsideClick);
    document.addEventListener('keydown', closeOnEscape);
    return () => { document.removeEventListener('pointerdown', closeOnOutsideClick); document.removeEventListener('keydown', closeOnEscape); };
  }, [accountMenuOpen]);

  const role = roles[roleId];
  const isPlatformOwner = roleId === 'platform-owner';
  const clinic = clinicList.find(item => item.id === clinicId) ?? clinicList[0] ?? demoClinics[0];
  const allowedPages = useMemo(() => {
    const workspacePages = [...Object.keys(titles).filter(pageId => !['onboarding', 'onboarding-team'].includes(pageId)), 'messages'];
    if (isPlatformOwner && clinicList.length) return [...workspacePages, 'onboarding', 'onboarding-team'];
    if (isPlatformOwner) return ['onboarding', 'onboarding-team'];
    if (role?.pages === 'all') return workspacePages;
    return role?.platform ? [...(role.pages ?? [])] : [...(role?.pages ?? []), 'messages', ...(roleId === 'admin' ? ['parity'] : [])];
  }, [role, roleId, isPlatformOwner, clinicList.length]);
  function navigateTo(nextPage) {
    if (!allowedPages.includes(nextPage) && !(nextPage === 'profile' && roleId)) return;
    setPage(nextPage);
    setSearch('');
    setAccountMenuOpen(false);
  }
  function openAccountPage(nextPage) { navigateTo(nextPage); }
  const loadData = useCallback(async () => {
    if (!roleId) return;
    if (roles[roleId]?.platform && roleId !== 'platform-owner') { setData({}); setError(''); setLoading(false); return; }
    const requestedClinic = clinicId;
    const requestedRole = roleId;
    const revision = dataRevision;
    const current = () => requestedClinic === clinicId && requestedRole === roleId && revision === dataRevision && getAccessToken();
    if (!clinicsReady) {
      setLoading(true);
      setError('');
      return;
    }
    if (!clinicList.some(item => item.id === requestedClinic)) {
      setData({}); setSearchResults({}); setLoadedCategories([]); setRecordPages({});
      setLoading(false); setError('');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const categories = dataCategoriesByRole[requestedRole] ?? [];
      const priority = ['patients', 'appointments', 'billing', 'prescriptions', 'laboratory', 'pharmacy', 'inventory', 'doctors', 'settings'];
      const initialCategories = [...priority.filter(category => categories.includes(category)), ...categories.filter(category => !priority.includes(category))].slice(0, 4);
      const entries = await Promise.all(initialCategories
        .map(async category => [category, await apiRequest(`/api/${requestedClinic}/${category}?page=0&size=${category === 'patients' ? 50 : 100}`)]));
      if (current() && getAccessToken()) {
        setData(Object.fromEntries(entries));
        setSearchResults({});
        setLoadedCategories(initialCategories);
        setRecordPages(Object.fromEntries(entries.map(([category, rows]) => [category, { page: 0, hasMore: rows.length === (category === 'patients' ? 50 : 100) }])));
      }
    } catch (loadError) {
      if (current() && getAccessToken()) setError(loadError.message);
    } finally {
      if (current()) setLoading(false);
    }
  }, [clinicId, clinicList, clinicsReady, roleId, dataRevision]);

  const ensureCategoryLoaded = useCallback(async category => {
    if (!roleId || !dataCategoriesByRole[roleId]?.includes(category) || loadedCategories.includes(category)) return;
    const requestedClinic = clinicId;
    const requestedRole = roleId;
    const requestedRevision = dataRevisionRef.current;
    const rows = await apiRequest(`/api/${requestedClinic}/${category}?page=0&size=${category === 'patients' ? 50 : 100}`);
    if (requestedClinic === clinicId && requestedRole === roleId && requestedRevision === dataRevisionRef.current && getAccessToken()) {
      setData(current => ({ ...current, [category]: rows }));
      setLoadedCategories(current => current.includes(category) ? current : [...current, category]);
      setRecordPages(current => ({ ...current, [category]: { page: 0, hasMore: rows.length === (category === 'patients' ? 50 : 100) } }));
    }
  }, [clinicId, loadedCategories, roleId]);

  const loadMoreRecords = useCallback(async category => {
    const requestedClinic = clinicId;
    const requestedRevision = dataRevisionRef.current;
    const requestedRole = roleId;
    const state = recordPages[category] ?? { page: 0, hasMore: false };
    if (!state.hasMore) return;
    const size = category === 'patients' ? 50 : 100;
    const rows = await apiRequest(`/api/${requestedClinic}/${category}?page=${state.page + 1}&size=${size}`);
    if (requestedClinic !== clinicId || requestedRevision !== dataRevisionRef.current || requestedRole !== roleId || !getAccessToken()) return;
    setData(current => ({ ...current, [category]: [...(current[category] ?? []), ...rows] }));
    setRecordPages(current => ({ ...current, [category]: { page: state.page + 1, hasMore: rows.length === size } }));
  }, [clinicId, recordPages]);

  const searchRecords = useCallback(async (category, query) => {
    const requestedClinic = clinicId;
    const size = category === 'patients' ? 50 : 100;
    const requestedRevision = dataRevisionRef.current;
    const sequence = (searchSequence.current[category] ?? 0) + 1;
    searchSequence.current[category] = sequence;
    const rows = await apiRequest(`/api/${requestedClinic}/${category}?q=${encodeURIComponent(query)}&size=${size}`);
    if (sequence !== searchSequence.current[category] || requestedClinic !== clinicId || requestedRevision !== dataRevisionRef.current || !getAccessToken()) return;
    setSearchResults(current => ({ ...current, [category]: rows }));
    setRecordPages(current => ({ ...current, [category]: { page: 0, hasMore: !query && rows.length === size } }));
  }, [clinicId]);

  const refreshCategory = useCallback(async category => {
    const requestedClinic = clinicId;
    const requestedRole = roleId;
    const requestedRevision = dataRevisionRef.current;
    const size = category === 'patients' ? 50 : 100;
    const rows = await apiRequest(`/api/${requestedClinic}/${category}?page=0&size=${size}`);
    if (requestedClinic !== clinicId || requestedRole !== roleId || requestedRevision !== dataRevisionRef.current || !getAccessToken()) return;
    setData(current => ({ ...current, [category]: rows }));
    setSearchResults(current => ({ ...current, [category]: null }));
    setLoadedCategories(current => current.includes(category) ? current : [...current, category]);
    setRecordPages(current => ({ ...current, [category]: { page: 0, hasMore: rows.length === size } }));
  }, [clinicId, roleId]);

  const exportCategory = useCallback(async (category, filename) => {
    const requestedClinic = clinicId;
    const requestedRole = roleId;
    const requestedRevision = dataRevisionRef.current;
    const pageSize = 200;
    const allRows = [];
    for (let pageIndex = 0; ; pageIndex += 1) {
      const batch = await apiRequest(`/api/${requestedClinic}/${category}?page=${pageIndex}&size=${pageSize}`);
      if (requestedClinic !== clinicId || requestedRole !== roleId || requestedRevision !== dataRevisionRef.current || !getAccessToken()) return;
      allRows.push(...batch);
      if (batch.length < pageSize) break;
    }
    downloadRows(allRows, filename);
  }, [clinicId, roleId]);

  const notify = useCallback(message => toast({ description: message }), [toast]);
  const addNotification = useCallback(message => notify(message), [notify]);
  const changeNotificationsEnabled = useCallback(enabled => {
    setNotificationsEnabled(enabled);
    if (!enabled) setNotifications([]);
    try { localStorage.setItem('clinic-notifications-enabled', String(enabled)); }
    catch { /* Keep notification controls usable when storage is unavailable. */ }
  }, []);
  const clinicRoleId = role?.platform && !isPlatformOwner ? '' : roleId;
  useMessages({ clinicId, page, roleId: clinicRoleId, peer, setMessages, setContacts: setMessageContacts, setPeer, onError: notify });
  useNotifications({ clinicId, roleId: clinicRoleId, enabled: notificationsEnabled, setItems: setNotifications, onNotification: addNotification });

  useEffect(() => { loadData(); }, [loadData]);
  useEffect(() => {
    if (page !== 'reports' || !roleId) return undefined;
    const categories = ['billing', 'pharmacy'].filter(category =>
      dataCategoriesByRole[roleId]?.includes(category) && !loadedCategories.includes(category));
    if (!categories.length) return undefined;

    let active = true;
    const requestedClinic = clinicId;
    const requestedRole = roleId;
    const requestedRevision = dataRevisionRef.current;
    const isCurrent = () => active && requestedClinic === clinicId
      && requestedRole === roleId && requestedRevision === dataRevisionRef.current;
    setLoading(true);
    setError('');
    Promise.all(categories.map(async category => [
      category,
      await apiRequest(`/api/${requestedClinic}/${category}?page=0&size=100`),
    ])).then(entries => {
      if (!isCurrent() || !getAccessToken()) return;
      setData(current => ({ ...current, ...Object.fromEntries(entries) }));
      setLoadedCategories(current => [...new Set([...current, ...categories])]);
      setRecordPages(current => ({
        ...current,
        ...Object.fromEntries(entries.map(([category, rows]) => [category, { page: 0, hasMore: rows.length === 100 }])),
      }));
    }).catch(loadError => {
      if (isCurrent()) setError(loadError.message);
    }).finally(() => {
      if (isCurrent()) setLoading(false);
    });
    return () => { active = false; };
  }, [clinicId, loadedCategories, page, roleId]);
  useEffect(() => {
    if (page === 'pharmacy' && roleId && (!roles[roleId]?.platform || roleId === 'platform-owner')) {
      const needed = ['prescriptions', 'pharmacy'].filter(category => dataCategoriesByRole[roleId]?.includes(category) && !loadedCategories.includes(category));
      const requestedClinic = clinicId;
      const requestedRole = roleId;
      const requestedRevision = dataRevisionRef.current;
      if (needed.length) Promise.all(needed.map(async category => [category, await apiRequest(`/api/${requestedClinic}/${category}?page=0&size=100`)]))
        .then(entries => {
          if (requestedClinic !== clinicId || requestedRole !== roleId || requestedRevision !== dataRevisionRef.current || !getAccessToken()) return;
          setData(current => ({ ...current, ...Object.fromEntries(entries) }));
          setLoadedCategories(current => [...new Set([...current, ...entries.map(([category]) => category)])]);
          setRecordPages(current => ({ ...current, ...Object.fromEntries(entries.map(([category, rows]) => [category, { page: 0, hasMore: rows.length === 100 }])) }));
        }).catch(loadError => {
          if (requestedClinic === clinicId && requestedRole === roleId && requestedRevision === dataRevisionRef.current) setError(loadError.message);
        });
    }
  }, [clinicId, loadedCategories, page, roleId]);
  useEffect(() => {
    if (!roleId || !schemas[page] || loadedCategories.includes(page)) return;
    ensureCategoryLoaded(page).catch(loadError => setError(loadError.message));
  }, [ensureCategoryLoaded, loadedCategories, page, roleId]);
  useEffect(() => {
    if (!roleId) return undefined;
    if (role?.platform && roleId !== 'platform-owner') { setClinicList([]); return undefined; }
    let active = true;
    setClinicsReady(false);
    setClinicList([]);
    setData({});
    setSearchResults({});
    setLoadedCategories([]);
    setRecordPages({});
    setError('');
    apiRequest('/api/clinics').then(items => {
      if (!active) return;
      setClinicList(items);
      setClinicId(current => items.some(item => item.id === current) ? current : items[0]?.id ?? '');
      setClinicsReady(true);
      if (roleId === 'platform-owner' && items.length === 0) setPage('onboarding');
    }).catch(() => {
      if (!active) return;
      const fallback = import.meta.env.DEV ? demoClinics : [];
      setClinicList(fallback);
      setClinicId(current => fallback.some(item => item.id === current) ? current : fallback[0]?.id ?? '');
      setClinicsReady(true);
      if (roleId === 'platform-owner' && fallback.length === 0) setPage('onboarding');
    });
    return () => { active = false; };
  }, [roleId, role?.platform]);
  useEffect(() => {
    const onExpired = () => {
      dataRevisionRef.current += 1;
      setDataRevision(value => value + 1);
      setRoleId(''); setUsername(''); setData({}); setMessages([]); setNotifications([]); setPage('dashboard');
      notify('Your session expired. Please sign in again.');
    };
    window.addEventListener('clincanywhere:session-expired', onExpired);
    return () => window.removeEventListener('clincanywhere:session-expired', onExpired);
  }, [notify]);

  async function submitLogin(event) {
    event.preventDefault();
    setBusy(true); setLoginError('');
    try {
      const result = await apiRequest(registering ? '/api/auth/register' : '/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username: loginUsername.trim(), password: loginPassword, invitationToken, ...registrationProfile }),
      });
      const nextRole = roleIdsByAuthority[result.role];
      if (!nextRole) throw new Error('This account role is not supported.');
      const accountIsPlatform = Boolean(roles[nextRole]?.platform);
      if (accountIsPlatform !== (loginPortal === 'platform')) {
        throw new Error(accountIsPlatform
          ? 'This is a platform account. Switch to Platform team sign in.'
          : 'This is a hospital account. Switch to Hospital team sign in.');
      }
      setAccessToken(result.accessToken);
      setAuthenticatedUsername(result.username);
      if (!roles[nextRole]?.platform || nextRole === 'platform-owner') {
        try {
          const memberships = await apiRequest('/api/clinics');
          setClinicList(memberships);
          setClinicId(memberships[0]?.id ?? '');
          setClinicsReady(true);
        } catch { /* A clinic workspace can still be selected from the current session if needed. */ }
      }
      setUsername(result.username);
      setRoleId(nextRole);
      setPage(nextRole === 'onboarding' ? 'onboarding' : 'dashboard');
      setRegistering(false);
    } catch (authError) {
      setLoginError(registering
        ? authError.message
        : authError.message.startsWith('This is a ')
          ? authError.message
          : 'Sign-in failed. Check your password, or choose the demo username shown above.');
    }
    finally { setBusy(false); }
  }

  function selectDemoRole(id) {
    const credential = demoCredentials[id];
    setLoginError('');
    setLoginUsername(credential.username);
    setLoginPassword(credential.password);
  }

  function changeLoginPortal(nextPortal) {
    setLoginPortal(nextPortal);
    setRegistering(false);
    setLoginError('');
    if (demoModeEnabled) selectDemoRole(nextPortal === 'platform' ? 'platform-owner' : 'admin');
  }

  function logout() {
    dataRevisionRef.current += 1;
    setDataRevision(value => value + 1);
    clearAccessToken(); setRoleId(''); setUsername(''); setData({}); setSearchResults({}); setLoadedCategories([]); setClinicList([]); setClinicsReady(false); setMessages([]); setNotifications([]); setPage('dashboard'); setError(''); setShowSignIn(false);
  }

  function switchClinic(nextClinicId) {
    if (nextClinicId === clinicId) return;
    dataRevisionRef.current += 1;
    setData({}); setSearchResults({}); setLoadedCategories([]); setRecordPages({}); setError(''); setLoading(true); setClinicId(nextClinicId);
  }

  async function refreshPlatformClinics() {
    const items = await apiRequest('/api/clinics');
    setClinicList(items);
    setClinicId(current => items.some(item => item.id === current) ? current : items[0]?.id ?? '');
    setClinicsReady(true);
    setPage(items.length ? 'dashboard' : 'onboarding');
  }

  async function saveRecord(category, record, isNew) {
    const requestedClinic = clinicId;
    const requestedRevision = dataRevisionRef.current;
    const url = `/api/${requestedClinic}/${category}${isNew ? '' : `/${encodeURIComponent(record.id)}`}`;
    await apiRequest(url, { method: isNew ? 'POST' : 'PUT', body: JSON.stringify(record) });
    if (requestedClinic !== clinicId || requestedRevision !== dataRevisionRef.current || !getAccessToken()) return;
    setModal(null);
    await refreshCategory(category);
    notify(`${titles[category] ?? 'Record'} ${isNew ? 'added' : 'updated'}`);
  }

  async function deleteRecord(category, record) {
    if (!window.confirm(`Delete ${record.name || record.patient || record.id}? This cannot be undone.`)) return;
    const requestedClinic = clinicId;
    const requestedRevision = dataRevisionRef.current;
    try {
      await apiRequest(`/api/${requestedClinic}/${category}/${encodeURIComponent(record.id)}`, { method: 'DELETE' });
      if (requestedClinic !== clinicId || requestedRevision !== dataRevisionRef.current || !getAccessToken()) return;
      await refreshCategory(category);
      notify('Record deleted');
    } catch (deleteError) { notify(deleteError.message); }
  }

  async function sendMessage(event) {
    event.preventDefault();
    const body = draft.trim();
    if (!body || !peer) return;
    const requestedClinic = clinicId;
    const requestedPeer = peer;
    const requestedRevision = dataRevisionRef.current;
    try {
      const saved = await apiRequest(`/api/${requestedClinic}/messages`, { method: 'POST', body: JSON.stringify({ recipient: requestedPeer, body }) });
      if (requestedClinic !== clinicId || requestedPeer !== peer || requestedRevision !== dataRevisionRef.current || !getAccessToken()) return;
      setMessages(previous => previous.some(item => item.id === saved.id) ? previous : [...previous, saved]);
      setDraft('');
    } catch (sendError) { notify(sendError.message); }
  }

  if (!roleId) return <>
    <MarketingSite onSignIn={portal => { changeLoginPortal(portal); setLoginError(''); setShowSignIn(true); }} />
    {showSignIn && <LoginPage embedded onClose={() => { setShowSignIn(false); setLoginError(''); }} loginPortal={loginPortal} onPortalChange={changeLoginPortal} registering={registering} setRegistering={setRegistering} loginError={loginError} onSubmit={submitLogin} onSelectRole={selectDemoRole} invitationToken={invitationToken} setInvitationToken={setInvitationToken} registrationProfile={registrationProfile} setRegistrationProfile={setRegistrationProfile} loginUsername={loginUsername} setLoginUsername={setLoginUsername} loginPassword={loginPassword} setLoginPassword={setLoginPassword} busy={busy} />}
  </>;
  return <SidebarProvider open={!sidebarCollapsed} onOpenChange={open => setSidebarCollapsed(!open)}>
    <div className={`app-shell ${sidebarCollapsed ? 'app-shell-sidebar-collapsed' : ''}`}>
      {mobileOpen && <button className="sidebar-backdrop" aria-label="Close navigation" onClick={() => setMobileOpen(false)} />}
      <Sidebar className={mobileOpen ? 'sidebar-open' : ''}>
        <div className="brand"><div className="brand-identity"><div className="brandmark"><HeartPulse size={24} /></div><div className="brand-copy"><strong>ClincyAnywhere</strong><small>{role?.platform ? 'PLATFORM CONSOLE' : 'HOSPITAL WORKSPACE'}</small></div></div><SidebarTrigger className="sidebar-collapse-toggle" aria-label={sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'} title={sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}>{sidebarCollapsed ? <PanelLeftOpen size={18} /> : <PanelLeftClose size={18} />}</SidebarTrigger></div>
        <nav className="navigation" aria-label="Main navigation">
          {navGroups.map(group => {
            const items = group.items.filter(([id]) => allowedPages.includes(id));
            if (!items.length) return null;
            return <div className="nav-group" key={group.label}><div className="nav-label">{group.label}</div>{items.map(([id, label, Icon]) => <button type="button" key={id} title={localizedTitles[id] ?? label} aria-label={localizedTitles[id] ?? label} className={`nav-item ${page === id ? 'selected' : ''}`} onClick={() => { navigateTo(id); setMobileOpen(false); }}><Icon size={18} /><span>{localizedTitles[id] ?? label}</span>{id === 'patients' && <small className="nav-count">{data.patients?.length ?? ''}</small>}</button>)}</div>;
          })}
        </nav>
        <div className="sidebar-foot"><span className="secure-dot"><ShieldCheck size={15} /></span><div><b>{isPlatformOwner ? 'Platform administrator' : role?.platform ? 'Platform access' : 'Hospital access'}</b><small>{isPlatformOwner ? 'All hospital workspaces' : role?.platform ? 'Onboarding across hospitals' : 'Hospital-scoped workspace'}</small></div></div>
      </Sidebar>
      <main className="main-area">
        <OfflineBanner />
        <header className="topbar">
          <div className="topbar-start"><button type="button" className="icon-button mobile-menu" aria-label={mobileOpen ? 'Close navigation' : 'Open navigation'} aria-expanded={mobileOpen} onClick={() => setMobileOpen(open => !open)}><Menu size={20} /></button><div className="breadcrumb"><span>{role?.platform && (!isPlatformOwner || clinicList.length === 0) ? 'Platform workspace' : clinic.name}</span><b>{page === 'profile' ? 'My profile' : localizedTitles[page] ?? 'Overview'}</b></div></div>
          <div className="top-actions">
            {(!role?.platform || isPlatformOwner) && <label className="top-search"><Search size={16} /><Input aria-label="Search current view" value={search} onChange={event => setSearch(event.target.value)} placeholder="Search this view..." /></label>}
            {(!role?.platform || isPlatformOwner) && clinicList.length > 0 && <label className="clinic-select"><BriefcaseMedical size={15} /><Select aria-label="Clinic workspace" value={clinicId} onChange={event => switchClinic(event.target.value)}>{clinicList.map(item => <option value={item.id} key={item.id}>{item.name}</option>)}</Select><ChevronDown size={14} /></label>}
            {allowedPages.includes('followups') && <Button variant="ghost" className="notification-button" aria-label={`Notifications ${notifications.length}`} title={notifications[0]?.message ?? 'Notifications'} onClick={() => navigateTo('followups')}><Bell size={17} />{notifications.length > 0 && <i />}</Button>}
            <Select aria-label="Interface language" value={lang} onChange={event=>setLanguage(event.target.value)}><option value="en">EN</option><option value="hi">हिंदी</option><option value="mr">मराठी</option></Select>
            <div className="account-menu-wrap" ref={accountMenuRef}>
              <button type="button" className={`user-chip account-menu-trigger ${accountMenuOpen ? 'account-menu-active' : ''}`} aria-haspopup="menu" aria-expanded={accountMenuOpen} onClick={() => setAccountMenuOpen(value => !value)}>
                <span className="avatar">{role.initials}</span><span className="user-details"><b>{role.label}</b><small>{username}</small></span><ChevronDown className="account-menu-chevron" size={14}/>
              </button>
              {accountMenuOpen && <div className="account-menu" role="menu" aria-label="Account menu">
                <div className="account-menu-heading"><span className="avatar">{role.initials}</span><span><b>{role.label}</b><small>{username}</small></span></div>
                <button type="button" role="menuitem" onClick={() => openAccountPage('profile')}><UserRound size={16}/>My profile</button>
                {allowedPages.includes('settings') && <button type="button" role="menuitem" onClick={() => openAccountPage('settings')}><Settings2 size={16}/>Workspace settings</button>}
                <div className="account-menu-divider"/>
                <button type="button" role="menuitem" className="account-menu-logout" onClick={() => { setAccountMenuOpen(false); logout(); }}><LogOut size={16}/>Sign out</button>
              </div>}
            </div>
          </div>
        </header>
        <ScrollArea className="content">
          {error && <Alert variant="destructive" className="error-banner"><span><b>Could not load clinic data.</b> {error}</span><Button variant="outline" onClick={loadData}><RefreshCw size={14} /> Retry</Button></Alert>}
          {loading && <div className="loading-line" role="status" aria-live="polite"><Spinner /> {t('loading')}</div>}
          {!loading && page === 'dashboard' && <Dashboard data={data} clinic={clinic} allowedPages={allowedPages} onNavigate={navigateTo} username={username} role={role} />}
          {!loading && page === 'onboarding' && role?.platform && <PlatformOnboardingPage roleId={roleId} username={username} onNotify={notify} onWorkspaceCreated={isPlatformOwner ? refreshPlatformClinics : undefined} />}
          {!loading && page === 'onboarding-team' && roleId === 'platform-owner' && <PlatformTeamPage onNotify={notify} />}
            {page === 'messages' && <MessagesPage clinicId={clinicId} peer={peer} contacts={messageContacts} onPeerChange={setPeer} messages={messages} draft={draft} onDraftChange={setDraft} onSend={sendMessage} username={username} />}
          {!loading && page === 'reports' && <ReportsPage data={data} onExport={downloadRows} />}
          {!loading && page === 'abdm' && <DigitalHealthPage clinicId={clinicId} data={data} roleId={roleId} onNotify={notify} />}
          {!loading && page === 'parity' && <ParityPage />}
          {!loading && page === 'support' && <SupportPage clinicId={clinicId} roleId={roleId} data={data} onNotify={notify} />}
          {!loading && page === 'recovery' && <RecoveryPage clinicId={clinicId} data={data} roleId={roleId} onRefresh={loadData} onNotify={notify} />}
          {!loading && page === 'voice' && <VoiceAssistantPage clinicId={clinicId} data={data} onSave={saveRecord} onNotify={notify} />}
          {!loading && page === 'settings' && <SettingsPage clinicId={clinicId} clinic={clinic} data={data.settings?.[0]} roleId={roleId} notificationsEnabled={notificationsEnabled} onNotificationsEnabledChange={changeNotificationsEnabled} onSaved={loadData} onExport={() => downloadRows(data, 'clincanywhere-demo-data.json', 'json')} onNotify={notify} />}
          {!loading && page === 'profile' && <ProfilePage onNotify={notify} />}
          {!loading && (page === 'pharmacy' ? <PharmacyPage clinicId={clinicId} data={data} roleId={roleId} onRefresh={loadData} onNotify={notify} /> : page === 'billing' ? <BillingPage clinicId={clinicId} records={search.trim() ? searchResults[page] ?? [] : data[page] ?? []} search={search} onSearchChange={setSearch} roleId={roleId} onAdd={() => setModal({ category: page })} onEdit={record => setModal({ category: page, record })} onDelete={record => deleteRecord(page, record)} onExport={(_rows, filename) => exportCategory(page, filename)} onRefresh={() => refreshCategory(page)} onNotify={notify} hasMore={!search.trim() && Boolean(recordPages[page]?.hasMore)} onLoadMore={() => loadMoreRecords(page)} /> : schemas[page] && <RecordsPage category={page} records={search.trim() ? searchResults[page] ?? [] : data[page] ?? []} search={search} onSearchChange={setSearch} onSearchRecords={searchRecords} hasMore={!search.trim() && Boolean(recordPages[page]?.hasMore)} onLoadMore={() => loadMoreRecords(page)} roleId={roleId} onAdd={() => setModal({ category: page })} onEdit={record => setModal({ category: page, record })} onDelete={record => deleteRecord(page, record)} onExport={(_rows, filename) => exportCategory(page, filename)} />)}
        </ScrollArea>
        <footer className="workspace-footer"><span><ShieldCheck size={14}/>{isPlatformOwner ? 'Platform owner · all hospital workspaces' : role?.platform ? 'Platform access · hospital onboarding' : `${clinic.name} · hospital-scoped records`}</span><span>ClincyAnywhere <i>·</i> Care workspace</span></footer>
      </main>
      <RecordDialog modal={modal} data={data} roleId={roleId} clinicId={clinicId} onClose={() => setModal(null)} onSave={saveRecord} />
    </div>
  </SidebarProvider>;
}







