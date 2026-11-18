package com.clincanywhere.service;

import com.clincanywhere.dto.MessageResponse;

/** Published inside the message transaction and delivered only after commit. */
public record DirectMessageCreated(String clinicId, String sender, String recipient, MessageResponse message) { }
