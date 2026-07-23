"use client";

import { useEffect, useRef, useCallback } from "react";
import { CriticalAlert } from "@/types/alert";
import { useTelemetryStore } from "@/store/useTelemetryStore";

export function useAlertWebSocket(url: string = "ws://localhost:8080/ws/alerts") {
  const alerts = useTelemetryStore((state) => state.alerts);
  const isConnected = useTelemetryStore((state) => state.isConnected);
  const addAlert = useTelemetryStore((state) => state.addAlert);
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
          console.log("[WebSocket] Connected to AeroGuard Gateway:", url);
        };

        ws.onmessage = (event) => {
          try {
            const data: CriticalAlert = JSON.parse(event.data);
            if (data && data.alert_id) {
              addAlert(data);
            }
          } catch (e) {
            console.error("[WebSocket] Failed to parse alert JSON:", e);
          }
        };

        ws.onclose = () => {
          setIsConnected(false);
          console.warn("[WebSocket] Connection closed. Reconnecting in 3s...");
          timeoutId = setTimeout(connect, 3000);
        };

        ws.onerror = (error) => {
          console.error("[WebSocket] Error:", error);
          ws?.close();
        };
      } catch (e) {
        console.error("[WebSocket] Connection failed:", e);
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
  }, [url, addAlert, setIsConnected]);

  return { alerts, isConnected, dismissAlert, addAlert };
}
