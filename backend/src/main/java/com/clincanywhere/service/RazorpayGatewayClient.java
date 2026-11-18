package com.clincanywhere.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Razorpay Orders API boundary. Secrets stay on the server and provider bodies are never logged. */
@Component
public class RazorpayGatewayClient {
    private static final String BASE_URL = "https://api.razorpay.com/v1";
    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public RazorpayGatewayClient(@Value("${app.payments.razorpay.key-id:}") String keyId,
                                 @Value("${app.payments.razorpay.key-secret:}") String keySecret,
                                 @Value("${app.payments.razorpay.webhook-secret:}") String webhookSecret,
                                 ObjectMapper mapper) {
        this.keyId = keyId == null ? "" : keyId.trim();
        this.keySecret = keySecret == null ? "" : keySecret;
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret;
        this.mapper = mapper;
    }

    public boolean enabled() { return !keyId.isBlank() && !keySecret.isBlank() && !webhookSecret.isBlank(); }
    public String keyId() { return keyId; }
    public boolean webhookEnabled() { return !webhookSecret.isBlank(); }

    public String createOrder(long amountPaise, String receipt, long attemptId) {
        requireEnabled();
        try {
            JsonNode order = request("POST", "/orders", Map.of(
                    "amount", amountPaise,
                    "currency", "INR",
                    "receipt", receipt,
                    "notes", Map.of("payment_attempt_id", Long.toString(attemptId))));
            String orderId = order.path("id").asText("");
            if (!orderId.matches("order_[A-Za-z0-9]+")) throw unavailable();
            if (order.path("amount").asLong(-1) != amountPaise || !"INR".equals(order.path("currency").asText())) {
                throw unavailable();
            }
            return orderId;
        } catch (ResponseStatusException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw unavailable();
        }
    }

    public CapturedPayment fetchPayment(String paymentId) {
        requireEnabled();
        if (paymentId == null || !paymentId.matches("pay_[A-Za-z0-9]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid gateway payment reference");
        }
        JsonNode payment = request("GET", "/payments/" + paymentId, null);
        return new CapturedPayment(payment.path("id").asText(""),
                payment.path("order_id").asText(""), payment.path("status").asText(""),
                payment.path("amount").asLong(-1), payment.path("currency").asText(""));
    }

    public boolean verifyCheckoutSignature(String trustedOrderId, String paymentId, String signature) {
        return verifyHmac(trustedOrderId + "|" + paymentId, keySecret, signature);
    }

    public boolean verifyWebhookSignature(byte[] rawBody, String signature) {
        return !webhookSecret.isBlank() && verifyHmac(rawBody, webhookSecret, signature);
    }

    private JsonNode request(String method, String path, Object body) {
        try {
            String token = Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Basic " + token)
                    .header("Accept", "application/json");
            if (body == null) builder.GET();
            else builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw unavailable();
            JsonNode parsed = mapper.readTree(response.body());
            if (parsed == null || !parsed.isObject()) throw unavailable();
            return parsed;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (IOException | RuntimeException failure) {
            if (failure instanceof ResponseStatusException status) throw status;
            throw unavailable();
        }
    }

    private void requireEnabled() {
        if (!enabled()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Online payments are not configured for this clinic");
    }

    private boolean verifyHmac(String message, String secret, String provided) {
        return verifyHmac(message == null ? null : message.getBytes(StandardCharsets.UTF_8), secret, provided);
    }

    private boolean verifyHmac(byte[] message, String secret, String provided) {
        if (message == null || secret == null || secret.isBlank() || provided == null || !provided.matches("[A-Fa-f0-9]{64}")) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = HexFormat.of().formatHex(mac.doFinal(message))
                    .getBytes(StandardCharsets.US_ASCII);
            return MessageDigest.isEqual(expected, provided.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
        } catch (Exception failure) {
            throw new IllegalStateException("Payment signature verification unavailable", failure);
        }
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Razorpay could not complete the payment request. No payment status was changed.");
    }

    public record CapturedPayment(String id, String orderId, String status, long amountPaise, String currency) { }
}
