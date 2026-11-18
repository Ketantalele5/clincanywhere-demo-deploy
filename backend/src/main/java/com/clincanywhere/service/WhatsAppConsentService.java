package com.clincanywhere.service;

import com.clincanywhere.entity.WhatsAppConsentEvent;
import com.clincanywhere.repository.WhatsAppConsentEventRepository;
import com.clincanywhere.repository.WhatsAppOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WhatsAppConsentService {
    private final WhatsAppConsentEventRepository events;
    private final WhatsAppOutboxRepository outbox;
    private final FieldEncryptionService encryption;
    public WhatsAppConsentService(WhatsAppConsentEventRepository events,WhatsAppOutboxRepository outbox,FieldEncryptionService encryption) {
        this.events=events; this.outbox=outbox; this.encryption=encryption;
    }
    @Transactional
    public void grant(String clinic,String phone,String evidenceReference,String actor) {
        events.save(new WhatsAppConsentEvent(clinic,encryption.fingerprint(normalize(phone)),"GRANTED",evidenceReference,actor));
    }
    @Transactional
    public void revoke(String clinic,String phone,String source,String actor) {
        String fingerprint=encryption.fingerprint(normalize(phone));
        events.save(new WhatsAppConsentEvent(clinic,fingerprint,"REVOKED",source,actor));
        outbox.suppressPending(clinic,fingerprint);
    }
    @Transactional(readOnly=true)
    public boolean isActive(String clinic,String phone) {
        return events.findTopByClinicIdAndPhoneFingerprintOrderByIdDesc(clinic,encryption.fingerprint(normalize(phone)))
                .map(event -> "GRANTED".equals(event.getAction())).orElse(false);
    }
    private String normalize(String phone) { return phone == null ? "" : phone.trim(); }
}
