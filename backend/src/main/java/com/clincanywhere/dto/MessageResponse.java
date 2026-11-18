package com.clincanywhere.dto;

import java.time.Instant;

public record MessageResponse(Long id, String clinicId, String sender, String recipient, String body, Instant sentAt) { }
