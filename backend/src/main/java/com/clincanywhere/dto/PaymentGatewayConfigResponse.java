package com.clincanywhere.dto;

public record PaymentGatewayConfigResponse(boolean enabled, String provider, String keyId, String currency) { }
