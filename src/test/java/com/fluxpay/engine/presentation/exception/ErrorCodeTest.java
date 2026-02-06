package com.fluxpay.engine.presentation.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorCode")
class ErrorCodeTest {

    @Nested
    @DisplayName("Order Errors (ORD_xxx)")
    class OrderErrors {

        @Test
        @DisplayName("ORDER_NOT_FOUND should have correct code, status, and message")
        void orderNotFoundShouldHaveCorrectValues() {
            ErrorCode errorCode = ErrorCode.ORDER_NOT_FOUND;

            assertThat(errorCode.getCode()).isEqualTo("ORD_001");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(errorCode.getMessage()).isEqualTo("Order not found");
        }

        @Test
        @DisplayName("ORDER_ALREADY_PROCESSED should have correct code, status, and message")
        void orderAlreadyProcessedShouldHaveCorrectValues() {
            ErrorCode errorCode = ErrorCode.ORDER_ALREADY_PROCESSED;

            assertThat(errorCode.getCode()).isEqualTo("ORD_002");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(errorCode.getMessage()).isEqualTo("Order has already been processed");
        }

        @Test
        @DisplayName("INVALID_ORDER_STATE should have correct code, status, and message")
        void invalidOrderStateShouldHaveCorrectValues() {
            ErrorCode errorCode = ErrorCode.INVALID_ORDER_STATE;

            assertThat(errorCode.getCode()).isEqualTo("ORD_003");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(errorCode.getMessage()).isEqualTo("Invalid order state transition");
        }
    }

    @Nested
    @DisplayName("Payment Errors (PAY_xxx)")
    class PaymentErrors {

        @Test
        @DisplayName("PAYMENT_NOT_FOUND should have correct code, status, and message")
        void paymentNotFoundShouldHaveCorrectValues() {
            ErrorCode errorCode = ErrorCode.PAYMENT_NOT_FOUND;

            assertThat(errorCode.getCode()).isEqualTo("PAY_001");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(errorCode.getMessage()).isEqualTo("Payment not found");
        }

        @Test
        @DisplayName("INVALID_PAYMENT_AMOUNT should have correct code, status, and message")
        void invalidPaymentAmountShouldHaveCorrectValues() {
            ErrorCode errorCode = ErrorCode.INVALID_PAYMENT_AMOUNT;

            assertThat(errorCode.getCode()).isEqualTo("PAY_002");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(errorCode.getMessage()).isEqualTo("Invalid payment amount");
        }

        @Test
        @DisplayName("PAYMENT_ALREADY_PROCESSED should have correct code, status, and message")
        void paymentAlreadyProcessedShouldHaveCorrectValues() {
            ErrorCode errorCode = ErrorCode.PAYMENT_ALREADY_PROCESSED;

            assertThat(errorCode.getCode()).isEqualTo("PAY_003");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(errorCode.getMessage()).isEqualTo("Payment has already been processed");
        }

        @Test
        @DisplayName("UNSUPPORTED_PAYMENT_METHOD should have correct code, status, and message")
        void unsupportedPaymentMethodShouldHaveCorrectValues() {
            ErrorCode errorCode = ErrorCode.UNSUPPORTED_PAYMENT_METHOD;

            assertThat(errorCode.getCode()).isEqualTo("PAY_004");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(errorCode.getMessage()).isEqualTo("Unsupported payment method");
        }

        @Test
        @DisplayName("PG_CONNECTION_ERROR should have correct code, status, and message")
        void pgConnectionErrorShouldHaveCorrectValues() {
            ErrorCode errorCode = ErrorCode.PG_CONNECTION_ERROR;

            assertThat(errorCode.getCode()).isEqualTo("PAY_005");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
            assertThat(errorCode.getMessage()).isEqualTo("Payment gateway connection error");
        }

        @Test
        @DisplayName("INVALID_PAYMENT_STATE should have correct code, status, and message")
        void invalidPaymentStateShouldHaveCorrectValues() {
            ErrorCode errorCode = ErrorCode.INVALID_PAYMENT_STATE;

            assertThat(errorCode.getCode()).isEqualTo("PAY_006");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(errorCode.getMessage()).isEqualTo("Invalid payment state transition");
        }
    }

    @Nested
    @DisplayName("Validation Errors (VAL_xxx)")
    class ValidationErrors {

        @Test
        @DisplayName("VALIDATION_FAILED should have correct code, status, and message")
        void validationFailedShouldHaveCorrectValues() {
            ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;

            assertThat(errorCode.getCode()).isEqualTo("VAL_001");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(errorCode.getMessage()).isEqualTo("Request validation failed");
        }

        @Test
        @DisplayName("IDEMPOTENCY_KEY_MISSING should have correct code, status, and message")
        void idempotencyKeyMissingShouldHaveCorrectValues() {
            ErrorCode errorCode = ErrorCode.IDEMPOTENCY_KEY_MISSING;

            assertThat(errorCode.getCode()).isEqualTo("VAL_002");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(errorCode.getMessage()).isEqualTo("X-Idempotency-Key header is required");
        }

