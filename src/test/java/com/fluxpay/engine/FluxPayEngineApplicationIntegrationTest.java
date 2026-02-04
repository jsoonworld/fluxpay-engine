package com.fluxpay.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Application Context Tests")
class FluxPayEngineApplicationIntegrationTest {

    @Test
    @DisplayName("should load Spring context successfully")
    void contextLoads() {
        // Spring context loads successfully if this test passes
    }
}
