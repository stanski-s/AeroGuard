package org.aeroguard.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class Alert {

    @JsonProperty("alert_id")
    private String alertId;

    @JsonProperty("asset_id")
    private String assetId;

    @JsonProperty("sensor_id")
    private String sensorId;

    @JsonProperty("alert_type")
    private String alertType;

    @JsonProperty("temperature")
    private double temperature;

    @JsonProperty("threshold")
    private double threshold;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("message")
    private String message;

    public Alert() {}

    public Alert(String alertId, String assetId, String sensorId, String alertType, double temperature, double threshold, Instant timestamp, String message) {
        this.alertId = alertId;
        this.assetId = assetId;
        this.sensorId = sensorId;
        this.alertType = alertType;
        this.temperature = temperature;
        this.threshold = threshold;
        this.timestamp = timestamp;
        this.message = message;
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

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
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

    @Override
    public String toString() {
        return "Alert{" +
                "alertId='" + alertId + '\'' +
                ", assetId='" + assetId + '\'' +
                ", sensorId='" + sensorId + '\'' +
                ", alertType='" + alertType + '\'' +
                ", temperature=" + temperature +
                ", threshold=" + threshold +
                ", timestamp=" + timestamp +
                ", message='" + message + '\'' +
                '}';
    }
}
