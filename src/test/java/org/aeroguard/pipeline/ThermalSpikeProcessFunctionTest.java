package org.aeroguard.pipeline;

import org.aeroguard.model.Alert;
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

    private KeyedBroadcastOperatorTestHarness<String, Telemetry, ThresholdConfig, Alert> testHarness;

    @BeforeEach
    void setUp() throws Exception {
        ThermalSpikeProcessFunction processFunction = new ThermalSpikeProcessFunction(80.0, 5);
        testHarness = ProcessFunctionTestHarnesses.forKeyedBroadcastProcessFunction(
                processFunction,
                Telemetry::getAssetId,
                Types.STRING,
                ThermalSpikeProcessFunction.THRESHOLD_STATE_DESCRIPTOR
        );
        testHarness.open();
    }

    @Test
    void testNormalTemperaturesDoNotTriggerAlert() throws Exception {
        Instant now = Instant.ofEpochMilli(1700000000000L);
        Telemetry t1 = new Telemetry("turbine-1", "sensor-1", now, 1.2, 50.0);
        Telemetry t2 = new Telemetry("turbine-1", "sensor-1", now.plusSeconds(1), 1.3, 60.0);

        testHarness.processElement(t1, now.toEpochMilli());
        testHarness.processElement(t2, now.plusSeconds(1).toEpochMilli());

        List<Alert> alerts = testHarness.extractOutputValues();
        assertTrue(alerts.isEmpty(), "No alerts should be emitted for normal temperatures");
    }

    @Test
    void testThermalSpikeTriggersDeterministicAlert() throws Exception {
        Instant now = Instant.ofEpochMilli(1700000000000L);
        Telemetry t1 = new Telemetry("turbine-2", "sensor-2", now, 1.0, 85.0);
        Telemetry t2 = new Telemetry("turbine-2", "sensor-2", now.plusSeconds(1), 1.1, 95.0);

        testHarness.processElement(t1, now.toEpochMilli());
        testHarness.processElement(t2, now.plusSeconds(1).toEpochMilli());

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
        testHarness.processElement(t1, now.toEpochMilli());

        List<Alert> alerts = testHarness.extractOutputValues();
        assertTrue(alerts.isEmpty(), "Vibration sensors should not trigger thermal spike alerts");
    }

    @Test
    void testDynamicThresholdBroadcastUpdatesAlertLimitImmediately() throws Exception {
        Instant now = Instant.ofEpochMilli(1700000000000L);

        // 75°C is below default threshold 80°C
        Telemetry t1 = new Telemetry("turbine-4", "sensor-1", now, 1.0, 75.0);
        testHarness.processElement(t1, now.toEpochMilli());
        assertTrue(testHarness.extractOutputValues().isEmpty());

        // Broadcast a lower dynamic threshold (70°C) for turbine-4
        ThresholdConfig config = new ThresholdConfig("turbine-4", 70.0);
        testHarness.processBroadcastElement(config, now.plusSeconds(1).toEpochMilli());

        // Next telemetry at 75°C now breaches the new dynamic threshold (70°C)
        Telemetry t2 = new Telemetry("turbine-4", "sensor-1", now.plusSeconds(2), 1.0, 75.0);
        testHarness.processElement(t2, now.plusSeconds(2).toEpochMilli());

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

        // Broadcast global threshold of 65°C
        ThresholdConfig globalConfig = new ThresholdConfig("GLOBAL", 65.0);
        testHarness.processBroadcastElement(globalConfig, now.toEpochMilli());

        // Turbine-5 with 70°C should breach global threshold 65°C
        Telemetry t1 = new Telemetry("turbine-5", "sensor-1", now.plusSeconds(1), 1.0, 70.0);
        testHarness.processElement(t1, now.plusSeconds(1).toEpochMilli());

        List<Alert> alerts = testHarness.extractOutputValues();
        assertEquals(1, alerts.size());
        Alert alert = alerts.get(0);
        assertEquals("turbine-5", alert.getAssetId());
        assertEquals(65.0, alert.getThreshold());
    }

    @Test
    void testSpecificAssetThresholdOverridesGlobalBroadcastThreshold() throws Exception {
        Instant now = Instant.ofEpochMilli(1700000000000L);

        // Set global threshold 60°C
        testHarness.processBroadcastElement(new ThresholdConfig("GLOBAL", 60.0), now.toEpochMilli());
        // Set specific threshold 80°C for turbine-6
        testHarness.processBroadcastElement(new ThresholdConfig("turbine-6", 80.0), now.plusSeconds(1).toEpochMilli());

        // Telemetry for turbine-6 at 70°C: below specific threshold 80°C, so no alert
        Telemetry t1 = new Telemetry("turbine-6", "sensor-1", now.plusSeconds(2), 1.0, 70.0);
        testHarness.processElement(t1, now.plusSeconds(2).toEpochMilli());

        assertTrue(testHarness.extractOutputValues().isEmpty());

        // Telemetry for turbine-7 (no specific threshold) at 70°C: breaches global threshold 60°C
        Telemetry t2 = new Telemetry("turbine-7", "sensor-1", now.plusSeconds(3), 1.0, 70.0);
        testHarness.processElement(t2, now.plusSeconds(3).toEpochMilli());

        List<Alert> alerts = testHarness.extractOutputValues();
        assertEquals(1, alerts.size());
        assertEquals("turbine-7", alerts.get(0).getAssetId());
        assertEquals(60.0, alerts.get(0).getThreshold());
    }

    @Test
    void testOtherAlertTypeThresholdsAreIgnored() throws Exception {
        Instant now = Instant.ofEpochMilli(1700000000000L);

        // Broadcast threshold for a different alert type (e.g. VIBRATION_SPIKE) with lower threshold 50.0
        ThresholdConfig vibrationConfig = new ThresholdConfig("turbine-8", 50.0, "VIBRATION_SPIKE");
        testHarness.processBroadcastElement(vibrationConfig, now.toEpochMilli());

        // Telemetry at 70°C for turbine-8 should not breach default thermal threshold 80°C because vibrationConfig is ignored
        Telemetry t1 = new Telemetry("turbine-8", "sensor-1", now.plusSeconds(1), 1.0, 70.0);
        testHarness.processElement(t1, now.plusSeconds(1).toEpochMilli());

        List<Alert> alerts = testHarness.extractOutputValues();
        assertTrue(alerts.isEmpty(), "Threshold configs for other alert types must be ignored");
    }
}
