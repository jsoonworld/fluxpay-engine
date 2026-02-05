package com.fluxpay.engine.infrastructure.tenant;

/**
 * Exception thrown when tenant ID is not found in the Reactor Context.
 */
public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(String message) {
        super(message);
    }
}
