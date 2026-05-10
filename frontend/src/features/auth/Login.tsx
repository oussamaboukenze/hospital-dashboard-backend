import { FormEvent, useState } from 'react';
import { Activity, Lock, Mail } from 'lucide-react';
import { Navigate, useNavigate } from 'react-router-dom';
import api from '../../api/axios';
import { loginFailure, loginStart, loginSuccess } from './authSlice';
import { useAppDispatch, useAppSelector } from '../../hooks/redux';
import styles from './Login.module.css';

export default function Login() {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { user, loading, error } = useAppSelector((state) => state.auth);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  if (user) {
    return <Navigate to="/" replace />;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    dispatch(loginStart());

    try {
      const { data } = await api.post('/auth/login', { email, password });
      dispatch(loginSuccess({ user: data.user, token: data.token }));
      navigate('/', { replace: true });
    } catch {
      dispatch(loginFailure('Email ou mot de passe incorrect'));
    }
  }

  return (
    <main className={styles.container}>
      <form className={styles.form} onSubmit={handleSubmit}>
        <div className={styles.brand}>
          <span className={styles.brandIcon}>
            <Activity size={26} aria-hidden />
          </span>
          <div>
            <h1>MediCold</h1>
            <p>Surveillance hospitaliere temps reel</p>
          </div>
        </div>

        {error && <div className={styles.error}>{error}</div>}

        <label className={styles.field}>
          <span>Email</span>
          <div className={styles.inputWrap}>
            <Mail size={18} aria-hidden />
            <input
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
          </div>
        </label>

        <label className={styles.field}>
          <span>Mot de passe</span>
          <div className={styles.inputWrap}>
            <Lock size={18} aria-hidden />
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
            />
          </div>
        </label>

        <button className={styles.button} type="submit" disabled={loading}>
          {loading ? 'Connexion...' : 'Se connecter'}
        </button>
      </form>
    </main>
  );
}
