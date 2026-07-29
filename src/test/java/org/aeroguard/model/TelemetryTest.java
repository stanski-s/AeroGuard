package org.aeroguard.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TelemetryTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testTelemetryGettersAndSetters() {
        Telemetry telemetry = new Telemetry();
        telemetry.setAssetId("turbine-1");
        Instant now = Instant.now();
        telemetry.setTimestamp(now);
        telemetry.setVibration(0.15);
        telemetry.setTemperature(65.0);
        telemetry.setPowerOutputMw(13.2);
        telemetry.setPitchAngleDeg(4.4);
        telemetry.setRotorSpeedRpm(7.6);
        telemetry.setNacelleTempC(37.8);

        assertEquals("turbine-1", telemetry.getAssetId());
        assertEquals(now, telemetry.getTimestamp());
        assertEquals(0.15, telemetry.getVibration(), 0.0001);
        assertEquals(65.0, telemetry.getTemperature(), 0.0001);
        assertEquals(13.2, telemetry.getPowerOutputMw(), 0.0001);
        assertEquals(4.4, telemetry.getPitchAngleDeg(), 0.0001);
        assertEquals(7.6, telemetry.getRotorSpeedRpm(), 0.0001);
        assertEquals(37.8, telemetry.getNacelleTempC(), 0.0001);
    }

    @Test
    void testJacksonSerializationAndDeserialization() throws Exception {
        Instant timestamp = Instant.parse("2026-07-28T12:00:00Z");
        Telemetry telemetry = new Telemetry(
                "turbine-5", timestamp, 0.22, 71.0,
                14.0, 4.9, 8.1, 41.2
        );

        String json = objectMapper.writeValueAsString(telemetry);

        assertTrue(json.contains("\"asset_id\":\"turbine-5\""));
        assertTrue(json.contains("\"power_output_mw\":14.0"));
        assertTrue(json.contains("\"pitch_angle_deg\":4.9"));
        assertTrue(json.contains("\"rotor_speed_rpm\":8.1"));
        assertTrue(json.contains("\"nacelle_temp_c\":41.2"));

        Telemetry deserialized = objectMapper.readValue(json, Telemetry.class);
        assertEquals("turbine-5", deserialized.getAssetId());
        assertEquals(timestamp, deserialized.getTimestamp());
        assertEquals(14.0, deserialized.getPowerOutputMw(), 0.0001);
        assertEquals(4.9, deserialized.getPitchAngleDeg(), 0.0001);
        assertEquals(8.1, deserialized.getRotorSpeedRpm(), 0.0001);
        assertEquals(41.2, deserialized.getNacelleTempC(), 0.0001);
    }
}
