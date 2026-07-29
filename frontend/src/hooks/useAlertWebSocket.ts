"use client";

import { useEffect, useRef } from "react";
import { CriticalAlert } from "@/types/alert";
import { TelemetryPoint } from "@/types/telemetry";
import { useTelemetryStore } from "@/store/useTelemetryStore";

const DEFAULT_WS_URL = process.env.NEXT_PUBLIC_WS_URL || "ws://localhost:8080/ws/stream";

export function useTelemetryStream(url: string = DEFAULT_WS_URL) {
  const alerts = useTelemetryStore((state) => state.alerts);
  const isConnected = useTelemetryStore((state) => state.isConnected);
  const addAlert = useTelemetryStore((state) => state.addAlert);
  const addTelemetryPoint = useTelemetryStore((state) => state.addTelemetryPoint);
  const updateAssetOperatingMode = useTelemetryStore((state) => state.updateAssetOperatingMode);
  const dismissAlert = useTelemetryStore((state) => state.dismissAlert);
  const setIsConnected = useTelemetryStore((state) => state.setIsConnected);

  const socketRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    let ws: WebSocket | null = null;
    let timeoutId: NodeJS.Timeout;

    const connect = () => {
      try {
        ws = new WebSocket(url);
        socketRef.current = ws;

        ws.onopen = () => {
          setIsConnected(true);
          console.log("[WebSocket Stream] Connected to AeroGuard Gateway:", url);
        };

        ws.onmessage = (event) => {
          try {
            const rawData = JSON.parse(event.data);
            
            // Check if frame is typed frame from /ws/stream
            if (rawData && typeof rawData === "object" && rawData.type) {
              const { type, payload } = rawData;
              if (type === "ALERT" && payload) {
                const alertData: CriticalAlert = payload;
                if (alertData.alert_id || alertData.asset_id) {
                  addAlert(alertData);
                }
              } else if (type === "TELEMETRY" && payload) {
                const point: TelemetryPoint = {
                  asset_id: payload.assetId || payload.asset_id,
                  temperature: payload.temperature,
                  vibration: payload.vibration,
                  powerOutputMw: payload.powerOutputMw ?? payload.power_output_mw,
                  pitchAngleDeg: payload.pitchAngleDeg ?? payload.pitch_angle_deg,
                  rotorSpeedRpm: payload.rotorSpeedRpm ?? payload.rotor_speed_rpm,
                  nacelleTempC: payload.nacelleTempC ?? payload.nacelle_temp_c,
                  timestamp: payload.timestamp || new Date().toISOString(),
                };
                addTelemetryPoint(point);
              } else if (type === "OPERATING_MODE" && payload) {
                if (payload.assetId && payload.operatingMode) {
                  updateAssetOperatingMode(payload.assetId, payload.operatingMode);
                }
              }
            } else if (rawData && (rawData.alert_id || rawData.alertType || rawData.alert_type)) {
              // Backward compatibility for raw alert frames
              addAlert(rawData);
            }
          } catch (e) {
            console.error("[WebSocket Stream] Failed to parse JSON frame:", e);
          }
        };

        ws.onclose = () => {
          setIsConnected(false);
          console.warn("[WebSocket Stream] Connection closed. Reconnecting in 3s...");
          timeoutId = setTimeout(connect, 3000);
        };

        ws.onerror = (error) => {
          console.error("[WebSocket Stream] Error:", error);
          ws?.close();
        };
      } catch (e) {
        console.error("[WebSocket Stream] Connection failed:", e);
        setIsConnected(false);
        timeoutId = setTimeout(connect, 3000);
      }
    };

    connect();

    return () => {
      clearTimeout(timeoutId);
      if (ws) {
        ws.onclose = null;
        ws.close();
      }
    };
  }, [url, addAlert, addTelemetryPoint, updateAssetOperatingMode, setIsConnected]);

  return { alerts, isConnected, dismissAlert, addAlert };
}

// Retain alias for backward compatibility
export const useAlertWebSocket = useTelemetryStream;
