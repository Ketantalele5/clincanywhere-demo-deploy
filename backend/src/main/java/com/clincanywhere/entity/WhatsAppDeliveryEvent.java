package com.clincanywhere.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="whatsapp_delivery_events", uniqueConstraints=
        @UniqueConstraint(name="uq_whatsapp_receipt", columnNames={"provider_message_id","provider_status","occurred_at"}))
public class WhatsAppDeliveryEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="provider_message_id", nullable=false, length=120) private String providerMessageId;
    @Column(name="provider_status", nullable=false, length=20) private String status;
    @Column(name="occurred_at", nullable=false) private Instant occurredAt;
    @Column(name="received_at", nullable=false, updatable=false) private Instant receivedAt=Instant.now();
    @Column(name="error_code", length=40) private String errorCode;
    @Column(name="processed_at") private Instant processedAt;
    protected WhatsAppDeliveryEvent() { }
    public WhatsAppDeliveryEvent(String id, String status, Instant occurredAt, String errorCode) {
        this.providerMessageId=id; this.status=status; this.occurredAt=occurredAt; this.errorCode=errorCode;
    }
    public String getProviderMessageId() { return providerMessageId; }
    public String getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
    public Instant getReceivedAt() { return receivedAt; }
    public void markProcessed() { this.processedAt=Instant.now(); }
}
