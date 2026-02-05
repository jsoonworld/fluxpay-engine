package com.fluxpay.engine.infrastructure.external.pg.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TossResponseDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Nested
    @DisplayName("TossPaymentResponse")
    class TossPaymentResponseTests {

        @Test
        @DisplayName("Should deserialize success response with all fields")
        void shouldDeserializeSuccessResponse() throws Exception {
            String json = """
                {
                    "paymentKey": "pk_test_abc123",
                    "orderId": "order_123",
                    "orderName": "Test Product",
                    "status": "DONE",
                    "totalAmount": 10000,
                    "method": "카드",
                    "transactionKey": "tx_123",
                    "approvedAt": "2026-02-04T10:30:00+09:00",
                    "receipt": {
                        "url": "https://receipt.tosspayments.com/test"
                    }
                }
                """;

            TossPaymentResponse response = objectMapper.readValue(json, TossPaymentResponse.class);

            assertThat(response.paymentKey()).isEqualTo("pk_test_abc123");
            assertThat(response.orderId()).isEqualTo("order_123");
            assertThat(response.orderName()).isEqualTo("Test Product");
            assertThat(response.status()).isEqualTo("DONE");
            assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(10000));
            assertThat(response.method()).isEqualTo("카드");
            assertThat(response.transactionKey()).isEqualTo("tx_123");
            assertThat(response.approvedAt()).isNotNull();
            assertThat(response.receipt()).isNotNull();
            assertThat(response.receipt().url()).isEqualTo("https://receipt.tosspayments.com/test");
        }

        @Test
        @DisplayName("Should return true for isApproved when status is DONE")
        void shouldReturnTrueForIsApprovedWhenStatusIsDone() throws Exception {
            String json = """
                {
                    "paymentKey": "pk_test",
                    "orderId": "order_123",
                    "status": "DONE",
                    "totalAmount": 10000
                }
                """;

            TossPaymentResponse response = objectMapper.readValue(json, TossPaymentResponse.class);

            assertThat(response.isApproved()).isTrue();
        }

        @Test
        @DisplayName("Should return false for isApproved when status is WAITING_FOR_DEPOSIT")
        void shouldReturnFalseForIsApprovedWhenStatusIsWaitingForDeposit() throws Exception {
            String json = """
                {
                    "paymentKey": "pk_test",
                    "orderId": "order_123",
                    "status": "WAITING_FOR_DEPOSIT",
                    "totalAmount": 10000
                }
                """;

            TossPaymentResponse response = objectMapper.readValue(json, TossPaymentResponse.class);

            assertThat(response.isApproved()).isFalse();
        }

        @Test
        @DisplayName("Should return false for isApproved when status is IN_PROGRESS")
        void shouldReturnFalseForIsApprovedWhenStatusIsInProgress() throws Exception {
            String json = """
                {
                    "paymentKey": "pk_test",
                    "orderId": "order_123",
                    "status": "IN_PROGRESS",
                    "totalAmount": 10000
                }
                """;

            TossPaymentResponse response = objectMapper.readValue(json, TossPaymentResponse.class);

            assertThat(response.isApproved()).isFalse();
        }

        @Test
        @DisplayName("Should ignore unknown properties during deserialization")
        void shouldIgnoreUnknownProperties() throws Exception {
            String json = """
                {
                    "paymentKey": "pk_test",
                    "orderId": "order_123",
                    "status": "DONE",
                    "totalAmount": 10000,
                    "unknownField": "some value",
                    "anotherUnknown": 12345
                }
                """;

            TossPaymentResponse response = objectMapper.readValue(json, TossPaymentResponse.class);

            assertThat(response.paymentKey()).isEqualTo("pk_test");
            assertThat(response.status()).isEqualTo("DONE");
        }

        @Test
        @DisplayName("Should handle null receipt")
        void shouldHandleNullReceipt() throws Exception {
            String json = """
                {
                    "paymentKey": "pk_test",
                    "orderId": "order_123",
                    "status": "DONE",
                    "totalAmount": 10000
                }
                """;

            TossPaymentResponse response = objectMapper.readValue(json, TossPaymentResponse.class);

            assertThat(response.receipt()).isNull();
        }

        @Test
        @DisplayName("Should correctly parse approvedAt timestamp with timezone")
        void shouldCorrectlyParseApprovedAtTimestamp() throws Exception {
            String json = """
                {
                    "paymentKey": "pk_test",
                    "orderId": "order_123",
                    "status": "DONE",
                    "totalAmount": 10000,
                    "approvedAt": "2026-02-04T10:30:00+09:00"
                }
                """;

            TossPaymentResponse response = objectMapper.readValue(json, TossPaymentResponse.class);

            assertThat(response.approvedAt()).isNotNull();
            // Verify the timestamp represents the correct instant in time
            // 10:30:00+09:00 = 01:30:00 UTC
            OffsetDateTime expected = OffsetDateTime.of(2026, 2, 4, 10, 30, 0, 0, ZoneOffset.ofHours(9));
            assertThat(response.approvedAt().toInstant()).isEqualTo(expected.toInstant());
        }
    }

    @Nested
    @DisplayName("TossCancelResponse")
    class TossCancelResponseTests {

        @Test
        @DisplayName("Should deserialize cancel response with cancels list")
        void shouldDeserializeCancelResponseWithCancelsList() throws Exception {
            String json = """
                {
                    "paymentKey": "pk_test_abc123",
                    "orderId": "order_123",
                    "status": "CANCELED",
                    "cancels": [
                        {
                            "cancelAmount": 5000,
                            "cancelReason": "Customer request",
                            "canceledAt": "2026-02-04T11:00:00+09:00",
                            "transactionKey": "tx_cancel_123"
                        }
                    ]
                }
                """;

            TossCancelResponse response = objectMapper.readValue(json, TossCancelResponse.class);

            assertThat(response.paymentKey()).isEqualTo("pk_test_abc123");
            assertThat(response.orderId()).isEqualTo("order_123");
            assertThat(response.status()).isEqualTo("CANCELED");
            assertThat(response.cancels()).hasSize(1);
            assertThat(response.cancels().get(0).cancelAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000));
            assertThat(response.cancels().get(0).cancelReason()).isEqualTo("Customer request");
            assertThat(response.cancels().get(0).transactionKey()).isEqualTo("tx_cancel_123");
        }

        @Test
        @DisplayName("Should return true for isCancelled when status is CANCELED")
        void shouldReturnTrueForIsCancelledWhenStatusIsCanceled() throws Exception {
            String json = """
                {
                    "paymentKey": "pk_test",
                    "orderId": "order_123",
                    "status": "CANCELED",
                    "cancels": []
                }
                """;

            TossCancelResponse response = objectMapper.readValue(json, TossCancelResponse.class);

            assertThat(response.isCancelled()).isTrue();
        }

        @Test
        @DisplayName("Should return false for isCancelled when status is DONE")
        void shouldReturnFalseForIsCancelledWhenStatusIsDone() throws Exception {
            String json = """
                {
                    "paymentKey": "pk_test",
                    "orderId": "order_123",
                    "status": "DONE",
                    "cancels": []
                }
                """;

            TossCancelResponse response = objectMapper.readValue(json, TossCancelResponse.class);

            assertThat(response.isCancelled()).isFalse();
        }

        @Test
        @DisplayName("Should return false for isCancelled when status is PARTIAL_CANCELED")
        void shouldReturnFalseForIsCancelledWhenStatusIsPartialCanceled() throws Exception {
            String json = """
                {
                    "paymentKey": "pk_test",
                    "orderId": "order_123",
                    "status": "PARTIAL_CANCELED",
                    "cancels": []
                }
                """;

            TossCancelResponse response = objectMapper.readValue(json, TossCancelResponse.class);

            assertThat(response.isCancelled()).isFalse();
        }

        @Test
        @DisplayName("Should deserialize multiple cancels for partial cancellations")
        void shouldDeserializeMultipleCancels() throws Exception {
            String json = """
                {
                    "paymentKey": "pk_test_abc123",
                    "orderId": "order_123",
                    "status": "CANCELED",
                    "cancels": [
                        {
                            "cancelAmount": 3000,
                            "cancelReason": "First partial cancel",
                            "canceledAt": "2026-02-04T11:00:00+09:00",
                            "transactionKey": "tx_cancel_1"
                        },
                        {
                            "cancelAmount": 7000,
                            "cancelReason": "Final cancel",
                            "canceledAt": "2026-02-04T12:00:00+09:00",
                            "transactionKey": "tx_cancel_2"
                        }
                    ]
                }
                """;

            TossCancelResponse response = objectMapper.readValue(json, TossCancelResponse.class);

            assertThat(response.cancels()).hasSize(2);
            assertThat(response.cancels().get(0).cancelAmount()).isEqualByComparingTo(BigDecimal.valueOf(3000));
            assertThat(response.cancels().get(1).cancelAmount()).isEqualByComparingTo(BigDecimal.valueOf(7000));
        }

        @Test
        @DisplayName("Should ignore unknown properties during deserialization")
        void shouldIgnoreUnknownProperties() throws Exception {
            String json = """
                {
                    "paymentKey": "pk_test",
                    "orderId": "order_123",
                    "status": "CANCELED",
                    "cancels": [],
                    "unknownField": "some value"
                }
                """;

            TossCancelResponse response = objectMapper.readValue(json, TossCancelResponse.class);

            assertThat(response.paymentKey()).isEqualTo("pk_test");
            assertThat(response.status()).isEqualTo("CANCELED");
        }

        @Test
        @DisplayName("Should correctly parse canceledAt timestamp with timezone")
        void shouldCorrectlyParseCanceledAtTimestamp() throws Exception {
            String json = """
                {
                    "paymentKey": "pk_test",
                    "orderId": "order_123",
                    "status": "CANCELED",
                    "cancels": [
                        {
                            "cancelAmount": 5000,
                            "cancelReason": "Test",
                            "canceledAt": "2026-02-04T14:30:00+09:00",
                            "transactionKey": "tx_123"
                        }
                    ]
                }
                """;

            TossCancelResponse response = objectMapper.readValue(json, TossCancelResponse.class);

            assertThat(response.cancels().get(0).canceledAt()).isNotNull();
            // Verify the timestamp represents the correct instant in time
            // 14:30:00+09:00 = 05:30:00 UTC
            OffsetDateTime expected = OffsetDateTime.of(2026, 2, 4, 14, 30, 0, 0, ZoneOffset.ofHours(9));
            assertThat(response.cancels().get(0).canceledAt().toInstant()).isEqualTo(expected.toInstant());
        }
    }
}
