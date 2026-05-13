import { useEffect, useState } from 'react';
import { LogOut, Plus, Save, Settings, Trash2, X } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axios';
import { logout } from '../features/auth/authSlice';
import { useAppDispatch, useAppSelector } from '../hooks/redux';
import type { Refrigerator, User } from '../types';
import styles from './AdminPanel.module.css';

interface TechForm {
  name: string;
  email: string;
  password: string;
  fridgeIds: string[];
}

const EMPTY_FORM: TechForm = { name: '', email: '', password: '', fridgeIds: [] };

export default function AdminPanel() {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const userName = useAppSelector((state) => state.auth.user?.name);

  const [users, setUsers] = useState<User[]>([]);
  const [fridges, setFridges] = useState<Refrigerator[]>([]);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editFridgeIds, setEditFridgeIds] = useState<string[]>([]);
  const [form, setForm] = useState<TechForm>(EMPTY_FORM);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const technicians = users.filter((u) => u.role === 'technician');

  useEffect(() => {
    Promise.all([
      api.get<User[]>('/users'),
      api.get<Refrigerator[]>('/fridges'),
    ]).then(([usersRes, fridgesRes]) => {
      setUsers(usersRes.data);
      setFridges(fridgesRes.data);
    }).catch(() => setError('Impossible de charger les données'));
  }, []);

  function toggleFridgeInForm(deviceId: string) {
    setForm((f) => ({
      ...f,
      fridgeIds: f.fridgeIds.includes(deviceId)
        ? f.fridgeIds.filter((id) => id !== deviceId)
        : [...f.fridgeIds, deviceId],
    }));
  }

  function toggleFridgeInEdit(deviceId: string) {
    setEditFridgeIds((ids) =>
      ids.includes(deviceId) ? ids.filter((id) => id !== deviceId) : [...ids, deviceId],
    );
  }

  async function handleCreateTechnician() {
    if (!form.name || !form.email || !form.password) return;
    setSaving(true);
    setError(null);
    try {
      const { data } = await api.post<User>('/users', {
        name: form.name,
        email: form.email,
        password: form.password,
        role: 'technician',
        assignedFridgeIds: form.fridgeIds,
      });
      setUsers((prev) => [...prev, data]);
      setForm(EMPTY_FORM);
      setShowForm(false);
    } catch {
      setError('Erreur lors de la création du technicien');
    } finally {
      setSaving(false);
    }
  }

  async function handleSaveFridges(userId: string) {
    setSaving(true);
    setError(null);
    try {
      const { data } = await api.put<User>(`/users/${userId}/fridges`, { fridgeIds: editFridgeIds });
      setUsers((prev) => prev.map((u) => (u.id === userId ? data : u)));
      setEditingId(null);
    } catch {
      setError('Erreur lors de la mise à jour');
    } finally {
      setSaving(false);
    }
  }

  function startEdit(user: User) {
    setEditingId(user.id);
    setEditFridgeIds(user.assignedFridgeIds ?? []);
  }

  function cancelEdit() {
    setEditingId(null);
    setEditFridgeIds([]);
  }

  function fridgeLabel(deviceId: string) {
    return fridges.find((f) => f.deviceId === deviceId)?.label ?? deviceId;
  }

  return (
    <div className={styles.shell}>
      <header className={styles.topbar}>
        <div className={styles.topbarLeft}>
          <Settings size={20} />
          <span>MediCold</span>
          <span className={styles.sep}>/</span>
          <strong>Administration</strong>
        </div>
        <div className={styles.topbarRight}>
          {userName && <span className={styles.user}>{userName}</span>}
          <button className={styles.backBtn} onClick={() => navigate('/')}>Dashboard</button>
          <button className={styles.logoutBtn} onClick={() => dispatch(logout())}>
            <LogOut size={16} />
            Déconnexion
          </button>
        </div>
      </header>

      <main className={styles.main}>
        {error && <div className={styles.error}>{error}</div>}

        <section className={styles.section}>
          <div className={styles.sectionHeader}>
            <div>
              <h2>Techniciens</h2>
              <p>{technicians.length} compte(s) technicien</p>
            </div>
            <button className={styles.addBtn} onClick={() => { setShowForm((v) => !v); setForm(EMPTY_FORM); }}>
              <Plus size={18} />
              Nouveau technicien
            </button>
          </div>

          {showForm && (
            <div className={styles.createCard}>
              <div className={styles.createCardHeader}>
                <h3>Créer un technicien</h3>
                <button className={styles.closeBtn} onClick={() => setShowForm(false)}><X size={18} /></button>
              </div>
              <div className={styles.formGrid}>
                <label className={styles.field}>
                  <span>Nom complet</span>
                  <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="Ex: Technicien Bloc B" />
                </label>
                <label className={styles.field}>
                  <span>Email</span>
                  <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="tech@hospital.com" />
                </label>
                <label className={styles.field}>
                  <span>Mot de passe</span>
                  <input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} placeholder="••••••••" />
                </label>
              </div>

              <div className={styles.fridgeSection}>
                <p className={styles.fridgeSectionTitle}>Réfrigérateurs assignés</p>
                <div className={styles.fridgeGrid}>
                  {fridges.map((fridge) => (
                    <label key={fridge.deviceId} className={`${styles.fridgeCheck} ${form.fridgeIds.includes(fridge.deviceId) ? styles.checked : ''}`}>
                      <input
                        type="checkbox"
                        checked={form.fridgeIds.includes(fridge.deviceId)}
                        onChange={() => toggleFridgeInForm(fridge.deviceId)}
                      />
                      <div>
                        <span className={`${styles.dot} ${styles[fridge.status.toLowerCase()]}`} />
                        <strong>{fridge.label}</strong>
                        <small>{fridge.location}</small>
                      </div>
                    </label>
                  ))}
                </div>
              </div>

              <div className={styles.formActions}>
                <button className={styles.cancelBtn} onClick={() => setShowForm(false)}>Annuler</button>
                <button className={styles.saveBtn} onClick={handleCreateTechnician} disabled={saving || !form.name || !form.email || !form.password}>
                  <Save size={16} />
                  {saving ? 'Création...' : 'Créer'}
                </button>
              </div>
            </div>
          )}

          <div className={styles.techList}>
            {technicians.length === 0 && !showForm && (
              <div className={styles.empty}>Aucun technicien. Créez-en un avec le bouton ci-dessus.</div>
            )}
            {technicians.map((tech) => (
              <div key={tech.id} className={styles.techCard}>
                <div className={styles.techCardHeader}>
                  <div className={styles.techInfo}>
                    <strong>{tech.name}</strong>
                    <span className={styles.email}>{tech.email}</span>
                    <span className={styles.roleBadge}>Technicien</span>
                  </div>
                  {editingId !== tech.id && (
                    <button className={styles.editBtn} onClick={() => startEdit(tech)}>
                      Modifier les accès
                    </button>
                  )}
                </div>

                {editingId === tech.id ? (
                  <div className={styles.editBlock}>
                    <p className={styles.fridgeSectionTitle}>Sélectionner les réfrigérateurs accessibles</p>
                    <div className={styles.fridgeGrid}>
                      {fridges.map((fridge) => (
                        <label key={fridge.deviceId} className={`${styles.fridgeCheck} ${editFridgeIds.includes(fridge.deviceId) ? styles.checked : ''}`}>
                          <input
                            type="checkbox"
                            checked={editFridgeIds.includes(fridge.deviceId)}
                            onChange={() => toggleFridgeInEdit(fridge.deviceId)}
                          />
                          <div>
                            <span className={`${styles.dot} ${styles[fridge.status.toLowerCase()]}`} />
                            <strong>{fridge.label}</strong>
                            <small>{fridge.location}</small>
                          </div>
                        </label>
                      ))}
                    </div>
                    <div className={styles.editActions}>
                      <button className={styles.cancelBtn} onClick={cancelEdit}>Annuler</button>
                      <button className={styles.saveBtn} onClick={() => handleSaveFridges(tech.id)} disabled={saving}>
                        <Save size={16} />
                        {saving ? 'Sauvegarde...' : 'Sauvegarder'}
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className={styles.assignedList}>
                    {(tech.assignedFridgeIds ?? []).length === 0
                      ? <span className={styles.noFridges}>Aucun réfrigérateur assigné</span>
                      : (tech.assignedFridgeIds ?? []).map((id) => (
                        <span key={id} className={styles.fridgeChip}>{fridgeLabel(id)}</span>
                      ))
                    }
                  </div>
                )}
              </div>
            ))}
          </div>
        </section>
      </main>
    </div>
  );
}
