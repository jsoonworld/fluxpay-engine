package com.fluxpay.engine.presentation.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RequestDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("CreateOrderRequest Tests")
    class CreateOrderRequestTest {

        @Test
        @DisplayName("should create valid CreateOrderRequest")
        void shouldCreateValidCreateOrderRequest() {
            // Given
            var lineItem = new CreateOrderRequest.LineItemRequest(
                "prod-123",
                "Test Product",
                2,
                new BigDecimal("10000.00")
            );
            var request = new CreateOrderRequest(
                "user-123",
                List.of(lineItem),
                "KRW",
                Map.of("key", "value")
            );

            // When
            Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
            assertThat(request.userId()).isEqualTo("user-123");
            assertThat(request.lineItems()).hasSize(1);
            assertThat(request.currency()).isEqualTo("KRW");
            assertThat(request.metadata()).containsEntry("key", "value");
        }

        @Test
        @DisplayName("should fail validation when userId is blank")
        void shouldFailWhenUserIdIsBlank() {
            // Given
            var lineItem = new CreateOrderRequest.LineItemRequest(
                "prod-123",
                "Test Product",
                2,
                new BigDecimal("10000.00")
            );
            var request = new CreateOrderRequest(
                "",
                List.of(lineItem),
                "KRW",
                null
            );

            // When
            Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("사용자 ID는 필수입니다");
        }

        @Test
        @DisplayName("should fail validation when userId exceeds max length")
        void shouldFailWhenUserIdExceedsMaxLength() {
            // Given
            String longUserId = "a".repeat(101);
            var lineItem = new CreateOrderRequest.LineItemRequest(
                "prod-123",
                "Test Product",
                2,
                new BigDecimal("10000.00")
            );
            var request = new CreateOrderRequest(
                longUserId,
                List.of(lineItem),
                "KRW",
                null
            );

            // When
            Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("사용자 ID는 100자 이하여야 합니다");
        }

        @Test
        @DisplayName("should fail validation when lineItems is empty")
        void shouldFailWhenLineItemsIsEmpty() {
            // Given
            var request = new CreateOrderRequest(
                "user-123",
                List.of(),
                "KRW",
                null
            );

            // When
            Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("주문 항목은 최소 1개 이상이어야 합니다");
        }

        @Test
        @DisplayName("should fail validation when currency is invalid")
        void shouldFailWhenCurrencyIsInvalid() {
            // Given
            var lineItem = new CreateOrderRequest.LineItemRequest(
                "prod-123",
                "Test Product",
                2,
                new BigDecimal("10000.00")
            );
            var request = new CreateOrderRequest(
                "user-123",
                List.of(lineItem),
                "GBP",
                null
            );

            // When
            Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("지원하지 않는 통화입니다");
        }

        @Test
        @DisplayName("should accept all supported currencies")
        void shouldAcceptAllSupportedCurrencies() {
            // Given
            List<String> supportedCurrencies = List.of("KRW", "USD", "JPY", "EUR");

            for (String currency : supportedCurrencies) {
                var lineItem = new CreateOrderRequest.LineItemRequest(
                    "prod-123",
                    "Test Product",
                    1,
                    new BigDecimal("100.00")
                );
                var request = new CreateOrderRequest(
                    "user-123",
                    List.of(lineItem),
                    currency,
                    null
                );

                // When
                Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);

                // Then
                assertThat(violations).isEmpty();
            }
        }

        @Nested
        @DisplayName("LineItemRequest Tests")
        class LineItemRequestTest {

            @Test
            @DisplayName("should create valid LineItemRequest")
            void shouldCreateValidLineItemRequest() {
                // Given
                var lineItem = new CreateOrderRequest.LineItemRequest(
                    "prod-123",
                    "Test Product",
                    5,
                    new BigDecimal("15000.50")
                );

                // When
                Set<ConstraintViolation<CreateOrderRequest.LineItemRequest>> violations = validator.validate(lineItem);

                // Then
                assertThat(violations).isEmpty();
                assertThat(lineItem.productId()).isEqualTo("prod-123");
                assertThat(lineItem.productName()).isEqualTo("Test Product");
                assertThat(lineItem.quantity()).isEqualTo(5);
                assertThat(lineItem.unitPrice()).isEqualTo(new BigDecimal("15000.50"));
            }

            @Test
            @DisplayName("should fail validation when productId is blank")
            void shouldFailWhenProductIdIsBlank() {
                // Given
                var lineItem = new CreateOrderRequest.LineItemRequest(
                    "",
                    "Test Product",
                    1,
                    new BigDecimal("100.00")
                );

                // When
                Set<ConstraintViolation<CreateOrderRequest.LineItemRequest>> violations = validator.validate(lineItem);

                // Then
                assertThat(violations).hasSize(1);
                assertThat(violations.iterator().next().getMessage()).isEqualTo("상품 ID는 필수입니다");
            }

            @Test
            @DisplayName("should fail validation when productName is blank")
            void shouldFailWhenProductNameIsBlank() {
                // Given
                var lineItem = new CreateOrderRequest.LineItemRequest(
                    "prod-123",
                    "",
                    1,
                    new BigDecimal("100.00")
                );

                // When
                Set<ConstraintViolation<CreateOrderRequest.LineItemRequest>> violations = validator.validate(lineItem);

                // Then
                assertThat(violations).hasSize(1);
                assertThat(violations.iterator().next().getMessage()).isEqualTo("상품명은 필수입니다");
            }

            @Test
            @DisplayName("should fail validation when productName exceeds max length")
            void shouldFailWhenProductNameExceedsMaxLength() {
                // Given
                String longProductName = "a".repeat(256);
                var lineItem = new CreateOrderRequest.LineItemRequest(
                    "prod-123",
                    longProductName,
                    1,
                    new BigDecimal("100.00")
                );

                // When
                Set<ConstraintViolation<CreateOrderRequest.LineItemRequest>> violations = validator.validate(lineItem);

                // Then
                assertThat(violations).hasSize(1);
                assertThat(violations.iterator().next().getMessage()).isEqualTo("상품명은 255자 이하여야 합니다");
            }

            @Test
            @DisplayName("should fail validation when quantity is null")
            void shouldFailWhenQuantityIsNull() {
                // Given
                var lineItem = new CreateOrderRequest.LineItemRequest(
                    "prod-123",
                    "Test Product",
                    null,
                    new BigDecimal("100.00")
                );

                // When
                Set<ConstraintViolation<CreateOrderRequest.LineItemRequest>> violations = validator.validate(lineItem);

                // Then
                assertThat(violations).hasSize(1);
                assertThat(violations.iterator().next().getMessage()).isEqualTo("수량은 필수입니다");
            }

            @Test
            @DisplayName("should fail validation when quantity is less than 1")
            void shouldFailWhenQuantityIsLessThanOne() {
                // Given
                var lineItem = new CreateOrderRequest.LineItemRequest(
                    "prod-123",
                    "Test Product",
                    0,
                    new BigDecimal("100.00")
                );

                // When
                Set<ConstraintViolation<CreateOrderRequest.LineItemRequest>> violations = validator.validate(lineItem);

                // Then
                assertThat(violations).hasSize(1);
                assertThat(violations.iterator().next().getMessage()).isEqualTo("수량은 1 이상이어야 합니다");
            }

            @Test
            @DisplayName("should fail validation when unitPrice is null")
            void shouldFailWhenUnitPriceIsNull() {
                // Given
                var lineItem = new CreateOrderRequest.LineItemRequest(
                    "prod-123",
                    "Test Product",
                    1,
                    null
                );

                // When
                Set<ConstraintViolation<CreateOrderRequest.LineItemRequest>> violations = validator.validate(lineItem);

                // Then
                assertThat(violations).hasSize(1);
                assertThat(violations.iterator().next().getMessage()).isEqualTo("단가는 필수입니다");
            }

            @Test
            @DisplayName("should fail validation when unitPrice is zero")
            void shouldFailWhenUnitPriceIsZero() {
                // Given
                var lineItem = new CreateOrderRequest.LineItemRequest(
                    "prod-123",
                    "Test Product",
                    1,
                    BigDecimal.ZERO
                );

                // When
                Set<ConstraintViolation<CreateOrderRequest.LineItemRequest>> violations = validator.validate(lineItem);

                // Then
                assertThat(violations).hasSize(1);
                assertThat(violations.iterator().next().getMessage()).isEqualTo("단가는 0보다 커야 합니다");
            }

            @Test
            @DisplayName("should fail validation when unitPrice is negative")
            void shouldFailWhenUnitPriceIsNegative() {
                // Given
                var lineItem = new CreateOrderRequest.LineItemRequest(
                    "prod-123",
                    "Test Product",
                    1,
                    new BigDecimal("-100.00")
                );

                // When
                Set<ConstraintViolation<CreateOrderRequest.LineItemRequest>> violations = validator.validate(lineItem);

                // Then
                assertThat(violations).hasSize(1);
                assertThat(violations.iterator().next().getMessage()).isEqualTo("단가는 0보다 커야 합니다");
            }
        }

        @Test
        @DisplayName("should validate nested LineItemRequest")
        void shouldValidateNestedLineItemRequest() {
            // Given - Invalid nested LineItemRequest
            var invalidLineItem = new CreateOrderRequest.LineItemRequest(
                "",
                "",
                0,
                BigDecimal.ZERO
            );
            var request = new CreateOrderRequest(
                "user-123",
                List.of(invalidLineItem),
                "KRW",
                null
            );

            // When
            Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(4);
        }
    }

    @Nested
    @DisplayName("CreatePaymentRequest Tests")
    class CreatePaymentRequestTest {

        @Test
        @DisplayName("should create valid CreatePaymentRequest with UUID orderId")
        void shouldCreateValidCreatePaymentRequest() {
            // Given
            String validUuid = "550e8400-e29b-41d4-a716-446655440000";
            var request = new CreatePaymentRequest(
                validUuid,
                new BigDecimal("50000.00"),
                "KRW"
            );

            // When
            Set<ConstraintViolation<CreatePaymentRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
            assertThat(request.orderId()).isEqualTo(validUuid);
            assertThat(request.amount()).isEqualTo(new BigDecimal("50000.00"));
            assertThat(request.currency()).isEqualTo("KRW");
        }

        @Test
        @DisplayName("should fail validation when orderId is not a valid UUID format")
        void shouldFailWhenOrderIdIsNotUuid() {
            // Given
            var request = new CreatePaymentRequest(
                "order-123",
                new BigDecimal("50000.00"),
                "KRW"
            );

            // When
            Set<ConstraintViolation<CreatePaymentRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("주문 ID는 UUID 형식이어야 합니다");
        }

        @Test
        @DisplayName("should fail validation when orderId is blank")
        void shouldFailWhenOrderIdIsBlank() {
            // Given
            var request = new CreatePaymentRequest(
                "",
                new BigDecimal("50000.00"),
                "KRW"
            );

            // When
            Set<ConstraintViolation<CreatePaymentRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).isNotEmpty();
            assertThat(violations.stream().anyMatch(v -> v.getMessage().equals("주문 ID는 필수입니다"))).isTrue();
        }

        @Test
        @DisplayName("should fail validation when amount is null")
        void shouldFailWhenAmountIsNull() {
            // Given
            String validUuid = "550e8400-e29b-41d4-a716-446655440000";
            var request = new CreatePaymentRequest(
                validUuid,
                null,
                "KRW"
            );

            // When
            Set<ConstraintViolation<CreatePaymentRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("결제 금액은 필수입니다");
        }

        @Test
        @DisplayName("should fail validation when amount is zero")
        void shouldFailWhenAmountIsZero() {
            // Given
            String validUuid = "550e8400-e29b-41d4-a716-446655440000";
            var request = new CreatePaymentRequest(
                validUuid,
                BigDecimal.ZERO,
                "KRW"
            );

            // When
            Set<ConstraintViolation<CreatePaymentRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("결제 금액은 0보다 커야 합니다");
        }

        @Test
        @DisplayName("should fail validation when amount is negative")
        void shouldFailWhenAmountIsNegative() {
            // Given
            String validUuid = "550e8400-e29b-41d4-a716-446655440000";
            var request = new CreatePaymentRequest(
                validUuid,
                new BigDecimal("-1000.00"),
                "KRW"
            );

            // When
            Set<ConstraintViolation<CreatePaymentRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("결제 금액은 0보다 커야 합니다");
        }

        @Test
        @DisplayName("should fail validation when currency is blank")
        void shouldFailWhenCurrencyIsBlank() {
            // Given
            String validUuid = "550e8400-e29b-41d4-a716-446655440000";
            var request = new CreatePaymentRequest(
                validUuid,
                new BigDecimal("50000.00"),
                ""
            );

            // When
            Set<ConstraintViolation<CreatePaymentRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).isNotEmpty();
            assertThat(violations.stream().anyMatch(v -> v.getMessage().equals("통화는 필수입니다"))).isTrue();
        }

        @Test
        @DisplayName("should fail validation when currency is invalid")
        void shouldFailWhenCurrencyIsInvalid() {
            // Given
            String validUuid = "550e8400-e29b-41d4-a716-446655440000";
            var request = new CreatePaymentRequest(
                validUuid,
                new BigDecimal("50000.00"),
                "GBP"
            );

            // When
            Set<ConstraintViolation<CreatePaymentRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("지원하지 않는 통화입니다");
        }

        @Test
        @DisplayName("should accept all supported currencies")
        void shouldAcceptAllSupportedCurrencies() {
            // Given
            String validUuid = "550e8400-e29b-41d4-a716-446655440000";
            List<String> supportedCurrencies = List.of("KRW", "USD", "JPY", "EUR");

            for (String currency : supportedCurrencies) {
                var request = new CreatePaymentRequest(
                    validUuid,
                    new BigDecimal("50000.00"),
                    currency
                );

                // When
                Set<ConstraintViolation<CreatePaymentRequest>> violations = validator.validate(request);

                // Then
                assertThat(violations).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("ApprovePaymentRequest Tests")
    class ApprovePaymentRequestTest {

        @Test
        @DisplayName("should create valid ApprovePaymentRequest")
        void shouldCreateValidApprovePaymentRequest() {
            // Given
            var request = new ApprovePaymentRequest("CARD", null);

            // When
            Set<ConstraintViolation<ApprovePaymentRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
            assertThat(request.paymentMethod()).isEqualTo("CARD");
            assertThat(request.easyPayProvider()).isNull();
        }

        @Test
        @DisplayName("should create valid ApprovePaymentRequest with easyPayProvider")
        void shouldCreateValidApprovePaymentRequestWithEasyPayProvider() {
            // Given
            var request = new ApprovePaymentRequest("EASY_PAY", "KAKAOPAY");

            // When
            Set<ConstraintViolation<ApprovePaymentRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
            assertThat(request.paymentMethod()).isEqualTo("EASY_PAY");
            assertThat(request.easyPayProvider()).isEqualTo("KAKAOPAY");
        }

        @Test
        @DisplayName("should fail validation when EASY_PAY is selected without easyPayProvider")
        void shouldFailWhenEasyPayWithoutProvider() {
            // Given
            var request = new ApprovePaymentRequest("EASY_PAY", null);

            // When
            Set<ConstraintViolation<ApprovePaymentRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("간편결제 시 easyPayProvider는 필수입니다");
        }

        @Test
        @DisplayName("should fail validation when EASY_PAY is selected with blank easyPayProvider")
        void shouldFailWhenEasyPayWithBlankProvider() {
            // Given
            var request = new ApprovePaymentRequest("EASY_PAY", "   ");

            // When
            Set<ConstraintViolation<ApprovePaymentRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("간편결제 시 easyPayProvider는 필수입니다");
        }

        @Test
        @DisplayName("should fail validation when paymentMethod is blank")
        void shouldFailWhenPaymentMethodIsBlank() {
            // Given
            var request = new ApprovePaymentRequest("", null);

            // When
            Set<ConstraintViolation<ApprovePaymentRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).isNotEmpty();
            assertThat(violations.stream().anyMatch(v -> v.getMessage().equals("결제 수단은 필수입니다"))).isTrue();
        }

        @Test
        @DisplayName("should fail validation when paymentMethod is invalid")
        void shouldFailWhenPaymentMethodIsInvalid() {
            // Given
            var request = new ApprovePaymentRequest("BITCOIN", null);

            // When
            Set<ConstraintViolation<ApprovePaymentRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("지원하지 않는 결제 수단입니다");
        }

        @Test
        @DisplayName("should accept all supported payment methods")
        void shouldAcceptAllSupportedPaymentMethods() {
            // Given - EASY_PAY requires a provider, others don't
            List<String> nonEasyPayMethods = List.of("CARD", "BANK_TRANSFER", "VIRTUAL_ACCOUNT", "MOBILE");

            for (String method : nonEasyPayMethods) {
                var request = new ApprovePaymentRequest(method, null);

                // When
                Set<ConstraintViolation<ApprovePaymentRequest>> violations = validator.validate(request);

                // Then
                assertThat(violations).isEmpty();
            }

            // EASY_PAY with provider
            var easyPayRequest = new ApprovePaymentRequest("EASY_PAY", "KAKAOPAY");
            Set<ConstraintViolation<ApprovePaymentRequest>> easyPayViolations = validator.validate(easyPayRequest);
            assertThat(easyPayViolations).isEmpty();
        }
    }
}
