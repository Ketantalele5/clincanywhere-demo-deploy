package com.clincanywhere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PlatformConvertLeadRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9-]{2,40}") String clinicId,
                                         @NotBlank @Size(min = 3, max = 80) @Pattern(regexp = "[A-Za-z0-9._-]+") String adminUsername,
                                         @NotBlank @Size(min = 8, max = 72) String adminPassword) { }
