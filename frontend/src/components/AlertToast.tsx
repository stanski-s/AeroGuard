"use client";

import React from "react";
import { CriticalAlert } from "@/types/alert";
import { AlertTriangle, X, Thermometer, ShieldAlert } from "lucide-react";
import { formatTemperature, formatTimestamp } from "@/utils/formatters";

interface AlertToastProps {
  alert: CriticalAlert;
  onDismiss: (id: string) => void;
}

export const AlertToast: React.FC<AlertToastProps> = ({ alert, onDismiss }) => {
  const action = alert.diagnostic_action;

  return (
    <div className="relative overflow-hidden rounded-xl bg-gradient-to-r from-rose-950/80 via-slate-900/90 to-slate-950/90 border border-rose-500/40 p-4 shadow-2xl backdrop-blur-md transition-all duration-300 hover:border-rose-400 transform animate-slide-in">
      {/* Flashing glow accent */}
      <div className="absolute top-0 left-0 bottom-0 w-1.5 bg-rose-500 animate-pulse" />
      
      <div className="flex items-start justify-between gap-3 pl-2">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-rose-500/20 text-rose-400 border border-rose-500/30 shadow-inner">
            <AlertTriangle className="h-5 w-5 animate-bounce" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-mono text-xs font-semibold uppercase tracking-wider text-rose-400">
                {alert.alert_type}
              </span>
              <span className="rounded bg-rose-500/10 px-2 py-0.5 text-[10px] font-mono font-medium text-rose-300 border border-rose-500/20">
                Asset: {alert.asset_id}
              </span>
            </div>
            <h4 className="mt-0.5 text-sm font-semibold text-slate-100">
              Thermal Breach Detected
            </h4>
          </div>
        </div>

        <button
          onClick={() => onDismiss(alert.alert_id)}
          className="rounded-lg p-1 text-slate-400 hover:bg-slate-800 hover:text-slate-200 transition-colors"
          aria-label="Dismiss alert"
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      {/* Enriched Diagnostic Action Banner */}
      {action && (
        <div className="mt-3 mx-2 rounded-lg bg-amber-500/10 border border-amber-500/30 p-2.5 text-xs text-amber-200">
          <div className="flex items-center gap-2 font-semibold text-amber-300">
            <ShieldAlert className="h-4 w-4 shrink-0 text-amber-400" />
            <span>Action: {action.title}</span>
            {action.is_fallback && (
              <span className="ml-auto text-[10px] font-mono px-1.5 py-0.5 rounded bg-amber-500/20 text-amber-300">
                Fallback
              </span>
            )}
          </div>
          <p className="mt-1 text-[11px] text-amber-200/80 leading-relaxed">
            {action.description}
          </p>
        </div>
      )}

      <div className="mt-3 flex items-center justify-between border-t border-slate-800/80 pt-2.5 pl-2 text-xs text-slate-300">
        <div className="flex items-center gap-1.5 font-mono">
          <Thermometer className="h-4 w-4 text-rose-400" />
          <span>
            Temp: <strong className="text-rose-400 font-bold">{formatTemperature(alert.temperature)}</strong>
          </span>
          <span className="text-slate-500">/</span>
          <span className="text-slate-400">Thresh: {formatTemperature(alert.threshold)}</span>
        </div>
        <time className="font-mono text-[11px] text-slate-400">
          {formatTimestamp(alert.timestamp)}
        </time>
      </div>
    </div>
  );
};
