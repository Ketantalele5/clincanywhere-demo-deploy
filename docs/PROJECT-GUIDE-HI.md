# ClincyAnywhere ko samajhne ki guide

Yeh guide project ko bina code padhe samajhne ke liye hai: project kis kaam ka hai, har role kya karta hai, aur basic workflow kaise chalta hai.
## 0. Local demo jaldi se kaise chalayein

### Zaroori tools

- Java 17 ya usse naya version
- Maven 3.9 ya usse naya version
- Node.js 20.19+ ya 22.12+

### API aur frontend start karein

Project ke root folder se do terminals kholein. Pehle terminal mein demo API aur local H2 database start karein:

```powershell
cd backend
run-demo.cmd
```

Doosre terminal mein React frontend start karein:

```powershell
cd frontend
npm install
npm run dev
```

Terminal mein dikhaya gaya Vite URL browser mein kholein. Frontend `/api` requests ko local API `http://127.0.0.1:8080` par bhejta hai. Demo login usernames aur password section 7 mein diye hain. Yeh accounts aur data sirf local development/test ke liye hain; real patient data ke liye nahi.

API ya frontend band karne ke liye us terminal mein `Ctrl+C` dabayein. Setup ke aur options aur architecture ke liye root ka `README.md` dekhein.

## 1. Project kya hai?

ClincyAnywhere ek multi-clinic operations app hai. Iska matlab alag clinics apna workspace use kar sakte hain. Clinic staff patients, appointments, clinical records, lab, pharmacy, billing aur inventory jaise kaam ek app mein sambhalte hain. Alag platform team naye clinics ko onboard karti hai.

### Technology

- **Frontend:** React + Vite. Browser mein dikhne wale pages aur forms.
- **Backend:** Java + Spring Boot REST API. Login, role permissions, validation aur business operations.
- **Database:** H2 local development aur automated tests ke liye; production profile PostgreSQL ke liye bana hai.
- **Authentication:** Username/password login ke baad short-lived JWT; protected API requests mein token bheja jata hai.

> **Development aur production ka farq:** Local H2 aur demo accounts development ke liye hain. Production mein PostgreSQL, secret management, backups, monitoring, privacy/security controls aur clinic ki formal approval zaroori hain. Is repo ko healthcare production certified na samjhein.

## 2. Sabse pehle do tarah ke accounts samjhein

| Group | Kaam | Access kahan tak |
|---|---|---|
| **Clinic team** | Ek clinic ko chalana aur patients ko care dena | Sirf un clinics ka data jahan account member hai; role ke hisaab se modules |
| **Platform team** | Naye clinics ko onboard karna; Platform Owner ko global clinic admin access bhi hai | Platform Owner: sabhi registered clinics. Onboarding Teammate: assigned/created leads only. |

**Administrator** aur **Platform Owner** alag hain. Administrator apne hospital ko manage karta hai. Platform Owner global administrator hai: woh kisi bhi registered hospital ko select karke uske patient records aur clinic admin features chala sakta hai. Onboarding Teammate sirf assigned/created clinic leads manage karta hai; patient/staff data nahi dekh sakta.

## 3. Role-wise kaun kya karta hai?

### Clinic team — 8 roles

| Role | Asaan bhasha mein zimmedari | Kya add/update kar sakta hai? |
|---|---|---|
| **Administrator** | Clinic workspace ka main operator. Settings aur clinic ki team sambhalta hai; operational modules par broad control. | Clinic ke sabhi record modules aur settings. Staff accounts provision kar sakta hai. Sirf apne clinic scope mein. |
| **Receptionist / Front Desk** | Patient ka registration, appointment booking aur front-desk coordination. | Patients, appointments, billing, follow-ups aur instant reminders. Doctors/staff list dekh sakta hai, par staff records manage nahi karta. |
| **Doctor** | Patient ki clinical care aur treatment workflow. | Patients, appointments, medical records, prescriptions, lab orders, follow-ups aur instant reminders. |
| **Laboratory Technician** | Ordered tests aur lab/medical record workflow update karna. | Laboratory orders aur medical records. Clinical prescription/diagnosis ka role nahi. |
| **Pharmacist** | Pharmacy items/stock workflow sambhalna aur prescriptions dekhna. | Pharmacy records/stock. Prescriptions dekh sakta hai; prescription create/approve karna Doctor ka role hai. |
| **Billing Executive** | Invoice aur payment status sambhalna; reports dekhna. | Billing/invoice records. Patients aur appointments bhi dekh sakta hai. |
| **Inventory Manager** | Supplies, medicine stock aur reorder levels dekhna/sambhalna. | General inventory aur pharmacy stock records. Reports dekh sakta hai. |
| **Patient** | Apni care information dekhna aur clinic team se message karna. | Sirf apna linked patient record padh sakta hai; doosre patients ko nahi dekh sakta. Account clinic invitation se banta hai. |

### Platform team — 2 roles

| Role | Asaan bhasha mein zimmedari | Access ki seema |
|---|---|---|
| **Platform Owner** | Sabhi registered hospitals aur unke patient/staff workflows par global admin access. Clinic leads, onboarding teammates, aur workspace conversion bhi manage karta hai. | All-hospital administration. |
| **Onboarding Teammate** | Apne ya assigned clinic prospects se contact karta hai aur lead ka status/follow-up update karta hai. | Sirf apne banaye/assigned clinic leads. Team management aur patient/clinic data nahi. |

## 4. Kaam dekhkar role chunein

