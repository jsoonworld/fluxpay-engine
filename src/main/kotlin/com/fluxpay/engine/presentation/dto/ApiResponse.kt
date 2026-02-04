package com.fluxpay.engine.presentation.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: T? = null
) {
    companion object {
        fun <T> success(result: T? = null, message: String = "요청이 성공적으로 처리되었습니다."): ApiResponse<T> {
            return ApiResponse(
                isSuccess = true,
                code = "SUCCESS",
                message = message,
                result = result
            )
        }

        fun <T> error(code: String, message: String): ApiResponse<T> {
            return ApiResponse(
                isSuccess = false,
                code = code,
                message = message,
                result = null
            )
        }
    }
}
