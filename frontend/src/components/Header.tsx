import { LogOut, Menu, RefreshCw, Settings, Wifi, WifiOff } from 'lucide-react';
import { NavLink } from 'react-router-dom';
import styles from './Header.module.css';

interface HeaderProps {
  userName?: string;
  userRole?: string;
  wsConnected: boolean;
  onMenuClick: () => void;
  onRefresh: () => void;
  onLogout: () => void;
}

export default function Header({ userName, userRole, wsConnected, onMenuClick, onRefresh, onLogout }: HeaderProps) {
  return (
    <header className={styles.header}>
      <div className={styles.left}>
        <button className={styles.iconButton} onClick={onMenuClick} title="Basculer le menu" aria-label="Basculer le menu">
          <Menu size={22} />
        </button>
        <div>
          <h1>MediCold</h1>
          <p>Réfrigérateurs hospitaliers</p>
        </div>
      </div>

      <div className={styles.right}>
        <span className={wsConnected ? styles.live : styles.offline}>
          {wsConnected ? <Wifi size={16} /> : <WifiOff size={16} />}
          {wsConnected ? 'Temps réel' : 'Hors ligne'}
        </span>
        <button className={styles.iconButton} onClick={onRefresh} title="Actualiser" aria-label="Actualiser">
          <RefreshCw size={20} />
        </button>
        {userRole === 'admin' && (
          <NavLink to="/admin" className={({ isActive }) => `${styles.adminLink} ${isActive ? styles.adminLinkActive : ''}`}>
            <Settings size={17} />
            Admin
          </NavLink>
        )}
        {userName && <span className={styles.user}>{userName}</span>}
        <button className={styles.logout} onClick={onLogout}>
          <LogOut size={17} />
          Déconnexion
        </button>
      </div>
    </header>
  );
}
