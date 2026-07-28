package org.aeroguard.pipeline;

import org.aeroguard.model.Alert;
import org.aeroguard.model.AssetEvent;
import org.aeroguard.model.AssetOperatingModeEvent;
import org.aeroguard.model.ConfigEvent;
import org.aeroguard.model.DiagnosticAction;
import org.aeroguard.model.DiagnosticActionRule;
import org.aeroguard.model.Telemetry;
import org.aeroguard.model.ThresholdConfig;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.util.KeyedBroadcastOperatorTestHarness;
import org.apache.flink.streaming.util.ProcessFunctionTestHarnesses;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ThermalSpikeProcessFunctionTest {

    private KeyedBroadcastOperatorTestHarness<String, AssetEvent, ConfigEvent, Alert> testHarness;

    @BeforeEach
    void setUp() throws Exception {
        ThermalSpikeProcessFunction processFunction = new ThermalSpikeProcessFunction(80.0, 5);
        testHarness = ProcessFunctionTestHarnesses.forKeyedBroadcastProcessFunction(
                processFunction,
                AssetEvent::getAssetId,
                Types.STRING,
                ThermalSpikeProcessFunction.THRESHOLD_STATE_DESCRIPTOR,
                ThermalSpikeProcessFunction.DIAGNOSTIC_ACTION_STATE_DESCRIPTOR
        );
        testHarness.open();
    }

    @Test
    void testNormalTemperaturesDoNotTriggerAlert() throws Exception {
        Instant now = Instant.ofEpochMilli(1700000000000L);
        Telemetry t1 = new Telemetry("turbine-1", "sensor-1", now, 1.2, 50.0);
        Telemetry t2 = new Telemetry("turbine-1", "sensor-1", now.plusSeconds(1), 1.3, 60.0);

        testHarness.processElement(AssetEvent.fromTelemetry(t1), now.toEpochMilli());
        testHarness.processElement(AssetEvent.fromTelemetry(t2), now.plusSeconds(1).toEpochMilli());

        List<Alert> alerts = testHarness.extractOutputValues();
        assertTrue(alerts.isEmpty(), "No alerts should be emitted for normal temperatures");
    }

    @Test
    void testThermalSpikeTriggersDeterministicAlert() throws Exception {
        Instant now = Instant.ofEpochMilli(1700000000000L);
        Telemetry t1 = new Telemetry("turbine-2", "sensor-2", now, 1.0, 85.0);
        Telemetry t2 = new Telemetry("turbine-2", "sensor-2", now.plusSeconds(1), 1.1, 95.0);

        testHarness.processElement(AssetEvent.fromTelemetry(t1), now.toEpochMilli());
        testHarness.processElement(AssetEvent.fromTelemetry(t2), now.plusSeconds(1).toEpochMilli());

        List<Alert> alerts = testHarness.extractOutputValues();
        assertEquals(1, alerts.size());

        Alert alert1 = alerts.get(0);
        assertEquals("turbine-2", alert1.getAssetId());
        assertEquals("sensor-2", alert1.getSensorId());
        assertEquals("THERMAL_SPIKE", alert1.getAlertType());
        assertEquals(85.0, alert1.getTemperature());
        assertEquals(80.0, alert1.getThreshold());

        String expectedUUID = ThermalSpikeProcessFunction.generateAlertId("turbine-2", now.toEpochMilli(), "THERMAL_SPIKE");
        assertEquals(expectedUUID, alert1.getAlertId());
    }

    @Test
    void testVibrationSensorIgnoredForThermalSpike() throws Exception {
        Instant now = Instant.ofEpochMilli(1700000000000L);
        Telemetry t1 = new Telemetry("turbine-3", "vibration-sensor-1", now, 5.0, 95.0);
        testHarness.processElement(AssetEvent.fromTelemetry(t1), now.toEpochMilli());

        List<Alert> alerts = testHarness.extractOutputValues();
        assertTrue(alerts.isEmpty(), "Vibration sensors should not trigger thermal spike alerts");
    }

    @Test
    void testDynamicThresholdBroadcastUpdatesAlertLimitImmediately() throws Exception {
        Instant now = Instant.ofEpochMilli(1700000000000L);

        Telemetry t1 = new Telemetry("turbine-4", "sensor-1", now, 1.0, 75.0);
        testHarness.processElement(AssetEvent.fromTelemetry(t1), now.toEpochMilli());
        assertTrue(testHarness.extractOutputValues().isEmpty());

        ThresholdConfig config = new ThresholdConfig("turbine-4", 70.0);
        testHarness.processBroadcastElement(ConfigEvent.fromThreshold(config), now.plusSeconds(1).toEpochMilli());

        Telemetry t2 = new Telemetry("turbine-4", "sensor-1", now.plusSeconds(2), 1.0, 75.0);
        testHarness.processElement(AssetEvent.fromTelemetry(t2), now.plusSeconds(2).toEpochMilli());

        List<Alert> alerts = testHarness.extractOutputValues();
        assertEquals(1, alerts.size());
        Alert alert = alerts.get(0);
        assertEquals("turbine-4", alert.getAssetId());
        assertEquals(70.0, alert.getThreshold());
        assertEquals(75.0, alert.getTemperature());
    }

    @Test
    void testGlobalDynamicThresholdBroadcastAppliesToAllAssets() throws Exception {
        Instant now = Instant.ofEpochMilli(1700000000000L);

        ThresholdConfig globalConfig = new ThresholdConfig("GLOBAL", 65.0);
        testHarness.processBroadcastElement(ConfigEvent.fromThreshold(globalConfig), now.toEpochMilli());

        Telemetry t1 = new Telemetry("turbine-5", "sensor-1", now.plusSeconds(1), 1.0, 70.0);
        testHarness.processElement(AssetEvent.fromTelemetry(t1), now.plusSeconds(1).toEpochMilli());

        List<Alert> alerts = testHarness.extractOutputValues();
        assertEquals(1, alerts.size());
        Alert alert = alerts.get(0);
        assertEquals("turbine-5", alert.getAssetId());
        assertEquals(65.0, alert.getThreshold());
    }

    @Test
    void testSpecificAssetThresholdOverridesGlobalBroadcastThreshold() throws Exception {
        Instant now = Instant.ofEpochMilli(1700000000000L);

        testHarness.processBroadcastElement(ConfigEvent.fromThreshold(new ThresholdConfig("GLOBAL", 60.0)), now.toEpochMilli());
        testHarness.processBroadcastElement(ConfigEvent.fromThreshold(new ThresholdConfig("turbine-6", 80.0)), now.plusSeconds(1).toEpochMilli());

        Telemetry t1 = new Telemetry("turbine-6", "sensor-1", now.plusSeconds(2), 1.0, 70.0);
        testHarness.processElement(AssetEvent.fromTelemetry(t1), now.plusSeconds(2).toEpochMilli());

        assertTrue(testHarness.extractOutputValues().isEmpty());

        Telemetry t2 = new Telemetry("turbine-7", "sensor-1", now.plusSeconds(3), 1.0, 70.0);
        testHarness.processElement(AssetEvent.fromTelemetry(t2), now.plusSeconds(3).toEpochMilli());

        List<Alert> alerts = testHarness.extractOutputValues();
        assertEquals(1, alerts.size());
        assertEquals("turbine-7", alerts.get(0).getAssetId());
        assertEquals(60.0, alerts.get(0).getThreshold());
    }

    @Test
    void testOtherAlertTypeThresholdsAreIgnored() throws Exception {
        Instant now = Instant.ofEpochMilli(1700000000000L);

        ThresholdConfig vibrationConfig = new ThresholdConfig("turbine-8", 50.0, "VIBRATION_SPIKE");
        testHarness.processBroadcastElement(ConfigEvent.fromThreshold(vibrationConfig), now.toEpochMilli());

        Telemetry t1 = new Telemetry("turbine-8", "sensor-1", now.plusSeconds(1), 1.0, 70.0);
        testHarness.processElement(AssetEvent.fromTelemetry(t1), now.plusSeconds(1).toEpochMilli());

        List<Alert> alerts = testHarness.extractOutputValues();
        assertTrue(alerts.isEmpty(), "Threshold configs for other alert types must be ignored");
    }

    @Test
    void testMaintenanceModeSuppressesThermalSpikeAlerts() throws Exception {
        Instant now = Instant.ofEpochMilli(1700000000000L);

        AssetOperatingModeEvent modeEvent = new AssetOperatingModeEvent("turbine-10", "MAINTENANCE_MODE", now);
        testHarness.processElement(AssetEvent.fromOperatingMode(modeEvent), now.toEpochMilli());

        Telemetry t1 = new Telemetry("turbine-10", "sensor-1", now.plusSeconds(1), 1.0, 95.0);
        testHarness.processElement(AssetEvent.fromTelemetry(t1), now.plusSeconds(1).toEpochMilli());

        List<Alert> alerts = testHarness.extractOutputValues();
        assertTrue(alerts.isEmpty(), "Thermal spike alerts must be suppressed when asset is in MAINTENANCE_MODE");
    }

    @Test
    void testTransitionFromMaintenanceModeToOnlineResumesAlerts() throws Exception {
        Instant now = Instant.ofEpochMilli(1700000000000L);

        AssetOperatingModeEvent maintenance = new AssetOperatingModeEvent("turbine-11", "MAINTENANCE_MODE", now);
        testHarness.processElement(AssetEvent.fromOperatingMode(maintenance), now.toEpochMilli());

        Telemetry t1 = new Telemetry("turbine-11", "sensor-1", now.plusSeconds(1), 1.0, 90.0);
        testHarness.processElement(AssetEvent.fromTelemetry(t1), now.plusSeconds(1).toEpochMilli());
        assertTrue(testHarness.extractOutputValues().isEmpty());

        AssetOperatingModeEvent online = new AssetOperatingModeEvent("turbine-11", "ONLINE", now.plusSeconds(2));
        testHarness.processElement(AssetEvent.fromOperatingMode(online), now.plusSeconds(2).toEpochMilli());

        Telemetry t2 = new Telemetry("turbine-11", "sensor-1", now.plusSeconds(3), 1.0, 92.0);
        testHarness.processElement(AssetEvent.fromTelemetry(t2), now.plusSeconds(3).toEpochMilli());

        List<Alert> alerts = testHarness.extractOutputValues();
        assertEquals(1, alerts.size());
        assertEquals("turbine-11", alerts.get(0).getAssetId());
        assertEquals(91.0, alerts.get(0).getTemperature(), 0.01);
    }

    @Test
    void testMaintenanceModeIsKeyedPerAsset() throws Exception {
        Instant now = Instant.ofEpochMilli(1700000000000L);

        testHarness.processElement(AssetEvent.fromOperatingMode(new AssetOperatingModeEvent("turbine-A", "MAINTENANCE_MODE", now)), now.toEpochMilli());
        testHarness.processElement(AssetEvent.fromOperatingMode(new AssetOperatingModeEvent("turbine-B", "ONLINE", now)), now.toEpochMilli());

        Telemetry tA = new Telemetry("turbine-A", "sensor-1", now.plusSeconds(1), 1.0, 95.0);
        testHarness.processElement(AssetEvent.fromTelemetry(tA), now.plusSeconds(1).toEpochMilli());

        Telemetry tB = new Telemetry("turbine-B", "sensor-1", now.plusSeconds(2), 1.0, 95.0);
        testHarness.processElement(AssetEvent.fromTelemetry(tB), now.plusSeconds(2).toEpochMilli());

        List<Alert> alerts = testHarness.extractOutputValues();
        assertEquals(1, alerts.size());
        assertEquals("turbine-B", alerts.get(0).getAssetId());
    }

    @Test
    void testAlertIncludesDiagnosticActionWithFallbackWhenNoRuleRegistered() throws Exception {
        Instant now = Instant.ofEpochMilli(1700000000000L);
        Telemetry t1 = new Telemetry("turbine-fallback", "sensor-1", now, 1.0, 95.0);

        testHarness.processElement(AssetEvent.fromTelemetry(t1), now.toEpochMilli());

        List<Alert> alerts = testHarness.extractOutputValues();
        assertEquals(1, alerts.size());
        Alert alert = alerts.get(0);
        assertNotNull(alert.getDiagnosticAction(), "Alert must have enriched diagnostic action");
        assertTrue(alert.getDiagnosticAction().isFallback());
        assertEquals("Dispatch Tech Team & Manual Inspection Required", alert.getDiagnosticAction().getTitle());
    }

    @Test
    void testBroadcastedDiagnosticActionRuleEnrichesAlert() throws Exception {
        Instant now = Instant.ofEpochMilli(1700000000000L);

        DiagnosticAction customAction = new DiagnosticAction(
                "ACT-TEST-99",
                "Remote Pitch Aerodynamic Calibration",
                "Execute automatic 15-degree pitch adjustment to reduce rotor temperature.",
                "CRITICAL",
                95,
                "AUTOMATED_SYSTEM"
        );

        DiagnosticActionRule rule = new DiagnosticActionRule(
                "RULE-TEST-99",
                "turbine-custom",
                "THERMAL_SPIKE",
                "ONLINE",
                customAction
        );

        testHarness.processBroadcastElement(ConfigEvent.fromDiagnosticActionRule(rule), now.toEpochMilli());

        Telemetry t1 = new Telemetry("turbine-custom", "sensor-1", now.plusSeconds(1), 1.0, 95.0);
        testHarness.processElement(AssetEvent.fromTelemetry(t1), now.plusSeconds(1).toEpochMilli());

        List<Alert> alerts = testHarness.extractOutputValues();
        assertEquals(1, alerts.size());
        Alert alert = alerts.get(0);
        assertNotNull(alert.getDiagnosticAction());
        assertFalse(alert.getDiagnosticAction().isFallback());
        assertEquals("ACT-TEST-99", alert.getDiagnosticAction().getActionId());
        assertEquals("Remote Pitch Aerodynamic Calibration", alert.getDiagnosticAction().getTitle());
    }
}
