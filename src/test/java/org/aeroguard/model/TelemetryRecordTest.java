package org.aeroguard.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.avro.Schema;
import org.apache.avro.reflect.ReflectData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TelemetryRecordTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testTelemetryRecordExtendedFieldsGettersAndSetters() {
        TelemetryRecord record = new TelemetryRecord();
        record.setAssetId("turbine-1");
        record.setSensorId("temp-turbine-1");
        record.setTimestamp(1700000000000L);
        record.setVibration(0.15);
        record.setTemperature(68.5);
        record.setPowerOutputMw(12.8);
        record.setPitchAngleDeg(4.1);
        record.setRotorSpeedRpm(7.5);
        record.setNacelleTempC(38.0);

        assertEquals("turbine-1", record.getAssetId());
        assertEquals("temp-turbine-1", record.getSensorId());
        assertEquals(1700000000000L, record.getTimestamp());
        assertEquals(0.15, record.getVibration(), 0.0001);
        assertEquals(68.5, record.getTemperature(), 0.0001);
        assertEquals(12.8, record.getPowerOutputMw(), 0.0001);
        assertEquals(4.1, record.getPitchAngleDeg(), 0.0001);
        assertEquals(7.5, record.getRotorSpeedRpm(), 0.0001);
        assertEquals(38.0, record.getNacelleTempC(), 0.0001);
    }

    @Test
    void testFromTelemetryMapsExtendedFields() {
        Instant now = Instant.parse("2026-07-28T10:00:00Z");
        Telemetry telemetry = new Telemetry("turbine-2", "sensor-2", now, 0.2, 70.0, 13.5, 4.5, 7.8, 39.2);

        TelemetryRecord record = TelemetryRecord.fromTelemetry(telemetry);

        assertEquals("turbine-2", record.getAssetId());
        assertEquals("sensor-2", record.getSensorId());
        assertEquals(now.toEpochMilli(), record.getTimestamp());
        assertEquals(0.2, record.getVibration(), 0.0001);
        assertEquals(70.0, record.getTemperature(), 0.0001);
        assertEquals(13.5, record.getPowerOutputMw(), 0.0001);
        assertEquals(4.5, record.getPitchAngleDeg(), 0.0001);
        assertEquals(7.8, record.getRotorSpeedRpm(), 0.0001);
        assertEquals(39.2, record.getNacelleTempC(), 0.0001);
    }

    @Test
    void testJacksonSerializationAndDeserialization() throws Exception {
        TelemetryRecord record = new TelemetryRecord(
                "turbine-3", "sensor-3", 1700000000000L, 0.18, 67.2,
                12.1, 3.9, 7.0, 36.5
        );

        String json = objectMapper.writeValueAsString(record);

        assertTrue(json.contains("\"power_output_mw\":12.1"));
        assertTrue(json.contains("\"pitch_angle_deg\":3.9"));
        assertTrue(json.contains("\"rotor_speed_rpm\":7.0"));
        assertTrue(json.contains("\"nacelle_temp_c\":36.5"));

        TelemetryRecord deserialized = objectMapper.readValue(json, TelemetryRecord.class);
        assertEquals("turbine-3", deserialized.getAssetId());
        assertEquals(12.1, deserialized.getPowerOutputMw(), 0.0001);
        assertEquals(3.9, deserialized.getPitchAngleDeg(), 0.0001);
        assertEquals(7.0, deserialized.getRotorSpeedRpm(), 0.0001);
        assertEquals(36.5, deserialized.getNacelleTempC(), 0.0001);
    }

    @Test
    void testAvroSchemaIncludesExtendedFields() {
        Schema schema = ReflectData.get().getSchema(TelemetryRecord.class);
        assertNotNull(schema);
        assertNotNull(schema.getField("powerOutputMw"));
        assertNotNull(schema.getField("pitchAngleDeg"));
        assertNotNull(schema.getField("rotorSpeedRpm"));
        assertNotNull(schema.getField("nacelleTempC"));
    }
}
