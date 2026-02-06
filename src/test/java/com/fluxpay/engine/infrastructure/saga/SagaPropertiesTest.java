package com.fluxpay.engine.infrastructure.saga;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = SagaPropertiesTest.TestConfig.class)
@TestPropertySource(properties = {
    "fluxpay.saga.enabled=true",
    "fluxpay.saga.timeout=30s",
    "fluxpay.saga.step-timeout=10s",
    "fluxpay.saga.compensation.max-retries=3",
    "fluxpay.saga.compensation.retry-delay=1s",
    "fluxpay.saga.cleanup.enabled=true",
    "fluxpay.saga.cleanup.retention-days=30",
    "fluxpay.saga.cleanup.cron=0 0 2 * * *"
})
@DisplayName("SagaProperties")
class SagaPropertiesTest {

    @Autowired
    private SagaProperties sagaProperties;

    @EnableConfigurationProperties(SagaProperties.class)
    static class TestConfig {}

    @Test
    @DisplayName("should load enabled flag")
    void shouldLoadEnabledFlag() {
        assertThat(sagaProperties.enabled()).isTrue();
    }

    @Test
    @DisplayName("should load timeout")
    void shouldLoadTimeout() {
        assertThat(sagaProperties.timeout()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("should load step timeout")
    void shouldLoadStepTimeout() {
        assertThat(sagaProperties.stepTimeout()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    @DisplayName("should load compensation settings")
    void shouldLoadCompensationSettings() {
        assertThat(sagaProperties.compensation()).isNotNull();
        assertThat(sagaProperties.compensation().maxRetries()).isEqualTo(3);
        assertThat(sagaProperties.compensation().retryDelay()).isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    @DisplayName("should load cleanup settings")
    void shouldLoadCleanupSettings() {
        assertThat(sagaProperties.cleanup()).isNotNull();
        assertThat(sagaProperties.cleanup().enabled()).isTrue();
        assertThat(sagaProperties.cleanup().retentionDays()).isEqualTo(30);
        assertThat(sagaProperties.cleanup().cron()).isEqualTo("0 0 2 * * *");
    }
}
