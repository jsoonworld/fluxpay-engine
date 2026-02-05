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
            assertThat(errorCode.getMessage()).isEqualTo("주문을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("ORDER_ALREADY_PROCESSED should have correct code, status, and message")
        void orderAlreadyProcessedShouldHaveCorrectValues() {
            ErrorCode errorCode = ErrorCode.ORDER_ALREADY_PROCESSED;

            assertThat(errorCode.getCode()).isEqualTo("ORD_002");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(errorCode.getMessage()).isEqualTo("이미 처리된 주문입니다");
        }

        @Test
        @DisplayName("INVALID_ORDER_STATE should have correct code, status, and message")
        void invalidOrderStateShouldHaveCorrectValues() {
            ErrorCode errorCode = ErrorCode.INVALID_ORDER_STATE;

            assertThat(errorCode.getCode()).isEqualTo("ORD_003");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(errorCode.getMessage()).isEqualTo("잘못된 주문 상태 전이입니다");
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
            assertThat(errorCode.getMessage()).isEqualTo("결제를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("INVALID_PAYMENT_AMOUNT should have correct code, status, and message")
        void invalidPaymentAmountShouldHaveCorrectValues() {
            ErrorCode errorCode = ErrorCode.INVALID_PAYMENT_AMOUNT;

            assertThat(errorCode.getCode()).isEqualTo("PAY_002");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(errorCode.getMessage()).isEqualTo("잘못된 결제 금액입니다");
        }

        @Test
        @DisplayName("PAYMENT_ALREADY_PROCESSED should have correct code, status, and message")
        void paymentAlreadyProcessedShouldHaveCorrectValues() {
            ErrorCode errorCode = ErrorCode.PAYMENT_ALREADY_PROCESSED;

            assertThat(errorCode.getCode()).isEqualTo("PAY_003");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(errorCode.getMessage()).isEqualTo("이미 처리된 결제입니다");
        }

        @Test
        @DisplayName("UNSUPPORTED_PAYMENT_METHOD should have correct code, status, and message")
        void unsupportedPaymentMethodShouldHaveCorrectValues() {
            ErrorCode errorCode = ErrorCode.UNSUPPORTED_PAYMENT_METHOD;

            assertThat(errorCode.getCode()).isEqualTo("PAY_004");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(errorCode.getMessage()).isEqualTo("지원하지 않는 결제 수단입니다");
        }

        @Test
        @DisplayName("PG_CONNECTION_ERROR should have correct code, status, and message")
        void pgConnectionErrorShouldHaveCorrectValues() {
            ErrorCode errorCode = ErrorCode.PG_CONNECTION_ERROR;

            assertThat(errorCode.getCode()).isEqualTo("PAY_005");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
            assertThat(errorCode.getMessage()).isEqualTo("PG 연동 오류가 발생했습니다");
        }

        @Test
        @DisplayName("INVALID_PAYMENT_STATE should have correct code, status, and message")
        void invalidPaymentStateShouldHaveCorrectValues() {
            ErrorCode errorCode = ErrorCode.INVALID_PAYMENT_STATE;

            assertThat(errorCode.getCode()).isEqualTo("PAY_006");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(errorCode.getMessage()).isEqualTo("잘못된 결제 상태 전이입니다");
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
            assertThat(errorCode.getMessage()).isEqualTo("요청 검증에 실패했습니다");
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
            assertThat(errorCode.getMessage()).isEqualTo("내부 서버 오류가 발생했습니다");
        }

        @Test
        @DisplayName("SERVICE_UNAVAILABLE should have correct code, status, and message")
        void serviceUnavailableShouldHaveCorrectValues() {
            ErrorCode errorCode = ErrorCode.SERVICE_UNAVAILABLE;

            assertThat(errorCode.getCode()).isEqualTo("SYS_002");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(errorCode.getMessage()).isEqualTo("서비스를 일시적으로 사용할 수 없습니다");
        }

        @Test
        @DisplayName("GATEWAY_TIMEOUT should have correct code, status, and message")
        void gatewayTimeoutShouldHaveCorrectValues() {
            ErrorCode errorCode = ErrorCode.GATEWAY_TIMEOUT;

            assertThat(errorCode.getCode()).isEqualTo("SYS_003");
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
            assertThat(errorCode.getMessage()).isEqualTo("외부 서비스 응답 시간 초과");
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
                Arguments.of(ErrorCode.ORDER_NOT_FOUND, "ORD_001", HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다"),
                Arguments.of(ErrorCode.ORDER_ALREADY_PROCESSED, "ORD_002", HttpStatus.CONFLICT, "이미 처리된 주문입니다"),
                Arguments.of(ErrorCode.INVALID_ORDER_STATE, "ORD_003", HttpStatus.BAD_REQUEST, "잘못된 주문 상태 전이입니다"),
                Arguments.of(ErrorCode.PAYMENT_NOT_FOUND, "PAY_001", HttpStatus.NOT_FOUND, "결제를 찾을 수 없습니다"),
                Arguments.of(ErrorCode.INVALID_PAYMENT_AMOUNT, "PAY_002", HttpStatus.BAD_REQUEST, "잘못된 결제 금액입니다"),
                Arguments.of(ErrorCode.PAYMENT_ALREADY_PROCESSED, "PAY_003", HttpStatus.CONFLICT, "이미 처리된 결제입니다"),
                Arguments.of(ErrorCode.UNSUPPORTED_PAYMENT_METHOD, "PAY_004", HttpStatus.UNPROCESSABLE_ENTITY, "지원하지 않는 결제 수단입니다"),
                Arguments.of(ErrorCode.PG_CONNECTION_ERROR, "PAY_005", HttpStatus.BAD_GATEWAY, "PG 연동 오류가 발생했습니다"),
                Arguments.of(ErrorCode.INVALID_PAYMENT_STATE, "PAY_006", HttpStatus.BAD_REQUEST, "잘못된 결제 상태 전이입니다"),
                Arguments.of(ErrorCode.VALIDATION_FAILED, "VAL_001", HttpStatus.BAD_REQUEST, "요청 검증에 실패했습니다"),
                Arguments.of(ErrorCode.INTERNAL_SERVER_ERROR, "SYS_001", HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류가 발생했습니다"),
                Arguments.of(ErrorCode.SERVICE_UNAVAILABLE, "SYS_002", HttpStatus.SERVICE_UNAVAILABLE, "서비스를 일시적으로 사용할 수 없습니다"),
                Arguments.of(ErrorCode.GATEWAY_TIMEOUT, "SYS_003", HttpStatus.GATEWAY_TIMEOUT, "외부 서비스 응답 시간 초과")
            );
        }
    }

    @Nested
    @DisplayName("All Enum Values")
    class AllEnumValues {

        @Test
        @DisplayName("should have exactly 13 error codes defined")
        void shouldHaveExactly13ErrorCodes() {
            assertThat(ErrorCode.values()).hasSize(13);
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
