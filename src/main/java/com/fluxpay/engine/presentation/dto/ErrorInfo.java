package com.fluxpay.engine.presentation.dto;

import java.util.List;

/**
 * Contains error information for API error responses.
 *
 * @param code        the error code (e.g., "PAY_001", "VAL_001")
 * @param message     the human-readable error message
 * @param fieldErrors list of field-level validation errors (empty for non-validation errors)
 */
public record ErrorInfo(String code, String message, List<FieldError> fieldErrors) {

    /**
     * Creates an ErrorInfo without field errors (for non-validation errors).
     *
     * @param code    the error code
     * @param message the error message
     */
    public ErrorInfo(String code, String message) {
        this(code, message, List.of());
    }
}
