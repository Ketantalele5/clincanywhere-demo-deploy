package com.clincanywhere.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="whatsapp_consent_events", indexes=@Index(name="ix_whatsapp_consent_latest",columnList="clinic_id,phone_fingerprint,id"))
public class WhatsAppConsentEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="clinic_id",nullable=false,length=40) private String clinicId;
    @Column(name="phone_fingerprint",nullable=false,length=64) private String phoneFingerprint;
    @Column(name="action",nullable=false,length=12) private String action;
    @Column(name="evidence_reference",length=200) private String evidenceReference;
    @Column(name="actor",nullable=false,length=80) private String actor;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt=Instant.now();
    protected WhatsAppConsentEvent() { }
    public WhatsAppConsentEvent(String clinic,String fingerprint,String action,String evidence,String actor) {
        this.clinicId=clinic; this.phoneFingerprint=fingerprint; this.action=action; this.evidenceReference=evidence; this.actor=actor;
    }
    public String getAction() { return action; }
}
