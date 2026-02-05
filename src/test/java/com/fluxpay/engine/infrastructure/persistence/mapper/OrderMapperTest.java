package com.fluxpay.engine.infrastructure.persistence.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.order.Order;
import com.fluxpay.engine.domain.model.order.OrderId;
import com.fluxpay.engine.domain.model.order.OrderLineItem;
import com.fluxpay.engine.domain.model.order.OrderStatus;
import com.fluxpay.engine.infrastructure.persistence.entity.OrderEntity;
import com.fluxpay.engine.infrastructure.persistence.entity.OrderLineItemEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for OrderMapper.
 * Following TDD: RED -> GREEN -> REFACTOR
 */
@DisplayName("OrderMapper")
class OrderMapperTest {

    private OrderMapper mapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mapper = new OrderMapper(objectMapper);
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("should convert Order to OrderEntity")
        void shouldConvertOrderToOrderEntity() {
            // Given
            OrderLineItem lineItem = OrderLineItem.create(
                    "PROD-001", "Test Product", 2,
                    Money.of(BigDecimal.valueOf(10000), Currency.KRW)
            );
            Order order = Order.create("user-123", List.of(lineItem), Currency.KRW, null);

            // When
            OrderEntity entity = mapper.toEntity(order);

            // Then
            assertThat(entity.getId()).isEqualTo(order.getId().value());
            assertThat(entity.getUserId()).isEqualTo("user-123");
            assertThat(entity.getTotalAmount()).isEqualTo(BigDecimal.valueOf(20000));
            assertThat(entity.getCurrency()).isEqualTo("KRW");
            assertThat(entity.getStatus()).isEqualTo("PENDING");
            assertThat(entity.getCreatedAt()).isEqualTo(order.getCreatedAt());
            assertThat(entity.getUpdatedAt()).isEqualTo(order.getUpdatedAt());
        }

        @Test
        @DisplayName("should serialize metadata to JSON")
        void shouldSerializeMetadataToJson() {
            // Given
            OrderLineItem lineItem = OrderLineItem.create(
                    "PROD-001", "Test Product", 1,
                    Money.of(BigDecimal.valueOf(10000), Currency.KRW)
            );
            Map<String, Object> metadata = Map.of("channel", "WEB", "referenceId", "REF-123");
            Order order = Order.create("user-123", List.of(lineItem), Currency.KRW, metadata);

            // When
            OrderEntity entity = mapper.toEntity(order);

            // Then
            assertThat(entity.getMetadata()).contains("channel");
            assertThat(entity.getMetadata()).contains("WEB");
            assertThat(entity.getMetadata()).contains("referenceId");
        }

        @Test
        @DisplayName("should handle null metadata")
        void shouldHandleNullMetadata() {
            // Given
            OrderLineItem lineItem = OrderLineItem.create(
                    "PROD-001", "Test Product", 1,
                    Money.of(BigDecimal.valueOf(10000), Currency.KRW)
            );
            Order order = Order.create("user-123", List.of(lineItem), Currency.KRW, null);

            // When
            OrderEntity entity = mapper.toEntity(order);

            // Then
            assertThat(entity.getMetadata()).isNull();
        }

