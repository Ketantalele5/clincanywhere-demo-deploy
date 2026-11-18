package com.clincanywhere.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "direct_messages", indexes = {
        @Index(name = "ix_message_conversation", columnList = "clinic_id,sender,recipient,id")
})
public class DirectMessage extends BaseEntity {
    @Column(name = "clinic_id", nullable = false, length = 40)
    private String clinicId;
    @Column(nullable = false, length = 80)
    private String sender;
    @Column(nullable = false, length = 80)
    private String recipient;
    @Column(nullable = false, length = 4000)
    private String body;

    protected DirectMessage() { }
    public DirectMessage(String clinicId, String sender, String recipient, String body) {
        this.clinicId = clinicId;
        this.sender = sender;
        this.recipient = recipient;
        this.body = body;
    }
    public String getClinicId() { return clinicId; }
    public String getSender() { return sender; }
    public String getRecipient() { return recipient; }
    public String getBody() { return body; }
    public void replaceBody(String body) { this.body=body; }
}
