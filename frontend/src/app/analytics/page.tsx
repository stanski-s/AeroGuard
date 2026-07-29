"use client";

import React, { useMemo } from "react";
import { useTelemetryStore } from "@/store/useTelemetryStore";
import {
  Activity,
  Grid,
  AlertTriangle,
  CheckCircle2,
  WifiOff,
} from "lucide-react";

export default function AnalyticsPage() {
  const alerts = useTelemetryStore((state) => state.alerts);
  const assets = useTelemetryStore((state) => state.assets);
  const isConnected = useTelemetryStore((state) => state.isConnected);
  const telemetryHistory = useTelemetryStore((state) => state.telemetryHistory);

  // Total session telemetry points across all assets
  const totalTelemetryEvents = useMemo(() => {
    return Object.values(telemetryHistory).reduce((sum, points) => sum + points.length, 0);
  }, [telemetryHistory]);

  // Count only assets with real incoming stream telemetry
  const activeStreamingAssetsCount = useMemo(() => {
    return Object.keys(telemetryHistory).filter(
      (id) => telemetryHistory[id] && telemetryHistory[id].length > 0
    ).length;
  }, [telemetryHistory]);

  // Comprehensive physical telemetry & metadata aggregation for every monitored asset
  const fullAssetTelemetryMatrix = useMemo(() => {
    return assets.map((asset) => {
      const history = telemetryHistory[asset.id] || [];
      const latest = history.length > 0 ? history[history.length - 1] : null;
      const hasAlert = alerts.some((a) => a.asset_id === asset.id);

      const hasData = latest !== null && latest !== undefined;
      const modeLabel = hasData
        ? asset.operatingMode
        : isConnected
        ? "NO STREAM"
        : "NO CONNECTION";

      return {
        id: asset.id,
        name: asset.name,
        clusterName: asset.clusterName || "Dogger Bank Alpha",
        locationName: asset.locationName,
        lat: asset.lat,
        lng: asset.lng,
        mode: modeLabel,
        isRealMode: hasData,
        historyCount: history.length,
        latestPower: hasData && latest?.powerOutputMw !== undefined ? `${latest.powerOutputMw.toFixed(1)} MW` : (isConnected ? "NO DATA" : "NO CONNECTION"),
        latestRotor: hasData && latest?.rotorSpeedRpm !== undefined ? `${latest.rotorSpeedRpm.toFixed(1)} RPM` : (isConnected ? "NO DATA" : "NO CONNECTION"),
        latestPitch: hasData && latest?.pitchAngleDeg !== undefined ? `${latest.pitchAngleDeg.toFixed(1)}°` : (isConnected ? "NO DATA" : "NO CONNECTION"),
        latestTemp: hasData && latest?.temperature !== undefined ? `${latest.temperature.toFixed(1)}°C` : (isConnected ? "NO DATA" : "NO CONNECTION"),
        latestNacelleTemp: hasData && latest?.nacelleTempC !== undefined ? `${latest.nacelleTempC.toFixed(1)}°C` : (isConnected ? "NO DATA" : "NO CONNECTION"),
        latestVib: hasData && latest?.vibration !== undefined ? `${latest.vibration.toFixed(3)}g` : (isConnected ? "NO DATA" : "NO CONNECTION"),
        rawTempNum: latest?.temperature,
        lastTime: hasData && latest?.timestamp ? new Date(latest.timestamp).toLocaleTimeString() : (isConnected ? "WAITING FOR STREAM" : "DISCONNECTED"),
        hasAlert,
      };
    });
  }, [assets, telemetryHistory, alerts, isConnected]);

  return (
    <div className="pt-20 pb-16 ml-[64px] px-6 min-h-screen bg-[#f8f9ff] text-[#0d1c2e] space-y-6">
      {/* Page Header */}
      <div className="flex justify-between items-center bg-white p-4 rounded-lg border border-[#c3c6d2] shadow-xs">
        <div>
          <h1 className="text-2xl font-bold font-sans text-[#002a58] tracking-tight">
            Grid &amp; Telemetry Stream Analytics
          </h1>
          <p className="text-xs text-[#424750] font-mono mt-0.5">
            Full-Spectrum Telemetry Matrix &amp; Physical Asset Telemetry Breakdown
          </p>
        </div>

        <div className="flex items-center gap-2">
          <span className={`px-3 py-1.5 rounded font-mono text-xs font-bold flex items-center gap-1.5 border ${
            isConnected
              ? "bg-[#eff4ff] border-[#004080]/30 text-[#004080]"
              : "bg-[#ffdad6]/40 border-[#ba1a1a]/30 text-[#ba1a1a]"
          }`}>
            <span className={`h-2 w-2 rounded-full ${isConnected ? "bg-[#006a6a] animate-pulse" : "bg-[#ba1a1a]"}`} />
            {isConnected ? "WS Stream Connected" : "WS Reconnecting"}
          </span>
        </div>
      </div>

      {/* KPI Header Row */}
      <section className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Total Ingested Events */}
        <div className="bg-white border border-[#c3c6d2] p-6 rounded-lg shadow-sm flex flex-col justify-between">
          <div className="flex justify-between items-center text-[#424750]">
            <span className="font-mono text-xs uppercase font-bold tracking-wider">Stream Session Volume</span>
            <Activity className="h-5 w-5 text-[#004080]" />
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="font-mono text-3xl font-bold text-[#002a58]">{totalTelemetryEvents}</span>
            <span className="font-mono text-xs font-bold text-[#006a6a]">Telemetry Frames</span>
          </div>
          <div className="w-full bg-[#eff4ff] h-1.5 mt-3 rounded overflow-hidden">
            <div
              className="bg-[#004080] h-full transition-all duration-300"
              style={{ width: `${Math.min(100, (totalTelemetryEvents / 300) * 100)}%` }}
            />
          </div>
        </div>

        {/* Monitored Assets Count */}
        <div className="bg-white border border-[#c3c6d2] p-6 rounded-lg shadow-sm flex flex-col justify-between">
          <div className="flex justify-between items-center text-[#424750]">
            <span className="font-mono text-xs uppercase font-bold tracking-wider">Asset Fleet Status</span>
            <Grid className={`h-5 w-5 ${isConnected ? "text-[#006a6a]" : "text-[#ba1a1a]"}`} />
          </div>
          <div className="mt-3 font-mono">
            {isConnected ? (
              <>
                <div className="flex items-baseline gap-2">
                  <span className="text-3xl font-bold text-[#002a58]">{activeStreamingAssetsCount}/{assets.length}</span>
                  <span className="text-xs text-[#006a6a] font-bold">Active Streaming</span>
                </div>
                <div className="flex items-center gap-2 mt-3 text-xs text-[#424750]">
                  <span className="h-2 w-2 rounded-full bg-[#006a6a] animate-pulse" />
                  <span>{activeStreamingAssetsCount} Active Stream Feeds</span>
                </div>
              </>
            ) : (
              <>
                <div className="flex items-center gap-1.5 text-xl font-bold text-[#ba1a1a]">
                  <WifiOff className="h-4 w-4" /> DISCONNECTED
                </div>
                <p className="text-xs text-[#737781] mt-2 italic">
                  No active WebSocket connection
                </p>
              </>
            )}
          </div>
        </div>

        {/* Active Alerts KPI */}
        <div className="bg-white border border-[#c3c6d2] p-6 rounded-lg shadow-sm flex flex-col justify-between">
          <div className="flex justify-between items-center text-[#424750]">
            <span className="font-mono text-xs uppercase font-bold tracking-wider">Active Critical Alerts</span>
            <AlertTriangle className={`h-5 w-5 ${alerts.length > 0 ? "text-[#ba1a1a]" : "text-[#737781]"}`} />
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className={`font-mono text-3xl font-bold ${alerts.length > 0 ? "text-[#ba1a1a]" : "text-[#002a58]"}`}>
              {alerts.length}
            </span>
            <span className="font-mono text-xs font-bold text-[#ba1a1a]">Breaches</span>
          </div>
          <div className="flex items-center gap-2 mt-3 text-xs font-mono text-[#424750]">
            <CheckCircle2 className="h-4 w-4 text-[#006a6a]" />
            <span>Kafka events.status synced</span>
          </div>
        </div>
      </section>

      {/* Full-Spectrum Telemetry Matrix Table */}
      <section className="bg-white border border-[#c3c6d2] rounded-lg p-6 shadow-sm space-y-4">
        <div>
          <h3 className="text-lg font-bold text-[#002a58] tracking-tight font-sans">
            Full-Spectrum Asset Telemetry Matrix
          </h3>
          <p className="text-xs text-[#424750] font-mono">
            Complete real-time breakdown of all turbine physical attributes &amp; location metadata
          </p>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse font-mono text-xs whitespace-nowrap">
            <thead>
              <tr className="border-b border-[#c3c6d2] bg-[#f8f9ff] text-[#424750]">
                <th className="py-2.5 px-3">Asset ID</th>
                <th className="py-2.5 px-3">Unit Name</th>
                <th className="py-2.5 px-3">Cluster</th>
                <th className="py-2.5 px-3">Location</th>
                <th className="py-2.5 px-3">Mode</th>
                <th className="py-2.5 px-3">Power</th>
                <th className="py-2.5 px-3">Rotor</th>
                <th className="py-2.5 px-3">Pitch</th>
                <th className="py-2.5 px-3">Gen Temp</th>
                <th className="py-2.5 px-3">Nacelle Temp</th>
                <th className="py-2.5 px-3">Vibration</th>
                <th className="py-2.5 px-3">Buffer</th>
                <th className="py-2.5 px-3">Last Event</th>
                <th className="py-2.5 px-3">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#c3c6d2]/50">
              {fullAssetTelemetryMatrix.map((item) => (
                <tr key={item.id} className="hover:bg-[#eff4ff]/60 transition-colors">
                  <td className="py-3 px-3 font-bold text-[#002a58]">{item.id}</td>
                  <td className="py-3 px-3 font-semibold">{item.name}</td>
                  <td className="py-3 px-3 text-[#424750]">{item.clusterName}</td>
                  <td className="py-3 px-3 text-[#424750]">{item.locationName}</td>
                  <td className="py-3 px-3">
                    <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                      !item.isRealMode
                        ? "bg-slate-100 text-[#737781] border border-slate-300 italic"
                        : item.mode === "ONLINE"
                        ? "bg-[#90efef]/20 text-[#006a6a] border border-[#006a6a]/30"
                        : item.mode === "MAINTENANCE_MODE"
                        ? "bg-amber-100 text-amber-800 border border-amber-300"
                        : item.mode === "DEGRADED"
                        ? "bg-orange-100 text-orange-800 border border-orange-300"
                        : "bg-slate-100 text-slate-700 border border-slate-300"
                    }`}>
                      {item.mode}
                    </span>
                  </td>
                  <td className={`py-3 px-3 ${item.isRealMode ? "font-bold text-[#002a58]" : "text-[#737781] italic"}`}>
                    {item.latestPower}
                  </td>
                  <td className={`py-3 px-3 ${item.isRealMode ? "" : "text-[#737781] italic"}`}>
                    {item.latestRotor}
                  </td>
                  <td className={`py-3 px-3 ${item.isRealMode ? "" : "text-[#737781] italic"}`}>
                    {item.latestPitch}
                  </td>
                  <td className={`py-3 px-3 ${item.isRealMode ? (item.rawTempNum && item.rawTempNum > 80 ? "font-bold text-[#ba1a1a]" : "font-bold text-[#0d1c2e]") : "text-[#737781] italic"}`}>
                    {item.latestTemp}
                  </td>
                  <td className={`py-3 px-3 ${item.isRealMode ? "" : "text-[#737781] italic"}`}>
                    {item.latestNacelleTemp}
                  </td>
                  <td className={`py-3 px-3 ${item.isRealMode ? "" : "text-[#737781] italic"}`}>
                    {item.latestVib}
                  </td>
                  <td className="py-3 px-3">{item.historyCount}/60</td>
                  <td className="py-3 px-3 text-[#424750]">{item.lastTime}</td>
                  <td className="py-3 px-3">
                    {!item.isRealMode ? (
                      <span className="px-2 py-0.5 rounded bg-slate-100 text-[#737781] border border-slate-300 font-bold text-[10px] flex items-center gap-1 w-fit">
                        <WifiOff className="h-3 w-3" /> NO CONNECTION
                      </span>
                    ) : item.hasAlert || (item.rawTempNum && item.rawTempNum > 80) ? (
                      <span className="px-2 py-0.5 rounded bg-[#ffdad6] text-[#ba1a1a] border border-[#ba1a1a]/40 font-bold text-[10px]">
                        BREACH
                      </span>
                    ) : (
                      <span className="px-2 py-0.5 rounded bg-[#90efef]/20 text-[#006a6a] border border-[#006a6a]/30 font-bold text-[10px]">
                        NOMINAL
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {/* Real-time Alerts Stream Log */}
      {alerts.length > 0 && (
        <section className="bg-white border border-[#ba1a1a]/30 rounded-lg p-[#6] shadow-sm space-y-3">
          <div className="flex items-center gap-2">
            <AlertTriangle className="h-5 w-5 text-[#ba1a1a]" />
            <h3 className="text-lg font-bold text-[#ba1a1a] tracking-tight font-sans">
              Critical Telemetry Alerts Log ({alerts.length})
            </h3>
          </div>
          <div className="space-y-2 font-mono text-xs">
            {alerts.map((alert) => (
              <div key={alert.alert_id} className="p-3 bg-[#ffdad6]/30 border border-[#ba1a1a]/30 rounded flex justify-between items-center">
                <div>
                  <span className="font-bold text-[#ba1a1a] mr-2">[{alert.alert_type}]</span>
                  <span className="text-[#0d1c2e]">{alert.message}</span>
                </div>
                <span className="text-[10px] text-[#424750]">{new Date(alert.timestamp).toLocaleTimeString()}</span>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
