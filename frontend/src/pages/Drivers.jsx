import React, { useState, useEffect } from 'react';
import Modal from '../components/Modal';
import { drivers as initialDrivers } from '../data/mockData';
import { driverApi } from '../api/endpoints';
import { useAuth } from '../auth/AuthContext';
import { canEdit } from '../auth/permissions';

const AVATAR_COLORS = [
  { bg: '#bcd9f5', fg: '#0d2a4a' },
  { bg: '#5b9fe0', fg: '#0d2a4a' },
  { bg: '#1f5fa8', fg: '#ffffff' },
  { bg: '#3bb3d6', fg: '#0d2a4a' },
  { bg: '#f4b27a', fg: '#0d2a4a' },
  { bg: '#bcd9f5', fg: '#0d2a4a' }
];

const cap = (s) => (s ? s.charAt(0).toUpperCase() + s.slice(1).toLowerCase() : '');

const vehicleText = (d) => {
  if (d.vehicleType || d.vehicleNo) return `${cap(d.vehicleType)} ${d.vehicleNo || ''}`.trim();
  return d.vehicle || '-';
};

export default function Drivers() {
  const { user } = useAuth();
  const editable = canEdit(user?.role);
  const [driverList, setDriverList] = useState(initialDrivers);
  const [showAdd, setShowAdd] = useState(false);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [form, setForm] = useState({
    name: '', licenseNo: '', phone: '', vehicleType: 'TRUCK', vehicleNo: '', licenseExpiry: ''
  });

  const loadDrivers = () => {
    driverApi.getAll({ q: search || undefined, status: statusFilter })
      .then(res => {
        if (res && res.content && res.content.length > 0) {
          setDriverList(res.content.map(d => ({
            id: d.driverId,
            name: d.name,
            license: d.licenseNo,
            licenseExpiry: d.licenseExpiry || '-',
            phone: d.phone,
            vehicleType: d.vehicleType,
            vehicleNo: d.vehicleNo,
            status: d.active ? 'Available' : 'Not Available'
          })));
        }
      })
      .catch(() => {});
  };

  useEffect(() => {
    loadDrivers();
  }, [search, statusFilter]);

  const handleAdd = async (e) => {
    e.preventDefault();
    try {
      await driverApi.create({
        name: form.name,
        licenseNo: form.licenseNo,
        licenseExpiry: form.licenseExpiry || null,
        phone: form.phone,
        vehicleType: form.vehicleType,
        vehicleNo: form.vehicleNo
      });
      setShowAdd(false);
      setForm({ name: '', licenseNo: '', phone: '', vehicleType: 'TRUCK', vehicleNo: '', licenseExpiry: '' });
      loadDrivers();
    } catch (err) {
      alert(err.message || 'Failed to add driver');
    }
  };

  const handleToggle = async (id) => {
    try {
      await driverApi.toggleStatus(id);
      loadDrivers();
    } catch (err) {
      alert(err.message || 'Failed to toggle status');
    }
  };

  const q = search.trim().toLowerCase();
  const filtered = driverList.filter(d => {
    const matchSearch =
      (d.name || '').toLowerCase().includes(q) ||
      (d.license || '').toLowerCase().includes(q) ||
      (d.phone || '').includes(search.trim());
    const matchStatus = statusFilter === 'ALL' ||
      (statusFilter === 'ACTIVE' && d.status === 'Available') ||
      (statusFilter === 'INACTIVE' && d.status === 'Not Available');
    return matchSearch && matchStatus;
  });

  const FILTERS = [
    { key: 'ALL', label: 'All' },
    { key: 'ACTIVE', label: 'Available' },
    { key: 'INACTIVE', label: 'Not Available' }
  ];

  return (
    <div>
      {/* Search + status chips + add button */}
      <div className="dr-toolbar">
        <div className="dr-filters">
          <input
            className="dr-search"
            placeholder="Search name, phone or license"
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
          {FILTERS.map(f => (
            <button
              type="button"
              key={f.key}
              className={`dr-chip ${statusFilter === f.key ? 'active' : ''}`}
              onClick={() => setStatusFilter(f.key)}
            >
              {f.label}
            </button>
          ))}
        </div>
        {editable && (
          <button className="btn btn-primary dr-add" onClick={() => setShowAdd(true)}>+ Add Driver</button>
        )}
      </div>

      {/* Driver cards */}
      <div className="dr-grid">
        {filtered.map((d, idx) => {
          const color = AVATAR_COLORS[idx % AVATAR_COLORS.length];
          const available = d.status === 'Available';
          return (
            <div className="dr-card" key={d.id || idx}>
              <div className="dr-avatar" style={{ background: color.bg, color: color.fg }}>
                {(d.name || '?').charAt(0).toUpperCase()}
              </div>
              <h3 className="dr-name">{d.name}</h3>
              <p>License: {d.license}</p>
              <p>Phone: {d.phone}</p>
              <p>Vehicle: {vehicleText(d)}</p>
              <div className="dr-card-foot">
                <span className={`dr-status ${available ? 'available' : 'unavailable'}`}>{d.status}</span>
                {editable && (
                <button
                  type="button"
                  className={`dr-toggle ${available ? 'off' : 'on'}`}
                  onClick={() => handleToggle(d.id)}
                >
                  {available ? 'Deactivate' : 'Activate'}
                </button>
              )}
              </div>
            </div>
          );
        })}
        {filtered.length === 0 && <div className="dr-empty">No drivers found</div>}
      </div>

      {editable && showAdd && (
        <Modal title="Add New Driver" onClose={() => setShowAdd(false)}>
          <form onSubmit={handleAdd}>
            <div className="form-group">
              <label>Driver Full Name</label>
              <input required value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} placeholder="e.g. Rafiqul Islam" />
            </div>
            <div className="form-group">
              <label>License No.</label>
              <input required value={form.licenseNo} onChange={e => setForm({ ...form, licenseNo: e.target.value })} placeholder="e.g. DL-2024-8899" />
            </div>
            <div className="form-group">
              <label>License Expiry Date</label>
              <input type="date" value={form.licenseExpiry} onChange={e => setForm({ ...form, licenseExpiry: e.target.value })} />
            </div>
            <div className="form-group">
              <label>Phone Number (11 digits)</label>
              <input required value={form.phone} onChange={e => setForm({ ...form, phone: e.target.value })} placeholder="01711000000" />
            </div>
            <div className="form-group">
              <label>Vehicle Type</label>
              <select value={form.vehicleType} onChange={e => setForm({ ...form, vehicleType: e.target.value })}>
                <option value="TRUCK">Truck</option>
                <option value="PICKUP">Pickup</option>
                <option value="VAN">Van</option>
              </select>
            </div>
            <div className="form-group">
              <label>Vehicle No.</label>
              <input required value={form.vehicleNo} onChange={e => setForm({ ...form, vehicleNo: e.target.value })} placeholder="e.g. DHK-MET-1122" />
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 20 }}>
              <button type="button" className="btn btn-secondary" onClick={() => setShowAdd(false)}>Cancel</button>
              <button type="submit" className="btn btn-primary">Save Driver</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
