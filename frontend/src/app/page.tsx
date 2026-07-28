"use client";

import React, { useEffect } from "react";
import dynamic from "next/dynamic";
import { useTelemetryStream } from "@/hooks/useAlertWebSocket";
import { useTelemetryStore } from "@/store/useTelemetryStore";
import { TelemetryChart } from "@/components/TelemetryChart";
import { KPICards } from "@/components/KPICards";
import { AlertsTable } from "@/components/AlertsTable";
import { AssetDetailPanel } from "@/components/AssetDetailPanel";
import { AlertToastContainer } from "@/components/AlertToastContainer";
import { startTelemetrySimulation, stopTelemetrySimulation } from "@/utils/telemetrySimulator";
import { Flame } from "lucide-react";

// Dynamic import for MapLibre component to prevent SSR window issues
const AssetMap = dynamic(
  () => import("@/components/AssetMap").then((mod) => mod.AssetMap),
  {
    ssr: false,
    loading: () => (
      <div className="w-full h-[520px] rounded-lg border border-[#c3c6d2] bg-white flex items-center justify-center text-[#424750] font-mono text-xs animate-pulse">
        Loading AeroGuard Spatial Map Engine...
      </div>
    ),
  }
 );

export default function FleetMapPage() {
  const { alerts, isConnected, dismissAlert } = useTelemetryStream();
  const selectedAssetId = useTelemetryStore((state) => state.selectedAssetId);
  const triggerThermalSpike = useTelemetryStore((state) => state.triggerThermalSpike);

  useEffect(() => {
    startTelemetrySimulation();
    return () => {
      stopTelemetrySimulation();
    };
  }, []);

  const handleSimulateAlert = () => {
    triggerThermalSpike(selectedAssetId || "BAL-WTG-001", 88.5);
  };

  return (
    <div className="pt-16 pb-16 ml-[64px] min-h-screen bg-[#f8f9ff] text-[#0d1c2e] p-6 space-y-6">
      {/* Top Banner Action Row */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-4 rounded-lg border border-[#c3c6d2] shadow-xs">
        <div>
          <h2 className="text-lg font-bold font-sans text-[#002a58]">
            Offshore Wind Turbine Fleet Control
          </h2>
          <p className="text-xs text-[#424750] font-mono">
            Spatial Field View • Baltic Coast Arrays (West • Central • East)
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={handleSimulateAlert}
            className="flex items-center gap-1.5 px-3.5 py-2 rounded bg-[#ba1a1a] hover:bg-[#93000a] text-white font-mono text-xs font-bold uppercase transition-all shadow-sm active:scale-95"
          >
            <Flame className="h-4 w-4" />
            Trigger Kafka Thermal Spike
          </button>
        </div>
      </div>

      {/* Primary Map Stage with Overlaid Panels */}
      <section className="relative w-full rounded-lg overflow-hidden border border-[#c3c6d2] shadow-md bg-white">
        <AssetMap />

        {/* Floating Left Panel: Selected Asset Telemetry & AI Decision Support */}
        {selectedAssetId && (
          <div className="absolute left-4 top-4 z-20 pointer-events-auto">
            <AssetDetailPanel />
          </div>
        )}
      </section>

      {/* KPI Cards */}
      <KPICards />

      {/* Live Telemetry Stream Chart */}
      <section className="w-full">
        <TelemetryChart />
      </section>

      {/* Live Anomaly Alerts Log */}
      <AlertsTable />

      {/* Real-time Toast Notifications */}
      <AlertToastContainer alerts={alerts} onDismiss={dismissAlert} />
    </div>
  );
}
