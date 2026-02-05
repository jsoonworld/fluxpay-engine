package com.fluxpay.engine.infrastructure.persistence.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.order.Order;
import com.fluxpay.engine.domain.model.order.OrderId;
import com.fluxpay.engine.domain.model.order.OrderLineItem;
import com.fluxpay.engine.domain.model.order.OrderStatus;
import com.fluxpay.engine.infrastructure.persistence.entity.OrderEntity;
import com.fluxpay.engine.infrastructure.persistence.entity.OrderLineItemEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mapper for converting between Order domain objects and persistence entities.
 */
@Component
public class OrderMapper {

    private final ObjectMapper objectMapper;

    public OrderMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Converts an Order domain object to an OrderEntity.
     *
     * @param order the Order domain object
     * @return the OrderEntity
     * @throws IllegalArgumentException if order is null
     */
    public OrderEntity toEntity(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        OrderEntity entity = new OrderEntity();
        entity.setId(order.getId().value());
        entity.setUserId(order.getUserId());
        entity.setTotalAmount(order.getTotalAmount().amount());
        entity.setCurrency(order.getCurrency().name());
        entity.setStatus(order.getStatus().name());
        entity.setMetadata(serializeMetadata(order.getMetadata()));
        entity.setCreatedAt(order.getCreatedAt());
        entity.setUpdatedAt(order.getUpdatedAt());
        entity.setPaidAt(order.getPaidAt());
        entity.setCompletedAt(order.getCompletedAt());

        return entity;
    }

    /**
     * Converts Order line items to OrderLineItemEntity list.
     *
     * @param order the Order domain object
     * @return the list of OrderLineItemEntity
     */
    public List<OrderLineItemEntity> toLineItemEntities(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        UUID orderId = order.getId().value();
        String currency = order.getCurrency().name();

        return order.getLineItems().stream()
                .map(lineItem -> toLineItemEntity(lineItem, orderId, currency))
                .toList();
    }

    private OrderLineItemEntity toLineItemEntity(OrderLineItem lineItem, UUID orderId, String currency) {
        OrderLineItemEntity entity = new OrderLineItemEntity();
        entity.setId(lineItem.getId());
        entity.setOrderId(orderId);
        entity.setProductId(lineItem.getProductId());
        entity.setProductName(lineItem.getProductName());
        entity.setQuantity(lineItem.getQuantity());
        entity.setUnitPrice(lineItem.getUnitPrice().amount());
        entity.setTotalPrice(lineItem.getTotalPrice().amount());
        entity.setCurrency(currency);
        return entity;
    }

    /**
     * Converts an OrderEntity and its line items to an Order domain object.
     *
     * @param orderEntity    the OrderEntity
     * @param lineItemEntities the list of OrderLineItemEntity
     * @return the Order domain object
     * @throws IllegalArgumentException if orderEntity or lineItemEntities is null
     */
    public Order toDomain(OrderEntity orderEntity, List<OrderLineItemEntity> lineItemEntities) {
        if (orderEntity == null) {
            throw new IllegalArgumentException("OrderEntity cannot be null");
        }
        if (lineItemEntities == null) {
            throw new IllegalArgumentException("Line items list cannot be null");
        }

        Currency currency = Currency.valueOf(orderEntity.getCurrency());

        List<OrderLineItem> lineItems = lineItemEntities.stream()
                .map(this::toLineItemDomain)
                .toList();

        return Order.restore(
                OrderId.of(orderEntity.getId()),
                orderEntity.getUserId(),
                lineItems,
                currency,
                deserializeMetadata(orderEntity.getMetadata()),
                OrderStatus.valueOf(orderEntity.getStatus()),
                Money.of(orderEntity.getTotalAmount(), currency),
                orderEntity.getCreatedAt(),
                orderEntity.getUpdatedAt(),
                orderEntity.getPaidAt(),
                orderEntity.getCompletedAt()
        );
    }

    /**
     * Converts an OrderLineItemEntity to an OrderLineItem domain object.
     *
     * @param entity the OrderLineItemEntity
     * @return the OrderLineItem domain object
     * @throws IllegalArgumentException if entity is null
     */
    public OrderLineItem toLineItemDomain(OrderLineItemEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("OrderLineItemEntity cannot be null");
        }

        Currency currency = Currency.valueOf(entity.getCurrency());

        return OrderLineItem.restore(
                entity.getId(),
                entity.getProductId(),
                entity.getProductName(),
                entity.getQuantity(),
                Money.of(entity.getUnitPrice(), currency),
                Money.of(entity.getTotalPrice(), currency)
        );
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize metadata", e);
        }
    }

    private Map<String, Object> deserializeMetadata(String metadata) {
        if (metadata == null || metadata.isBlank() || "{}".equals(metadata)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(metadata, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize metadata", e);
        }
    }
}
