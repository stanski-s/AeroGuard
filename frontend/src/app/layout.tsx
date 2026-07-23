import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "AeroGuard - Real-Time Command Center",
  description: "Smart Grid Anomaly Detection and Stream Telemetry Pipeline",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className="bg-slate-950 text-slate-100 min-h-screen">
        {children}
      </body>
    </html>
  );
}
