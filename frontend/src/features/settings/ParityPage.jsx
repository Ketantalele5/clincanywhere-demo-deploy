import { Check, CircleHelp, ShieldCheck } from 'lucide-react';
import { Card } from '../../components/ui/index.js';

const available = [
  ['Clinic and OPD', 'Patient registration, appointments, visit records, prescriptions and billing.'],
  ['Clinic operations', 'Pharmacy stock and dispensing, lab orders, reports, staff roles and follow-up recovery.'],
  ['Local language support', 'English, Hindi and Marathi navigation, plus reviewed voice-to-text drafts.'],
  ['Digital health preparation', 'Facility/professional reference fields, local consent requests, and draft EHR/FHIR/HL7 exports.'],
];

const pending = [
  ['ABHA and linked records', 'No official ABHA identity, profile lookup or linked-record exchange is connected. Use the official ABDM flow; this app must not collect Aadhaar or OTP.'],
  ['Scan & Share and ABDM exchange', 'Requires facility and HIP/HIU onboarding, approved credentials, verified callbacks, encryption and conformance review.'],
  ['HPR/HFR verification', 'IDs here are references or staff attestations; no live registry lookup is configured.'],
  ['AIIMS clinical decision support', 'The pre-visit checklist is a workflow reminder, not a diagnosis or treatment recommendation.'],
  ['Telemedicine', 'Appointments can store an HTTPS room link from a clinic-selected provider, but the app does not host video calls or provide patient consent and link delivery workflows.'],
];

export function ParityPage() {
  return <section className="page-section">
    <div className="page-heading"><div><h1>e-Sushrut comparison</h1><p>What this clinic app can do today and what still needs official integration</p></div></div>
    <Card style={{ padding: 20, marginBottom: 16 }}><h2><ShieldCheck size={19}/> Available in this app</h2><div className="dashboard-grid">{available.map(([title, description]) => <article key={title}><h3><Check size={16}/> {title}</h3><p>{description}</p></article>)}</div></Card>
    <Card style={{ padding: 20, marginBottom: 16 }}><h2><CircleHelp size={19}/> Requires external onboarding or development</h2><div className="dashboard-grid">{pending.map(([title, description]) => <article key={title}><h3>{title}</h3><p>{description}</p></article>)}</div></Card>
    <p>ClincyAnywhere is independently developed. It is not e-Sushrut, C-DAC or government-approved. Do not use demo data for real patient care. These workflow features alone do not establish clinical or production readiness.</p>
  </section>;
}
