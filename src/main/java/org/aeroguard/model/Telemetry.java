package org.aeroguard.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Telemetry {

    @JsonProperty("asset_id")
    @JsonAlias({"assetId", "asset_id"})
    private String assetId;

    @JsonProperty("timestamp")
    private Instant timestamp;

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

    public Telemetry() {}

    public Telemetry(String assetId, Instant timestamp, double vibration, double temperature) {
        this(assetId, timestamp, vibration, temperature, 12.5, 4.2, 7.2, 38.5);
    }

    public Telemetry(String assetId, Instant timestamp, double vibration, double temperature,
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

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
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
        return "Telemetry{" +
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
