"use client";

import React, { useEffect, useRef, useState, useMemo } from "react";
import * as echarts from "echarts";
import { useTelemetryStore } from "@/store/useTelemetryStore";
import { Activity, Flame } from "lucide-react";

export function TelemetryChart() {
  const chartRef = useRef<HTMLDivElement>(null);
  const chartInstance = useRef<echarts.ECharts | null>(null);

  // Fine-grained selectors
  const selectedAssetId = useTelemetryStore((state) => state.selectedAssetId);
  const setSelectedAssetId = useTelemetryStore((state) => state.setSelectedAssetId);
  const assets = useTelemetryStore((state) => state.assets);
  const telemetryHistory = useTelemetryStore((state) => state.telemetryHistory);
  const alerts = useTelemetryStore((state) => state.alerts);

  const [metricMode, setMetricMode] = useState<"temperature" | "vibration">("temperature");

  const selectedAsset = assets.find((a) => a.id === selectedAssetId) || assets[0];

  const historyPoints = useMemo(() => {
    return (selectedAssetId && telemetryHistory[selectedAssetId]) || [];
  }, [selectedAssetId, telemetryHistory]);

  const activeAlert = alerts.find((a) => a.asset_id === selectedAssetId);

  // Initialize ECharts with Canvas/WebGL renderer & dark theme
  useEffect(() => {
    if (!chartRef.current) return;

    if (!chartInstance.current) {
      chartInstance.current = echarts.init(chartRef.current, "dark", {
        renderer: "canvas", // WebGL/Canvas high-performance rendering engine
      });
    }

    const handleResize = () => {
      chartInstance.current?.resize();
    };

    window.addEventListener("resize", handleResize);

    return () => {
      window.removeEventListener("resize", handleResize);
      chartInstance.current?.dispose();
      chartInstance.current = null;
    };
  }, []);

  // Update ECharts options whenever telemetry points, metric mode or asset change
  useEffect(() => {
    if (!chartInstance.current) return;

    const times = historyPoints.map((p) =>
      new Date(p.timestamp).toLocaleTimeString([], {
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
      })
    );

    const dataValues = historyPoints.map((p) =>
      metricMode === "temperature" ? p.temperature : p.vibration
    );

    const isTempMode = metricMode === "temperature";
    const thresholdVal = isTempMode ? 80.0 : 0.35;
    const hasSpike = dataValues.some((v) => v > thresholdVal);

    const lineColor = isTempMode
      ? hasSpike
        ? "#f43f5e" // Rose
        : "#06b6d4" // Cyan
      : "#a855f7"; // Purple for vibration

    const option: echarts.EChartsOption = {
      backgroundColor: "transparent",
      tooltip: {
        trigger: "axis",
        backgroundColor: "rgba(15, 23, 42, 0.9)",
        borderColor: "#334155",
        textStyle: { color: "#f8fafc", fontFamily: "monospace", fontSize: 12 },
        formatter: (params: any) => {
          if (!Array.isArray(params) || params.length === 0) return "";
          const p = params[0];
          const unit = isTempMode ? "°C" : " g";
          const val = p.value;
          const conditionText =
            val > thresholdVal
              ? "<span style='color:#f43f5e;font-weight:bold;'>CRITICAL BREACH</span>"
              : "<span style='color:#34d399;'>NORMAL</span>";
          return `
            <div style="font-weight:bold;margin-bottom:4px;">${p.name}</div>
            <div>Value: <b>${val}${unit}</b></div>
            <div>Operating Condition: ${conditionText}</div>
          `;
        },
      },
      grid: {
        top: 45,
        left: 45,
        right: 25,
        bottom: 35,
        containLabel: false,
      },
      xAxis: {
        type: "category",
        data: times,
        boundaryGap: false,
        axisLine: { lineStyle: { color: "#334155" } },
        axisLabel: { color: "#94a3b8", fontFamily: "monospace", fontSize: 10 },
      },
      yAxis: {
        type: "value",
        min: isTempMode ? 50 : 0,
        max: isTempMode ? 100 : 0.5,
        axisLine: { show: false },
        splitLine: { lineStyle: { color: "rgba(51, 65, 85, 0.4)", type: "dashed" } },
        axisLabel: {
          color: "#94a3b8",
          fontFamily: "monospace",
          fontSize: 10,
          formatter: (val: number) => (isTempMode ? `${val}°C` : `${val}`),
        },
      },
      series: [
        {
          name: isTempMode ? "Generator Temp" : "Vibration Level",
          type: "line",
          smooth: true,
          showSymbol: false,
          data: dataValues,
          lineStyle: { color: lineColor, width: 2.5 },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              {
                offset: 0,
                color: isTempMode
                  ? hasSpike
                    ? "rgba(244, 63, 94, 0.4)"
                    : "rgba(6, 182, 212, 0.35)"
                  : "rgba(168, 85, 247, 0.35)",
              },
              { offset: 1, color: "rgba(15, 23, 42, 0.0)" },
            ]),
          },
          markLine: {
            symbol: "none",
            data: [
              {
                yAxis: thresholdVal,
                name: "Critical Threshold",
                lineStyle: { color: "#f43f5e", type: "dashed", width: 1.5 },
                label: {
                  formatter: `Threshold (${thresholdVal}${isTempMode ? "°C" : ""})`,
                  color: "#f43f5e",
                  fontFamily: "monospace",
                  fontSize: 10,
                  position: "insideEndTop",
                },
              },
            ],
          },
        },
      ],
    };

    chartInstance.current.setOption(option, { notMerge: false, lazyUpdate: true });
  }, [historyPoints, metricMode, selectedAssetId]);

  const latestPoint = historyPoints[historyPoints.length - 1];

  return (
    <div className="relative w-full h-[520px] rounded-2xl border border-slate-800 bg-slate-950 p-5 flex flex-col justify-between shadow-2xl">
      {/* Chart Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-800/80">
        <div className="flex items-center gap-3">
          <div className={`flex h-9 w-9 items-center justify-center rounded-xl border ${
            activeAlert ? "bg-rose-500/10 border-rose-500/40 text-rose-400" : "bg-cyan-500/10 border-cyan-500/30 text-cyan-400"
          }`}>
            <Activity className="h-5 w-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-sm font-bold text-white tracking-tight">
                Live Streaming Telemetry
              </h3>
              <span className="text-[10px] font-mono font-semibold px-2 py-0.5 rounded bg-blue-500/20 text-blue-400 border border-blue-500/30">
                ECharts Canvas/WebGL
              </span>
            </div>
            <p className="text-xs text-slate-400">
              High-frequency time series telemetry feed
            </p>
          </div>
        </div>

        {/* Controls: Asset Selector & Metric Toggle */}
        <div className="flex items-center gap-2.5">
          <select
            value={selectedAssetId || ""}
            onChange={(e) => setSelectedAssetId(e.target.value)}
            className="bg-slate-900 border border-slate-700 text-slate-200 text-xs font-mono rounded-lg px-2.5 py-1.5 focus:ring-1 focus:ring-cyan-500 outline-none"
          >
            {assets.map((asset) => (
              <option key={asset.id} value={asset.id}>
                {asset.name} ({asset.id})
              </option>
            ))}
          </select>

          <div className="flex bg-slate-900 border border-slate-800 rounded-lg p-0.5 text-xs font-mono">
            <button
              onClick={() => setMetricMode("temperature")}
              className={`px-2.5 py-1 rounded-md transition-colors ${
                metricMode === "temperature"
                  ? "bg-cyan-500/20 text-cyan-300 font-bold border border-cyan-500/30"
                  : "text-slate-400 hover:text-slate-200"
              }`}
            >
              Temp (°C)
            </button>
            <button
              onClick={() => setMetricMode("vibration")}
              className={`px-2.5 py-1 rounded-md transition-colors ${
                metricMode === "vibration"
                  ? "bg-purple-500/20 text-purple-300 font-bold border border-purple-500/30"
                  : "text-slate-400 hover:text-slate-200"
              }`}
            >
              Vibration
            </button>
          </div>
        </div>
      </div>

      {/* Live Value Indicator */}
      <div className="flex items-center justify-between mt-3 px-1">
        <div className="flex items-baseline gap-3">
          <span className="text-3xl font-bold font-mono text-white tracking-tight">
            {latestPoint
              ? metricMode === "temperature"
                ? `${latestPoint.temperature.toFixed(1)}°C`
                : `${latestPoint.vibration.toFixed(3)}g`
              : "--"}
          </span>
          <span className="text-xs font-mono text-slate-400">
            {selectedAsset ? selectedAsset.name : ""}
          </span>
        </div>

        {activeAlert && (
          <div className="flex items-center gap-1.5 text-rose-400 bg-rose-500/10 border border-rose-500/30 px-2.5 py-1 rounded-lg text-xs font-mono font-bold animate-pulse">
            <Flame className="h-4 w-4" />
            <span>THRESHOLD EXCEEDED (&gt;80.0°C)</span>
          </div>
        )}
      </div>

      {/* ECharts Render Container */}
      <div className="relative flex-1 w-full mt-2">
        <div ref={chartRef} className="absolute inset-0 w-full h-full" />
      </div>

      {/* Footer Info */}
      <div className="flex items-center justify-between text-[11px] font-mono text-slate-500 pt-2 border-t border-slate-900">
        <span>Window: 60s Rolling Buffer • Canvas/WebGL Engine</span>
        <span>Keyed Process Function Stream</span>
      </div>
    </div>
  );
}
