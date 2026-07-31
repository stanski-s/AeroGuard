package org.aeroguard.model;

public enum OperatingMode {
    ONLINE,
    MAINTENANCE_MODE,
    OFFLINE;

    public static boolean isMaintenanceMode(String mode) {
        if (mode == null) {
            return false;
        }
        return MAINTENANCE_MODE.name().equalsIgnoreCase(mode.trim());
    }
}
