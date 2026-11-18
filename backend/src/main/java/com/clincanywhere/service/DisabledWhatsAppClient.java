package com.clincanywhere.service;

import com.clincanywhere.common.exception.BadRequestException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.whatsapp.enabled", havingValue = "false", matchIfMissing = true)
public class DisabledWhatsAppClient implements WhatsAppClient {
    @Override public String sendText(String phoneNumber, String message) {
        throw new BadRequestException("WhatsApp delivery is disabled; configure provider credentials and recipient consent");
    }
}
