"use client";

import React from "react";
import { CriticalAlert } from "@/types/alert";
import { AlertToast } from "./AlertToast";

interface AlertToastContainerProps {
  alerts: CriticalAlert[];
  onDismiss: (id: string) => void;
}

export const AlertToastContainer: React.FC<AlertToastContainerProps> = ({
  alerts,
  onDismiss,
}) => {
  if (alerts.length === 0) return null;

  return (
    <div className="fixed bottom-6 right-6 z-50 flex max-w-md w-full flex-col gap-3 pointer-events-none">
      {alerts.map((alert) => (
        <div key={alert.alert_id} className="pointer-events-auto">
          <AlertToast alert={alert} onDismiss={onDismiss} />
        </div>
      ))}
    </div>
  );
};
