import { useEffect, useMemo, useState } from 'react';
import { AlertTriangle, Bell, Droplets, FlaskConical, Play, Snowflake, Thermometer } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import Header from '../components/Header';
import MetricCard from '../components/MetricCard';
import ReadingChart from '../components/ReadingChart';
import Sidebar from '../components/Sidebar';
import { logout } from '../features/auth/authSlice';
import { useAppDispatch, useAppSelector } from '../hooks/redux';
import { useHospitalData } from '../hooks/useHospitalData';
import styles from './Dashboard.module.css';

export default function Dashboard() {
  const { deviceId } = useParams();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const userName = useAppSelector((state) => state.auth.user?.name);
  const userRole = useAppSelector((state) => state.auth.user?.role);
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const {
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
  } = useHospitalData(deviceId);

  useEffect(() => {
    if (!deviceId && selectedDeviceId) {
      navigate(`/fridges/${selectedDeviceId}`, { replace: true });
    }
  }, [deviceId, navigate, selectedDeviceId]);

  const metrics = useMemo(() => {
    const activeAlerts = alerts.length;
    const critical = fridges.filter((fridge) => fridge.status === 'CRITICAL').length;
    const averageTemperature = fridges.length
      ? fridges.reduce((sum, fridge) => sum + (fridge.lastTemperature ?? 0), 0) / fridges.length
      : 0;
    return { activeAlerts, critical, averageTemperature };
  }, [alerts.length, fridges]);

  return (
    <div className={styles.shell}>
      <Header
        userName={userName}
        userRole={userRole}
        wsConnected={wsConnected}
        onMenuClick={() => setSidebarOpen((open) => !open)}
        onRefresh={refresh}
        onLogout={() => dispatch(logout())}
      />

      <div className={styles.workspace}>
        <Sidebar fridges={fridges} isOpen={sidebarOpen} />

        <main className={styles.main}>
          {error && <div className={styles.error}>{error}</div>}

          <section className={styles.hero}>
            <div>
              <p className={styles.eyebrow}>Surveillance IoT MQTT</p>
              <h2>{selectedFridge?.label ?? 'Aucun refrigerateur selectionne'}</h2>
              <span>{selectedFridge?.location ?? 'En attente des donnees du backend'}</span>
            </div>
            <button className={styles.simulate} onClick={simulateReading}>
              <Play size={18} />
              Simuler une mesure
            </button>
          </section>

          <section className={styles.metrics}>
            <MetricCard
              label="Temperature"
              value={selectedFridge?.lastTemperature != null ? `${selectedFridge.lastTemperature.toFixed(1)} C` : '--'}
              hint={`Plage cible ${selectedFridge?.minTemperature ?? 2} C - ${selectedFridge?.maxTemperature ?? 8} C`}
              tone={selectedFridge?.status === 'CRITICAL' ? 'red' : selectedFridge?.status === 'WARNING' ? 'amber' : 'teal'}
              icon={Thermometer}
            />
            <MetricCard
              label="Humidite"
              value={selectedFridge?.lastHumidity != null ? `${selectedFridge.lastHumidity.toFixed(0)}%` : '--'}
              hint="Mesure DHT22 depuis ESP32"
              tone="blue"
              icon={Droplets}
            />
            <MetricCard
              label="Alertes"
              value={metrics.activeAlerts.toString()}
              hint={`${metrics.critical} equipement(s) critique(s)`}
              tone={metrics.activeAlerts > 0 ? 'red' : 'teal'}
              icon={Bell}
            />
            <MetricCard
              label="Moyenne parc"
              value={`${metrics.averageTemperature.toFixed(1)} C`}
              hint={`${fridges.length} equipement(s) suivis`}
              tone="amber"
              icon={Snowflake}
            />
          </section>

          <section className={styles.grid}>
            <div className={styles.panel}>
              <div className={styles.panelHeader}>
                <div>
                  <h3>Courbe temperature / humidite</h3>
                  <p>{readings.length} mesures recentes</p>
                </div>
                <FlaskConical size={22} />
              </div>
              {loading ? <div className={styles.placeholder}>Chargement...</div> : <ReadingChart readings={readings} fridge={selectedFridge} />}
            </div>

            <div className={styles.panel}>
              <div className={styles.panelHeader}>
                <div>
                  <h3>Alertes actives</h3>
                  <p>Temperature, porte et integrite froide</p>
                </div>
                <AlertTriangle size={22} />
              </div>

              <div className={styles.alerts}>
                {alerts.length === 0 && <div className={styles.empty}>Aucune alerte active.</div>}
                {alerts.map((alert) => (
                  <article key={alert.id} className={`${styles.alert} ${styles[alert.severity.toLowerCase()]}`}>
                    <div>
                      <strong>{alert.deviceId}</strong>
                      <p>{alert.message}</p>
                      <small>{new Date(alert.createdAt).toLocaleString('fr-FR')}</small>
                    </div>
                    <button onClick={() => resolveAlert(alert.id)}>Resoudre</button>
                  </article>
                ))}
              </div>
            </div>
          </section>

          <section className={styles.panel}>
            <div className={styles.panelHeader}>
              <div>
                <h3>Historique des mesures</h3>
                <p>Mesures stockees en base H2 depuis le flux MQTT</p>
              </div>
            </div>
            <div className={styles.tableWrap}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Device</th>
                    <th>Temperature</th>
                    <th>Humidite</th>
                    <th>Porte</th>
                    <th>Batterie</th>
                  </tr>
                </thead>
                <tbody>
                  {history.slice(-30).reverse().map((reading) => (
                    <tr key={reading.id}>
                      <td>{new Date(reading.recordedAt).toLocaleString('fr-FR')}</td>
                      <td>{reading.deviceId}</td>
                      <td>{reading.temperature.toFixed(1)} C</td>
                      <td>{reading.humidity.toFixed(0)}%</td>
                      <td>{reading.doorOpen ? 'Ouverte' : 'Fermee'}</td>
                      <td>{reading.battery}%</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </main>
      </div>
    </div>
  );
}
