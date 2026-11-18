package com.clincanywhere.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfessionalProfileRequest(@Size(max = 80) String hprId,
                                         @Pattern(regexp = "NOT_REGISTERED|PENDING_VERIFICATION|VERIFIED") String status) { }
