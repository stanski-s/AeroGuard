package org.aeroguard.gateway.controller;

import org.aeroguard.gateway.exception.EventPublishingException;
import org.aeroguard.gateway.publisher.ControlEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * Minimal REST API Gateway controller.
 * Delegates domain event dispatching to ControlEventPublisher seam.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class GatewayController {

    private static final Logger logger = LoggerFactory.getLogger(GatewayController.class);

    private static final Set<String> ALLOWED_ACTIONS = Set.of(
            "LOCK_BRAKES",
            "DERATE_POWER",
            "RECALIBRATE_PITCH",
            "DISPATCH_TECH"
    );

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

    private final ControlEventPublisher eventPublisher;

    public GatewayController(ControlEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/assets/{assetId}/operating-mode")
    public ResponseEntity<Map<String, Object>> updateOperatingMode(
            @PathVariable("assetId") String assetId,
            @RequestParam("mode") String mode) {
        try {
            eventPublisher.publishOperatingMode(assetId, mode);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "assetId", assetId,
                    "operatingMode", mode
            ));
        } catch (EventPublishingException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/simulator/spike")
    public ResponseEntity<Map<String, Object>> triggerThermalSpike(
            @RequestParam(value = "assetId", defaultValue = "turbine-1") String assetId,
            @RequestParam(value = "temperature", defaultValue = "88.5") double temperature) {
        try {
            eventPublisher.publishThermalSpike(assetId, temperature);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "assetId", assetId,
                    "temperature", temperature
            ));
        } catch (EventPublishingException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()
            ));
        }
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
        try {
            eventPublisher.publishDiagnosticAction(assetId, normalizedAction);
            return ResponseEntity.ok(ActionResponse.success(assetId, normalizedAction));
        } catch (EventPublishingException e) {
            return ResponseEntity.internalServerError().body(ActionResponse.error(e.getMessage()));
        }
    }
}
