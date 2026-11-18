package com.clincanywhere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record GatewayPaymentVerifyRequest(@Positive long attemptId,
                                          @NotBlank @Size(max = 80) String orderId,
                                          @NotBlank @Size(max = 80) String paymentId,
                                          @NotBlank @Size(max = 128) String signature) { }
