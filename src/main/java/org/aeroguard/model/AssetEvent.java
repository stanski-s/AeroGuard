package org.aeroguard.model;

import java.io.Serializable;

public class AssetEvent implements Serializable {

    public enum EventType {
        TELEMETRY,
        OPERATING_MODE
    }

    private EventType type;
    private String assetId;
    private Telemetry telemetry;
    private AssetOperatingModeEvent operatingModeEvent;

    public AssetEvent() {}

    public static AssetEvent fromTelemetry(Telemetry telemetry) {
        AssetEvent event = new AssetEvent();
        event.type = EventType.TELEMETRY;
        event.assetId = telemetry != null ? telemetry.getAssetId() : null;
        event.telemetry = telemetry;
        return event;
    }

    public static AssetEvent fromOperatingMode(AssetOperatingModeEvent operatingModeEvent) {
        AssetEvent event = new AssetEvent();
        event.type = EventType.OPERATING_MODE;
        event.assetId = operatingModeEvent != null ? operatingModeEvent.getAssetId() : null;
        event.operatingModeEvent = operatingModeEvent;
        return event;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public String getAssetId() {
        return assetId != null ? assetId : "";
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public Telemetry getTelemetry() {
        return telemetry;
    }

    public void setTelemetry(Telemetry telemetry) {
        this.telemetry = telemetry;
    }

    public AssetOperatingModeEvent getOperatingModeEvent() {
        return operatingModeEvent;
    }

    public void setOperatingModeEvent(AssetOperatingModeEvent operatingModeEvent) {
        this.operatingModeEvent = operatingModeEvent;
    }

    @Override
    public String toString() {
        return "AssetEvent{" +
                "type=" + type +
                ", assetId='" + assetId + '\'' +
                ", telemetry=" + telemetry +
                ", operatingModeEvent=" + operatingModeEvent +
                '}';
    }
}
