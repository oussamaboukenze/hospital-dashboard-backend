import { useEffect } from 'react';
import { Navigate, Outlet, Route, Routes } from 'react-router-dom';
import { setAuthToken } from './api/axios';
import Login from './features/auth/Login';
import { useAppSelector } from './hooks/redux';

export default function App() {
  const token = useAppSelector((state) => state.auth.token);

  useEffect(() => {
    setAuthToken(token);
  }, [token]);

  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/" element={<div>Dashboard à venir</div>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

function ProtectedRoute() {
  const user = useAppSelector((state) => state.auth.user);
  return user ? <Outlet /> : <Navigate to="/login" replace />;
}
