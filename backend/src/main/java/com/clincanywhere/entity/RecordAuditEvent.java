package com.clincanywhere.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "record_audit_events", indexes = @Index(name = "ix_record_audit_scope_time", columnList = "clinic_id,created_at"))
public class RecordAuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="clinic_id", nullable=false, length=40) private String clinicId;
    @Column(name="record_category", nullable=false, length=40) private String category;
    @Column(name="record_external_id", nullable=false, length=80) private String externalId;
    @Column(name="actor", nullable=false, length=80) private String actor;
    @Column(name="action", nullable=false, length=12) private String action;
    @Column(name="correlation_id", length=80) private String correlationId;
    @Column(name="created_at", nullable=false, updatable=false) private Instant createdAt=Instant.now();
    protected RecordAuditEvent() { }
    public RecordAuditEvent(String clinic, String category, String id, String actor, String action, String correlationId) {
        this.clinicId=clinic; this.category=category; this.externalId=id; this.actor=actor; this.action=action; this.correlationId=correlationId;
    }
}
