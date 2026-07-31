"use client";

import React from "react";
import dynamic from "next/dynamic";
import { useTelemetryStream } from "@/hooks/useAlertWebSocket";
import { useTelemetryStore } from "@/store/useTelemetryStore";
import { TelemetryChart } from "@/components/TelemetryChart";
import { KPICards } from "@/components/KPICards";
import { AlertsTable } from "@/components/AlertsTable";
import { AssetDetailPanel } from "@/components/AssetDetailPanel";
import { AlertToastContainer } from "@/components/AlertToastContainer";

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
