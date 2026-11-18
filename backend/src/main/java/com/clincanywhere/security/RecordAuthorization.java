package com.clincanywhere.security;

import java.util.Map;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("recordAuthorization")
public class RecordAuthorization {
    private static final Map<String, Set<String>> WRITE_ACCESS = Map.of(
            "RECEPTIONIST", Set.of("patients", "appointments", "billing", "followups", "instantreminders", "support"),
            "DOCTOR", Set.of("patients", "appointments", "records", "prescriptions", "followups", "instantreminders", "laboratory"),
            "LAB", Set.of("laboratory", "records"),
            // Pharmacists can read prescriptions, but must use the dispense workflow to update them.
            "PHARMACIST", Set.of("pharmacy"),
            "BILLING", Set.of("billing"),
            "INVENTORY", Set.of("pharmacy", "inventory"));
    private static final Set<String> CLINIC_ROLES = Set.of("ADMIN", "RECEPTIONIST", "DOCTOR", "LAB", "PHARMACIST", "BILLING", "INVENTORY", "PATIENT");

    public boolean canWrite(String category, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        if (authentication.getAuthorities() != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority != null && "ROLE_PLATFORM_OWNER".equals(authority.getAuthority()))) return true;
        return authentication.getAuthorities().stream().map(a -> a.getAuthority().replace("ROLE_", ""))
                .filter(CLINIC_ROLES::contains)
                .anyMatch(role -> role.equals("ADMIN") || WRITE_ACCESS.getOrDefault(role, Set.of()).contains(category));
    }
    public boolean canRead(String category, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        if (authentication.getAuthorities() != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority != null && "ROLE_PLATFORM_OWNER".equals(authority.getAuthority()))) return true;
        return authentication.getAuthorities().stream().anyMatch(a -> {
            String role = a.getAuthority().replace("ROLE_", "");
            return CLINIC_ROLES.contains(role) && (role.equals("ADMIN")
                    || WRITE_ACCESS.getOrDefault(role, Set.of()).contains(category)
                    || category.equals("pharmacy") && Set.of("PHARMACIST", "INVENTORY").contains(role)
                    || Set.of("patients", "appointments", "doctors").contains(category)
                    && Set.of("RECEPTIONIST", "DOCTOR", "BILLING", "PHARMACIST", "LAB", "INVENTORY").contains(role)
                    || category.equals("prescriptions") && Set.of("DOCTOR", "PHARMACIST").contains(role)
                    || category.equals("patients") && role.equals("PATIENT")
                    || category.equals("support") && Set.of("PATIENT", "RECEPTIONIST", "DOCTOR", "ADMIN").contains(role));
        });
    }
    public boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated() && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream().anyMatch(a -> a != null
                && (a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_PLATFORM_OWNER")));
    }
}
