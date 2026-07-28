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
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class GatewayController {

    private static final Logger logger = LoggerFactory.getLogger(GatewayController.class);
    private static final String EVENTS_STATUS_TOPIC = "events.status";
    private static final String TELEMETRY_TOPIC = "telemetry.raw";
    private static final Set<String> ALLOWED_ACTIONS = Set.of(
            "LOCK_BRAKES",
            "DERATE_POWER",
            "RECALIBRATE_PITCH",
            "DISPATCH_TECH"
    );

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

    public record ActionResponse(
            String status,
            String assetId,
            String action,
            String message
    ) {
        public static ActionResponse success(String assetId, String action) {
            return new ActionResponse("SUCCESS", assetId, action, null);
        }

        public static ActionResponse error(String message) {
            return new ActionResponse("ERROR", null, null, message);
        }
    }

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
        OperatingModeEvent event = new OperatingModeEvent(
                assetId,
                mode,
                "OPERATING_MODE_CHANGE",
                Instant.now().toString()
        );
        Map<String, Object> responseData = Map.of("operatingMode", mode);
        return publishKafkaEvent(EVENTS_STATUS_TOPIC, assetId, event, responseData,
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
    public ResponseEntity<ActionResponse> triggerDiagnosticAction(
            @PathVariable("assetId") String assetId,
            @RequestParam("action") String action) {
        if (action == null || !ALLOWED_ACTIONS.contains(action.toUpperCase())) {
            ActionResponse errorResponse = ActionResponse.error(
                    "Invalid diagnostic action: " + action + ". Allowed actions: " + ALLOWED_ACTIONS
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }

        String normalizedAction = action.toUpperCase();
        DiagnosticActionEvent event = new DiagnosticActionEvent(
                assetId,
                normalizedAction,
                "DIAGNOSTIC_ACTION",
                Instant.now().toString()
        );

        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(EVENTS_STATUS_TOPIC, assetId, jsonPayload).get(5, TimeUnit.SECONDS);
            logger.info("Published DiagnosticAction for asset {}: action={}", assetId, normalizedAction);
            return ResponseEntity.ok(ActionResponse.success(assetId, normalizedAction));
        } catch (Exception e) {
            logger.error("Failed to publish Kafka event to topic {} for asset {}", EVENTS_STATUS_TOPIC, assetId, e);
            ActionResponse errorResponse = ActionResponse.error(
                    e.getMessage() != null ? e.getMessage() : "Failed to publish diagnostic action to Kafka"
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    private ResponseEntity<Map<String, Object>> publishKafkaEvent(
            String topic,
            String assetId,
            Object payload,
            Map<String, Object> responseData,
            String logMessage) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, assetId, jsonPayload).get(5, TimeUnit.SECONDS);
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
