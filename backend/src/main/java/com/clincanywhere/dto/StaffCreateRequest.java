package com.clincanywhere.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record StaffCreateRequest(
        @NotBlank @Size(min = 3, max = 80) @Pattern(regexp = "[A-Za-z0-9._-]+") String username,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 120) String displayName,
        @Nullable @Email @Size(max = 160) String email,
        @Nullable @Size(max = 32) String phone,
        @NotBlank @Pattern(regexp = "RECEPTIONIST|DOCTOR|LAB|PHARMACIST|BILLING|INVENTORY") String role) { }
