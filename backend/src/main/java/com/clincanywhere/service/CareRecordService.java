package com.clincanywhere.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.math.BigDecimal;
import java.math.RoundingMode;

import com.clincanywhere.entity.CareRecord;
import com.clincanywhere.repository.CareRecordRepository;
import com.clincanywhere.mapper.CareRecordMapper;
import com.clincanywhere.dto.RecordRequest;
import com.clincanywhere.dto.RecordResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.slf4j.MDC;
import com.clincanywhere.common.exception.NotFoundException;
import com.clincanywhere.common.exception.ConflictException;
import com.clincanywhere.common.exception.BadRequestException;
import com.clincanywhere.security.RecordAuthorization;
import com.clincanywhere.security.RecordScope;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.PageRequest;

@Service
public class CareRecordService {
    private final CareRecordRepository repository;
    private final com.clincanywhere.repository.BillingPaymentRepository billingPayments;
    private final CareRecordMapper mapper;
    private final FieldEncryptionService encryption;
    private final com.clincanywhere.repository.RecordAuditRepository audit;
    private final PrescriptionPolicy prescriptionPolicy;
    private final CurrentActorProvider actorProvider;
    private final RecordScope scope;
    private final RecordAuthorization authorization;

    public CareRecordService(CareRecordRepository repository, com.clincanywhere.repository.BillingPaymentRepository billingPayments,
                             CareRecordMapper mapper, FieldEncryptionService encryption,
                             com.clincanywhere.repository.RecordAuditRepository audit, PrescriptionPolicy prescriptionPolicy,
                             CurrentActorProvider actorProvider, RecordScope scope, RecordAuthorization authorization) {
        this.repository = repository;
        this.billingPayments = billingPayments;
        this.mapper = mapper;
        this.encryption = encryption;
        this.audit = audit;
        this.prescriptionPolicy = prescriptionPolicy;
        this.actorProvider = actorProvider;
        this.scope = scope;
        this.authorization = authorization;
    }

    /** Read records only after checking the authenticated caller's clinic membership and role. */
    public List<RecordResponse> list(String clinicId, String category, Authentication authentication) {
        return list(clinicId, category, authentication, 0, 50);
    }

    /** Resolves the single clinic profile without materializing every settings record. */
    public java.util.Optional<RecordResponse> clinicProfile(String clinicId, Authentication authentication) {
        scope.requireScope(clinicId, "settings");
        scope.requireMember(clinicId, authentication);
        return repository.findFirstByClinicIdAndCategoryAndExternalId(clinicId, "settings", "clinic-profile")
                .map(this::toResponse);
    }

    public List<String> allClinicIds() {
        return repository.findAllClinicIds();
    }

    public List<RecordResponse> list(String clinicId, String category, Authentication authentication, int page, int size) {
        requireReadAccess(clinicId, category, authentication);
        String patientId = scope.patientRecordId(authentication.getName());
        if (patientId != null) {
            if (!"patients".equals(category)) throw new AccessDeniedException("Patients can only access their own patient record");
            return List.of(getScoped(clinicId, category, patientId));
        }
        audit.recordListAccess(clinicId, category, authentication.getName(), MDC.get(com.clincanywhere.common.CorrelationIdFilter.MDC_KEY));
        return repository.findByClinicIdAndCategoryOrderByIdAsc(clinicId, category, PageRequest.of(page, size))
                .stream().map(this::toResponse).toList();
    }

    public List<RecordResponse> search(String clinicId, String category, Authentication authentication, String query, int size) {
        requireReadAccess(clinicId, category, authentication);
        String patientId = scope.patientRecordId(authentication.getName());
        if (patientId != null) {
            if (!"patients".equals(category)) throw new AccessDeniedException("Patients can only access their own patient record");
            return List.of(getScoped(clinicId, category, patientId));
        }
        audit.recordListAccess(clinicId, category, authentication.getName(), MDC.get(com.clincanywhere.common.CorrelationIdFilter.MDC_KEY));
        String rawQuery = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        String normalized = rawQuery.isEmpty() ? "" : encryption.fingerprint(rawQuery);
        if (normalized.isEmpty()) return repository.findByClinicIdAndCategoryOrderByIdAsc(clinicId, category, PageRequest.of(0, size))
                .stream().map(this::toResponse).toList();
        return repository.searchByClinicAndCategory(clinicId, category, normalized, PageRequest.of(0, size))
                .stream().map(this::toResponse).toList();
    }

