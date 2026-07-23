export interface TelemetryPoint {
  asset_id: string;
  sensor_id: string;
  temperature: number;
  vibration: number;
  timestamp: string;
}

export type OperatingMode = "ONLINE" | "MAINTENANCE_MODE" | "OFFLINE";

export interface AssetInfo {
  id: string;
  name: string;
  type: "WIND_TURBINE" | "SOLAR_MICROGRID" | "SUBSTATION";
  lat: number;
  lng: number;
  operatingMode: OperatingMode;
  locationName: string;
}
