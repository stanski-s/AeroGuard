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
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("assetId", assetId);
            event.put("operatingMode", mode);
            event.put("timestamp", Instant.now().toString());

            String jsonPayload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("events.status", assetId, jsonPayload);

            logger.info("Published AssetOperatingModeEvent to Kafka events.status for asset {}: mode={}", assetId, mode);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("assetId", assetId);
            response.put("operatingMode", mode);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to publish OperatingMode event for asset {}", assetId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/simulator/spike")
    public ResponseEntity<Map<String, Object>> triggerThermalSpike(
            @RequestParam(value = "assetId", defaultValue = "turbine-1") String assetId,
            @RequestParam(value = "temperature", defaultValue = "88.5") double temperature) {
        try {
            Map<String, Object> telemetry = new HashMap<>();
            telemetry.put("assetId", assetId);
            telemetry.put("sensorId", "temp-" + assetId);
            telemetry.put("temperature", temperature);
            telemetry.put("vibration", 0.28);
            telemetry.put("timestamp", Instant.now().toString());

            String jsonPayload = objectMapper.writeValueAsString(telemetry);
            kafkaTemplate.send("telemetry.raw", assetId, jsonPayload);

            logger.info("Triggered thermal spike for asset {}: temperature={}°C", assetId, temperature);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("assetId", assetId);
            response.put("temperature", temperature);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to publish thermal spike for asset {}", assetId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/assets/{assetId}/action")
    public ResponseEntity<Map<String, Object>> triggerDiagnosticAction(
            @PathVariable("assetId") String assetId,
            @RequestParam("action") String action) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("assetId", assetId);
            event.put("action", action);
            event.put("timestamp", Instant.now().toString());

            String jsonPayload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("events.status", assetId, jsonPayload);

            logger.info("Published DiagnosticAction to Kafka events.status for asset {}: action={}", assetId, action);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("assetId", assetId);
            response.put("action", action);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to publish DiagnosticAction for asset {}", assetId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
