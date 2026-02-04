package com.fluxpay.engine.presentation.dto;

/**
 * Represents a field-level validation error.
 *
 * @param field   the name of the field that failed validation
 * @param message the validation error message
 */
public record FieldError(String field, String message) {
}
