package com.fluxpay.engine.infrastructure.messaging.outbox;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Publishes outbox events to Kafka topics.
 * Topic is derived from aggregate type: fluxpay.{aggregateType}.events
 * Partition key is {tenantId}:{aggregateId} for ordering guarantees.
 */
@Component
public class KafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private static final String TOPIC_PREFIX = "fluxpay.";
    private static final String TOPIC_SUFFIX = ".events";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sends an outbox event to the appropriate Kafka topic.
     *
     * @param event the outbox event to send
     * @return Mono completing when send is acknowledged
     */
    public Mono<Void> send(OutboxEvent event) {
        String topic = deriveTopic(event.getAggregateType());
        String key = event.getTenantId() + ":" + event.getAggregateId();

        log.debug("Publishing to Kafka: topic={}, key={}, eventId={}",
            topic, key, event.getEventId());

        return Mono.fromFuture(() ->
            kafkaTemplate.send(topic, key, event.getPayload()).toCompletableFuture()
        )
        .doOnSuccess(result -> log.debug("Published to Kafka: eventId={}", event.getEventId()))
        .doOnError(error -> log.error("Failed to publish to Kafka: eventId={}",
            event.getEventId(), error))
        .then();
    }

    private String deriveTopic(String aggregateType) {
        Objects.requireNonNull(aggregateType, "aggregateType is required for topic derivation");
        return TOPIC_PREFIX + aggregateType.toLowerCase() + TOPIC_SUFFIX;
    }
}
