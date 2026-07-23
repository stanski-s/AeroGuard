package org.aeroguard.pipeline;

import org.aeroguard.model.AssetOperatingModeEvent;
import org.aeroguard.model.OperatingMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AssetOperatingModeDeserializerTest {

    private TelemetryPipeline.AssetOperatingModeDeserializer deserializer;

    @BeforeEach
    void setUp() throws Exception {
        deserializer = new TelemetryPipeline.AssetOperatingModeDeserializer();
        deserializer.open(null);
    }

    @Test
    void testDeserializeSnakeCaseJson() throws Exception {
        String json = "{\"asset_id\":\"turbine-1\",\"operating_mode\":\"MAINTENANCE_MODE\"}";
        AssetOperatingModeEvent event = deserializer.map(json);

        assertNotNull(event);
        assertEquals("turbine-1", event.getAssetId());
        assertEquals("MAINTENANCE_MODE", event.getOperatingMode());
        assertTrue(OperatingMode.isMaintenanceMode(event.getOperatingMode()));
    }

    @Test
    void testDeserializeCamelCaseJson() throws Exception {
        String json = "{\"assetId\":\"turbine-2\",\"operatingMode\":\"ONLINE\"}";
        AssetOperatingModeEvent event = deserializer.map(json);

        assertNotNull(event);
        assertEquals("turbine-2", event.getAssetId());
        assertEquals("ONLINE", event.getOperatingMode());
        assertFalse(OperatingMode.isMaintenanceMode(event.getOperatingMode()));
    }

    @Test
    void testDeserializeStatusAliasJson() throws Exception {
        String json = "{\"assetId\":\"turbine-3\",\"status\":\"MAINTENANCE_MODE\"}";
        AssetOperatingModeEvent event = deserializer.map(json);

        assertNotNull(event);
        assertEquals("turbine-3", event.getAssetId());
        assertEquals("MAINTENANCE_MODE", event.getOperatingMode());
        assertTrue(OperatingMode.isMaintenanceMode(event.getOperatingMode()));
    }
}
