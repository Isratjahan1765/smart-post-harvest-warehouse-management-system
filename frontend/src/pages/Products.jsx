import React, { useState, useEffect } from 'react';
import { Pencil, Trash2 } from 'lucide-react';
import Modal from '../components/Modal';
import { productApi } from '../api/endpoints';

const CATEGORIES = ['Grain', 'Vegetable', 'Fruit', 'Pulse', 'Others'];
const STATUSES = [
  { key: 'IN_STOCK', label: 'In Stock' },
  { key: 'LOW_STOCK', label: 'Low Stock' },
  { key: 'OUT_OF_STOCK', label: 'Out of Stock' },
  { key: 'NEAR_EXPIRY', label: 'Near Expiry' },
];
const CHART_COLORS = {
  Others: '#f4b27a',
  Fruit: '#3db5d8',
  Pulse: '#1f5fa8',
  Vegetable: '#5b9fe0',
  Grain: '#b9d8f3',
};

const normalizeStatus = (s) => String(s || '').toUpperCase().replace(/[\s-]+/g, '_');
const statusLabel = (key) =>
  STATUSES.find((x) => x.key === key)?.label || key.replace(/_/g, ' ');
const statusClass = (key) =>
  ({
    IN_STOCK: 'pill-in',
    LOW_STOCK: 'pill-low',
    OUT_OF_STOCK: 'pill-out',
    NEAR_EXPIRY: 'pill-near',
  }[key] || 'pill-gray');

const formatCategory = (c) =>
  c ? c.charAt(0).toUpperCase() + c.slice(1).toLowerCase() : 'Others';

// availableIn string ("A: 900 kg") ba object, duto-i handle kore
const normalizeStock = (item) => {
  if (typeof item === 'string') {
    const idx = item.indexOf(':');
    const rawLabel = idx > -1 ? item.slice(0, idx).trim() : item.trim();
    const label = rawLabel.replace(/^warehouse\s+/i, '');
    const rest = idx > -1 ? item.slice(idx + 1).trim() : '';
    return { label, text: rest ? `${label}: ${rest}` : label };
  }
  const raw = item.warehouseName || item.warehouse || item.name || item.warehouseId || '?';
  const label = String(raw).replace(/^warehouse\s+/i, '');
  const qty = item.quantity ?? item.qty ?? item.stock ?? 0;
  return { label, text: `${label}: ${Number(qty).toLocaleString()} kg` };
};

const effectiveStatus = (p) => {
  const status = normalizeStatus(p.status);
  if (status !== 'IN_STOCK' || !p.expiryDate) return status;
  const days = (new Date(p.expiryDate) - new Date()) / 86400000;
  return days <= 7 ? 'NEAR_EXPIRY' : status;
};

const emptyForm = {
  name: '', category: 'Grain', purchasePrice: '', sellingPrice: '',
  reorderLevel: '', warehouseId: '1', quantity: '0', entryDate: '', expiryDate: '',
};

