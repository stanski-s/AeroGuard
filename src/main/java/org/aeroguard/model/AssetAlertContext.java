package org.aeroguard.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable domain context describing an asset anomaly occurrence,
 * passed to diagnostic action resolution engines.
 */
public class AssetAlertContext implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String assetId;
    private final String alertType;
    private final String operatingMode;
    private final String severity;

    public AssetAlertContext(String assetId, String alertType, String operatingMode, String severity) {
        this.assetId = assetId;
        this.alertType = alertType;
        this.operatingMode = operatingMode != null ? operatingMode : "ONLINE";
        this.severity = severity != null ? severity : "CRITICAL";
    }

    public static AssetAlertContext of(String assetId, String alertType, String operatingMode, String severity) {
        return new AssetAlertContext(assetId, alertType, operatingMode, severity);
    }

    public String getAssetId() {
        return assetId;
    }

    public String getAlertType() {
        return alertType;
    }

    public String getOperatingMode() {
        return operatingMode;
    }

    public String getSeverity() {
        return severity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssetAlertContext context = (AssetAlertContext) o;
        return Objects.equals(assetId, context.assetId) &&
               Objects.equals(alertType, context.alertType) &&
               Objects.equals(operatingMode, context.operatingMode) &&
               Objects.equals(severity, context.severity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assetId, alertType, operatingMode, severity);
    }

    @Override
    public String toString() {
        return "AssetAlertContext{" +
                "assetId='" + assetId + '\'' +
                ", alertType='" + alertType + '\'' +
                ", operatingMode='" + operatingMode + '\'' +
                ", severity='" + severity + '\'' +
                '}';
    }
}
