package com.fluxpay.engine.presentation.exception;

import com.fluxpay.engine.domain.exception.InvalidOrderStateException;
import com.fluxpay.engine.domain.exception.InvalidPaymentStateException;
import com.fluxpay.engine.domain.exception.OrderNotFoundException;
import com.fluxpay.engine.domain.exception.PaymentNotFoundException;
import com.fluxpay.engine.domain.exception.PaymentProcessingException;
import com.fluxpay.engine.infrastructure.idempotency.IdempotencyConflictException;
import com.fluxpay.engine.infrastructure.idempotency.IdempotencyKeyInvalidException;
import com.fluxpay.engine.infrastructure.idempotency.IdempotencyKeyMissingException;
import com.fluxpay.engine.infrastructure.idempotency.IdempotencyProcessingException;
import com.fluxpay.engine.infrastructure.tenant.TenantNotFoundException;
import com.fluxpay.engine.domain.model.order.OrderId;
import com.fluxpay.engine.domain.model.order.OrderStatus;
import com.fluxpay.engine.domain.model.payment.PaymentId;
import com.fluxpay.engine.domain.model.payment.PaymentStatus;
import com.fluxpay.engine.presentation.dto.ApiResponse;
import com.fluxpay.engine.presentation.dto.FieldError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("handleValidationError")
    class HandleValidationError {

        @Test
        @DisplayName("should return BAD_REQUEST with field errors for validation exception")
        void shouldReturnBadRequestWithFieldErrors() {
            // Given
            BindingResult bindingResult = mock(BindingResult.class);
            org.springframework.validation.FieldError fieldError1 =
                new org.springframework.validation.FieldError(
                    "request", "amount", null, false, null, null, "Amount is required");
            org.springframework.validation.FieldError fieldError2 =
                new org.springframework.validation.FieldError(
                    "request", "currency", null, false, null, null, "Invalid currency code");
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

            MethodParameter methodParameter = mock(MethodParameter.class);
            WebExchangeBindException ex = new WebExchangeBindException(methodParameter, bindingResult);

            // When
            ResponseEntity<ApiResponse<Void>> response = handler.handleValidationError(ex).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

            ApiResponse<Void> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.success()).isFalse();
            assertThat(body.data()).isNull();
            assertThat(body.error()).isNotNull();
            assertThat(body.error().code()).isEqualTo("VAL_001");
            assertThat(body.error().message()).isEqualTo("요청 검증에 실패했습니다");
            assertThat(body.error().fieldErrors()).hasSize(2);
            assertThat(body.error().fieldErrors())
                .extracting(FieldError::field)
                .containsExactly("amount", "currency");
            assertThat(body.metadata()).isNotNull();
            assertThat(body.metadata().timestamp()).isNotNull();
        }

        @Test
        @DisplayName("should return empty field errors list when no validation errors")
        void shouldReturnEmptyFieldErrorsList() {
            // Given
            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(List.of());

            MethodParameter methodParameter = mock(MethodParameter.class);
            WebExchangeBindException ex = new WebExchangeBindException(methodParameter, bindingResult);

            // When
            ResponseEntity<ApiResponse<Void>> response = handler.handleValidationError(ex).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().error().fieldErrors()).isEmpty();
        }
    }

    @Nested
    @DisplayName("handleGenericException")
    class HandleGenericException {

        @Test
        @DisplayName("should return INTERNAL_SERVER_ERROR for unhandled exception")
        void shouldReturnInternalServerError() {
            // Given
            Exception ex = new RuntimeException("Unexpected error");

            // When
            ResponseEntity<ApiResponse<Void>> response = handler.handleGenericException(ex).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

            ApiResponse<Void> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.success()).isFalse();
            assertThat(body.data()).isNull();
            assertThat(body.error()).isNotNull();
            assertThat(body.error().code()).isEqualTo("SYS_001");
            assertThat(body.error().message()).isEqualTo("내부 서버 오류가 발생했습니다");
            assertThat(body.error().fieldErrors()).isEmpty();
            assertThat(body.metadata()).isNotNull();
        }

        @Test
        @DisplayName("should handle NullPointerException")
        void shouldHandleNullPointerException() {
            // Given
            Exception ex = new NullPointerException("Null reference");

            // When
            ResponseEntity<ApiResponse<Void>> response = handler.handleGenericException(ex).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().error().code()).isEqualTo("SYS_001");
        }
    }

    @Nested
    @DisplayName("handleOrderNotFoundException")
    class HandleOrderNotFoundException {

        @Test
        @DisplayName("should return NOT_FOUND with ORDER_NOT_FOUND error code")
        void shouldReturnNotFoundWithOrderNotFoundErrorCode() {
            // Given
            String orderId = UUID.randomUUID().toString();
            OrderNotFoundException ex = new OrderNotFoundException(orderId);

            // When
            ResponseEntity<ApiResponse<Void>> response = handler.handleOrderNotFoundException(ex).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

            ApiResponse<Void> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.success()).isFalse();
            assertThat(body.data()).isNull();
            assertThat(body.error()).isNotNull();
            assertThat(body.error().code()).isEqualTo("ORD_001");
            assertThat(body.error().message()).isEqualTo("주문을 찾을 수 없습니다");
            assertThat(body.error().fieldErrors()).isEmpty();
            assertThat(body.metadata()).isNotNull();
        }

        @Test
        @DisplayName("should return NOT_FOUND when constructed with OrderId")
        void shouldReturnNotFoundWhenConstructedWithOrderId() {
            // Given
            OrderId orderId = new OrderId(UUID.randomUUID());
            OrderNotFoundException ex = new OrderNotFoundException(orderId);

            // When
            ResponseEntity<ApiResponse<Void>> response = handler.handleOrderNotFoundException(ex).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().error().code()).isEqualTo("ORD_001");
        }
    }

    @Nested
    @DisplayName("handlePaymentNotFoundException")
    class HandlePaymentNotFoundException {

        @Test
        @DisplayName("should return NOT_FOUND with PAYMENT_NOT_FOUND error code")
        void shouldReturnNotFoundWithPaymentNotFoundErrorCode() {
            // Given
            PaymentId paymentId = new PaymentId(UUID.randomUUID());
            PaymentNotFoundException ex = new PaymentNotFoundException(paymentId);

            // When
            ResponseEntity<ApiResponse<Void>> response = handler.handlePaymentNotFoundException(ex).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

            ApiResponse<Void> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.success()).isFalse();
            assertThat(body.data()).isNull();
            assertThat(body.error()).isNotNull();
            assertThat(body.error().code()).isEqualTo("PAY_001");
            assertThat(body.error().message()).isEqualTo("결제를 찾을 수 없습니다");
            assertThat(body.error().fieldErrors()).isEmpty();
            assertThat(body.metadata()).isNotNull();
        }

        @Test
        @DisplayName("should return NOT_FOUND when constructed with message")
        void shouldReturnNotFoundWhenConstructedWithMessage() {
            // Given
            PaymentNotFoundException ex = new PaymentNotFoundException("Payment not found");

            // When
            ResponseEntity<ApiResponse<Void>> response = handler.handlePaymentNotFoundException(ex).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().error().code()).isEqualTo("PAY_001");
        }
    }

    @Nested
    @DisplayName("handleInvalidOrderStateException")
    class HandleInvalidOrderStateException {

        @Test
        @DisplayName("should return BAD_REQUEST with INVALID_ORDER_STATE error code")
        void shouldReturnBadRequestWithInvalidOrderStateErrorCode() {
            // Given
            InvalidOrderStateException ex = new InvalidOrderStateException(
                OrderStatus.PENDING, OrderStatus.COMPLETED);

            // When
            ResponseEntity<ApiResponse<Void>> response = handler.handleInvalidOrderStateException(ex).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

            ApiResponse<Void> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.success()).isFalse();
            assertThat(body.data()).isNull();
            assertThat(body.error()).isNotNull();
            assertThat(body.error().code()).isEqualTo("ORD_003");
            assertThat(body.error().message()).isEqualTo("잘못된 주문 상태 전이입니다");
            assertThat(body.error().fieldErrors()).isEmpty();
            assertThat(body.metadata()).isNotNull();
        }

        @Test
        @DisplayName("should return BAD_REQUEST when constructed with message")
        void shouldReturnBadRequestWhenConstructedWithMessage() {
            // Given
            InvalidOrderStateException ex = new InvalidOrderStateException("Invalid state");

            // When
            ResponseEntity<ApiResponse<Void>> response = handler.handleInvalidOrderStateException(ex).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().error().code()).isEqualTo("ORD_003");
        }
    }

    @Nested
    @DisplayName("handleInvalidPaymentStateException")
    class HandleInvalidPaymentStateException {

        @Test
        @DisplayName("should return BAD_REQUEST with INVALID_PAYMENT_STATE error code")
        void shouldReturnBadRequestWithInvalidPaymentStateErrorCode() {
            // Given
            InvalidPaymentStateException ex = new InvalidPaymentStateException(
                PaymentStatus.READY, PaymentStatus.CONFIRMED);

            // When
            ResponseEntity<ApiResponse<Void>> response = handler.handleInvalidPaymentStateException(ex).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

            ApiResponse<Void> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.success()).isFalse();
            assertThat(body.data()).isNull();
            assertThat(body.error()).isNotNull();
            assertThat(body.error().code()).isEqualTo("PAY_006");
            assertThat(body.error().message()).isEqualTo("잘못된 결제 상태 전이입니다");
            assertThat(body.error().fieldErrors()).isEmpty();
            assertThat(body.metadata()).isNotNull();
        }

        @Test
        @DisplayName("should return BAD_REQUEST when constructed with message")
        void shouldReturnBadRequestWhenConstructedWithMessage() {
            // Given
            InvalidPaymentStateException ex = new InvalidPaymentStateException("Invalid state");

            // When
            ResponseEntity<ApiResponse<Void>> response = handler.handleInvalidPaymentStateException(ex).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().error().code()).isEqualTo("PAY_006");
        }
    }

    @Nested
    @DisplayName("handlePaymentProcessingException")
    class HandlePaymentProcessingException {

        @Test
        @DisplayName("should return BAD_GATEWAY with PG_CONNECTION_ERROR error code")
        void shouldReturnBadGatewayWithPgConnectionErrorCode() {
            // Given
            PaymentProcessingException ex = new PaymentProcessingException("PG connection failed");

            // When
            ResponseEntity<ApiResponse<Void>> response = handler.handlePaymentProcessingException(ex).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);

            ApiResponse<Void> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.success()).isFalse();
            assertThat(body.data()).isNull();
            assertThat(body.error()).isNotNull();
            assertThat(body.error().code()).isEqualTo("PAY_005");
            assertThat(body.error().message()).isEqualTo("PG 연동 오류가 발생했습니다");
            assertThat(body.error().fieldErrors()).isEmpty();
            assertThat(body.metadata()).isNotNull();
        }

        @Test
        @DisplayName("should return BAD_GATEWAY when constructed with cause")
        void shouldReturnBadGatewayWhenConstructedWithCause() {
            // Given
            PaymentProcessingException ex = new PaymentProcessingException(
                "PG error", new RuntimeException("Connection timeout"));

            // When
            ResponseEntity<ApiResponse<Void>> response = handler.handlePaymentProcessingException(ex).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
            assertThat(response.getBody().error().code()).isEqualTo("PAY_005");
        }
    }

    @Nested
    @DisplayName("handleIllegalArgumentException")
    class HandleIllegalArgumentException {

        @Test
        @DisplayName("should return BAD_REQUEST with VALIDATION_FAILED error code")
        void shouldReturnBadRequestWithValidationFailedErrorCode() {
            // Given
            IllegalArgumentException ex = new IllegalArgumentException("Invalid argument provided");

            // When
            ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalArgumentException(ex).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

            ApiResponse<Void> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.success()).isFalse();
            assertThat(body.data()).isNull();
            assertThat(body.error()).isNotNull();
            assertThat(body.error().code()).isEqualTo("VAL_001");
            assertThat(body.error().message()).isEqualTo("요청 검증에 실패했습니다");
            assertThat(body.error().fieldErrors()).isEmpty();
            assertThat(body.metadata()).isNotNull();
        }
    }

    @Nested
    @DisplayName("handleTenantNotFoundException")
    class HandleTenantNotFoundException {

        @Test
        @DisplayName("should return BAD_REQUEST with TNT_001 error code")
        void shouldHandleTenantNotFoundException() {
            // Given
            TenantNotFoundException exception = new TenantNotFoundException("X-Tenant-Id header is required");

            // When
            ResponseEntity<ApiResponse<Void>> response = handler.handleTenantNotFoundException(exception).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

            ApiResponse<Void> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.success()).isFalse();
            assertThat(body.data()).isNull();
            assertThat(body.error()).isNotNull();
            assertThat(body.error().code()).isEqualTo("TNT_001");
            assertThat(body.error().message()).isEqualTo("X-Tenant-Id 헤더가 필요합니다");
            assertThat(body.error().fieldErrors()).isEmpty();
            assertThat(body.metadata()).isNotNull();
        }
    }

    @Nested
    @DisplayName("handleIdempotencyKeyMissingException")
    class HandleIdempotencyKeyMissingException {

        @Test
        @DisplayName("should return BAD_REQUEST with VAL_002 error code")
        void shouldHandleIdempotencyKeyMissing() {
            // Given
            IdempotencyKeyMissingException exception = new IdempotencyKeyMissingException();

            // When
            ResponseEntity<ApiResponse<Void>> response = handler.handleIdempotencyKeyMissing(exception).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

            ApiResponse<Void> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.success()).isFalse();
            assertThat(body.data()).isNull();
            assertThat(body.error()).isNotNull();
            assertThat(body.error().code()).isEqualTo("VAL_002");
            assertThat(body.error().message()).isEqualTo("X-Idempotency-Key 헤더가 필요합니다");
            assertThat(body.error().fieldErrors()).isEmpty();
            assertThat(body.metadata()).isNotNull();
        }
    }

    @Nested
    @DisplayName("handleIdempotencyKeyInvalidException")
    class HandleIdempotencyKeyInvalidException {

        @Test
        @DisplayName("should return BAD_REQUEST with VAL_003 error code")
        void shouldHandleIdempotencyKeyInvalid() {
            // Given
            IdempotencyKeyInvalidException exception = new IdempotencyKeyInvalidException("not-a-uuid");

            // When
            ResponseEntity<ApiResponse<Void>> response = handler.handleIdempotencyKeyInvalid(exception).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

            ApiResponse<Void> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.success()).isFalse();
            assertThat(body.data()).isNull();
            assertThat(body.error()).isNotNull();
            assertThat(body.error().code()).isEqualTo("VAL_003");
            assertThat(body.error().message()).isEqualTo("유효하지 않은 멱등 키 형식입니다 (UUID 필요)");
            assertThat(body.error().fieldErrors()).isEmpty();
            assertThat(body.metadata()).isNotNull();
        }
    }

    @Nested
    @DisplayName("handleIdempotencyConflictException")
    class HandleIdempotencyConflictException {

        @Test
        @DisplayName("should return UNPROCESSABLE_ENTITY with VAL_004 error code")
        void shouldHandleIdempotencyConflict() {
            // Given
            IdempotencyConflictException exception = new IdempotencyConflictException("550e8400-e29b-41d4-a716-446655440000");

            // When
            ResponseEntity<ApiResponse<Void>> response = handler.handleIdempotencyConflict(exception).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

            ApiResponse<Void> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.success()).isFalse();
            assertThat(body.data()).isNull();
            assertThat(body.error()).isNotNull();
            assertThat(body.error().code()).isEqualTo("VAL_004");
            assertThat(body.error().message()).isEqualTo("동일한 멱등 키에 다른 페이로드가 사용되었습니다");
            assertThat(body.error().fieldErrors()).isEmpty();
            assertThat(body.metadata()).isNotNull();
        }
    }

    @Nested
    @DisplayName("handleIdempotencyProcessingException")
    class HandleIdempotencyProcessingException {

        @Test
        @DisplayName("should return CONFLICT with VAL_005 error code")
        void shouldHandleIdempotencyProcessing() {
            // Given
            IdempotencyProcessingException exception = new IdempotencyProcessingException("550e8400-e29b-41d4-a716-446655440000");

            // When
            ResponseEntity<ApiResponse<Void>> response = handler.handleIdempotencyProcessing(exception).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

            ApiResponse<Void> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.success()).isFalse();
            assertThat(body.data()).isNull();
            assertThat(body.error()).isNotNull();
            assertThat(body.error().code()).isEqualTo("VAL_005");
            assertThat(body.error().message()).isEqualTo("동일한 멱등 키로 요청이 처리 중입니다. 잠시 후 다시 시도해주세요");
            assertThat(body.error().fieldErrors()).isEmpty();
            assertThat(body.metadata()).isNotNull();
        }
    }
}
