package com.clincanywhere.controller;

import com.clincanywhere.common.exception.BadRequestException;
import com.clincanywhere.dto.WhatsAppRequest;
import com.clincanywhere.security.RecordScope;
import com.clincanywhere.service.WhatsAppOutboxService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/{clinicId}/whatsapp")
public class WhatsAppController {
    private final WhatsAppOutboxService outbox;
    private final RecordScope scope;
    private final boolean enabled;
    private final com.clincanywhere.service.WhatsAppConsentService consents;
    private final com.clincanywhere.service.PatientNotificationService notifications;
    private final boolean emailSmsEnabled;
    private final com.clincanywhere.service.PatientContactConsentService contactConsents;
    public WhatsAppController(WhatsAppOutboxService outbox, RecordScope scope,
                              @org.springframework.beans.factory.annotation.Value("${app.whatsapp.enabled:false}") boolean enabled,
                              com.clincanywhere.service.WhatsAppConsentService consents,
                              com.clincanywhere.service.PatientNotificationService notifications,
                              com.clincanywhere.service.PatientContactConsentService contactConsents,
                              @org.springframework.beans.factory.annotation.Value("${app.notifications.provider-enabled:false}") boolean emailSmsEnabled) {
        this.outbox = outbox; this.scope = scope; this.enabled = enabled; this.consents=consents;this.notifications=notifications;this.contactConsents=contactConsents;this.emailSmsEnabled=emailSmsEnabled;
    }
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public java.util.Map<String, Object> send(@PathVariable String clinicId, @Valid @RequestBody WhatsAppRequest request, Authentication authentication) {
        scope.requireMember(clinicId, authentication);
        if (!enabled) throw new BadRequestException("WhatsApp delivery is disabled; configure the provider before sending");
        if (!scope.isStaffOrAdmin(authentication)) throw new org.springframework.security.access.AccessDeniedException("Clinic staff required");
        if (!request.consentConfirmed()) throw new BadRequestException("Patient WhatsApp consent is required");
        if (!consents.isActive(clinicId,request.phoneNumber())) throw new BadRequestException("No active, evidenced WhatsApp consent exists for this recipient");
        var item = outbox.enqueue(clinicId, authentication.getName(), request.phoneNumber(), request.message());
        return java.util.Map.of("id", item.getId(), "status", item.getStatus().name());
    }
    @org.springframework.web.bind.annotation.PostMapping("/consents")
    @ResponseStatus(HttpStatus.CREATED)
    public java.util.Map<String,String> recordConsent(@PathVariable String clinicId,
            @Valid @RequestBody com.clincanywhere.dto.WhatsAppConsentRequest request,Authentication authentication) {
        requireConsentManager(clinicId, authentication);
        consents.grant(clinicId,request.phoneNumber(),request.evidenceReference(),authentication.getName());
        return java.util.Map.of("status","CONSENTED");
    }
    @org.springframework.web.bind.annotation.GetMapping
    public java.util.List<java.util.Map<String, Object>> outbox(@PathVariable String clinicId, Authentication authentication) {
        scope.requireMember(clinicId, authentication);
        return outbox.list(clinicId, authentication.getName(), scope.isAdmin(authentication));
    }
    @org.springframework.web.bind.annotation.PostMapping("/notifications")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public java.util.Map<String,Object> notifyPatient(@PathVariable String clinicId,@Valid @RequestBody com.clincanywhere.dto.PatientNotificationRequest request,Authentication authentication){
        scope.requireMember(clinicId,authentication);
        if(!scope.isStaffOrAdmin(authentication))throw new org.springframework.security.access.AccessDeniedException("Clinic staff required");
        if(!request.consentConfirmed())throw new BadRequestException("Patient communication consent is required");
        if(request.channel().equals("WHATSAPP")){if(!enabled)throw new BadRequestException("WhatsApp delivery is disabled");if(!consents.isActive(clinicId,request.destination()))throw new BadRequestException("No active, evidenced WhatsApp consent exists");var item=outbox.enqueue(clinicId,authentication.getName(),request.destination(),request.message());return java.util.Map.of("channel","WHATSAPP","status",item.getStatus().name(),"id",item.getId());}
        if(!request.channel().equals("EMAIL")&&!request.channel().equals("SMS"))throw new BadRequestException("Supported channels: WHATSAPP, EMAIL, SMS");
        if(!emailSmsEnabled)throw new BadRequestException("SMS/email provider is not configured");
        if(!contactConsents.isActive(clinicId,request.channel(),request.destination()))throw new BadRequestException("No active, evidenced consent exists for this recipient and channel");
        var item=notifications.enqueue(clinicId,authentication.getName(),request.destination(),request.channel(),request.message());
        return java.util.Map.of("channel",request.channel(),"status",item.status(),"id",item.getId());
    }
    public record ContactConsentRequest(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Pattern(regexp="SMS|EMAIL") String channel,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=200) String destination,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Pattern(regexp="GRANTED|REVOKED") String action,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=300) String evidenceReference) {}
    @org.springframework.web.bind.annotation.PostMapping("/notifications/consents")
    @ResponseStatus(HttpStatus.CREATED)
    public java.util.Map<String,String> recordContactConsent(@PathVariable String clinicId,@Valid @RequestBody ContactConsentRequest request,Authentication authentication){
        requireConsentManager(clinicId,authentication);
        contactConsents.record(clinicId,request.channel(),request.destination(),request.action(),request.evidenceReference(),authentication.getName());
        return java.util.Map.of("status",request.action());
    }

    private void requireConsentManager(String clinicId, Authentication authentication) {
        scope.requireMember(clinicId, authentication);
        if (!scope.isDoctorOrAdmin(authentication.getName())) {
            throw new org.springframework.security.access.AccessDeniedException("Doctor or clinic administrator required to manage messaging consent");
        }
    }
    @org.springframework.web.bind.annotation.GetMapping("/notifications")
    public java.util.List<java.util.Map<String,Object>> notificationHistory(@PathVariable String clinicId,Authentication authentication){scope.requireMember(clinicId,authentication);if(!scope.isStaffOrAdmin(authentication))throw new org.springframework.security.access.AccessDeniedException("Clinic staff required");return notifications.list(clinicId);}
}
