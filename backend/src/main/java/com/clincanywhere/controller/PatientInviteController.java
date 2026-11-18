package com.clincanywhere.controller;

import com.clincanywhere.dto.PatientInviteRequest;
import com.clincanywhere.service.PatientInviteService;
import com.clincanywhere.security.RecordScope;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/{clinicId}/patient-invitations")
public class PatientInviteController {
    private final PatientInviteService invites;
    private final RecordScope scope;
    public PatientInviteController(PatientInviteService invites, RecordScope scope) { this.invites = invites; this.scope = scope; }
    @PostMapping
    public Map<String, Object> issue(@PathVariable String clinicId, @Valid @RequestBody PatientInviteRequest request,
                                     Authentication authentication) {
        scope.requireMember(clinicId, authentication);
        if (!scope.isAdmin(authentication) && !scope.isDoctor(authentication.getName())) {
            throw new org.springframework.security.access.AccessDeniedException("Doctor or administrator role required");
        }
        String token = invites.issue(clinicId, request.patientRecordId(), authentication.getName());
        return Map.of("invitationToken", token, "expiresAt", Instant.now().plusSeconds(86400));
    }
}
