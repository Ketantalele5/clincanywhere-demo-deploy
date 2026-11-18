package com.clincanywhere.service;

import com.clincanywhere.security.RecordScope;

import com.clincanywhere.common.exception.BadRequestException;
import com.clincanywhere.common.exception.NotFoundException;
import com.clincanywhere.entity.PatientInvite;
import com.clincanywhere.repository.CareRecordRepository;
import com.clincanywhere.repository.PatientInviteRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientInviteService {
    private final PatientInviteRepository invites;
    private final CareRecordRepository records;
    private final RecordScope scope;
    private final SecureRandom random = new SecureRandom();
    public PatientInviteService(PatientInviteRepository invites, CareRecordRepository records, RecordScope scope) {
        this.invites = invites; this.records = records; this.scope = scope;
    }
    @Transactional
    public String issue(String clinic, String patientRecordId, String issuedBy) {
        scope.requireClinic(clinic);
        if (records.findByClinicIdAndCategoryAndExternalId(clinic, "patients", patientRecordId).isEmpty()) {
            throw new NotFoundException("Patient record not found");
        }
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        invites.save(new PatientInvite(clinic, patientRecordId, issuedBy, hash(token), Instant.now().plusSeconds(86400)));
        return token;
    }
    @Transactional
    public Invitation consume(String token) {
        if (token == null || token.isBlank()) throw new BadRequestException("A patient invitation token is required");
        PatientInvite invite = invites.findByTokenHashForUpdate(hash(token))
                .orElseThrow(() -> new BadRequestException("Patient invitation is invalid or expired"));
        if (invite.isUsed() || invite.getExpiresAt() == null || !invite.getExpiresAt().isAfter(Instant.now())) throw new BadRequestException("Patient invitation is invalid or expired");
        invite.markUsed();
        return new Invitation(invite.getClinicId(), invite.getPatientRecordId());
    }
    private String hash(String token) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    public record Invitation(String clinicId, String patientRecordId) { }
}