| Kaam | Role |
|---|---|
| Naya patient register karna ya appointment book karna | Receptionist |
| Diagnosis/clinical note, prescription ya lab order | Doctor |
| Lab order/test status update karna | Laboratory Technician |
| Pharmacy medicines/stock update | Pharmacist |
| General clinic supplies aur reorder level update | Inventory Manager |
| Invoice ya payment entry update | Billing Executive |
| Clinic settings, clinic staff accounts aur clinic-wide operations | Administrator |
| Sirf apni patient information dekhna | Patient |
| Naye clinic prospect ko contact/follow-up karna | Onboarding Teammate |
| Onboarding team banana, leads assign karna, workspace provision karna | Platform Owner |

## 5. Project ke main features

- **Dashboard:** Clinic ke daily operations ka summary.
- **Patients:** Patient profiles aur clinic patient list.
- **Appointments:** Patient/doctor ke saath appointment scheduling/status.
- **Doctors & staff:** Clinic ke clinicians/staff directory.
- **Medical records:** Patient ke clinical record aur notes.
- **Prescriptions:** Doctor ke prescriptions aur pharmacy status workflow.
- **Laboratory:** Test orders aur result/status workflow.
- **Follow-ups & reminders:** Follow-up plan aur reminder tasks.
- **Pharmacy & inventory:** Medicine items, stock quantity, reorder level aur expiry; supplies ka inventory.
- **Billing & reports:** Invoices, received payment, outstanding balance aur summary reports.
- **Beds & wards:** Bed/ward availability records (clinic admin access).
- **Settings/team:** Clinic profile; admin ke zariye clinic staff accounts.
- **Care team messages:** Clinic ke authorized users ke darmiyan private messages.
- **Notifications:** Clinic-scoped recent notifications aur live updates.
- **Clinic onboarding:** Website enquiries/prospects ko pipeline mein manage karna aur workspace provision karna.
- **WhatsApp integration:** Configuration aur consent ke baad outbound message queue; in-app chat se alag feature.

## 6. Do basic workflows

### Clinic ke andar patient care

1. Receptionist patient register karta hai aur appointment book karta hai.
2. Doctor patient dekhta hai, clinical record banata hai, zarurat par prescription ya lab order likhta hai.
3. Lab Technician test workflow update karta hai; Pharmacist prescription dekhkar pharmacy stock/dispensing workflow sambhalta hai.
4. Billing Executive invoice/payment sambhalta hai; Receptionist ya Doctor follow-up schedule kar sakte hain.
5. Patient invitation se apna account activate karke apna linked record dekh sakta hai.

### Naya clinic ClincyAnywhere par lana

1. Website enquiry ya outreach prospect lead pipeline mein aata hai.
2. Platform Owner lead ko Onboarding Teammate ko assign kar sakta hai.
3. Teammate contact karke lead status update karta hai: New → Contacted → Qualified (ya Lost).
4. Clinic ready ho to authorized onboarding user workspace ID aur pehla clinic admin login create karke lead convert karta hai.
5. Naya Clinic Administrator alag se sign in karke apne clinic ka staff aur operations manage karta hai.

## 7. Demo login — sirf local development/test

Demo password sab listed demo accounts ka `demo123` hai. **Production mein yeh accounts/password use nahi hote.**

| Username | Role |
|---|---|
| `admin` | Administrator |
| `reception` | Receptionist |
| `doctor` | Doctor |
| `labtech` | Laboratory Technician |
| `pharmacy` | Pharmacist |
| `billing` | Billing Executive |
| `store` | Inventory Manager |
| `owner` | Platform Owner |
| `onboarder` | Onboarding Teammate |

Patient account demo se seed nahi hota; clinic se one-time invitation lena hota hai. Local demo data/accounts test fixtures hain, real patient information ke liye nahi.

## 8. Code kahan milega?

- Frontend ka entry flow, folder map, page add karne ke steps, import direction aur current boundaries `frontend/README.md` mein hain; naye frontend developer ko pehle yeh guide padhni chahiye.
- `frontend/src/main.jsx` React app start karta hai; `frontend/src/app/ClincyAnywhere.jsx` current app shell, navigation aur shared page/data flow sambhalta hai.
- `frontend/src/features/` — user-facing feature folders: `auth`, `dashboard`, `marketing`, `messaging`, `platform`, `records`, `reports`, `settings`.
- `frontend/src/components/ui/` — reusable visual controls; `frontend/src/components/shared/` — feature ke beech reuse hone wale domain display components.
- `frontend/src/hooks/` — reusable React behavior, filhaal realtime messaging/notifications; `frontend/src/lib/` — API client, app configuration, translations, formatting aur CSV/JSON download helper.
- `frontend/src/styles.css` — global aur feature styling; `frontend/e2e/` — Playwright browser tests.
- `backend/src/main/java/com/clincanywhere/controller/` — REST API endpoints.
- `backend/src/main/java/com/clincanywhere/service/` — business logic.
- `backend/src/main/java/com/clincanywhere/security/` — role/clinic scope checks.
- `backend/src/main/resources/db/changelog/` — Liquibase database schema migrations.
- `backend/src/test/` aur `frontend/e2e/` — backend aur browser tests.

## 9. Yaad rakhne layak baatein

- Frontend mein koi page dikhna apne aap edit permission nahi deta; backend role aur clinic membership verify karta hai.
- Ek user ko alag-alag accounts share na karayein. Har staff member ka apna account hona chahiye.
- H2 local development/test database hai; production ke liye supported PostgreSQL profile diya gaya hai.
- Real patient data se pehle project ke `ARCHITECTURE.md` mein diye privacy, security, audit, backup/restore, monitoring, clinical safety aur governance controls ko complete aur review karna zaroori hai.

---

**Yeh guide current frontend role setup aur Spring Boot authorization code se tayyar ki gayi hai.** Iska maqsad app samjhana hai; yeh medical advice, compliance certification, ya production-readiness approval nahi hai.

