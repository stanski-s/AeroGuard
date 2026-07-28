package org.aeroguard.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public class ConfigEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum ConfigType {
        THRESHOLD,
        DIAGNOSTIC_ACTION
    }

    @JsonProperty("type")
    private ConfigType type;

    @JsonProperty("threshold_config")
    private ThresholdConfig thresholdConfig;

    @JsonProperty("diagnostic_action_rule")
    private DiagnosticActionRule diagnosticActionRule;

    public ConfigEvent() {}

    public static ConfigEvent fromThreshold(ThresholdConfig thresholdConfig) {
        ConfigEvent event = new ConfigEvent();
        event.type = ConfigType.THRESHOLD;
        event.thresholdConfig = thresholdConfig;
        return event;
    }

    public static ConfigEvent fromDiagnosticActionRule(DiagnosticActionRule rule) {
        ConfigEvent event = new ConfigEvent();
        event.type = ConfigType.DIAGNOSTIC_ACTION;
        event.diagnosticActionRule = rule;
        return event;
    }

    public ConfigType getType() {
        return type;
    }

    public void setType(ConfigType type) {
        this.type = type;
    }

    public ThresholdConfig getThresholdConfig() {
        return thresholdConfig;
    }

    public void setThresholdConfig(ThresholdConfig thresholdConfig) {
        this.thresholdConfig = thresholdConfig;
    }

    public DiagnosticActionRule getDiagnosticActionRule() {
        return diagnosticActionRule;
    }

    public void setDiagnosticActionRule(DiagnosticActionRule diagnosticActionRule) {
        this.diagnosticActionRule = diagnosticActionRule;
    }
}
