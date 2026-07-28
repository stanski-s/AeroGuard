package org.aeroguard.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class GatewayController {

    private static final Logger logger = LoggerFactory.getLogger(GatewayController.class);
    private static final String OPERATING_MODE_TOPIC = "events.status";
    private static final String TELEMETRY_TOPIC = "telemetry.raw";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public GatewayController(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @PostMapping("/assets/{assetId}/operating-mode")
    public ResponseEntity<Map<String, Object>> updateOperatingMode(
            @PathVariable("assetId") String assetId,
            @RequestParam("mode") String mode) {
        Map<String, Object> event = Map.of(
                "assetId", assetId,
                "operatingMode", mode,
                "timestamp", Instant.now().toString()
        );
        Map<String, Object> responseData = Map.of("operatingMode", mode);
        return publishKafkaEvent(OPERATING_MODE_TOPIC, assetId, event, responseData,
                String.format("Published AssetOperatingModeEvent for asset %s: mode=%s", assetId, mode));
    }

    @PostMapping("/simulator/spike")
    public ResponseEntity<Map<String, Object>> triggerThermalSpike(
            @RequestParam(value = "assetId", defaultValue = "turbine-1") String assetId,
            @RequestParam(value = "temperature", defaultValue = "88.5") double temperature) {
        Map<String, Object> telemetry = Map.of(
                "assetId", assetId,
                "sensorId", "temp-" + assetId,
                "temperature", temperature,
                "vibration", 0.28,
                "timestamp", Instant.now().toString()
        );
        Map<String, Object> responseData = Map.of("temperature", temperature);
        return publishKafkaEvent(TELEMETRY_TOPIC, assetId, telemetry, responseData,
                String.format("Triggered thermal spike for asset %s: temperature=%.1f°C", assetId, temperature));
    }

    @PostMapping("/assets/{assetId}/action")
    public ResponseEntity<Map<String, Object>> triggerDiagnosticAction(
            @PathVariable("assetId") String assetId,
            @RequestParam("action") String action) {
        Map<String, Object> event = Map.of(
                "assetId", assetId,
                "action", action,
                "timestamp", Instant.now().toString()
        );
        Map<String, Object> responseData = Map.of("action", action);
        return publishKafkaEvent(OPERATING_MODE_TOPIC, assetId, event, responseData,
                String.format("Published DiagnosticAction for asset %s: action=%s", assetId, action));
    }

    private ResponseEntity<Map<String, Object>> publishKafkaEvent(
            String topic,
            String assetId,
            Map<String, Object> payload,
            Map<String, Object> responseData,
            String logMessage) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, assetId, jsonPayload);
            logger.info(logMessage);

            Map<String, Object> response = new HashMap<>(responseData);
            response.put("status", "SUCCESS");
            response.put("assetId", assetId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to publish Kafka event to topic {} for asset {}", topic, assetId, e);
            Map<String, Object> errorResponse = Map.of(
                    "status", "ERROR",
                    "message", e.getMessage() != null ? e.getMessage() : "Unknown error"
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
