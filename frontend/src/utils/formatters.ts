export function formatTemperature(temp: number): string {
  return `${temp.toFixed(1)}°C`;
}

export function formatTimestamp(timestamp: string): string {
  return new Date(timestamp).toLocaleTimeString();
}
