import { memo } from 'react';
import { NavLink } from 'react-router-dom';
import { Battery, MapPin, Thermometer } from 'lucide-react';
import type { Refrigerator } from '../types';
import styles from './Sidebar.module.css';

interface SidebarProps {
  fridges: Refrigerator[];
  isOpen: boolean;
}

function Sidebar({ fridges, isOpen }: SidebarProps) {
  return (
    <aside className={`${styles.sidebar} ${isOpen ? styles.open : styles.closed}`}>
      <div className={styles.titleBlock}>
        <h2>Équipements</h2>
        <span>{fridges.length}</span>
      </div>

      <nav className={styles.list}>
        {fridges.map((fridge) => (
          <NavLink
            key={fridge.deviceId}
            to={`/fridges/${fridge.deviceId}`}
            className={({ isActive }) => `${styles.item} ${isActive ? styles.active : ''}`}
          >
            <span className={`${styles.status} ${styles[fridge.status.toLowerCase()]}`} />
            <strong>{fridge.label}</strong>
            <small>
              <MapPin size={13} />
              {fridge.location}
            </small>
            <span className={styles.meta}>
              <span>
                <Thermometer size={13} />
                {fridge.lastTemperature?.toFixed(1) ?? '--'}°C
              </span>
              <span>
                <Battery size={13} />
                {fridge.battery ?? '--'}%
              </span>
            </span>
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}

export default memo(Sidebar);
