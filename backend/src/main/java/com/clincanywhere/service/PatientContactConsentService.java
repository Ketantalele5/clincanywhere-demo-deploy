package com.clincanywhere.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PatientContactConsentService {
    private final JdbcTemplate jdbc;
    private final FieldEncryptionService encryption;

    public PatientContactConsentService(JdbcTemplate jdbc, FieldEncryptionService encryption) {
        this.jdbc = jdbc;
        this.encryption = encryption;
    }

    public void record(String clinic, String channel, String destination, String action, String evidence, String actor) {
        jdbc.update("insert into patient_message_consent_events(clinic_id,channel,recipient_fingerprint,action,evidence_reference,recorded_by) values(?,?,?,?,?,?)",
                clinic, channel, fingerprint(channel, destination), action, evidence.trim(), actor);
    }

    public boolean isActive(String clinic, String channel, String destination) {
        var actions = jdbc.query("select action from patient_message_consent_events where clinic_id=? and channel=? and recipient_fingerprint=? order by id desc fetch first 1 row only",
                (rs, row) -> rs.getString(1), clinic, channel, fingerprint(channel, destination));
        return actions.stream().findFirst().filter("GRANTED"::equals).isPresent();
    }

    private String fingerprint(String channel, String destination) {
        String normalized = destination == null ? "" : destination.trim().toLowerCase(java.util.Locale.ROOT);
        return encryption.fingerprint(channel + ":" + normalized);
    }
}