    public RecordResponse get(String clinicId, String category, String externalId, Authentication authentication) {
        requireReadAccess(clinicId, category, authentication);
        String patientId = scope.patientRecordId(authentication.getName());
        if (patientId != null && (!"patients".equals(category) || !patientId.equals(externalId))) {
            throw new AccessDeniedException("Patients can only access their own record");
        }
        return getScoped(clinicId, category, externalId);
    }

    /** Unscoped helpers remain only for explicit trusted internal workflows; do not expose from controllers. */
    @Deprecated(forRemoval = true)
    public List<RecordResponse> list(String clinicId, String category) {
        throw new AccessDeniedException("Authenticated clinic context is required");
    }

    @Deprecated(forRemoval = true)
    public RecordResponse get(String clinicId, String category, String externalId) {
        throw new AccessDeniedException("Authenticated clinic context is required");
    }

    private RecordResponse getScoped(String clinicId, String category, String externalId) {
        audit(clinicId, category, externalId, "READ");
        return toResponse(find(clinicId, category, externalId));
    }

    private void requireReadAccess(String clinicId, String category, Authentication authentication) {
        scope.requireScope(clinicId, category);
        scope.requireMember(clinicId, authentication);
        if (!authorization.canRead(category, authentication)) throw new AccessDeniedException("Role cannot read this category");
    }

    @Transactional
    public RecordResponse lockForUpdate(String clinicId, String category, String externalId) {
        requireTrustedWorkflowAccess(clinicId, category);
        return repository.lockByClinicAndCategoryAndExternalId(clinicId, category, externalId)
                .map(this::toResponse).orElseThrow(() -> new NotFoundException("Record not found"));
    }

    @Transactional
    public RecordResponse lockByPayloadName(String clinicId, String category, String name) {
        requireTrustedWorkflowAccess(clinicId, category);
        if (name == null || name.isBlank()) throw new BadRequestException("Record name is required");
        String searchName = encryption.fingerprint(name.trim().toLowerCase(java.util.Locale.ROOT));
        return repository.lockByClinicCategoryAndSearchName(clinicId, category, searchName).stream()
                .findFirst().map(this::toResponse).orElseThrow(() -> new NotFoundException("Record not found"));
    }

    @Transactional
    @CacheEvict(value = "records", key = "#clinicId + ':' + #category")
    public RecordResponse create(String clinicId, String category, RecordRequest request) {
        throw new AccessDeniedException("Authenticated clinic context is required");
    }

    @Transactional
    @CacheEvict(value = "records", key = "#clinicId + ':' + #category")
    public RecordResponse create(String clinicId, String category, RecordRequest request, String actor, boolean doctor) {
        requireWriteAccess(clinicId, category, actor);
        return createInternal(clinicId, category, request, actor, doctor);
    }

    /** Used by controlled clinic provisioning before the first clinic admin exists. */
    @Transactional
    public RecordResponse createProvisioningRecord(String clinicId, String category, RecordRequest request) {
        if (!"settings".equals(category)) throw new AccessDeniedException("Provisioning may only create clinic settings");
        if (request == null || request.fields() == null || !"clinic-profile".equals(request.fields().get("id"))) {
            throw new AccessDeniedException("Provisioning may only create the initial clinic profile");
        }
        return createInternal(clinicId, category, request, actorProvider.currentActor(), false);
    }

    private RecordResponse createInternal(String clinicId, String category, RecordRequest request, String actor, boolean doctor) {
        if (request == null || request.fields() == null) throw new BadRequestException("Record payload cannot be empty");
        Map<String, Object> payload = prescriptionPolicy.prepareForWrite(category, doctor, actor, request.fields());
        if (payload == null || payload.isEmpty()) throw new BadRequestException("Record payload cannot be empty");
        validatePayload(payload);
        Map<String, Object> values = new LinkedHashMap<>(payload);
        if ("billing".equals(category)) {
            values.put("amount", invoiceAmount(values.get("amount")));
            values.put("paid", BigDecimal.ZERO.setScale(2));
            values.put("status", "Pending");
        }
        Object requestedId = values.get("id");
        String externalId = requestedId == null
                ? category.substring(0, Math.min(2, category.length())).toUpperCase() + "-"
                    + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
                : requestedId.toString().trim();
        if (externalId.isBlank()) {
            throw new BadRequestException("Record id cannot be blank");
        }
        if (repository.findByClinicIdAndCategoryAndExternalId(clinicId, category, externalId).isPresent()) {
            throw new ConflictException("A record with this id already exists");
        }
        values.put("id", externalId);
        CareRecord record = mapper.toEntity(clinicId, category, externalId, values);
        record.replacePayload(encryption.encrypt(record.getPayloadJson()));
        record.replaceSearchIndexes(searchHash(values, List.of("name", "patient", "patientName", "email")),
                searchHash(values, List.of("phone")), encryption.fingerprint(externalId.toLowerCase(java.util.Locale.ROOT)));
        repository.save(record);
        audit(clinicId, category, externalId, "CREATE");
        return toResponse(record);
    }

