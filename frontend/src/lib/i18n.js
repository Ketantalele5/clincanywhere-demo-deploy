import { useSyncExternalStore } from 'react';

const en = { overview: 'Overview', patients: 'Patients', appointments: 'Appointments', doctors: 'Doctors & Staff', records: 'Medical Records', prescriptions: 'Prescriptions', followups: 'Follow-up & Reminders', recovery: 'Patient recovery', voice: 'Voice assistant', pharmacy: 'Pharmacy', billing: 'Billing & Payments', reports: 'Reports & Analytics', settings: 'Settings', messages: 'Care team messages', abdm: 'Digital Health & ABDM', support: 'Patient support', onboarding: 'Clinic onboarding', 'onboarding-team': 'Onboarding team', signOut: 'Sign out', loading: 'Loading clinic workspace...', clinic: 'Your clinic at a glance', operations: 'Clinic operations', parity: 'e-Sushrut comparison', noRecordsMatch: 'No records match your search.', noRecordsYet: 'No records are available yet.' };
const hi = { overview: 'मुख्य पृष्ठ', patients: 'मरीज़', appointments: 'अपॉइंटमेंट', doctors: 'डॉक्टर और कर्मचारी', records: 'मेडिकल रिकॉर्ड', prescriptions: 'प्रिस्क्रिप्शन', followups: 'फ़ॉलो-अप और रिमाइंडर', recovery: 'मरीज़ की वापसी', voice: 'वॉइस असिस्टेंट', pharmacy: 'फ़ार्मेसी', billing: 'बिल और भुगतान', reports: 'रिपोर्ट और विश्लेषण', settings: 'सेटिंग्स', messages: 'टीम संदेश', abdm: 'डिजिटल स्वास्थ्य और ABDM', support: 'मरीज़ सहायता', onboarding: 'क्लिनिक ऑनबोर्डिंग', 'onboarding-team': 'ऑनबोर्डिंग टीम', signOut: 'साइन आउट', loading: 'क्लिनिक लोड हो रहा है...', clinic: 'क्लिनिक का सार', operations: 'क्लिनिक संचालन', parity: 'e-Sushrut तुलना', noRecordsMatch: 'आपकी खोज से कोई रिकॉर्ड नहीं मिला।', noRecordsYet: 'अभी कोई रिकॉर्ड उपलब्ध नहीं है।' };
const mr = { overview: 'आढावा', patients: 'रुग्ण', appointments: 'अपॉइंटमेंट', doctors: 'डॉक्टर आणि कर्मचारी', records: 'वैद्यकीय नोंदी', prescriptions: 'प्रिस्क्रिप्शन', followups: 'फॉलो-अप आणि स्मरणपत्रे', recovery: 'रुग्णांचा पाठपुरावा', voice: 'आवाज सहाय्यक', pharmacy: 'फार्मसी', billing: 'बिल आणि देयके', reports: 'अहवाल आणि विश्लेषण', settings: 'सेटिंग्ज', messages: 'टीम संदेश', abdm: 'डिजिटल आरोग्य आणि ABDM', support: 'रुग्ण सहाय्य', onboarding: 'क्लिनिक ऑनबोर्डिंग', 'onboarding-team': 'ऑनबोर्डिंग टीम', signOut: 'साइन आउट', loading: 'क्लिनिक लोड होत आहे...', clinic: 'क्लिनिकचा आढावा', operations: 'क्लिनिक कामकाज', parity: 'e-Sushrut तुलना', noRecordsMatch: 'तुमच्या शोधाशी जुळणाऱ्या नोंदी नाहीत.', noRecordsYet: 'अद्याप कोणत्याही नोंदी उपलब्ध नाहीत.' };

