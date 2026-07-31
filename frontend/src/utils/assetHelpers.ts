import { AssetInfo, OperatingMode } from "@/types/telemetry";

export interface AssetOperatingModeDisplay {
  stateLabel: string;
  badgeClass: string;
  markerColorClass: string;
  dotColorClass: string;
  hexBgColor: string;
  hexTextColor: string;
  isAlerting: boolean;
  isMaintenance: boolean;
  isOffline: boolean;
}

export type AssetOperatingStatus = AssetOperatingModeDisplay;

export function getAssetOperatingModeDisplay(
  asset: AssetInfo,
  hasAlert: boolean
): AssetOperatingModeDisplay {
  if (hasAlert) {
    return {
      stateLabel: "CRITICAL",
      badgeClass: "bg-rose-600 text-white font-bold border border-rose-300",
      markerColorClass: "bg-rose-600 text-white font-extrabold shadow-lg scale-110",
      dotColorClass: "bg-white",
      hexBgColor: "#e11d48",
      hexTextColor: "#ffffff",
      isAlerting: true,
      isMaintenance: false,
      isOffline: false,
    };
  }

  switch (asset.operatingMode) {
    case "MAINTENANCE_MODE":
      return {
        stateLabel: "MAINT_MODE",
        badgeClass: "bg-amber-500 text-white font-bold border border-amber-200",
        markerColorClass: "bg-amber-500 text-white font-extrabold shadow-md",
        dotColorClass: "bg-white",
        hexBgColor: "#f59e0b",
        hexTextColor: "#ffffff",
        isAlerting: false,
        isMaintenance: true,
        isOffline: false,
      };
    case "OFFLINE":
      return {
        stateLabel: "OFFLINE",
        badgeClass: "bg-slate-600 text-white font-bold border border-slate-400",
        markerColorClass: "bg-slate-600 text-white font-extrabold shadow-sm",
        dotColorClass: "bg-white",
        hexBgColor: "#475569",
        hexTextColor: "#ffffff",
        isAlerting: false,
        isMaintenance: false,
        isOffline: true,
      };
    case "ONLINE":
    default:
      return {
        stateLabel: "ONLINE",
        badgeClass: "bg-emerald-600 text-white font-bold border border-emerald-300",
        markerColorClass: "bg-emerald-600 text-white font-extrabold shadow-md",
        dotColorClass: "bg-white",
        hexBgColor: "#059669",
        hexTextColor: "#ffffff",
        isAlerting: false,
        isMaintenance: false,
        isOffline: false,
      };
  }
}

export const getAssetOperatingStatus = getAssetOperatingModeDisplay;

export function countAssetsByOperatingMode(assets: AssetInfo[]) {
  let online = 0;
  let maintenance = 0;
  let offline = 0;

  for (const asset of assets) {
    if (asset.operatingMode === "MAINTENANCE_MODE") {
      maintenance++;
    } else if (asset.operatingMode === "OFFLINE") {
      offline++;
    } else {
      online++;
    }
  }

  return { online, maintenance, offline, total: assets.length };
}
