package com.fluxpay.engine.infrastructure.messaging.outbox;

/**
 * Exception thrown when serialization to CloudEvents format fails.
 */
public class CloudEventSerializationException extends RuntimeException {

    public CloudEventSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
