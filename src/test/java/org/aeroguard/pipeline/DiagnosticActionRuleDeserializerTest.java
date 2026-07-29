package org.aeroguard.pipeline;

import org.aeroguard.model.DiagnosticActionRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiagnosticActionRuleDeserializerTest {

    private TelemetryPipeline.DiagnosticActionRuleDeserializer deserializer;

    @BeforeEach
    void setUp() throws Exception {
        deserializer = new TelemetryPipeline.DiagnosticActionRuleDeserializer();
        deserializer.open(null);
    }

    @Test
    void testDeserializeSnakeCaseDiagnosticActionRuleJson() throws Exception {
        String json = "{"
                + "\"rule_id\":\"RULE-001\","
                + "\"asset_id\":\"turbine-301\","
                + "\"alert_type\":\"THERMAL_SPIKE\","
                + "\"operating_mode\":\"ONLINE\","
                + "\"action\":{"
                + "  \"action_id\":\"ACT-001\","
                + "  \"title\":\"Lock Brakes Immediately\","
                + "  \"description\":\"Engage mechanical rotor lock.\","
                + "  \"severity\":\"CRITICAL\","
                + "  \"priority\":90,"
                + "  \"target_actor\":\"FIELD_OPERATOR\""
                + "}"
                + "}";

        DiagnosticActionRule rule = deserializer.map(json);

        assertNotNull(rule);
        assertEquals("RULE-001", rule.getRuleId());
        assertEquals("turbine-301", rule.getAssetId());
        assertEquals("THERMAL_SPIKE", rule.getAlertType());
        assertEquals("ONLINE", rule.getOperatingMode());
        assertNotNull(rule.getAction());
        assertEquals("ACT-001", rule.getAction().getActionId());
        assertEquals("Lock Brakes Immediately", rule.getAction().getTitle());
        assertEquals(90, rule.getAction().getPriority());
    }

    @Test
    void testDeserializeCamelCaseDiagnosticActionRuleJson() throws Exception {
        String json = "{"
                + "\"ruleId\":\"RULE-002\","
                + "\"assetId\":\"GLOBAL\","
                + "\"alertType\":\"THERMAL_SPIKE\","
                + "\"operatingMode\":\"ALL\","
                + "\"action\":{"
                + "  \"actionId\":\"ACT-002\","
                + "  \"title\":\"De-rate Power (70%)\","
                + "  \"description\":\"Reduce generator load to prevent overheating.\","
                + "  \"severity\":\"WARNING\","
                + "  \"priority\":50,"
                + "  \"targetActor\":\"AUTOMATED_SYSTEM\""
                + "}"
                + "}";

        DiagnosticActionRule rule = deserializer.map(json);

        assertNotNull(rule);
        assertEquals("RULE-002", rule.getRuleId());
        assertEquals("GLOBAL", rule.getAssetId());
        assertEquals("THERMAL_SPIKE", rule.getAlertType());
        assertEquals("ALL", rule.getOperatingMode());
        assertNotNull(rule.getAction());
        assertEquals("ACT-002", rule.getAction().getActionId());
        assertEquals(50, rule.getAction().getPriority());
    }

    @Test
    void testDeserializeInvalidJsonThrowsException() {
        assertThrows(Exception.class, () -> deserializer.map("invalid json"));
    }
}
