import { CriticalAlert } from "@/types/alert";

export function generateMockThermalAlert(): CriticalAlert {
  const assetNum = Math.floor(Math.random() * 10) + 1;
  const temp = parseFloat((85 + Math.random() * 20).toFixed(1));
  const timestampStr = new Date().toISOString();
  const alertIdStr = `sim-alert-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`;
  const assetIdStr = `turbine-${assetNum}`;
  const sensorIdStr = `temp-sensor-${assetNum}`;

  return {
    alert_id: alertIdStr,
    asset_id: assetIdStr,
    sensor_id: sensorIdStr,
    alert_type: "THERMAL_SPIKE",
    temperature: temp,
    threshold: 80.0,
    timestamp: timestampStr,
    message: `Thermal spike detected on asset ${assetIdStr}: temperature ${temp}°C breaches threshold 80.0°C`,
  };
}
