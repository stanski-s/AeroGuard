"use client";

import React, { useEffect } from "react";
import dynamic from "next/dynamic";
import { useAlertWebSocket } from "@/hooks/useAlertWebSocket";
import { useTelemetryStore } from "@/store/useTelemetryStore";
import { AlertToastContainer } from "@/components/AlertToastContainer";
import { TelemetryChart } from "@/components/TelemetryChart";
import { KPICards } from "@/components/KPICards";
import { AssetControlPanel } from "@/components/AssetControlPanel";
import { AlertsTable } from "@/components/AlertsTable";
import { startTelemetrySimulation, stopTelemetrySimulation } from "@/utils/telemetrySimulator";
import { generateMockThermalAlert } from "@/utils/mockAlertGenerator";
import {
  Shield,
  Radio,
  Flame,
  Play,
  Pause,
  RefreshCw,
  Zap,
} from "lucide-react";

// Dynamic import for Mapbox component to prevent SSR window issues
const AssetMap = dynamic(
  () => import("@/components/AssetMap").then((mod) => mod.AssetMap),
  {
    ssr: false,
    loading: () => (
      <div className="w-full h-[520px] rounded-2xl border border-slate-800 bg-slate-950 flex items-center justify-center text-slate-500 font-mono text-xs animate-pulse">
        Initializing Mapbox GL JS Spatial Engine...
      </div>
    ),
  }
);

export default function DashboardPage() {
  const { alerts, isConnected, dismissAlert, addAlert } = useAlertWebSocket();
  const isSimulating = useTelemetryStore((state) => state.isSimulating);
  const toggleSimulation = useTelemetryStore((state) => state.toggleSimulation);

  useEffect(() => {
    startTelemetrySimulation();
    return () => {
      stopTelemetrySimulation();
    };
  }, []);

  const handleSimulateAlert = () => {
    addAlert(generateMockThermalAlert());
  };

  return (
    <main className="min-h-screen bg-slate-950 text-slate-100 p-4 md:p-8 font-sans selection:bg-cyan-500 selection:text-slate-950">
      {/* Background Subtle Gradient Highlights */}
      <div className="fixed inset-0 pointer-events-none z-0 overflow-hidden">
        <div className="absolute -top-40 -left-40 w-96 h-96 bg-cyan-600/10 rounded-full blur-3xl"></div>
        <div className="absolute top-1/3 -right-40 w-96 h-96 bg-rose-600/10 rounded-full blur-3xl"></div>
        <div className="absolute -bottom-40 left-1/3 w-96 h-96 bg-blue-600/10 rounded-full blur-3xl"></div>
      </div>

      <div className="relative z-10 max-w-7xl mx-auto space-y-8">
        {/* Header Bar */}
        <header className="flex flex-col lg:flex-row lg:items-center justify-between gap-5 border-b border-slate-800/80 pb-6">
          <div className="flex items-center gap-4">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-cyan-500/10 border border-cyan-500/30 text-cyan-400 shadow-xl shadow-cyan-500/10">
              <Shield className="h-7 w-7" />
            </div>
            <div>
              <div className="flex items-center gap-3">
                <h1 className="text-3xl font-extrabold tracking-tight text-white">
                  AeroGuard
                </h1>
                <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-cyan-500/20 text-cyan-300 border border-cyan-500/40 font-mono">
                  Command Center UI
                </span>
                <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-rose-500/20 text-rose-400 border border-rose-500/40 font-mono">
                  Premium Dark Mode
                </span>
              </div>
              <p className="text-xs text-slate-400 mt-1">
                Real-Time Offshore Energy &amp; Smart Grid Telemetry Stream Monitor
              </p>
            </div>
          </div>

          {/* Actions & Connection Controls */}
          <div className="flex flex-wrap items-center gap-3">
            {/* WebSocket Connection Status Pill */}
            <div
              className={`flex items-center gap-2 px-3.5 py-2 rounded-xl border text-xs font-mono font-semibold ${
                isConnected
                  ? "bg-emerald-950/60 border-emerald-500/40 text-emerald-400 shadow-lg shadow-emerald-500/10"
                  : "bg-amber-950/60 border-amber-500/40 text-amber-400 animate-pulse"
              }`}
            >
              <Radio className={`h-4 w-4 ${isConnected ? "animate-pulse" : ""}`} />
              <span>{isConnected ? "WS CONNECTED (ws://localhost:8080/ws/alerts)" : "WS RECONNECTING..."}</span>
            </div>

            {/* Simulation Toggle */}
            <button
              onClick={toggleSimulation}
              className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-mono font-semibold border transition-all ${
                isSimulating
                  ? "bg-slate-900 border-cyan-500/50 text-cyan-300 hover:bg-slate-800"
                  : "bg-slate-800 border-slate-700 text-slate-400 hover:bg-slate-700"
              }`}
            >
              {isSimulating ? <Pause className="h-4 w-4 text-cyan-400" /> : <Play className="h-4 w-4 text-slate-400" />}
              <span>Stream: {isSimulating ? "LIVE" : "PAUSED"}</span>
            </button>

            {/* Thermal Spike Trigger Button */}
            <button
              onClick={handleSimulateAlert}
              className="flex items-center gap-2 px-4 py-2 rounded-xl bg-gradient-to-r from-rose-600 to-rose-500 hover:from-rose-500 hover:to-rose-400 text-white font-medium text-xs shadow-lg shadow-rose-600/30 transition-all hover:scale-[1.02] active:scale-[0.98]"
            >
              <Flame className="h-4 w-4" />
              Trigger Thermal Spike
            </button>
          </div>
        </header>

        {/* KPI Metric Cards */}
        <KPICards />

        {/* Main Dual Grid: Mapbox Spatial Grid & ECharts Telemetry Stream */}
        <section className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <AssetMap />
          <TelemetryChart />
        </section>

        {/* Fleet & Operating Mode Manager */}
        <AssetControlPanel />

        {/* Alerts Log Table */}
        <AlertsTable />
      </div>

      {/* Real-time Toast Notifications */}
      <AlertToastContainer alerts={alerts} onDismiss={dismissAlert} />
    </main>
  );
}
