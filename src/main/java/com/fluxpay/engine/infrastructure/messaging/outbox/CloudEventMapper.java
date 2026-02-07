package com.fluxpay.engine.infrastructure.messaging.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fluxpay.engine.domain.event.DomainEvent;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Maps DomainEvents to CloudEvents 1.0 JSON format.
 */
@Component
public class CloudEventMapper {

    private static final String SPEC_VERSION = "1.0";
    private static final String SOURCE = "fluxpay-engine";
    private static final String DATA_CONTENT_TYPE = "application/json";
    private static final String TYPE_PREFIX = "com.fluxpay.";

    private final ObjectMapper objectMapper;

    public CloudEventMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Converts a DomainEvent to CloudEvents 1.0 JSON string.
     *
     * @param event    the domain event
     * @param tenantId the tenant ID
     * @return CloudEvents 1.0 JSON string
     */
    public String toJson(DomainEvent event, String tenantId) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId must not be null");
        }

        try {
            ObjectNode cloudEvent = objectMapper.createObjectNode();
            cloudEvent.put("specversion", SPEC_VERSION);
            cloudEvent.put("id", event.eventId());
            cloudEvent.put("source", SOURCE);
            cloudEvent.put("type", TYPE_PREFIX + event.eventType());
            cloudEvent.put("datacontenttype", DATA_CONTENT_TYPE);
            cloudEvent.put("time", DateTimeFormatter.ISO_INSTANT.format(event.occurredAt()));
            cloudEvent.put("tenantid", tenantId);
            cloudEvent.set("data", objectMapper.valueToTree(event));

            return objectMapper.writeValueAsString(cloudEvent);
        } catch (Exception e) {
            throw new CloudEventSerializationException(
                "Failed to serialize domain event to CloudEvents format", e);
        }
    }
}
