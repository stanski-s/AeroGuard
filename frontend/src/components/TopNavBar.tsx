"use client";

import React, { useEffect, useState } from "react";
import { useTelemetryStore } from "@/store/useTelemetryStore";
import { Activity, AlertTriangle, Clock } from "lucide-react";

export function TopNavBar() {
  const alerts = useTelemetryStore((state) => state.alerts);
  const telemetryHistory = useTelemetryStore((state) => state.telemetryHistory);
  const [localTime, setLocalTime] = useState("");

  useEffect(() => {
    const updateClock = () => {
      const now = new Date();
      setLocalTime(now.toLocaleTimeString());
    };
    updateClock();
    const interval = setInterval(updateClock, 1000);
    return () => clearInterval(interval);
  }, []);

  const criticalCount = alerts.length;
  const totalEvents = Object.values(telemetryHistory).reduce((acc, pts) => acc + pts.length, 0);

  return (
    <header className="fixed top-0 right-0 w-[calc(100%-64px)] h-14 bg-white border-b border-[#c3c6d2] flex justify-between items-center px-6 z-40 shadow-xs">
      <div className="flex items-center gap-4">
        <h1 className="font-sans text-xl font-bold text-[#002a58] tracking-tight">AeroGuard</h1>
        <div className="h-4 w-[1px] bg-[#c3c6d2]" />
        <span className="font-mono text-xs font-semibold text-[#424750] uppercase tracking-wider">
          Offshore Smart Grid Control
        </span>
      </div>

      <div className="flex items-center gap-6 font-mono text-xs">
        {/* Stream Ingestion Real Event Counter */}
        <div className="flex items-center gap-1.5 px-2.5 py-1 rounded bg-[#eff4ff] border border-[#c3c6d2] text-[#004080]">
          <Activity className="h-3.5 w-3.5 text-[#006a6a]" />
          <span className="font-bold">{totalEvents} Events</span>
        </div>

        {/* Active Alerts KPI Badge */}
        <div className="flex items-center gap-1.5 px-2.5 py-1 rounded bg-[#eff4ff] border border-[#c3c6d2]">
          <AlertTriangle className={`h-3.5 w-3.5 ${criticalCount > 0 ? "text-[#ba1a1a] animate-pulse" : "text-[#737781]"}`} />
          <span className={`font-bold ${criticalCount > 0 ? "text-[#ba1a1a]" : "text-[#424750]"}`}>
            {criticalCount} Active Alerts
          </span>
        </div>

        {/* System Local Clock */}
        <div className="hidden md:flex items-center gap-1.5 text-[#424750] bg-[#f8f9ff] px-2.5 py-1 rounded border border-[#c3c6d2]">
          <Clock className="h-3.5 w-3.5 text-[#004080]" />
          <span>{localTime || "--:--:--"}</span>
        </div>
      </div>
    </header>
  );
}
