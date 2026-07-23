"use client";

import React from "react";
import { useAlertWebSocket } from "@/hooks/useAlertWebSocket";
import { AlertToastContainer } from "@/components/AlertToastContainer";
import { Shield, Activity, Radio, AlertOctagon, Flame, CheckCircle, Trash2 } from "lucide-react";
import { formatTemperature, formatTimestamp } from "@/utils/formatters";
import { generateMockThermalAlert } from "@/utils/mockAlertGenerator";

export default function DashboardPage() {
  const { alerts, isConnected, dismissAlert, addAlert } = useAlertWebSocket();

  const handleSimulateAlert = () => {
    addAlert(generateMockThermalAlert());
  };

  return (
    <main className="min-h-screen bg-slate-950 text-slate-100 p-6 md:p-10 font-sans">
      {/* Header */}
      <header className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-800 pb-6">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-500 shadow-lg shadow-rose-500/10">
            <Shield className="h-6 w-6" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
              AeroGuard <span className="text-xs font-semibold px-2 py-0.5 rounded bg-rose-500/20 text-rose-400 border border-rose-500/30">Hot Path Alerting</span>
            </h1>
            <p className="text-xs text-slate-400 mt-0.5">
              Real-time stream telemetry & anomaly monitoring system
            </p>
          </div>
        </div>

        {/* WebSocket Connection Status */}
        <div className="flex items-center gap-4">
          <div className={`flex items-center gap-2 px-3 py-1.5 rounded-full border text-xs font-mono font-medium ${
            isConnected
              ? "bg-emerald-950/60 border-emerald-500/40 text-emerald-400"
              : "bg-amber-950/60 border-amber-500/40 text-amber-400 animate-pulse"
          }`}>
            <Radio className={`h-3.5 w-3.5 ${isConnected ? "animate-pulse" : ""}`} />
            <span>{isConnected ? "WS CONNECTED (ws://localhost:8080/ws/alerts)" : "WS RECONNECTING..."}</span>
          </div>

          <button
            onClick={handleSimulateAlert}
            className="flex items-center gap-2 px-4 py-2 rounded-lg bg-rose-600 hover:bg-rose-500 text-white font-medium text-xs shadow-lg shadow-rose-600/30 transition-all hover:scale-[1.02] active:scale-[0.98]"
          >
            <Flame className="h-4 w-4" />
            Simulate Thermal Spike
          </button>
        </div>
      </header>

      {/* KPI Stats */}
      <section className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-8">
        <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-5 backdrop-blur-sm">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs font-medium uppercase tracking-wider">Active Critical Alerts</span>
            <AlertOctagon className="h-5 w-5 text-rose-500" />
          </div>
          <p className="text-3xl font-bold text-white mt-2 font-mono">{alerts.length}</p>
          <p className="text-xs text-slate-400 mt-1">Real-time WebSocket alerts</p>
        </div>

        <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-5 backdrop-blur-sm">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs font-medium uppercase tracking-wider">Processing Engine</span>
            <Activity className="h-5 w-5 text-blue-400" />
          </div>
          <p className="text-lg font-bold text-white mt-2 font-mono">Apache Flink</p>
          <p className="text-xs text-slate-400 mt-1">Keyed State Rolling Average</p>
        </div>

        <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-5 backdrop-blur-sm">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs font-medium uppercase tracking-wider">Pipeline Health</span>
            <CheckCircle className="h-5 w-5 text-emerald-400" />
          </div>
          <p className="text-lg font-bold text-emerald-400 mt-2 font-mono">HEALTHY</p>
          <p className="text-xs text-slate-400 mt-1">Kafka topic: alerts.critical</p>
        </div>
      </section>

      {/* Alerts Feed */}
      <section className="mt-8">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-bold text-white flex items-center gap-2">
            Critical Alerts Log
            {alerts.length > 0 && (
              <span className="rounded-full bg-rose-500/20 px-2.5 py-0.5 text-xs text-rose-400 font-mono border border-rose-500/30">
                {alerts.length}
              </span>
            )}
          </h2>
        </div>

        <div className="rounded-xl border border-slate-800 bg-slate-900/60 overflow-hidden shadow-xl">
          {alerts.length === 0 ? (
            <div className="p-12 text-center text-slate-500">
              <Shield className="h-10 w-10 mx-auto mb-3 text-slate-700 opacity-60" />
              <p className="text-sm font-medium">No critical alerts detected</p>
              <p className="text-xs mt-1 text-slate-600">
                System is monitoring generator temperatures in real-time.
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm text-slate-300">
                <thead className="bg-slate-900 text-xs font-mono uppercase text-slate-400 border-b border-slate-800">
                  <tr>
                    <th className="px-6 py-3.5">Alert ID</th>
                    <th className="px-6 py-3.5">Asset ID</th>
                    <th className="px-6 py-3.5">Sensor ID</th>
                    <th className="px-6 py-3.5">Type</th>
                    <th className="px-6 py-3.5">Temperature</th>
                    <th className="px-6 py-3.5">Threshold</th>
                    <th className="px-6 py-3.5">Timestamp</th>
                    <th className="px-6 py-3.5 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/80 font-mono text-xs">
                  {alerts.map((alert) => (
                    <tr key={alert.alert_id} className="hover:bg-slate-800/50 transition-colors">
                      <td className="px-6 py-4 font-semibold text-slate-400 truncate max-w-[140px]">
                        {alert.alert_id}
                      </td>
                      <td className="px-6 py-4 font-bold text-white">
                        {alert.asset_id}
                      </td>
                      <td className="px-6 py-4 text-slate-400">
                        {alert.sensor_id}
                      </td>
                      <td className="px-6 py-4">
                        <span className="px-2 py-0.5 rounded bg-rose-500/10 text-rose-400 border border-rose-500/20 font-medium">
                          {alert.alert_type}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-rose-400 font-bold">
                        {formatTemperature(alert.temperature)}
                      </td>
                      <td className="px-6 py-4 text-slate-400">
                        {formatTemperature(alert.threshold)}
                      </td>
                      <td className="px-6 py-4 text-slate-400">
                        {formatTimestamp(alert.timestamp)}
                      </td>
                      <td className="px-6 py-4 text-right">
                        <button
                          onClick={() => dismissAlert(alert.alert_id)}
                          className="p-1.5 rounded-lg text-slate-400 hover:bg-slate-800 hover:text-slate-200 transition-colors"
                          title="Dismiss"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </section>

      {/* Toast Notification Container */}
      <AlertToastContainer alerts={alerts} onDismiss={dismissAlert} />
    </main>
  );
}
