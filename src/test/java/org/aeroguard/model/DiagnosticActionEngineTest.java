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

        DiagnosticAction resolved = DiagnosticActionEngine.resolveAction(
                rulesMap.values(),
                "WIND-TURBINE-01",
                "THERMAL_SPIKE",
                "ONLINE",
                "CRITICAL"
        );

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

        DiagnosticAction resolved = DiagnosticActionEngine.resolveAction(
                rulesMap.values(),
                "WIND-TURBINE-01",
                "THERMAL_SPIKE",
                "ONLINE",
                "CRITICAL"
        );

        assertNotNull(resolved);
        assertEquals("ACT-HIGH", resolved.getActionId());
        assertEquals(95, resolved.getPriority());
    }

    @Test
    public void testFallbackActionWhenNoRuleMatches() {
        DiagnosticAction resolved = DiagnosticActionEngine.resolveAction(
                rulesMap.values(),
                "UNKNOWN-ASSET",
                "VIBRATION_ANOMALY",
                "ONLINE",
                "CRITICAL"
        );

        assertNotNull(resolved);
        assertTrue(resolved.getActionId().startsWith("FALLBACK-"));
        assertEquals("Dispatch Tech Team & Manual Inspection Required", resolved.getTitle());
        assertTrue(resolved.isFallback());
    }
}
