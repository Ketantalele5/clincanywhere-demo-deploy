package com.clincanywhere.dto;

public record PlatformOverviewResponse(long leads, long activeClinics, long openLeads, long qualifiedLeads,
                                       long onboardingTeamMembers) { }
