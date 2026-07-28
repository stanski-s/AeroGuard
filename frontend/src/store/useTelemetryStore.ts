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

  // Actions
  setIsConnected: (connected: boolean) => void;
  addAlert: (alert: CriticalAlert) => void;
  dismissAlert: (alertId: string) => void;
  clearAlerts: () => void;
  setSelectedAssetId: (assetId: string | null) => void;
  updateAssetOperatingMode: (assetId: string, mode: OperatingMode) => void;
  addTelemetryPoint: (point: TelemetryPoint) => void;
  addTelemetryBatch: (points: TelemetryPoint[]) => void;
  triggerThermalSpike: (assetId?: string, temperature?: number) => Promise<void>;
  triggerDiagnosticAction: (assetId: string, action: string) => Promise<void>;
}

const MAX_POINTS_PER_ASSET = 60;
const GATEWAY_URL = process.env.NEXT_PUBLIC_GATEWAY_URL || "http://localhost:8080";

export const useTelemetryStore = create<TelemetryState>((set, get) => ({
  alerts: [],
  isConnected: false,
  assets: INITIAL_ASSETS,
  selectedAssetId: "BAL-WTG-001",
  telemetryHistory: {},

  setIsConnected: (connected) => set({ isConnected: connected }),

  addAlert: (alert) =>
    set((state) => {
      // Idempotency check to prevent duplicate alert_ids
      if (state.alerts.some((a) => a.alert_id === alert.alert_id)) {
        return state;
      }

      // Automatically sync telemetry history with alert's spike point to prevent graph/alert mismatch
      const spikePoint: TelemetryPoint = {
        asset_id: alert.asset_id,
        sensor_id: alert.sensor_id || `temp-${alert.asset_id}`,
        temperature: alert.temperature,
        vibration: 0.28,
        powerOutputMw: 12.4,
        pitchAngleDeg: 4.2,
        rotorSpeedRpm: 7.2,
        nacelleTempC: alert.temperature > 80 ? 48.2 : 38.5,
        timestamp: alert.timestamp || new Date().toISOString(),
      };

      const existingHistory = state.telemetryHistory[alert.asset_id] || [];
      const updatedHistory = [...existingHistory, spikePoint].slice(-MAX_POINTS_PER_ASSET);

      return {
        alerts: [alert, ...state.alerts],
        telemetryHistory: {
          ...state.telemetryHistory,
          [alert.asset_id]: updatedHistory,
        },
      };
    }),

  dismissAlert: (alertId) =>
    set((state) => ({
      alerts: state.alerts.filter((a) => a.alert_id !== alertId),
    })),

  clearAlerts: () => set({ alerts: [] }),

  setSelectedAssetId: (assetId) => set({ selectedAssetId: assetId }),

  updateAssetOperatingMode: (assetId, mode) => {
    // 1. Update local state
    set((state) => ({
      assets: state.assets.map((asset) =>
        asset.id === assetId ? { ...asset, operatingMode: mode } : asset
      ),
    }));

    // 2. Publish event to Kafka events.status via Gateway REST API
    fetch(`${GATEWAY_URL}/api/assets/${assetId}/operating-mode?mode=${mode}`, {
      method: "POST",
    })
      .then((res) => res.json())
      .then((data) => {
        console.log(`[Operating Mode Kafka Event] Successfully updated ${assetId} to ${mode}`, data);
      })
      .catch((err) => {
        console.warn(`[Operating Mode Kafka Event] Gateway request failed (using local state fallback):`, err);
      });
  },

  triggerThermalSpike: async (assetId, temperature = 88.5) => {
    const targetAssetId = assetId || get().selectedAssetId || "BAL-WTG-001";
    console.log(`[Thermal Spike Trigger] Triggering ${temperature}°C spike for ${targetAssetId}`);

    try {
      const res = await fetch(
        `${GATEWAY_URL}/api/simulator/spike?assetId=${encodeURIComponent(targetAssetId)}&temperature=${temperature}`,
        { method: "POST" }
      );
      const data = await res.json();
      console.log(`[Thermal Spike Trigger] Response:`, data);
    } catch (err) {
      console.warn(`[Thermal Spike Trigger] Gateway request failed:`, err);
    }
  },

  triggerDiagnosticAction: async (assetId: string, action: string) => {
    console.log(`[Diagnostic Action] Triggering ${action} for ${assetId}`);
    try {
      const res = await fetch(
        `${GATEWAY_URL}/api/assets/${encodeURIComponent(assetId)}/action?action=${encodeURIComponent(action)}`,
        { method: "POST" }
      );
      const data = await res.json();
      console.log(`[Diagnostic Action] Response:`, data);
    } catch (err) {
      console.warn(`[Diagnostic Action] Gateway request failed:`, err);
    }
  },

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
}));
