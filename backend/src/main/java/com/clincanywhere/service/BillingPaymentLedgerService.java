package com.clincanywhere.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import com.clincanywhere.common.exception.BadRequestException;
import com.clincanywhere.common.exception.ConflictException;
import com.clincanywhere.common.exception.NotFoundException;
import com.clincanywhere.dto.BillingPaymentResponse;
import com.clincanywhere.entity.BillingPayment;
import com.clincanywhere.entity.CareRecord;
import com.clincanywhere.repository.BillingPaymentRepository;
import com.clincanywhere.repository.CareRecordRepository;
import com.clincanywhere.security.RecordAuthorization;
import com.clincanywhere.security.RecordScope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Clinic-scoped payment ledger. Invoice balances change only when a receipt is committed here. */
@Service
public class BillingPaymentLedgerService {
    private static final List<String> ACTIVE_GATEWAY_STATUSES = List.of("INITIATING", "PENDING");
    private final BillingPaymentRepository payments;
    private final CareRecordRepository records;
    private final com.clincanywhere.repository.RecordAuditRepository audit;
    private final FieldEncryptionService encryption;
    private final ObjectMapper mapper;
    private final RecordScope scope;
    private final RecordAuthorization authorization;

    public BillingPaymentLedgerService(BillingPaymentRepository payments, CareRecordRepository records,
                                       com.clincanywhere.repository.RecordAuditRepository audit,
                                       FieldEncryptionService encryption, ObjectMapper mapper,
                                       RecordScope scope, RecordAuthorization authorization) {
        this.payments = payments;
        this.records = records;
        this.audit = audit;
        this.encryption = encryption;
        this.mapper = mapper;
        this.scope = scope;
        this.authorization = authorization;
    }

    @Transactional(readOnly = true)
    public List<BillingPaymentResponse> list(String clinicId, String invoiceId, Authentication authentication) {
        requireReader(clinicId, authentication);
        CareRecord invoice = findInvoice(clinicId, invoiceId);
        return payments.findByClinicIdAndInvoiceRecordIdOrderByIdDesc(clinicId, invoice.getId())
                .stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<BillingPaymentResponse> pending(String clinicId, Authentication authentication) {
        requireReader(clinicId, authentication);
        return payments.findByClinicIdAndStatusInOrderByIdDesc(clinicId, ACTIVE_GATEWAY_STATUSES)
                .stream().map(this::response).toList();
    }

    @Transactional
    @CacheEvict(value = "records", key = "#clinicId + ':billing'")
    public BillingPaymentResponse recordCash(String clinicId, String invoiceId, BigDecimal amount,
                                             Authentication authentication) {
        requireWriter(clinicId, authentication);
        CareRecord invoice = lockInvoice(clinicId, invoiceId);
        long amountPaise = toPaise(amount);
        long remaining = remainingPaise(invoice);
        if (amountPaise > remaining) throw new BadRequestException("Cash payment cannot exceed the invoice balance");
        rejectActiveGateway(clinicId, invoice.getId());
        BillingPayment payment = payments.save(new BillingPayment(clinicId, invoice.getId(), invoiceId,
                amountPaise, "INR", "CASH", "COMPLETED", authentication.getName(), null));
        updateInvoicePaid(invoice, amountPaise);
        recordAudit(clinicId, invoiceId, authentication.getName(), "PAYMENT");
        return response(payment);
    }

    @Transactional
    public GatewayAttempt beginGatewayAttempt(String clinicId, String invoiceId, BigDecimal amount,
                                              Authentication authentication) {
        requireWriter(clinicId, authentication);
        CareRecord invoice = lockInvoice(clinicId, invoiceId);
        long amountPaise = toPaise(amount);
        if (amountPaise < 100) throw new BadRequestException("Online payments must be at least ₹1.00");
        long remaining = remainingPaise(invoice);
        if (remaining < 1) throw new ConflictException("This invoice is already paid");
        if (amountPaise > remaining) throw new BadRequestException("Online payment cannot exceed the invoice balance");
        var active = payments.findFirstByClinicIdAndInvoiceRecordIdAndStatusInOrderByIdDesc(
                clinicId, invoice.getId(), ACTIVE_GATEWAY_STATUSES);
        if (active.isPresent()) {
            BillingPayment existing = active.get();
            if ("INITIATING".equals(existing.getStatus())) {
                throw new ConflictException("An online payment is already being prepared for this invoice");
            }
            if (existing.getAmountPaise() != amountPaise) {
                throw new ConflictException("Finish or check the pending online payment before changing its amount");
            }
            return attempt(existing);
        }
        String receipt = "CA" + invoice.getId() + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 18);
        BillingPayment payment = payments.save(new BillingPayment(clinicId, invoice.getId(), invoiceId,
                amountPaise, "INR", "RAZORPAY", "INITIATING", authentication.getName(), receipt));
        return attempt(payment);
    }

