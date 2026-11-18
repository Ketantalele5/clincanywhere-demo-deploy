package com.clincanywhere.controller;

import com.clincanywhere.dto.TicketCreateRequest;
import com.clincanywhere.dto.TicketUpdateRequest;
import com.clincanywhere.service.PatientSupportService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/{clinicId}/support")
public class PatientSupportController {
    private final PatientSupportService support;

    public PatientSupportController(PatientSupportService support) {
        this.support = support;
    }

    @GetMapping
    public List<Map<String, Object>> list(@PathVariable String clinicId, Authentication auth) {
        return support.list(clinicId, auth);
    }

    @PostMapping
    public Map<String, Object> create(@PathVariable String clinicId,
                                      @Valid @RequestBody TicketCreateRequest request,
                                      Authentication auth) {
        return support.create(clinicId, request, auth);
    }

    @PutMapping("/{ticketId}")
    public Map<String, String> update(@PathVariable String clinicId, @PathVariable long ticketId,
                                      @Valid @RequestBody TicketUpdateRequest request,
                                      Authentication auth) {
        return support.update(clinicId, ticketId, request, auth);
    }
}
