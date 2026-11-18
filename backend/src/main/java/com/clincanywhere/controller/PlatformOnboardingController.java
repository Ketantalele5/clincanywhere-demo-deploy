package com.clincanywhere.controller;

import com.clincanywhere.common.exception.ConflictException;
import com.clincanywhere.common.exception.NotFoundException;
import com.clincanywhere.dto.PlatformConvertLeadRequest;
import com.clincanywhere.dto.PlatformLeadAssignmentRequest;
import com.clincanywhere.dto.PlatformLeadCreateRequest;
import com.clincanywhere.dto.PlatformLeadResponse;
import com.clincanywhere.dto.PlatformLeadStatusRequest;
import com.clincanywhere.dto.PlatformOverviewResponse;
import com.clincanywhere.dto.PlatformTeamCreateRequest;
import com.clincanywhere.dto.PlatformTeamMemberResponse;
import com.clincanywhere.dto.RecordRequest;
import com.clincanywhere.entity.ClinicLead;
import com.clincanywhere.entity.UserAccount;
import com.clincanywhere.repository.ClinicLeadRepository;
import com.clincanywhere.repository.UserAccountRepository;
import com.clincanywhere.service.CareRecordService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import com.clincanywhere.repository.CareRecordRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Cross-clinic onboarding CRM. Clinic ADMIN accounts cannot access these endpoints. */
@RestController
@RequestMapping("/api/platform")
@PreAuthorize("hasAnyRole('PLATFORM_OWNER', 'ONBOARDING')")
public class PlatformOnboardingController {
    private final ClinicLeadRepository leads;
    private final UserAccountRepository accounts;
    private final PasswordEncoder passwords;
    private final CareRecordService records;
    private final CareRecordRepository careRecords;

    public PlatformOnboardingController(ClinicLeadRepository leads, UserAccountRepository accounts,
                                        PasswordEncoder passwords, CareRecordService records, CareRecordRepository careRecords) {
        this.leads = leads; this.accounts = accounts; this.passwords = passwords; this.records = records; this.careRecords = careRecords;
    }

    @GetMapping("/overview")
    @Transactional(readOnly = true)
    public PlatformOverviewResponse overview(Authentication auth) {
        List<ClinicLead> visible = visibleLeads(auth);
        long converted = visible.stream().filter(lead -> "CONVERTED".equals(lead.getStatus())).count();
        long open = visible.stream().filter(lead -> !Set.of("CONVERTED", "LOST").contains(lead.getStatus())).count();
        long qualified = visible.stream().filter(lead -> "QUALIFIED".equals(lead.getStatus())).count();
        return new PlatformOverviewResponse(visible.size(), converted, open, qualified,
                isOwner(auth) ? accounts.countByRole("ONBOARDING") : 0);
    }

    @GetMapping("/leads")
    @Transactional(readOnly = true)
    public List<PlatformLeadResponse> listLeads(Authentication auth) {
        return visibleLeads(auth).stream().map(PlatformLeadResponse::from).toList();
    }

    @PostMapping("/leads")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public PlatformLeadResponse createLead(@Valid @RequestBody PlatformLeadCreateRequest request, Authentication auth) {
        String username = auth.getName();
        var lead = new ClinicLead(clean(request.clinicName()), clean(request.contactName()), clean(request.email()).toLowerCase(),
                blankToNull(request.phone()), blankToNull(request.city()), blankToNull(request.clinicType()), blankToNull(request.message()));
        lead.setAttribution("OUTREACH", username, isOwner(auth) ? null : username);
        return PlatformLeadResponse.from(leads.save(lead));
    }

    @PatchMapping("/leads/{id}/status")
    @Transactional
    public PlatformLeadResponse updateStatus(@PathVariable Long id, @Valid @RequestBody PlatformLeadStatusRequest request, Authentication auth) {
        var lead = lead(id);
        requireLeadAccess(lead, auth);
        if ("CONVERTED".equals(lead.getStatus())) throw new ConflictException("A converted clinic cannot be moved back into the lead pipeline");
        lead.updateStatus(request.status());
        return PlatformLeadResponse.from(leads.save(lead));
    }

    @PutMapping("/leads/{id}/assignment")
    @Transactional
    @PreAuthorize("hasRole('PLATFORM_OWNER')")
    public PlatformLeadResponse assignLead(@PathVariable Long id, @Valid @RequestBody PlatformLeadAssignmentRequest request) {
        var lead = lead(id);
        String assignedTo = request.assignedTo().trim();
        var member = accounts.findByUsername(assignedTo).orElseThrow(() -> new NotFoundException("Onboarding team member not found"));
        if (!"ONBOARDING".equals(member.getRole())) throw new NotFoundException("Onboarding team member not found");
        lead.assignTo(assignedTo);
        return PlatformLeadResponse.from(leads.save(lead));
    }

