package org.aeroguard.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ThresholdConfig {

    public static final String DEFAULT_ALERT_TYPE = "THERMAL_SPIKE";

    @JsonProperty("asset_id")
    @JsonAlias({"assetId", "asset_id"})
    private String assetId;

    @JsonProperty("threshold")
    private double threshold;

    @JsonProperty("alert_type")
    @JsonAlias({"alertType", "alert_type"})
    private String alertType = DEFAULT_ALERT_TYPE;

    public ThresholdConfig() {}

    public ThresholdConfig(String assetId, double threshold) {
        this(assetId, threshold, DEFAULT_ALERT_TYPE);
    }

    public ThresholdConfig(String assetId, double threshold, String alertType) {
        this.assetId = assetId;
        this.threshold = threshold;
        this.alertType = alertType != null ? alertType : DEFAULT_ALERT_TYPE;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    @Override
    public String toString() {
        return "ThresholdConfig{" +
                "assetId='" + assetId + '\'' +
                ", threshold=" + threshold +
                ", alertType='" + alertType + '\'' +
                '}';
    }
}
