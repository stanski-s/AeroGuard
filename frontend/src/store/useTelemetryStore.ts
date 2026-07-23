import { create } from "zustand";
import { CriticalAlert } from "@/types/alert";
import { AssetInfo, OperatingMode, TelemetryPoint } from "@/types/telemetry";
import { INITIAL_ASSETS } from "@/utils/initialAssets";

interface TelemetryState {
  alerts: CriticalAlert[];
  isConnected: boolean;
  assets: AssetInfo[];
  selectedAssetId: string | null;
  telemetryHistory: Record<string, TelemetryPoint[]>;
  isSimulating: boolean;

  // Actions
  setIsConnected: (connected: boolean) => void;
  addAlert: (alert: CriticalAlert) => void;
  dismissAlert: (alertId: string) => void;
  clearAlerts: () => void;
  setSelectedAssetId: (assetId: string | null) => void;
  updateAssetOperatingMode: (assetId: string, mode: OperatingMode) => void;
  addTelemetryPoint: (point: TelemetryPoint) => void;
  addTelemetryBatch: (points: TelemetryPoint[]) => void;
  toggleSimulation: () => void;
  tickSimulatedTelemetryStep: () => void;
}

const MAX_POINTS_PER_ASSET = 60;
const baseTemps: Record<string, number> = {};

export const useTelemetryStore = create<TelemetryState>((set, get) => ({
  alerts: [],
  isConnected: false,
  assets: INITIAL_ASSETS,
  selectedAssetId: "turbine-1",
  telemetryHistory: {},
  isSimulating: true,

  setIsConnected: (connected) => set({ isConnected: connected }),

  addAlert: (alert) =>
    set((state) => {
      // Idempotency check to prevent duplicate alert_ids
      if (state.alerts.some((a) => a.alert_id === alert.alert_id)) {
        return state;
      }
      return { alerts: [alert, ...state.alerts] };
    }),

  dismissAlert: (alertId) =>
    set((state) => ({
      alerts: state.alerts.filter((a) => a.alert_id !== alertId),
    })),

  clearAlerts: () => set({ alerts: [] }),

  setSelectedAssetId: (assetId) => set({ selectedAssetId: assetId }),

  updateAssetOperatingMode: (assetId, mode) =>
    set((state) => ({
      assets: state.assets.map((asset) =>
        asset.id === assetId ? { ...asset, operatingMode: mode } : asset
      ),
    })),

  addTelemetryPoint: (point) =>
    set((state) => {
      const existing = state.telemetryHistory[point.asset_id] || [];
      const updated = [...existing, point].slice(-MAX_POINTS_PER_ASSET);
      return {
        telemetryHistory: {
          ...state.telemetryHistory,
          [point.asset_id]: updated,
        },
      };
    }),

  addTelemetryBatch: (points) =>
    set((state) => {
      const newHistory = { ...state.telemetryHistory };
      for (const point of points) {
        const existing = newHistory[point.asset_id] || [];
        newHistory[point.asset_id] = [...existing, point].slice(-MAX_POINTS_PER_ASSET);
      }
      return { telemetryHistory: newHistory };
    }),

  toggleSimulation: () =>
    set((state) => ({ isSimulating: !state.isSimulating })),

  tickSimulatedTelemetryStep: () => {
    const { assets, isSimulating, alerts } = get();
    if (!isSimulating) return;

    const timeStr = new Date().toISOString();
    const batch: TelemetryPoint[] = [];
    const newAlerts: CriticalAlert[] = [];

    assets.forEach((asset) => {
      let curTemp = baseTemps[asset.id] || 68 + Math.random() * 5;
      curTemp += (Math.random() - 0.49) * 1.5;

      // If asset is in MAINTENANCE_MODE, suppress thermal spikes
      if (asset.operatingMode === "MAINTENANCE_MODE") {
        curTemp = Math.min(curTemp, 72.0);
      }

      curTemp = Math.max(50, Math.min(98, curTemp));
      baseTemps[asset.id] = curTemp;

      const point: TelemetryPoint = {
        asset_id: asset.id,
        sensor_id: `temp-${asset.id}`,
        temperature: parseFloat(curTemp.toFixed(2)),
        vibration: parseFloat((0.12 + Math.random() * 0.25).toFixed(3)),
        timestamp: timeStr,
      };
      batch.push(point);

      // Trigger critical alert if threshold > 80.0°C and NOT in maintenance mode
      if (curTemp > 80.0 && asset.operatingMode !== "MAINTENANCE_MODE") {
        const hasActiveAlert = alerts.some((a) => a.asset_id === asset.id);
        if (!hasActiveAlert) {
          newAlerts.push({
            alert_id: `alert-${Date.now()}-${asset.id}`,
            asset_id: asset.id,
            sensor_id: point.sensor_id,
            alert_type: "THERMAL_SPIKE",
            temperature: parseFloat(curTemp.toFixed(1)),
            threshold: 80.0,
            timestamp: timeStr,
            message: `Critical thermal spike detected on ${asset.name}: ${curTemp.toFixed(1)}°C exceeds threshold 80.0°C`,
          });
        }
      }
    });

    set((state) => {
      const newHistory = { ...state.telemetryHistory };
      for (const point of batch) {
        const existing = newHistory[point.asset_id] || [];
        newHistory[point.asset_id] = [...existing, point].slice(-MAX_POINTS_PER_ASSET);
      }

      const mergedAlerts = [...state.alerts];
      for (const alert of newAlerts) {
        if (!mergedAlerts.some((a) => a.alert_id === alert.alert_id)) {
          mergedAlerts.unshift(alert);
        }
      }

      return {
        telemetryHistory: newHistory,
        alerts: mergedAlerts,
      };
    });
  },
}));
