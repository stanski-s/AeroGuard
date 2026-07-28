export type AlertType = "THERMAL_SPIKE" | "VIBRATION_ANOMALY" | string;

export interface DiagnosticAction {
  action_id: string;
  title: string;
  description: string;
  severity: "CRITICAL" | "WARNING" | string;
  priority: number;
  recommended_role: string;
  is_fallback?: boolean;
}

export interface CriticalAlert {
  alert_id: string;
  asset_id: string;
  sensor_id: string;
  alert_type: AlertType;
  temperature: number;
  threshold: number;
  timestamp: string;
  message: string;
  diagnostic_action?: DiagnosticAction;
}
