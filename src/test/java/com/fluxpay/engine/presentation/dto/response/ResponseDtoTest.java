package com.fluxpay.engine.presentation.dto.response;

import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.order.Order;
import com.fluxpay.engine.domain.model.order.OrderLineItem;
import com.fluxpay.engine.domain.model.order.OrderStatus;
import com.fluxpay.engine.domain.model.payment.Payment;
import com.fluxpay.engine.domain.model.payment.PaymentMethod;
import com.fluxpay.engine.domain.model.payment.PaymentMethodType;
import com.fluxpay.engine.domain.model.payment.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Response DTO Tests")
class ResponseDtoTest {

    @Nested
    @DisplayName("OrderResponse")
    class OrderResponseTest {

        @Test
        @DisplayName("should map Order to OrderResponse correctly")
        void shouldMapOrderToOrderResponse() {
            // Given
            OrderLineItem lineItem = OrderLineItem.create(
                "PROD-001",
                "Test Product",
                2,
                Money.krw(10000)
            );
            Map<String, Object> metadata = Map.of("source", "web", "priority", 1);
            Order order = Order.create(
                "user-123",
                List.of(lineItem),
                Currency.KRW,
                metadata
            );

            // When
            OrderResponse response = OrderResponse.from(order);

            // Then
            assertThat(response.orderId()).isEqualTo(order.getId().value().toString());
            assertThat(response.userId()).isEqualTo("user-123");
            assertThat(response.status()).isEqualTo(OrderStatus.PENDING.name());
            assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(20000));
            assertThat(response.currency()).isEqualTo("KRW");
            assertThat(response.metadata()).containsEntry("source", "web");
            assertThat(response.metadata()).containsEntry("priority", 1);
            assertThat(response.createdAt()).isNotNull();
            assertThat(response.updatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should map line items correctly")
        void shouldMapLineItemsCorrectly() {
            // Given
            OrderLineItem lineItem1 = OrderLineItem.create(
                "PROD-001",
                "Product A",
                2,
                Money.krw(10000)
            );
            OrderLineItem lineItem2 = OrderLineItem.create(
                "PROD-002",
                "Product B",
                1,
                Money.krw(5000)
            );
            Order order = Order.create(
                "user-123",
                List.of(lineItem1, lineItem2),
                Currency.KRW,
                null
            );

            // When
            OrderResponse response = OrderResponse.from(order);

            // Then
            assertThat(response.lineItems()).hasSize(2);

            OrderResponse.LineItemResponse item1 = response.lineItems().get(0);
            assertThat(item1.id()).isEqualTo(lineItem1.getId().toString());
            assertThat(item1.productId()).isEqualTo("PROD-001");
            assertThat(item1.productName()).isEqualTo("Product A");
            assertThat(item1.quantity()).isEqualTo(2);
            assertThat(item1.unitPrice()).isEqualByComparingTo(BigDecimal.valueOf(10000));
            assertThat(item1.totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(20000));

            OrderResponse.LineItemResponse item2 = response.lineItems().get(1);
            assertThat(item2.productId()).isEqualTo("PROD-002");
            assertThat(item2.productName()).isEqualTo("Product B");
            assertThat(item2.quantity()).isEqualTo(1);
            assertThat(item2.unitPrice()).isEqualByComparingTo(BigDecimal.valueOf(5000));
            assertThat(item2.totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        }

        @Test
        @DisplayName("should handle order with null metadata")
        void shouldHandleNullMetadata() {
            // Given
            OrderLineItem lineItem = OrderLineItem.create(
                "PROD-001",
                "Test Product",
                1,
                Money.krw(10000)
            );
            Order order = Order.create(
                "user-123",
                List.of(lineItem),
                Currency.KRW,
                null
            );

            // When
            OrderResponse response = OrderResponse.from(order);

            // Then
            assertThat(response.metadata()).isNotNull();
            assertThat(response.metadata()).isEmpty();
        }

        @Test
        @DisplayName("should map order with different currency")
        void shouldMapOrderWithDifferentCurrency() {
            // Given
            OrderLineItem lineItem = OrderLineItem.create(
                "PROD-001",
                "Test Product",
                1,
                Money.of(BigDecimal.valueOf(100), Currency.USD)
            );
            Order order = Order.create(
                "user-123",
                List.of(lineItem),
                Currency.USD,
                null
            );

            // When
            OrderResponse response = OrderResponse.from(order);

            // Then
            assertThat(response.currency()).isEqualTo("USD");
            assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        }

        @Test
        @DisplayName("should map LineItemResponse from OrderLineItem correctly")
        void shouldMapLineItemResponseFromOrderLineItem() {
            // Given
            OrderLineItem lineItem = OrderLineItem.create(
                "PROD-001",
                "Test Product",
                3,
                Money.krw(5000)
            );

            // When
            OrderResponse.LineItemResponse response = OrderResponse.LineItemResponse.from(lineItem);

            // Then
            assertThat(response.id()).isEqualTo(lineItem.getId().toString());
            assertThat(response.productId()).isEqualTo("PROD-001");
            assertThat(response.productName()).isEqualTo("Test Product");
            assertThat(response.quantity()).isEqualTo(3);
            assertThat(response.unitPrice()).isEqualByComparingTo(BigDecimal.valueOf(5000));
            assertThat(response.totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(15000));
        }
    }

    @Nested
    @DisplayName("PaymentResponse")
    class PaymentResponseTest {

        @Test
        @DisplayName("should map Payment to PaymentResponse correctly")
        void shouldMapPaymentToPaymentResponse() {
            // Given
            Payment payment = Payment.create(
                com.fluxpay.engine.domain.model.order.OrderId.generate(),
                Money.krw(50000)
            );

            // When
            PaymentResponse response = PaymentResponse.from(payment);

            // Then
            assertThat(response.paymentId()).isEqualTo(payment.getId().toString());
            assertThat(response.orderId()).isEqualTo(payment.getOrderId().value().toString());
            assertThat(response.status()).isEqualTo(PaymentStatus.READY.name());
            assertThat(response.amount()).isEqualByComparingTo(BigDecimal.valueOf(50000));
            assertThat(response.currency()).isEqualTo("KRW");
            assertThat(response.paymentMethod()).isNull();
            assertThat(response.pgTransactionId()).isNull();
            assertThat(response.failureReason()).isNull();
            assertThat(response.approvedAt()).isNull();
            assertThat(response.confirmedAt()).isNull();
            assertThat(response.createdAt()).isNotNull();
            assertThat(response.updatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should map Payment with payment method")
        void shouldMapPaymentWithPaymentMethod() {
            // Given
            Payment payment = Payment.create(
                com.fluxpay.engine.domain.model.order.OrderId.generate(),
                Money.krw(50000)
            );
            payment.startProcessing(PaymentMethod.card("Visa"));

            // When
            PaymentResponse response = PaymentResponse.from(payment);

            // Then
            assertThat(response.status()).isEqualTo(PaymentStatus.PROCESSING.name());
            assertThat(response.paymentMethod()).isEqualTo(PaymentMethodType.CARD.name());
        }

        @Test
        @DisplayName("should map approved Payment with PG transaction ID")
        void shouldMapApprovedPayment() {
            // Given
            Payment payment = Payment.create(
                com.fluxpay.engine.domain.model.order.OrderId.generate(),
                Money.krw(50000)
            );
            payment.startProcessing(PaymentMethod.card());
            payment.approve("pg-txn-123456");

            // When
            PaymentResponse response = PaymentResponse.from(payment);

            // Then
            assertThat(response.status()).isEqualTo(PaymentStatus.APPROVED.name());
            assertThat(response.pgTransactionId()).isEqualTo("pg-txn-123456");
            assertThat(response.approvedAt()).isNotNull();
            assertThat(response.confirmedAt()).isNull();
        }

        @Test
        @DisplayName("should map confirmed Payment")
        void shouldMapConfirmedPayment() {
            // Given
            Payment payment = Payment.create(
                com.fluxpay.engine.domain.model.order.OrderId.generate(),
                Money.krw(50000)
            );
            payment.startProcessing(PaymentMethod.bankTransfer());
            payment.approve("pg-txn-123456");
            payment.confirm();

            // When
            PaymentResponse response = PaymentResponse.from(payment);

            // Then
            assertThat(response.status()).isEqualTo(PaymentStatus.CONFIRMED.name());
            assertThat(response.approvedAt()).isNotNull();
            assertThat(response.confirmedAt()).isNotNull();
        }

        @Test
        @DisplayName("should map failed Payment with failure reason")
        void shouldMapFailedPayment() {
            // Given
            Payment payment = Payment.create(
                com.fluxpay.engine.domain.model.order.OrderId.generate(),
                Money.krw(50000)
            );
            payment.fail("Insufficient funds");

            // When
            PaymentResponse response = PaymentResponse.from(payment);

            // Then
            assertThat(response.status()).isEqualTo(PaymentStatus.FAILED.name());
            assertThat(response.failureReason()).isEqualTo("Insufficient funds");
        }

        @Test
        @DisplayName("should handle null payment method gracefully")
        void shouldHandleNullPaymentMethod() {
            // Given
            Payment payment = Payment.create(
                com.fluxpay.engine.domain.model.order.OrderId.generate(),
                Money.krw(10000)
            );
            // Payment is in READY state, no payment method set yet

            // When
            PaymentResponse response = PaymentResponse.from(payment);

            // Then
            assertThat(response.paymentMethod()).isNull();
        }

        @Test
        @DisplayName("should map Payment with different currency")
        void shouldMapPaymentWithDifferentCurrency() {
            // Given
            Payment payment = Payment.create(
                com.fluxpay.engine.domain.model.order.OrderId.generate(),
                Money.of(BigDecimal.valueOf(99.99), Currency.USD)
            );

            // When
            PaymentResponse response = PaymentResponse.from(payment);

            // Then
            assertThat(response.currency()).isEqualTo("USD");
            assertThat(response.amount()).isEqualByComparingTo(BigDecimal.valueOf(99.99));
        }

        @Test
        @DisplayName("should map Payment with easy pay method")
        void shouldMapPaymentWithEasyPayMethod() {
            // Given
            Payment payment = Payment.create(
                com.fluxpay.engine.domain.model.order.OrderId.generate(),
                Money.krw(25000)
            );
            payment.startProcessing(PaymentMethod.easyPay("Kakao Pay"));

            // When
            PaymentResponse response = PaymentResponse.from(payment);

            // Then
            assertThat(response.paymentMethod()).isEqualTo(PaymentMethodType.EASY_PAY.name());
        }
    }
}