const dashboardEn = {
  dashboardWelcomeBack: 'Welcome back', dashboardLatestFrom: 'Here’s the latest from', dashboardTeamWork: 'Your team’s work, all in one place.',
  dashboardTodayAppointments: "Today's appointments", dashboardTotalScheduled: '{count} scheduled in total', dashboardRegisteredPatients: 'Registered patients', dashboardThisClinic: 'In this clinic',
  dashboardCareTeam: 'Care team', dashboardStaffProfiles: 'Staff profiles', dashboardPrescriptionQueue: 'Prescription queue', dashboardAwaitingPharmacy: 'Awaiting pharmacy action',
  dashboardOutstandingBalance: 'Outstanding balance', dashboardOpenInvoices: 'Open invoices: {count}', dashboardPharmacyItems: 'Pharmacy items', dashboardCatalogueItems: 'Items in the catalogue',
  dashboardInventoryItems: 'Inventory items', dashboardTrackedItems: 'Items being tracked', dashboardAppointmentsToday: "Today's appointments", dashboardPatientDirectory: 'Patient directory',
  dashboardLaboratoryOrders: 'Laboratory orders', dashboardLaboratoryQueue: 'Lab orders', dashboardOrdersOnFile: 'Orders on file', dashboardPharmacyCatalogue: 'Pharmacy catalogue', dashboardOpenInvoicesAction: 'Open invoices', dashboardInventoryAction: 'Inventory items',
  dashboardFollowUps: 'Follow-up reminders', dashboardSignedInAs: 'Signed in as', dashboardClinicTeam: 'Clinic team', dashboardBookAppointment: 'Book appointment',
  dashboardCurrent: 'Current', dashboardRecentlyRegistered: 'Recently registered patients', dashboardLatestPatientRecords: 'Your latest patient records', dashboardViewAll: 'View all',
  dashboardPatient: 'Patient', dashboardPatientId: 'Patient ID', dashboardCondition: 'Condition', dashboardStatus: 'Status', dashboardNoPatientRecords: 'No patient records yet.', dashboardNewRegistrations: 'New registrations will appear here.',
  dashboardUpcomingVisits: 'Upcoming visits', dashboardAppointmentsGlance: 'Your next appointments at a glance', dashboardViewSchedule: 'View schedule', dashboardPatientVisit: 'Patient visit',
  dashboardClinicAppointment: 'Clinic appointment', dashboardTimePending: 'Time pending', dashboardScheduled: 'Scheduled', dashboardNoUpcomingVisits: 'No upcoming visits', dashboardVisitsAppear: 'Appointments booked by your team will show up here.',
  dashboardQuickAccess: 'Quick access', dashboardTeamWorkflow: 'Go straight to a team workflow', dashboardShortcutsAppear: 'Your workspace shortcuts will appear here.', dashboardClinicWorkspace: 'ClincyAnywhere clinic workspace',
  dashboardPrivacyWorkflows: 'Use only according to your organization’s approved privacy and clinical workflows.',
};
const dashboardHi = {
  dashboardWelcomeBack: 'वापसी पर स्वागत है', dashboardLatestFrom: 'ताज़ा जानकारी', dashboardTeamWork: 'आपकी टीम का काम, एक ही जगह पर।',
  dashboardTodayAppointments: 'आज के अपॉइंटमेंट', dashboardTotalScheduled: 'कुल {count} निर्धारित', dashboardRegisteredPatients: 'पंजीकृत मरीज़', dashboardThisClinic: 'इस क्लिनिक में',
  dashboardCareTeam: 'देखभाल टीम', dashboardStaffProfiles: 'कर्मचारी प्रोफ़ाइल', dashboardPrescriptionQueue: 'प्रिस्क्रिप्शन कतार', dashboardAwaitingPharmacy: 'फ़ार्मेसी की कार्रवाई बाकी',
  dashboardOutstandingBalance: 'बाकी भुगतान', dashboardOpenInvoices: 'खुले बिल: {count}', dashboardPharmacyItems: 'फ़ार्मेसी आइटम', dashboardCatalogueItems: 'सूची में आइटम',
  dashboardInventoryItems: 'इन्वेंटरी आइटम', dashboardTrackedItems: 'ट्रैक किए जा रहे आइटम', dashboardAppointmentsToday: 'आज के अपॉइंटमेंट', dashboardPatientDirectory: 'मरीज़ सूची',
  dashboardLaboratoryOrders: 'लैब ऑर्डर', dashboardLaboratoryQueue: 'लैब ऑर्डर', dashboardOrdersOnFile: 'दर्ज ऑर्डर', dashboardPharmacyCatalogue: 'फ़ार्मेसी सूची', dashboardOpenInvoicesAction: 'खुले बिल', dashboardInventoryAction: 'इन्वेंटरी आइटम',
  dashboardFollowUps: 'फ़ॉलो-अप रिमाइंडर', dashboardSignedInAs: 'साइन इन किया है', dashboardClinicTeam: 'क्लिनिक टीम', dashboardBookAppointment: 'अपॉइंटमेंट बुक करें',
  dashboardCurrent: 'अभी', dashboardRecentlyRegistered: 'हाल ही में पंजीकृत मरीज़', dashboardLatestPatientRecords: 'नए मरीज़ रिकॉर्ड', dashboardViewAll: 'सभी देखें',
  dashboardPatient: 'मरीज़', dashboardPatientId: 'मरीज़ आईडी', dashboardCondition: 'स्थिति', dashboardStatus: 'स्टेटस', dashboardNoPatientRecords: 'अभी कोई मरीज़ रिकॉर्ड नहीं है।', dashboardNewRegistrations: 'नए पंजीकरण यहाँ दिखेंगे।',
  dashboardUpcomingVisits: 'आगामी विज़िट', dashboardAppointmentsGlance: 'आगे के अपॉइंटमेंट एक नज़र में', dashboardViewSchedule: 'शेड्यूल देखें', dashboardPatientVisit: 'मरीज़ की विज़िट',
  dashboardClinicAppointment: 'क्लिनिक अपॉइंटमेंट', dashboardTimePending: 'समय तय होना बाकी', dashboardScheduled: 'निर्धारित', dashboardNoUpcomingVisits: 'कोई आगामी विज़िट नहीं', dashboardVisitsAppear: 'आपकी टीम के बुक किए अपॉइंटमेंट यहाँ दिखेंगे।',
  dashboardQuickAccess: 'त्वरित पहुँच', dashboardTeamWorkflow: 'टीम के काम पर सीधे जाएँ', dashboardShortcutsAppear: 'वर्कस्पेस के शॉर्टकट यहाँ दिखेंगे।', dashboardClinicWorkspace: 'ClincyAnywhere क्लिनिक वर्कस्पेस',
  dashboardPrivacyWorkflows: 'इसे अपने संगठन की स्वीकृत गोपनीयता और क्लिनिकल प्रक्रियाओं के अनुसार ही उपयोग करें।',
};
const dashboardMr = {
  dashboardWelcomeBack: 'पुन्हा स्वागत आहे', dashboardLatestFrom: 'ताज्या घडामोडी', dashboardTeamWork: 'तुमच्या टीमचे काम, एकाच ठिकाणी.',
  dashboardTodayAppointments: 'आजचे अपॉइंटमेंट', dashboardTotalScheduled: 'एकूण {count} नियोजित', dashboardRegisteredPatients: 'नोंदणीकृत रुग्ण', dashboardThisClinic: 'या क्लिनिकमध्ये',
  dashboardCareTeam: 'काळजी टीम', dashboardStaffProfiles: 'कर्मचारी प्रोफाइल', dashboardPrescriptionQueue: 'प्रिस्क्रिप्शन रांग', dashboardAwaitingPharmacy: 'फार्मसीच्या कृतीची प्रतीक्षा',
  dashboardOutstandingBalance: 'बाकी देय रक्कम', dashboardOpenInvoices: 'बाकी बिले: {count}', dashboardPharmacyItems: 'फार्मसीमधील वस्तू', dashboardCatalogueItems: 'यादीतील वस्तू',
  dashboardInventoryItems: 'इन्व्हेंटरी वस्तू', dashboardTrackedItems: 'नोंद ठेवलेल्या वस्तू', dashboardAppointmentsToday: 'आजचे अपॉइंटमेंट', dashboardPatientDirectory: 'रुग्ण यादी',
  dashboardLaboratoryOrders: 'प्रयोगशाळा ऑर्डर', dashboardLaboratoryQueue: 'लॅब ऑर्डर', dashboardOrdersOnFile: 'नोंदवलेल्या ऑर्डर', dashboardPharmacyCatalogue: 'फार्मसी यादी', dashboardOpenInvoicesAction: 'बाकी बिले', dashboardInventoryAction: 'इन्व्हेंटरी वस्तू',
  dashboardFollowUps: 'फॉलो-अप स्मरणपत्रे', dashboardSignedInAs: 'साइन इन केले आहे', dashboardClinicTeam: 'क्लिनिक टीम', dashboardBookAppointment: 'अपॉइंटमेंट बुक करा',
  dashboardCurrent: 'सध्याचे', dashboardRecentlyRegistered: 'अलीकडे नोंदलेले रुग्ण', dashboardLatestPatientRecords: 'नवीन रुग्ण नोंदी', dashboardViewAll: 'सर्व पहा',
  dashboardPatient: 'रुग्ण', dashboardPatientId: 'रुग्ण आयडी', dashboardCondition: 'स्थिती', dashboardStatus: 'स्टेटस', dashboardNoPatientRecords: 'अद्याप रुग्ण नोंदी नाहीत.', dashboardNewRegistrations: 'नवीन नोंदी येथे दिसतील.',
  dashboardUpcomingVisits: 'आगामी भेटी', dashboardAppointmentsGlance: 'पुढील अपॉइंटमेंट एका नजरेत', dashboardViewSchedule: 'वेळापत्रक पहा', dashboardPatientVisit: 'रुग्ण भेट',
  dashboardClinicAppointment: 'क्लिनिक अपॉइंटमेंट', dashboardTimePending: 'वेळ ठरायची आहे', dashboardScheduled: 'नियोजित', dashboardNoUpcomingVisits: 'आगामी भेटी नाहीत', dashboardVisitsAppear: 'तुमच्या टीमने बुक केलेल्या भेटी येथे दिसतील.',
  dashboardQuickAccess: 'जलद प्रवेश', dashboardTeamWorkflow: 'टीमच्या कामाकडे थेट जा', dashboardShortcutsAppear: 'वर्कस्पेसचे शॉर्टकट येथे दिसतील.', dashboardClinicWorkspace: 'ClincyAnywhere क्लिनिक वर्कस्पेस',
  dashboardPrivacyWorkflows: 'तुमच्या संस्थेच्या मान्य गोपनीयता आणि क्लिनिकल प्रक्रियांनुसारच वापरा.',
};
const dictionaries = {
  en: { ...en, ...dashboardEn },
  hi: { ...hi, ...dashboardEn, ...dashboardHi },
  mr: { ...mr, ...dashboardEn, ...dashboardMr },
};
const savedLanguage = typeof localStorage === 'undefined' ? null : localStorage.getItem('clinic-language');
let language = Object.hasOwn(dictionaries, savedLanguage) ? savedLanguage : 'en';
const listeners = new Set();

function subscribe(listener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

function snapshot() {
  return language;
}

export function setLanguage(next) {
  language = Object.hasOwn(dictionaries, next) ? next : 'en';
  if (typeof localStorage !== 'undefined') localStorage.setItem('clinic-language', language);
  listeners.forEach(listener => listener());
}

export function useLanguage() {
  const lang = useSyncExternalStore(subscribe, snapshot);
  return {
    lang,
    t: (key, values) => {
      const value = dictionaries[lang][key] ?? en[key] ?? key;
      return values ? value.replace(/\{(\w+)\}/g, (match, name) => String(values[name] ?? match)) : value;
    },
    setLanguage,
  };
}
