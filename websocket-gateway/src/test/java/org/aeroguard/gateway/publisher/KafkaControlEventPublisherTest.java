package org.aeroguard.gateway.publisher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aeroguard.gateway.exception.EventPublishingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class KafkaControlEventPublisherTest {

    private KafkaTemplate<String, String> kafkaTemplate;
    private KafkaControlEventPublisher publisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        publisher = new KafkaControlEventPublisher(kafkaTemplate);
    }

    @Test
    void publishOperatingMode_shouldSerializeAndSendToEventsStatusTopic() throws Exception {
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

        publisher.publishOperatingMode("turbine-10", "MAINTENANCE_MODE");

        verify(kafkaTemplate).send(eq("events.status"), eq("turbine-10"), payloadCaptor.capture());
        JsonNode jsonNode = objectMapper.readTree(payloadCaptor.getValue());

        assertEquals("turbine-10", jsonNode.get("assetId").asText());
        assertEquals("MAINTENANCE_MODE", jsonNode.get("operatingMode").asText());
        assertEquals("OPERATING_MODE_CHANGE", jsonNode.get("eventType").asText());
    }

    @Test
    void publishThermalSpike_shouldSerializeAndSendToTelemetryTopic() throws Exception {
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

        publisher.publishThermalSpike("turbine-10", 95.0);

        verify(kafkaTemplate).send(eq("telemetry.raw"), eq("turbine-10"), payloadCaptor.capture());
        JsonNode jsonNode = objectMapper.readTree(payloadCaptor.getValue());

        assertEquals("turbine-10", jsonNode.get("assetId").asText());
        assertEquals(95.0, jsonNode.get("temperature").asDouble());
    }

    @Test
    void publishDiagnosticAction_shouldSerializeAndSendToEventsStatusTopic() throws Exception {
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

        publisher.publishDiagnosticAction("turbine-10", "LOCK_BRAKES");

        verify(kafkaTemplate).send(eq("events.status"), eq("turbine-10"), payloadCaptor.capture());
        JsonNode jsonNode = objectMapper.readTree(payloadCaptor.getValue());

        assertEquals("turbine-10", jsonNode.get("assetId").asText());
        assertEquals("LOCK_BRAKES", jsonNode.get("action").asText());
        assertEquals("DIAGNOSTIC_ACTION", jsonNode.get("eventType").asText());
    }

    @Test
    void publish_whenKafkaFails_shouldThrowEventPublishingException() {
        CompletableFuture<org.springframework.kafka.support.SendResult<String, String>> failedFuture =
                new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka timeout"));

        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(failedFuture);

        assertThrows(EventPublishingException.class, () ->
                publisher.publishDiagnosticAction("turbine-10", "LOCK_BRAKES")
        );
    }
}
