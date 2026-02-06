package com.fluxpay.engine.infrastructure.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * WebFilter that extracts the X-Tenant-Id header from incoming requests
 * and propagates it through the Reactor Context.
 *
 * <p>This filter runs at the highest precedence to ensure tenant context
 * is available to all downstream components in the reactive chain.</p>
 *
 * <p>This filter is only active when {@code fluxpay.tenant.enabled=true}.
 * Certain paths like health checks and actuator endpoints are excluded
 * from tenant validation.</p>
 *
 * <p>Usage: Clients must include the {@code X-Tenant-Id} header in all requests.
 * The tenant ID will be accessible via {@link TenantContext#getTenantId()}.</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "fluxpay.tenant.enabled", havingValue = "true", matchIfMissing = false)
public class TenantWebFilter implements WebFilter {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    private static final List<String> EXCLUDED_PATHS = List.of(
        "/actuator/**",
        "/health",
        "/health/**",
        "/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip tenant validation for excluded paths
        if (isExcludedPath(path)) {
            log.debug("Skipping tenant validation for excluded path: {}", path);
            return chain.filter(exchange);
        }

        String tenantId = request.getHeaders().getFirst(TENANT_HEADER);

        if (tenantId == null || tenantId.isBlank()) {
            log.warn("Request rejected: {} header is missing or blank. URI: {}",
                TENANT_HEADER, path);
            return Mono.error(new TenantNotFoundException(
                "X-Tenant-Id header is required but was not provided or is empty"));
        }

        log.debug("Processing request with tenant: {}", tenantId);
        return chain.filter(exchange)
            .contextWrite(ctx -> ctx.put(TenantContext.TENANT_KEY, tenantId));
    }

    private boolean isExcludedPath(String path) {
        return EXCLUDED_PATHS.stream()
            .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
}
