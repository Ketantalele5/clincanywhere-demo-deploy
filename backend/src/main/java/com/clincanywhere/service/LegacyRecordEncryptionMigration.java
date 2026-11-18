package com.clincanywhere.service;

import com.clincanywhere.repository.CareRecordRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.jspecify.annotations.Nullable;

/** Converts pre-encryption record rows in bounded batches during rollout. */
@Component
public class LegacyRecordEncryptionMigration implements ApplicationRunner {
    private final CareRecordRepository records;
    private final FieldEncryptionService encryption;
    private final com.clincanywhere.repository.DirectMessageRepository messages;
    private final com.clincanywhere.repository.WhatsAppOutboxRepository outbox;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    public LegacyRecordEncryptionMigration(CareRecordRepository records, FieldEncryptionService encryption,
            com.clincanywhere.repository.DirectMessageRepository messages,
            com.clincanywhere.repository.WhatsAppOutboxRepository outbox,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.records=records; this.encryption=encryption; this.messages=messages; this.outbox=outbox; this.objectMapper=objectMapper;
    }
    @Override
    public void run(@Nullable ApplicationArguments args) {
        int changed=0;
        while (true) {
            var pageable = PageRequest.of(0, 200);
            var batch=(encryption.enabled() ? records.findRecordsNeedingEncryptionOrSearchIndexes(pageable)
                    : records.findRecordsNeedingSearchIndexes(pageable));
            if (batch.isEmpty()) break;
            for (var record : batch) {
                String value=record.getPayloadJson();
                String plain = encryption.decrypt(value);
                if (encryption.enabled() && !encryption.isEncrypted(value)) { record.replacePayload(encryption.encrypt(value)); changed++; }
                if (record.getSearchId().isBlank()) {
                    try {
                        var fields = objectMapper.readValue(plain,
                                new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String,Object>>() {});
                        String name = java.util.stream.Stream.of("name", "patient", "patientName", "email")
                                .map(indexedField -> fields.get(indexedField)).filter(java.util.Objects::nonNull).map(searchValue -> searchValue.toString()).findFirst()
                                .map(nameValue -> encryption.fingerprint(nameValue.trim().toLowerCase(java.util.Locale.ROOT))).orElse("");
                        String phone = java.util.Optional.ofNullable(fields.get("phone")).map(rawPhone -> rawPhone.toString())
                                .map(normalizedPhone -> encryption.fingerprint(normalizedPhone.trim().toLowerCase(java.util.Locale.ROOT))).orElse("");
                        record.replaceSearchIndexes(name, phone,
                                encryption.fingerprint(record.getExternalId().toLowerCase(java.util.Locale.ROOT)));
                    } catch (java.io.IOException invalid) {
                        throw new IllegalStateException("Stored care record is invalid", invalid);
                    }
                }
            }
            records.saveAll(batch);
            records.flush();
        }
        if (!encryption.enabled()) return;
        int protectedMessages=0;
        int page=0;
        while (true) {
            var batch=messages.findAll(PageRequest.of(page++, 200)).getContent();
            if (batch.isEmpty()) break;
            for (var message : batch) {
                String value=message.getBody();
                if (!encryption.isEncrypted(value)) { message.replaceBody(encryption.encrypt(value)); protectedMessages++; }
            }
            messages.saveAll(batch); messages.flush();
            if (batch.size()<200) break;
        }
        int protectedOutbox=0;
        page=0;
        while (true) {
            var batch=outbox.findAll(PageRequest.of(page++, 200)).getContent();
            if (batch.isEmpty()) break;
            for (var item : batch) {
                String phone=item.getPhoneNumber(), body=item.getMessage();
                String protectedPhone=encryption.isEncrypted(phone) ? phone : encryption.encrypt(phone);
                String protectedBody=encryption.isEncrypted(body) ? body : encryption.encrypt(body);
                if (!protectedPhone.equals(phone) || !protectedBody.equals(body) || item.getPhoneFingerprint()==null) {
                    item.protectContact(protectedPhone, protectedBody);
                    if (item.getPhoneFingerprint()==null) item.setPhoneFingerprint(encryption.fingerprint(encryption.decrypt(protectedPhone)));
                    protectedOutbox++;
                }
            }
            outbox.saveAll(batch); outbox.flush();
            if (batch.size()<200) break;
        }
        if (changed+protectedMessages+protectedOutbox>0) org.slf4j.LoggerFactory.getLogger(getClass())
                .info("Encrypted legacy sensitive rows careRecords={} directMessages={} whatsappOutbox={}", changed, protectedMessages, protectedOutbox);
    }
}
