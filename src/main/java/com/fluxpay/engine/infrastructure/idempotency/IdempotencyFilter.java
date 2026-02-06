package com.fluxpay.engine.infrastructure.idempotency;

import com.fluxpay.engine.domain.model.idempotency.IdempotencyKey;
import com.fluxpay.engine.domain.model.idempotency.IdempotencyResult;
import com.fluxpay.engine.infrastructure.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

/**
 * WebFilter that handles idempotency for mutating HTTP requests.
 *
 * <p>This filter intercepts POST and PUT requests to specific API endpoints
 * and ensures idempotent behavior using the X-Idempotency-Key header.</p>
 *
 * <p>Flow with atomic lock acquisition:
 * <ol>
 *   <li>Validate idempotency key format (UUID)</li>
 *   <li>Compute SHA-256 hash of request body</li>
 *   <li>Atomically acquire lock (prevents race conditions)</li>
 *   <li>On MISS: Process request and store response</li>
 *   <li>On HIT: Return cached response</li>
 *   <li>On CONFLICT: Return 422 error</li>
 *   <li>On PROCESSING: Return 409 (request in progress)</li>
 * </ol></p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@EnableConfigurationProperties(IdempotencyProperties.class)
public class IdempotencyFilter implements WebFilter {

    private static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";
    private static final String DEFAULT_TENANT_ID = "__default__";

    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    private static final List<EndpointPattern> IDEMPOTENT_ENDPOINTS = List.of(
        new EndpointPattern(HttpMethod.POST, "/api/v1/payments"),
        new EndpointPattern(HttpMethod.POST, "/api/v1/orders"),
        new EndpointPattern(HttpMethod.PUT, "/api/v1/orders/*/cancel")
    );

    private static final List<String> EXCLUDED_PATHS = List.of(
        "/actuator/**",
        "/health",
        "/health/**",
        "/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html"
    );

    private final IdempotencyService idempotencyService;
    private final IdempotencyProperties properties;
    private final AntPathMatcher pathMatcher;

    public IdempotencyFilter(IdempotencyService idempotencyService, IdempotencyProperties properties) {
        this.idempotencyService = idempotencyService;
        this.properties = properties != null ? properties : IdempotencyProperties.defaults();
        this.pathMatcher = new AntPathMatcher();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Skip if idempotency is disabled
        if (!properties.enabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        if (isExcludedPath(path)) {
            log.debug("Skipping idempotency check for excluded path: {}", path);
            return chain.filter(exchange);
        }

        if (!requiresIdempotency(method, path)) {
            log.debug("Endpoint does not require idempotency: {} {}", method, path);
            return chain.filter(exchange);
        }

        String idempotencyKey = request.getHeaders().getFirst(IDEMPOTENCY_HEADER);

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            log.warn("Missing idempotency key for {} {}", method, path);
            return Mono.error(new IdempotencyKeyMissingException());
        }

        if (!isValidUuid(idempotencyKey)) {
            log.warn("Invalid idempotency key format: {}", idempotencyKey);
            return Mono.error(new IdempotencyKeyInvalidException(idempotencyKey));
        }

        return processWithIdempotency(exchange, chain, method, path, idempotencyKey);
    }

    private Mono<Void> processWithIdempotency(
        ServerWebExchange exchange,
        WebFilterChain chain,
        HttpMethod method,
        String path,
        String idempotencyKey
    ) {
        // Get tenant ID, use default if not available (tenant mode disabled)
        return getTenantIdOrDefault()
            .flatMap(tenantId -> readBodyAndProcess(exchange, chain, method, path, idempotencyKey, tenantId));
    }

    private Mono<String> getTenantIdOrDefault() {
        return TenantContext.getTenantIdOrEmpty()
            .defaultIfEmpty(DEFAULT_TENANT_ID);
    }

    private Mono<Void> readBodyAndProcess(
        ServerWebExchange exchange,
        WebFilterChain chain,
        HttpMethod method,
        String path,
        String idempotencyKey,
        String tenantId
    ) {
        return DataBufferUtils.join(exchange.getRequest().getBody())
            .defaultIfEmpty(new DefaultDataBufferFactory().allocateBuffer(0))
            .flatMap(dataBuffer -> {
                byte[] bodyBytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bodyBytes);
                DataBufferUtils.release(dataBuffer);

                String requestBody = new String(bodyBytes, StandardCharsets.UTF_8);
                String payloadHash = computeSha256(requestBody);
                String endpoint = method.name() + ":" + path;

                IdempotencyKey key = new IdempotencyKey(tenantId, endpoint, idempotencyKey);
                Duration ttl = properties.ttl();

                log.debug("Acquiring idempotency lock for key: {}, hash: {}", key.toCompositeKey(), payloadHash);

                // Use atomic lock acquisition instead of check
                return idempotencyService.acquireLock(key, payloadHash, ttl)
                    .flatMap(result -> handleIdempotencyResult(
                        result, exchange, chain, key, payloadHash, bodyBytes, idempotencyKey, ttl
                    ));
            });
    }

    private Mono<Void> handleIdempotencyResult(
        IdempotencyResult result,
        ServerWebExchange exchange,
        WebFilterChain chain,
        IdempotencyKey key,
        String payloadHash,
        byte[] bodyBytes,
        String idempotencyKey,
        Duration ttl
    ) {
        return switch (result.status()) {
            case HIT -> {
                log.info("Idempotency cache hit for key: {}", key.toCompositeKey());
                yield returnCachedResponse(exchange, result.cachedResponse(), result.cachedHttpStatus());
            }
            case CONFLICT -> {
                log.warn("Idempotency conflict for key: {}", key.toCompositeKey());
                yield Mono.error(new IdempotencyConflictException(idempotencyKey));
            }
            case PROCESSING -> {
                log.warn("Request already being processed for key: {}", key.toCompositeKey());
                yield Mono.error(new IdempotencyProcessingException(idempotencyKey));
            }
            case MISS -> {
                log.debug("Lock acquired for key: {}", key.toCompositeKey());
                yield processAndCacheResponse(exchange, chain, key, payloadHash, bodyBytes, ttl);
            }
        };
    }

    private Mono<Void> returnCachedResponse(
        ServerWebExchange exchange,
        String cachedResponse,
        int httpStatus
    ) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.valueOf(httpStatus));
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] responseBytes = cachedResponse.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(responseBytes);

        return response.writeWith(Mono.just(buffer));
    }

    private Mono<Void> processAndCacheResponse(
        ServerWebExchange exchange,
        WebFilterChain chain,
        IdempotencyKey key,
        String payloadHash,
        byte[] bodyBytes,
        Duration ttl
    ) {
        CachingServerWebExchange cachingExchange = new CachingServerWebExchange(exchange, bodyBytes);

        return chain.filter(cachingExchange)
            .then(Mono.defer(() -> {
                String responseBody = cachingExchange.getCachedResponseBody();
                int httpStatus = cachingExchange.getResponse().getStatusCode() != null
                    ? cachingExchange.getResponse().getStatusCode().value()
                    : 200;

                log.debug("Storing idempotency result for key: {}, status: {}", key.toCompositeKey(), httpStatus);

                return idempotencyService.store(key, payloadHash, responseBody, httpStatus, ttl);
            }))
            .onErrorResume(ex -> {
                // On processing failure, release the lock so the request can be retried
                log.warn("Request processing failed for key {}, releasing lock: {}", key.toCompositeKey(), ex.getMessage());
                return idempotencyService.releaseLock(key)
                    .then(Mono.error(ex));
            });
    }

    private boolean requiresIdempotency(HttpMethod method, String path) {
        return IDEMPOTENT_ENDPOINTS.stream()
            .anyMatch(endpoint -> endpoint.matches(method, path, pathMatcher));
    }

    private boolean isExcludedPath(String path) {
        return EXCLUDED_PATHS.stream()
            .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private boolean isValidUuid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return UUID_PATTERN.matcher(value).matches();
    }

    private String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private record EndpointPattern(HttpMethod method, String pathPattern) {
        boolean matches(HttpMethod requestMethod, String requestPath, AntPathMatcher matcher) {
            return this.method.equals(requestMethod) && matcher.match(this.pathPattern, requestPath);
        }
    }

    private static class CachingServerWebExchange extends ServerWebExchangeDecorator {
        private final CachingServerHttpRequest cachedRequest;
        private final CachingServerHttpResponse cachedResponse;

        CachingServerWebExchange(ServerWebExchange delegate, byte[] requestBody) {
            super(delegate);
            this.cachedRequest = new CachingServerHttpRequest(delegate.getRequest(), requestBody);
            this.cachedResponse = new CachingServerHttpResponse(delegate.getResponse());
        }

        @Override
        public ServerHttpRequest getRequest() {
            return cachedRequest;
        }

        @Override
        public ServerHttpResponse getResponse() {
            return cachedResponse;
        }

        String getCachedResponseBody() {
            return cachedResponse.getCachedBody();
        }
    }

    private static class CachingServerHttpRequest extends ServerHttpRequestDecorator {
        private final byte[] cachedBody;

        CachingServerHttpRequest(ServerHttpRequest delegate, byte[] cachedBody) {
            super(delegate);
            this.cachedBody = cachedBody;
        }

        @Override
        public Flux<DataBuffer> getBody() {
            if (cachedBody == null || cachedBody.length == 0) {
                return Flux.empty();
            }
            DataBuffer buffer = new DefaultDataBufferFactory().wrap(cachedBody);
            return Flux.just(buffer);
        }
    }

    private static class CachingServerHttpResponse extends ServerHttpResponseDecorator {
        private final StringBuilder cachedBody = new StringBuilder();

        CachingServerHttpResponse(ServerHttpResponse delegate) {
            super(delegate);
        }

        @Override
        public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
            return Flux.from(body)
                .map(dataBuffer -> {
                    byte[] content = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(content);
                    DataBufferUtils.release(dataBuffer);
                    cachedBody.append(new String(content, StandardCharsets.UTF_8));
                    return bufferFactory().wrap(content);
                })
                .as(super::writeWith);
        }

        String getCachedBody() {
            return cachedBody.toString();
        }
    }
}
