package com.fluxpay.engine.presentation.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiResponse Unit Tests")
class ApiResponseTest {

    @Test
    @DisplayName("should create success response with data")
    void shouldCreateSuccessResponse() {
        // Given
        String data = "test data";

        // When
        ApiResponse<String> response = ApiResponse.success(data);

        // Then
        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo(data);
        assertThat(response.error()).isNull();
        assertThat(response.metadata()).isNotNull();
        assertThat(response.metadata().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("should create error response with code and message")
    void shouldCreateErrorResponse() {
        // Given
        String code = "ERR_001";
        String message = "Error occurred";

        // When
        ApiResponse<Void> response = ApiResponse.error(code, message);

        // Then
        assertThat(response.success()).isFalse();
        assertThat(response.data()).isNull();
        assertThat(response.error()).isNotNull();
        assertThat(response.error().code()).isEqualTo(code);
        assertThat(response.error().message()).isEqualTo(message);
        assertThat(response.error().fieldErrors()).isEmpty();
    }

    @Test
    @DisplayName("should create validation error response with field errors")
    void shouldCreateValidationErrorResponse() {
        // Given
        String code = "VAL_001";
        String message = "Validation failed";
        List<FieldError> fieldErrors = List.of(
            new FieldError("email", "must not be blank"),
            new FieldError("amount", "must be positive")
        );

        // When
        ApiResponse<Void> response = ApiResponse.validationError(code, message, fieldErrors);

        // Then
        assertThat(response.success()).isFalse();
        assertThat(response.data()).isNull();
        assertThat(response.error()).isNotNull();
        assertThat(response.error().code()).isEqualTo(code);
        assertThat(response.error().message()).isEqualTo(message);
        assertThat(response.error().fieldErrors()).hasSize(2);
        assertThat(response.error().fieldErrors().get(0).field()).isEqualTo("email");
    }

    @Test
    @DisplayName("should include metadata with timestamp")
    void shouldIncludeMetadataWithTimestamp() {
        // When
        ApiResponse<String> response = ApiResponse.success("data");

        // Then
        assertThat(response.metadata()).isNotNull();
        assertThat(response.metadata().timestamp()).isNotNull();
    }
}
