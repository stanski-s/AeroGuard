"use client";

import React from "react";
import { useTelemetryStore } from "@/store/useTelemetryStore";
import { AlertOctagon, Zap, Database, WifiOff } from "lucide-react";

export function KPICards() {
  const alerts = useTelemetryStore((state) => state.alerts);
  const assets = useTelemetryStore((state) => state.assets);
  const isConnected = useTelemetryStore((state) => state.isConnected);
  const telemetryHistory = useTelemetryStore((state) => state.telemetryHistory);

  const criticalCount = alerts.length;
  const totalPoints = Object.values(telemetryHistory).reduce((acc, pts) => acc + pts.length, 0);

  // Count only assets that actually have incoming stream data
  const activeStreamingAssetsCount = Object.keys(telemetryHistory).filter(
    (id) => telemetryHistory[id] && telemetryHistory[id].length > 0
  ).length;

  return (
    <section className="grid grid-cols-1 md:grid-cols-3 gap-5">
      {/* Active Alerts KPI */}
      <div className="bg-white border border-[#c3c6d2] p-5 rounded-lg shadow-sm flex flex-col justify-between">
        <div className="flex items-center justify-between text-[#424750]">
          <span className="text-xs font-bold uppercase tracking-wider font-mono">Active Anomaly Alerts</span>
          <AlertOctagon className={`h-5 w-5 ${criticalCount > 0 ? "text-[#ba1a1a] animate-pulse" : "text-[#737781]"}`} />
        </div>
        <div className="mt-3">
          <p className="text-3xl font-bold text-[#002a58] font-mono">{criticalCount}</p>
          <p className="text-xs text-[#424750] mt-1 flex items-center gap-1">
            {criticalCount > 0 ? (
              <span className="text-[#ba1a1a] font-semibold font-mono">Thermal Threshold Breaches (&gt;80°C)</span>
            ) : (
              <span className="text-[#006a6a] font-semibold font-mono">All assets operating normally</span>
            )}
          </p>
        </div>
      </div>

      {/* Asset Fleet Status KPI */}
      <div className="bg-white border border-[#c3c6d2] p-5 rounded-lg shadow-sm flex flex-col justify-between">
        <div className="flex items-center justify-between text-[#424750]">
          <span className="text-xs font-bold uppercase tracking-wider font-mono">Monitored Assets</span>
          <Zap className={`h-5 w-5 ${isConnected ? "text-[#004080]" : "text-[#ba1a1a]"}`} />
        </div>
        <div className="mt-3">
          {isConnected ? (
            <>
              <p className="text-3xl font-bold text-[#002a58] font-mono">
                {activeStreamingAssetsCount}<span className="text-lg text-[#737781]">/{assets.length}</span>
              </p>
              <p className="text-[11px] text-[#006a6a] font-bold mt-1 font-mono">
                {activeStreamingAssetsCount} Active Stream Feeds
              </p>
            </>
          ) : (
            <>
              <p className="text-xl font-bold text-[#ba1a1a] font-mono flex items-center gap-1.5">
                <WifiOff className="h-4 w-4" /> DISCONNECTED
              </p>
              <p className="text-xs text-[#737781] mt-1 font-mono italic">
                No WebSocket connection to backend
              </p>
            </>
          )}
        </div>
      </div>

      {/* Ingestion Pipeline KPI */}
      <div className="bg-white border border-[#c3c6d2] p-5 rounded-lg shadow-sm flex flex-col justify-between">
        <div className="flex items-center justify-between text-[#424750]">
          <span className="text-xs font-bold uppercase tracking-wider font-mono">Ingestion Pipeline</span>
          <Database className="h-5 w-5 text-[#006a6a]" />
        </div>
        <div className="mt-3">
          <p className={`text-xl font-bold font-mono ${isConnected ? "text-[#006a6a]" : "text-[#ba1a1a]"}`}>
            {isConnected ? "WS Stream Active" : "Disconnected"}
          </p>
          <p className="text-xs text-[#424750] mt-1 font-mono">
            Kafka: telemetry.raw ({totalPoints} recs)
          </p>
        </div>
      </div>
    </section>
  );
}
