export type AlertType = "THERMAL_SPIKE" | "VIBRATION_ANOMALY" | string;

export interface CriticalAlert {
  alert_id: string;
  asset_id: string;
  sensor_id: string;
  alert_type: AlertType;
  temperature: number;
  threshold: number;
  timestamp: string;
  message: string;
}
