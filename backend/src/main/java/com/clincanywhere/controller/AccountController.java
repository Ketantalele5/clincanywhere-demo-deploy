package com.clincanywhere.controller;

import com.clincanywhere.dto.AccountProfileResponse;
import com.clincanywhere.dto.ProfileUpdateRequest;
import com.clincanywhere.dto.PasswordChangeRequest;
import com.clincanywhere.dto.StaffCreateRequest;
import com.clincanywhere.entity.UserAccount;
import com.clincanywhere.repository.UserAccountRepository;
import com.clincanywhere.security.RecordScope;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AccountController {
    private final UserAccountRepository accounts;
    private final RecordScope scope;
    private final PasswordEncoder passwords;

    public AccountController(UserAccountRepository accounts, RecordScope scope, PasswordEncoder passwords) {
        this.accounts = accounts; this.scope = scope; this.passwords = passwords;
    }

    @GetMapping("/account/profile")
    @Transactional(readOnly = true)
    public AccountProfileResponse profile(Authentication authentication) {
        return response(current(authentication));
    }

    @PutMapping("/account/profile")
    @Transactional
    public AccountProfileResponse updateProfile(@Valid @RequestBody ProfileUpdateRequest request, Authentication authentication) {
        var account = current(authentication);
        account.updateProfile(request.displayName().trim(), blankToNull(request.email()), blankToNull(request.phone()));
        return response(account);
    }

    @PutMapping("/account/password")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody PasswordChangeRequest request, Authentication authentication) {
        var account = current(authentication);
        if (!passwords.matches(request.currentPassword(), account.getPasswordHash())) {
            throw new com.clincanywhere.common.exception.BadRequestException("Current password is incorrect");
        }
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new com.clincanywhere.common.exception.BadRequestException("Password must be no longer than 72 UTF-8 bytes");
        }
        account.changePassword(passwords.encode(request.newPassword()));
    }

    @GetMapping("/clinics/{clinicId}/team")
    public List<AccountProfileResponse> team(@PathVariable String clinicId, Authentication authentication) {
        scope.requireMember(clinicId, authentication);
        requireAdmin(authentication);
        return accounts.findClinicAccounts(clinicId).stream().map(account -> response(account, clinicId)).toList();
    }

    @PostMapping("/clinics/{clinicId}/team")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public AccountProfileResponse createStaff(@PathVariable String clinicId, @Valid @RequestBody StaffCreateRequest request, Authentication authentication) {
        scope.requireMember(clinicId, authentication);
        requireAdmin(authentication);
        // Validate the target clinic itself before creating a membership. The caller's
        // membership check alone does not prove that this clinic identifier exists.
        scope.requireScope(clinicId, "settings");
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new com.clincanywhere.common.exception.BadRequestException("Password must be no longer than 72 UTF-8 bytes");
        }
        var account = new UserAccount(request.username().trim(), passwords.encode(request.password()), request.role(), java.util.Set.of(clinicId));
        account.updateProfile(request.displayName().trim(), blankToNull(request.email()), blankToNull(request.phone()));
        try { return response(accounts.saveAndFlush(account)); }
        catch (DataIntegrityViolationException duplicate) { throw new com.clincanywhere.common.exception.ConflictException("Username is already taken"); }
    }

    private UserAccount current(Authentication authentication) {
        return accounts.findByUsername(authentication.getName())
                .orElseThrow(() -> new com.clincanywhere.common.exception.NotFoundException("Account not found"));
    }
    private void requireAdmin(Authentication authentication) {
        if (!scope.isAdmin(authentication)) throw new org.springframework.security.access.AccessDeniedException("Clinic administrator access is required");
    }
    private @Nullable String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private AccountProfileResponse response(UserAccount account) {
        return new AccountProfileResponse(account.getUsername(), account.getDisplayName() == null ? account.getUsername() : account.getDisplayName(), account.getEmail(), account.getPhone(), account.getRole(),
                account.getClinicIds().stream().sorted().toList());
    }
    private AccountProfileResponse response(UserAccount account, String clinicId) {
        return new AccountProfileResponse(account.getUsername(), account.getDisplayName() == null ? account.getUsername() : account.getDisplayName(), account.getEmail(), account.getPhone(), account.getRole(), List.of(clinicId));
    }
}
