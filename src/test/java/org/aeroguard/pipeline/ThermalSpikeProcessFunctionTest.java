package org.aeroguard.pipeline;

import org.aeroguard.model.Alert;
import org.aeroguard.model.Telemetry;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.util.KeyedOneInputStreamOperatorTestHarness;
import org.apache.flink.streaming.util.ProcessFunctionTestHarnesses;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ThermalSpikeProcessFunctionTest {

    private KeyedOneInputStreamOperatorTestHarness<String, Telemetry, Alert> testHarness;

    @BeforeEach
    void setUp() throws Exception {
        ThermalSpikeProcessFunction processFunction = new ThermalSpikeProcessFunction(80.0, 5);
        testHarness = ProcessFunctionTestHarnesses.forKeyedProcessFunction(
                processFunction,
                Telemetry::getAssetId,
                Types.STRING
        );
        testHarness.open();
    }

    @Test
    void testNormalTemperaturesDoNotTriggerAlert() throws Exception {
        Instant now = Instant.now();
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
        assertEquals(2, alerts.size());

        Alert alert1 = alerts.get(0);
        assertEquals("turbine-2", alert1.getAssetId());
        assertEquals("sensor-2", alert1.getSensorId());
        assertEquals("THERMAL_SPIKE", alert1.getAlertType());
        assertEquals(85.0, alert1.getTemperature());
        assertEquals(80.0, alert1.getThreshold());

        // Check deterministic UUID format
        String expectedUUID = ThermalSpikeProcessFunction.generateAlertId("turbine-2", now.toEpochMilli(), "THERMAL_SPIKE");
        assertEquals(expectedUUID, alert1.getAlertId());

        Alert alert2 = alerts.get(1);
        assertEquals(90.0, alert2.getTemperature()); // rolling average of 85.0 and 95.0 is 90.0
    }
}
