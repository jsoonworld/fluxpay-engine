package com.fluxpay.engine.presentation.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record CreatePaymentRequest(
    @NotBlank(message = "주문 ID는 필수입니다")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
             message = "주문 ID는 UUID 형식이어야 합니다")
    String orderId,

    @NotNull(message = "결제 금액은 필수입니다")
    @DecimalMin(value = "0.0", inclusive = false, message = "결제 금액은 0보다 커야 합니다")
    BigDecimal amount,

    @NotBlank(message = "통화는 필수입니다")
    @Pattern(regexp = "^(KRW|USD|JPY|EUR)$", message = "지원하지 않는 통화입니다")
    String currency
) {}
