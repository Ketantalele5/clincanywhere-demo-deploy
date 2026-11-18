package com.clincanywhere.controller;

import com.clincanywhere.dto.ClinicOnboardingRequest;
import com.clincanywhere.entity.ClinicLead;
import com.clincanywhere.repository.ClinicLeadRepository;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/clinic-onboarding")
public class ClinicOnboardingController {
    private final ClinicLeadRepository leads;
    public ClinicOnboardingController(ClinicLeadRepository leads) { this.leads = leads; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> submit(@Valid @RequestBody ClinicOnboardingRequest request) {
        var lead = leads.save(new ClinicLead(request.clinicName().trim(), request.contactName().trim(),
                request.email().trim().toLowerCase(), request.phone(), request.city(), request.clinicType(), request.message()));
        return Map.of("id", lead.getId(), "status", lead.getStatus(), "message", "Thanks. The ClincyAnywhere team will contact you about onboarding.");
    }
}
