import { useEffect } from 'react';
import { Navigate, Outlet, Route, Routes } from 'react-router-dom';
import { setAuthToken } from './api/axios';
import Login from './features/auth/Login';
import { useAppSelector } from './hooks/redux';
import AdminPanel from './pages/AdminPanel';
import Dashboard from './pages/Dashboard';

export default function App() {
  const token = useAppSelector((state) => state.auth.token);

  useEffect(() => {
    setAuthToken(token);
  }, [token]);

  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/" element={<Dashboard />} />
        <Route path="/fridges/:deviceId" element={<Dashboard />} />
      </Route>
      <Route element={<AdminRoute />}>
        <Route path="/admin" element={<AdminPanel />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

function ProtectedRoute() {
  const user = useAppSelector((state) => state.auth.user);
  return user ? <Outlet /> : <Navigate to="/login" replace />;
}

function AdminRoute() {
  const user = useAppSelector((state) => state.auth.user);
  if (!user) return <Navigate to="/login" replace />;
  if (user.role !== 'admin') return <Navigate to="/" replace />;
  return <Outlet />;
}
