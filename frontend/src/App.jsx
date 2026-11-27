import React, { useState } from 'react';
import { Routes, Route } from 'react-router-dom';
import Sidebar from './components/Sidebar';
import TopBar from './components/TopBar';
import Dashboard from './pages/Dashboard';
import Products from './pages/Products';
import Arrivals from './pages/Arrivals';
import Buyers from './pages/Buyers';
import Donations from './pages/Donations';
import Drivers from './pages/Drivers';
import Warehouses from './pages/Warehouses';
import Reports from './pages/Reports';
import Alerts from './pages/Alerts';
import TransportCost from './pages/TransportCost';
import Profile from './pages/Profile';
import Login from './pages/Login';
import { ProtectedRoute, useAuth } from './auth/AuthContext';
import { canView, homePage } from './auth/permissions';
import './App.css';

function Shell() {
  const { user } = useAuth();
  const role = user?.role;
  const [activePage, setActivePage] = useState(() => homePage(role));
  const [sidebarOpen, setSidebarOpen] = useState(true);

  // a page the role is not allowed to see falls back to the home page
  const page = canView(role, activePage) ? activePage : homePage(role);

  const renderPage = () => {
    switch (page) {
      case 'Dashboard': return <Dashboard />;
      case 'Products': return <Products />;
      case 'Arrivals': return <Arrivals />;
      case 'Buyers': return <Buyers />;
      case 'Donations': return <Donations />;
      case 'Drivers': return <Drivers />;
      case 'Warehouses': return <Warehouses />;
      case 'Reports': return <Reports />;
      case 'Alerts': return <Alerts />;
      case 'Transport Cost': return <TransportCost />;
      case 'Profile': return <Profile />;
      default: return <Profile />;
    }
  };

  return (
    <div className="app">
      <Sidebar activePage={page} setActivePage={setActivePage} isOpen={sidebarOpen} />
      <div className={`main-content ${sidebarOpen ? '' : 'sidebar-collapsed'}`}>
        <TopBar
          title={page}
          toggleSidebar={() => setSidebarOpen(!sidebarOpen)}
          setActivePage={setActivePage}
        />
        <div className="page-content">{renderPage()}</div>
      </div>
    </div>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/*" element={<ProtectedRoute><Shell /></ProtectedRoute>} />
    </Routes>
  );
}
