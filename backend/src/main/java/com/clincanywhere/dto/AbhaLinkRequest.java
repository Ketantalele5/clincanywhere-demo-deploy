package com.clincanywhere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AbhaLinkRequest(@NotBlank @Size(max = 100) String abhaAddress,
                              @NotBlank @Size(max = 200) String verificationReference,
                              boolean patientConsent) { }
