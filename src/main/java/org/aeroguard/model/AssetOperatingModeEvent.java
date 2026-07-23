package org.aeroguard.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class AssetOperatingModeEvent {

    @JsonProperty("asset_id")
    @JsonAlias({"assetId"})
    private String assetId;

    @JsonProperty("operating_mode")
    @JsonAlias({"operatingMode", "status", "mode"})
    private String operatingMode;

    @JsonProperty("timestamp")
    private Instant timestamp;

    public AssetOperatingModeEvent() {}

    public AssetOperatingModeEvent(String assetId, String operatingMode) {
        this(assetId, operatingMode, Instant.now());
    }

    public AssetOperatingModeEvent(String assetId, String operatingMode, Instant timestamp) {
        this.assetId = assetId;
        this.operatingMode = operatingMode;
        this.timestamp = timestamp;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getOperatingMode() {
        return operatingMode;
    }

    public void setOperatingMode(String operatingMode) {
        this.operatingMode = operatingMode;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "AssetOperatingModeEvent{" +
                "assetId='" + assetId + '\'' +
                ", operatingMode='" + operatingMode + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