    @Transactional
    @CacheEvict(value = "records", key = "#clinicId + ':' + #category")
    public RecordResponse update(String clinicId, String category, String externalId, RecordRequest request) {
        throw new AccessDeniedException("Authenticated clinic context is required");
    }

    @Transactional
    @CacheEvict(value = "records", key = "#clinicId + ':' + #category")
    public RecordResponse update(String clinicId, String category, String externalId, RecordRequest request, String actor, boolean doctor) {
        requireWriteAccess(clinicId, category, actor);
        return updateInternal(clinicId, category, externalId, request, actor, doctor);
    }

    /** Pharmacy dispensing uses this only after locking the prescription and stock rows. */
    @Transactional
    @CacheEvict(value = "records", key = "#clinicId + ':' + #category")
    public RecordResponse updateForDispense(String clinicId, String category, String externalId, RecordRequest request, Authentication authentication) {
        scope.requireScope(clinicId, category);
        scope.requireMember(clinicId, authentication);
        if (!"pharmacy".equals(category) && !"prescriptions".equals(category)) throw new AccessDeniedException("Dispensing can only update pharmacy records");
        if (authentication == null || authentication.getAuthorities().stream().noneMatch(a -> a != null && Set.of("ROLE_PLATFORM_OWNER", "ROLE_PHARMACIST", "ROLE_ADMIN", "ROLE_INVENTORY").contains(a.getAuthority()))) {
            throw new AccessDeniedException("Pharmacy or inventory role required");
        }
        return updateInternal(clinicId, category, externalId, request, authentication.getName(), false);
    }

    private RecordResponse updateInternal(String clinicId, String category, String externalId, RecordRequest request, String actor, boolean doctor) {
        if (request == null || request.fields() == null) throw new BadRequestException("Record payload cannot be empty");
        Map<String, Object> payload = prescriptionPolicy.prepareForWrite(category, doctor, actor, request.fields());
        if (payload == null || payload.isEmpty()) throw new BadRequestException("Record payload cannot be empty");
        validatePayload(payload);
        CareRecord record = "billing".equals(category)
                ? repository.lockByClinicAndCategoryAndExternalId(clinicId, category, externalId)
                        .orElseThrow(() -> new NotFoundException("Record not found"))
                : find(clinicId, category, externalId);
        Map<String, Object> existing = mapper.toResponse(record, encryption.decrypt(record.getPayloadJson())).fields();
        prescriptionPolicy.requireUpdatable(category, doctor, existing, payload);
        Map<String, Object> values = new LinkedHashMap<>(payload);
        if ("billing".equals(category)) {
            BigDecimal previousAmount = invoiceAmount(existing.get("amount"));
            BigDecimal paid = existing.get("paid") == null ? BigDecimal.ZERO.setScale(2) : billingMoney(existing.get("paid"), true);
            BigDecimal nextAmount = values.containsKey("amount") ? invoiceAmount(values.get("amount")) : previousAmount;
            if (nextAmount.compareTo(paid) < 0) throw new BadRequestException("Invoice amount cannot be lower than the amount already paid");
            if (billingPayments.existsByClinicIdAndInvoiceRecordId(clinicId, record.getId())
                    && nextAmount.compareTo(previousAmount) != 0) {
                throw new ConflictException("Invoice amount cannot change after a payment has been recorded");
            }
            values.put("amount", nextAmount);
            values.put("paid", paid);
            values.put("status", existing.getOrDefault("status", paid.signum() > 0 ? "Partially paid" : "Pending"));
        }
        values.put("id", externalId);
        record.replacePayload(encryption.encrypt(mapper.serialize(values)));
        record.replaceSearchIndexes(searchHash(values, List.of("name", "patient", "patientName", "email")),
                searchHash(values, List.of("phone")), encryption.fingerprint(externalId.toLowerCase(java.util.Locale.ROOT)));
        repository.save(record);
        audit(clinicId, category, externalId, "UPDATE");
        return toResponse(record);
    }

