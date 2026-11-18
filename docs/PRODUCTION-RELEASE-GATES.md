# Production release gates

Complete this record for each deployment that may contain real patient data. A checked box needs a link to evidence and an accountable owner; repository code is not evidence of deployed controls.

| Gate | Required evidence | Owner | Status / evidence link |
|---|---|---|---|
| TLS and network isolation | TLS endpoint, ingress policy, private DB/management ports, trusted proxy configuration | Platform | |
| Secrets and key recovery | Secret manager references; encryption-key recovery test; rotation plan | Security | |
| Database protection | Encrypted off-host backups, alerting, retention policy, successful isolated restore with achieved RPO/RTO | SRE | |
| Audit operations | Access to audit stream restricted; approved retention configured; encrypted monthly archive delivered to Object Lock COMPLIANCE storage and recovery tested; review process established | Security / Privacy | |
| Abuse controls | Effective limits verified at every public ingress, including multi-replica behavior and trusted client IP parsing | Platform | |
| Realtime scale | Managed HA STOMP broker configured; SSE sticky routing or shared fan-out validated under failover | Platform | |
| Patient identity | Identity proofing method and exception handling approved | Clinical operations | |
| Consent and messaging | Consent evidence references trace to independently captured proof; verified inbound opt-out and suppression behavior and provider receipt reconciliation tested; signed webhook verified | Privacy / Product | |
| Clinical safety | Intended use, hazard review, clinical validation protocol/results, rollback and incident process approved | Clinical safety lead | |
| Privacy and regulation | Jurisdiction-specific privacy/security assessment and required contracts/notifications completed | Privacy / Legal | |
| Governance | Named accountable approvers and signed production decision | Executive / Clinical governance | |

**Release decision:** ☐ approved ☐ rejected  
**Application version / schema version:**  
**Approvers and dates:**  
**Exceptions with expiry and owner:**

Do not enable real-patient data or WhatsApp delivery while required gates are incomplete. Never put patient data in the evidence links or this checklist.
