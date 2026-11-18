package com.clincanywhere.controller;

import java.util.List;
import com.clincanywhere.dto.ClinicResponse;
import com.clincanywhere.dto.HealthResponse;
import com.clincanywhere.service.CareRecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api")
public class ClinicController {
    private final CareRecordService records;
    private final com.clincanywhere.repository.UserAccountRepository users;

    public ClinicController(CareRecordService records, com.clincanywhere.repository.UserAccountRepository users) {
        this.records = records;
        this.users = users;
    }

    @GetMapping("/clinics")
    @Transactional(readOnly = true)
    public List<ClinicResponse> clinics(Authentication authentication) {
        if (authentication != null && authentication.getAuthorities().stream().anyMatch(authority ->
                authority.getAuthority().equals("ROLE_ONBOARDING") || authority.getAuthority().equals("ROLE_PATIENT"))) return List.of();
        if (authentication == null) return List.of();
        if (authentication.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_PLATFORM_OWNER"))) {
            return records.allClinicIds().stream().sorted()
                    .map(id -> clinic(id, id, authentication)).flatMap(result -> result.stream()).toList();
        }
        return users.findByUsername(authentication.getName()).stream()
                .flatMap(account -> account.getClinicIds().stream().sorted())
                .map(id -> clinic(id, id, authentication)).flatMap(result -> result.stream()).toList();
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("ok", "ClincyAnywhere API");
    }

    private java.util.Optional<ClinicResponse> clinic(String clinicId, String defaultName, Authentication authentication) {
        return records.clinicProfile(clinicId, authentication)
                .map(response -> response.fields())
                .map(fields -> {
                    Object value = fields.get("name");
                    String name = value instanceof String clinicName && !clinicName.isBlank() ? clinicName : defaultName;
                    return new ClinicResponse(clinicId, name);
                });
    }

}
