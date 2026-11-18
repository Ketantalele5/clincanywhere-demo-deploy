package com.clincanywhere.dto;

import com.clincanywhere.entity.ClinicLead;
import java.time.Instant;

public record PlatformLeadResponse(Long id, String clinicName, String contactName, String email, String phone,
                                   String city, String clinicType, String message, String status, String source,
                                   String createdBy, String assignedTo, String workspaceId, Instant createdAt) {
    public static PlatformLeadResponse from(ClinicLead lead) {
        return new PlatformLeadResponse(lead.getId(), lead.getClinicName(), lead.getContactName(), lead.getEmail(),
                lead.getPhone(), lead.getCity(), lead.getClinicType(), lead.getMessage(), lead.getStatus(),
                lead.getSource(), lead.getCreatedBy(), lead.getAssignedTo(), lead.getWorkspaceId(), lead.getCreatedAt());
    }
}
