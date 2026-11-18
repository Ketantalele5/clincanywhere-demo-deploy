package com.clincanywhere.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record FacilityProfileRequest(@Nullable @Size(max = 80) String hfrId,
                                     @Nullable @Pattern(regexp = "NOT_REGISTERED|PENDING_VERIFICATION|VERIFIED") String hfrStatus,
                                     @Nullable @Size(max = 50) String facilityType,
                                     @Nullable @Size(max = 1000) String services,
                                     @Nullable @Size(max = 500) String address) { }
