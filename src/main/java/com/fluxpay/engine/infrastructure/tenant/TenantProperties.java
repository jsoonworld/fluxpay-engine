package com.fluxpay.engine.infrastructure.tenant;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "fluxpay.tenants")
@Validated
public record TenantProperties(
    Map<String, TenantConfig> configs
) {
    public TenantProperties {
        if (configs == null) {
            configs = new HashMap<>();
        }
    }

    public TenantConfig getConfig(String tenantId) {
        return configs.getOrDefault(tenantId, configs.get("default"));
    }

    public record TenantConfig(
        @Positive int rateLimit,
        boolean creditEnabled,
        boolean subscriptionEnabled,
        String webhookUrl
    ) {
        public TenantConfig {
            if (rateLimit <= 0) {
                rateLimit = 100; // default
            }
        }
    }
}
