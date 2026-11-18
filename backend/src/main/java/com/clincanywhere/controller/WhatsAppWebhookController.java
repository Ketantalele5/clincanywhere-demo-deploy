package com.clincanywhere.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Meta webhook verification boundary. Never logs the message body or patient data. */
@RestController
@RequestMapping("/api/webhooks/whatsapp")
public class WhatsAppWebhookController {
    private final String verifyToken;
    private final String appSecret;
    private final ObjectMapper mapper;
    private final com.clincanywhere.service.WhatsAppReceiptService receipts;
    private final com.clincanywhere.service.WhatsAppConsentService consents;
    private final String clinicId;
    private final String phoneNumberId;
    public WhatsAppWebhookController(@Value("${app.whatsapp.webhook-verify-token:}") String verifyToken,
                                     @Value("${app.whatsapp.app-secret:}") String appSecret,
                                     @Value("${app.whatsapp.enabled:false}") boolean enabled,
                                     @Value("${app.whatsapp.clinic-id:}") String clinicId,
                                     @Value("${WHATSAPP_PHONE_NUMBER_ID:}") String phoneNumberId,
                                     ObjectMapper mapper, com.clincanywhere.service.WhatsAppReceiptService receipts,
                                     com.clincanywhere.service.WhatsAppConsentService consents) {
        if (enabled && (verifyToken.isBlank() || appSecret.isBlank() || clinicId.isBlank() || phoneNumberId.isBlank()))
            throw new IllegalStateException("WhatsApp webhook verify token, app secret, clinic id, and sender id are required when WhatsApp is enabled");
        this.verifyToken=verifyToken; this.appSecret=appSecret;
        this.mapper=mapper; this.receipts=receipts;
        this.consents=consents; this.clinicId=clinicId;
        this.phoneNumberId=phoneNumberId;
    }

    @GetMapping
    public ResponseEntity<String> verify(@RequestParam(name="hub.mode", required=false) @Nullable String mode,
            @RequestParam(name="hub.verify_token", required=false) @Nullable String token,
            @RequestParam(name="hub.challenge", required=false) @Nullable String challenge) {
        if ("subscribe".equals(mode) && !verifyToken.isBlank() && constantEquals(verifyToken, token) && challenge != null)
            return ResponseEntity.ok(challenge);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("");
    }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody String rawBody,
            @RequestHeader(name="X-Hub-Signature-256", required=false) @Nullable String signature) {
        if (appSecret.isBlank() || !validSignature(rawBody, signature)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            JsonNode payload=mapper.readTree(rawBody);
            if (payload == null || !payload.isObject()) return ResponseEntity.badRequest().build();
            receipts.accept(payload,phoneNumberId);
            processOptOuts(payload);
            return ResponseEntity.ok().build();
        } catch (com.fasterxml.jackson.core.JsonProcessingException malformed) {
            return ResponseEntity.badRequest().build();
        }
    }

    private void processOptOuts(JsonNode payload) {
        if (clinicId.isBlank()) return;
        for (JsonNode entry : payload.path("entry")) for (JsonNode change : entry.path("changes"))
            if (phoneNumberId.equals(change.path("value").path("metadata").path("phone_number_id").asText()))
            for (JsonNode message : change.path("value").path("messages")) {
                String from=message.path("from").isTextual()?message.path("from").asText():null;
                String body=message.path("text").path("body").isTextual()?message.path("text").path("body").asText().trim():null;
                if (from==null || body==null || !body.matches("(?i)(STOP|UNSUBSCRIBE|END|CANCEL|QUIT)")) continue;
                String phone="+"+from.replaceAll("[^0-9]","");
                consents.revoke(clinicId,phone,"META_INBOUND_OPT_OUT","provider-webhook");
            }
    }

    private boolean validSignature(String body, String signature) {
        if (signature == null || !signature.startsWith("sha256=")) return false;
        try {
            Mac mac=Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            StringBuilder expected=new StringBuilder("sha256=");
            for (byte b : mac.doFinal(body.getBytes(StandardCharsets.UTF_8))) expected.append(String.format("%02x", b));
            return constantEquals(expected.toString(), signature);
        } catch (Exception failure) { throw new IllegalStateException("Webhook signature verification unavailable", failure); }
    }
    private boolean constantEquals(String a, @Nullable String b) {
        return b != null && MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
