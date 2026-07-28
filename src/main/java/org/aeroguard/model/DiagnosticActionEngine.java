package org.aeroguard.model;

import java.util.Collection;

public class DiagnosticActionEngine {

    public static DiagnosticAction resolveAction(
            Collection<DiagnosticActionRule> rules,
            String assetId,
            String alertType,
            String operatingMode,
            String severity
    ) {
        DiagnosticAction bestAction = null;
        int highestPriority = -1;

        if (rules != null) {
            for (DiagnosticActionRule rule : rules) {
                if (rule != null && rule.matches(assetId, alertType, operatingMode)) {
                    DiagnosticAction action = rule.getAction();
                    if (action != null) {
                        if (action.getPriority() > highestPriority) {
                            highestPriority = action.getPriority();
                            bestAction = action;
                        }
                    }
                }
            }
        }

        if (bestAction != null) {
            return bestAction;
        }

        return createSeverityFallback(severity);
    }

    public static DiagnosticAction createSeverityFallback(String severity) {
        String sev = severity != null ? severity.toUpperCase() : "CRITICAL";
        if ("WARNING".equals(sev)) {
            return new DiagnosticAction(
                    "FALLBACK-WARNING",
                    "Monitor Sensor Telemetry",
                    "Automated fallback: Verify sensor calibration and monitor telemetry trend.",
                    "WARNING",
                    1,
                    "OPERATOR",
                    true
            );
        }

        return new DiagnosticAction(
                "FALLBACK-CRITICAL",
                "Dispatch Tech Team & Manual Inspection Required",
                "Automated fallback: Immediate field dispatch required to inspect asset physical integrity.",
                "CRITICAL",
                1,
                "FIELD_OPERATOR",
                true
        );
    }
}
