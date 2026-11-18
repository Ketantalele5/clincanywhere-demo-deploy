package com.clincanywhere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketCreateRequest(@Size(max = 80) String patientId,
                                  @NotBlank @Size(max = 160) String subject,
                                  @NotBlank @Size(max = 2000) String message) { }
