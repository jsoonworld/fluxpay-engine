package com.fluxpay.engine.infrastructure.tenant;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * A tenant-aware ConnectionFactory that automatically sets the tenant_id
 * session variable on each connection based on the Reactor Context.
 *
 * <p>This enables row-level security (RLS) in PostgreSQL by setting
 * {@code app.tenant_id} for each database connection. The tenant ID is
 * retrieved from the Reactor Context using {@link TenantContext}.
 *
 * <p><strong>Security:</strong> When no tenant ID is present in the context,
 * the session variable is explicitly reset to empty string to prevent
 * tenant context leakage between pooled connection reuses.
 *
 * <p>This bean is marked as {@code @Primary} and wraps the delegate
 * ConnectionFactory qualified as "postgresConnectionFactory". In environments
 * where this qualifier is not available, consider using
 * {@link TenantAwareConnectionFactory#TenantAwareConnectionFactory(ConnectionFactory)}
 * directly.
 */
@Component
@Primary
@ConditionalOnProperty(name = "fluxpay.tenant.enabled", havingValue = "true", matchIfMissing = false)
public class TenantAwareConnectionFactory implements ConnectionFactory {

    private static final Logger log = LoggerFactory.getLogger(TenantAwareConnectionFactory.class);
    private static final String SET_TENANT_ID_SQL = "SET app.tenant_id = $1";
    private static final String RESET_TENANT_ID_SQL = "RESET app.tenant_id";

    private final ConnectionFactory delegate;

    /**
     * Creates a new TenantAwareConnectionFactory wrapping the given delegate.
     *
     * @param delegate the underlying ConnectionFactory to wrap
     */
    public TenantAwareConnectionFactory(ConnectionFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public Publisher<? extends Connection> create() {
        return Mono.from(delegate.create())
            .flatMap(connection ->
                TenantContext.getTenantIdOrEmpty()
                    .flatMap(tenantId -> setTenantId(connection, tenantId))
                    .switchIfEmpty(Mono.defer(() -> resetTenantId(connection)))
            );
    }

    @Override
    public ConnectionFactoryMetadata getMetadata() {
        return delegate.getMetadata();
    }

    private Mono<Connection> setTenantId(Connection connection, String tenantId) {
        return Mono.from(connection.createStatement(SET_TENANT_ID_SQL)
                .bind("$1", tenantId)
                .execute())
            .doOnSuccess(result -> log.debug("Set tenant_id to '{}' on connection", tenantId))
            .then(Mono.just(connection));
    }

    private Mono<Connection> resetTenantId(Connection connection) {
        return Mono.from(connection.createStatement(RESET_TENANT_ID_SQL)
                .execute())
            .doOnSuccess(result -> log.debug("Reset tenant_id on connection (no tenant in context)"))
            .then(Mono.just(connection));
    }
}
