package org.aeroguard.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class Telemetry {
    
    @JsonProperty("asset_id")
    private String assetId;
    
    @JsonProperty("sensor_id")
    private String sensorId;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("vibration")
    private double vibration;
    
    @JsonProperty("temperature")
    private double temperature;

    public Telemetry() {}

    public Telemetry(String assetId, String sensorId, Instant timestamp, double vibration, double temperature) {
        this.assetId = assetId;
        this.sensorId = sensorId;
        this.timestamp = timestamp;
        this.vibration = vibration;
        this.temperature = temperature;
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

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public double getVibration() {
        return vibration;
    }

    public void setVibration(double vibration) {
        this.vibration = vibration;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    @Override
    public String toString() {
        return "Telemetry{" +
                "assetId='" + assetId + '\'' +
                ", sensorId='" + sensorId + '\'' +
                ", timestamp=" + timestamp +
                ", vibration=" + vibration +
                ", temperature=" + temperature +
                '}';
    }
}
