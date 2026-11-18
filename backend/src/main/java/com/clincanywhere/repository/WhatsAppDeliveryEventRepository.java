package com.clincanywhere.repository;
import com.clincanywhere.entity.WhatsAppDeliveryEvent;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
public interface WhatsAppDeliveryEventRepository extends JpaRepository<WhatsAppDeliveryEvent, Long> {
    boolean existsByProviderMessageIdAndStatusAndOccurredAt(String providerMessageId, String status, Instant occurredAt);
    java.util.List<WhatsAppDeliveryEvent> findTop100ByProcessedAtIsNullOrderByIdAsc();
}
