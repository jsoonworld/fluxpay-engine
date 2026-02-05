package com.fluxpay.engine.infrastructure.external.pg.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TossRequestDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("TossPaymentRequest")
    class TossPaymentRequestTests {

        @Test
        @DisplayName("should create TossPaymentRequest via factory method")
        void shouldCreateViaFactoryMethod() {
            // Given
            String paymentKey = "toss_payment_key_123";
            String orderId = "order_456";
            BigDecimal amount = new BigDecimal("10000");

            // When
            TossPaymentRequest request = TossPaymentRequest.of(paymentKey, orderId, amount);

            // Then
            assertThat(request.paymentKey()).isEqualTo(paymentKey);
            assertThat(request.orderId()).isEqualTo(orderId);
            assertThat(request.amount()).isEqualTo(amount);
        }

        @Test
        @DisplayName("should create TossPaymentRequest via constructor")
        void shouldCreateViaConstructor() {
            // Given
            String paymentKey = "toss_payment_key_123";
            String orderId = "order_456";
            BigDecimal amount = new BigDecimal("50000");

            // When
            TossPaymentRequest request = new TossPaymentRequest(paymentKey, orderId, amount);

            // Then
            assertThat(request.paymentKey()).isEqualTo(paymentKey);
            assertThat(request.orderId()).isEqualTo(orderId);
            assertThat(request.amount()).isEqualTo(amount);
        }

        @Test
        @DisplayName("should serialize TossPaymentRequest to JSON")
        void shouldSerializeToJson() throws Exception {
            // Given
            TossPaymentRequest request = TossPaymentRequest.of(
                "payment_key_abc",
                "order_xyz",
                new BigDecimal("25000")
            );

            // When
            String json = objectMapper.writeValueAsString(request);

            // Then
            assertThat(json).contains("\"paymentKey\":\"payment_key_abc\"");
            assertThat(json).contains("\"orderId\":\"order_xyz\"");
            assertThat(json).contains("\"amount\":25000");
        }
    }

    @Nested
    @DisplayName("TossConfirmRequest")
    class TossConfirmRequestTests {

        @Test
        @DisplayName("should create TossConfirmRequest via factory method")
        void shouldCreateViaFactoryMethod() {
            // Given
            String paymentKey = "confirm_payment_key_789";
            String orderId = "confirm_order_012";
            BigDecimal amount = new BigDecimal("30000");

            // When
            TossConfirmRequest request = TossConfirmRequest.of(paymentKey, orderId, amount);

            // Then
            assertThat(request.paymentKey()).isEqualTo(paymentKey);
            assertThat(request.orderId()).isEqualTo(orderId);
            assertThat(request.amount()).isEqualTo(amount);
        }

        @Test
        @DisplayName("should create TossConfirmRequest via constructor")
        void shouldCreateViaConstructor() {
            // Given
            String paymentKey = "confirm_payment_key_789";
            String orderId = "confirm_order_012";
            BigDecimal amount = new BigDecimal("15000");

            // When
            TossConfirmRequest request = new TossConfirmRequest(paymentKey, orderId, amount);

            // Then
            assertThat(request.paymentKey()).isEqualTo(paymentKey);
            assertThat(request.orderId()).isEqualTo(orderId);
            assertThat(request.amount()).isEqualTo(amount);
        }

        @Test
        @DisplayName("should serialize TossConfirmRequest to JSON")
        void shouldSerializeToJson() throws Exception {
            // Given
            TossConfirmRequest request = TossConfirmRequest.of(
                "confirm_key_def",
                "confirm_order_ghi",
                new BigDecimal("40000")
            );

            // When
            String json = objectMapper.writeValueAsString(request);

            // Then
            assertThat(json).contains("\"paymentKey\":\"confirm_key_def\"");
            assertThat(json).contains("\"orderId\":\"confirm_order_ghi\"");
            assertThat(json).contains("\"amount\":40000");
        }
    }

    @Nested
    @DisplayName("TossCancelRequest")
    class TossCancelRequestTests {

        @Test
        @DisplayName("should create TossCancelRequest via factory method")
        void shouldCreateViaFactoryMethod() {
            // Given
            String cancelReason = "Customer requested cancellation";

            // When
            TossCancelRequest request = TossCancelRequest.of(cancelReason);

            // Then
            assertThat(request.cancelReason()).isEqualTo(cancelReason);
        }

        @Test
        @DisplayName("should create TossCancelRequest via constructor")
        void shouldCreateViaConstructor() {
            // Given
            String cancelReason = "Order cancelled by merchant";

            // When
            TossCancelRequest request = new TossCancelRequest(cancelReason);

            // Then
            assertThat(request.cancelReason()).isEqualTo(cancelReason);
        }

        @Test
        @DisplayName("should serialize TossCancelRequest to JSON")
        void shouldSerializeToJson() throws Exception {
            // Given
            TossCancelRequest request = TossCancelRequest.of("Duplicate payment");

            // When
            String json = objectMapper.writeValueAsString(request);

            // Then
            assertThat(json).contains("\"cancelReason\":\"Duplicate payment\"");
        }
    }

    @Nested
    @DisplayName("TossErrorResponse")
    class TossErrorResponseTests {

        @Test
        @DisplayName("should deserialize TossErrorResponse from JSON")
        void shouldDeserializeFromJson() throws Exception {
            // Given
            String json = """
                {
                    "code": "INVALID_PAYMENT_KEY",
                    "message": "The payment key is invalid"
                }
                """;

            // When
            TossErrorResponse response = objectMapper.readValue(json, TossErrorResponse.class);

            // Then
            assertThat(response.code()).isEqualTo("INVALID_PAYMENT_KEY");
            assertThat(response.message()).isEqualTo("The payment key is invalid");
        }

        @Test
        @DisplayName("should deserialize TossErrorResponse ignoring unknown properties")
        void shouldDeserializeIgnoringUnknownProperties() throws Exception {
            // Given
            String json = """
                {
                    "code": "AMOUNT_MISMATCH",
                    "message": "Amount does not match",
                    "unknownField": "some value",
                    "anotherUnknown": 12345
                }
                """;

            // When
            TossErrorResponse response = objectMapper.readValue(json, TossErrorResponse.class);

            // Then
            assertThat(response.code()).isEqualTo("AMOUNT_MISMATCH");
            assertThat(response.message()).isEqualTo("Amount does not match");
        }

        @Test
        @DisplayName("should format error message correctly via toErrorMessage")
        void shouldFormatErrorMessageCorrectly() {
            // Given
            TossErrorResponse response = new TossErrorResponse(
                "PAYMENT_FAILED",
                "Payment processing failed"
            );

            // When
            String errorMessage = response.toErrorMessage();

            // Then
            assertThat(errorMessage).isEqualTo("[PAYMENT_FAILED] Payment processing failed");
        }

        @Test
        @DisplayName("should create TossErrorResponse via constructor")
        void shouldCreateViaConstructor() {
            // Given
            String code = "ALREADY_CANCELED";
            String message = "Payment has already been canceled";

            // When
            TossErrorResponse response = new TossErrorResponse(code, message);

            // Then
            assertThat(response.code()).isEqualTo(code);
            assertThat(response.message()).isEqualTo(message);
        }
    }
}
