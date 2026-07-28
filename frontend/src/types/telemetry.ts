export interface TelemetryPoint {
  asset_id: string;
  sensor_id: string;
  temperature: number;
  vibration: number;
  powerOutputMw?: number;
  pitchAngleDeg?: number;
  rotorSpeedRpm?: number;
  nacelleTempC?: number;
  timestamp: string;
}

export type OperatingMode = "ONLINE" | "MAINTENANCE_MODE" | "DEGRADED" | "OFFLINE";
export type DiagnosticAction = "LOCK_BRAKES" | "DERATE_POWER" | "RECALIBRATE_PITCH" | "DISPATCH_TECH";

export interface AssetInfo {
  id: string;
  name: string;
  type: "WIND_TURBINE" | "SOLAR_MICROGRID" | "SUBSTATION";
  lat: number;
  lng: number;
  operatingMode: OperatingMode;
  locationName: string;
  clusterName?: string;
  powerOutputMw?: number;
  pitchAngleDeg?: number;
  rotorSpeedRpm?: number;
  nacelleTempC?: number;
}
