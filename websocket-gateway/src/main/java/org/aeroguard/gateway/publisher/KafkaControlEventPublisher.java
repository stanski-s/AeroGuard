package org.aeroguard.gateway.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.aeroguard.gateway.exception.EventPublishingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Concrete Kafka adapter implementing ControlEventPublisher seam.
 * Encapsulates Kafka topics, DTO serialization, async futures, and 5-second timeouts.
 */
@Service
public class KafkaControlEventPublisher implements ControlEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(KafkaControlEventPublisher.class);
    private static final String EVENTS_STATUS_TOPIC = "events.status";
    private static final String TELEMETRY_TOPIC = "telemetry.raw";
    private static final long DEFAULT_TIMEOUT_SECONDS = 5L;

    public record DiagnosticActionEvent(
            String assetId,
            String action,
            String eventType,
            String timestamp
    ) {}

    public record OperatingModeEvent(
            String assetId,
            String operatingMode,
            String eventType,
            String timestamp
    ) {}

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaControlEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public void publishOperatingMode(String assetId, String mode) {
        OperatingModeEvent event = new OperatingModeEvent(
                assetId,
                mode,
                "OPERATING_MODE_CHANGE",
                Instant.now().toString()
        );
        sendKafkaEvent(EVENTS_STATUS_TOPIC, assetId, event,
                String.format("Published AssetOperatingModeEvent for asset %s: mode=%s", assetId, mode));
    }

    @Override
    public void publishThermalSpike(String assetId, double temperature) {
        Map<String, Object> telemetry = Map.of(
                "assetId", assetId,
                "temperature", temperature,
                "vibration", 0.28,
                "timestamp", Instant.now().toString()
        );
        sendKafkaEvent(TELEMETRY_TOPIC, assetId, telemetry,
                String.format("Triggered thermal spike for asset %s: temperature=%.1f°C", assetId, temperature));
    }

    @Override
    public void publishDiagnosticAction(String assetId, String action) {
        DiagnosticActionEvent event = new DiagnosticActionEvent(
                assetId,
                action,
                "DIAGNOSTIC_ACTION",
                Instant.now().toString()
        );
        sendKafkaEvent(EVENTS_STATUS_TOPIC, assetId, event,
                String.format("Published DiagnosticAction for asset %s: action=%s", assetId, action));
    }

    private void sendKafkaEvent(String topic, String assetId, Object payload, String logMessage) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, assetId, jsonPayload).get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            logger.info(logMessage);
        } catch (Exception e) {
            logger.error("Failed to publish Kafka event to topic {} for asset {}", topic, assetId, e);
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Failed to publish event to Kafka";
            throw new EventPublishingException(errorMsg, e);
        }
    }
}
