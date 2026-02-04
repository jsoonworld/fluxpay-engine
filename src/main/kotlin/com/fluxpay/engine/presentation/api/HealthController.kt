package com.fluxpay.engine.presentation.api

import com.fluxpay.engine.presentation.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Health", description = "서버 상태 확인 API")
@RestController
@RequestMapping("/api/v1")
class HealthController {

    @Operation(summary = "헬스 체크", description = "서버 상태를 확인합니다.")
    @GetMapping("/health")
    suspend fun health(): ApiResponse<Map<String, String>> {
        return ApiResponse.success(
            result = mapOf(
                "status" to "UP",
                "service" to "FluxPay Engine"
            )
        )
    }
}
