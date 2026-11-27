import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { authApi } from '../api/endpoints';

const KEY = 'sphwms_auth';

const read = () => {
  try { return JSON.parse(localStorage.getItem(KEY)); } catch { return null; }
};

// api wrapper-e Authorization header-er jonno eta use korben
export const getToken = () => read()?.token || null;

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(read);

  const login = async (username, password) => {
    const res = await authApi.login({ username, password });
    const data = {
      token: res.token,
      user: res.user ?? { username: res.username, fullName: res.fullName, role: res.role },
    };
    localStorage.setItem(KEY, JSON.stringify(data));
    setAuth(data);
    return data.user;
  };

  const logout = useCallback(() => {
    localStorage.removeItem(KEY);
    setAuth(null);
  }, []);

  // api wrapper 401 pele window.dispatchEvent(new Event('auth-expired')) pathabe
  useEffect(() => {
    window.addEventListener('auth-expired', logout);
    return () => window.removeEventListener('auth-expired', logout);
  }, [logout]);

  return (
    <AuthContext.Provider
      value={{ user: auth?.user ?? null, token: auth?.token ?? null, isAuthenticated: !!auth?.token, login, logout }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);

// <ProtectedRoute><Layout/></ProtectedRoute>  ba  <ProtectedRoute roles={['ADMIN']}>...</ProtectedRoute>
export function ProtectedRoute({ children, roles }) {
  const { isAuthenticated, user } = useAuth();
  const location = useLocation();
  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: location }} />;
  if (roles && !roles.includes(user?.role)) return <Navigate to="/" replace />;
  return children;
}