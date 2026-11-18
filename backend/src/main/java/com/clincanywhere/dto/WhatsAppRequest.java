package com.clincanywhere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WhatsAppRequest(
        @NotBlank @Pattern(regexp = "\\+[1-9][0-9]{7,14}") String phoneNumber,
        @NotBlank @Size(max = 1000) String message,
        boolean consentConfirmed) { }