        @Test
        @DisplayName("should throw exception when order is null")
        void shouldThrowExceptionWhenOrderIsNull() {
            // When & Then
            assertThatThrownBy(() -> mapper.toEntity(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Order cannot be null");
        }
    }

    @Nested
    @DisplayName("toLineItemEntities")
    class ToLineItemEntities {

        @Test
        @DisplayName("should convert line items to entities")
        void shouldConvertLineItemsToEntities() {
            // Given
            OrderLineItem lineItem1 = OrderLineItem.create(
                    "PROD-001", "Product 1", 2,
                    Money.of(BigDecimal.valueOf(10000), Currency.KRW)
            );
            OrderLineItem lineItem2 = OrderLineItem.create(
                    "PROD-002", "Product 2", 1,
                    Money.of(BigDecimal.valueOf(5000), Currency.KRW)
            );
            Order order = Order.create("user-123", List.of(lineItem1, lineItem2), Currency.KRW, null);

            // When
            List<OrderLineItemEntity> entities = mapper.toLineItemEntities(order);

            // Then
            assertThat(entities).hasSize(2);
            assertThat(entities.get(0).getProductId()).isEqualTo("PROD-001");
            assertThat(entities.get(0).getOrderId()).isEqualTo(order.getId().value());
            assertThat(entities.get(0).getCurrency()).isEqualTo("KRW");
            assertThat(entities.get(1).getProductId()).isEqualTo("PROD-002");
        }

        @Test
        @DisplayName("should use order currency for line items")
        void shouldUseOrderCurrencyForLineItems() {
            // Given
            OrderLineItem lineItem = OrderLineItem.create(
                    "PROD-001", "Product", 1,
                    Money.of(BigDecimal.valueOf(100), Currency.USD)
            );
            Order order = Order.create("user-123", List.of(lineItem), Currency.USD, null);

            // When
            List<OrderLineItemEntity> entities = mapper.toLineItemEntities(order);

            // Then
            assertThat(entities.get(0).getCurrency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("should throw exception when order is null")
        void shouldThrowExceptionWhenOrderIsNull() {
            // When & Then
            assertThatThrownBy(() -> mapper.toLineItemEntities(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Order cannot be null");
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ToDomain {

        @Test
        @DisplayName("should convert entity to domain Order")
        void shouldConvertEntityToDomainOrder() {
            // Given
            UUID orderId = UUID.randomUUID();
            UUID lineItemId = UUID.randomUUID();
            Instant now = Instant.now();

            OrderEntity orderEntity = new OrderEntity();
            orderEntity.setId(orderId);
            orderEntity.setUserId("user-123");
            orderEntity.setTotalAmount(BigDecimal.valueOf(20000));
            orderEntity.setCurrency("KRW");
            orderEntity.setStatus("PENDING");
            orderEntity.setCreatedAt(now);
            orderEntity.setUpdatedAt(now);

            OrderLineItemEntity lineItemEntity = new OrderLineItemEntity();
            lineItemEntity.setId(lineItemId);
            lineItemEntity.setOrderId(orderId);
            lineItemEntity.setProductId("PROD-001");
            lineItemEntity.setProductName("Test Product");
            lineItemEntity.setQuantity(2);
            lineItemEntity.setUnitPrice(BigDecimal.valueOf(10000));
            lineItemEntity.setTotalPrice(BigDecimal.valueOf(20000));
            lineItemEntity.setCurrency("KRW");

            // When
            Order order = mapper.toDomain(orderEntity, List.of(lineItemEntity));

            // Then
            assertThat(order.getId().value()).isEqualTo(orderId);
            assertThat(order.getUserId()).isEqualTo("user-123");
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(order.getTotalAmount()).isEqualTo(Money.of(BigDecimal.valueOf(20000), Currency.KRW));
            assertThat(order.getLineItems()).hasSize(1);
        }

        @Test
        @DisplayName("should deserialize metadata from JSON")
        void shouldDeserializeMetadataFromJson() {
            // Given
            UUID orderId = UUID.randomUUID();
            Instant now = Instant.now();

            OrderEntity orderEntity = new OrderEntity();
            orderEntity.setId(orderId);
            orderEntity.setUserId("user-123");
            orderEntity.setTotalAmount(BigDecimal.valueOf(10000));
            orderEntity.setCurrency("KRW");
            orderEntity.setStatus("PENDING");
            orderEntity.setMetadata("{\"channel\":\"WEB\",\"referenceId\":\"REF-123\"}");
            orderEntity.setCreatedAt(now);
            orderEntity.setUpdatedAt(now);

            OrderLineItemEntity lineItemEntity = createLineItemEntity(orderId);

            // When
            Order order = mapper.toDomain(orderEntity, List.of(lineItemEntity));

            // Then
            assertThat(order.getMetadata()).containsEntry("channel", "WEB");
            assertThat(order.getMetadata()).containsEntry("referenceId", "REF-123");
        }

        @Test
        @DisplayName("should handle empty metadata")
        void shouldHandleEmptyMetadata() {
            // Given
            UUID orderId = UUID.randomUUID();
            Instant now = Instant.now();

            OrderEntity orderEntity = new OrderEntity();
            orderEntity.setId(orderId);
            orderEntity.setUserId("user-123");
            orderEntity.setTotalAmount(BigDecimal.valueOf(10000));
            orderEntity.setCurrency("KRW");
            orderEntity.setStatus("PENDING");
            orderEntity.setMetadata("{}");
            orderEntity.setCreatedAt(now);
            orderEntity.setUpdatedAt(now);

            OrderLineItemEntity lineItemEntity = createLineItemEntity(orderId);

            // When
            Order order = mapper.toDomain(orderEntity, List.of(lineItemEntity));

            // Then
            assertThat(order.getMetadata()).isEmpty();
        }

        @Test
        @DisplayName("should handle null metadata")
        void shouldHandleNullMetadata() {
            // Given
            UUID orderId = UUID.randomUUID();
            Instant now = Instant.now();

            OrderEntity orderEntity = new OrderEntity();
            orderEntity.setId(orderId);
            orderEntity.setUserId("user-123");
            orderEntity.setTotalAmount(BigDecimal.valueOf(10000));
            orderEntity.setCurrency("KRW");
            orderEntity.setStatus("PENDING");
            orderEntity.setMetadata(null);
            orderEntity.setCreatedAt(now);
            orderEntity.setUpdatedAt(now);

            OrderLineItemEntity lineItemEntity = createLineItemEntity(orderId);

            // When
            Order order = mapper.toDomain(orderEntity, List.of(lineItemEntity));

            // Then
            assertThat(order.getMetadata()).isEmpty();
        }

        @Test
        @DisplayName("should throw exception when orderEntity is null")
        void shouldThrowExceptionWhenOrderEntityIsNull() {
            // When & Then
            assertThatThrownBy(() -> mapper.toDomain(null, List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("OrderEntity cannot be null");
        }

        @Test
        @DisplayName("should throw exception when lineItems list is null")
        void shouldThrowExceptionWhenLineItemsListIsNull() {
            // Given
            OrderEntity orderEntity = new OrderEntity();
            orderEntity.setId(UUID.randomUUID());

            // When & Then
            assertThatThrownBy(() -> mapper.toDomain(orderEntity, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Line items list cannot be null");
        }

        @Test
        @DisplayName("should throw exception for invalid currency")
        void shouldThrowExceptionForInvalidCurrency() {
            // Given
            UUID orderId = UUID.randomUUID();
            Instant now = Instant.now();

            OrderEntity orderEntity = new OrderEntity();
            orderEntity.setId(orderId);
            orderEntity.setUserId("user-123");
            orderEntity.setTotalAmount(BigDecimal.valueOf(10000));
            orderEntity.setCurrency("INVALID_CURRENCY");
            orderEntity.setStatus("PENDING");
            orderEntity.setCreatedAt(now);
            orderEntity.setUpdatedAt(now);

            // When & Then
            assertThatThrownBy(() -> mapper.toDomain(orderEntity, List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        private OrderLineItemEntity createLineItemEntity(UUID orderId) {
            OrderLineItemEntity entity = new OrderLineItemEntity();
            entity.setId(UUID.randomUUID());
            entity.setOrderId(orderId);
            entity.setProductId("PROD-001");
            entity.setProductName("Test Product");
            entity.setQuantity(1);
            entity.setUnitPrice(BigDecimal.valueOf(10000));
            entity.setTotalPrice(BigDecimal.valueOf(10000));
            entity.setCurrency("KRW");
            return entity;
        }
    }

    @Nested
    @DisplayName("toLineItemDomain")
    class ToLineItemDomain {

        @Test
        @DisplayName("should convert entity to domain OrderLineItem")
        void shouldConvertEntityToDomainOrderLineItem() {
            // Given
            UUID lineItemId = UUID.randomUUID();
            OrderLineItemEntity entity = new OrderLineItemEntity();
            entity.setId(lineItemId);
            entity.setOrderId(UUID.randomUUID());
            entity.setProductId("PROD-001");
            entity.setProductName("Test Product");
            entity.setQuantity(3);
            entity.setUnitPrice(BigDecimal.valueOf(10000));
            entity.setTotalPrice(BigDecimal.valueOf(30000));
            entity.setCurrency("KRW");

            // When
            OrderLineItem lineItem = mapper.toLineItemDomain(entity);

            // Then
            assertThat(lineItem.getId()).isEqualTo(lineItemId);
            assertThat(lineItem.getProductId()).isEqualTo("PROD-001");
            assertThat(lineItem.getProductName()).isEqualTo("Test Product");
            assertThat(lineItem.getQuantity()).isEqualTo(3);
            assertThat(lineItem.getUnitPrice()).isEqualTo(Money.of(BigDecimal.valueOf(10000), Currency.KRW));
            assertThat(lineItem.getTotalPrice()).isEqualTo(Money.of(BigDecimal.valueOf(30000), Currency.KRW));
        }

        @Test
        @DisplayName("should throw exception when entity is null")
        void shouldThrowExceptionWhenEntityIsNull() {
            // When & Then
            assertThatThrownBy(() -> mapper.toLineItemDomain(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("OrderLineItemEntity cannot be null");
        }
    }

    @Nested
    @DisplayName("Round-trip conversion")
    class RoundTrip {

        @Test
        @DisplayName("should preserve data through domain -> entity -> domain conversion")
        void shouldPreserveDataThroughRoundTrip() {
            // Given
            OrderLineItem lineItem = OrderLineItem.create(
                    "PROD-001", "Test Product", 2,
                    Money.of(BigDecimal.valueOf(10000), Currency.KRW)
            );
            Map<String, Object> metadata = Map.of("channel", "MOBILE", "version", "1.0");
            Order originalOrder = Order.create("user-123", List.of(lineItem), Currency.KRW, metadata);

            // When - convert to entity and back
            OrderEntity orderEntity = mapper.toEntity(originalOrder);
            List<OrderLineItemEntity> lineItemEntities = mapper.toLineItemEntities(originalOrder);
            Order restoredOrder = mapper.toDomain(orderEntity, lineItemEntities);

            // Then
            assertThat(restoredOrder.getId()).isEqualTo(originalOrder.getId());
            assertThat(restoredOrder.getUserId()).isEqualTo(originalOrder.getUserId());
            assertThat(restoredOrder.getStatus()).isEqualTo(originalOrder.getStatus());
            assertThat(restoredOrder.getTotalAmount()).isEqualTo(originalOrder.getTotalAmount());
            assertThat(restoredOrder.getCurrency()).isEqualTo(originalOrder.getCurrency());
            assertThat(restoredOrder.getMetadata()).containsEntry("channel", "MOBILE");
            assertThat(restoredOrder.getLineItems()).hasSize(1);
            assertThat(restoredOrder.getLineItems().get(0).getProductId()).isEqualTo("PROD-001");
        }
    }
}