export default function Products() {
  const [productList, setProductList] = useState([]);
  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [search, setSearch] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('All');
  const [statusFilter, setStatusFilter] = useState('All');
  const [warehouseFilter, setWarehouseFilter] = useState('All');
  const [form, setForm] = useState(emptyForm);

  const loadProducts = () => {
    productApi
      .getAll()
      .then((data) => {
        if (!data) return;
        setProductList(
          data.map((p) => ({
            id: p.productId,
            code: p.code || '',
            name: p.name || '',
            category: formatCategory(p.category),
            purchasePrice: Number(p.purchasePrice),
            sellingPrice: Number(p.sellingPrice),
            reorderLevel: p.reorderLevel ?? '',
            stocks: (p.availableIn || []).map(normalizeStock),
            expiryDate: p.expiryDate,
            status: effectiveStatus({ status: p.status, expiryDate: p.expiryDate }),
          }))
        );
      })
      .catch(() => {});
  };

  useEffect(() => {
    loadProducts();
  }, []);

  const openAdd = () => {
    setEditingId(null);
    setForm(emptyForm);
    setShowModal(true);
  };

  const openEdit = (p) => {
    setEditingId(p.id);
    setForm({
      ...emptyForm,
      name: p.name,
      category: p.category,
      purchasePrice: String(p.purchasePrice),
      sellingPrice: String(p.sellingPrice),
      reorderLevel: String(p.reorderLevel),
    });
    setShowModal(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    const payload = {
      name: form.name,
      category: form.category.toUpperCase(),
      purchasePrice: Number(form.purchasePrice),
      sellingPrice: Number(form.sellingPrice),
      reorderLevel: Number(form.reorderLevel || 0),
      warehouseId: Number(form.warehouseId),
      quantity: Number(form.quantity || 0),
      entryDate: form.entryDate || new Date().toISOString().split('T')[0],
      expiryDate:
        form.expiryDate ||
        new Date(Date.now() + 30 * 86400000).toISOString().split('T')[0],
    };
    try {
      if (editingId) {
        if (!productApi.update) throw new Error('productApi.update endpoints.js-e nei');
        await productApi.update(editingId, payload);
      } else {
        await productApi.create(payload);
      }
      setShowModal(false);
      loadProducts();
    } catch (err) {
      alert(err.message || 'Failed to save product');
    }
  };

  const handleDelete = async (p) => {
    if (!window.confirm(`Delete "${p.name}"?`)) return;
    const del = productApi.remove || productApi.delete;
    if (!del) {
      alert('productApi.remove / delete endpoints.js-e nei');
      return;
    }
    try {
      await del(p.id);
      loadProducts();
    } catch (err) {
      alert(err.message || 'Failed to delete product');
    }
  };

  const warehouseOptions = [
    ...new Set(productList.flatMap((p) => p.stocks.map((s) => s.label))),
  ].sort();

  const filtered = productList.filter((p) => {
    const q = search.toLowerCase();
    const matchSearch = p.code.toLowerCase().includes(q) || p.name.toLowerCase().includes(q);
    const matchCat = categoryFilter === 'All' || p.category === categoryFilter;
    const matchStatus = statusFilter === 'All' || p.status === statusFilter;
    const matchWh =
      warehouseFilter === 'All' || p.stocks.some((s) => s.label === warehouseFilter);
    return matchSearch && matchCat && matchStatus && matchWh;
  });

  // Chart data: choto theke boro, upor theke niche
  const counts = CATEGORIES.map((c) => ({
    name: c,
    count: productList.filter((p) => p.category === c).length,
  })).sort((a, b) => a.count - b.count);
  const maxCount = Math.max(1, ...counts.map((c) => c.count));

  return (
    <div>
      {/* Filter card */}
      <div className="pg-card" style={{ marginBottom: 24 }}>
        <div className="pg-filter-top">
          <input
            className="pg-search"
            placeholder="Search by Product ID"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <select
            className="pg-select"
            value={warehouseFilter}
            onChange={(e) => setWarehouseFilter(e.target.value)}
          >
            <option value="All">All Warehouses</option>
            {warehouseOptions.map((w) => (
              <option key={w} value={w}>Warehouse {w}</option>
            ))}
          </select>
          <button className="btn btn-primary" style={{ marginLeft: 'auto' }} onClick={openAdd}>
            + Add Product
          </button>
        </div>

        <div className="pg-pills">
          {['All', ...CATEGORIES].map((c) => (
            <button
              key={c}
              className={`pg-pill ${categoryFilter === c ? 'active' : ''}`}
              onClick={() => setCategoryFilter(c)}
            >
              {c}
            </button>
          ))}
        </div>

        <div className="pg-pills">
          {[{ key: 'All', label: 'All' }, ...STATUSES].map((s) => (
            <button
              key={s.key}
              className={`pg-pill ${statusFilter === s.key ? 'active' : ''}`}
              onClick={() => setStatusFilter(s.key)}
            >
              {s.label}
            </button>
          ))}
        </div>
      </div>

      {/* Table + chart */}
      <div className="pg-grid">
        <div className="pg-card">
          <table className="pg-table">
            <thead>
              <tr>
                <th>Code</th>
                <th>Name</th>
                <th>Category</th>
                <th>Purchase</th>
                <th>Selling</th>
                <th>Available In</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((p) => (
                <tr key={p.id}>
                  <td>{p.code}</td>
                  <td><strong>{p.name}</strong></td>
                  <td>{p.category}</td>
                  <td>Tk {p.purchasePrice}</td>
                  <td>Tk {p.sellingPrice}</td>
                  <td>
                    {p.stocks.length ? p.stocks.map((s) => s.text).join(', ') : 'Not available'}
                  </td>
                  <td>
                    <span className={`pill ${statusClass(p.status)}`}>
                      {statusLabel(p.status)}
                    </span>
                  </td>
                  <td className="pg-actions">
    <button
    type="button"
    className="icon-btn"
    title="Edit"
    aria-label={`Edit ${p.name}`}
    onClick={() => openEdit(p)}
  >
    <Pencil size={18} />
  </button>
  <button
    type="button"
    className="icon-btn icon-btn-danger"
    title="Delete"
    aria-label={`Delete ${p.name}`}
    onClick={() => handleDelete(p)}
  >
    <Trash2 size={18} />
  </button>
</td>
                </tr>
              ))}
              {filtered.length === 0 && (
                <tr>
                  <td colSpan={8} style={{ textAlign: 'center', padding: 30, opacity: 0.6 }}>
                    No products found
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="pg-card">
          <h3 className="pg-card-title">Products by Category</h3>
          <div className="pg-funnel">
            {counts.map((c) => (
              <div className="pg-funnel-row" key={c.name}>
                <div className="pg-funnel-bar-wrap">
                  <div
                    className="pg-funnel-bar"
                    title={`${c.name}: ${c.count}`}
                    style={{
                      width: `${35 + 65 * (c.count / maxCount)}%`,
                      background: CHART_COLORS[c.name],
                    }}
                  />
                </div>
                <span className="pg-funnel-label">{c.name} ({c.count})</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Add / Edit modal */}
      {showModal && (
        <Modal
          title={editingId ? 'Edit Product' : 'Add New Product'}
          onClose={() => setShowModal(false)}
        >
          <form onSubmit={handleSave}>
            <div className="form-group">
              <label>Product Name</label>
              <input required value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                placeholder="e.g. Miniket Rice" />
            </div>
            <div className="form-group">
              <label>Category</label>
              <select value={form.category}
                onChange={(e) => setForm({ ...form, category: e.target.value })}>
                {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label>Purchase Price (Tk/kg)</label>
              <input required type="number" value={form.purchasePrice}
                onChange={(e) => setForm({ ...form, purchasePrice: e.target.value })} />
            </div>
            <div className="form-group">
              <label>Selling Price (Tk/kg)</label>
              <input required type="number" value={form.sellingPrice}
                onChange={(e) => setForm({ ...form, sellingPrice: e.target.value })} />
            </div>
            <div className="form-group">
              <label>Reorder Level (kg)</label>
              <input type="number" value={form.reorderLevel}
                onChange={(e) => setForm({ ...form, reorderLevel: e.target.value })} />
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 20 }}>
              <button type="button" className="btn btn-secondary"
                onClick={() => setShowModal(false)}>Cancel</button>
              <button type="submit" className="btn btn-primary">Save Product</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}