"use client";

import React, { useEffect, useRef, useState } from "react";
import * as maplibregl from "maplibre-gl";
import "maplibre-gl/dist/maplibre-gl.css";
import { useTelemetryStore } from "@/store/useTelemetryStore";
import { getAssetOperatingStatus } from "@/utils/assetHelpers";
import { Navigation, AlertTriangle, CheckCircle2, Wrench, AlertCircle, PowerOff, Layers } from "lucide-react";

const VOYAGER_STYLE: maplibregl.StyleSpecification = {
  version: 8,
  sources: {
    "carto-voyager-tiles": {
      type: "raster",
      tiles: [
        "https://a.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}@2x.png",
        "https://b.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}@2x.png",
        "https://c.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}@2x.png",
        "https://d.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}@2x.png",
      ],
      tileSize: 256,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> &copy; <a href="https://carto.com/attributions">CARTO</a>',
    },
  },
  layers: [
    {
      id: "carto-voyager-layer",
      type: "raster",
      source: "carto-voyager-tiles",
      minzoom: 0,
      maxzoom: 22,
    },
  ],
};

const DARK_STYLE: maplibregl.StyleSpecification = {
  version: 8,
  sources: {
    "carto-dark-tiles": {
      type: "raster",
      tiles: [
        "https://a.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}@2x.png",
        "https://b.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}@2x.png",
        "https://c.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}@2x.png",
        "https://d.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}@2x.png",
      ],
      tileSize: 256,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> &copy; <a href="https://carto.com/attributions">CARTO</a>',
    },
  },
  layers: [
    {
      id: "carto-dark-layer",
      type: "raster",
      source: "carto-dark-tiles",
      minzoom: 0,
      maxzoom: 22,
    },
  ],
};

