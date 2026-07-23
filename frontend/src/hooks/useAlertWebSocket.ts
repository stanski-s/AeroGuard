"use client";

import { useEffect, useState, useRef, useCallback } from "react";
import { CriticalAlert } from "@/types/alert";

export function useAlertWebSocket(url: string = "ws://localhost:8080/ws/alerts") {
  const [alerts, setAlerts] = useState<CriticalAlert[]>([]);
  const [isConnected, setIsConnected] = useState<boolean>(false);
  const socketRef = useRef<WebSocket | null>(null);

  const dismissAlert = useCallback((alertId: string) => {
    setAlerts((prev) => prev.filter((a) => a.alert_id !== alertId));
  }, []);

  const addAlert = useCallback((alert: CriticalAlert) => {
    setAlerts((prev) => {
      // Idempotency check: don't duplicate alert with same alert_id
      if (prev.some((a) => a.alert_id === alert.alert_id)) {
        return prev;
      }
      return [alert, ...prev];
    });
  }, []);

  useEffect(() => {
    let ws: WebSocket | null = null;
    let timeoutId: NodeJS.Timeout;

    const connect = () => {
      try {
        ws = new WebSocket(url);
        socketRef.current = ws;

        ws.onopen = () => {
          setIsConnected(true);
          console.log("[WebSocket] Connected to AeroGuard Gateway");
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
        timeoutId = setTimeout(connect, 3000);
      }
    };

    connect();

    return () => {
      clearTimeout(timeoutId);
      if (ws) {
        ws.onclose = null; // Prevent reconnect on unmount
        ws.close();
      }
    };
  }, [url, addAlert]);

  return { alerts, isConnected, dismissAlert, addAlert };
}