        @Test
        @DisplayName("IDEMPOTENCY_KEY_INVALID should have correct code, status, and message")
        void idempotencyKeyInvalidShouldHaveCorrectValues() {
            ErrorCode errorCode = ErrorCode.IDEMPOTENCY_KEY_INVALID;

            assertThat(errorCode.getCode()).isEqualTo("VAL_003");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(errorCode.getMessage()).isEqualTo("Invalid idempotency key format (UUID required)");
        }

        @Test
        @DisplayName("IDEMPOTENCY_CONFLICT should have correct code, status, and message")
        void idempotencyConflictShouldHaveCorrectValues() {
            ErrorCode errorCode = ErrorCode.IDEMPOTENCY_CONFLICT;

            assertThat(errorCode.getCode()).isEqualTo("VAL_004");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(errorCode.getMessage()).isEqualTo("Different payload used with the same idempotency key");
        }

        @Test
        @DisplayName("IDEMPOTENCY_PROCESSING should have correct code, status, and message")
        void idempotencyProcessingShouldHaveCorrectValues() {
            ErrorCode errorCode = ErrorCode.IDEMPOTENCY_PROCESSING;

            assertThat(errorCode.getCode()).isEqualTo("VAL_005");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(errorCode.getMessage()).isEqualTo("Request with the same idempotency key is being processed. Please retry later");
        }
    }

    @Nested
    @DisplayName("Tenant Errors (TNT_xxx)")
    class TenantErrors {

        @Test
        @DisplayName("TENANT_HEADER_MISSING should have correct code, status, and message")
        void shouldHaveTenantHeaderMissingErrorCode() {
            ErrorCode errorCode = ErrorCode.TENANT_HEADER_MISSING;

            assertThat(errorCode.getCode()).isEqualTo("TNT_001");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(errorCode.getMessage()).isEqualTo("X-Tenant-Id header is required");
        }

        @Test
        @DisplayName("TENANT_NOT_FOUND should have correct code, status, and message")
        void shouldHaveTenantNotFoundErrorCode() {
            ErrorCode errorCode = ErrorCode.TENANT_NOT_FOUND;

            assertThat(errorCode.getCode()).isEqualTo("TNT_002");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(errorCode.getMessage()).isEqualTo("Unknown tenant");
        }
    }

    @Nested
    @DisplayName("System Errors (SYS_xxx)")
    class SystemErrors {

        @Test
        @DisplayName("INTERNAL_SERVER_ERROR should have correct code, status, and message")
        void internalServerErrorShouldHaveCorrectValues() {
            ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

            assertThat(errorCode.getCode()).isEqualTo("SYS_001");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(errorCode.getMessage()).isEqualTo("Internal server error");
        }

        @Test
        @DisplayName("SERVICE_UNAVAILABLE should have correct code, status, and message")
        void serviceUnavailableShouldHaveCorrectValues() {
            ErrorCode errorCode = ErrorCode.SERVICE_UNAVAILABLE;

            assertThat(errorCode.getCode()).isEqualTo("SYS_002");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(errorCode.getMessage()).isEqualTo("Service temporarily unavailable");
        }

        @Test
        @DisplayName("GATEWAY_TIMEOUT should have correct code, status, and message")
        void gatewayTimeoutShouldHaveCorrectValues() {
            ErrorCode errorCode = ErrorCode.GATEWAY_TIMEOUT;

            assertThat(errorCode.getCode()).isEqualTo("SYS_003");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
            assertThat(errorCode.getMessage()).isEqualTo("External service response timeout");
        }
    }

    @Nested
    @DisplayName("Getter Methods")
    class GetterMethods {

        @ParameterizedTest(name = "{0} should return code={1}")
        @MethodSource("errorCodeProvider")
        @DisplayName("getCode should return correct code")
        void getCodeShouldReturnCorrectCode(ErrorCode errorCode, String expectedCode, HttpStatus expectedStatus, String expectedMessage) {
            assertThat(errorCode.getCode()).isEqualTo(expectedCode);
        }

        @ParameterizedTest(name = "{0} should return status={2}")
        @MethodSource("errorCodeProvider")
        @DisplayName("getHttpStatus should return correct HTTP status")
        void getHttpStatusShouldReturnCorrectStatus(ErrorCode errorCode, String expectedCode, HttpStatus expectedStatus, String expectedMessage) {
            assertThat(errorCode.getHttpStatus()).isEqualTo(expectedStatus);
        }

        @ParameterizedTest(name = "{0} should return message={3}")
        @MethodSource("errorCodeProvider")
        @DisplayName("getMessage should return correct message")
        void getMessageShouldReturnCorrectMessage(ErrorCode errorCode, String expectedCode, HttpStatus expectedStatus, String expectedMessage) {
            assertThat(errorCode.getMessage()).isEqualTo(expectedMessage);
        }

