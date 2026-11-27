import React, { useState, useEffect } from 'react';
import Modal from '../components/Modal';
import { warehouses as initialWarehouses } from '../data/mockData';
import { warehouseApi } from '../api/endpoints';

export default function Warehouses() {
  const [warehouseList, setWarehouseList] = useState(initialWarehouses);
  const [showAdd, setShowAdd] = useState(false);
  const [showDetails, setShowDetails] = useState(null);
  const [search, setSearch] = useState('');
  const [form, setForm] = useState({ name: '', location: '', capacityKg: '' });

  const loadWarehouses = () => {
    warehouseApi.getAll()
      .then(data => {
        if (data && data.length > 0) {
          setWarehouseList(data.map(w => ({
            id: w.warehouseId,
            code: w.code,
            name: w.name,
            location: w.location,
            capacity: Number(w.capacityKg),
            used: Number(w.totalStock || 0),
            usedPct: Math.round((Number(w.totalStock || 0) / Number(w.capacityKg || 1)) * 100),
            status: w.status === 'ACTIVE' ? 'Active' : 'Inactive',
            stockBreakdown: w.stockBreakdown || []
          })));
        }
      })
      .catch(() => {});
  };

  useEffect(() => {
    loadWarehouses();
  }, []);

  const handleAdd = async (e) => {
    e.preventDefault();
    try {
      await warehouseApi.create({
        name: form.name,
        location: form.location,
        capacityKg: Number(form.capacityKg),
      });
      setShowAdd(false);
      setForm({ name: '', location: '', capacityKg: '' });
      loadWarehouses();
    } catch (err) {
      alert(err.message || 'Failed to create warehouse');
    }
  };

  const filtered = warehouseList.filter(w =>
    w.name.toLowerCase().includes(search.toLowerCase()) ||
    w.code.toLowerCase().includes(search.toLowerCase()) ||
    w.location.toLowerCase().includes(search.toLowerCase())
  );

  const usedColor = (pct) => {
    if (pct >= 80) return 'var(--danger)';
    if (pct >= 50) return 'var(--warning)';
    return 'var(--success)';
  };

  return (
    <div>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <div className="table-filters">
          <input className="search-input" placeholder="Search name, code or location..." value={search} onChange={e => setSearch(e.target.value)} />
        </div>
        <button className="btn btn-primary" onClick={() => setShowAdd(true)}>+ Add Warehouse</button>
      </div>

      {/* Warehouse Table */}
      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Code</th>
              <th>Name</th>
              <th>Location</th>
              <th>Capacity (kg)</th>
              <th>Stock Used (kg)</th>
              <th>Utilization</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map(w => (
              <tr key={w.id}>
                <td><strong>{w.code}</strong></td>
                <td>{w.name}</td>
                <td>{w.location}</td>
                <td>{w.capacity.toLocaleString()}</td>
                <td>{w.used.toLocaleString()}</td>
                <td>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <div className="bar-wrapper" style={{ width: 100, height: 8 }}>
                      <div className="bar-fill" style={{ width: `${Math.min(w.usedPct, 100)}%`, backgroundColor: usedColor(w.usedPct) }} />
                    </div>
                    <span style={{ fontSize: '0.8rem', fontWeight: 600, color: usedColor(w.usedPct) }}>{w.usedPct}%</span>
                  </div>
                </td>
                <td><span className={`badge ${w.status === 'Active' ? 'badge-success' : 'badge-gray'}`}>{w.status}</span></td>
                <td>
                  <button className="btn btn-secondary" style={{ padding: '4px 10px', fontSize: '0.8rem' }} onClick={() => setShowDetails(w)}>
                    Details
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Add Warehouse Modal */}
      {showAdd && (
        <Modal title="Add New Warehouse" onClose={() => setShowAdd(false)}>
          <form onSubmit={handleAdd}>
            <div className="form-group">
              <label>Warehouse Name</label>
              <input required value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} placeholder="e.g. Central Warehouse" />
            </div>
            <div className="form-group">
              <label>Location</label>
              <input required value={form.location} onChange={e => setForm({ ...form, location: e.target.value })} placeholder="e.g. Dhaka, Bangladesh" />
            </div>
            <div className="form-group">
              <label>Capacity (kg)</label>
              <input required type="number" value={form.capacityKg} onChange={e => setForm({ ...form, capacityKg: e.target.value })} placeholder="e.g. 10000" />
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 20 }}>
              <button type="button" className="btn btn-secondary" onClick={() => setShowAdd(false)}>Cancel</button>
              <button type="submit" className="btn btn-primary">Save</button>
            </div>
          </form>
        </Modal>
      )}

      {/* Details Modal */}
      {showDetails && (
        <Modal title={`Warehouse: ${showDetails.name} (${showDetails.code})`} onClose={() => setShowDetails(null)}>
          <div style={{ marginBottom: 15 }}>
            <p><strong>Location:</strong> {showDetails.location}</p>
            <p><strong>Capacity:</strong> {showDetails.capacity.toLocaleString()} kg</p>
            <p><strong>Used Stock:</strong> {showDetails.used.toLocaleString()} kg ({showDetails.usedPct}%)</p>
          </div>
          <h4>Stock Breakdown by Product</h4>
          <div className="table-container" style={{ marginTop: 10 }}>
            <table>
              <thead>
                <tr>
                  <th>Product</th>
                  <th>Quantity (kg)</th>
                </tr>
              </thead>
              <tbody>
                {showDetails.stockBreakdown && showDetails.stockBreakdown.length > 0 ? (
                  showDetails.stockBreakdown.map((sb, idx) => (
                    <tr key={idx}>
                      <td>{sb.productName || sb.product}</td>
                      <td>{Number(sb.quantity || sb.qty).toLocaleString()}</td>
                    </tr>
                  ))
                ) : (
                  <tr><td colSpan="2" style={{ textAlign: 'center', color: '#888' }}>No active stock records</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </Modal>
      )}
    </div>
  );
}
