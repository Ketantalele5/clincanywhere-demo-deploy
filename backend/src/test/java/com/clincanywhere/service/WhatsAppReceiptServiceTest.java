package com.clincanywhere.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

import com.clincanywhere.entity.WhatsAppOutboxItem;
import com.clincanywhere.repository.WhatsAppDeliveryEventRepository;
import com.clincanywhere.repository.WhatsAppOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@SuppressWarnings("null") // Mockito-generated doubles and matchers are passed to non-null APIs.
class WhatsAppReceiptServiceTest {
    @Test void persistsReceiptAndAdvancesOutboxIdempotently() throws Exception {
        var events=mock(WhatsAppDeliveryEventRepository.class);
        var outbox=mock(WhatsAppOutboxRepository.class);
        var item=new WhatsAppOutboxItem("clinic-a","doctor","enc:v1:phone","enc:v1:body");
        item.sent("wamid.123");
        when(events.existsByProviderMessageIdAndStatusAndOccurredAt(anyString(),anyString(),any())).thenReturn(false);
        when(events.save(any())).thenAnswer(call -> call.getArgument(0));
        when(outbox.findByProviderMessageId("wamid.123")).thenReturn(Optional.of(item));
        var payload=new ObjectMapper().readTree("""
                {"entry":[{"changes":[{"value":{"metadata":{"phone_number_id":"sender-1"},"statuses":[{"id":"wamid.123","status":"delivered","timestamp":"1790000000"}]}}]}]}
                """);

        int accepted=new WhatsAppReceiptService(events,outbox).accept(payload,"sender-1");

        assertEquals(1,accepted);
        assertEquals(WhatsAppOutboxItem.Status.DELIVERED,item.getStatus());
        item.sent("wamid.123");
        assertEquals(WhatsAppOutboxItem.Status.DELIVERED,item.getStatus(),
                "A late send response must not downgrade a delivery receipt already applied");
        verify(events).save(any());
        verify(outbox).findByProviderMessageId("wamid.123");
    }
}
