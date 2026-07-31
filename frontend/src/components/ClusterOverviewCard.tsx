"use client";

import React, { useMemo } from "react";
import { useTelemetryStore } from "@/store/useTelemetryStore";
import { Zap, CheckCircle2 } from "lucide-react";
import { countAssetsByOperatingMode } from "@/utils/assetHelpers";

export function ClusterOverviewCard() {
  const assets = useTelemetryStore((state) => state.assets);
  const telemetryHistory = useTelemetryStore((state) => state.telemetryHistory);
  const isConnected = useTelemetryStore((state) => state.isConnected);
  const { online, total } = countAssetsByOperatingMode(assets);

  // Calculate live total MW output based on actual telemetry stream & asset states
  const totalMwOutput = useMemo(() => {
    return assets.reduce((sum, asset) => {
      if (asset.operatingMode === "OFFLINE") return sum;
      const history = telemetryHistory[asset.id];
      const latest = history && history.length > 0 ? history[history.length - 1] : null;
      const power = latest?.powerOutputMw ?? 0;
      return sum + power;
    }, 0).toFixed(1);
  }, [assets, telemetryHistory]);

  // Calculate grid stability percentage dynamically based on online assets ratio
  const gridStability = useMemo(() => {
    if (total === 0) return "100.00";
    return ((online / total) * 100).toFixed(2);
  }, [online, total]);

  // Generate real SVG sparkline path from real telemetry history
  const sparklinePath = useMemo(() => {
    const allPoints = Object.values(telemetryHistory).flat();
    if (allPoints.length < 2) {
      return "M 0 20 L 300 20";
    }
    const recentPoints = allPoints.slice(-25);
    const temps = recentPoints.map((p) => p.temperature);
    const minTemp = Math.min(...temps, 30);
    const maxTemp = Math.max(...temps, 90);
    const range = maxTemp - minTemp || 1;

    return recentPoints
      .map((p, idx) => {
        const x = (idx / (recentPoints.length - 1)) * 300;
        const norm = (p.temperature - minTemp) / range;
        const y = 32 - norm * 24;
        return `${idx === 0 ? "M" : "L"} ${x.toFixed(1)} ${y.toFixed(1)}`;
      })
      .join(" ");
  }, [telemetryHistory]);

  return (
    <div className="bg-white/95 backdrop-blur-md border border-[#c3c6d2] p-4 rounded-lg shadow-md w-80 text-xs">
      <div className="flex justify-between items-start mb-3 pb-2 border-b border-[#c3c6d2]/60">
        <div>
          <h2 className="font-bold text-sm text-[#002a58] flex items-center gap-1.5 font-sans">
            <Zap className="h-4 w-4 text-[#006a6a]" />
            Baltic Offshore Fleet
          </h2>
          <p className="font-mono text-[10px] text-[#424750] uppercase mt-0.5">Pomerania • Słupsk • Gdańsk Bay</p>
        </div>
        <div className={`flex items-center gap-1 px-2 py-0.5 rounded border ${
          isConnected
            ? "bg-[#90efef]/20 border-[#006a6a]/30 text-[#006a6a]"
            : "bg-[#ffdad6]/40 border-[#ba1a1a]/30 text-[#ba1a1a]"
        }`}>
          <span className={`h-2 w-2 rounded-full ${isConnected ? "bg-[#006a6a] animate-pulse" : "bg-[#ba1a1a]"}`} />
          <span className="font-mono text-[10px] font-bold uppercase">
            {isConnected ? "LIVE STREAM" : "OFFLINE"}
          </span>
        </div>
      </div>

      <div className="space-y-2.5 font-mono">
        <div className="flex justify-between items-end border-b border-[#c3c6d2]/40 pb-1.5">
          <span className="text-[10px] text-[#424750] uppercase">Active Turbines</span>
          <span className="text-base text-[#002a58] font-bold">
            {online}<span className="text-xs text-[#737781] ml-0.5">/{total}</span>
          </span>
        </div>

        <div className="flex justify-between items-end border-b border-[#c3c6d2]/40 pb-1.5">
          <span className="text-[10px] text-[#424750] uppercase">Total Output</span>
          <span className="text-base text-[#002a58] font-bold">
            {totalMwOutput} <span className="text-xs font-normal text-[#424750]">MW</span>
          </span>
        </div>
      </div>

      <div className="mt-3">
        <div className="h-10 w-full bg-[#f8f9ff] relative overflow-hidden rounded border border-[#c3c6d2]/60 flex items-center justify-center">
          <svg className="absolute inset-0 w-full h-full" viewBox="0 0 300 40">
            <path
              className="opacity-70"
              d={sparklinePath}
              fill="none"
              stroke="#004080"
              strokeWidth="1.5"
            />
          </svg>
          <span className="font-mono text-[10px] text-[#004080] font-bold z-10 flex items-center gap-1 bg-white/70 px-2 py-0.5 rounded border border-[#c3c6d2]/40">
            <CheckCircle2 className="h-3 w-3 text-[#006a6a]" /> GRID STABILITY: {gridStability}%
          </span>
        </div>
      </div>
    </div>
  );
}
