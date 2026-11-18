package com.clincanywhere.dto;

import java.time.Instant;

public record NotificationResponse(String id, String message, Instant createdAt, String clinicId, String recipient) {
    public NotificationResponse(String id, String message, Instant createdAt, String clinicId) { this(id, message, createdAt, clinicId, null); }
    public NotificationResponse(String id, String message, Instant createdAt) { this(id, message, createdAt, null, null); }
}
