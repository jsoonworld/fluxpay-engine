package com.fluxpay.engine.presentation.dto;

import java.util.List;

public record ApiResponse<T>(
    boolean success,
    T data,
    ErrorInfo error,
    ResponseMetadata metadata
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, ResponseMetadata.now());
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorInfo(code, message), ResponseMetadata.now());
    }

    public static <T> ApiResponse<T> validationError(String code, String message, List<FieldError> fieldErrors) {
        return new ApiResponse<>(false, null,
            new ErrorInfo(code, message, fieldErrors != null ? fieldErrors : List.of()),
            ResponseMetadata.now());
    }
}
