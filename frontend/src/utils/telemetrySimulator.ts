import { useTelemetryStore } from "@/store/useTelemetryStore";
import { TelemetryPoint } from "@/types/telemetry";

let simulationInterval: NodeJS.Timeout | null = null;

export function startTelemetrySimulation() {
  if (simulationInterval) return;

  const store = useTelemetryStore.getState();

  // Pre-fill initial historical points for each asset if empty
  const initialHistory: Record<string, TelemetryPoint[]> = {};
  const now = Date.now();
  store.assets.forEach((asset) => {
    const points: TelemetryPoint[] = [];
    let curTemp = 68 + Math.random() * 5;
    for (let i = 30; i >= 0; i--) {
      const timeStr = new Date(now - i * 2000).toISOString();
      curTemp += (Math.random() - 0.48) * 1.2;
      curTemp = Math.max(55, Math.min(88, curTemp));
      points.push({
        asset_id: asset.id,
        sensor_id: `temp-${asset.id}`,
        temperature: parseFloat(curTemp.toFixed(2)),
        vibration: parseFloat((0.15 + Math.random() * 0.2).toFixed(3)),
        timestamp: timeStr,
      });
    }
    initialHistory[asset.id] = points;
  });

  store.addTelemetryBatch(Object.values(initialHistory).flat());

  // Interval trigger calling domain action on store
  simulationInterval = setInterval(() => {
    useTelemetryStore.getState().tickSimulatedTelemetryStep();
  }, 1500);
}

export function stopTelemetrySimulation() {
  if (simulationInterval) {
    clearInterval(simulationInterval);
    simulationInterval = null;
  }
}
