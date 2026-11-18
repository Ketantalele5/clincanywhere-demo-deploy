package com.clincanywhere.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record ProfileUpdateRequest(
        @NotBlank @Size(max = 120) String displayName,
        @Nullable @Email @Size(max = 160) String email,
        @Nullable @Size(max = 32) String phone) { }
