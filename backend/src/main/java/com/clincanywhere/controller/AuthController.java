package com.clincanywhere.controller;

import com.clincanywhere.dto.LoginRequest;
import com.clincanywhere.dto.LoginResponse;
import com.clincanywhere.dto.RegisterRequest;
import com.clincanywhere.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }
    @PostMapping("/login") public LoginResponse login(@Valid @RequestBody LoginRequest request) { return auth.login(request); }
    @PostMapping("/register") public LoginResponse register(@Valid @RequestBody RegisterRequest request) { return auth.register(request); }
}
