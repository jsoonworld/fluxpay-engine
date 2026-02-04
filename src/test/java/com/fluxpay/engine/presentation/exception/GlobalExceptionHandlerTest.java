package com.fluxpay.engine.presentation.exception;

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
            assertThat(body.error().message()).isEqualTo("Validation failed");
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
            assertThat(body.error().message()).isEqualTo("Internal server error");
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

        @Test
        @DisplayName("should handle IllegalArgumentException")
        void shouldHandleIllegalArgumentException() {
            // Given
            Exception ex = new IllegalArgumentException("Invalid argument");

            // When
            ResponseEntity<ApiResponse<Void>> response = handler.handleGenericException(ex).block();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().error().code()).isEqualTo("SYS_001");
        }
    }
}
