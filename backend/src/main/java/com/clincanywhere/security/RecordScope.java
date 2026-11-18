package com.clincanywhere.security;

import java.util.Set;
import org.springframework.security.core.Authentication;
import org.jspecify.annotations.Nullable;
import com.clincanywhere.common.exception.NotFoundException;
import org.springframework.stereotype.Component;

@Component
public class RecordScope {
    private static final Set<String> CATEGORIES = Set.of("patients", "appointments", "doctors", "records",
            "prescriptions", "followups", "instantreminders", "laboratory", "pharmacy", "beds",
            "billing", "inventory", "settings");
    private final com.clincanywhere.repository.UserAccountRepository users;
    private final com.clincanywhere.repository.CareRecordRepository records;

    public RecordScope(com.clincanywhere.repository.UserAccountRepository users, com.clincanywhere.repository.CareRecordRepository records) { this.users = users; this.records = records; }

    public void requireClinic(String clinicId) {
        if (clinicId == null || !clinicId.matches("[A-Za-z0-9-]{2,40}")
                || !Set.of("clinic-a", "clinic-b", "clinic-c").contains(clinicId)
                && !records.existsByClinicIdAndCategoryAndExternalId(clinicId, "settings", "clinic-profile")) {
            throw new NotFoundException("Unknown clinic");
        }
    }

    public void requireScope(String clinicId, String category) {
        requireClinic(clinicId);
        if (!CATEGORIES.contains(category)) throw new NotFoundException("Unknown data category");
    }
    public String requirePatientRead(String clinicId, String category, String username) {
        requirePeerMember(clinicId, username);
        String patientId = patientRecordId(username);
        if (patientId != null && "patients".equals(category)) return patientId;
        throw new org.springframework.security.access.AccessDeniedException("Patients can only access their own patient record");
    }

    public void requireMember(String clinicId, Authentication authentication) {
        requireClinic(clinicId);
        if (isPlatformOwner(authentication)) return;
        if (authentication == null || authentication.getName() == null || authentication.getAuthorities() == null
                || authentication.getAuthorities().stream().filter(java.util.Objects::nonNull).noneMatch(a -> a.getAuthority() != null && a.getAuthority().startsWith("ROLE_"))
                || !users.existsMembership(authentication.getName(), clinicId)) {
            throw new com.clincanywhere.common.exception.NotFoundException("Clinic not found");
        }
    }

    public void requirePeerMember(String clinicId, String username) {
        requireClinic(clinicId);
        if (username == null || username.isBlank() || !isPlatformOwner(username) && !users.existsMembership(username, clinicId)) {
            throw new NotFoundException("Message recipient not found");
        }
    }

    public boolean isPlatformOwner(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated() && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream().filter(java.util.Objects::nonNull)
                .anyMatch(authority -> "ROLE_PLATFORM_OWNER".equals(authority.getAuthority()));
    }

    private boolean isPlatformOwner(String username) {
        return users.findRoleByUsername(username).filter("PLATFORM_OWNER"::equals).isPresent();
    }

    public boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities() != null && authentication.getAuthorities().stream()
                .filter(java.util.Objects::nonNull).anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                        || "ROLE_PLATFORM_OWNER".equals(authority.getAuthority()));
    }
    public boolean isDoctorOrAdmin(String username) {
        return users.findByUsername(username).map(account -> "DOCTOR".equals(account.getRole()) || "ADMIN".equals(account.getRole())
                || "PLATFORM_OWNER".equals(account.getRole())).orElse(false);
    }
    public boolean isDoctor(String username) {
        return users.findByUsername(username).map(account -> "DOCTOR".equals(account.getRole())).orElse(false);
    }
    public boolean isStaffOrAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities() != null && authentication.getAuthorities().stream().anyMatch(authority ->
                Set.of("ROLE_PLATFORM_OWNER", "ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_RECEPTIONIST", "ROLE_LAB", "ROLE_PHARMACIST", "ROLE_BILLING", "ROLE_INVENTORY").contains(authority.getAuthority()));
    }
    public @Nullable String patientRecordId(String username) {
        return users.findByUsername(username).filter(account -> "PATIENT".equals(account.getRole()))
                .map(account -> account.getPatientRecordId()).orElse(null);
    }
    public java.util.List<com.clincanywhere.dto.MessageContact> messageContacts(String clinic, String username) {
        requireClinic(clinic);
        String role = users.findRoleByUsername(username).orElseThrow(() -> new NotFoundException("Account not found"));
        return users.findMessageContacts(clinic, username, role);
    }
    public java.util.List<com.clincanywhere.dto.MessageContact> searchMessageContacts(String clinic, String username, String search, int limit) {
        requireClinic(clinic);
        String role = users.findRoleByUsername(username).orElseThrow(() -> new NotFoundException("Account not found"));
        return users.searchMessageContacts(clinic, username, role, search == null ? "" : search.trim(),
                org.springframework.data.domain.PageRequest.of(0, limit));
    }
    public void requireMessagePermission(String clinic, String sender, String recipient) {
        requirePeerMember(clinic, sender);
        requirePeerMember(clinic, recipient);
        var from = users.findByUsername(sender).orElseThrow(() -> new NotFoundException("Account not found"));
        var to = users.findByUsername(recipient).orElseThrow(() -> new NotFoundException("Message recipient not found"));
        if (from.getRole().equals("PATIENT") && !(to.getRole().equals("DOCTOR") || to.getRole().equals("ADMIN"))) {
            throw new NotFoundException("Message recipient not found");
        }
        if (to.getRole().equals("PATIENT") && !(from.getRole().equals("DOCTOR") || from.getRole().equals("ADMIN")
                || from.getRole().equals("PLATFORM_OWNER"))) {
            throw new NotFoundException("Message recipient not found");
        }
    }
}
