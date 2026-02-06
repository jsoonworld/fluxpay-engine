package com.fluxpay.engine.presentation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CreateOrderRequest(
    @NotBlank(message = "User ID is required")
    @Size(max = 100, message = "User ID must be at most 100 characters")
    String userId,

    @NotEmpty(message = "At least one line item is required")
    @Valid
    List<LineItemRequest> lineItems,

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^(KRW|USD|JPY|EUR)$", message = "Unsupported currency")
    String currency,

    Map<String, Object> metadata
) {
    public record LineItemRequest(
        @NotBlank(message = "Product ID is required")
        String productId,

        @NotBlank(message = "Product name is required")
        @Size(max = 255, message = "Product name must be at most 255 characters")
        String productName,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be greater than 0")
        BigDecimal unitPrice
    ) {}
}