    @Transactional
    @CacheEvict(value = "records", key = "#clinicId + ':' + #category")
    public void delete(String clinicId, String category, String externalId) {
        throw new AccessDeniedException("Authenticated clinic context is required");
    }

    @Transactional
    @CacheEvict(value = "records", key = "#clinicId + ':' + #category")
    public void delete(String clinicId, String category, String externalId, boolean doctor) {
        throw new AccessDeniedException("Authenticated clinic context is required");
    }

    @Transactional
    @CacheEvict(value = "records", key = "#clinicId + ':' + #category")
    public void delete(String clinicId, String category, String externalId, String actor, boolean doctor) {
        requireWriteAccess(clinicId, category, actor);
        deleteInternal(clinicId, category, externalId, doctor);
    }

    private void deleteInternal(String clinicId, String category, String externalId, boolean doctor) {
        CareRecord record = "billing".equals(category)
                ? repository.lockByClinicAndCategoryAndExternalId(clinicId, category, externalId)
                        .orElseThrow(() -> new NotFoundException("Record not found"))
                : find(clinicId, category, externalId);
        if ("billing".equals(category) && billingPayments.existsByClinicIdAndInvoiceRecordId(clinicId, record.getId())) {
            throw new ConflictException("Invoices with payment history cannot be deleted");
        }
        RecordResponse existing = toResponse(record);
        prescriptionPolicy.requireDeletable(category, doctor, existing.fields());
        repository.deleteByClinicIdAndCategoryAndExternalId(clinicId, category, externalId);
        audit(clinicId, category, externalId, "DELETE");
    }

    private void requireWriteAccess(String clinicId, String category, String actor) {
        if (actor == null || actor.isBlank()) throw new AccessDeniedException("Authenticated clinic context is required");
        scope.requireScope(clinicId, category);
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !actor.equals(authentication.getName())) throw new AccessDeniedException("Authenticated actor does not match the request");
        scope.requireMember(clinicId, authentication);
        if (!authorization.canWrite(category, authentication)) throw new AccessDeniedException("Role cannot write this category");
    }

    private void requireTrustedWorkflowAccess(String clinicId, String category) {
        scope.requireScope(clinicId, category);
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) throw new AccessDeniedException("Authenticated clinic context is required");
        scope.requireMember(clinicId, authentication);
        if (!authorization.canRead(category, authentication)) throw new AccessDeniedException("Role cannot access this category");
    }

    private CareRecord find(String clinicId, String category, String externalId) {
        return repository.findByClinicIdAndCategoryAndExternalId(clinicId, category, externalId)
                .orElseThrow(() -> new NotFoundException("Record not found"));
    }

    private RecordResponse toResponse(CareRecord record) {
        return mapper.toResponse(record, encryption.decrypt(record.getPayloadJson()));
    }

    private String searchHash(Map<String, Object> values, List<String> fields) {
        return fields.stream().map(field -> values.get(field)).filter(java.util.Objects::nonNull).map(value -> value.toString())
                .map(value -> value.trim()).filter(value -> !value.isEmpty()).findFirst()
                .map(value -> encryption.fingerprint(value.toLowerCase(java.util.Locale.ROOT))).orElse("");
    }

    private void audit(String clinic, String category, String externalId, String action) {
        String actor = actorProvider.currentActor();
        audit.save(new com.clincanywhere.entity.RecordAuditEvent(clinic, category, externalId, actor, action,
                MDC.get(com.clincanywhere.common.CorrelationIdFilter.MDC_KEY)));
    }

    private void validatePayload(Map<String, Object> payload) {
        final int serializedBytes;
        try {
            serializedBytes = mapper.serialize(payload).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        } catch (IllegalArgumentException invalid) {
            throw new BadRequestException("Record payload is invalid");
        }
        if (serializedBytes > 19_000) throw new BadRequestException("Record payload exceeds the maximum supported size");
    }

    private BigDecimal invoiceAmount(Object raw) {
        return billingMoney(raw, false);
    }

    private BigDecimal billingMoney(Object raw, boolean allowZero) {
        try {
            if (raw == null) throw new NumberFormatException();
            BigDecimal amount = new BigDecimal(raw.toString()).setScale(2, RoundingMode.UNNECESSARY);
            if (allowZero ? amount.signum() < 0 : amount.signum() <= 0) throw new NumberFormatException();
            return amount;
        } catch (RuntimeException invalid) {
            throw new BadRequestException((allowZero ? "Paid amount" : "Invoice amount")
                    + " must be " + (allowZero ? "zero or greater" : "greater than zero")
                    + " and have at most two decimal places");
        }
    }

}
