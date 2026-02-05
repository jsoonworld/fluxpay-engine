package com.fluxpay.engine.infrastructure.external.pg;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TossPaymentsExceptionTest {

    @Test
    @DisplayName("Should create exception with error code and message")
    void shouldCreateWithErrorCodeAndMessage() {
        TossPaymentsException ex = new TossPaymentsException("INVALID_AMOUNT", "Invalid amount");

        assertThat(ex.getErrorCode()).isEqualTo("INVALID_AMOUNT");
        assertThat(ex.getMessage()).isEqualTo("Invalid amount");
    }

    @Test
    @DisplayName("Should create exception with message and cause, defaulting errorCode to UNKNOWN")
    void shouldCreateWithMessageAndCause() {
        Throwable cause = new RuntimeException("Original error");
        TossPaymentsException ex = new TossPaymentsException("Connection failed", cause);

        assertThat(ex.getErrorCode()).isEqualTo("UNKNOWN");
        assertThat(ex.getMessage()).isEqualTo("Connection failed");
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}
