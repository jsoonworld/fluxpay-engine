package com.fluxpay.engine.presentation.api;

import com.fluxpay.engine.domain.model.common.Currency;
import com.fluxpay.engine.domain.model.common.Money;
import com.fluxpay.engine.domain.model.order.OrderId;
import com.fluxpay.engine.domain.model.order.OrderLineItem;
import com.fluxpay.engine.domain.service.OrderService;
import com.fluxpay.engine.presentation.dto.ApiResponse;
import com.fluxpay.engine.presentation.dto.request.CreateOrderRequest;
import com.fluxpay.engine.presentation.dto.response.OrderResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<OrderResponse>> createOrder(
        @Valid @RequestBody CreateOrderRequest request
    ) {
        log.info("Creating order for user: {}", request.userId());

        Currency currency = Currency.valueOf(request.currency());
        List<OrderLineItem> lineItems = request.lineItems().stream()
            .map(item -> OrderLineItem.create(
                item.productId(),
                item.productName(),
                item.quantity(),
                Money.of(item.unitPrice(), currency)
            ))
            .toList();

        return orderService.createOrder(
                request.userId(),
                lineItems,
                currency,
                request.metadata()
            )
            .map(order -> ApiResponse.success(OrderResponse.from(order)));
    }

    @GetMapping("/{orderId}")
    public Mono<ApiResponse<OrderResponse>> getOrder(
        @PathVariable String orderId
    ) {
        log.info("Getting order: {}", orderId);

        return orderService.getOrder(OrderId.of(orderId))
            .map(order -> ApiResponse.success(OrderResponse.from(order)));
    }

    @GetMapping
    public Mono<ApiResponse<List<OrderResponse>>> getOrdersByUserId(
        @RequestParam String userId
    ) {
        log.info("Getting orders for user: {}", userId);

        return orderService.getOrdersByUserId(userId)
            .map(OrderResponse::from)
            .collectList()
            .map(ApiResponse::success);
    }
}
