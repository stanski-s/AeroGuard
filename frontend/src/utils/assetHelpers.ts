import { AssetInfo, OperatingMode } from "@/types/telemetry";

export interface AssetOperatingStatus {
  stateLabel: string;
  badgeClass: string;
  markerColorClass: string;
  dotColorClass: string;
  isAlerting: boolean;
  isMaintenance: boolean;
}

export function getAssetOperatingStatus(
  asset: AssetInfo,
  hasAlert: boolean
): AssetOperatingStatus {
  const isMaintenance = asset.operatingMode === "MAINTENANCE_MODE";

  if (hasAlert) {
    return {
      stateLabel: "CRITICAL",
      badgeClass: "bg-rose-500/20 text-rose-300 border border-rose-500/30",
      markerColorClass: "bg-rose-500/30 border-rose-500 text-rose-300 shadow-rose-500/50 scale-110",
      dotColorClass: "bg-rose-500",
      isAlerting: true,
      isMaintenance: false,
    };
  }

  if (isMaintenance) {
    return {
      stateLabel: "MAINT_MODE",
      badgeClass: "bg-amber-500/20 text-amber-300 border border-amber-500/30",
      markerColorClass: "bg-amber-500/20 border-amber-400 text-amber-300 shadow-amber-500/30",
      dotColorClass: "bg-amber-400",
      isAlerting: false,
      isMaintenance: true,
    };
  }

  return {
    stateLabel: "ONLINE",
    badgeClass: "bg-emerald-500/20 text-emerald-300 border border-emerald-500/30",
    markerColorClass: "bg-emerald-500/20 border-emerald-400 text-emerald-300 shadow-emerald-500/30",
    dotColorClass: "bg-emerald-400",
    isAlerting: false,
    isMaintenance: false,
  };
}

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
