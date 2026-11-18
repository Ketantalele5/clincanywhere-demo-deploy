package com.clincanywhere.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;

public record PlatformLeadStatusRequest(@NotBlank @Pattern(regexp = "NEW|CONTACTED|QUALIFIED|LOST") String status) { }
