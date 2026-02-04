package com.fluxpay.engine.presentation.api;

import com.fluxpay.engine.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@Tag(name = "Health", description = "서버 상태 확인 API")
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @Operation(summary = "헬스 체크", description = "서버 상태를 확인합니다.")
    @GetMapping("/health")
    public Mono<ApiResponse<Map<String, String>>> health() {
        return Mono.just(ApiResponse.success(
                Map.of(
                        "status", "UP",
                        "service", "FluxPay Engine"
                )
        ));
    }
}
