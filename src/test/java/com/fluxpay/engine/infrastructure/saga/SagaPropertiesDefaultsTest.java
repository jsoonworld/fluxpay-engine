package com.fluxpay.engine.infrastructure.saga;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = SagaPropertiesDefaultsTest.TestConfig.class)
@DisplayName("SagaProperties defaults")
class SagaPropertiesDefaultsTest {

    @Autowired
    private SagaProperties sagaProperties;

    @EnableConfigurationProperties(SagaProperties.class)
    static class TestConfig {}

    @Test
    @DisplayName("should default compensation to non-null with nested defaults when omitted from config")
    void shouldDefaultCompensationWhenOmitted() {
        assertThat(sagaProperties.compensation()).isNotNull();
        assertThat(sagaProperties.compensation().maxRetries()).isEqualTo(3);
        assertThat(sagaProperties.compensation().retryDelay()).isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    @DisplayName("should default cleanup to non-null with nested defaults when omitted from config")
    void shouldDefaultCleanupWhenOmitted() {
        assertThat(sagaProperties.cleanup()).isNotNull();
        assertThat(sagaProperties.cleanup().enabled()).isTrue();
        assertThat(sagaProperties.cleanup().retentionDays()).isEqualTo(30);
        assertThat(sagaProperties.cleanup().cron()).isEqualTo("0 0 2 * * *");
    }

    @Test
    @DisplayName("should default top-level properties when no saga config is provided")
    void shouldDefaultTopLevelProperties() {
        assertThat(sagaProperties.enabled()).isTrue();
        assertThat(sagaProperties.timeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(sagaProperties.stepTimeout()).isEqualTo(Duration.ofSeconds(10));
    }
}
