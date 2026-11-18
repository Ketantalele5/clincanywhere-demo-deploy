package com.clincanywhere.controller;

import com.clincanywhere.dto.BillingPaymentAmountRequest;
import com.clincanywhere.dto.BillingPaymentResponse;
import com.clincanywhere.dto.GatewayOrderResponse;
import com.clincanywhere.dto.GatewayPaymentVerifyRequest;
import com.clincanywhere.dto.PaymentGatewayConfigResponse;
import com.clincanywhere.security.RecordScope;
import com.clincanywhere.service.BillingPaymentLedgerService;
import com.clincanywhere.service.RazorpayGatewayClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/payments")
public class BillingPaymentController {
    private final BillingPaymentLedgerService ledger;
    private final RazorpayGatewayClient gateway;
    private final RecordScope scope;
    private final ObjectMapper mapper;

    public BillingPaymentController(BillingPaymentLedgerService ledger, RazorpayGatewayClient gateway,
                                    RecordScope scope, ObjectMapper mapper) {
        this.ledger = ledger;
        this.gateway = gateway;
        this.scope = scope;
        this.mapper = mapper;
    }

    @GetMapping("/{clinicId}/config")
    @PreAuthorize("@recordAuthorization.canRead('billing', authentication)")
    public PaymentGatewayConfigResponse config(@PathVariable @NotBlank String clinicId, Authentication authentication) {
        scope.requireScope(clinicId, "billing");
        scope.requireMember(clinicId, authentication);
        return new PaymentGatewayConfigResponse(gateway.enabled(), "RAZORPAY",
                gateway.enabled() ? gateway.keyId() : "", "INR");
    }

    @GetMapping("/{clinicId}/invoices/{invoiceId}")
    @PreAuthorize("@recordAuthorization.canRead('billing', authentication)")
    public List<BillingPaymentResponse> list(@PathVariable @NotBlank String clinicId,
                                             @PathVariable @NotBlank String invoiceId,
                                             Authentication authentication) {
        return ledger.list(clinicId, invoiceId, authentication);
    }

    @GetMapping("/{clinicId}/pending")
    @PreAuthorize("@recordAuthorization.canRead('billing', authentication)")
    public List<BillingPaymentResponse> pending(@PathVariable @NotBlank String clinicId, Authentication authentication) {
        return ledger.pending(clinicId, authentication);
    }

    @PostMapping("/{clinicId}/invoices/{invoiceId}/cash")
    @PreAuthorize("@recordAuthorization.canWrite('billing', authentication)")
    @ResponseStatus(HttpStatus.CREATED)
    public BillingPaymentResponse cash(@PathVariable @NotBlank String clinicId,
                                       @PathVariable @NotBlank String invoiceId,
                                       @Valid @RequestBody BillingPaymentAmountRequest request,
                                       Authentication authentication) {
        return ledger.recordCash(clinicId, invoiceId, request.amount(), authentication);
    }

    @PostMapping("/{clinicId}/invoices/{invoiceId}/gateway/orders")
    @PreAuthorize("@recordAuthorization.canWrite('billing', authentication)")
    @ResponseStatus(HttpStatus.CREATED)
    public GatewayOrderResponse createGatewayOrder(@PathVariable @NotBlank String clinicId,
                                                   @PathVariable @NotBlank String invoiceId,
                                                   @Valid @RequestBody BillingPaymentAmountRequest request,
                                                   Authentication authentication) {
        if (!gateway.enabled()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Online payments are not configured. Add Razorpay keys and a webhook secret to the server environment.");
        var attempt = ledger.beginGatewayAttempt(clinicId, invoiceId, request.amount(), authentication);
        String orderId = attempt.orderId();
        if (orderId == null || orderId.isBlank()) {
            try {
                orderId = gateway.createOrder(attempt.amountPaise(), attempt.receipt(), attempt.id());
                ledger.attachGatewayOrder(attempt.id(), orderId);
            } catch (RuntimeException failure) {
                ledger.markGatewayCreationFailed(attempt.id());
                throw failure;
            }
        }
        return new GatewayOrderResponse(attempt.id(), orderId, gateway.keyId(),
                attempt.amountPaise(), "INR", invoiceId);
    }

    @PostMapping("/{clinicId}/invoices/{invoiceId}/gateway/verify")
    @PreAuthorize("@recordAuthorization.canWrite('billing', authentication)")
    public BillingPaymentResponse verifyGatewayPayment(@PathVariable @NotBlank String clinicId,
                                                       @PathVariable @NotBlank String invoiceId,
                                                       @Valid @RequestBody GatewayPaymentVerifyRequest request,
                                                       Authentication authentication) {
        scope.requireScope(clinicId, "billing");
        scope.requireMember(clinicId, authentication);
        var attempt = ledger.getAttempt(clinicId, request.attemptId(), authentication);
        if (!invoiceId.equals(attempt.invoiceId()) || !"RAZORPAY".equals(attempt.method())
                || !request.orderId().equals(attempt.gatewayOrderId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment details do not match this invoice");
        }
        if (!gateway.verifyCheckoutSignature(attempt.gatewayOrderId(), request.paymentId(), request.signature())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment signature could not be verified");
        }
        var providerPayment = gateway.fetchPayment(request.paymentId());
        if (!providerPayment.id().equals(request.paymentId()) || !providerPayment.orderId().equals(attempt.gatewayOrderId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment reference does not match the Razorpay order");
        }
        if (!"captured".equals(providerPayment.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment is not captured yet; the invoice was not updated");
        }
        return ledger.completeGatewayPayment(clinicId, attempt.gatewayOrderId(), providerPayment.id(),
                providerPayment.amountPaise(), providerPayment.currency());
    }

    /** Razorpay webhook boundary; raw request bytes are signature-checked before parsing. */
    @PostMapping("/webhooks/razorpay")
    public ResponseEntity<Void> razorpayWebhook(@RequestBody byte[] rawBody,
            @RequestHeader(name = "X-Razorpay-Signature", required = false) String signature) {
        if (!gateway.webhookEnabled()) return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        if (!gateway.verifyWebhookSignature(rawBody, signature)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            JsonNode event = mapper.readTree(rawBody);
            if (event == null || !event.isObject()) return ResponseEntity.badRequest().build();
            String eventType = event.path("event").asText("");
            JsonNode payment = event.path("payload").path("payment").path("entity");
            if (!payment.isObject()) return ResponseEntity.ok().build();
            String orderId = payment.path("order_id").asText("");
            String paymentId = payment.path("id").asText("");
            if (!orderId.matches("order_[A-Za-z0-9]+") || !paymentId.matches("pay_[A-Za-z0-9]+")) {
                return ResponseEntity.ok().build();
            }
            var attempt = ledger.findAttemptByOrderId(orderId);
            if (attempt == null) return ResponseEntity.ok().build();
            if ("payment.captured".equals(eventType)) {
                ledger.completeGatewayPayment(attempt.clinicId(), orderId, paymentId,
                        payment.path("amount").asLong(-1), payment.path("currency").asText(""));
            } else if ("payment.failed".equals(eventType)) {
                ledger.failGatewayPayment(attempt.clinicId(), orderId, paymentId);
            }
            return ResponseEntity.ok().build();
        } catch (IOException malformed) {
            return ResponseEntity.badRequest().build();
        }
    }
}
