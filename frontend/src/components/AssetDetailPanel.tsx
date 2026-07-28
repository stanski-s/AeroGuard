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
  WifiOff,
} from "lucide-react";
import { getAssetOperatingStatus } from "@/utils/assetHelpers";
import { DiagnosticAction } from "@/types/telemetry";

export function AssetDetailPanel() {
  const selectedAssetId = useTelemetryStore((state) => state.selectedAssetId);
  const assets = useTelemetryStore((state) => state.assets);
  const alerts = useTelemetryStore((state) => state.alerts);
  const telemetryHistory = useTelemetryStore((state) => state.telemetryHistory);
  const isConnected = useTelemetryStore((state) => state.isConnected);
  const setSelectedAssetId = useTelemetryStore((state) => state.setSelectedAssetId);
  const triggerDiagnosticAction = useTelemetryStore((state) => state.triggerDiagnosticAction);

  const [lastActionStatus, setLastActionStatus] = useState<string | null>(null);

  const selectedAsset = assets.find((a) => a.id === selectedAssetId);
  if (!selectedAsset) return null;

  const hasAlert = alerts.some((a) => a.asset_id === selectedAsset.id);
  const assetHistory = telemetryHistory[selectedAsset.id] || [];
  const latestPoint = assetHistory[assetHistory.length - 1];
  const hasData = Boolean(latestPoint);

  // Physical telemetry parameters from real stream
  const powerOutputMw = latestPoint?.powerOutputMw;
  const rotorSpeedRpm = latestPoint?.rotorSpeedRpm;
  const pitchAngleDeg = latestPoint?.pitchAngleDeg;
  const nacelleTempC = latestPoint?.temperature;
  const vibrationVal = latestPoint?.vibration;
  const vibrationPct = vibrationVal !== undefined ? Math.min(100, Math.round((vibrationVal / 0.50) * 100)) : 0;

  // AI Recommendation Logic Engine based on real telemetry
  let aiDiagnosis = "All subsystems operate within nominal tolerances.";
  let aiConfidence = "98%";
  let aiSeverity: "NOMINAL" | "WARNING" | "CRITICAL" = "NOMINAL";
  let recommendedAction: DiagnosticAction = "RECALIBRATE_PITCH";

  if (!hasData) {
    aiSeverity = "WARNING";
    aiDiagnosis = isConnected
      ? "Waiting for incoming telemetry stream frame from Kafka."
      : "WebSocket Gateway disconnected. No telemetry data received.";
    aiConfidence = "0%";
  } else if (hasAlert || (nacelleTempC && nacelleTempC > 80)) {
    aiSeverity = "CRITICAL";
    aiDiagnosis = "Thermal Spike detected in Generator Stator (Cooling Failure / Lubrication Degradation).";
    aiConfidence = "94%";
    recommendedAction = "DERATE_POWER";
  } else if (vibrationPct > 70) {
    aiSeverity = "WARNING";
    aiDiagnosis = "Elevated Mechanical Vibration (Drive Train Misalignment or Bearing Wear).";
    aiConfidence = "88%";
    recommendedAction = "LOCK_BRAKES";
  } else if (selectedAsset.operatingMode === "MAINTENANCE_MODE") {
    aiSeverity = "WARNING";
    aiDiagnosis = "Asset in Maintenance Mode. Anomaly alerts are suppressed.";
    aiConfidence = "100%";
    recommendedAction = "DISPATCH_TECH";
  }

  const handleActionClick = async (action: DiagnosticAction) => {
    setLastActionStatus(`Sending ${action}...`);
    await triggerDiagnosticAction(selectedAsset.id, action);
    setLastActionStatus(`Command [${action}] sent successfully to ${selectedAsset.id}`);
    setTimeout(() => setLastActionStatus(null), 4000);
  };

  return (
    <div className="bg-white/95 backdrop-blur-lg border border-[#c3c6d2] rounded-lg shadow-xl w-[420px] overflow-hidden text-xs">
      {/* Panel Header */}
      <div className="bg-[#eff4ff] px-4 py-3 flex justify-between items-center border-b border-[#c3c6d2]">
        <div className="flex items-center gap-2">
          <Wind className="h-5 w-5 text-[#004080]" />
          <span className="font-bold text-sm uppercase tracking-wider text-[#004080]">
            UNIT {selectedAsset.name}
          </span>
        </div>
        <button
          onClick={() => setSelectedAssetId(null)}
          className="p-1 hover:bg-[#dce9ff] rounded transition-colors text-[#424750]"
          title="Close details"
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      {/* Telemetry Metrics Grid */}
      <div className="p-4 grid grid-cols-2 gap-3 font-mono">
        <div className="bg-[#eff4ff] p-3 border border-[#c3c6d2]/60 rounded">
          <p className="text-[10px] text-[#424750] font-bold uppercase">Power Output</p>
          <p className="text-lg text-[#002a58] font-bold">
            {hasData && powerOutputMw !== undefined ? `${powerOutputMw.toFixed(1)} MW` : (
              <span className="text-xs text-[#737781] italic">NO CONNECTION</span>
            )}
          </p>
        </div>

        <div className="bg-[#eff4ff] p-3 border border-[#c3c6d2]/60 rounded">
          <p className="text-[10px] text-[#424750] font-bold uppercase">Rotor Speed</p>
          <p className="text-lg text-[#002a58] font-bold">
            {hasData && rotorSpeedRpm !== undefined ? `${rotorSpeedRpm.toFixed(1)} RPM` : (
              <span className="text-xs text-[#737781] italic">NO CONNECTION</span>
            )}
          </p>
        </div>

        <div className="bg-[#eff4ff] p-3 border border-[#c3c6d2]/60 rounded">
          <p className="text-[10px] text-[#424750] font-bold uppercase">Pitch Angle</p>
          <p className="text-lg text-[#002a58] font-bold">
            {hasData && pitchAngleDeg !== undefined ? `${pitchAngleDeg.toFixed(1)}°` : (
              <span className="text-xs text-[#737781] italic">NO CONNECTION</span>
            )}
          </p>
        </div>

        <div className="bg-[#eff4ff] p-3 border border-[#c3c6d2]/60 rounded">
          <p className="text-[10px] text-[#424750] font-bold uppercase">Nacelle Temp</p>
          <p className={`text-lg font-bold ${nacelleTempC && nacelleTempC > 80 ? "text-[#ba1a1a]" : "text-[#002a58]"}`}>
            {hasData && nacelleTempC !== undefined ? `${nacelleTempC.toFixed(1)}°C` : (
              <span className="text-xs text-[#737781] italic">NO CONNECTION</span>
            )}
          </p>
        </div>
      </div>

      {/* Vibration Threshold Bar */}
      <div className="px-4 pb-3 space-y-1">
        <div className="flex justify-between font-mono text-[10px] font-bold uppercase">
          <span className="text-[#424750]">Vibration Threshold</span>
          <span className={!hasData ? "text-[#737781]" : vibrationPct > 70 ? "text-[#ba1a1a]" : "text-[#006a6a]"}>
            {!hasData ? "NO CONNECTION" : vibrationPct > 70 ? `EXCEEDED (${vibrationPct}%)` : `NOMINAL (${vibrationPct}%)`}
          </span>
        </div>
        <div className="h-2 w-full bg-[#dce9ff] rounded-full overflow-hidden">
          <div
            className={`h-full transition-all duration-300 ${vibrationPct > 70 ? "bg-[#ba1a1a]" : "bg-[#006a6a]"}`}
            style={{ width: `${hasData ? vibrationPct : 0}%` }}
          />
        </div>
      </div>

      {/* AI Decision Support Box */}
      <div className="mx-4 mb-4 p-3 bg-[#e6eeff] border border-[#305ea0]/30 rounded-lg">
        <div className="flex items-center justify-between pb-1.5 border-b border-[#305ea0]/20 mb-2">
          <div className="flex items-center gap-1.5 font-bold text-xs text-[#002a58]">
            <Cpu className="h-4 w-4 text-[#004080]" />
            <span>AI Decision Support</span>
          </div>
          <span className="font-mono text-[10px] px-2 py-0.5 rounded bg-[#004080] text-white font-bold">
            {aiConfidence} Confidence
          </span>
        </div>

        <p className="text-[11px] text-[#0d1c2e] leading-snug mb-3">
          {aiDiagnosis}
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
