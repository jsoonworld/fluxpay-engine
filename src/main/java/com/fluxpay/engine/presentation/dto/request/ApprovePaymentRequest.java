package com.fluxpay.engine.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@ValidEasyPayProvider
public record ApprovePaymentRequest(
    @NotBlank(message = "결제 수단은 필수입니다")
    @Pattern(regexp = "^(CARD|BANK_TRANSFER|VIRTUAL_ACCOUNT|MOBILE|EASY_PAY)$",
             message = "지원하지 않는 결제 수단입니다")
    String paymentMethod,

    String easyPayProvider
) {}
