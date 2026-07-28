package org.aeroguard.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public class DiagnosticActionRule implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String GLOBAL_KEY = "GLOBAL";

    @JsonProperty("rule_id")
    private String ruleId;

    @JsonProperty("asset_id")
    private String assetId;

    @JsonProperty("alert_type")
    private String alertType;

    @JsonProperty("operating_mode")
    private String operatingMode;

    @JsonProperty("action")
    private DiagnosticAction action;

    public DiagnosticActionRule() {}

    public DiagnosticActionRule(String ruleId, String assetId, String alertType, String operatingMode, DiagnosticAction action) {
        this.ruleId = ruleId;
        this.assetId = assetId;
        this.alertType = alertType;
        this.operatingMode = operatingMode;
        this.action = action;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getOperatingMode() {
        return operatingMode;
    }

    public void setOperatingMode(String operatingMode) {
        this.operatingMode = operatingMode;
    }

    public DiagnosticAction getAction() {
        return action;
    }

    public void setAction(DiagnosticAction action) {
        this.action = action;
    }

    public boolean matches(String targetAssetId, String targetAlertType, String targetOperatingMode) {
        boolean assetMatches = isWildcardOrMatch(assetId, targetAssetId);
        boolean alertTypeMatches = isWildcardOrMatch(alertType, targetAlertType);
        boolean modeMatches = isWildcardOrMatch(operatingMode, targetOperatingMode);
        return assetMatches && alertTypeMatches && modeMatches;
    }

    private static boolean isWildcardOrMatch(String ruleVal, String targetVal) {
        if (ruleVal == null || ruleVal.trim().isEmpty() || GLOBAL_KEY.equalsIgnoreCase(ruleVal.trim())) {
            return true;
        }
        return targetVal != null && ruleVal.trim().equalsIgnoreCase(targetVal.trim());
    }
}
