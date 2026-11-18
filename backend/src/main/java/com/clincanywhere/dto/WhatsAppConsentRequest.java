package com.clincanywhere.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
public record WhatsAppConsentRequest(
        @NotBlank @Pattern(regexp="\\+[1-9][0-9]{7,14}") String phoneNumber,
        @NotBlank @jakarta.validation.constraints.Pattern(regexp="[A-Za-z0-9._:/-]{1,200}") String evidenceReference) { }
