package com.clincanywhere.entity;
import jakarta.persistence.*;
@Entity @Table(name="patient_notification_outbox")
public class PatientNotificationItem extends BaseEntity {
 @Column(name="clinic_id",nullable=false,length=40) private String clinicId;
 @Column(name="requested_by",nullable=false,length=80) private String requestedBy;
 @Column(nullable=false,length=12) private String channel;
 @Column(nullable=false,length=400) private String destination;
 @Column(name="message_body",nullable=false,length=5000) private String message;
 @Column(nullable=false,length=20) private String status="PENDING";
 @Column(name="attempt_count",nullable=false) private int attempts;
 @Column(name="provider_reference",length=120) private String providerReference;
 @Column(name="last_error",length=300) private String lastError;
 protected PatientNotificationItem(){}
 public PatientNotificationItem(String clinic,String actor,String channel,String destination,String message){this.clinicId=clinic;this.requestedBy=actor;this.channel=channel;this.destination=destination;this.message=message;}
 public void delivered(String ref){status="ACCEPTED";providerReference=ref;lastError=null;}
 public void failed(String reason){status="FAILED";attempts++;lastError=reason==null?"Provider delivery failed":reason.substring(0,Math.min(300,reason.length()));}
 public Long id(){return getId();} public String status(){return status;} public String channel(){return channel;} public String destination(){return destination;} public String message(){return message;}
}
