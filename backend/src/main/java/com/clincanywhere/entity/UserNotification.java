package com.clincanywhere.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_notifications", indexes = {
        @Index(name = "ix_user_notifications_recipient_order", columnList = "clinic_id,recipient,id")
})
public class UserNotification extends BaseEntity {
    @Column(name = "clinic_id", nullable = false, length = 40)
    private String clinicId;

    @Column(nullable = false, length = 80)
    private String recipient;

    @Column(nullable = false, length = 500)
    private String message;

    protected UserNotification() { }

    public UserNotification(String clinicId, String recipient, String message) {
        this.clinicId = clinicId;
        this.recipient = recipient;
        this.message = message;
    }

    public String getClinicId() { return clinicId; }
    public String getRecipient() { return recipient; }
    public String getMessage() { return message; }
}
