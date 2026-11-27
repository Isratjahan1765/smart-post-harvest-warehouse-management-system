import React, { useState, useEffect } from 'react';
import { ClipboardCheck } from 'lucide-react';
import Modal from '../components/Modal';
import { arrivalApi, productApi } from '../api/endpoints';

// Warehouse id gula amar andaj (1 = A, 2 = B). Tomar DB-r sathe milao.
const WAREHOUSES = [
  { id: 1, label: 'A' },
  { id: 2, label: 'B' },
];
const GRADES = ['A', 'B', 'C'];
const CHECKS = [
  'Packaging ok',
  'No damage / rot',
  'Quantity matches',
  'Expiry valid',
  'Grade matches',
];
const FILTERS = [
  { key: 'ALL', label: 'All' },
  { key: 'PENDING', label: 'Pending' },
  { key: 'CONFIRMED', label: 'Confirmed' },
  { key: 'REJECTED', label: 'Rejected' },
];
const STATUS_LABEL = { PENDING: 'Pending', CONFIRMED: 'Confirmed', REJECTED: 'Rejected' };
const STATUS_CLASS = { PENDING: 'pill-low', CONFIRMED: 'pill-in', REJECTED: 'pill-out' };
const PAGE_SIZES = [5, 10, 20];

const formatDate = (iso) =>
  iso
    ? new Date(`${String(iso).split('T')[0]}T00:00:00`).toLocaleDateString('en-GB', {
        day: '2-digit', month: 'short', year: 'numeric',
      })
    : '-';
const formatMoney = (n) => `Tk ${Number(n || 0).toLocaleString('en-US')}`;

const nextBatchNo = (list) => {
  const max = list.reduce(
    (m, a) => Math.max(m, Number(String(a.batch).split('-').pop()) || 0),
    0
  );
  return `B-${new Date().getFullYear()}-${max + 1}`;
};

const emptyForm = { productId: '', warehouseId: '1', quantity: '', grade: 'A', expiryDate: '' };

