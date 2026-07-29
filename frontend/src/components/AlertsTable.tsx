"use client";

import React from "react";
import { useTelemetryStore } from "@/store/useTelemetryStore";
import { Shield, Trash2, AlertOctagon, Flame, ShieldAlert } from "lucide-react";
import { formatTemperature, formatTimestamp } from "@/utils/formatters";

export function AlertsTable() {
  const alerts = useTelemetryStore((state) => state.alerts);
  const dismissAlert = useTelemetryStore((state) => state.dismissAlert);
  const clearAlerts = useTelemetryStore((state) => state.clearAlerts);
  const setSelectedAssetId = useTelemetryStore((state) => state.setSelectedAssetId);

  return (
    <div className="rounded-lg border border-[#c3c6d2] bg-white overflow-hidden shadow-xs">
      <div className="flex items-center justify-between px-6 py-4 border-b border-[#c3c6d2]">
        <h2 className="text-base font-bold text-[#002a58] flex items-center gap-2">
          <AlertOctagon className="h-5 w-5 text-[#ba1a1a]" />
          Critical Anomaly Log (WebSocket Feed)
          {alerts.length > 0 && (
            <span className="rounded px-2 py-0.5 text-xs text-[#ba1a1a] bg-[#ffdad6] font-mono border border-[#ba1a1a]/30 font-bold">
              {alerts.length}
            </span>
          )}
        </h2>

        {alerts.length > 0 && (
          <button
            onClick={clearAlerts}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded bg-[#f8f9ff] hover:bg-[#eff4ff] text-[#424750] text-xs font-mono border border-[#c3c6d2] transition-colors"
          >
            <Trash2 className="h-3.5 w-3.5" /> Clear All
          </button>
        )}
      </div>

      {alerts.length === 0 ? (
        <div className="p-10 text-center text-[#737781]">
          <Shield className="h-9 w-9 mx-auto mb-2 text-[#737781] opacity-50" />
          <p className="text-xs font-medium text-[#424750]">No critical anomaly alerts detected</p>
          <p className="text-[11px] mt-1 text-[#737781] font-mono">
            System is monitoring telemetry streams in real-time.
          </p>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-[#0d1c2e]">
            <thead className="bg-[#f8f9ff] font-mono uppercase text-[#424750] border-b border-[#c3c6d2]">
              <tr>
                <th className="px-6 py-3">Alert ID</th>
                <th className="px-6 py-3">Asset ID</th>
                <th className="px-6 py-3">Alert Type</th>
                <th className="px-6 py-3">Temp / Thresh</th>
                <th className="px-6 py-3">Diagnostic Action</th>
                <th className="px-6 py-3">Timestamp</th>
                <th className="px-6 py-3 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#c3c6d2]/60 font-mono">
              {alerts.map((alert) => (
                <tr
                  key={alert.alert_id}
                  onClick={() => setSelectedAssetId(alert.asset_id)}
                  className="hover:bg-[#eff4ff] cursor-pointer transition-colors"
                >
                  <td className="px-6 py-3 font-semibold text-[#424750] truncate max-w-[120px]">
                    {alert.alert_id}
                  </td>
                  <td className="px-6 py-3 font-bold text-[#002a58]">
                    {alert.asset_id}
                  </td>
                  <td className="px-6 py-3">
                    <span className="px-2 py-0.5 rounded bg-[#ffdad6] text-[#ba1a1a] border border-[#ba1a1a]/30 font-bold flex items-center gap-1 w-fit">
                      <Flame className="h-3 w-3" />
                      {alert.alert_type}
                    </span>
                  </td>
                  <td className="px-6 py-3 text-[#ba1a1a] font-bold">
                    {formatTemperature(alert.temperature)} / <span className="text-[#737781] font-normal">{formatTemperature(alert.threshold)}</span>
                  </td>
                  <td className="px-6 py-3">
                    {alert.diagnostic_action ? (
                      <span className="px-2 py-1 rounded bg-[#fff8f6] text-[#904d00] border border-[#ffdcbe] font-medium flex items-center gap-1.5 w-fit text-[11px]">
                        <ShieldAlert className="h-3.5 w-3.5 text-[#904d00] shrink-0" />
                        {alert.diagnostic_action.title}
                      </span>
                    ) : (
                      <span className="text-[#737781] italic">Standard Procedure</span>
                    )}
                  </td>
                  <td className="px-6 py-3 text-[#424750]">
                    {formatTimestamp(alert.timestamp)}
                  </td>
                  <td className="px-6 py-3 text-right">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        dismissAlert(alert.alert_id);
                      }}
                      className="p-1 rounded text-[#737781] hover:bg-[#ffdad6] hover:text-[#ba1a1a] transition-colors"
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
