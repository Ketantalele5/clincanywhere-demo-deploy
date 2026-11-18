package com.clincanywhere.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.clincanywhere.entity.WhatsAppConsentEvent;
import com.clincanywhere.repository.WhatsAppConsentEventRepository;
import com.clincanywhere.repository.WhatsAppOutboxRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@SuppressWarnings("null") // Mockito-generated doubles are passed to non-null APIs.
class WhatsAppConsentServiceTest {
    @Test void recordsConsentTransitionsAndCancelsQueuedMessagesOnOptOut() {
        var events=mock(WhatsAppConsentEventRepository.class);
        var outbox=mock(WhatsAppOutboxRepository.class);
        var consent=new WhatsAppConsentService(events,outbox,new FieldEncryptionService("","test"));
        when(events.findTopByClinicIdAndPhoneFingerprintOrderByIdDesc(anyString(),anyString()))
                .thenReturn(Optional.of(new WhatsAppConsentEvent("clinic-a","fingerprint","GRANTED","consent-123","doctor")));

        assertTrue(consent.isActive("clinic-a","+14155550100"));
        consent.revoke("clinic-a","+14155550100","META_INBOUND_OPT_OUT","provider-webhook");

        verify(events).save(argThat(event -> event.getAction().equals("REVOKED")));
        verify(outbox).suppressPending(eq("clinic-a"),anyString());
    }
}
