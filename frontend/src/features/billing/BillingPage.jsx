import { useEffect, useMemo, useState } from 'react';
import { Banknote, CreditCard, Download, Edit2, History, Plus, Search, Trash2 } from 'lucide-react';
import { Alert, Button, Card, Dialog, Input, Label, Spinner, Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../../components/ui/index.js';
import { PersonCell, StatusBadge } from '../../components/shared/RecordDisplay.jsx';
import { roleWrites, titles } from '../../lib/appConfig.js';
import { apiRequest } from '../../lib/apiClient.js';

let razorpayScriptPromise;

function loadRazorpayCheckout() {
  if (window.Razorpay) return Promise.resolve();
  if (!razorpayScriptPromise) {
    razorpayScriptPromise = new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = 'https://checkout.razorpay.com/v1/checkout.js';
      script.async = true;
      script.onload = () => resolve();
      script.onerror = () => { razorpayScriptPromise = null; reject(new Error('Could not load Razorpay checkout. Check the internet connection and retry.')); };
      document.head.appendChild(script);
    });
  }
  return razorpayScriptPromise;
}

const currency = value => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(Number(value) || 0);
const shortDate = value => value ? new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium' }).format(new Date(value)) : '—';

export function BillingPage({ clinicId, records, search, onSearchChange, roleId, onAdd, onEdit, onDelete, onExport, onRefresh, onNotify, hasMore, onLoadMore }) {
  const [gateway, setGateway] = useState({ enabled: false, keyId: '' });
  const [pendingPayments, setPendingPayments] = useState({});
  const [cashInvoice, setCashInvoice] = useState(null);
  const [cashAmount, setCashAmount] = useState('');
  const [cashBusy, setCashBusy] = useState(false);
  const [busyInvoice, setBusyInvoice] = useState('');
  const [historyInvoice, setHistoryInvoice] = useState(null);
  const [historyRows, setHistoryRows] = useState([]);
  const [historyBusy, setHistoryBusy] = useState(false);
  const [pageError, setPageError] = useState('');
  const canWrite = roleId === 'admin' || (roleWrites[roleId] ?? []).includes('billing');

  async function refreshPaymentState() {
    const [config, pending] = await Promise.all([
      apiRequest(`/api/payments/${clinicId}/config`),
      apiRequest(`/api/payments/${clinicId}/pending`),
    ]);
    setGateway(config);
    setPendingPayments(Object.fromEntries(pending.map(payment => [payment.invoiceId, payment])));
  }

  useEffect(() => {
    let active = true;
    setPendingPayments({});
    setGateway({ enabled: false, keyId: '' });
    Promise.all([
      apiRequest(`/api/payments/${clinicId}/config`),
      apiRequest(`/api/payments/${clinicId}/pending`),
    ]).then(([config, pending]) => {
      if (!active) return;
      setGateway(config);
      setPendingPayments(Object.fromEntries(pending.map(payment => [payment.invoiceId, payment])));
      setPageError('');
    }).catch(error => { if (active) setPageError(error.message); });
    return () => { active = false; };
  }, [clinicId]);

  const hasPendingPayment = Object.keys(pendingPayments).length > 0;
  useEffect(() => {
    if (!hasPendingPayment) return undefined;
    let active = true;
    const timer = window.setInterval(async () => {
      try {
        const pending = await apiRequest(`/api/payments/${clinicId}/pending`);
        if (active) setPendingPayments(Object.fromEntries(pending.map(payment => [payment.invoiceId, payment])));
      } catch { /* Keep the last known status during a temporary network failure. */ }
    }, 5000);
    return () => { active = false; window.clearInterval(timer); };
  }, [clinicId, hasPendingPayment]);

  const filtered = useMemo(() => records.filter(record => Object.values(record).join(' ').toLowerCase().includes(search.toLowerCase())), [records, search]);

  async function reloadAfterPayment() {
    let invoiceRefreshFailed = false;
    let paymentRefreshFailed = false;
    try { await onRefresh(); }
    catch { invoiceRefreshFailed = true; }
    try { await refreshPaymentState(); }
    catch { paymentRefreshFailed = true; }
    if (invoiceRefreshFailed || paymentRefreshFailed) {
      setPageError('Payment was saved, but some invoice data could not be refreshed. Reload this page to check it.');
    }
  }

  async function recordCash(event) {
    event.preventDefault();
    if (!cashInvoice) return;
    setCashBusy(true);
    setPageError('');
    try {
      await apiRequest(`/api/payments/${clinicId}/invoices/${encodeURIComponent(cashInvoice.id)}/cash`, {
        method: 'POST', body: JSON.stringify({ amount: Number(cashAmount) }),
      });
      setCashInvoice(null);
      setCashAmount('');
      onNotify('Cash payment recorded against the invoice.');
      await reloadAfterPayment();
    } catch (error) { setPageError(error.message); }
    finally { setCashBusy(false); }
  }

  async function startOnlinePayment(invoice) {
    if (!gateway.enabled) return;
    setBusyInvoice(invoice.id);
    setPageError('');
    try {
      await loadRazorpayCheckout();
      const duePaise = Math.max(Math.round(((Number(invoice.amount) || 0) - (Number(invoice.paid) || 0)) * 100), 0);
      const due = duePaise / 100;
      const order = await apiRequest(`/api/payments/${clinicId}/invoices/${encodeURIComponent(invoice.id)}/gateway/orders`, {
        method: 'POST', body: JSON.stringify({ amount: due }),
      });
      setPendingPayments(current => ({ ...current, [invoice.id]: { invoiceId: invoice.id, status: 'PENDING', gatewayOrderId: order.orderId } }));
      const checkout = new window.Razorpay({
        key: order.keyId,
        order_id: order.orderId,
        amount: order.amountPaise,
        currency: order.currency,
        name: 'ClincyAnywhere',
        description: `Invoice ${invoice.id}`,
        theme: { color: '#147d72' },
        handler: async response => {
          try {
            const receipt = await apiRequest(`/api/payments/${clinicId}/invoices/${encodeURIComponent(invoice.id)}/gateway/verify`, {
              method: 'POST', body: JSON.stringify({
                attemptId: order.attemptId,
                orderId: response.razorpay_order_id,
                paymentId: response.razorpay_payment_id,
                signature: response.razorpay_signature,
              }),
            });
            onNotify(receipt.status === 'REVIEW_REQUIRED'
              ? 'Payment received; the invoice needs a payment reconciliation review.'
              : 'Online payment verified and recorded.');
            await reloadAfterPayment();
          } catch (error) {
            setPageError(error.message);
            try { await refreshPaymentState(); } catch { /* Keep the original payment verification error visible. */ }
          }
        },
        modal: { ondismiss: () => onNotify('Checkout closed. The invoice stays unchanged until payment is verified.') },
      });
      checkout.on('payment.failed', response => {
        const reason = response?.error?.description;
        onNotify(reason ? `Payment was not completed: ${reason}` : 'Payment was not completed. You can retry or record cash after the gateway reports its final status.');
      });
      checkout.open();
    } catch (error) { setPageError(error.message); }
    finally { setBusyInvoice(''); }
  }

  async function openHistory(invoice) {
    setHistoryInvoice(invoice);
    setHistoryRows([]);
    setHistoryBusy(true);
    try {
      const rows = await apiRequest(`/api/payments/${clinicId}/invoices/${encodeURIComponent(invoice.id)}`);
      setHistoryRows(rows);
    } catch (error) { setPageError(error.message); }
    finally { setHistoryBusy(false); }
  }

  return <section className="billing-page">
    <div className="page-heading"><div><h1>{titles.billing}</h1><p>Invoices, cash receipts, online payments, and balances</p></div>
      <div className="heading-actions"><Button variant="outline" onClick={() => onExport(records, 'billing.csv')}><Download size={15} /> Export CSV</Button>{canWrite && <Button variant="primary" onClick={onAdd}><Plus size={16} /> Create invoice</Button>}</div>
    </div>
    {!gateway.enabled && <Alert className="form-error">Online payments are unavailable until Razorpay keys and a webhook secret are configured on the server. Cash recording remains available.</Alert>}
    {pageError && <Alert variant="destructive" className="form-error">{pageError}</Alert>}
    <Card className="data-card"><div className="table-toolbar"><span>{filtered.length} invoices loaded</span><label className="table-search"><Search size={15} /><Input aria-label="Search invoices" value={search} onChange={event => onSearchChange(event.target.value)} placeholder="Search invoices..." /></label></div>
      <div className="table-scroll"><Table><TableHeader><TableRow><TableHead>Invoice</TableHead><TableHead>Patient</TableHead><TableHead>Date</TableHead><TableHead>Description</TableHead><TableHead>Total</TableHead><TableHead>Received</TableHead><TableHead>Balance</TableHead><TableHead>Status</TableHead><TableHead>Actions</TableHead></TableRow></TableHeader><TableBody>
        {filtered.map(invoice => {
          const amount = Number(invoice.amount) || 0;
          const paid = Number(invoice.paid) || 0;
          const due = Math.max(amount - paid, 0);
          const pending = pendingPayments[invoice.id];
          return <TableRow key={invoice.id}><TableCell>{invoice.id}</TableCell><TableCell><PersonCell name={invoice.patient} /></TableCell><TableCell>{invoice.date || '—'}</TableCell><TableCell>{invoice.description}</TableCell><TableCell>{currency(amount)}</TableCell><TableCell>{currency(paid)}</TableCell><TableCell>{currency(due)}</TableCell><TableCell><StatusBadge value={pending ? 'Online payment pending' : invoice.status} /></TableCell>
            <TableCell><div className="row-actions">
              <Button variant="outline" title="Payment history" aria-label={`Payment history ${invoice.id}`} onClick={() => openHistory(invoice)}><History size={14} /></Button>
              {canWrite && <>
                <Button variant="outline" title="Record cash" aria-label={`Record cash payment ${invoice.id}`} disabled={due <= 0 || Boolean(pending)} onClick={() => { setCashInvoice(invoice); setCashAmount(due.toFixed(2)); }}><Banknote size={14} /></Button>
                <Button variant="outline" title={pending ? 'Resume online payment' : 'Pay online'} aria-label={`${pending ? 'Resume' : 'Pay online'} ${invoice.id}`} disabled={!gateway.enabled || due <= 0 || busyInvoice === invoice.id} onClick={() => startOnlinePayment(invoice)}>{busyInvoice === invoice.id ? <Spinner /> : <CreditCard size={14} />}</Button>
                <Button variant="ghost" aria-label={`Edit invoice ${invoice.id}`} title="Edit invoice" onClick={() => onEdit(invoice)}><Edit2 size={14} /></Button>
                <Button variant="ghost" aria-label={`Delete invoice ${invoice.id}`} title="Delete invoice" onClick={() => onDelete(invoice)}><Trash2 size={14} /></Button>
              </>}
            </div></TableCell>
          </TableRow>;
        })}
      </TableBody></Table>{!filtered.length && <div className="empty-inline">{records.length ? 'No invoices match your search.' : 'No invoices are available yet.'}</div>}</div>
      {hasMore && !search.trim() && <Button variant="outline" onClick={onLoadMore}>Load more invoices</Button>}
    </Card>

    <Dialog open={Boolean(cashInvoice)} onOpenChange={open => { if (!open && !cashBusy) setCashInvoice(null); }} title="Record cash payment" description={cashInvoice ? `Invoice ${cashInvoice.id} · balance ${currency((Number(cashInvoice.amount) || 0) - (Number(cashInvoice.paid) || 0))}` : ''} className="modal-card">
      {cashInvoice && <form onSubmit={recordCash}><div className="modal-body form-grid"><Label htmlFor="cash-amount">Cash received (₹)<Input id="cash-amount" type="number" min="0.01" step="0.01" required value={cashAmount} onChange={event => setCashAmount(event.target.value)} /></Label></div><div className="modal-footer"><span className="muted">A cash receipt will be added to the payment history.</span><div><Button type="button" variant="outline" onClick={() => setCashInvoice(null)}>Cancel</Button><Button type="submit" variant="primary" disabled={cashBusy}>{cashBusy ? <Spinner /> : null}Record payment</Button></div></div></form>}
    </Dialog>

    <Dialog open={Boolean(historyInvoice)} onOpenChange={open => !open && setHistoryInvoice(null)} title={historyInvoice ? `Payment history · ${historyInvoice.id}` : 'Payment history'} description="Cash receipts and verified Razorpay transactions" className="modal-card">
      <div className="modal-body">{historyBusy ? <Spinner /> : historyRows.length ? <Table><TableHeader><TableRow><TableHead>Date</TableHead><TableHead>Method</TableHead><TableHead>Amount</TableHead><TableHead>Status</TableHead><TableHead>Reference</TableHead></TableRow></TableHeader><TableBody>{historyRows.map(row => <TableRow key={row.id}><TableCell>{shortDate(row.createdAt)}</TableCell><TableCell>{row.method === 'RAZORPAY' ? 'Razorpay' : 'Cash'}</TableCell><TableCell>{currency(row.amountPaise / 100)}</TableCell><TableCell><StatusBadge value={row.status} /></TableCell><TableCell>{row.gatewayPaymentId || row.gatewayOrderId || '—'}</TableCell></TableRow>)}</TableBody></Table> : <p className="muted">No payment transactions have been recorded for this invoice yet. Older manually entered balances are not included in this history.</p>}</div>
      <div className="modal-footer"><span className="muted">Payment records are kept separately from invoice edits.</span><Button variant="outline" onClick={() => setHistoryInvoice(null)}>Close</Button></div>
    </Dialog>
  </section>;
}
