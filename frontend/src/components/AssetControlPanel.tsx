"use client";

import React from "react";
import { useTelemetryStore } from "@/store/useTelemetryStore";
import { Wrench, ShieldCheck } from "lucide-react";
import { OperatingMode } from "@/types/telemetry";
import { getAssetOperatingStatus, countAssetsByOperatingMode } from "@/utils/assetHelpers";

export function AssetControlPanel() {
  const assets = useTelemetryStore((state) => state.assets);
  const alerts = useTelemetryStore((state) => state.alerts);
  const selectedAssetId = useTelemetryStore((state) => state.selectedAssetId);
  const setSelectedAssetId = useTelemetryStore((state) => state.setSelectedAssetId);
  const updateAssetOperatingMode = useTelemetryStore((state) => state.updateAssetOperatingMode);

  const { online, maintenance } = countAssetsByOperatingMode(assets);

  const handleToggleMaintenance = (assetId: string, currentMode: OperatingMode) => {
    const nextMode: OperatingMode = currentMode === "MAINTENANCE_MODE" ? "ONLINE" : "MAINTENANCE_MODE";
    updateAssetOperatingMode(assetId, nextMode);
  };

  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900/60 backdrop-blur-md p-6 shadow-xl">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
        <div>
          <h2 className="text-lg font-bold text-white flex items-center gap-2">
            <ShieldCheck className="h-5 w-5 text-cyan-400" />
            Asset Fleet &amp; Operating Mode Manager
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Toggle MAINTENANCE_MODE to suppress alerts during testing operations
          </p>
        </div>

        <div className="flex items-center gap-2 text-xs font-mono text-slate-400">
          <span className="flex items-center gap-1 px-2.5 py-1 rounded bg-slate-800 border border-slate-700">
            <span className="h-2 w-2 rounded-full bg-emerald-400"></span>
            Online: {online}
          </span>
          <span className="flex items-center gap-1 px-2.5 py-1 rounded bg-slate-800 border border-slate-700">
            <span className="h-2 w-2 rounded-full bg-amber-400"></span>
            Maint: {maintenance}
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
              className={`cursor-pointer rounded-xl border p-3.5 transition-all duration-200 flex flex-col justify-between ${
                isSelected
                  ? "bg-slate-800/90 border-cyan-500/80 shadow-lg shadow-cyan-500/10 ring-1 ring-cyan-500/40"
                  : "bg-slate-900/80 border-slate-800 hover:border-slate-700 hover:bg-slate-800/50"
              }`}
            >
              <div>
                <div className="flex items-center justify-between">
                  <span className="font-bold text-xs text-white truncate max-w-[120px]">
                    {asset.name}
                  </span>
                  <span className={`h-2 w-2 rounded-full ${statusInfo.dotColorClass} ${statusInfo.isAlerting ? "animate-ping" : ""}`}></span>
                </div>
                <p className="text-[10px] font-mono text-slate-400 mt-1 truncate">
                  {asset.id} • {asset.locationName}
                </p>
              </div>

              <div className="mt-3 flex items-center justify-between pt-2 border-t border-slate-800/80">
                <span className={`text-[10px] font-mono font-semibold px-2 py-0.5 rounded ${statusInfo.badgeClass}`}>
                  {statusInfo.stateLabel}
                </span>

                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    handleToggleMaintenance(asset.id, asset.operatingMode);
                  }}
                  className={`p-1.5 rounded-lg border text-[10px] font-mono transition-colors ${
                    statusInfo.isMaintenance
                      ? "bg-amber-500/10 hover:bg-amber-500/20 border-amber-500/40 text-amber-300"
                      : "bg-slate-800 hover:bg-slate-700 border-slate-700 text-slate-300"
                  }`}
                  title={statusInfo.isMaintenance ? "Return to ONLINE mode" : "Set to MAINTENANCE_MODE (Suppress Alerts)"}
                >
                  <Wrench className="h-3.5 w-3.5" />
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
