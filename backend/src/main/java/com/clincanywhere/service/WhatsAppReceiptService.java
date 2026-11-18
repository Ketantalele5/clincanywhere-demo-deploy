package com.clincanywhere.service;

import com.clincanywhere.entity.WhatsAppDeliveryEvent;
import com.clincanywhere.repository.WhatsAppDeliveryEventRepository;
import com.clincanywhere.repository.WhatsAppOutboxRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

/** Persists idempotent provider status receipts without retaining full webhook payloads. */
@Service
public class WhatsAppReceiptService {
    private final WhatsAppDeliveryEventRepository events;
    private final WhatsAppOutboxRepository outbox;
    public WhatsAppReceiptService(WhatsAppDeliveryEventRepository events, WhatsAppOutboxRepository outbox) {
        this.events=events; this.outbox=outbox;
    }
    @Transactional
    public int accept(JsonNode payload,String expectedPhoneNumberId) {
        if (payload == null || !payload.isObject() || expectedPhoneNumberId == null || expectedPhoneNumberId.isBlank()) return 0;
        int saved=0;
        for (JsonNode entry : payload.path("entry")) {
            for (JsonNode change : entry.path("changes")) {
                if (!expectedPhoneNumberId.equals(change.path("value").path("metadata").path("phone_number_id").asText())) continue;
                for (JsonNode receipt : change.path("value").path("statuses")) {
                    String id=text(receipt,"id"), status=text(receipt,"status"), timestamp=text(receipt,"timestamp");
                    if (id == null || status == null || timestamp == null) continue;
                    if (!java.util.Set.of("sent","delivered","read","failed").contains(status)) continue;
                    final Instant occurredAt;
                    try { occurredAt=Instant.ofEpochSecond(Long.parseLong(timestamp)); }
                    catch (NumberFormatException | java.time.DateTimeException invalid) { continue; }
                    String errorCode=receipt.path("errors").path(0).path("code").isNumber()
                            ? receipt.path("errors").path(0).path("code").asText() : null;
                    if (events.existsByProviderMessageIdAndStatusAndOccurredAt(id,status,occurredAt)) continue;
                    WhatsAppDeliveryEvent event=events.save(new WhatsAppDeliveryEvent(id,status,occurredAt,errorCode));
                    apply(event);
                    saved++;
                }
            }
        }
        return saved;
    }
    @Scheduled(fixedDelayString="${WHATSAPP_RECEIPT_RECONCILE_MS:30000}")
    @Transactional
    public void reconcilePending() {
        for (WhatsAppDeliveryEvent event : events.findTop100ByProcessedAtIsNullOrderByIdAsc()) apply(event);
    }
    private void apply(WhatsAppDeliveryEvent event) {
        var item=outbox.findByProviderMessageId(event.getProviderMessageId());
        if (item.isPresent()) {
            item.ifPresent(outboxItem -> outboxItem.applyProviderStatus(event.getStatus(), event.getErrorCode()));
            event.markProcessed();
        } else if (event.getReceivedAt() != null && event.getReceivedAt().isBefore(Instant.now().minusSeconds(600))) {
            // Keep unmatched provider callbacks briefly to cover send/receipt transaction races.
            event.markProcessed();
        }
    }
    private String text(JsonNode node, String field) {
        JsonNode value=node.path(field);
        return value.isTextual() ? value.asText("") : "";
    }
}
