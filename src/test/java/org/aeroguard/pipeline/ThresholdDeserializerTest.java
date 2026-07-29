package org.aeroguard.pipeline;

import org.aeroguard.model.ThresholdConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThresholdDeserializerTest {

    private TelemetryPipeline.ThresholdDeserializer deserializer;

    @BeforeEach
    void setUp() throws Exception {
        deserializer = new TelemetryPipeline.ThresholdDeserializer();
        deserializer.open(null);
    }

    @Test
    void testDeserializeSnakeCaseThresholdJson() throws Exception {
        String json = "{\"asset_id\":\"turbine-201\",\"threshold\":75.5,\"alert_type\":\"THERMAL_SPIKE\"}";
        ThresholdConfig config = deserializer.map(json);

        assertNotNull(config);
        assertEquals("turbine-201", config.getAssetId());
        assertEquals(75.5, config.getThreshold(), 0.001);
        assertEquals("THERMAL_SPIKE", config.getAlertType());
    }

    @Test
    void testDeserializeCamelCaseThresholdJson() throws Exception {
        String json = "{\"assetId\":\"turbine-202\",\"threshold\":68.0,\"alertType\":\"VIBRATION_SPIKE\"}";
        ThresholdConfig config = deserializer.map(json);

        assertNotNull(config);
        assertEquals("turbine-202", config.getAssetId());
        assertEquals(68.0, config.getThreshold(), 0.001);
        assertEquals("VIBRATION_SPIKE", config.getAlertType());
    }

    @Test
    void testDeserializeDefaultAlertType() throws Exception {
        String json = "{\"asset_id\":\"turbine-203\",\"threshold\":85.0}";
        ThresholdConfig config = deserializer.map(json);

        assertNotNull(config);
        assertEquals("turbine-203", config.getAssetId());
        assertEquals(85.0, config.getThreshold(), 0.001);
        assertEquals(ThresholdConfig.DEFAULT_ALERT_TYPE, config.getAlertType());
    }

    @Test
    void testDeserializeGlobalAssetThreshold() throws Exception {
        String json = "{\"asset_id\":\"GLOBAL\",\"threshold\":70.0}";
        ThresholdConfig config = deserializer.map(json);

        assertNotNull(config);
        assertEquals("GLOBAL", config.getAssetId());
        assertEquals(70.0, config.getThreshold(), 0.001);
    }

    @Test
    void testDeserializeInvalidJsonThrowsException() {
        assertThrows(Exception.class, () -> deserializer.map("not a json"));
    }
}
