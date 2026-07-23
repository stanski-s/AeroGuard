package org.aeroguard.model;

import java.io.Serializable;

public class TelemetryRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private String assetId;
    private String sensorId;
    private long timestamp;
    private double vibration;
    private double temperature;

    public TelemetryRecord() {}

    public TelemetryRecord(String assetId, String sensorId, long timestamp, double vibration, double temperature) {
        this.assetId = assetId;
        this.sensorId = sensorId;
        this.timestamp = timestamp;
        this.vibration = vibration;
        this.temperature = temperature;
    }

    public static TelemetryRecord fromTelemetry(Telemetry t) {
        return new TelemetryRecord(
                t.getAssetId(),
                t.getSensorId(),
                t.getTimestamp() != null ? t.getTimestamp().toEpochMilli() : 0L,
                t.getVibration(),
                t.getTemperature()
        );
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
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
        return "TelemetryRecord{" +
                "assetId='" + assetId + '\'' +
                ", sensorId='" + sensorId + '\'' +
                ", timestamp=" + timestamp +
                ", vibration=" + vibration +
                ", temperature=" + temperature +
                '}';
    }
}
