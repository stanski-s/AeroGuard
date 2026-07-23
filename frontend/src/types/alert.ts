export interface CriticalAlert {
  alert_id: string;
  asset_id: string;
  sensor_id: string;
  alert_type: string;
  temperature: number;
  threshold: number;
  timestamp: string;
  message: string;
}
