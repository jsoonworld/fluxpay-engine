package com.fluxpay.engine.presentation.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * Request DTO for creating a refund.
 * Requires payment ID, amount, and currency.
 * Reason is optional.
 */
public record CreateRefundRequest(
    @NotBlank(message = "Payment ID is required")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
             message = "Payment ID must be a UUID")
    String paymentId,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    BigDecimal amount,

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^(KRW|USD|JPY|EUR)$", message = "Unsupported currency")
    String currency,

    String reason
) {}
