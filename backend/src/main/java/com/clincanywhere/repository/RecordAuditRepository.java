package com.clincanywhere.repository;
import com.clincanywhere.entity.RecordAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RecordAuditRepository extends JpaRepository<RecordAuditEvent, Long> {
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = "insert into record_audit_events (clinic_id, record_category, record_external_id, actor, action, correlation_id, created_at) values (:clinic, :category, '*', :actor, 'LIST', :correlation, current_timestamp)", nativeQuery = true)
    int recordListAccess(@org.springframework.data.repository.query.Param("clinic") String clinic,
                         @org.springframework.data.repository.query.Param("category") String category,
                         @org.springframework.data.repository.query.Param("actor") String actor,
                         @org.springframework.data.repository.query.Param("correlation") @org.jspecify.annotations.Nullable String correlation);
}
