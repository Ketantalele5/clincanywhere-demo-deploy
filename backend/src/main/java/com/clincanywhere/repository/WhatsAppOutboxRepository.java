package com.clincanywhere.repository;

import com.clincanywhere.entity.WhatsAppOutboxItem;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import jakarta.persistence.QueryHint;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

public interface WhatsAppOutboxRepository extends JpaRepository<WhatsAppOutboxItem, Long> {
    java.util.Optional<WhatsAppOutboxItem> findByProviderMessageId(String providerMessageId);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update WhatsAppOutboxItem o set o.status=com.clincanywhere.entity.WhatsAppOutboxItem.Status.FAILED, o.lastError='Suppressed after patient opt-out' where o.clinicId=:clinic and o.phoneFingerprint=:fingerprint and o.status=com.clincanywhere.entity.WhatsAppOutboxItem.Status.PENDING")
    int suppressPending(@Param("clinic") String clinic,@Param("fingerprint") String fingerprint);
    java.util.List<WhatsAppOutboxItem> findByClinicId(String clinicId);
    java.util.List<WhatsAppOutboxItem> findByClinicIdAndRequestedBy(String clinicId, String username);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select o from WhatsAppOutboxItem o where (o.status = :pending or (o.status = :processing and o.nextAttemptAt <= :now)) and o.nextAttemptAt <= :now order by o.id")
    List<WhatsAppOutboxItem> lockReady(@Param("now") Instant now,
                                       @Param("pending") WhatsAppOutboxItem.Status pending,
                                       @Param("processing") WhatsAppOutboxItem.Status processing, Pageable pageable);
}
