import { Activity, Bell, BedDouble, Building2, CalendarDays, ChartNoAxesCombined, ClipboardList, CreditCard, FileText, FlaskConical, HeartPulse, LayoutDashboard, MessageCircle, Package, Pill, Settings, Stethoscope, Users, ShieldCheck, Scale } from 'lucide-react';
import { useLanguage } from './i18n.js';

export const demoModeEnabled = import.meta.env.DEV || import.meta.env.VITE_DEMO_MODE === 'true';
export const roleIdsByAuthority = { ADMIN: 'admin', RECEPTIONIST: 'receptionist', DOCTOR: 'doctor', LAB: 'lab', PHARMACIST: 'pharmacist', BILLING: 'billing', INVENTORY: 'inventory', PATIENT: 'patient', PLATFORM_OWNER: 'platform-owner', ONBOARDING: 'onboarding' };
export const clinics = [
  { id: 'clinic-a', name: 'ClincyAnywhere Health Centre' },
  { id: 'clinic-b', name: 'Sunrise Family Clinic' },
  { id: 'clinic-c', name: 'Lotus Multispeciality' },
];

export const roles = {
  admin: { label: 'Administrator', username: 'admin', initials: 'AD', pages: 'all' },
  receptionist: { label: 'Receptionist / Front Desk', username: 'reception', initials: 'FR', pages: ['dashboard', 'patients', 'appointments', 'doctors', 'billing', 'followups', 'instantreminders', 'recovery', 'voice', 'support'] },
  doctor: { label: 'Doctor', username: 'doctor', initials: 'DR', pages: ['dashboard', 'patients', 'appointments', 'records', 'prescriptions', 'followups', 'instantreminders', 'laboratory', 'doctors', 'recovery', 'voice', 'abdm'] },
  lab: { label: 'Laboratory Technician', username: 'labtech', initials: 'LB', pages: ['dashboard', 'patients', 'laboratory', 'records', 'voice'] },
  pharmacist: { label: 'Pharmacist', username: 'pharmacy', initials: 'PH', pages: ['dashboard', 'pharmacy', 'prescriptions', 'voice'] },
  billing: { label: 'Billing Executive', username: 'billing', initials: 'BI', pages: ['dashboard', 'patients', 'appointments', 'billing', 'reports', 'recovery'] },
  inventory: { label: 'Inventory Manager', username: 'store', initials: 'IN', pages: ['dashboard', 'pharmacy', 'inventory', 'reports'] },
  patient: { label: 'Patient', username: 'patient', initials: 'PT', pages: ['dashboard', 'patients', 'support'] },
  'platform-owner': { label: 'Platform Owner', username: 'owner', initials: 'PO', pages: 'all', platform: true },
  onboarding: { label: 'Onboarding Teammate', username: 'onboarder', initials: 'OB', pages: ['onboarding'], platform: true },
};

// Keep the initial API read set aligned with RecordAuthorization.canRead on the API.
// This prevents a denied optional module from breaking the whole workspace load.
export const dataCategoriesByRole = {
  admin: ['patients', 'appointments', 'doctors', 'records', 'prescriptions', 'followups', 'instantreminders', 'laboratory', 'pharmacy', 'beds', 'billing', 'inventory', 'settings'],
  receptionist: ['patients', 'appointments', 'doctors', 'billing', 'followups', 'instantreminders', 'support', 'settings'],
  doctor: ['patients', 'appointments', 'doctors', 'records', 'prescriptions', 'followups', 'instantreminders', 'laboratory', 'settings'],
  lab: ['patients', 'laboratory', 'records', 'doctors', 'settings'],
  pharmacist: ['prescriptions', 'pharmacy', 'patients', 'doctors', 'settings'],
  billing: ['patients', 'appointments', 'billing', 'doctors', 'settings'],
  inventory: ['pharmacy', 'inventory', 'patients', 'appointments', 'doctors', 'settings'],
  patient: ['patients'],
  'platform-owner': ['patients', 'appointments', 'doctors', 'records', 'prescriptions', 'followups', 'instantreminders', 'laboratory', 'pharmacy', 'beds', 'billing', 'inventory', 'settings'],
};

export const navGroups = [
  { label: 'Workspace', items: [['dashboard', 'Overview', LayoutDashboard], ['patients', 'Patients', Users], ['appointments', 'Appointments', CalendarDays], ['doctors', 'Doctors & Staff', Stethoscope]] },
  { label: 'Care team', items: [['messages', 'Care team messages', MessageCircle], ['support', 'Patient support', HeartPulse]] },
  { label: 'Clinical care', items: [['records', 'Medical Records', ClipboardList], ['prescriptions', 'Prescriptions', FileText], ['followups', 'Follow-up & Reminders', Bell], ['recovery', 'Patient recovery', Activity], ['voice', 'Voice assistant', MessageCircle], ['instantreminders', 'Instant Reminder', Activity], ['laboratory', 'Laboratory', FlaskConical]] },
  { label: 'Operations', items: [['pharmacy', 'Pharmacy', Pill], ['beds', 'Beds & Wards', BedDouble], ['billing', 'Billing & Payments', CreditCard], ['inventory', 'Inventory', Package], ['abdm', 'Digital Health & ABDM', ShieldCheck], ['parity', 'e-Sushrut comparison', Scale], ['reports', 'Reports & Analytics', ChartNoAxesCombined], ['settings', 'Settings', Settings]] },
  { label: 'Platform', items: [['onboarding', 'Clinic onboarding', Building2], ['onboarding-team', 'Onboarding team', Users]] },
];

