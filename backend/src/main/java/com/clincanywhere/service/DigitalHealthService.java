package com.clincanywhere.service;

import com.clincanywhere.dto.AbhaLinkRequest;
import com.clincanywhere.dto.FacilityProfileRequest;
import com.clincanywhere.dto.ProfessionalProfileRequest;
import com.clincanywhere.security.RecordScope;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DigitalHealthService {
    private final RecordScope scope;
    private final JdbcTemplate jdbc;

    public DigitalHealthService(RecordScope scope, JdbcTemplate jdbc) {
        this.scope = scope;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> facility(String clinicId, Authentication auth) {
        scope.requireMember(clinicId, auth);
        Map<String, Object> facility = jdbc.query("select hfr_id as hfrId,hfr_status as hfrStatus,hpr_id as hprId,facility_type as facilityType,services,address,abdm_milestone as abdmMilestone from clinic_health_profiles where clinic_id=?", rs -> {
            if (!rs.next()) return Map.of("clinicId", clinicId, "hfrStatus", "NOT_REGISTERED", "abdmMilestone", "M0");
            return Map.of("clinicId", clinicId, "hfrId", text(rs.getString("hfrId")),
                    "hfrStatus", rs.getString("hfrStatus"), "hprId", text(rs.getString("hprId")),
                    "facilityType", text(rs.getString("facilityType")), "services", text(rs.getString("services")),
                    "address", text(rs.getString("address")), "abdmMilestone", rs.getString("abdmMilestone"));
        }, clinicId);
        return facility == null ? Map.of("clinicId", clinicId, "hfrStatus", "NOT_REGISTERED", "abdmMilestone", "M0") : facility;
    }

    @Transactional
    public Map<String, String> updateFacility(String clinicId, FacilityProfileRequest request, Authentication auth) {
        scope.requireMember(clinicId, auth);
        if (!scope.isAdmin(auth)) throw new AccessDeniedException("Clinic administrator required");
        int updated = jdbc.update("update clinic_health_profiles set hfr_id=?,hfr_status=?,facility_type=?,services=?,address=?,updated_at=current_timestamp where clinic_id=?",
                request.hfrId(), request.hfrStatus(), request.facilityType(), request.services(), request.address(), clinicId);
        if (updated == 0) jdbc.update("insert into clinic_health_profiles(clinic_id,hfr_id,hfr_status,facility_type,services,address,updated_at) values(?,?,?,?,?,?,current_timestamp)",
                clinicId, request.hfrId(), request.hfrStatus(), request.facilityType(), request.services(), request.address());
        return Map.of("status", "SAVED", "hfrStatus", request.hfrStatus() == null ? "NOT_REGISTERED" : request.hfrStatus());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> professionals(String clinicId, Authentication auth) {
        scope.requireMember(clinicId, auth);
        requireStaff(auth);
        return jdbc.queryForList("select username,hpr_id as hprId,status from professional_health_profiles where clinic_id=? order by username", clinicId);
    }

    @Transactional
    public Map<String, String> updateProfessional(String clinicId, ProfessionalProfileRequest request, Authentication auth) {
        scope.requireMember(clinicId, auth);
        requireStaff(auth);
        int updated = jdbc.update("update professional_health_profiles set hpr_id=?,status=?,updated_at=current_timestamp where clinic_id=? and username=?",
                request.hprId(), request.status(), clinicId, auth.getName());
        if (updated == 0) jdbc.update("insert into professional_health_profiles(clinic_id,username,hpr_id,status,updated_at) values(?,?,?,?,current_timestamp)",
                clinicId, auth.getName(), request.hprId(), request.status());
        return Map.of("status", "SAVED", "registryStatus", request.status());
    }

    @Transactional(readOnly = true)
    public Map<String, String> linkAbha(String clinicId, String patientId, AbhaLinkRequest request, Authentication auth) {
        scope.requireMember(clinicId, auth);
        if (!scope.isDoctorOrAdmin(auth.getName())) throw new AccessDeniedException("Doctor or administrator required");
        if (jdbc.queryForList("select id from care_records where clinic_id=? and category='patients' and external_id=?", clinicId, patientId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
        }
        if (!request.patientConsent()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient consent is required");
        return Map.of("status", "REQUIRES_ABDM_VERIFICATION",
                "message", "ABHA is stored only after verified identity and consent through an approved ABDM flow. No ABHA value was stored.");
    }

    private void requireStaff(Authentication auth) {
        if (!scope.isStaffOrAdmin(auth)) throw new AccessDeniedException("Clinic staff required");
    }

    private String text(String value) { return value == null ? "" : value; }
}
