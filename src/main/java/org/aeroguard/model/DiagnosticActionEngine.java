package org.aeroguard.model;

import java.util.Collection;

/**
 * Deep domain resolution engine for evaluating dynamic Diagnostic Action rules
 * and assigning guaranteed remediation workflows to asset anomaly alerts.
 */
public class DiagnosticActionEngine {

    /**
     * Resolves the highest-priority diagnostic action matching the given asset alert context
     * from a collection of active diagnostic rules. Guarantees a non-null return value by
     * falling back to severity-based defaults if no rule matches.
     *
     * @param context The asset anomaly context (assetId, alertType, operatingMode, severity)
     * @param rules Active diagnostic rules to evaluate
     * @return Highest-priority matching DiagnosticAction, or severity fallback if unmatched
     */
    public static DiagnosticAction resolveAction(AssetAlertContext context, Iterable<DiagnosticActionRule> rules) {
        if (context == null) {
            return createSeverityFallback("CRITICAL");
        }

        DiagnosticAction bestAction = null;
        int highestPriority = -1;

        if (rules != null) {
            for (DiagnosticActionRule rule : rules) {
                if (rule != null && rule.matches(context.getAssetId(), context.getAlertType(), context.getOperatingMode())) {
                    DiagnosticAction action = rule.getAction();
                    if (action != null && action.getPriority() > highestPriority) {
                        highestPriority = action.getPriority();
                        bestAction = action;
                    }
                }
            }
        }

        if (bestAction != null) {
            return bestAction;
        }

        return createSeverityFallback(context.getSeverity());
    }

    /**
     * Backward-compatible delegation overload accepting primitive parameters.
     */
    public static DiagnosticAction resolveAction(
            Iterable<DiagnosticActionRule> rules,
            String assetId,
            String alertType,
            String operatingMode,
            String severity
    ) {
        AssetAlertContext context = AssetAlertContext.of(assetId, alertType, operatingMode, severity);
        return resolveAction(context, rules);
    }

    /**
     * Creates a severity-based fallback diagnostic action when no rules match.
     */
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
