package com.clincanywhere.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "patient_invites")
public class PatientInvite extends BaseEntity {
    @Column(name = "clinic_id", nullable = false, length = 40) private String clinicId;
    @Column(name = "patient_record_id", nullable = false, length = 80) private String patientRecordId;
    @Column(name = "issued_by", nullable = false, length = 80) private String issuedBy;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(nullable = false) private boolean used;
    protected PatientInvite() { }
    public PatientInvite(String clinicId, String patientRecordId, String issuedBy, String tokenHash, Instant expiresAt) {
        this.clinicId = clinicId; this.patientRecordId = patientRecordId; this.issuedBy = issuedBy; this.tokenHash = tokenHash; this.expiresAt = expiresAt;
    }
    public String getClinicId() { return clinicId; }
    public String getPatientRecordId() { return patientRecordId; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isUsed() { return used; }
    public void markUsed() { this.used = true; }
}
