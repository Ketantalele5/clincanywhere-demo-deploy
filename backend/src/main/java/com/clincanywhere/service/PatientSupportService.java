package com.clincanywhere.service;

import com.clincanywhere.dto.TicketCreateRequest;
import com.clincanywhere.dto.TicketUpdateRequest;
import com.clincanywhere.security.RecordScope;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PatientSupportService {
    private final RecordScope scope;
    private final JdbcTemplate jdbc;
    private final com.clincanywhere.repository.UserAccountRepository users;

    public PatientSupportService(RecordScope scope, JdbcTemplate jdbc,
                                 com.clincanywhere.repository.UserAccountRepository users) {
        this.scope = scope;
        this.jdbc = jdbc;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(String clinicId, Authentication auth) {
        scope.requireMember(clinicId, auth);
        String patientId = scope.patientRecordId(auth.getName());
        if (patientId != null) {
            return jdbc.queryForList("select id,subject,message,status,created_at as createdAt from patient_support_tickets where clinic_id=? and patient_id=? order by id desc", clinicId, patientId);
        }
        requireStaff(auth);
        return jdbc.queryForList("select id,patient_id as patientId,subject,message,status,assigned_to as assignedTo,created_at as createdAt from patient_support_tickets where clinic_id=? order by id desc", clinicId);
    }

    @Transactional
    public Map<String, Object> create(String clinicId, TicketCreateRequest request, Authentication auth) {
        scope.requireMember(clinicId, auth);
        String patientId = scope.patientRecordId(auth.getName());
        if (patientId == null) {
            requireStaff(auth);
            patientId = resolveRequestedPatient(clinicId, request.patientId());
            if (patientId == null) throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Select a patient for this support ticket");
        }
        String assignedPatientId = patientId;
        var key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                    "insert into patient_support_tickets(clinic_id,patient_id,subject,message,status) values(?,?,?,?,'OPEN')",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, clinicId);
            statement.setString(2, assignedPatientId);
            statement.setString(3, request.subject());
            statement.setString(4, request.message());
            return statement;
        }, key);
        var generatedKey = key.getKey();
        if (generatedKey == null) throw new IllegalStateException("Support ticket was saved without a generated id");
        return Map.of("id", generatedKey.longValue(), "status", "OPEN");
    }

    @Transactional
    public Map<String, String> update(String clinicId, long ticketId, TicketUpdateRequest request, Authentication auth) {
        scope.requireMember(clinicId, auth);
        requireStaff(auth);
        String assignee = request.assignedTo();
        if (assignee != null && !assignee.isBlank()) {
            var account = users.findByUsername(assignee.trim())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket assignee is not a clinic staff member"));
            if (!users.existsMembership(assignee.trim(), clinicId)
                    || !java.util.Set.of("ADMIN", "DOCTOR", "RECEPTIONIST", "LAB", "PHARMACIST", "BILLING", "INVENTORY").contains(account.getRole())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket assignee is not a clinic staff member");
            }
            assignee = assignee.trim();
        } else {
            assignee = null;
        }
        int updated = jdbc.update("update patient_support_tickets set status=?,assigned_to=?,updated_at=current_timestamp where clinic_id=? and id=?",
                request.status(), assignee, clinicId, ticketId);
        if (updated == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Support ticket not found");
        return Map.of("status", request.status());
    }

    private @Nullable String resolveRequestedPatient(String clinicId, String patientId) {
        if (patientId == null || patientId.isBlank()) return null;
        if (jdbc.queryForList("select id from care_records where clinic_id=? and category='patients' and external_id=?", clinicId, patientId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
        }
        return patientId;
    }

    private void requireStaff(Authentication auth) {
        if (!scope.isStaffOrAdmin(auth)) throw new AccessDeniedException("Clinic staff access required");
    }
}
