package org.aeroguard.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Alert {

    @JsonProperty("alert_id")
    @JsonAlias({"alertId", "alert_id"})
    private String alertId;

    @JsonProperty("asset_id")
    @JsonAlias({"assetId", "asset_id"})
    private String assetId;

    @JsonProperty("alert_type")
    @JsonAlias({"alertType", "alert_type"})
    private String alertType;

    @JsonProperty("trigger_value")
    @JsonAlias({"triggerValue", "trigger_value", "temperature", "value"})
    private double triggerValue;

    @JsonProperty("threshold")
    private double threshold;

    @JsonProperty("vibration")
    private double vibration;

    @JsonProperty("temperature")
    private double temperature;

    @JsonProperty("power_output_mw")
    @JsonAlias({"powerOutputMw", "power_output_mw"})
    private double powerOutputMw;

    @JsonProperty("pitch_angle_deg")
    @JsonAlias({"pitchAngleDeg", "pitch_angle_deg"})
    private double pitchAngleDeg;

    @JsonProperty("rotor_speed_rpm")
    @JsonAlias({"rotorSpeedRpm", "rotor_speed_rpm"})
    private double rotorSpeedRpm;

    @JsonProperty("nacelle_temp_c")
    @JsonAlias({"nacelleTempC", "nacelle_temp_c"})
    private double nacelleTempC;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("message")
    private String message;

    @JsonProperty("diagnostic_action")
    @JsonAlias({"diagnosticAction", "diagnostic_action"})
    private DiagnosticAction diagnosticAction;

    public Alert() {}

    public Alert(String alertId, String assetId, String alertType, double temperature, double threshold, Instant timestamp, String message) {
        this(alertId, assetId, alertType, temperature, threshold, timestamp, message, null);
    }

    public Alert(String alertId, String assetId, String alertType, double temperature, double threshold, Instant timestamp, String message, DiagnosticAction diagnosticAction) {
        this.alertId = alertId;
        this.assetId = assetId;
        this.alertType = alertType;
        this.triggerValue = temperature;
        this.temperature = temperature;
        this.threshold = threshold;
        this.timestamp = timestamp;
        this.message = message;
        this.diagnosticAction = diagnosticAction;
    }

    public String getAlertId() {
        return alertId;
    }

    public void setAlertId(String alertId) {
        this.alertId = alertId;
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

    public double getTriggerValue() {
        return triggerValue != 0 ? triggerValue : temperature;
    }

    public void setTriggerValue(double triggerValue) {
        this.triggerValue = triggerValue;
        if (this.temperature == 0) {
            this.temperature = triggerValue;
        }
    }

    public double getTemperature() {
        return temperature != 0 ? temperature : triggerValue;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
        if (this.triggerValue == 0) {
            this.triggerValue = temperature;
        }
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public double getVibration() {
        return vibration;
    }

    public void setVibration(double vibration) {
        this.vibration = vibration;
    }

    public double getPowerOutputMw() {
        return powerOutputMw;
    }

    public void setPowerOutputMw(double powerOutputMw) {
        this.powerOutputMw = powerOutputMw;
    }

    public double getPitchAngleDeg() {
        return pitchAngleDeg;
    }

    public void setPitchAngleDeg(double pitchAngleDeg) {
        this.pitchAngleDeg = pitchAngleDeg;
    }

    public double getRotorSpeedRpm() {
        return rotorSpeedRpm;
    }

    public void setRotorSpeedRpm(double rotorSpeedRpm) {
        this.rotorSpeedRpm = rotorSpeedRpm;
    }

    public double getNacelleTempC() {
        return nacelleTempC;
    }

    public void setNacelleTempC(double nacelleTempC) {
        this.nacelleTempC = nacelleTempC;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public DiagnosticAction getDiagnosticAction() {
        return diagnosticAction;
    }

    public void setDiagnosticAction(DiagnosticAction diagnosticAction) {
        this.diagnosticAction = diagnosticAction;
    }

    @Override
    public String toString() {
        return "Alert{" +
                "alertId='" + alertId + '\'' +
                ", assetId='" + assetId + '\'' +
                ", alertType='" + alertType + '\'' +
                ", triggerValue=" + getTriggerValue() +
                ", threshold=" + threshold +
                ", timestamp=" + timestamp +
                ", message='" + message + '\'' +
                '}';
    }
}
