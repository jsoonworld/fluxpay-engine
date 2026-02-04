package com.fluxpay.engine.domain.exception;

import com.fluxpay.engine.domain.model.order.OrderStatus;

/**
 * Exception thrown when an invalid order state transition is attempted.
 */
public class InvalidOrderStateException extends RuntimeException {

    private final OrderStatus currentStatus;
    private final OrderStatus targetStatus;

    public InvalidOrderStateException(String message) {
        super(message);
        this.currentStatus = null;
        this.targetStatus = null;
    }

    public InvalidOrderStateException(OrderStatus currentStatus, OrderStatus targetStatus) {
        super(String.format("Cannot transition order from %s to %s", currentStatus, targetStatus));
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }

    public OrderStatus getTargetStatus() {
        return targetStatus;
    }
}
