package com.fluxpay.engine.infrastructure.persistence.adapter;

import com.fluxpay.engine.infrastructure.tenant.TenantNotFoundException;
import com.fluxpay.engine.domain.model.order.Order;
import com.fluxpay.engine.domain.model.order.OrderId;
import com.fluxpay.engine.domain.port.outbound.OrderRepository;
import com.fluxpay.engine.infrastructure.persistence.entity.OrderEntity;
import com.fluxpay.engine.infrastructure.persistence.entity.OrderLineItemEntity;
import com.fluxpay.engine.infrastructure.persistence.mapper.OrderMapper;
import com.fluxpay.engine.infrastructure.persistence.repository.OrderLineItemR2dbcRepository;
import com.fluxpay.engine.infrastructure.persistence.repository.OrderR2dbcRepository;
import com.fluxpay.engine.infrastructure.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * R2DBC implementation of OrderRepository.
 * Adapts the domain repository interface to Spring Data R2DBC.
 */
@Repository
public class R2dbcOrderRepository implements OrderRepository {

    private static final Logger log = LoggerFactory.getLogger(R2dbcOrderRepository.class);

    private final OrderR2dbcRepository orderR2dbcRepository;
    private final OrderLineItemR2dbcRepository lineItemR2dbcRepository;
    private final OrderMapper mapper;

    public R2dbcOrderRepository(OrderR2dbcRepository orderR2dbcRepository,
                                OrderLineItemR2dbcRepository lineItemR2dbcRepository,
                                OrderMapper mapper) {
        this.orderR2dbcRepository = orderR2dbcRepository;
        this.lineItemR2dbcRepository = lineItemR2dbcRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Mono<Order> save(Order order) {
        log.debug("Saving order: id={}", order.getId());

        return TenantContext.getTenantIdOrEmpty()
                .switchIfEmpty(Mono.error(new TenantNotFoundException("Tenant ID is required for saving orders")))
                .flatMap(tenantId -> {
                    OrderEntity orderEntity = mapper.toEntity(order, tenantId);
                    List<OrderLineItemEntity> lineItemEntities = mapper.toLineItemEntities(order, tenantId);

                    return orderR2dbcRepository.existsById(orderEntity.getId())
                            .flatMap(exists -> {
                                if (exists) {
                                    return updateOrder(orderEntity, lineItemEntities, order);
                                } else {
                                    return insertOrder(orderEntity, lineItemEntities, order);
                                }
                            });
                })
                .doOnSuccess(savedOrder -> log.debug("Order saved: id={}", savedOrder.getId()))
                .doOnError(error -> log.error("Failed to save order: id={}", order.getId(), error));
    }

    private Mono<Order> insertOrder(OrderEntity orderEntity, List<OrderLineItemEntity> lineItemEntities, Order order) {
        return orderR2dbcRepository.save(orderEntity)
                .flatMap(savedEntity -> saveLineItems(lineItemEntities)
                        .then(Mono.just(savedEntity)))
                .flatMap(savedEntity -> loadOrderWithLineItems(savedEntity.getId()));
    }

    private Mono<Order> updateOrder(OrderEntity orderEntity, List<OrderLineItemEntity> lineItemEntities, Order order) {
        UUID orderId = orderEntity.getId();

        // Mark entity as existing (not new) to trigger UPDATE instead of INSERT
        orderEntity.markAsExisting();

        // Delete existing line items, then save new ones
        return lineItemR2dbcRepository.deleteByOrderId(orderId)
                .then(orderR2dbcRepository.save(orderEntity))
                .flatMap(savedEntity -> saveLineItems(lineItemEntities)
                        .then(Mono.just(savedEntity)))
                .flatMap(savedEntity -> loadOrderWithLineItems(savedEntity.getId()));
    }

    private Mono<Void> saveLineItems(List<OrderLineItemEntity> lineItemEntities) {
        if (lineItemEntities.isEmpty()) {
            return Mono.empty();
        }
        return lineItemR2dbcRepository.saveAll(lineItemEntities).then();
    }

    private Mono<Order> loadOrderWithLineItems(UUID orderId) {
        return orderR2dbcRepository.findById(orderId)
                .flatMap(orderEntity ->
                        lineItemR2dbcRepository.findByOrderId(orderId)
                                .collectList()
                                .map(lineItems -> mapper.toDomain(orderEntity, lineItems)));
    }

    @Override
    public Mono<Order> findById(OrderId id) {
        log.debug("Finding order by id: {}", id);

        UUID uuid = id.value();
        return loadOrderWithLineItems(uuid)
                .doOnNext(order -> log.debug("Order found: id={}", id))
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("Order not found: id={}", id);
                    return Mono.empty();
                }));
    }

    @Override
    public Flux<Order> findByUserId(String userId) {
        log.debug("Finding orders by userId: {}", userId);

        // Batch fetch to avoid N+1 query problem:
        // 1. Fetch all orders for the user
        // 2. Collect all order IDs
        // 3. Batch fetch all line items for those orders in one query
        // 4. Group line items by orderId and map to domain objects
        return orderR2dbcRepository.findByUserId(userId)
                .collectList()
                .flatMapMany(orderEntities -> {
                    if (orderEntities.isEmpty()) {
                        return Flux.empty();
                    }

                    List<UUID> orderIds = orderEntities.stream()
                            .map(OrderEntity::getId)
                            .toList();

                    return lineItemR2dbcRepository.findByOrderIdIn(orderIds)
                            .collectList()
                            .map(lineItems -> {
                                // Group line items by orderId
                                Map<UUID, List<OrderLineItemEntity>> lineItemsByOrderId =
                                        lineItems.stream()
                                                .collect(java.util.stream.Collectors.groupingBy(
                                                        OrderLineItemEntity::getOrderId));

                                // Map each order entity to domain with its line items
                                return orderEntities.stream()
                                        .map(orderEntity -> {
                                            List<OrderLineItemEntity> orderLineItems =
                                                    lineItemsByOrderId.getOrDefault(
                                                            orderEntity.getId(), List.of());
                                            return mapper.toDomain(orderEntity, orderLineItems);
                                        })
                                        .toList();
                            })
                            .flatMapMany(Flux::fromIterable);
                })
                .doOnComplete(() -> log.debug("Completed finding orders for userId: {}", userId));
    }

    @Override
    public Mono<Boolean> existsById(OrderId id) {
        log.debug("Checking if order exists: id={}", id);

        return orderR2dbcRepository.existsById(id.value())
                .doOnSuccess(exists -> log.debug("Order exists check: id={}, exists={}", id, exists));
    }

    @Override
    @Transactional
    public Mono<Void> deleteById(OrderId id) {
        log.debug("Deleting order: id={}", id);

        UUID uuid = id.value();
        // Line items will be deleted by cascade (ON DELETE CASCADE in schema)
        return orderR2dbcRepository.deleteById(uuid)
                .doOnSuccess(v -> log.debug("Order deleted: id={}", id))
                .doOnError(error -> log.error("Failed to delete order: id={}", id, error));
    }
}
