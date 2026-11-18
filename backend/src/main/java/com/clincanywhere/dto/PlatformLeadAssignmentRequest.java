package com.clincanywhere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlatformLeadAssignmentRequest(@NotBlank @Size(max = 80) String assignedTo) { }
