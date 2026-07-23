"use client";

import React from "react";
import { useTelemetryStore } from "@/store/useTelemetryStore";
import { AlertOctagon, Activity, Zap, Database } from "lucide-react";
import { countAssetsByOperatingMode } from "@/utils/assetHelpers";

export function KPICards() {
  const alerts = useTelemetryStore((state) => state.alerts);
  const assets = useTelemetryStore((state) => state.assets);

  const criticalCount = alerts.length;
  const { online, maintenance } = countAssetsByOperatingMode(assets);

  return (
    <section className="grid grid-cols-1 md:grid-cols-4 gap-5">
      {/* Active Alerts KPI */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-5 backdrop-blur-md shadow-xl flex flex-col justify-between">
        <div className="flex items-center justify-between text-slate-400">
          <span className="text-xs font-medium uppercase tracking-wider font-mono">Active Anomaly Alerts</span>
          <AlertOctagon className={`h-5 w-5 ${criticalCount > 0 ? "text-rose-500 animate-pulse" : "text-slate-500"}`} />
        </div>
        <div className="mt-3">
          <p className="text-3xl font-bold text-white font-mono">{criticalCount}</p>
          <p className="text-xs text-slate-400 mt-1 flex items-center gap-1">
            {criticalCount > 0 ? (
              <span className="text-rose-400 font-semibold font-mono">Thermal Threshold Breaches (&gt;80°C)</span>
            ) : (
              <span className="text-emerald-400 font-semibold font-mono">All assets operating normally</span>
            )}
          </p>
        </div>
      </div>

      {/* Asset Fleet Status KPI */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-5 backdrop-blur-md shadow-xl flex flex-col justify-between">
        <div className="flex items-center justify-between text-slate-400">
          <span className="text-xs font-medium uppercase tracking-wider font-mono">Monitored Assets</span>
          <Zap className="h-5 w-5 text-amber-400" />
        </div>
        <div className="mt-3">
          <p className="text-3xl font-bold text-white font-mono">{assets.length}</p>
          <p className="text-xs text-slate-400 mt-1 font-mono">
            <span className="text-emerald-400">{online} Online</span> •{" "}
            <span className="text-amber-400">{maintenance} Maintenance</span>
          </p>
        </div>
      </div>

      {/* Stream Engine KPI */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-5 backdrop-blur-md shadow-xl flex flex-col justify-between">
        <div className="flex items-center justify-between text-slate-400">
          <span className="text-xs font-medium uppercase tracking-wider font-mono">Stream Processing Engine</span>
          <Activity className="h-5 w-5 text-blue-400" />
        </div>
        <div className="mt-3">
          <p className="text-xl font-bold text-white font-mono">Apache Flink</p>
          <p className="text-xs text-slate-400 mt-1 font-mono">
            Keyed Process Function • Rolling State
          </p>
        </div>
      </div>

      {/* Gateway & Pipeline KPI */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-5 backdrop-blur-md shadow-xl flex flex-col justify-between">
        <div className="flex items-center justify-between text-slate-400">
          <span className="text-xs font-medium uppercase tracking-wider font-mono">Ingestion Pipeline</span>
          <Database className="h-5 w-5 text-emerald-400" />
        </div>
        <div className="mt-3">
          <p className="text-xl font-bold text-emerald-400 font-mono">10,000+ msg/s</p>
          <p className="text-xs text-slate-400 mt-1 font-mono">
            Kafka: telemetry.raw &amp; alerts.critical
          </p>
        </div>
      </div>
    </section>
  );
}
