package com.clincanywhere.service;

import com.clincanywhere.entity.WhatsAppOutboxItem;
import com.clincanywhere.repository.WhatsAppOutboxRepository;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WhatsAppOutboxService {
    private final WhatsAppOutboxRepository outbox;
    private final WhatsAppClient client;
    private final org.springframework.transaction.support.TransactionTemplate transactions;
    private final java.util.concurrent.Executor whatsappExecutor;
    private final NotificationService notifications;
    private final FieldEncryptionService encryption;
    private final WhatsAppConsentService consent;
    public WhatsAppOutboxService(WhatsAppOutboxRepository outbox, WhatsAppClient client,
                                 org.springframework.transaction.PlatformTransactionManager transactionManager,
                                 @org.springframework.beans.factory.annotation.Qualifier("whatsappTaskExecutor") java.util.concurrent.Executor whatsappExecutor,
                                 NotificationService notifications, FieldEncryptionService encryption, WhatsAppConsentService consent) {
        this.outbox = outbox; this.client = client;
        this.transactions = new org.springframework.transaction.support.TransactionTemplate(java.util.Objects.requireNonNull(transactionManager));
        this.whatsappExecutor = whatsappExecutor;
        this.notifications = notifications;
        this.encryption = encryption;
        this.consent = consent;
    }
    @Transactional
    public WhatsAppOutboxItem enqueue(String clinic, String username, String phone, String message) {
        return outbox.save(new WhatsAppOutboxItem(clinic, username, encryption.encrypt(phone), encryption.encrypt(message), encryption.fingerprint(phone.trim())));
    }
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.List<java.util.Map<String, Object>> list(String clinic, String username, boolean admin) {
        return (admin ? outbox.findByClinicId(clinic) : outbox.findByClinicIdAndRequestedBy(clinic, username))
                .stream().map(item -> java.util.Map.<String, Object>of("id", requireId(item), "status", item.getStatus().name(),
                        "attempts", item.getAttempts(), "createdAt", item.getCreatedAt())).toList();
    }
    @Scheduled(fixedDelayString = "${WHATSAPP_OUTBOX_POLL_MS:5000}")
    public void dispatch() {
        for (Object value : claimBatch()) {
            Long id = java.util.Objects.requireNonNull((Long) value);
            whatsappExecutor.execute(() -> deliver(id));
        }
    }
    protected java.util.List<Long> claimBatch() {
        return transactions.execute(status -> {
            var batch = outbox.lockReady(Instant.now(), WhatsAppOutboxItem.Status.PENDING,
                    WhatsAppOutboxItem.Status.PROCESSING, org.springframework.data.domain.PageRequest.of(0, 20));
            batch.forEach(item -> item.claim());
            return batch.stream().map(item -> requireId(item)).toList();
        });
    }
    public void deliver(Long id) {
        Delivery delivery = transactions.execute(status -> outbox.findById(id)
                .filter(item -> item.getStatus() == WhatsAppOutboxItem.Status.PROCESSING)
                .map(item -> new Delivery(requireId(item),item.getClinicId(), item.getRequestedBy(), encryption.decrypt(item.getPhoneNumber()), encryption.decrypt(item.getMessage()))).orElse(null));
        if (delivery == null) return;
        if (!consent.isActive(delivery.clinicId(),delivery.phoneNumber())) {
            transactions.executeWithoutResult(status -> outbox.findById(id).ifPresent(item -> item.suppressOptOut()));
            return;
        }
        String errorType = null;
        String providerMessageId = null;
        try { providerMessageId = client.sendText(delivery.phoneNumber(), delivery.message()); }
        catch (RuntimeException failure) {
            errorType = failure.getClass().getSimpleName();
            org.slf4j.LoggerFactory.getLogger(WhatsAppOutboxService.class)
                    .warn("WhatsApp delivery failed exceptionType={}", errorType);
        }
        String deliveryErrorType = errorType;
        String providerId = providerMessageId;
        boolean acceptedByProvider = deliveryErrorType == null;
        transactions.executeWithoutResult(status -> outbox.findById(id).ifPresent(item -> {
            if (item.getStatus() != WhatsAppOutboxItem.Status.PROCESSING) return;
            if (acceptedByProvider) item.sent(providerId);
            else item.failed("WhatsApp provider request failed (" + deliveryErrorType + ")");
        }));
        notifications.publish(delivery.clinicId(), delivery.requestedBy(), acceptedByProvider
                ? "WhatsApp message accepted by the provider"
                : "WhatsApp delivery attempt failed; check the outbox for the current status");
    }
    private record Delivery(Long id,String clinicId, String requestedBy, String phoneNumber, String message) { }
    private static Long requireId(WhatsAppOutboxItem item) {
        Long id = item.getId();
        if (id == null) throw new IllegalStateException("Persisted WhatsApp outbox item has no identifier");
        return id;
    }
}
