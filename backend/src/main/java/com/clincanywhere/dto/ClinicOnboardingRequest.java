package com.clincanywhere.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record ClinicOnboardingRequest(
        @NotBlank @Size(max = 120) String clinicName,
        @NotBlank @Size(max = 120) String contactName,
        @NotBlank @Email @Size(max = 160) String email,
        @Nullable @Size(max = 32) String phone,
        @Nullable @Size(max = 80) String city,
        @Nullable @Size(max = 60) String clinicType,
        @Nullable @Size(max = 1000) String message) { }