export default function Arrivals() {
  const [arrivalList, setArrivalList] = useState([]);
  const [products, setProducts] = useState([]);
  const [filter, setFilter] = useState('ALL');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const [showNew, setShowNew] = useState(false);
  const [form, setForm] = useState(emptyForm);

  const [showQC, setShowQC] = useState(null);
  const [checks, setChecks] = useState(CHECKS.map(() => false));
  const [rejectReason, setRejectReason] = useState('');

  const loadArrivals = () => {
    arrivalApi
      .getAll()
      .then((data) => {
        if (!data) return;
        setArrivalList(
          data.map((a) => ({
            id: a.arrivalId,
            batch: a.batchNo,
            product: a.productName || `Product #${a.productId}`,
            warehouse: String(a.warehouseName || `#${a.warehouseId}`).replace(/^warehouse\s+/i, ''),
            quantity: Number(a.quantity),
            grade: a.grade,
            expiry: a.expiryDate,
            totalCost: a.totalCost ?? Number(a.quantity) * Number(a.purchasePrice),
            status: String(a.status || 'PENDING').toUpperCase(),
          }))
        );
      })
      .catch(() => {});
  };

  useEffect(() => {
    loadArrivals();
    productApi
      .getAll()
      .then((data) => {
        if (!data) return;
        setProducts(
          data.map((p) => ({ id: p.productId, name: p.name, price: Number(p.purchasePrice) }))
        );
      })
      .catch(() => {});
  }, []);

  // ---------- stats ----------
  const count = (s) => arrivalList.filter((a) => a.status === s).length;
  const stats = [
    { label: 'Total arrivals', value: arrivalList.length, color: '#b9d8f3' },
    { label: 'Pending', value: count('PENDING'), color: '#8fb4e0' },
    { label: 'Confirmed', value: count('CONFIRMED'), color: '#4a9be0' },
    { label: 'Rejected', value: count('REJECTED'), color: '#f4b5ab' },
  ];

  // ---------- filter + paging ----------
  const filtered = arrivalList.filter((a) => filter === 'ALL' || a.status === filter);
  const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
  const currentPage = Math.min(page, totalPages);
  const start = (currentPage - 1) * pageSize;
  const pageRows = filtered.slice(start, start + pageSize);
  const from = filtered.length === 0 ? 0 : start + 1;
  const to = Math.min(start + pageSize, filtered.length);

  // ---------- New Arrival ----------
  const openNew = () => {
    setForm({ ...emptyForm, productId: products[0] ? String(products[0].id) : '' });
    setShowNew(true);
  };
  const selected = products.find((p) => String(p.id) === form.productId);
  const unitPrice = selected ? selected.price : 0;
  const totalCost = unitPrice * Number(form.quantity || 0);

  const handleCreate = async (e) => {
    e.preventDefault();
    if (!selected) return alert('Please select a product');
    try {
      await arrivalApi.create({
        productId: Number(form.productId),
        warehouseId: Number(form.warehouseId),
        quantity: Number(form.quantity),
        grade: form.grade,
        purchasePrice: unitPrice,
        expiryDate: form.expiryDate,
      });
      setShowNew(false);
      setFilter('ALL');
      setPage(1);
      loadArrivals();
    } catch (err) {
      alert(err.message || 'Failed to create arrival');
    }
  };

  // ---------- Quality Check ----------
  const openQC = (a) => {
    setShowQC(a);
    setChecks(CHECKS.map(() => false));
    setRejectReason('');
  };
  const allChecked = checks.every(Boolean);

  const handleConfirm = async () => {
    if (!showQC) return;
    try {
      await arrivalApi.confirm(showQC.id);
      setShowQC(null);
      loadArrivals();
    } catch (err) {
      alert(err.message || 'Failed to confirm arrival');
    }
  };

  const handleReject = async () => {
    if (!showQC) return;
    if (!rejectReason.trim()) {
      alert('Please provide a rejection reason.');
      return;
    }
    try {
      await arrivalApi.reject(showQC.id, rejectReason);
      setShowQC(null);
      setRejectReason('');
      loadArrivals();
    } catch (err) {
      alert(err.message || 'Failed to reject arrival');
    }
  };

  return (
    <div>
      <div className="ar-head">
        <button className="btn btn-primary" onClick={openNew}>+ New Arrival</button>
      </div>

      <div className="ar-stats">
        {stats.map((s) => (
          <div className="ar-stat" key={s.label}>
            <div className="ar-stat-icon" style={{ background: s.color }} />
            <div>
              <div className="ar-stat-label">{s.label}</div>
              <div className="ar-stat-value">{s.value}</div>
            </div>
          </div>
        ))}
      </div>

      <div className="pg-card">
        <h3 className="pg-card-title">Arrivals</h3>

        <div className="pg-pills">
          {FILTERS.map((f) => (
            <button
              key={f.key}
              className={`pg-pill ${filter === f.key ? 'active' : ''}`}
              onClick={() => { setFilter(f.key); setPage(1); }}
            >
              {f.label}
            </button>
          ))}
        </div>

        <table className="pg-table">
          <thead>
            <tr>
              <th>Batch No</th>
              <th>Product</th>
              <th>Warehouse</th>
              <th>Quantity</th>
              <th>Expiry Date</th>
              <th>Total Cost</th>
              <th>Status</th>
              <th>Quality Check</th>
            </tr>
          </thead>
          <tbody>
            {pageRows.map((a) => (
              <tr key={a.id}>
                <td>{a.batch}</td>
                <td>{a.product}</td>
                <td>{a.warehouse}</td>
                <td>{a.quantity} kg</td>
                <td>{formatDate(a.expiry)}</td>
                <td>{formatMoney(a.totalCost)}</td>
                <td>
                  <span className={`pill ${STATUS_CLASS[a.status] || 'pill-gray'}`}>
                    {STATUS_LABEL[a.status] || a.status}
                  </span>
                </td>
                <td>
                  {a.status === 'PENDING' ? (
                    <button className="qc-btn" onClick={() => openQC(a)}>
                      <ClipboardCheck size={16} /> Quality Check
                    </button>
                  ) : (
                    <span style={{ opacity: 0.4 }}>–</span>
                  )}
                </td>
              </tr>
            ))}
            {pageRows.length === 0 && (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center', padding: 30, opacity: 0.6 }}>
                  No arrivals found
                </td>
              </tr>
            )}
          </tbody>
        </table>

        {/* Paging */}
        <div className="pager">
          <span className="pager-info">
            Showing {from}–{to} of {filtered.length}
          </span>
          <div className="pager-controls">
            <select
              className="pager-size"
              value={pageSize}
              onChange={(e) => { setPageSize(Number(e.target.value)); setPage(1); }}
            >
              {PAGE_SIZES.map((n) => (
                <option key={n} value={n}>Rows: {n}</option>
              ))}
            </select>
            <button
              className="pager-btn"
              disabled={currentPage === 1}
              onClick={() => setPage(currentPage - 1)}
            >
              ‹ Prev
            </button>
            {Array.from({ length: totalPages }, (_, i) => i + 1).map((n) => (
              <button
                key={n}
                className={`pager-btn ${n === currentPage ? 'active' : ''}`}
                onClick={() => setPage(n)}
              >
                {n}
              </button>
            ))}
            <button
              className="pager-btn"
              disabled={currentPage === totalPages}
              onClick={() => setPage(currentPage + 1)}
            >
              Next ›
            </button>
          </div>
        </div>
      </div>

      {/* New Arrival modal */}
      {showNew && (
        <Modal title="New Arrival" onClose={() => setShowNew(false)}>
          <form onSubmit={handleCreate}>
            <div className="form-group">
              <label>Product</label>
              <select
                required
                value={form.productId}
                onChange={(e) => setForm({ ...form, productId: e.target.value })}
              >
                {products.map((p) => (
                  <option key={p.id} value={p.id}>{p.name}</option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label>Destination Warehouse</label>
              <select
                value={form.warehouseId}
                onChange={(e) => setForm({ ...form, warehouseId: e.target.value })}
              >
                {WAREHOUSES.map((w) => (
                  <option key={w.id} value={w.id}>{w.label}</option>
                ))}
              </select>
            </div>
            <div className="ar-row">
              <div className="form-group">
                <label>Quantity (kg)</label>
                <input
                  required type="number" min="1" value={form.quantity}
                  onChange={(e) => setForm({ ...form, quantity: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Grade</label>
                <select
                  value={form.grade}
                  onChange={(e) => setForm({ ...form, grade: e.target.value })}
                >
                  {GRADES.map((g) => <option key={g} value={g}>{g}</option>)}
                </select>
              </div>
            </div>
            <div className="form-group">
              <label>Expiry Date</label>
              <input
                required type="date" value={form.expiryDate}
                onChange={(e) => setForm({ ...form, expiryDate: e.target.value })}
              />
            </div>
            <div className="ar-row">
              <div className="form-group">
                <label>Purchase Price (auto)</label>
                <input readOnly className="ar-readonly" value={formatMoney(unitPrice)} />
              </div>
              <div className="form-group">
                <label>Total Cost</label>
                <input readOnly className="ar-readonly" value={formatMoney(totalCost)} />
              </div>
            </div>
            <p className="ar-batch">
              Batch No: <strong>{nextBatchNo(arrivalList)}</strong> · Status:{' '}
              <span className="pill pill-low">Pending</span>
            </p>
            <button type="submit" className="btn btn-primary ar-full">Save Arrival</button>
          </form>
        </Modal>
      )}

      {/* Quality Check modal */}
      {showQC && (
        <Modal title={`Quality Check – ${showQC.batch}`} onClose={() => setShowQC(null)}>
          {CHECKS.map((label, i) => (
            <label className="ar-check" key={label}>
              <input
                type="checkbox"
                checked={checks[i]}
                onChange={() => setChecks(checks.map((c, j) => (j === i ? !c : c)))}
              />
              {label}
            </label>
          ))}
          <p className="ar-hint">Confirm Arrival is enabled only when all items are ticked.</p>
          <div className="form-group">
            <label>Rejection reason</label>
            <textarea rows={3} value={rejectReason} onChange={(e) => setRejectReason(e.target.value)} />
          </div>
          <div className="ar-qc-actions">
            <button className="btn btn-primary" disabled={!allChecked} onClick={handleConfirm}>
              Confirm Arrival
            </button>
            <button className="btn btn-outline" onClick={handleReject}>
              Reject Arrival
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
}