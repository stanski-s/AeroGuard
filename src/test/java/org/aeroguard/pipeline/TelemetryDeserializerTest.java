package org.aeroguard.pipeline;

import org.aeroguard.model.Telemetry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TelemetryDeserializerTest {

    private TelemetryPipeline.TelemetryDeserializer deserializer;

    @BeforeEach
    void setUp() throws Exception {
        deserializer = new TelemetryPipeline.TelemetryDeserializer();
        deserializer.open(null);
    }

    @Test
    void testDeserializeSnakeCaseTelemetryJson() throws Exception {
        String json = "{"
                + "\"asset_id\":\"turbine-101\","
                + "\"sensor_id\":\"temp-sensor-01\","
                + "\"timestamp\":\"2026-07-29T10:00:00Z\","
                + "\"vibration\":0.45,"
                + "\"temperature\":82.5,"
                + "\"power_output_mw\":12.4,"
                + "\"pitch_angle_deg\":4.2,"
                + "\"rotor_speed_rpm\":7.5,"
                + "\"nacelle_temp_c\":38.1"
                + "}";

        Telemetry telemetry = deserializer.map(json);

        assertNotNull(telemetry);
        assertEquals("turbine-101", telemetry.getAssetId());
        assertEquals(Instant.parse("2026-07-29T10:00:00Z"), telemetry.getTimestamp());
        assertEquals(0.45, telemetry.getVibration(), 0.001);
        assertEquals(82.5, telemetry.getTemperature(), 0.001);
        assertEquals(12.4, telemetry.getPowerOutputMw(), 0.001);
        assertEquals(4.2, telemetry.getPitchAngleDeg(), 0.001);
        assertEquals(7.5, telemetry.getRotorSpeedRpm(), 0.001);
        assertEquals(38.1, telemetry.getNacelleTempC(), 0.001);
    }

    @Test
    void testDeserializeCamelCaseTelemetryJson() throws Exception {
        String json = "{"
                + "\"assetId\":\"turbine-102\","
                + "\"sensorId\":\"temp-sensor-02\","
                + "\"timestamp\":\"2026-07-29T10:05:00Z\","
                + "\"vibration\":0.30,"
                + "\"temperature\":71.0,"
                + "\"powerOutputMw\":13.1,"
                + "\"pitchAngleDeg\":4.0,"
                + "\"rotorSpeedRpm\":7.8,"
                + "\"nacelleTempC\":36.5"
                + "}";

        Telemetry telemetry = deserializer.map(json);

        assertNotNull(telemetry);
        assertEquals("turbine-102", telemetry.getAssetId());
        assertEquals(Instant.parse("2026-07-29T10:05:00Z"), telemetry.getTimestamp());
        assertEquals(0.30, telemetry.getVibration(), 0.001);
        assertEquals(71.0, telemetry.getTemperature(), 0.001);
        assertEquals(13.1, telemetry.getPowerOutputMw(), 0.001);
        assertEquals(4.0, telemetry.getPitchAngleDeg(), 0.001);
        assertEquals(7.8, telemetry.getRotorSpeedRpm(), 0.001);
        assertEquals(36.5, telemetry.getNacelleTempC(), 0.001);
    }

    @Test
    void testDeserializePartialTelemetryJson() throws Exception {
        String json = "{"
                + "\"asset_id\":\"turbine-103\","
                + "\"sensor_id\":\"sensor-03\","
                + "\"vibration\":0.12,"
                + "\"temperature\":65.0"
                + "}";

        Telemetry telemetry = deserializer.map(json);

        assertNotNull(telemetry);
        assertEquals("turbine-103", telemetry.getAssetId());
        assertEquals(0.12, telemetry.getVibration(), 0.001);
        assertEquals(65.0, telemetry.getTemperature(), 0.001);
        assertEquals(0.0, telemetry.getPowerOutputMw(), 0.001);
    }

    @Test
    void testDeserializeInvalidJsonThrowsException() {
        String invalidJson = "{ invalid json content }";
        assertThrows(Exception.class, () -> deserializer.map(invalidJson));
    }
}
