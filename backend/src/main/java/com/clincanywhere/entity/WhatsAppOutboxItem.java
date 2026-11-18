package com.clincanywhere.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "whatsapp_outbox")
public class WhatsAppOutboxItem extends BaseEntity {
    public enum Status { PENDING, PROCESSING, SENT, DELIVERED, READ, FAILED }
    @Column(name = "clinic_id", nullable = false, length = 40) private String clinicId;
    @Column(name = "requested_by", nullable = false, length = 80) private String requestedBy;
    @Column(name = "phone_fingerprint", length = 64) private String phoneFingerprint;
    @Column(name = "phone_number", nullable = false, length = 128) private String phoneNumber;
    @Column(name = "message_body", nullable = false, length = 2048) private String message;
    @Column(name = "consent_confirmed_at", nullable = false, updatable = false) private Instant consentConfirmedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status = Status.PENDING;
    @Column(name = "attempt_count", nullable = false) private int attempts;
    @Column(name = "provider_message_id", length = 120) private String providerMessageId;
    @Column(name = "last_error", length = 500) private String lastError;
    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt = Instant.now();
    protected WhatsAppOutboxItem() { }
    public WhatsAppOutboxItem(String clinicId, String requestedBy, String phoneNumber, String message) {
        this.clinicId = clinicId; this.requestedBy = requestedBy; this.phoneNumber = phoneNumber; this.message = message;
        this.consentConfirmedAt = Instant.now();
    }
    public WhatsAppOutboxItem(String clinicId,String requestedBy,String phoneNumber,String message,String phoneFingerprint) {
        this(clinicId,requestedBy,phoneNumber,message); this.phoneFingerprint=phoneFingerprint;
    }
    public String getClinicId() { return clinicId; }
    public String getRequestedBy() { return requestedBy; }
    public String getPhoneFingerprint() { return phoneFingerprint; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getMessage() { return message; }
    public Instant getConsentConfirmedAt() { return consentConfirmedAt; }
    public Status getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public void protectContact(String phone, String body) { this.phoneNumber=phone; this.message=body; }
    public void setPhoneFingerprint(String fingerprint) { this.phoneFingerprint=fingerprint; }
    public void suppressOptOut() { if (status == Status.PENDING || status == Status.PROCESSING) { status=Status.FAILED; lastError="Suppressed after patient opt-out"; } }
    public void sent(String providerMessageId) {
        this.providerMessageId = providerMessageId;
        this.lastError = null;
        if (this.status == Status.PROCESSING) this.status = Status.SENT;
    }
    public void applyProviderStatus(String status, String errorCode) {
        switch (status) {
            case "sent" -> { if (this.status == Status.PENDING || this.status == Status.PROCESSING) this.status=Status.SENT; }
            case "delivered" -> { if (this.status != Status.READ && this.status != Status.FAILED) this.status=Status.DELIVERED; }
            case "read" -> { if (this.status != Status.FAILED) this.status=Status.READ; }
            case "failed" -> { this.status=Status.FAILED; this.lastError=errorCode == null ? "Provider delivery failed" : ("Provider error code " + errorCode); }
            default -> { }
        }
    }
    public void claim() { this.status = Status.PROCESSING; this.nextAttemptAt = Instant.now().plusSeconds(300); }
    public void failed(String error) { this.attempts++; String safeError = error == null || error.isBlank() ? "Provider delivery failed" : error; this.lastError = safeError.substring(0, Math.min(500, safeError.length())); this.nextAttemptAt = Instant.now().plusSeconds(Math.min(3600, 15L << Math.min(attempts, 8))); this.status = attempts >= 8 ? Status.FAILED : Status.PENDING; }
}
