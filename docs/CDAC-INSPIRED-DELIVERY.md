# CDAC inspired clinic workflows

This app borrows product ideas from the supplied e-Sushrut clinic page. It is an independent implementation; the CDAC product, branding and source are not copied.

## Available in this application

- Patient recovery queue for overdue follow-ups and missed appointments, with contact outcome and return tracking.
- Marathi, Hindi and English interface choices, voice command draft review, keyboard accessible controls, and per-device accessibility preferences.
- Clinic settings for name, phone, timezone and accent color; billing and clinic activity reports already live in the application.
- Pharmacy and prescription workflows with stock and dispensing status.
- Patient support tickets for patients and clinic staff.
- Persistent consent evidence for patient messaging; notification attempts are recorded with provider status.
- Digital health workspace for clinic facility and professional profile references, consent requests, local EHR JSON, FHIR R4 draft and HL7 v2 draft export.
- Clinician pre-visit checklist for allergy, identity, medicines, follow-up and escalation review.

## Integrations that need external onboarding

- ABHA creation/verification, ABDM M1–M4 transactions and live consent callbacks require ABDM sandbox/production enrollment, approved identifiers, credentials, encryption/key management, callback signature validation and conformance review. The current workspace records local readiness and consent requests; it does not claim official ABHA/HPR/HFR verification or production ABDM exchange.
- HPR/HFR IDs are stored as externally issued references. Their verification status is a staff attestation, not an automated registry lookup.
- FHIR and HL7 exports are draft mappings for review. They are not certified conformance packages and should be validated against the receiving system and applicable MDDS profile before exchange.
- SMS/email send only when an HTTP notification provider is configured. The provider adapter expects `POST /messages`, Bearer authentication and JSON `{channel,to,message,from}`, returning a message `id`. `ACCEPTED` means the provider accepted the request, not that the recipient received it. WhatsApp also needs a configured provider.
- Actual data backup, recovery testing, contractual 24x7 support and production privacy/security operations depend on the clinic's deployment and support arrangements.

## e-Sushrut@Clinic reference scope and current gap

The public feature map in the marketing site and Digital Health workspace separates local workflows from government-network integrations. This product is independently developed and must not be presented as e-Sushrut, C-DAC or government-approved.

| Reference capability | Current ClincyAnywhere state |
| --- | --- |
| Patient registration, OPD appointments, prescriptions, billing and reports | Local clinic workflows exist; validate workflow depth with clinics before claiming feature parity. |
| Pharmacy, stock, follow-up recovery, multilingual UI and speech-to-text draft review | Local workflows exist. Speech recognition depends on browser support and requires staff review. |
| HFR/HPR | IDs and status are local references/attestations; no registry lookup or verification call exists. |
| Find/create ABHA | Not integrated. Never collect Aadhaar/OTP in this app; use the official ABDM identity flow. |
| Scan & Share | Not integrated; facility registration and official ABDM APIs are prerequisites. |
| ABDM consent-based health information exchange | Consent requests are stored locally; signed callbacks and exchange APIs are not configured. |
| AIIMS hypertension/diabetes CDSS | Not integrated. The local pre-visit checklist is not clinical decision support. |
| Telemedicine | Appointments can store an HTTPS meeting link from a separately selected provider. The app does not host calls, issue patient links, or provide a consent workflow; validate the provider and clinic process before patient use. |

Do not move a capability into the “integrated” state until the relevant ABDM sandbox approval, facility and professional onboarding, official API credentials, callback verification, security review and conformance evidence have been completed. Current public feature statuses intentionally make this distinction visible.

## Notification configuration

Set `APP_NOTIFICATIONS_PROVIDER_ENABLED=true`, `APP_NOTIFICATIONS_ENDPOINT`, `APP_NOTIFICATIONS_API_TOKEN`, `APP_NOTIFICATIONS_SMS_SENDER` and `APP_NOTIFICATIONS_EMAIL_SENDER` on the backend. Keep credentials in a secret manager or protected deployment environment. The provider must implement the contract above. Patient messages require a stored active consent event with an evidence reference for the selected channel; a later revoke event blocks further sends.
