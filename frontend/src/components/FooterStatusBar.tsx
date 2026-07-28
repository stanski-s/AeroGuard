"use client";

import React from "react";
import { useTelemetryStore } from "@/store/useTelemetryStore";
import { Wifi, Gauge, RefreshCw } from "lucide-react";

export function FooterStatusBar() {
  const isConnected = useTelemetryStore((state) => state.isConnected);
  const assets = useTelemetryStore((state) => state.assets);

  return (
    <footer className="fixed bottom-0 right-0 w-[calc(100%-64px)] h-8 bg-white border-t border-[#c3c6d2] flex justify-start items-center px-6 gap-6 z-50 text-[11px] font-mono">
      <div className={`flex items-center gap-1.5 font-semibold ${isConnected ? "text-[#006a6a]" : "text-[#ba1a1a]"}`}>
        <Wifi className="h-3 w-3" />
        <span>WebSocket: {isConnected ? "Active" : "Disconnected"}</span>
      </div>

      <div className="flex items-center gap-1.5 text-[#424750]">
        <Gauge className="h-3 w-3 text-[#004080]" />
        <span>Assets: {assets.length} Monitored</span>
      </div>

      <div className="flex items-center gap-1.5 text-[#424750]">
        <RefreshCw className="h-3 w-3 animate-spin text-[#004080]" style={{ animationDuration: "4s" }} />
        <span>Rolling State Active</span>
      </div>

      <div className="ml-auto text-[#737781] text-[10px] tracking-widest uppercase">
        AeroGuard Core Engine v4.2.0-STABLE
      </div>
    </footer>
  );
}
