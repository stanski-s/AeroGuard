"use client";

import React, { useState } from "react";
import { useTelemetryStore } from "@/store/useTelemetryStore";
import {
  Wind,
  Cpu,
  Lock,
  Gauge,
  RotateCw,
  Send,
  X,
  ShieldAlert,
} from "lucide-react";

export function AssetDetailPanel() {
  const selectedAssetId = useTelemetryStore((state) => state.selectedAssetId);
  const assets = useTelemetryStore((state) => state.assets);
  const alerts = useTelemetryStore((state) => state.alerts);
  const telemetryHistory = useTelemetryStore((state) => state.telemetryHistory);
  const isConnected = useTelemetryStore((state) => state.isConnected);
  const setSelectedAssetId = useTelemetryStore((state) => state.setSelectedAssetId);
  const updateAssetOperatingMode = useTelemetryStore((state) => state.updateAssetOperatingMode);
  const triggerDiagnosticAction = useTelemetryStore((state) => state.triggerDiagnosticAction);

  const [lastActionStatus, setLastActionStatus] = useState<string | null>(null);

  const selectedAsset = assets.find((a) => a.id === selectedAssetId);
  if (!selectedAsset) return null;

  const activeAlert = alerts.find((a) => a.asset_id === selectedAsset.id);
  const assetHistory = telemetryHistory[selectedAsset.id] || [];
  const latestPoint = assetHistory[assetHistory.length - 1];
  const hasData = Boolean(latestPoint);

  // Physical telemetry parameters from real stream (FIXED MAPPING)
  const powerOutputMw = latestPoint?.powerOutputMw;
  const rotorSpeedRpm = latestPoint?.rotorSpeedRpm;
  const pitchAngleDeg = latestPoint?.pitchAngleDeg;
  const generatorTempC = latestPoint?.temperature;
  const nacelleTempC = latestPoint?.nacelleTempC;
  const vibrationVal = latestPoint?.vibration;
  const vibrationPct = vibrationVal !== undefined ? Math.min(100, Math.round((vibrationVal / 0.50) * 100)) : 0;
  const timestampFormatted = latestPoint?.timestamp ? new Date(latestPoint.timestamp).toLocaleTimeString() : null;

  // Rule-based Diagnostic Engine resolution
  let diagnosisTitle = "Subsystems operating within nominal dynamic thresholds.";
  let diagnosisDetail = "No action required. Continuous telemetry stream active.";
  let priorityLabel = "Nominal";

  if (selectedAsset.operatingMode === "OFFLINE") {
    diagnosisTitle = "Asset Offline";
    diagnosisDetail = "Turbine is shut down. No power generation or active telemetry alerts.";
    priorityLabel = "Offline";
  } else if (!hasData) {
    diagnosisTitle = isConnected
      ? "Awaiting incoming telemetry stream frame from Kafka."
      : "WebSocket Gateway disconnected. Telemetry stream offline.";
    priorityLabel = "No Stream";
  } else if (selectedAsset.operatingMode === "MAINTENANCE_MODE") {
    diagnosisTitle = "Asset in Maintenance Mode";
    diagnosisDetail = "Scheduled testing operations active. Anomaly alerts suppressed.";
    priorityLabel = "Maintenance";
  } else if (activeAlert?.diagnostic_action) {
    diagnosisTitle = `Action: ${activeAlert.diagnostic_action.title}`;
    diagnosisDetail = activeAlert.diagnostic_action.description;
    priorityLabel = `Priority ${activeAlert.diagnostic_action.priority}`;
  } else if (activeAlert) {
    diagnosisTitle = "Thermal Spike Anomaly Detected";
    diagnosisDetail = "Emergency remediation recommended. Select a diagnostic action below.";
    priorityLabel = "Critical";
  } else if (vibrationPct > 70) {
    diagnosisTitle = "Elevated Mechanical Vibration Detected";
    diagnosisDetail = "Drive train vibration threshold exceeded. Check mechanical alignment.";
    priorityLabel = "Warning";
  }

  const handleActionClick = async (action: string) => {
    setLastActionStatus(`Executing ${action}...`);
    await triggerDiagnosticAction(selectedAsset.id, action as any);
    setLastActionStatus(`Command [${action}] dispatched to ${selectedAsset.id}`);
    setTimeout(() => setLastActionStatus(null), 4000);
  };

  return (
    <div className="bg-white/95 backdrop-blur-lg border border-[#c3c6d2] rounded-lg shadow-xl w-[440px] overflow-hidden text-xs">
      {/* Panel Header */}
      <div className="bg-[#eff4ff] px-4 py-3 flex justify-between items-center border-b border-[#c3c6d2]">
        <div className="flex items-center gap-2">
          <Wind className="h-5 w-5 text-[#004080]" />
          <div>
            <span className="font-bold text-sm uppercase tracking-wider text-[#004080] block">
              {selectedAsset.name}
            </span>
            <span className="text-[10px] font-mono text-[#424750] block">
              {selectedAsset.id} • {selectedAsset.clusterName || selectedAsset.locationName}
            </span>
          </div>
        </div>
        <div className="flex items-center gap-2">
          {/* Operating Mode Selector Badge */}
          <select
            value={selectedAsset.operatingMode}
            onChange={(e) => updateAssetOperatingMode(selectedAsset.id, e.target.value as any)}
            className={`text-[10px] font-mono font-bold px-2 py-1 rounded border cursor-pointer outline-none ${
              selectedAsset.operatingMode === "ONLINE"
                ? "bg-emerald-100 text-emerald-800 border-emerald-300"
                : selectedAsset.operatingMode === "MAINTENANCE_MODE"
                ? "bg-amber-100 text-amber-800 border-amber-300"
                : "bg-slate-100 text-slate-700 border-slate-300"
            }`}
          >
            <option value="ONLINE">ONLINE</option>
            <option value="MAINTENANCE_MODE">MAINT</option>
            <option value="OFFLINE">OFFLINE</option>
          </select>
          <button
            onClick={() => setSelectedAssetId(null)}
            className="p-1 hover:bg-[#dce9ff] rounded transition-colors text-[#424750]"
            title="Close details"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      </div>

      {/* Location Subheader & Last Frame Timestamp */}
      <div className="px-4 py-1.5 bg-[#f8f9ff] border-b border-[#c3c6d2]/50 flex justify-between items-center font-mono text-[10px] text-[#424750]">
        <span>Location: <strong className="text-[#002a58]">{selectedAsset.locationName}</strong></span>
        <span>Last Frame: <strong className="text-[#002a58]">{timestampFormatted || "N/A"}</strong></span>
      </div>

      {/* Telemetry Metrics Grid (5 Physical Metrics) */}
      <div className="p-4 grid grid-cols-3 gap-2.5 font-mono">
        <div className="bg-[#eff4ff] p-2.5 border border-[#c3c6d2]/60 rounded">
          <p className="text-[9px] text-[#424750] font-bold uppercase">Power Output</p>
          <p className="text-base text-[#002a58] font-bold mt-0.5">
            {hasData && powerOutputMw !== undefined ? `${powerOutputMw.toFixed(1)} MW` : (
              <span className="text-[10px] text-[#737781] italic">NO STREAM</span>
            )}
          </p>
        </div>

        <div className="bg-[#eff4ff] p-2.5 border border-[#c3c6d2]/60 rounded">
          <p className="text-[9px] text-[#424750] font-bold uppercase">Rotor Speed</p>
          <p className="text-base text-[#002a58] font-bold mt-0.5">
            {hasData && rotorSpeedRpm !== undefined ? `${rotorSpeedRpm.toFixed(1)} RPM` : (
              <span className="text-[10px] text-[#737781] italic">NO STREAM</span>
            )}
          </p>
        </div>

        <div className="bg-[#eff4ff] p-2.5 border border-[#c3c6d2]/60 rounded">
          <p className="text-[9px] text-[#424750] font-bold uppercase">Pitch Angle</p>
          <p className="text-base text-[#002a58] font-bold mt-0.5">
            {hasData && pitchAngleDeg !== undefined ? `${pitchAngleDeg.toFixed(1)}°` : (
              <span className="text-[10px] text-[#737781] italic">NO STREAM</span>
            )}
          </p>
        </div>

        <div className="bg-[#eff4ff] p-2.5 border border-[#c3c6d2]/60 rounded">
          <p className="text-[9px] text-[#424750] font-bold uppercase">Generator Temp</p>
          <p className={`text-base font-bold mt-0.5 ${generatorTempC && generatorTempC > 80 ? "text-[#ba1a1a]" : "text-[#002a58]"}`}>
            {hasData && generatorTempC !== undefined ? `${generatorTempC.toFixed(1)}°C` : (
              <span className="text-[10px] text-[#737781] italic">NO STREAM</span>
            )}
          </p>
        </div>

        <div className="bg-[#eff4ff] p-2.5 border border-[#c3c6d2]/60 rounded col-span-2">
          <p className="text-[9px] text-[#424750] font-bold uppercase">Nacelle Temp</p>
          <p className="text-base text-[#002a58] font-bold mt-0.5">
            {hasData && nacelleTempC !== undefined ? `${nacelleTempC.toFixed(1)}°C` : (
              <span className="text-[10px] text-[#737781] italic">NO STREAM</span>
            )}
          </p>
        </div>
      </div>

      {/* Vibration Threshold Bar */}
      <div className="px-4 pb-3 space-y-1">
        <div className="flex justify-between font-mono text-[10px] font-bold uppercase">
          <span className="text-[#424750]">Vibration Threshold</span>
          <span className={!hasData ? "text-[#737781]" : vibrationPct > 70 ? "text-[#ba1a1a]" : "text-[#006a6a]"}>
            {!hasData ? "NO STREAM" : vibrationPct > 70 ? `EXCEEDED (${vibrationPct}%)` : `NOMINAL (${vibrationPct}%)`}
          </span>
        </div>
        <div className="h-2 w-full bg-[#dce9ff] rounded-full overflow-hidden">
          <div
            className={`h-full transition-all duration-300 ${vibrationPct > 70 ? "bg-[#ba1a1a]" : "bg-[#006a6a]"}`}
            style={{ width: `${hasData ? vibrationPct : 0}%` }}
          />
        </div>
      </div>

      {/* Deterministic Diagnostic Decision Engine Box */}
      <div className="mx-4 mb-4 p-3 bg-[#e6eeff] border border-[#305ea0]/30 rounded-lg">
        <div className="flex items-center justify-between pb-1.5 border-b border-[#305ea0]/20 mb-2">
          <div className="flex items-center gap-1.5 font-bold text-xs text-[#002a58]">
            <ShieldAlert className="h-4 w-4 text-[#ba1a1a]" />
            <span>Diagnostic Decision Engine</span>
          </div>
          <span className="font-mono text-[10px] px-2 py-0.5 rounded bg-[#004080] text-white font-bold">
            {priorityLabel}
          </span>
        </div>

        <p className="font-bold text-[11px] text-[#002a58]">
          {diagnosisTitle}
        </p>
        <p className="text-[11px] text-[#424750] leading-snug mb-3 mt-0.5">
          {diagnosisDetail}
        </p>

        {/* Diagnostic Action Triggers */}
        <div className="grid grid-cols-2 gap-2">
          <button
            onClick={() => handleActionClick("LOCK_BRAKES")}
            className="flex items-center justify-center gap-1 py-2 px-2 bg-[#ba1a1a] hover:bg-[#93000a] text-white font-mono text-[10px] font-bold uppercase rounded transition-all active:scale-95"
            title="Emergency Lock Brakes"
          >
            <Lock className="h-3 w-3" /> Lock Brakes
          </button>

          <button
            onClick={() => handleActionClick("DERATE_POWER")}
            className="flex items-center justify-center gap-1 py-2 px-2 bg-[#004080] hover:bg-[#002a58] text-white font-mono text-[10px] font-bold uppercase rounded transition-all active:scale-95"
            title="De-rate generator output to 70%"
          >
            <Gauge className="h-3 w-3" /> De-rate (70%)
          </button>

          <button
            onClick={() => handleActionClick("RECALIBRATE_PITCH")}
            className="flex items-center justify-center gap-1 py-2 px-2 border border-[#004080] text-[#004080] hover:bg-[#dce9ff] font-mono text-[10px] font-bold uppercase rounded transition-all active:scale-95"
            title="Remote Pitch Calibration"
          >
            <RotateCw className="h-3 w-3" /> Pitch Recal
          </button>

          <button
            onClick={() => handleActionClick("DISPATCH_TECH")}
            className="flex items-center justify-center gap-1 py-2 px-2 border border-[#006a6a] text-[#006a6a] hover:bg-[#90efef]/20 font-mono text-[10px] font-bold uppercase rounded transition-all active:scale-95"
            title="Dispatch offshore technician team"
          >
            <Send className="h-3 w-3" /> Dispatch Tech
          </button>
        </div>

        {lastActionStatus && (
          <p className="mt-2 text-[10px] font-mono text-[#006a6a] font-semibold bg-[#90efef]/20 p-1.5 rounded border border-[#006a6a]/30">
            {lastActionStatus}
          </p>
        )}
      </div>
    </div>
  );
}
