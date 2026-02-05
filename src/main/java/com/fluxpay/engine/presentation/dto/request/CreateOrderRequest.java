package com.fluxpay.engine.presentation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CreateOrderRequest(
    @NotBlank(message = "사용자 ID는 필수입니다")
    @Size(max = 100, message = "사용자 ID는 100자 이하여야 합니다")
    String userId,

    @NotEmpty(message = "주문 항목은 최소 1개 이상이어야 합니다")
    @Valid
    List<LineItemRequest> lineItems,

    @NotBlank(message = "통화는 필수입니다")
    @Pattern(regexp = "^(KRW|USD|JPY|EUR)$", message = "지원하지 않는 통화입니다")
    String currency,

    Map<String, Object> metadata
) {
    public record LineItemRequest(
        @NotBlank(message = "상품 ID는 필수입니다")
        String productId,

        @NotBlank(message = "상품명은 필수입니다")
        @Size(max = 255, message = "상품명은 255자 이하여야 합니다")
        String productName,

        @NotNull(message = "수량은 필수입니다")
        @Min(value = 1, message = "수량은 1 이상이어야 합니다")
        Integer quantity,

        @NotNull(message = "단가는 필수입니다")
        @DecimalMin(value = "0.0", inclusive = false, message = "단가는 0보다 커야 합니다")
        BigDecimal unitPrice
    ) {}
}
