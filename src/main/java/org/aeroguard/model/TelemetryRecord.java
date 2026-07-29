package org.aeroguard.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public class TelemetryRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("asset_id")
    private String assetId;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("vibration")
    private double vibration;

    @JsonProperty("temperature")
    private double temperature;

    @JsonProperty("power_output_mw")
    private double powerOutputMw;

    @JsonProperty("pitch_angle_deg")
    private double pitchAngleDeg;

    @JsonProperty("rotor_speed_rpm")
    private double rotorSpeedRpm;

    @JsonProperty("nacelle_temp_c")
    private double nacelleTempC;

    public TelemetryRecord() {}

    public TelemetryRecord(String assetId, long timestamp, double vibration, double temperature) {
        this(assetId, timestamp, vibration, temperature, 0.0, 0.0, 0.0, 0.0);
    }

    public TelemetryRecord(String assetId, long timestamp, double vibration, double temperature,
                           double powerOutputMw, double pitchAngleDeg, double rotorSpeedRpm, double nacelleTempC) {
        this.assetId = assetId;
        this.timestamp = timestamp;
        this.vibration = vibration;
        this.temperature = temperature;
        this.powerOutputMw = powerOutputMw;
        this.pitchAngleDeg = pitchAngleDeg;
        this.rotorSpeedRpm = rotorSpeedRpm;
        this.nacelleTempC = nacelleTempC;
    }

    public static TelemetryRecord fromTelemetry(Telemetry t) {
        return new TelemetryRecord(
                t.getAssetId(),
                t.getTimestamp() != null ? t.getTimestamp().toEpochMilli() : 0L,
                t.getVibration(),
                t.getTemperature(),
                t.getPowerOutputMw(),
                t.getPitchAngleDeg(),
                t.getRotorSpeedRpm(),
                t.getNacelleTempC()
        );
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
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

    @Override
    public String toString() {
        return "TelemetryRecord{" +
                "assetId='" + assetId + '\'' +
                ", timestamp=" + timestamp +
                ", vibration=" + vibration +
                ", temperature=" + temperature +
                ", powerOutputMw=" + powerOutputMw +
                ", pitchAngleDeg=" + pitchAngleDeg +
                ", rotorSpeedRpm=" + rotorSpeedRpm +
                ", nacelleTempC=" + nacelleTempC +
                '}';
    }
}
