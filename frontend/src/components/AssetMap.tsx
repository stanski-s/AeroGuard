"use client";

import React, { useEffect, useRef } from "react";
import mapboxgl from "mapbox-gl";
import "mapbox-gl/dist/mapbox-gl.css";
import { useTelemetryStore } from "@/store/useTelemetryStore";
import { getAssetOperatingStatus } from "@/utils/assetHelpers";
import { Navigation, AlertTriangle, CheckCircle2, Wrench } from "lucide-react";

const MAPBOX_TOKEN = process.env.NEXT_PUBLIC_MAPBOX_TOKEN || "";

export function AssetMap() {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<mapboxgl.Map | null>(null);
  const markersRef = useRef<Map<string, mapboxgl.Marker>>(new Map());

  // Fine-grained Zustand state selectors
  const assets = useTelemetryStore((state) => state.assets);
  const alerts = useTelemetryStore((state) => state.alerts);
  const selectedAssetId = useTelemetryStore((state) => state.selectedAssetId);
  const setSelectedAssetId = useTelemetryStore((state) => state.setSelectedAssetId);

  // Initialize Map
  useEffect(() => {
    if (!mapContainerRef.current || mapRef.current) return;

    if (MAPBOX_TOKEN) {
      mapboxgl.accessToken = MAPBOX_TOKEN;
    }

    const mapStyle = MAPBOX_TOKEN
      ? "mapbox://styles/mapbox/dark-v11"
      : "https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json";

    const map = new mapboxgl.Map({
      container: mapContainerRef.current,
      style: mapStyle,
      center: [6.50, 54.55], // Centered around North Sea Offshore Array
      zoom: 9.5,
      pitch: 45,
      bearing: -15,
      attributionControl: false,
    });

    map.addControl(new mapboxgl.NavigationControl({ showCompass: true }), "top-right");
    mapRef.current = map;

    return () => {
      map.remove();
      mapRef.current = null;
    };
  }, []);

  // Efficient Marker Updates without unnecessary innerHTML teardowns
  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;

    const currentMarkerIds = new Set<string>();

    assets.forEach((asset) => {
      currentMarkerIds.add(asset.id);

      const hasAlert = alerts.some((a) => a.asset_id === asset.id);
      const isSelected = selectedAssetId === asset.id;
      const statusInfo = getAssetOperatingStatus(asset, hasAlert);

      let markerObj = markersRef.current.get(asset.id);
      let el = markerObj?.getElement();

      if (!el) {
        el = document.createElement("div");
        el.className = "cursor-pointer group relative flex items-center justify-center";

        const marker = new mapboxgl.Marker({ element: el })
          .setLngLat([asset.lng, asset.lat])
          .addTo(map);

        el.addEventListener("click", (e) => {
          e.stopPropagation();
          setSelectedAssetId(asset.id);
          map.flyTo({
            center: [asset.lng, asset.lat],
            zoom: 11,
            speed: 1.2,
            curve: 1.4,
          });
        });

        markersRef.current.set(asset.id, marker);
      }

      // Check if state attribute actually changed before rebuilding DOM structure
      const stateKey = `${statusInfo.stateLabel}-${isSelected}`;
      if (el.getAttribute("data-state-key") !== stateKey) {
        el.setAttribute("data-state-key", stateKey);

        const pulseRing = statusInfo.isAlerting
          ? `
            <span class="absolute -inset-2 rounded-full bg-rose-500/40 animate-ping"></span>
            <span class="absolute -inset-4 rounded-full bg-rose-500/20 animate-pulse"></span>
          `
          : "";

        const activeBorder = isSelected
          ? "ring-2 ring-cyan-400 ring-offset-2 ring-offset-slate-950 scale-125 z-50"
          : "";

        el.innerHTML = `
          <div class="relative flex items-center justify-center">
            ${pulseRing}
            <div class="relative h-8 w-8 rounded-full border-2 backdrop-blur-md flex items-center justify-center font-mono font-bold text-xs shadow-lg transition-all duration-300 ${statusInfo.markerColorClass} ${activeBorder}">
              <div class="h-2.5 w-2.5 rounded-full ${statusInfo.dotColorClass}"></div>
            </div>
            <div class="absolute -top-7 left-1/2 -translate-x-1/2 whitespace-nowrap rounded px-2 py-0.5 text-[10px] font-medium bg-slate-900/90 border border-slate-700 text-slate-200 opacity-90 group-hover:opacity-100 transition-opacity shadow-md pointer-events-none">
              ${asset.name}
            </div>
          </div>
        `;
      }
    });

    // Clean up removed markers
    markersRef.current.forEach((marker, id) => {
      if (!currentMarkerIds.has(id)) {
        marker.remove();
        markersRef.current.delete(id);
      }
    });
  }, [assets, alerts, selectedAssetId, setSelectedAssetId]);

  const selectedAsset = assets.find((a) => a.id === selectedAssetId);
  const selectedAlert = alerts.find((a) => a.asset_id === selectedAssetId);

  const resetView = () => {
    if (mapRef.current) {
      mapRef.current.flyTo({
        center: [6.50, 54.55],
        zoom: 9.5,
        pitch: 45,
        bearing: -15,
      });
    }
  };

  return (
    <div className="relative w-full h-[520px] rounded-2xl overflow-hidden border border-slate-800 shadow-2xl bg-slate-950">
      {/* Map Canvas */}
      <div ref={mapContainerRef} className="absolute inset-0 w-full h-full" />

      {/* Header Overlay */}
      <div className="absolute top-4 left-4 z-10 flex items-center gap-3 bg-slate-900/80 backdrop-blur-md border border-slate-700/60 px-4 py-2.5 rounded-xl shadow-xl">
        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-cyan-500/10 border border-cyan-500/30 text-cyan-400">
          <Navigation className="h-4 w-4" />
        </div>
        <div>
          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-200">
            Spatial Asset Grid
          </h3>
          <p className="text-[11px] text-slate-400 font-mono">
            North Sea Offshore Field • Mapbox GL Dark
          </p>
        </div>
        <button
          onClick={resetView}
          className="ml-3 px-2 py-1 rounded bg-slate-800 hover:bg-slate-700 text-[10px] font-mono text-slate-300 border border-slate-700 transition-colors"
          title="Reset map view"
        >
          Reset View
        </button>
      </div>

      {/* Map Legend */}
      <div className="absolute bottom-4 left-4 z-10 bg-slate-900/85 backdrop-blur-md border border-slate-800 px-3.5 py-2 rounded-xl text-[11px] flex items-center gap-4 text-slate-300 font-mono shadow-lg">
        <div className="flex items-center gap-1.5">
          <span className="h-2.5 w-2.5 rounded-full bg-emerald-400 shadow-sm shadow-emerald-400"></span>
          <span>Online</span>
        </div>
        <div className="flex items-center gap-1.5">
          <span className="h-2.5 w-2.5 rounded-full bg-rose-500 animate-pulse shadow-sm shadow-rose-500"></span>
          <span>Critical Spike</span>
        </div>
        <div className="flex items-center gap-1.5">
          <span className="h-2.5 w-2.5 rounded-full bg-amber-400 shadow-sm shadow-amber-400"></span>
          <span>Maintenance</span>
        </div>
      </div>

      {/* Selected Asset Overlay Card */}
      {selectedAsset && (
        <div className="absolute top-4 right-14 z-10 bg-slate-900/90 backdrop-blur-lg border border-slate-700/80 p-4 rounded-xl shadow-2xl max-w-xs text-xs">
          <div className="flex items-center justify-between gap-2 pb-2 border-b border-slate-800">
            <span className="font-bold text-white text-sm">{selectedAsset.name}</span>
            {selectedAlert ? (
              <span className="px-2 py-0.5 rounded bg-rose-500/20 text-rose-400 border border-rose-500/30 text-[10px] font-mono font-bold flex items-center gap-1 animate-pulse">
                <AlertTriangle className="h-3 w-3" /> ALERT
              </span>
            ) : selectedAsset.operatingMode === "MAINTENANCE_MODE" ? (
              <span className="px-2 py-0.5 rounded bg-amber-500/20 text-amber-400 border border-amber-500/30 text-[10px] font-mono flex items-center gap-1">
                <Wrench className="h-3 w-3" /> MAINT
              </span>
            ) : (
              <span className="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-[10px] font-mono flex items-center gap-1">
                <CheckCircle2 className="h-3 w-3" /> OK
              </span>
            )}
          </div>
          <div className="mt-2.5 space-y-1.5 text-slate-300 font-mono text-[11px]">
            <div className="flex justify-between">
              <span className="text-slate-400">Asset ID:</span>
              <span className="text-slate-200 font-semibold">{selectedAsset.id}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Coordinates:</span>
              <span className="text-slate-200">{selectedAsset.lat}°N, {selectedAsset.lng}°E</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Location:</span>
              <span className="text-slate-200 truncate max-w-[130px]">{selectedAsset.locationName}</span>
            </div>
            {selectedAlert && (
              <div className="mt-2 pt-2 border-t border-rose-500/30 text-rose-300 bg-rose-950/40 p-2 rounded border">
                <p className="font-bold">Spike: {selectedAlert.temperature}°C</p>
                <p className="text-[10px] text-rose-400 mt-0.5">{selectedAlert.message}</p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
