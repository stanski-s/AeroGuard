package org.aeroguard.pipeline;

import org.aeroguard.model.Alert;
import org.aeroguard.model.DiagnosticAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AlertSerializerTest {

    private TelemetryPipeline.AlertSerializer serializer;

    @BeforeEach
    void setUp() throws Exception {
        serializer = new TelemetryPipeline.AlertSerializer();
        serializer.open(null);
    }

    @Test
    void testSerializeAlertWithDiagnosticActionToJson() throws Exception {
        DiagnosticAction action = new DiagnosticAction(
                "ACT-001",
                "Lock Brakes",
                "Lock mechanical brakes",
                "CRITICAL",
                90,
                "OPERATOR"
        );

        Alert alert = new Alert(
                "ALERT-12345",
                "turbine-501",
                "temp-sensor-1",
                "THERMAL_SPIKE",
                92.5,
                80.0,
                Instant.parse("2026-07-29T11:00:00Z"),
                "Thermal spike detected on turbine-501",
                action
        );

        String json = serializer.map(alert);

        assertNotNull(json);
        assertTrue(json.contains("\"alert_id\":\"ALERT-12345\""));
        assertTrue(json.contains("\"asset_id\":\"turbine-501\""));
        assertTrue(json.contains("\"alert_type\":\"THERMAL_SPIKE\""));
        assertTrue(json.contains("\"temperature\":92.5"));
        assertTrue(json.contains("\"threshold\":80.0"));
        assertTrue(json.contains("\"diagnostic_action\""));
        assertTrue(json.contains("\"action_id\":\"ACT-001\""));
    }

    @Test
    void testSerializeAlertWithoutDiagnosticAction() throws Exception {
        Alert alert = new Alert(
                "ALERT-67890",
                "turbine-502",
                "temp-sensor-2",
                "THERMAL_SPIKE",
                88.0,
                80.0,
                Instant.parse("2026-07-29T11:05:00Z"),
                "Thermal spike detected",
                null
        );

        String json = serializer.map(alert);

        assertNotNull(json);
        assertTrue(json.contains("\"alert_id\":\"ALERT-67890\""));
        assertTrue(json.contains("\"asset_id\":\"turbine-502\""));
    }
}
