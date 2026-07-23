"use client";

import React from "react";
import { useTelemetryStore } from "@/store/useTelemetryStore";
import { Shield, Trash2, AlertOctagon, Flame } from "lucide-react";
import { formatTemperature, formatTimestamp } from "@/utils/formatters";

export function AlertsTable() {
  const alerts = useTelemetryStore((state) => state.alerts);
  const dismissAlert = useTelemetryStore((state) => state.dismissAlert);
  const clearAlerts = useTelemetryStore((state) => state.clearAlerts);
  const setSelectedAssetId = useTelemetryStore((state) => state.setSelectedAssetId);

  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900/60 backdrop-blur-md overflow-hidden shadow-xl">
      <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800">
        <h2 className="text-base font-bold text-white flex items-center gap-2">
          <AlertOctagon className="h-5 w-5 text-rose-500" />
          Critical Anomaly Log (WebSocket Feed)
          {alerts.length > 0 && (
            <span className="rounded-full bg-rose-500/20 px-2.5 py-0.5 text-xs text-rose-400 font-mono border border-rose-500/30">
              {alerts.length}
            </span>
          )}
        </h2>

        {alerts.length > 0 && (
          <button
            onClick={clearAlerts}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-mono border border-slate-700 transition-colors"
          >
            <Trash2 className="h-3.5 w-3.5" /> Clear All
          </button>
        )}
      </div>

      {alerts.length === 0 ? (
        <div className="p-12 text-center text-slate-500">
          <Shield className="h-10 w-10 mx-auto mb-3 text-slate-700 opacity-60" />
          <p className="text-sm font-medium text-slate-400">No critical anomaly alerts detected</p>
          <p className="text-xs mt-1 text-slate-600 font-mono">
            System is monitoring generator temperatures in real-time.
          </p>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-300">
            <thead className="bg-slate-900/90 text-xs font-mono uppercase text-slate-400 border-b border-slate-800">
              <tr>
                <th className="px-6 py-3.5">Alert ID</th>
                <th className="px-6 py-3.5">Asset ID</th>
                <th className="px-6 py-3.5">Sensor ID</th>
                <th className="px-6 py-3.5">Alert Type</th>
                <th className="px-6 py-3.5">Temperature</th>
                <th className="px-6 py-3.5">Threshold</th>
                <th className="px-6 py-3.5">Timestamp</th>
                <th className="px-6 py-3.5 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/80 font-mono text-xs">
              {alerts.map((alert) => (
                <tr
                  key={alert.alert_id}
                  onClick={() => setSelectedAssetId(alert.asset_id)}
                  className="hover:bg-slate-800/60 cursor-pointer transition-colors"
                >
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
                    <span className="px-2 py-0.5 rounded bg-rose-500/10 text-rose-400 border border-rose-500/20 font-medium flex items-center gap-1 w-fit">
                      <Flame className="h-3 w-3" />
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
                      onClick={(e) => {
                        e.stopPropagation();
                        dismissAlert(alert.alert_id);
                      }}
                      className="p-1.5 rounded-lg text-slate-400 hover:bg-slate-800 hover:text-slate-200 transition-colors"
                      title="Dismiss Alert"
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
  );
}
