import React, { useState, useEffect } from 'react';
import { alertApi } from '../api/endpoints';
import { useAuth } from '../auth/AuthContext';
import { canEdit } from '../auth/permissions';

const ROLE_LABELS = {
  MANAGER: 'Warehouse Manager',
  ADMIN: 'Administrator',
  DRIVER: 'Driver',
};

const initialsOf = (name) =>
  (name || '')
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0].toUpperCase())
    .join('') || 'U';

export default function TopBar({ title, toggleSidebar, setActivePage }) {
  const [unread, setUnread] = useState(0);
  const { user, logout } = useAuth();
  const showBell = canEdit(user?.role);

  useEffect(() => {
    if (!showBell) return undefined;
    const load = () =>
      alertApi.getUnreadCount()
        .then(res => setUnread(Number(res?.count ?? 0)))
        .catch(() => {});
    load();
    const timer = setInterval(load, 30000);
    return () => clearInterval(timer);
  }, [title, showBell]);

  const displayName = user?.fullName || user?.name || user?.username || 'User';
  const displayRole = ROLE_LABELS[user?.role] || user?.role || '';

  return (
    <div className="topbar">
      <div className="topbar-left">
        <button className="hamburger" onClick={toggleSidebar}>☰</button>
        <h1>{title}</h1>
      </div>
      <div className="topbar-right">
        {showBell && (
          <button className="topbar-icon" title="Notifications" onClick={() => setActivePage('Alerts')}>
            🔔
            {unread > 0 && <span className="notification-badge">{unread > 99 ? '99+' : unread}</span>}
          </button>
        )}
        <div className="user-info" onClick={() => setActivePage('Profile')}>
          <div className="user-avatar">{initialsOf(displayName)}</div>
          <div className="user-details">
            <div className="user-name">{displayName}</div>
            <div className="user-role">{displayRole}</div>
          </div>
        </div>
        <button className="logout-btn" title="Logout" onClick={logout}>
          <span className="logout-icon">⏻</span>
          <span className="logout-text">Logout</span>
        </button>
      </div>
    </div>
  );
}
