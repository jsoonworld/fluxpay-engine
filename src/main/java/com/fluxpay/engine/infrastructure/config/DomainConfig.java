package com.fluxpay.engine.infrastructure.config;

import com.fluxpay.engine.domain.port.outbound.OrderRepository;
import com.fluxpay.engine.domain.port.outbound.PaymentRepository;
import com.fluxpay.engine.domain.port.outbound.PgClient;
import com.fluxpay.engine.domain.service.OrderService;
import com.fluxpay.engine.domain.service.PaymentService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for domain service beans.
 * Follows hexagonal architecture by keeping domain layer free of Spring annotations.
 */
@Configuration
public class DomainConfig {

    @Bean
    public OrderService orderService(OrderRepository orderRepository) {
        return new OrderService(orderRepository);
    }

    @Bean
    public PaymentService paymentService(PaymentRepository paymentRepository, PgClient pgClient) {
        return new PaymentService(paymentRepository, pgClient);
    }
}