    @Transactional
    public void attachGatewayOrder(long attemptId, String orderId) {
        BillingPayment payment = payments.findById(attemptId)
                .orElseThrow(() -> new NotFoundException("Payment attempt not found"));
        payment.attachGatewayOrder(orderId);
    }

    @Transactional
    public void markGatewayCreationFailed(long attemptId) {
        payments.findById(attemptId).ifPresent(payment -> payment.failGatewayAttempt(null));
    }

    @Transactional(readOnly = true)
    public BillingPaymentResponse getAttempt(String clinicId, long attemptId, Authentication authentication) {
        requireReader(clinicId, authentication);
        BillingPayment payment = payments.findById(attemptId)
                .filter(item -> item.getClinicId().equals(clinicId))
                .orElseThrow(() -> new NotFoundException("Payment attempt not found"));
        return response(payment);
    }

    @Transactional(readOnly = true)
    public GatewayAttempt findAttemptByOrderId(String orderId) {
        return payments.findByGatewayOrderId(orderId).map(this::attempt).orElse(null);
    }

    @Transactional
    @CacheEvict(value = "records", key = "#clinicId + ':billing'")
    public BillingPaymentResponse completeGatewayPayment(String clinicId, String orderId,
                                                         String paymentId, long amountPaise,
                                                         String currency) {
        BillingPayment initial = payments.findByGatewayOrderId(orderId)
                .filter(item -> item.getClinicId().equals(clinicId))
                .orElseThrow(() -> new NotFoundException("Payment attempt not found"));
        CareRecord invoice = records.lockByClinicAndCategoryAndExternalId(clinicId, "billing", initial.getInvoiceExternalId())
                .orElseThrow(() -> new NotFoundException("Invoice not found"));
        BillingPayment payment = payments.lockByClinicAndGatewayOrderId(clinicId, orderId)
                .orElseThrow(() -> new NotFoundException("Payment attempt not found"));
        if ("COMPLETED".equals(payment.getStatus()) || "REVIEW_REQUIRED".equals(payment.getStatus())) {
            return response(payment);
        }
        boolean reviewRequired = amountPaise != payment.getAmountPaise() || !"INR".equals(currency);
        if (!reviewRequired) {
            long remaining = remainingPaise(invoice);
            long appliedPaise = Math.min(amountPaise, remaining);
            if (amountPaise > remaining) reviewRequired = true;
            if (appliedPaise > 0) updateInvoicePaid(invoice, appliedPaise);
        }
        payment.completeGatewayPayment(paymentId, reviewRequired);
        recordAudit(clinicId, payment.getInvoiceExternalId(), payment.getCreatedBy(), reviewRequired ? "PAY_REVIEW" : "PAYMENT");
        return response(payment);
    }

    @Transactional
    @CacheEvict(value = "records", key = "#clinicId + ':billing'")
    public void failGatewayPayment(String clinicId, String orderId, String paymentId) {
        BillingPayment initial = payments.findByGatewayOrderId(orderId)
                .filter(item -> item.getClinicId().equals(clinicId)).orElse(null);
        if (initial == null) return;
        records.lockByClinicAndCategoryAndExternalId(clinicId, "billing", initial.getInvoiceExternalId());
        payments.lockByClinicAndGatewayOrderId(clinicId, orderId).ifPresent(payment -> {
            if (!"COMPLETED".equals(payment.getStatus()) && !"REVIEW_REQUIRED".equals(payment.getStatus())) {
                payment.failGatewayAttempt(paymentId);
            }
        });
    }

    @Transactional(readOnly = true)
    public boolean hasPayments(String clinicId, long invoiceRecordId) {
        return payments.existsByClinicIdAndInvoiceRecordId(clinicId, invoiceRecordId);
    }

    private void rejectActiveGateway(String clinicId, long invoiceRecordId) {
        boolean active = payments.findFirstByClinicIdAndInvoiceRecordIdAndStatusInOrderByIdDesc(
                clinicId, invoiceRecordId, ACTIVE_GATEWAY_STATUSES).isPresent();
        if (active) throw new ConflictException("An online payment is pending; check its status before recording cash");
    }

