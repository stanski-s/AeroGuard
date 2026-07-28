package org.aeroguard.gateway.publisher;

/**
 * Deep domain interface for publishing asset control events and telemetry spikes.
 */
public interface ControlEventPublisher {

    /**
     * Publishes an asset operating mode change event.
     *
     * @param assetId Target asset ID
     * @param mode Target operating mode (e.g., ONLINE, MAINTENANCE_MODE)
     */
    void publishOperatingMode(String assetId, String mode);

    /**
     * Publishes a simulated telemetry thermal spike event.
     *
     * @param assetId Target asset ID
     * @param temperature Temperature value in Celsius
     */
    void publishThermalSpike(String assetId, double temperature);

    /**
     * Publishes a diagnostic action trigger event.
     *
     * @param assetId Target asset ID
     * @param action Diagnostic action name (e.g., LOCK_BRAKES, DERATE_POWER)
     */
    void publishDiagnosticAction(String assetId, String action);
}
