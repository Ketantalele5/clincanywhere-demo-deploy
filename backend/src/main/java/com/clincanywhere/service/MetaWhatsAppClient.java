package com.clincanywhere.service;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Semaphore;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.http.HttpClient;
import java.util.Objects;

@Component
@ConditionalOnProperty(name = "app.whatsapp.enabled", havingValue = "true")
public class MetaWhatsAppClient implements WhatsAppClient {
    private final RestClient client;
    private final String phoneNumberId;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicLong openedAt = new AtomicLong();
    private final Semaphore halfOpenProbe = new Semaphore(1);
    public MetaWhatsAppClient(@Value("${app.whatsapp.api-url:https://graph.facebook.com/v23.0}") String apiUrl,
                              @Value("${app.whatsapp.phone-number-id:}") String phoneNumberId,
                              @Value("${app.whatsapp.api-token:}") String token) {
        String configuredPhoneNumberId = Objects.requireNonNull(phoneNumberId, "WhatsApp phone number id");
        String configuredToken = Objects.requireNonNull(token, "WhatsApp API token");
        String configuredApiUrl = Objects.requireNonNull(apiUrl, "WhatsApp API URL");
        if (configuredPhoneNumberId.isBlank() || configuredToken.isBlank()) {
            throw new IllegalStateException("WHATSAPP_API_TOKEN and WHATSAPP_PHONE_NUMBER_ID are required when WhatsApp is enabled");
        }
        this.phoneNumberId = configuredPhoneNumberId;
        var requestFactory = new JdkClientHttpRequestFactory(java.util.Objects.requireNonNull(java.util.Objects.requireNonNull(HttpClient.newBuilder()).connectTimeout(java.util.Objects.requireNonNull(Duration.ofSeconds(3))).build()));
        requestFactory.setReadTimeout(java.util.Objects.requireNonNull(Duration.ofSeconds(8)));
        this.client = RestClient.builder().baseUrl(configuredApiUrl).requestFactory(requestFactory).defaultHeader("Authorization", "Bearer " + configuredToken).build();
    }
    @Override
    public String sendText(String phoneNumber, String message) {
        Objects.requireNonNull(phoneNumber, "WhatsApp recipient phone number");
        Objects.requireNonNull(message, "WhatsApp message");
        long open = openedAt.get();
        boolean probe = false;
        if (open > 0) {
            if (System.currentTimeMillis() - open < 30_000 || !halfOpenProbe.tryAcquire()) {
                throw new IllegalStateException("WhatsApp circuit is open; delivery will retry later");
            }
            probe = true;
        }
        try {
            JsonNode response = client.post().uri("/{phoneNumberId}/messages", phoneNumberId)
                .contentType(java.util.Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .body(java.util.Objects.requireNonNull(Map.of("messaging_product", "whatsapp", "recipient_type", "individual", "to", phoneNumber,
                        "type", "text", "text", java.util.Objects.requireNonNull(Map.of("preview_url", false, "body", message)))))
                .retrieve().body(JsonNode.class);
            JsonNode id = response == null ? null : response.path("messages").path(0).path("id");
            if (id == null || id.isMissingNode() || !id.isTextual() || id.asText().isBlank())
                throw new IllegalStateException("WhatsApp provider response did not include a message id");
            consecutiveFailures.set(0);
            openedAt.set(0);
            return id.asText();
        } catch (RestClientException | IllegalStateException failure) {
            int failures = consecutiveFailures.incrementAndGet();
            if (probe || failures >= 3) openedAt.set(System.currentTimeMillis());
            throw failure;
        } finally {
            if (probe) halfOpenProbe.release();
        }
    }
}
