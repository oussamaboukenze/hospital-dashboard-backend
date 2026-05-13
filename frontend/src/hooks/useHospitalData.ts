import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import api, { WS_URL } from '../api/axios';
import { useAppSelector } from './redux';
import type { AlertItem, Reading, Refrigerator } from '../types';

interface HospitalDataState {
  fridges: Refrigerator[];
  readings: Reading[];
  history: Reading[];
  alerts: AlertItem[];
  selectedDeviceId: string | null;
  selectedFridge?: Refrigerator;
  loading: boolean;
  error: string | null;
  wsConnected: boolean;
  refresh: () => Promise<void>;
  simulateReading: () => Promise<void>;
  resolveAlert: (id: number) => Promise<void>;
}

export function useHospitalData(routeDeviceId?: string): HospitalDataState {
  const currentUser = useAppSelector((state) => state.auth.user);
  const assignedFridgeIds = currentUser?.assignedFridgeIds;

  const [fridges, setFridges] = useState<Refrigerator[]>([]);
  const [readings, setReadings] = useState<Reading[]>([]);
  const [history, setHistory] = useState<Reading[]>([]);
  const [alerts, setAlerts] = useState<AlertItem[]>([]);
  const [selectedDeviceId, setSelectedDeviceId] = useState<string | null>(routeDeviceId ?? null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [wsConnected, setWsConnected] = useState(false);
  const selectedRef = useRef<string | null>(selectedDeviceId);

  useEffect(() => {
    selectedRef.current = selectedDeviceId;
  }, [selectedDeviceId]);

  const selectedFridge = useMemo(
    () => fridges.find((fridge) => fridge.deviceId === selectedDeviceId),
    [fridges, selectedDeviceId],
  );

  const loadReadings = useCallback(async (deviceId: string) => {
    const { data } = await api.get<Reading[]>(`/fridges/${deviceId}/readings?limit=80`);
    setReadings(data);
  }, []);

  const loadHistory = useCallback(async (deviceId: string) => {
    const { data } = await api.get<Reading[]>(`/history?deviceId=${deviceId}&limit=200`);
    setHistory(data);
  }, []);

  const refresh = useCallback(async () => {
    setError(null);
    try {
      const [fridgeResponse, alertResponse] = await Promise.all([
        api.get<Refrigerator[]>('/fridges'),
        api.get<AlertItem[]>('/alerts?resolved=false'),
      ]);

      setFridges(fridgeResponse.data);
      setAlerts(alertResponse.data);

      const nextDeviceId = routeDeviceId ?? selectedRef.current ?? fridgeResponse.data[0]?.deviceId ?? null;
      setSelectedDeviceId(nextDeviceId);
      if (nextDeviceId) {
        await Promise.all([loadReadings(nextDeviceId), loadHistory(nextDeviceId)]);
      }
    } catch {
      setError('Impossible de joindre le backend Spring Boot');
    } finally {
      setLoading(false);
    }
  }, [loadHistory, loadReadings, routeDeviceId]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  useEffect(() => {
    if (!routeDeviceId) {
      return;
    }
    setSelectedDeviceId(routeDeviceId);
    Promise.all([loadReadings(routeDeviceId), loadHistory(routeDeviceId)])
      .catch(() => setError('Impossible de charger les mesures'));
  }, [loadHistory, loadReadings, routeDeviceId]);

  useEffect(() => {
    const client = new Client({
      brokerURL: WS_URL,
      reconnectDelay: 4000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setWsConnected(true);
        client.subscribe('/topic/fridges', (message) => {
          const fridge = JSON.parse(message.body) as Refrigerator;
          if (assignedFridgeIds && assignedFridgeIds.length > 0 && !assignedFridgeIds.includes(fridge.deviceId)) return;
          setFridges((current) => {
            const exists = current.some((item) => item.deviceId === fridge.deviceId);
            return exists
              ? current.map((item) => (item.deviceId === fridge.deviceId ? fridge : item))
              : [...current, fridge];
          });
        });
        client.subscribe('/topic/readings', (message) => {
          const reading = JSON.parse(message.body) as Reading;
          if (reading.deviceId !== selectedRef.current) return;
          if (assignedFridgeIds && assignedFridgeIds.length > 0 && !assignedFridgeIds.includes(reading.deviceId)) return;
          setReadings((current) => [...current.slice(-79), reading]);
          setHistory((current) => [...current, reading].slice(-200));
        });
        client.subscribe('/topic/alerts', (message) => {
          const alert = JSON.parse(message.body) as AlertItem;
          if (assignedFridgeIds && assignedFridgeIds.length > 0 && !assignedFridgeIds.includes(alert.deviceId)) return;
          setAlerts((current) => {
            if (alert.resolved) {
              return current.filter((item) => item.id !== alert.id);
            }
            const exists = current.some((item) => item.id === alert.id);
            return exists ? current.map((item) => (item.id === alert.id ? alert : item)) : [alert, ...current];
          });
        });
      },
      onDisconnect: () => setWsConnected(false),
      onWebSocketClose: () => setWsConnected(false),
      onStompError: () => setWsConnected(false),
    });

    client.activate();
    return () => {
      client.deactivate();
    };
  }, []);

  const simulateReading = useCallback(async () => {
    const deviceId = selectedRef.current ?? fridges[0]?.deviceId ?? 'FRIDGE-001';
    const base = selectedFridge?.lastTemperature ?? 4.5;
    const shouldAlert = Math.random() > 0.78;
    const temperature = shouldAlert ? 8.8 + Math.random() * 2.4 : base + (Math.random() - 0.5) * 1.4;
    const humidity = 52 + Math.random() * 18;

    await api.post('/readings/simulate', {
      deviceId,
      temperature: Number(temperature.toFixed(2)),
      humidity: Number(humidity.toFixed(2)),
      doorOpen: shouldAlert && Math.random() > 0.5,
      battery: Math.max(45, Math.round((selectedFridge?.battery ?? 95) - Math.random() * 2)),
      timestamp: new Date().toISOString(),
    });

    if (!wsConnected) {
      await refresh();
    }
  }, [fridges, refresh, selectedFridge, wsConnected]);

  const resolveAlert = useCallback(async (id: number) => {
    await api.patch(`/alerts/${id}/resolve`);
    setAlerts((current) => current.filter((alert) => alert.id !== id));
  }, []);

  return {
    fridges,
    readings,
    history,
    alerts,
    selectedDeviceId,
    selectedFridge,
    loading,
    error,
    wsConnected,
    refresh,
    simulateReading,
    resolveAlert,
  };
}
