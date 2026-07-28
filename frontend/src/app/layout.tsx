import type { Metadata } from "next";
import "./globals.css";
import { SideNavBar } from "@/components/SideNavBar";
import { TopNavBar } from "@/components/TopNavBar";
import { FooterStatusBar } from "@/components/FooterStatusBar";
import { AlertToastContainer } from "@/components/AlertToastContainer";

export const metadata: Metadata = {
  title: "AeroGuard | Offshore Smart Grid Control",
  description: "Real-time distributed stream processing and telemetry diagnostics engine",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className="bg-[#f8f9ff] text-[#0d1c2e] min-h-screen font-sans overflow-x-hidden selection:bg-[#d6e3ff] selection:text-[#001b3d]">
        <SideNavBar />
        <TopNavBar />
        <main>{children}</main>
        <FooterStatusBar />
      </body>
    </html>
  );
}
