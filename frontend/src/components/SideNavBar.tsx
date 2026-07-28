"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Shield, MapPin, BarChart3, Radio, Wrench } from "lucide-react";

export function SideNavBar() {
  const pathname = usePathname();

  const isMapActive = pathname === "/" || pathname === "/map";
  const isAnalyticsActive = pathname === "/analytics";

  return (
    <aside className="fixed left-0 top-0 h-full w-[64px] bg-white border-r border-[#c3c6d2] flex flex-col items-center py-4 z-50 shadow-sm">
      {/* Brand Icon */}
      <Link href="/" className="mb-6 group flex items-center justify-center p-2 rounded-lg hover:bg-slate-100 transition-colors">
        <Shield className="h-7 w-7 text-[#004080] group-hover:scale-105 transition-transform" />
      </Link>

      {/* Navigation Links */}
      <nav className="flex flex-col gap-3 items-center w-full">
        <Link
          href="/"
          className={`w-full py-3 flex justify-center transition-all ${
            isMapActive
              ? "text-[#004080] border-l-4 border-[#004080] bg-[#e6eeff]/60 font-bold"
              : "text-[#424750] hover:bg-[#eff4ff] hover:text-[#002a58]"
          }`}
          title="Fleet Map View"
        >
          <MapPin className="h-5 w-5" />
        </Link>

        <Link
          href="/analytics"
          className={`w-full py-3 flex justify-center transition-all ${
            isAnalyticsActive
              ? "text-[#004080] border-l-4 border-[#004080] bg-[#e6eeff]/60 font-bold"
              : "text-[#424750] hover:bg-[#eff4ff] hover:text-[#002a58]"
          }`}
          title="Grid & Stream Engine Analytics"
        >
          <BarChart3 className="h-5 w-5" />
        </Link>
      </nav>

      {/* Footer / Engine Connection Indicator */}
      <div className="mt-auto flex flex-col items-center gap-3">
        <div className="h-2 w-2 rounded-full bg-[#006a6a] animate-pulse" title="Engine Live Connection" />
      </div>
    </aside>
  );
}
