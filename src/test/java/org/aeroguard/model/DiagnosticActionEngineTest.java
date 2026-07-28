package org.aeroguard.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DiagnosticActionEngineTest {

    private Map<String, DiagnosticActionRule> rulesMap;

    @BeforeEach
    public void setUp() {
        rulesMap = new HashMap<>();
    }

    @Test
    public void testResolveSpecificAssetAndAlertTypeRule() {
        DiagnosticAction action = new DiagnosticAction(
                "ACT-001",
                "Lock Brakes Immediately",
                "Engage mechanical rotor lock and pitch blades to 90 degrees.",
                "CRITICAL",
                90,
                "FIELD_OPERATOR"
        );

        DiagnosticActionRule rule = new DiagnosticActionRule(
                "RULE-001",
                "WIND-TURBINE-01",
                "THERMAL_SPIKE",
                "ONLINE",
                action
        );

        rulesMap.put(rule.getRuleId(), rule);

        AssetAlertContext context = AssetAlertContext.of(
                "WIND-TURBINE-01",
                "THERMAL_SPIKE",
                "ONLINE",
                "CRITICAL"
        );

        DiagnosticAction resolved = DiagnosticActionEngine.resolveAction(context, rulesMap.values());

        assertNotNull(resolved);
        assertEquals("ACT-001", resolved.getActionId());
        assertEquals("Lock Brakes Immediately", resolved.getTitle());
    }

    @Test
    public void testResolveHighestPriorityRuleWhenMultipleMatch() {
        DiagnosticAction lowPriorityAction = new DiagnosticAction(
                "ACT-LOW",
                "Monitor Temperature",
                "Log temperature trend every 5 minutes.",
                "WARNING",
                20,
                "OPERATOR"
        );

        DiagnosticAction highPriorityAction = new DiagnosticAction(
                "ACT-HIGH",
                "Emergency Shut Down",
                "Trigger immediate emergency power down.",
                "CRITICAL",
                95,
                "AUTOMATED_SYSTEM"
        );

        DiagnosticActionRule globalRule = new DiagnosticActionRule(
                "RULE-GLOBAL",
                "GLOBAL",
                "THERMAL_SPIKE",
                "ALL",
                lowPriorityAction
        );

        DiagnosticActionRule specificRule = new DiagnosticActionRule(
                "RULE-SPECIFIC",
                "WIND-TURBINE-01",
                "THERMAL_SPIKE",
                "ONLINE",
                highPriorityAction
        );

        rulesMap.put(globalRule.getRuleId(), globalRule);
        rulesMap.put(specificRule.getRuleId(), specificRule);

        AssetAlertContext context = AssetAlertContext.of(
                "WIND-TURBINE-01",
                "THERMAL_SPIKE",
                "ONLINE",
                "CRITICAL"
        );

        DiagnosticAction resolved = DiagnosticActionEngine.resolveAction(context, rulesMap.values());

        assertNotNull(resolved);
        assertEquals("ACT-HIGH", resolved.getActionId());
        assertEquals(95, resolved.getPriority());
    }

    @Test
    public void testFallbackActionWhenNoRuleMatches() {
        AssetAlertContext context = AssetAlertContext.of(
                "UNKNOWN-ASSET",
                "VIBRATION_ANOMALY",
                "ONLINE",
                "CRITICAL"
        );

        DiagnosticAction resolved = DiagnosticActionEngine.resolveAction(context, rulesMap.values());

        assertNotNull(resolved);
        assertTrue(resolved.getActionId().startsWith("FALLBACK-"));
        assertEquals("Dispatch Tech Team & Manual Inspection Required", resolved.getTitle());
        assertTrue(resolved.isFallback());
    }

    @Test
    public void testWarningFallbackAction() {
        AssetAlertContext context = AssetAlertContext.of(
                "UNKNOWN-ASSET",
                "VIBRATION_ANOMALY",
                "ONLINE",
                "WARNING"
        );

        DiagnosticAction resolved = DiagnosticActionEngine.resolveAction(context, rulesMap.values());

        assertNotNull(resolved);
        assertEquals("FALLBACK-WARNING", resolved.getActionId());
        assertEquals("Monitor Sensor Telemetry", resolved.getTitle());
    }
}