        static Stream<Arguments> errorCodeProvider() {
            return Stream.of(
                Arguments.of(ErrorCode.ORDER_NOT_FOUND, "ORD_001", HttpStatus.NOT_FOUND, "Order not found"),
                Arguments.of(ErrorCode.ORDER_ALREADY_PROCESSED, "ORD_002", HttpStatus.CONFLICT, "Order has already been processed"),
                Arguments.of(ErrorCode.INVALID_ORDER_STATE, "ORD_003", HttpStatus.BAD_REQUEST, "Invalid order state transition"),
                Arguments.of(ErrorCode.PAYMENT_NOT_FOUND, "PAY_001", HttpStatus.NOT_FOUND, "Payment not found"),
                Arguments.of(ErrorCode.INVALID_PAYMENT_AMOUNT, "PAY_002", HttpStatus.BAD_REQUEST, "Invalid payment amount"),
                Arguments.of(ErrorCode.PAYMENT_ALREADY_PROCESSED, "PAY_003", HttpStatus.CONFLICT, "Payment has already been processed"),
                Arguments.of(ErrorCode.UNSUPPORTED_PAYMENT_METHOD, "PAY_004", HttpStatus.UNPROCESSABLE_ENTITY, "Unsupported payment method"),
                Arguments.of(ErrorCode.PG_CONNECTION_ERROR, "PAY_005", HttpStatus.BAD_GATEWAY, "Payment gateway connection error"),
                Arguments.of(ErrorCode.INVALID_PAYMENT_STATE, "PAY_006", HttpStatus.BAD_REQUEST, "Invalid payment state transition"),
                Arguments.of(ErrorCode.VALIDATION_FAILED, "VAL_001", HttpStatus.BAD_REQUEST, "Request validation failed"),
                Arguments.of(ErrorCode.IDEMPOTENCY_KEY_MISSING, "VAL_002", HttpStatus.BAD_REQUEST, "X-Idempotency-Key header is required"),
                Arguments.of(ErrorCode.IDEMPOTENCY_KEY_INVALID, "VAL_003", HttpStatus.BAD_REQUEST, "Invalid idempotency key format (UUID required)"),
                Arguments.of(ErrorCode.IDEMPOTENCY_CONFLICT, "VAL_004", HttpStatus.UNPROCESSABLE_ENTITY, "Different payload used with the same idempotency key"),
                Arguments.of(ErrorCode.IDEMPOTENCY_PROCESSING, "VAL_005", HttpStatus.CONFLICT, "Request with the same idempotency key is being processed. Please retry later"),
                Arguments.of(ErrorCode.TENANT_HEADER_MISSING, "TNT_001", HttpStatus.BAD_REQUEST, "X-Tenant-Id header is required"),
                Arguments.of(ErrorCode.TENANT_NOT_FOUND, "TNT_002", HttpStatus.NOT_FOUND, "Unknown tenant"),
                Arguments.of(ErrorCode.INTERNAL_SERVER_ERROR, "SYS_001", HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
                Arguments.of(ErrorCode.SERVICE_UNAVAILABLE, "SYS_002", HttpStatus.SERVICE_UNAVAILABLE, "Service temporarily unavailable"),
                Arguments.of(ErrorCode.GATEWAY_TIMEOUT, "SYS_003", HttpStatus.GATEWAY_TIMEOUT, "External service response timeout")
            );
        }
    }

    @Nested
    @DisplayName("All Enum Values")
    class AllEnumValues {

        @Test
        @DisplayName("should have error codes defined")
        void shouldHaveErrorCodesDefined() {
            assertThat(ErrorCode.values()).hasSizeGreaterThanOrEqualTo(19);
        }

        @Test
        @DisplayName("all error codes should have non-null code")
        void allErrorCodesShouldHaveNonNullCode() {
            for (ErrorCode errorCode : ErrorCode.values()) {
                assertThat(errorCode.getCode())
                    .as("ErrorCode %s should have non-null code", errorCode.name())
                    .isNotNull()
                    .isNotBlank();
            }
        }

        @Test
        @DisplayName("all error codes should have non-null HTTP status")
        void allErrorCodesShouldHaveNonNullHttpStatus() {
            for (ErrorCode errorCode : ErrorCode.values()) {
                assertThat(errorCode.getHttpStatus())
                    .as("ErrorCode %s should have non-null HTTP status", errorCode.name())
                    .isNotNull();
            }
        }

        @Test
        @DisplayName("all error codes should have non-null message")
        void allErrorCodesShouldHaveNonNullMessage() {
            for (ErrorCode errorCode : ErrorCode.values()) {
                assertThat(errorCode.getMessage())
                    .as("ErrorCode %s should have non-null message", errorCode.name())
                    .isNotNull()
                    .isNotBlank();
            }
        }

        @Test
        @DisplayName("all error codes should have unique code values")
        void allErrorCodesShouldHaveUniqueCodes() {
            long uniqueCodeCount = Stream.of(ErrorCode.values())
                .map(ErrorCode::getCode)
                .distinct()
                .count();

            assertThat(uniqueCodeCount).isEqualTo(ErrorCode.values().length);
        }
    }
}
