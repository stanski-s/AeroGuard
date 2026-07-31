"use client";

import React from "react";
import { useTelemetryStore } from "@/store/useTelemetryStore";
import { ShieldCheck } from "lucide-react";
import { OperatingMode } from "@/types/telemetry";
import { getAssetOperatingStatus, countAssetsByOperatingMode } from "@/utils/assetHelpers";

const OPERATING_MODES: { value: OperatingMode; label: string }[] = [
  { value: "ONLINE", label: "ONLINE" },
  { value: "MAINTENANCE_MODE", label: "MAINT" },
  { value: "OFFLINE", label: "OFFLINE" },
];

export function AssetControlPanel() {
  const assets = useTelemetryStore((state) => state.assets);
  const alerts = useTelemetryStore((state) => state.alerts);
  const selectedAssetId = useTelemetryStore((state) => state.selectedAssetId);
  const setSelectedAssetId = useTelemetryStore((state) => state.setSelectedAssetId);
  const updateAssetOperatingMode = useTelemetryStore((state) => state.updateAssetOperatingMode);

  const { online, maintenance, offline } = countAssetsByOperatingMode(assets);

  const handleModeChange = (assetId: string, mode: OperatingMode) => {
    updateAssetOperatingMode(assetId, mode);
  };

  return (
    <div className="rounded-lg border border-[#c3c6d2] bg-white p-6 shadow-xs">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-[#c3c6d2]">
        <div>
          <h2 className="text-lg font-bold text-[#002a58] flex items-center gap-2">
            <ShieldCheck className="h-5 w-5 text-[#004080]" />
            Asset Fleet &amp; Operating Mode Manager
          </h2>
          <p className="text-xs text-[#424750] mt-0.5 font-mono">
            Set Operating Mode per asset to dictate Flink anomaly suppression rules
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2 text-xs font-mono text-[#424750]">
          <span className="flex items-center gap-1 px-2.5 py-1 rounded bg-[#eff4ff] border border-[#c3c6d2]">
            <span className="h-2 w-2 rounded-full bg-[#006a6a]"></span>
            Online: {online}
          </span>
          <span className="flex items-center gap-1 px-2.5 py-1 rounded bg-[#eff4ff] border border-[#c3c6d2]">
            <span className="h-2 w-2 rounded-full bg-amber-500"></span>
            Maint: {maintenance}
          </span>
          <span className="flex items-center gap-1 px-2.5 py-1 rounded bg-[#eff4ff] border border-[#c3c6d2]">
            <span className="h-2 w-2 rounded-full bg-[#737781]"></span>
            Offline: {offline}
          </span>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-3 mt-4">
        {assets.map((asset) => {
          const hasAlert = alerts.some((a) => a.asset_id === asset.id);
          const isSelected = selectedAssetId === asset.id;
          const statusInfo = getAssetOperatingStatus(asset, hasAlert);

          return (
            <div
              key={asset.id}
              onClick={() => setSelectedAssetId(asset.id)}
              className={`cursor-pointer rounded-lg border p-3.5 transition-all flex flex-col justify-between ${
                isSelected
                  ? "bg-[#eff4ff] border-[#004080] shadow-sm ring-1 ring-[#004080]"
                  : "bg-[#f8f9ff] border-[#c3c6d2] hover:border-[#004080] hover:bg-white"
              }`}
            >
              <div>
                <div className="flex items-center justify-between">
                  <span className="font-bold text-xs text-[#002a58] truncate max-w-[120px]">
                    {asset.name}
                  </span>
                  <span className={`h-2 w-2 rounded-full ${statusInfo.dotColorClass} ${statusInfo.isAlerting ? "animate-ping" : ""}`}></span>
                </div>
                <p className="text-[10px] font-mono text-[#424750] mt-1 truncate">
                  {asset.id} • {asset.locationName}
                </p>
              </div>

              <div className="mt-3 flex items-center justify-between gap-1 pt-2 border-t border-[#c3c6d2]">
                <span className={`text-[10px] font-mono font-semibold px-2 py-0.5 rounded ${statusInfo.badgeClass}`}>
                  {statusInfo.stateLabel}
                </span>

                <select
                  value={asset.operatingMode}
                  onClick={(e) => e.stopPropagation()}
                  onChange={(e) => {
                    e.stopPropagation();
                    handleModeChange(asset.id, e.target.value as OperatingMode);
                  }}
                  className="bg-white border border-[#c3c6d2] rounded px-1.5 py-0.5 text-[10px] font-mono text-[#002a58] focus:outline-none focus:border-[#004080] cursor-pointer"
                  title="Change turbine Operating Mode"
                >
                  {OPERATING_MODES.map((mode) => (
                    <option key={mode.value} value={mode.value} className="bg-white text-[#0d1c2e]">
                      {mode.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
