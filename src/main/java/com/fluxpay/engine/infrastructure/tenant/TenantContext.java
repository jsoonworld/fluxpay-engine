package com.fluxpay.engine.infrastructure.tenant;

import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * Utility class for tenant ID propagation using Reactor Context.
 * Provides methods to get, set, and propagate tenant information
 * through reactive streams.
 */
public class TenantContext {

    public static final String TENANT_KEY = "tenantId";

    private TenantContext() {
        // Utility class - prevent instantiation
    }

    /**
     * Retrieves the tenant ID from the current Reactor Context.
     *
     * @return Mono containing the tenant ID
     * @throws TenantNotFoundException if tenant ID is not present in context
     */
    public static Mono<String> getTenantId() {
        return Mono.deferContextual(ctx -> {
            if (ctx.hasKey(TENANT_KEY)) {
                return Mono.just(ctx.get(TENANT_KEY));
            }
            return Mono.error(new TenantNotFoundException("Tenant ID not found in context"));
        });
    }

    /**
     * Retrieves the tenant ID from the current Reactor Context,
     * returning an empty Mono if not present.
     *
     * @return Mono containing the tenant ID, or empty if not present
     */
    public static Mono<String> getTenantIdOrEmpty() {
        return Mono.deferContextual(ctx -> {
            if (ctx.hasKey(TENANT_KEY)) {
                return Mono.just(ctx.get(TENANT_KEY));
            }
            return Mono.empty();
        });
    }

    /**
     * Returns a function that adds the tenant ID to the Reactor Context.
     * Can be used with Mono.transform() or Flux.transform().
     *
     * @param tenantId the tenant ID to add to context
     * @param <T> the type of the Mono
     * @return a function that adds tenant context to a Mono
     */
    public static <T> Function<Mono<T>, Mono<T>> withTenant(String tenantId) {
        return mono -> mono.contextWrite(ctx -> ctx.put(TENANT_KEY, tenantId));
    }
}
