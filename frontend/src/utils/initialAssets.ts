import { AssetInfo } from "@/types/telemetry";

export const INITIAL_ASSETS: AssetInfo[] = [
  // Group 1: Far West (Offshore Świnoujście / Kołobrzeg - West Coast)
  {
    id: "BAL-WTG-001",
    name: "Baltic Alpha WTG-01",
    type: "WIND_TURBINE",
    lat: 54.20,
    lng: 14.76,
    operatingMode: "ONLINE",
    locationName: "Baltic Western Array (Pomerania)",
  },
  {
    id: "BAL-WTG-002",
    name: "Baltic Alpha WTG-02",
    type: "WIND_TURBINE",
    lat: 54.20,
    lng: 14.82,
    operatingMode: "ONLINE",
    locationName: "Baltic Western Array (Pomerania)",
  },
  {
    id: "BAL-WTG-003",
    name: "Baltic Alpha WTG-03",
    type: "WIND_TURBINE",
    lat: 54.24,
    lng: 14.76,
    operatingMode: "MAINTENANCE_MODE",
    locationName: "Baltic Western Array (Pomerania)",
  },
  {
    id: "BAL-WTG-004",
    name: "Baltic Alpha WTG-04",
    type: "WIND_TURBINE",
    lat: 54.24,
    lng: 14.82,
    operatingMode: "ONLINE",
    locationName: "Baltic Western Array (Pomerania)",
  },

  // Group 2: Center (Offshore Ustka / Łeba - Central Coast)
  {
    id: "BAL-WTG-005",
    name: "Baltic Beta WTG-01",
    type: "WIND_TURBINE",
    lat: 54.83,
    lng: 17.15,
    operatingMode: "ONLINE",
    locationName: "Baltic Central Array (Słupsk)",
  },
  {
    id: "BAL-WTG-006",
    name: "Baltic Beta WTG-02",
    type: "WIND_TURBINE",
    lat: 54.86,
    lng: 17.20,
    operatingMode: "DEGRADED",
    locationName: "Baltic Central Array (Słupsk)",
  },
  {
    id: "BAL-WTG-007",
    name: "Baltic Beta WTG-03",
    type: "WIND_TURBINE",
    lat: 54.83,
    lng: 17.25,
    operatingMode: "ONLINE",
    locationName: "Baltic Central Array (Słupsk)",
  },

  // Group 3: Far East (Offshore Hel / Władysławowo - East Coast)
  {
    id: "BAL-WTG-008",
    name: "Baltic Gamma WTG-01",
    type: "WIND_TURBINE",
    lat: 54.83,
    lng: 19.05,
    operatingMode: "ONLINE",
    locationName: "Baltic Eastern Array (Gdańsk Bay)",
  },
  {
    id: "BAL-WTG-009",
    name: "Baltic Gamma WTG-02",
    type: "WIND_TURBINE",
    lat: 54.86,
    lng: 19.12,
    operatingMode: "ONLINE",
    locationName: "Baltic Eastern Array (Gdańsk Bay)",
  },
  {
    id: "BAL-WTG-010",
    name: "Baltic Gamma WTG-03",
    type: "WIND_TURBINE",
    lat: 54.83,
    lng: 19.19,
    operatingMode: "OFFLINE",
    locationName: "Baltic Eastern Array (Gdańsk Bay)",
  },
];
