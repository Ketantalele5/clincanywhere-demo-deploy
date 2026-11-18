package com.clincanywhere.controller;

import com.clincanywhere.dto.AbhaLinkRequest;
import com.clincanywhere.dto.FacilityProfileRequest;
import com.clincanywhere.dto.ProfessionalProfileRequest;
import com.clincanywhere.service.DigitalHealthService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/{clinicId}/digital-health")
public class DigitalHealthController {
    private final DigitalHealthService digitalHealth;

    public DigitalHealthController(DigitalHealthService digitalHealth) {
        this.digitalHealth = digitalHealth;
    }

    @GetMapping("/facility")
    public Map<String, Object> facility(@PathVariable String clinicId, Authentication auth) {
        return digitalHealth.facility(clinicId, auth);
    }

    @PutMapping("/facility")
    public Map<String, String> updateFacility(@PathVariable String clinicId,
                                             @Valid @RequestBody FacilityProfileRequest request,
                                             Authentication auth) {
        return digitalHealth.updateFacility(clinicId, request, auth);
    }

    @GetMapping("/professionals")
    public List<Map<String, Object>> professionals(@PathVariable String clinicId, Authentication auth) {
        return digitalHealth.professionals(clinicId, auth);
    }

    @PutMapping("/professionals/me")
    public Map<String, String> updateProfessional(@PathVariable String clinicId,
                                                 @Valid @RequestBody ProfessionalProfileRequest request,
                                                 Authentication auth) {
        return digitalHealth.updateProfessional(clinicId, request, auth);
    }

    @PostMapping("/patients/{patientId}/abha")
    public Map<String, String> linkAbha(@PathVariable String clinicId, @PathVariable String patientId,
                                        @Valid @RequestBody AbhaLinkRequest request, Authentication auth) {
        return digitalHealth.linkAbha(clinicId, patientId, request, auth);
    }
}
