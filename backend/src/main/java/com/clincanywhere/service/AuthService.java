package com.clincanywhere.service;

import com.clincanywhere.security.JwtService;

import com.clincanywhere.dto.LoginRequest;
import com.clincanywhere.dto.LoginResponse;
import com.clincanywhere.dto.RegisterRequest;
import com.clincanywhere.entity.UserAccount;
import com.clincanywhere.repository.UserAccountRepository;
import com.clincanywhere.common.exception.BadRequestException;
import com.clincanywhere.common.exception.ConflictException;
import java.nio.charset.StandardCharsets;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService users;
    private final JwtService jwt;
    private final UserAccountRepository accounts;
    private final PasswordEncoder passwords;
    private final PatientInviteService patientInvites;
    public AuthService(AuthenticationManager authenticationManager, UserDetailsService users, JwtService jwt,
                       UserAccountRepository accounts, PasswordEncoder passwords, PatientInviteService patientInvites) {
        this.authenticationManager = authenticationManager; this.users = users; this.jwt = jwt;
        this.accounts = accounts; this.passwords = passwords; this.patientInvites = patientInvites;
    }
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (request == null || request.username() == null || request.password() == null) {
            throw new BadRequestException("Username and password are required");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadRequestException("Password must be no longer than 72 UTF-8 bytes");
        }
        String username = request.username().trim();
        if (accounts.existsByUsername(username)) throw new ConflictException("Username is already taken");
        var invitation = patientInvites.consume(request.invitationToken());
        try {
            var account = new UserAccount(username, passwords.encode(request.password()), "PATIENT",
                    java.util.Set.of(invitation.clinicId()), invitation.patientRecordId());
            String displayName = request.displayName();
            String email = request.email();
            String phone = request.phone();
            account.updateProfile(displayName == null || displayName.isBlank() ? username : displayName.trim(),
                    email == null || email.isBlank() ? null : email.trim(),
                    phone == null || phone.isBlank() ? null : phone.trim());
            accounts.saveAndFlush(account);
        } catch (DataIntegrityViolationException duplicate) {
            throw new ConflictException("Username is already taken");
        }
        var user = users.loadUserByUsername(username);
        return new LoginResponse(jwt.issue(user), "Bearer", jwt.expirationSeconds(), user.getUsername(), "PATIENT");
    }
    public LoginResponse login(LoginRequest request) {
        if (request == null || request.username() == null || request.password() == null) {
            throw new BadRequestException("Username and password are required");
        }
        String username = request.username().trim();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, request.password()));
        var user = users.loadUserByUsername(username);
        String role = user.getAuthorities().stream().findFirst().orElseThrow().getAuthority().replace("ROLE_", "");
        return new LoginResponse(jwt.issue(user), "Bearer", jwt.expirationSeconds(), user.getUsername(), role);
    }
}
