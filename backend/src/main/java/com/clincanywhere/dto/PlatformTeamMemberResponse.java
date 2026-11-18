package com.clincanywhere.dto;

public record PlatformTeamMemberResponse(String username, String displayName, String email, String phone,
                                          long leadsInPipeline, long clinicsOnboarded) { }
