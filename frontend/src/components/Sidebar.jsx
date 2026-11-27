import React from 'react';
import { useAuth } from '../auth/AuthContext';
import { canView } from '../auth/permissions';

const menuItems = [
  { name: 'Dashboard', icon: '📊' },
  { name: 'Products', icon: '📦' },
  { name: 'Arrivals', icon: '🚚' },
  { name: 'Buyers', icon: '🤝' },
  { name: 'Donations', icon: '🎁' },
  { name: 'Drivers', icon: '🚗' },
  { name: 'Warehouses', icon: '🏭' },
  { name: 'Reports', icon: '📈' },
  { name: 'Alerts', icon: '🔔' },
  { name: 'Transport Cost', icon: '💰' },
];

export default function Sidebar({ activePage, setActivePage, isOpen }) {
  const { user } = useAuth();
  const items = menuItems.filter((item) => canView(user?.role, item.name));

  return (
    <div className="sidebar" style={{ transform: isOpen ? 'translateX(0)' : 'translateX(-100%)' }}>
      <div className="sidebar-logo">
        <div className="logo-icon">S</div>
        <div>
          <h2>SPHWMS</h2>
          <span>Warehouse Management</span>
        </div>
      </div>
      <nav className="sidebar-nav">
        {items.map(item => (
          <div
            key={item.name}
            className={`nav-item ${activePage === item.name ? 'active' : ''}`}
            onClick={() => setActivePage(item.name)}
          >
            <span className="nav-icon">{item.icon}</span>
            <span>{item.name}</span>
          </div>
        ))}
      </nav>
    </div>
  );
}
