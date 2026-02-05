package com.fluxpay.engine.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@ValidEasyPayProvider
public record ApprovePaymentRequest(
    @NotBlank(message = "Payment method is required")
    @Pattern(regexp = "^(CARD|BANK_TRANSFER|VIRTUAL_ACCOUNT|MOBILE|EASY_PAY)$",
             message = "Unsupported payment method")
    String paymentMethod,

    String easyPayProvider
) {}