    private CareRecord findInvoice(String clinicId, String invoiceId) {
        return records.findByClinicIdAndCategoryAndExternalId(clinicId, "billing", invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found"));
    }

    private CareRecord lockInvoice(String clinicId, String invoiceId) {
        return records.lockByClinicAndCategoryAndExternalId(clinicId, "billing", invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found"));
    }

    private void requireReader(String clinicId, Authentication authentication) {
        scope.requireScope(clinicId, "billing");
        scope.requireMember(clinicId, authentication);
        if (!authorization.canRead("billing", authentication)) throw new AccessDeniedException("Role cannot read billing records");
    }

    private void requireWriter(String clinicId, Authentication authentication) {
        scope.requireScope(clinicId, "billing");
        scope.requireMember(clinicId, authentication);
        if (!authorization.canWrite("billing", authentication)) throw new AccessDeniedException("Role cannot record billing payments");
    }

    private long remainingPaise(CareRecord invoice) {
        Map<String, Object> values = payload(invoice);
        BigDecimal total = money(values.get("amount"), "Invoice amount");
        BigDecimal paid = values.get("paid") == null ? BigDecimal.ZERO : money(values.get("paid"), "Paid amount");
        BigDecimal remaining = total.subtract(paid);
        if (total.signum() <= 0 || paid.signum() < 0) throw new BadRequestException("Invoice amount or paid amount is invalid");
        return Math.max(remaining.movePointRight(2).longValueExact(), 0);
    }

    private long toPaise(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new BadRequestException("Payment amount must be greater than zero");
        try { return amount.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact(); }
        catch (ArithmeticException invalid) { throw new BadRequestException("Payment amount supports up to two decimal places"); }
    }

    private BigDecimal money(Object raw, String label) {
        try { return new BigDecimal(raw.toString()).setScale(2, RoundingMode.UNNECESSARY); }
        catch (RuntimeException invalid) { throw new BadRequestException(label + " is invalid"); }
    }

    private void updateInvoicePaid(CareRecord invoice, long amountPaise) {
        Map<String, Object> values = payload(invoice);
        BigDecimal total = money(values.get("amount"), "Invoice amount");
        BigDecimal paid = values.get("paid") == null ? BigDecimal.ZERO : money(values.get("paid"), "Paid amount");
        BigDecimal nextPaid = paid.add(BigDecimal.valueOf(amountPaise, 2)).min(total).setScale(2, RoundingMode.UNNECESSARY);
        values.put("paid", nextPaid);
        values.put("status", nextPaid.compareTo(total) >= 0 ? "Paid" : "Partially paid");
        try { invoice.replacePayload(encryption.encrypt(mapper.writeValueAsString(values))); }
        catch (JsonProcessingException failure) { throw new IllegalStateException("Invoice payment could not be saved", failure); }
    }

    private Map<String, Object> payload(CareRecord invoice) {
        try { return mapper.readValue(encryption.decrypt(invoice.getPayloadJson()), new TypeReference<Map<String, Object>>() { }); }
        catch (JsonProcessingException failure) { throw new IllegalStateException("Invoice data could not be read", failure); }
    }

    private GatewayAttempt attempt(BillingPayment payment) {
        return new GatewayAttempt(payment.getId(), payment.getClinicId(), payment.getInvoiceExternalId(),
                payment.getAmountPaise(), payment.getGatewayReceipt(), payment.getGatewayOrderId(), payment.getStatus());
    }

    private BillingPaymentResponse response(BillingPayment payment) {
        return new BillingPaymentResponse(payment.getId(), payment.getInvoiceExternalId(), payment.getAmountPaise(),
                payment.getCurrency(), payment.getMethod(), payment.getStatus(), payment.getGatewayOrderId(),
                payment.getGatewayPaymentId(), payment.getCreatedBy(), payment.getCreatedAt());
    }

    private void recordAudit(String clinicId, String invoiceId, String actor, String action) {
        audit.save(new com.clincanywhere.entity.RecordAuditEvent(clinicId, "billing", invoiceId, actor, action,
                org.slf4j.MDC.get(com.clincanywhere.common.CorrelationIdFilter.MDC_KEY)));
    }

    public record GatewayAttempt(Long id, String clinicId, String invoiceId, long amountPaise,
                                 String receipt, String orderId, String status) { }
}
