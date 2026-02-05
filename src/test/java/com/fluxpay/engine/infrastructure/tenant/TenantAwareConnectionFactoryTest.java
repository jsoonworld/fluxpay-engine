package com.fluxpay.engine.infrastructure.tenant;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TenantAwareConnectionFactory following TDD.
 * This class wraps a ConnectionFactory to automatically set tenant_id
 * on each connection from the Reactor Context.
 */
@ExtendWith(MockitoExtension.class)
class TenantAwareConnectionFactoryTest {

    private static final String TEST_TENANT_ID = "tenant-123";

    @Mock
    private ConnectionFactory delegate;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Mock
    private Result result;

    @Mock
    private ConnectionFactoryMetadata metadata;

    private TenantAwareConnectionFactory tenantAwareConnectionFactory;

    @BeforeEach
    void setUp() {
        tenantAwareConnectionFactory = new TenantAwareConnectionFactory(delegate);
    }

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("should set tenant ID when present in context")
        void shouldSetTenantIdWhenPresent() {
            // Given
            doReturn(Mono.just(connection)).when(delegate).create();
            when(connection.createStatement(anyString())).thenReturn(statement);
            when(statement.bind(anyString(), any())).thenReturn(statement);
            doReturn(Flux.just(result)).when(statement).execute();

            // When
            Mono<? extends Connection> connectionMono = Mono.from(tenantAwareConnectionFactory.create())
                .transform(TenantContext.withTenant(TEST_TENANT_ID));

            // Then
            StepVerifier.create(connectionMono)
                .assertNext(conn -> {
                    assertThat(conn).isEqualTo(connection);
                })
                .verifyComplete();

            verify(connection).createStatement("SET app.tenant_id = $1");
            verify(statement).bind("$1", TEST_TENANT_ID);
            verify(statement).execute();
        }

        @Test
        @DisplayName("should reset tenant ID when absent from context to prevent leakage")
        void shouldResetTenantIdWhenAbsent() {
            // Given
            doReturn(Mono.just(connection)).when(delegate).create();
            when(connection.createStatement("RESET app.tenant_id")).thenReturn(statement);
            doReturn(Flux.just(result)).when(statement).execute();

            // When
            Mono<? extends Connection> connectionMono = Mono.from(tenantAwareConnectionFactory.create());

            // Then
            StepVerifier.create(connectionMono)
                .assertNext(conn -> {
                    assertThat(conn).isEqualTo(connection);
                })
                .verifyComplete();

            // Verify RESET is called to prevent tenant context leakage
            verify(connection).createStatement("RESET app.tenant_id");
            verify(statement).execute();
        }
    }

    @Nested
    @DisplayName("getMetadata")
    class GetMetadataTests {

        @Test
        @DisplayName("should delegate metadata to underlying connection factory")
        void shouldDelegateMetadata() {
            // Given
            when(delegate.getMetadata()).thenReturn(metadata);

            // When
            ConnectionFactoryMetadata result = tenantAwareConnectionFactory.getMetadata();

            // Then
            assertThat(result).isEqualTo(metadata);
            verify(delegate).getMetadata();
        }
    }
}