export const schemas = {
  patients: { title: 'Register patient', prefix: 'PT', fields: [['name', 'Full name', 'text', true], ['age', 'Age', 'number', true], ['gender', 'Gender', 'select', true, ['Male', 'Female', 'Other']], ['phone', 'Phone number', 'tel', true], ['email', 'Email address', 'email'], ['blood', 'Blood group', 'select', false, ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-']], ['doctor', 'Assigned doctor', 'select', false, 'doctors'], ['condition', 'Reason / condition', 'text'], ['status', 'Patient status', 'select', false, ['Active', 'Follow-up', 'Discharged']]], columns: [['Patient', 'name', 'person'], ['Patient ID', 'id'], ['Age / Gender', 'ageGender'], ['Phone', 'phone'], ['Assigned doctor', 'doctor'], ['Status', 'status']] },
  appointments: { title: 'Book appointment', prefix: 'AP', fields: [['patient', 'Patient', 'select', true, 'patients'], ['doctor', 'Doctor', 'select', true, 'doctors'], ['department', 'Department', 'text', true], ['date', 'Date (IST)', 'date', true], ['time', 'Time (IST)', 'time', true], ['type', 'Appointment type', 'select', true, ['Consultation', 'Follow-up', 'Review', 'Procedure']], ['status', 'Status', 'select', true, ['Confirmed', 'Waiting', 'Checked in', 'Completed', 'No-show', 'Cancelled']], ['visitMode', 'Visit mode', 'select', false, ['In-person', 'Telemedicine']], ['videoLink', 'Video visit URL (from your clinic provider)', 'url'], ['visitInstructions', 'Video visit instructions', 'textarea']], columns: [['Patient', 'patient', 'person'], ['Appointment ID', 'id'], ['Doctor', 'doctor'], ['Department', 'department'], ['Date & time (IST)', 'dateTime'], ['Visit mode', 'visitMode'], ['Video visit', 'videoLink'], ['Status', 'status']] },
  doctors: { title: 'Add doctor / staff', prefix: 'DR', fields: [['name', 'Full name', 'text', true], ['specialty', 'Department / specialty', 'text', true], ['phone', 'Phone number', 'tel', true], ['email', 'Email', 'email'], ['shift', 'Shift (IST)', 'text'], ['status', 'Availability', 'select', true, ['Available', 'In surgery', 'On leave']]], columns: [['Clinician', 'name', 'person'], ['ID', 'id'], ['Specialty', 'specialty'], ['Phone', 'phone'], ['Shift (IST)', 'shift'], ['Availability', 'status']] },
  records: { title: 'Create medical record', prefix: 'MR', fields: [['patient', 'Patient', 'select', true, 'patients'], ['doctor', 'Doctor', 'select', true, 'doctors'], ['date', 'Date', 'date', true], ['diagnosis', 'Diagnosis', 'text', true], ['notes', 'Clinical notes', 'textarea'], ['prescription', 'Prescription notes', 'textarea'], ['status', 'Status', 'select', false, ['Reviewed', 'Follow-up', 'Pending']]], columns: [['Patient', 'patient', 'person'], ['Record ID', 'id'], ['Diagnosis', 'diagnosis'], ['Doctor', 'doctor'], ['Date', 'date'], ['Status', 'status']] },
  prescriptions: { title: 'Create prescription', prefix: 'RX', fields: [['patient', 'Patient', 'select', true, 'patients'], ['doctor', 'Doctor', 'select', true, 'doctors'], ['date', 'Prescription date', 'date', true], ['medicines', 'Medicines (clinician-approved)', 'textarea', true], ['instructions', 'Patient instructions', 'textarea'], ['followupDate', 'Follow-up date', 'date'], ['clinicalStatus', 'Clinician approval', 'select', false, ['Draft', 'Finalized']], ['pharmacyStatus', 'Pharmacy status', 'select', false, ['Draft', 'New', 'Processing', 'Ready', 'Dispensed']], ['whatsappStatus', 'WhatsApp status', 'text'], ['notes', 'Internal note', 'text']], columns: [['Patient', 'patient', 'person'], ['Prescription', 'id'], ['Doctor', 'doctor'], ['Date', 'date'], ['Clinical review', 'clinicalStatus'], ['Pharmacy', 'pharmacyStatus']] },
  followups: { title: 'Schedule follow-up', prefix: 'FU', fields: [['patient', 'Patient', 'select', true, 'patients'], ['doctor', 'Doctor', 'select', true, 'doctors'], ['dueDate', 'Follow-up date', 'date', true], ['reminderDays', 'Remind days before', 'number'], ['channel', 'Reminder channel', 'select', false, ['WhatsApp preview', 'SMS preview', 'Call task']], ['status', 'Reminder status', 'select', false, ['Scheduled', 'Reminder sent']], ['appointmentStatus', 'Appointment status', 'select', false, ['Pending', 'Booked']], ['contactStatus', 'Contact outcome', 'select', false, ['Not contacted', 'Contacted', 'Reached', 'No answer']], ['returnStatus', 'Clinic return', 'select', false, ['Not returned', 'Returned']], ['notes', 'Reception notes', 'textarea']], columns: [['Patient', 'patient', 'person'], ['Follow-up ID', 'id'], ['Doctor', 'doctor'], ['Due date', 'dueDate'], ['Reminder', 'status'], ['Appointment', 'appointmentStatus']] },
  instantreminders: { title: 'Create instant reminder', prefix: 'IR', fields: [['patientName', 'Patient name', 'text', true], ['phone', 'Phone number', 'tel'], ['channel', 'Channel', 'select', true, ['WhatsApp preview', 'SMS preview', 'Call task']], ['message', 'Message', 'textarea', true], ['status', 'Status', 'text']], columns: [['Patient', 'patientName', 'person'], ['Reminder ID', 'id'], ['Channel', 'channel'], ['Message', 'message'], ['Created', 'createdAt'], ['Status', 'status']] },
  laboratory: { title: 'Add laboratory order', prefix: 'LB', fields: [['patient', 'Patient', 'select', true, 'patients'], ['test', 'Test name', 'text', true], ['orderedBy', 'Ordered by', 'select', true, 'doctors'], ['date', 'Date', 'date', true], ['priority', 'Priority', 'select', false, ['Routine', 'Urgent']], ['status', 'Status', 'select', false, ['Ordered', 'Sample collected', 'In progress', 'Results ready']]], columns: [['Patient', 'patient', 'person'], ['Order ID', 'id'], ['Test', 'test'], ['Ordered by', 'orderedBy'], ['Date', 'date'], ['Status', 'status']] },
  pharmacy: { title: 'Add pharmacy item', prefix: 'MED', fields: [['name', 'Medicine name', 'text', true], ['category', 'Category', 'text', true], ['stock', 'Stock quantity', 'number', true], ['reorder', 'Reorder level', 'number', true], ['price', 'Unit price (₹)', 'number', true], ['expiry', 'Expiry date', 'date']], columns: [['Medicine', 'name', 'person'], ['SKU', 'id'], ['Category', 'category'], ['In stock', 'stock'], ['Reorder level', 'reorder'], ['Unit price', 'price'], ['Expiry', 'expiry']] },
  beds: { title: 'Add bed / ward', prefix: 'BED', fields: [['ward', 'Ward', 'text', true], ['type', 'Bed type', 'text', true], ['patient', 'Patient (optional)', 'select', false, 'patients'], ['status', 'Status', 'select', true, ['Available', 'Occupied', 'Cleaning', 'Maintenance']], ['dailyRate', 'Daily rate (₹)', 'number', true]], columns: [['Bed', 'id'], ['Ward', 'ward'], ['Bed type', 'type'], ['Patient', 'patient'], ['Daily rate', 'dailyRate'], ['Status', 'status']] },
  billing: { title: 'Create invoice', prefix: 'INV', fields: [['patient', 'Patient', 'select', true, 'patients'], ['date', 'Invoice date', 'date', true], ['description', 'Description', 'text', true], ['amount', 'Total amount (₹)', 'number', true]], columns: [['Invoice', 'id'], ['Patient', 'patient', 'person'], ['Invoice date', 'date'], ['Description', 'description'], ['Total', 'amount'], ['Paid', 'paid'], ['Status', 'status']] },
  inventory: { title: 'Add inventory item', prefix: 'ST', fields: [['name', 'Item name', 'text', true], ['category', 'Category', 'text', true], ['stock', 'Quantity in stock', 'number', true], ['reorder', 'Reorder level', 'number', true], ['unit', 'Unit', 'text', true], ['supplier', 'Supplier', 'text']], columns: [['Item', 'name', 'person'], ['Item ID', 'id'], ['Category', 'category'], ['Stock', 'stock'], ['Reorder level', 'reorder'], ['Unit', 'unit'], ['Supplier', 'supplier']] },
};

export const roleWrites = {
  receptionist: ['patients', 'appointments', 'billing', 'followups', 'instantreminders'],
  doctor: ['patients', 'appointments', 'records', 'prescriptions', 'followups', 'instantreminders', 'laboratory'],
  lab: ['laboratory', 'records'],
  pharmacist: ['pharmacy'],
  billing: ['billing'],
  inventory: ['pharmacy', 'inventory'],
};
export const titles = Object.fromEntries(navGroups.flatMap(group => group.items.map(([id, title]) => [id, title])));
export function useLocalizedTitles(){const {t}=useLanguage();return Object.fromEntries(Object.keys(titles).map(id=>{const translated=t(id);return[id,translated===id?titles[id]:translated];}));}
