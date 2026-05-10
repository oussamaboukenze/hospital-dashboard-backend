export type FridgeStatus = 'OK' | 'WARNING' | 'CRITICAL' | 'UNKNOWN';
export type AlertSeverity = 'OK' | 'WARNING' | 'CRITICAL' | 'UNKNOWN';

export interface User {
  id: string;
  email: string;
  name: string;
  role: string;
  assignedFridgeIds?: string[];
}

export interface Refrigerator {
  id: number;
  deviceId: string;
  label: string;
  location: string;
  minTemperature: number;
  maxTemperature: number;
  lastTemperature: number | null;
  lastHumidity: number | null;
  doorOpen: boolean | null;
  battery: number | null;
  lastSeenAt: string | null;
  status: FridgeStatus;
}

export interface Reading {
  id: number;
  deviceId: string;
  temperature: number;
  humidity: number;
  doorOpen: boolean;
  battery: number;
  recordedAt: string;
}

export interface AlertItem {
  id: number;
  deviceId: string;
  type: string;
  severity: AlertSeverity;
  message: string;
  createdAt: string;
  resolved: boolean;
}
