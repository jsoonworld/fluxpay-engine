package com.fluxpay.engine.presentation.dto.response;

import com.fluxpay.engine.domain.model.order.Order;
import com.fluxpay.engine.domain.model.order.OrderLineItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for Order entity.
 * Used to transfer order data to the presentation layer.
 */
public record OrderResponse(
    String orderId,
    String userId,
    String status,
    BigDecimal totalAmount,
    String currency,
    List<LineItemResponse> lineItems,
    Map<String, Object> metadata,
    Instant createdAt,
    Instant updatedAt
) {
    /**
     * Creates an OrderResponse from an Order domain entity.
     *
     * @param order the Order domain entity
     * @return an OrderResponse DTO
     */
    public static OrderResponse from(Order order) {
        return new OrderResponse(
            order.getId().value().toString(),
            order.getUserId(),
            order.getStatus().name(),
            order.getTotalAmount().amount(),
            order.getTotalAmount().currency().name(),
            order.getLineItems().stream()
                .map(LineItemResponse::from)
                .toList(),
            order.getMetadata(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }

    /**
     * Response DTO for order line items.
     */
    public record LineItemResponse(
        String id,
        String productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
    ) {
        /**
         * Creates a LineItemResponse from an OrderLineItem domain entity.
         *
         * @param item the OrderLineItem domain entity
         * @return a LineItemResponse DTO
         */
        public static LineItemResponse from(OrderLineItem item) {
            return new LineItemResponse(
                item.getId().toString(),
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice().amount(),
                item.getTotalPrice().amount()
            );
        }
    }
}
