package com.clincanywhere.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TicketUpdateRequest(@Pattern(regexp = "OPEN|IN_PROGRESS|RESOLVED") String status,
                                  @Size(max = 80) String assignedTo) { }
