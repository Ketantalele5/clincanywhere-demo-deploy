package com.clincanywhere.dto;

public record GatewayOrderResponse(long attemptId, String orderId, String keyId,
                                   long amountPaise, String currency, String invoiceId) { }