    @PostMapping("/leads/{id}/convert")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public PlatformLeadResponse convertLead(@PathVariable Long id, @Valid @RequestBody PlatformConvertLeadRequest request, Authentication auth) {
        var lead = lead(id);
        requireLeadAccess(lead, auth);
        if ("CONVERTED".equals(lead.getStatus())) throw new ConflictException("This clinic has already been onboarded");
        if ("LOST".equals(lead.getStatus())) throw new ConflictException("Reopen this lead before onboarding the clinic");
        if (accounts.existsByUsername(request.adminUsername().trim())) throw new ConflictException("Clinic admin username is already taken");
        if (request.adminPassword().getBytes(StandardCharsets.UTF_8).length > 72) throw new com.clincanywhere.common.exception.BadRequestException("Password must be no longer than 72 UTF-8 bytes");

        String clinicId = request.clinicId().trim().toLowerCase();
        if (careRecords.existsByClinicIdAndCategoryAndExternalId(clinicId, "settings", "clinic-profile")
                || careRecords.existsByClinicId(clinicId)) {
            throw new ConflictException("A workspace with this clinic id already exists");
        }
        var profile = new RecordRequest();
        profile.addField("id", "clinic-profile");
        profile.addField("name", lead.getClinicName());
        profile.addField("phone", lead.getPhone() == null ? "" : lead.getPhone());
        profile.addField("timezone", "Asia/Kolkata (IST)");
        try {
            records.createProvisioningRecord(clinicId, "settings", profile);
            var clinicAdmin = new UserAccount(request.adminUsername().trim(), passwords.encode(request.adminPassword()), "ADMIN", Set.of(clinicId));
            clinicAdmin.updateProfile(lead.getContactName(), lead.getEmail(), lead.getPhone());
            accounts.saveAndFlush(clinicAdmin);
            lead.markConverted(clinicId);
            return PlatformLeadResponse.from(leads.save(lead));
        } catch (DataIntegrityViolationException conflict) {
            throw new ConflictException("The requested clinic workspace or username already exists");
        }
    }

    @GetMapping("/team")
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('PLATFORM_OWNER')")
    public List<PlatformTeamMemberResponse> team() {
        var allLeads = leads.findAllByOrderByCreatedAtDesc();
        return accounts.findAllByRoleOrderByUsername("ONBOARDING").stream()
                .map(account -> {
                    String username = account.getUsername();
                    var credited = allLeads.stream().filter(lead -> username.equals(
                            lead.getAssignedTo() == null ? lead.getCreatedBy() : lead.getAssignedTo())).toList();
                    return new PlatformTeamMemberResponse(username, displayName(account), account.getEmail(), account.getPhone(),
                            credited.size(), credited.stream().filter(lead -> "CONVERTED".equals(lead.getStatus())).count());
                }).toList();
    }

    @PostMapping("/team")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    @PreAuthorize("hasRole('PLATFORM_OWNER')")
    public PlatformTeamMemberResponse createTeamMember(@Valid @RequestBody PlatformTeamCreateRequest request) {
        String username = request.username().trim();
        if (accounts.existsByUsername(username)) throw new ConflictException("Username is already taken");
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) throw new com.clincanywhere.common.exception.BadRequestException("Password must be no longer than 72 UTF-8 bytes");
        var member = new UserAccount(username, passwords.encode(request.password()), "ONBOARDING", Set.of());
        member.updateProfile(clean(request.displayName()), blankToNull(request.email()), blankToNull(request.phone()));
        try { accounts.saveAndFlush(member); }
        catch (DataIntegrityViolationException duplicate) { throw new ConflictException("Username is already taken"); }
        return new PlatformTeamMemberResponse(member.getUsername(), displayName(member), member.getEmail(), member.getPhone(), 0, 0);
    }

    private List<ClinicLead> visibleLeads(Authentication auth) {
        return isOwner(auth) ? leads.findAllByOrderByCreatedAtDesc()
                : leads.findAllByAssignedToOrCreatedByOrderByCreatedAtDesc(auth.getName(), auth.getName());
    }
    private void requireLeadAccess(ClinicLead lead, Authentication auth) {
        if (!isOwner(auth) && !auth.getName().equals(lead.getAssignedTo()) && !auth.getName().equals(lead.getCreatedBy())) {
            throw new AccessDeniedException("This lead is assigned to another onboarding team member");
        }
    }
    private boolean isOwner(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(authority -> "ROLE_PLATFORM_OWNER".equals(authority.getAuthority()));
    }
    private ClinicLead lead(Long id) { return leads.findById(java.util.Objects.requireNonNull(id, "lead id"))
            .orElseThrow(() -> new NotFoundException("Clinic lead not found")); }
    private String displayName(UserAccount account) { return account.getDisplayName() == null ? account.getUsername() : account.getDisplayName(); }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    private @Nullable String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
