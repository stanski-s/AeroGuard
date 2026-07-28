"use client";

import React, { useEffect, useRef, useState, useMemo } from "react";
import * as echarts from "echarts";
import { useTelemetryStore } from "@/store/useTelemetryStore";
import { Activity, Flame, AlertTriangle } from "lucide-react";

type MetricMode = "temperature" | "vibration" | "powerOutputMw" | "rotorSpeedRpm" | "pitchAngleDeg";

export function TelemetryChart() {
  const chartRef = useRef<HTMLDivElement>(null);
  const chartInstance = useRef<echarts.ECharts | null>(null);

  const selectedAssetId = useTelemetryStore((state) => state.selectedAssetId);
  const setSelectedAssetId = useTelemetryStore((state) => state.setSelectedAssetId);
  const assets = useTelemetryStore((state) => state.assets);
  const telemetryHistory = useTelemetryStore((state) => state.telemetryHistory);
  const alerts = useTelemetryStore((state) => state.alerts);
  const isConnected = useTelemetryStore((state) => state.isConnected);

  const [metricMode, setMetricMode] = useState<MetricMode>("temperature");

  const selectedAsset = assets.find((a) => a.id === selectedAssetId) || assets[0];

  const historyPoints = useMemo(() => {
    return (selectedAssetId && telemetryHistory[selectedAssetId]) || [];
  }, [selectedAssetId, telemetryHistory]);

  const activeAlert = alerts.find((a) => a.asset_id === selectedAssetId);

  useEffect(() => {
    if (!chartRef.current) return;

    if (!chartInstance.current) {
      chartInstance.current = echarts.init(chartRef.current, undefined, {
        renderer: "canvas",
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

  useEffect(() => {
    if (!chartInstance.current) return;

    const times = historyPoints.map((p) =>
      new Date(p.timestamp).toLocaleTimeString([], {
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
      })
    );

    const dataValues = historyPoints.map((p) => {
      switch (metricMode) {
        case "temperature":
          return p.temperature;
        case "vibration":
          return p.vibration;
        case "powerOutputMw":
          return p.powerOutputMw ?? selectedAsset?.powerOutputMw ?? 12.5;
        case "rotorSpeedRpm":
          return p.rotorSpeedRpm ?? selectedAsset?.rotorSpeedRpm ?? 7.5;
        case "pitchAngleDeg":
          return p.pitchAngleDeg ?? selectedAsset?.pitchAngleDeg ?? 4.2;
      }
    });

    const isTempMode = metricMode === "temperature";
    const thresholdVal = isTempMode ? 80.0 : metricMode === "vibration" ? 0.35 : null;
    const hasSpike = isTempMode && dataValues.some((v) => v > 80.0);

    const lineColor = isTempMode
      ? hasSpike
        ? "#ba1a1a"
        : "#004080"
      : metricMode === "vibration"
      ? "#006a6a"
      : "#004080";

    const getUnit = () => {
      switch (metricMode) {
        case "temperature": return "°C";
        case "vibration": return " g";
        case "powerOutputMw": return " MW";
        case "rotorSpeedRpm": return " RPM";
        case "pitchAngleDeg": return "°";
      }
    };

    const getMetricName = () => {
      switch (metricMode) {
        case "temperature": return "Generator Temp";
        case "vibration": return "Vibration Level";
        case "powerOutputMw": return "Power Output";
        case "rotorSpeedRpm": return "Rotor Speed";
        case "pitchAngleDeg": return "Pitch Angle";
      }
    };

    const option: echarts.EChartsOption = {
      backgroundColor: "transparent",
      tooltip: {
        trigger: "axis",
        backgroundColor: "rgba(255, 255, 255, 0.95)",
        borderColor: "#c3c6d2",
        textStyle: { color: "#0d1c2e", fontFamily: "monospace", fontSize: 12 },
        formatter: (params: any) => {
          if (!Array.isArray(params) || params.length === 0) return "";
          const p = params[0];
          const unit = getUnit();
          const val = p.value;
          const conditionText =
            thresholdVal && val > thresholdVal
              ? "<span style='color:#ba1a1a;font-weight:bold;'>CRITICAL BREACH</span>"
              : "<span style='color:#006a6a;font-weight:bold;'>NOMINAL</span>";
          return `
            <div style="font-weight:bold;margin-bottom:4px;">${p.name}</div>
            <div>Value: <b>${val}${unit}</b></div>
            <div>Condition: ${conditionText}</div>
          `;
        },
      },
      grid: {
        top: 40,
        left: 45,
        right: 25,
        bottom: 30,
        containLabel: false,
      },
      xAxis: {
        type: "category",
        data: times,
        boundaryGap: false,
        axisLine: { lineStyle: { color: "#c3c6d2" } },
        axisLabel: { color: "#424750", fontFamily: "monospace", fontSize: 10 },
      },
      yAxis: {
        type: "value",
        min: isTempMode ? 30 : 0,
        axisLine: { show: false },
        splitLine: { lineStyle: { color: "rgba(195, 198, 210, 0.5)", type: "dashed" } },
        axisLabel: {
          color: "#424750",
          fontFamily: "monospace",
          fontSize: 10,
          formatter: (val: number) => `${val}${getUnit()}`,
        },
      },
      series: [
        {
          name: getMetricName(),
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
                    ? "rgba(186, 26, 26, 0.3)"
                    : "rgba(0, 64, 128, 0.25)"
                  : "rgba(0, 106, 106, 0.25)",
              },
              { offset: 1, color: "rgba(248, 249, 255, 0.0)" },
            ]),
          },
          ...(thresholdVal ? {
            markLine: {
              symbol: "none",
              data: [
                {
                  yAxis: thresholdVal,
                  name: "Critical Threshold",
                  lineStyle: { color: "#ba1a1a", type: "dashed", width: 1.5 },
                  label: {
                    formatter: `Threshold (${thresholdVal}${getUnit()})`,
                    color: "#ba1a1a",
                    fontFamily: "monospace",
                    fontSize: 10,
                    position: "insideEndTop",
                  },
                },
              ],
            },
          } : {}),
        },
      ],
    };

    chartInstance.current.setOption(option, { notMerge: false, lazyUpdate: true });
  }, [historyPoints, metricMode, selectedAssetId, selectedAsset]);

  const latestPoint = historyPoints[historyPoints.length - 1];

  const getLatestDisplay = () => {
    if (!latestPoint) return "--";
    switch (metricMode) {
      case "temperature": return `${latestPoint.temperature.toFixed(1)}°C`;
      case "vibration": return `${latestPoint.vibration.toFixed(3)}g`;
      case "powerOutputMw": return `${(latestPoint.powerOutputMw ?? 12.5).toFixed(1)} MW`;
      case "rotorSpeedRpm": return `${(latestPoint.rotorSpeedRpm ?? 7.5).toFixed(1)} RPM`;
      case "pitchAngleDeg": return `${(latestPoint.pitchAngleDeg ?? 4.2).toFixed(1)}°`;
    }
  };

  return (
    <div className="relative w-full h-[520px] rounded-lg border border-[#c3c6d2] bg-white p-5 flex flex-col justify-between shadow-xs">
      {/* Chart Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-[#c3c6d2]">
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded border bg-[#eff4ff] border-[#c3c6d2] text-[#004080]">
            <Activity className="h-5 w-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-sm font-bold text-[#002a58] tracking-tight font-sans">
                Live Telemetry Stream
              </h3>
              <span className="text-[10px] font-mono font-bold px-2 py-0.5 rounded bg-[#eff4ff] text-[#004080] border border-[#004080]/20">
                ECharts Stream
              </span>
            </div>
            <p className="text-xs text-[#424750]">
              Real-time Kafka WebSocket telemetry feed
            </p>
          </div>
        </div>

        {/* Controls */}
        <div className="flex items-center gap-2 flex-wrap">
          <select
            value={selectedAssetId || ""}
            onChange={(e) => setSelectedAssetId(e.target.value)}
            className="bg-[#f8f9ff] border border-[#c3c6d2] text-[#002a58] text-xs font-mono rounded px-2.5 py-1.5 focus:ring-1 focus:ring-[#004080] outline-none"
          >
            {assets.map((asset) => (
              <option key={asset.id} value={asset.id}>
                {asset.name} ({asset.id})
              </option>
            ))}
          </select>

          <div className="flex bg-[#f8f9ff] border border-[#c3c6d2] rounded p-0.5 text-xs font-mono">
            <button
              onClick={() => setMetricMode("temperature")}
              className={`px-2 py-1 rounded transition-colors ${
                metricMode === "temperature"
                  ? "bg-[#004080] text-white font-bold"
                  : "text-[#424750] hover:text-[#002a58]"
              }`}
            >
              Temp
            </button>
            <button
              onClick={() => setMetricMode("vibration")}
              className={`px-2 py-1 rounded transition-colors ${
                metricMode === "vibration"
                  ? "bg-[#006a6a] text-white font-bold"
                  : "text-[#424750] hover:text-[#002a58]"
              }`}
            >
              Vib
            </button>
            <button
              onClick={() => setMetricMode("powerOutputMw")}
              className={`px-2 py-1 rounded transition-colors ${
                metricMode === "powerOutputMw"
                  ? "bg-[#004080] text-white font-bold"
                  : "text-[#424750] hover:text-[#002a58]"
              }`}
            >
              Power
            </button>
            <button
              onClick={() => setMetricMode("rotorSpeedRpm")}
              className={`px-2 py-1 rounded transition-colors ${
                metricMode === "rotorSpeedRpm"
                  ? "bg-[#004080] text-white font-bold"
                  : "text-[#424750] hover:text-[#002a58]"
              }`}
            >
              Rotor
            </button>
            <button
              onClick={() => setMetricMode("pitchAngleDeg")}
              className={`px-2 py-1 rounded transition-colors ${
                metricMode === "pitchAngleDeg"
                  ? "bg-[#004080] text-white font-bold"
                  : "text-[#424750] hover:text-[#002a58]"
              }`}
            >
              Pitch
            </button>
          </div>
        </div>
      </div>

      {/* Live Value Indicator */}
      <div className="flex items-center justify-between mt-3 px-1">
        <div className="flex items-baseline gap-3">
          <span className="text-3xl font-bold font-mono text-[#002a58] tracking-tight">
            {getLatestDisplay()}
          </span>
          <span className="text-xs font-mono text-[#424750]">
            {selectedAsset ? selectedAsset.name : ""}
          </span>
        </div>

        {activeAlert && (
          <div className="flex items-center gap-1.5 text-[#ba1a1a] bg-[#ffdad6] border border-[#ba1a1a]/40 px-2.5 py-1 rounded text-xs font-mono font-bold animate-pulse">
            <Flame className="h-4 w-4" />
            <span>THRESHOLD BREACH (&gt;80.0°C)</span>
          </div>
        )}
      </div>

      {/* ECharts Render Container */}
      <div className="relative flex-1 w-full mt-2">
        <div ref={chartRef} className="absolute inset-0 w-full h-full" />

        {historyPoints.length === 0 && (
          <div className="absolute inset-0 z-20 flex flex-col items-center justify-center bg-white/90 backdrop-blur-sm rounded-lg p-6 text-center border border-[#c3c6d2]">
            <AlertTriangle className="h-6 w-6 text-[#ba1a1a] animate-pulse mb-2" />
            <h4 className="text-xs font-bold text-[#ba1a1a] font-mono">
              {isConnected ? "WAITING FOR KAFKA TELEMETRY STREAM" : "WEBSOCKET GATEWAY DISCONNECTED"}
            </h4>
          </div>
        )}
      </div>

      {/* Footer Info */}
      <div className="flex items-center justify-between text-[10px] font-mono text-[#737781] pt-2 border-t border-[#c3c6d2]">
        <span>Window: 60s Rolling Buffer • ECharts Engine</span>
        <span>Keyed Process Function</span>
      </div>
    </div>
  );
}
