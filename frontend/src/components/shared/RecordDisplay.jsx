import { Badge, Card } from '../ui/index.js';
import { initials, statusClass } from '../../lib/formatters.js';

export function PersonCell({ name }) { return <span className="person-cell"><span className="mini-avatar">{initials(String(name ?? ''))}</span><b>{name || '—'}</b></span>; }
export function StatusBadge({ value }) { return <Badge className={`status-${statusClass(value)}`}><i />{value || '—'}</Badge>; }
export function Metric({ icon: Icon, label, value }) { return <Card className="stat-card"><div className="stat-top"><span>{label}</span><i className="stat-teal"><Icon size={18} /></i></div><div className="stat-value">{value}</div></Card>; }



