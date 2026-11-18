package com.clincanywhere.controller;

import com.clincanywhere.security.RecordScope;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

/** ABDM integration staging API. Network callbacks and live gateway calls require production credentials. */
@RestController
@RequestMapping("/api/{clinicId}/abdm")
public class AbdmController {
    private final RecordScope scope;
    private final JdbcTemplate jdbc;
    private final boolean enabled;
    private final String hipId;
    private final String mode;
    public AbdmController(RecordScope scope, JdbcTemplate jdbc,
                          @org.springframework.beans.factory.annotation.Value("${app.abdm.hip-id:}") String hipId,
                          @org.springframework.beans.factory.annotation.Value("${app.abdm.mode:SANDBOX_NOT_CONFIGURED}") String mode,
                          @org.springframework.beans.factory.annotation.Value("${app.abdm.enabled:false}") boolean enabled) {
        this.scope = scope; this.jdbc = jdbc; this.enabled = enabled; this.hipId=hipId; this.mode=mode;
    }

    @GetMapping("/status")
    public Map<String,Object> status(@PathVariable String clinicId, Authentication auth) {
        scope.requireMember(clinicId, auth);
        return Map.of("enabled", enabled, "mode", enabled ? mode : "SANDBOX_NOT_CONFIGURED",
                "hipId", hipId,
                "consentCallbacksVerified", false,
                "message", enabled
                        ? "Gateway settings are present, but verified consent callbacks are not configured"
                        : "Configure ABDM credentials and verified callback handling before live exchange");
    }

    @GetMapping("/consents")
    public List<Map<String,Object>> consents(@PathVariable String clinicId, Authentication auth) {
        scope.requireMember(clinicId, auth);
        if (!scope.isAdmin(auth) && !scope.isDoctorOrAdmin(auth.getName())) throw new AccessDeniedException("Doctor or administrator role required");
        return jdbc.queryForList("select id, patient_id as patientId, purpose, status, expires_at as expiresAt, created_at as createdAt from abdm_consent_requests where clinic_id=? order by id desc", clinicId);
    }

    @PostMapping("/consents") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> requestConsent(@PathVariable String clinicId, @Valid @RequestBody ConsentRequest body, Authentication auth) {
        scope.requireMember(clinicId, auth);
        if (!scope.isDoctorOrAdmin(auth.getName())) throw new AccessDeniedException("Doctor or administrator role required");
        if (!patientExists(clinicId, body.patientId())) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
        }
        OffsetDateTime expiresAt = parseFutureExpiry(body.expiresAt());
        var key = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                    "insert into abdm_consent_requests(clinic_id,patient_id,purpose,status,expires_at,created_by) " +
                            "values(?,?,?,'REQUESTED',?,?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, clinicId);
            statement.setString(2, body.patientId());
            statement.setString(3, body.purpose());
            statement.setObject(4, expiresAt);
            statement.setString(5, auth.getName());
            return statement;
        }, key);
        var generatedKey = key.getKey();
        if (generatedKey == null) throw new IllegalStateException("ABDM consent request was saved without a generated id");
        var id = generatedKey.longValue();
        return Map.of("id", id, "status", "REQUESTED", "message", "Awaiting patient authorization through the ABDM consent manager");
    }

    /**
     * Consent callbacks remain disabled until the gateway signature and callback payload can be verified.
     * Never trust a request body alone to mark a patient's consent as granted or revoked.
     */
    @PostMapping("/callbacks/consent")
    public Map<String,String> consentCallback(@PathVariable String clinicId, @Valid @RequestBody ConsentCallback body) {
        throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Verified ABDM consent callbacks are not configured; consent status was not changed");
    }

    private boolean patientExists(String clinicId, String patientId) {
        Integer count = jdbc.queryForObject(
                "select count(*) from care_records where clinic_id=? and category='patients' and external_id=?",
                Integer.class, clinicId, patientId);
        return count != null && count > 0;
    }

    private OffsetDateTime parseFutureExpiry(String value) {
        try {
            OffsetDateTime expiry = OffsetDateTime.parse(value);
            if (expiry.isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
                return expiry;
            }
        } catch (java.time.format.DateTimeParseException ignored) {
            // Return a consistent client error below rather than exposing a parser exception as HTTP 500.
        }
        throw new com.clincanywhere.common.exception.BadRequestException(
                "Consent expiry must be a valid future ISO-8601 timestamp");
    }

    public record ConsentRequest(@NotBlank @Size(max=80) String patientId, @NotBlank @Size(max=80) String purpose, @NotBlank String expiresAt) {}
    public record ConsentCallback(long requestId, @Pattern(regexp="GRANTED|DENIED|REVOKED") String status, @NotBlank @Size(max=120) String gatewayReference) {}
}
