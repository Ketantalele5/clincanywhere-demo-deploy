package com.clincanywhere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PatientInviteRequest(@NotBlank @Size(max = 80) String patientRecordId) { }