export function AssetMap() {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<maplibregl.Map | null>(null);
  const markersRef = useRef<Map<string, maplibregl.Marker>>(new Map());
  const [mapTheme, setMapTheme] = useState<"voyager" | "dark">("voyager");
  const [mapLoaded, setMapLoaded] = useState(false);

  const assets = useTelemetryStore((state) => state.assets);
  const alerts = useTelemetryStore((state) => state.alerts);
  const selectedAssetId = useTelemetryStore((state) => state.selectedAssetId);
  const setSelectedAssetId = useTelemetryStore((state) => state.setSelectedAssetId);

  // Initialize Map with CartoDB Voyager raster canvas
  useEffect(() => {
    if (!mapContainerRef.current || mapRef.current) return;

    const map = new maplibregl.Map({
      container: mapContainerRef.current,
      style: VOYAGER_STYLE,
      center: [16.95, 54.55], // Centered across full Polish Baltic coastline (West, Center, East)
      zoom: 7.3,
      pitch: 25,
      bearing: 0,
      attributionControl: false,
    });

    map.addControl(new maplibregl.NavigationControl({ showCompass: true }), "top-right");

    map.on("load", () => {
      map.resize();
      setMapLoaded(true);
    });

    // Fallback if load already fired
    if (map.isStyleLoaded()) {
      setMapLoaded(true);
    }

    mapRef.current = map;

    return () => {
      map.remove();
      mapRef.current = null;
      setMapLoaded(false);
    };
  }, []);

  // Update map style when theme state changes
  const toggleTheme = () => {
    const nextTheme = mapTheme === "voyager" ? "dark" : "voyager";
    setMapTheme(nextTheme);
    if (mapRef.current) {
      mapRef.current.setStyle(nextTheme === "dark" ? DARK_STYLE : VOYAGER_STYLE);
    }
  };

  // Render & update markers whenever assets, alerts, selectedAssetId, or mapLoaded change
  useEffect(() => {
    const map = mapRef.current;
    if (!map || !mapLoaded) return;

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
        el.className = "asset-marker-container cursor-pointer group relative flex items-center justify-center z-10";
        el.style.width = "48px";
        el.style.height = "48px";

        el.addEventListener("click", (e: MouseEvent) => {
          e.stopPropagation();
          setSelectedAssetId(asset.id);
          map.flyTo({
            center: [asset.lng, asset.lat],
            zoom: 11.5,
            speed: 1.2,
            curve: 1.4,
          });
        });

        const marker = new maplibregl.Marker({ element: el, anchor: "center" })
          .setLngLat([asset.lng, asset.lat])
          .addTo(map);

        markersRef.current.set(asset.id, marker);
      }

      const stateKey = `${statusInfo.stateLabel}-${isSelected}`;
      if (el.getAttribute("data-state-key") !== stateKey) {
        el.setAttribute("data-state-key", stateKey);

        const pulseRing = statusInfo.isAlerting
          ? `
            <span class="absolute -inset-2 rounded-full bg-[#ba1a1a]/50 animate-ping"></span>
            <span class="absolute -inset-3 rounded-full bg-[#ba1a1a]/30 animate-pulse"></span>
          `
          : "";

        const activeScale = isSelected
          ? "ring-2 ring-[#004080] ring-offset-2 ring-offset-white scale-125 z-50 shadow-lg"
          : "hover:scale-110";

        const labelBadgeStyle = isSelected
          ? "bg-[#002a58] border-2 border-sky-400 text-white scale-105 z-50 shadow-lg"
          : "bg-[#002a58] border border-[#004080] text-slate-100 group-hover:bg-[#001d3d] group-hover:scale-105 shadow-md";

        // Display exact Turbine ID and Name on map label badge
        const shortNum = asset.id.replace("BAL-WTG-", "WTG-");

        el.innerHTML = `
          <div class="relative flex items-center justify-center w-full h-full">
            ${pulseRing}
            <div class="relative h-11 w-11 rounded-full flex items-center justify-center font-mono font-extrabold text-[10px] tracking-tighter transition-all duration-300 shadow-md ${statusInfo.markerColorClass} ${activeScale}" style="background-color: ${statusInfo.hexBgColor}; color: ${statusInfo.hexTextColor}; border: 2.5px solid #ffffff;">
              ${shortNum}
            </div>
            <div class="absolute -top-9 left-1/2 -translate-x-1/2 whitespace-nowrap rounded-md px-2 py-1 text-[11px] font-mono font-bold border transition-all duration-200 pointer-events-none ${labelBadgeStyle}">
              <span class="text-sky-300 font-extrabold mr-1">[${asset.id}]</span>${asset.name}
            </div>
          </div>
        `;
      }
    });

    // Remove obsolete markers
    markersRef.current.forEach((marker, id) => {
      if (!currentMarkerIds.has(id)) {
        marker.remove();
        markersRef.current.delete(id);
      }
    });
  }, [mapLoaded, assets, alerts, selectedAssetId, setSelectedAssetId]);

  const selectedAsset = assets.find((a) => a.id === selectedAssetId);
  const selectedAlert = alerts.find((a) => a.asset_id === selectedAssetId);

  const resetView = () => {
    if (mapRef.current) {
      mapRef.current.flyTo({
        center: [16.95, 54.55],
        zoom: 7.3,
        pitch: 25,
        bearing: 0,
      });
    }
  };

  return (
    <div className="relative w-full h-[540px] rounded-lg overflow-hidden border border-[#c3c6d2] shadow-sm bg-white">
      {/* Map Canvas */}
      <div ref={mapContainerRef} className="absolute inset-0 w-full h-full" />

      {/* Map Legend */}
      <div className="absolute bottom-4 right-4 z-10 bg-white/95 backdrop-blur-md border border-[#c3c6d2] px-3.5 py-2 rounded-lg text-[11px] flex flex-wrap items-center gap-3.5 text-[#0d1c2e] font-mono shadow-sm">
        <div className="flex items-center gap-1.5">
          <span className="h-2.5 w-2.5 rounded-full bg-emerald-500"></span>
          <span>Online</span>
        </div>
        <div className="flex items-center gap-1.5">
          <span className="h-2.5 w-2.5 rounded-full bg-rose-500 animate-pulse"></span>
          <span>Critical Spike</span>
        </div>
        <div className="flex items-center gap-1.5">
          <span className="h-2.5 w-2.5 rounded-full bg-amber-400"></span>
          <span>Maintenance</span>
        </div>
        <div className="flex items-center gap-1.5">
          <span className="h-2.5 w-2.5 rounded-full bg-orange-400"></span>
          <span>Degraded</span>
        </div>
        <div className="flex items-center gap-1.5">
          <span className="h-2.5 w-2.5 rounded-full bg-slate-500"></span>
          <span>Offline</span>
        </div>
      </div>
    </div>
  );
}
