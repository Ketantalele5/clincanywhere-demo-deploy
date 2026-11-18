package com.clincanywhere.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** AES-256-GCM envelope for sensitive fields. The key must come from a secret manager. */
@Component
public class FieldEncryptionService {
    private static final String PREFIX = "enc:v1:";
    private static final int NONCE_BYTES = 12;
    private final SecretKeySpec key;
    private final SecretKeySpec fingerprintKey;
    private final SecureRandom random = new SecureRandom();

    @org.springframework.beans.factory.annotation.Autowired
    public FieldEncryptionService(@Value("${app.data-encryption.key:}") String encodedKey,
                                  @Value("${app.phone-fingerprint.key:}") String encodedFingerprintKey,
                                  @Value("${spring.profiles.active:}") String profile) {
        if (encodedKey == null || encodedKey.isBlank()) {
            if (profile.contains("prod")) throw new IllegalStateException("APP_DATA_ENCRYPTION_KEY is required in production");
            this.key = null;
        } else {
            byte[] bytes;
            try { bytes = Base64.getDecoder().decode(encodedKey); }
            catch (IllegalArgumentException invalid) { throw new IllegalStateException("APP_DATA_ENCRYPTION_KEY must be base64", invalid); }
            if (bytes.length != 32) throw new IllegalStateException("APP_DATA_ENCRYPTION_KEY must decode to exactly 32 bytes");
            this.key = new SecretKeySpec(bytes, "AES");
        }
        if (encodedFingerprintKey == null || encodedFingerprintKey.isBlank()) {
            if (profile.contains("prod")) throw new IllegalStateException("APP_PHONE_FINGERPRINT_KEY is required in production");
            this.fingerprintKey=new SecretKeySpec("development-only-fingerprint-key".getBytes(StandardCharsets.UTF_8),"HmacSHA256");
        } else {
            byte[] bytes;
            try { bytes=Base64.getDecoder().decode(encodedFingerprintKey); }
            catch (IllegalArgumentException invalid) { throw new IllegalStateException("APP_PHONE_FINGERPRINT_KEY must be base64",invalid); }
            if (bytes.length != 32) throw new IllegalStateException("APP_PHONE_FINGERPRINT_KEY must decode to exactly 32 bytes");
            this.fingerprintKey=new SecretKeySpec(bytes,"HmacSHA256");
        }
    }
    public FieldEncryptionService(String encodedKey,String profile) { this(encodedKey,"",profile); }

    public String encrypt(String value) {
        if (value == null || key == null) return value;
        try {
            byte[] nonce = new byte[NONCE_BYTES]; random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            byte[] ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getEncoder().encodeToString(ByteBuffer.allocate(nonce.length + ciphertext.length).put(nonce).put(ciphertext).array());
        } catch (Exception failure) { throw new IllegalStateException("Sensitive field encryption failed", failure); }
    }
    public boolean enabled() { return key != null; }
    public boolean isEncrypted(String value) { return value != null && value.startsWith(PREFIX); }
    public String fingerprint(String value) {
        try {
            javax.crypto.Mac mac=javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(fingerprintKey);
            return java.util.HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception failure) { throw new IllegalStateException("Sensitive identifier fingerprinting failed",failure); }
    }

    public String decrypt(String value) {
        if (value == null || !value.startsWith(PREFIX)) return value;
        if (key == null) throw new IllegalStateException("Encrypted data exists but APP_DATA_ENCRYPTION_KEY is not configured");
        try {
            byte[] packed = Base64.getDecoder().decode(value.substring(PREFIX.length()));
            if (packed.length <= NONCE_BYTES) throw new IllegalArgumentException("Invalid encrypted field");
            ByteBuffer buffer = ByteBuffer.wrap(packed); byte[] nonce = new byte[NONCE_BYTES]; buffer.get(nonce);
            byte[] ciphertext = new byte[buffer.remaining()]; buffer.get(ciphertext);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception failure) { throw new IllegalStateException("Sensitive field decryption failed", failure); }
    }
}
