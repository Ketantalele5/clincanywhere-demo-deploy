package com.clincanywhere.dto;

import java.time.Instant;

public record BillingPaymentResponse(long id, String invoiceId, long amountPaise, String currency,
                                     String method, String status, String gatewayOrderId,
                                     String gatewayPaymentId, String createdBy, Instant createdAt) { }
