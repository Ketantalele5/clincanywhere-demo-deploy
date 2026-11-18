package com.clincanywhere.dto;

import java.util.List;
import org.jspecify.annotations.Nullable;

public record AccountProfileResponse(String username, String displayName, @Nullable String email, @Nullable String phone,
                                     String role, List<String> clinicIds) { }
