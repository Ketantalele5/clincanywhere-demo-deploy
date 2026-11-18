package com.clincanywhere.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record BillingPaymentAmountRequest(@NotNull @DecimalMin("0.01") BigDecimal amount) { }
