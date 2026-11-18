package com.clincanywhere.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.Column;
import java.util.HashSet;
import java.util.Set;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "user_accounts")
public class UserAccount extends BaseEntity {
    @Column(nullable = false, unique = true, length = 80)
    private String username;
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
    @Column(nullable = false, length = 30)
    private String role;
    @Column(nullable = false)
    private boolean enabled = true;
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_clinic_memberships", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "clinic_id", nullable = false, length = 40)
    private Set<String> clinicIds = new HashSet<>();
    @Column(name = "patient_record_id", length = 80)
    private String patientRecordId;
    @Column(name = "display_name", length = 120)
    private String displayName;
    @Column(length = 160)
    private String email;
    @Column(length = 32)
    private String phone;

    protected UserAccount() { }
    public UserAccount(String username, String passwordHash, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }
    public UserAccount(String username, String passwordHash, String role, Set<String> clinicIds) {
        this(username, passwordHash, role);
        this.clinicIds = clinicIds == null ? new HashSet<>() : new HashSet<>(clinicIds);
    }
    public UserAccount(String username, String passwordHash, String role, Set<String> clinicIds, String patientRecordId) {
        this(username, passwordHash, role, clinicIds);
        this.patientRecordId = patientRecordId;
    }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public boolean isEnabled() { return enabled; }
    public Set<String> getClinicIds() { return clinicIds == null ? Set.of() : Set.copyOf(clinicIds); }
    public void assignClinics(Set<String> clinicIds) { this.clinicIds = clinicIds == null ? new HashSet<>() : new HashSet<>(clinicIds); }
    public @Nullable String getPatientRecordId() { return patientRecordId; }
    public @Nullable String getDisplayName() { return displayName; }
    public @Nullable String getEmail() { return email; }
    public @Nullable String getPhone() { return phone; }
    public void updateProfile(String displayName, @Nullable String email, @Nullable String phone) {
        this.displayName = displayName;
        this.email = email;
        this.phone = phone;
    }
    public void changePassword(String passwordHash) { this.passwordHash = passwordHash; }
}
