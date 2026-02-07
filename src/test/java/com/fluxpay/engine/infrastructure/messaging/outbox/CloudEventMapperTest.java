package com.fluxpay.engine.infrastructure.messaging.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fluxpay.engine.domain.event.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CloudEventMapper")
class CloudEventMapperTest {

    private CloudEventMapper mapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mapper = new CloudEventMapper(objectMapper);
    }

    @Nested
    @DisplayName("toJson")
    class ToJson {

        @Test
        @DisplayName("should convert domain event to CloudEvents 1.0 JSON format")
        void shouldConvertToCloudEventsFormat() throws Exception {
            TestOrderEvent event = new TestOrderEvent(
                "evt-123", "ord-456", Instant.parse("2026-02-04T10:30:00Z"));

            String json = mapper.toJson(event, "tenant-a");

            JsonNode node = objectMapper.readTree(json);
            assertThat(node.get("specversion").asText()).isEqualTo("1.0");
            assertThat(node.get("id").asText()).isEqualTo("evt-123");
            assertThat(node.get("source").asText()).isEqualTo("fluxpay-engine");
            assertThat(node.get("type").asText()).isEqualTo("com.fluxpay.order.created");
            assertThat(node.get("datacontenttype").asText()).isEqualTo("application/json");
            assertThat(node.get("time").asText()).isEqualTo("2026-02-04T10:30:00Z");
            assertThat(node.get("tenantid").asText()).isEqualTo("tenant-a");
            assertThat(node.has("data")).isTrue();
        }

        @Test
        @DisplayName("should include event data in data field")
        void shouldIncludeEventData() throws Exception {
            TestOrderEvent event = new TestOrderEvent(
                "evt-123", "ord-456", Instant.parse("2026-02-04T10:30:00Z"));

            String json = mapper.toJson(event, "tenant-a");

            JsonNode data = objectMapper.readTree(json).get("data");
            assertThat(data).isNotNull();
            assertThat(data.get("orderId").asText()).isEqualTo("ord-456");
        }

        @Test
        @DisplayName("should derive type from event type with prefix")
        void shouldDeriveTypeCorrectly() throws Exception {
            TestPaymentEvent event = new TestPaymentEvent(
                "evt-789", "pay-123", Instant.now());

            String json = mapper.toJson(event, "tenant-b");

            JsonNode node = objectMapper.readTree(json);
            assertThat(node.get("type").asText()).isEqualTo("com.fluxpay.payment.approved");
        }

        @Test
        @DisplayName("should throw exception when event is null")
        void shouldThrowWhenEventIsNull() {
            assertThatThrownBy(() -> mapper.toJson(null, "tenant-a"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("event");
        }

        @Test
        @DisplayName("should throw exception when tenantId is null")
        void shouldThrowWhenTenantIdIsNull() {
            TestOrderEvent event = new TestOrderEvent(
                "evt-123", "ord-456", Instant.now());

            assertThatThrownBy(() -> mapper.toJson(event, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");
        }
    }

    // Test helper records
    record TestOrderEvent(String eventId, String orderId, Instant occurredAt) implements DomainEvent {
        @Override public String aggregateType() { return "ORDER"; }
        @Override public String aggregateId() { return orderId; }
        @Override public String eventType() { return "order.created"; }
    }

    record TestPaymentEvent(String eventId, String paymentId, Instant occurredAt) implements DomainEvent {
        @Override public String aggregateType() { return "PAYMENT"; }
        @Override public String aggregateId() { return paymentId; }
        @Override public String eventType() { return "payment.approved"; }
    }
}
