package com.clincanywhere.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "clinic_leads")
public class ClinicLead extends BaseEntity {
    @Column(name = "clinic_name", nullable = false, length = 120) private String clinicName;
    @Column(name = "contact_name", nullable = false, length = 120) private String contactName;
    @Column(nullable = false, length = 160) private String email;
    @Column(length = 32) private String phone;
    @Column(length = 80) private String city;
    @Column(name = "clinic_type", length = 60) private String clinicType;
    @Column(length = 1000) private String message;
    @Column(nullable = false, length = 20) private String status = "NEW";
    @Column(name = "source", nullable = false, length = 20) private String source = "WEBSITE";
    @Column(name = "created_by", length = 80) private String createdBy;
    @Column(name = "assigned_to", length = 80) private String assignedTo;
    @Column(name = "workspace_id", length = 40) private String workspaceId;

    protected ClinicLead() { }
    public ClinicLead(String clinicName, String contactName, String email, @Nullable String phone, @Nullable String city,
                      @Nullable String clinicType, @Nullable String message) {
        this.clinicName = clinicName; this.contactName = contactName; this.email = email;
        this.phone = phone; this.city = city; this.clinicType = clinicType; this.message = message;
    }
    public String getClinicName() { return clinicName; }
    public String getContactName() { return contactName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getCity() { return city; }
    public String getClinicType() { return clinicType; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
    public String getSource() { return source; }
    public String getCreatedBy() { return createdBy; }
    public String getAssignedTo() { return assignedTo; }
    public String getWorkspaceId() { return workspaceId; }
    public void setAttribution(String source, String createdBy, String assignedTo) {
        this.source = source; this.createdBy = createdBy; this.assignedTo = assignedTo;
    }
    public void assignTo(String username) { this.assignedTo = username; }
    public void updateStatus(String status) { this.status = status; }
    public void markConverted(String clinicId) { this.workspaceId = clinicId; this.status = "CONVERTED"; }
}
