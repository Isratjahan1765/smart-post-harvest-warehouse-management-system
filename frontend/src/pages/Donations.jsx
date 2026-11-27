import React, { useState, useEffect } from 'react';
import { Search, Gift, Printer } from 'lucide-react';
import * as XLSX from 'xlsx';
import Modal from '../components/Modal';
import { donationApi } from '../api/endpoints';

const PAGE_SIZES = [5, 10, 20];
const RECEIVER_TYPES = ['Orphanage', 'NGO', 'Madrasa', 'Other'];

// Venn diagram: 3 ta circle, prottek region-er label position
const CIRCLES = [
  { cx: 140, cy: 130, color: '#5b9fe0' },
  { cx: 260, cy: 130, color: '#3db5d8' },
  { cx: 200, cy: 235, color: '#f4b27a' },
];
const NAME_POS = [[130, 16], [270, 16], [200, 366]];
const REGION_POS = {
  '0': [96, 112], '1': [304, 112], '2': [200, 288],
  '01': [200, 92], '02': [146, 204], '12': [254, 204], '012': [200, 164],
};

const fmtDay = (iso) =>
  iso
    ? new Date(`${String(iso).split('T')[0]}T00:00:00`).toLocaleDateString('en-GB', {
        day: '2-digit', month: 'short',
      })
    : '-';
const esc = (s) => String(s ?? '').replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));

const emptyForm = { batchId: '', quantity: '', receiverId: '', conditionNote: '' };
const emptyReceiver = { name: '', type: 'Orphanage', phone: '' };

export default function Donations() {
  const [donationList, setDonationList] = useState([]);
  const [eligibleList, setEligibleList] = useState([]);
  const [receivers, setReceivers] = useState([]);

  const [search, setSearch] = useState('');
  const [receiverFilter, setReceiverFilter] = useState('All');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const [showAdd, setShowAdd] = useState(false);
  const [confirmed, setConfirmed] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [showReceiver, setShowReceiver] = useState(false);
  const [receiverForm, setReceiverForm] = useState(emptyReceiver);

  const loadData = () => {
    donationApi
      .getEligible()
      .then((data) => {
        if (!data) return;
        setEligibleList(
          data.map((d) => ({
            batchId: d.batchId,
            product: d.productName,
            batch: d.batchNo,
            warehouse: d.warehouseName,
            qty: Number(d.remaining),
            expiry: d.expiryDate,
            days: Number(d.daysUntilExpiry),
          }))
        );
      })
      .catch(() => {});

    donationApi
      .getAll()
      .then((data) => {
        if (!data) return;
        setDonationList(
          data.map((d) => ({
            id: d.donationId,
            no: d.donationNo,
            receiver: d.receiverName,
            receiverId: d.receiverId,
            receiverPhone: d.receiverPhone,
            product: d.productName,
            qty: Number(d.quantity),
            dateRaw: String(d.donationDate || '').split('T')[0],
          }))
        );
      })
      .catch(() => {});

    donationApi
      .getReceivers()
      .then((data) => data && setReceivers(data))
      .catch(() => {});
  };

  useEffect(() => {
    loadData();
  }, []);

  const phoneOf = (d) =>
    d.receiverPhone || receivers.find((r) => r.name === d.receiver)?.phone || '-';

  // ---------- history: filter + paging ----------
  const q = search.toLowerCase();
  const filtered = donationList.filter((d) => {
    const matchSearch =
      (d.receiver || '').toLowerCase().includes(q) ||
      (d.product || '').toLowerCase().includes(q) ||
      (d.no || '').toLowerCase().includes(q);
    const matchReceiver = receiverFilter === 'All' || d.receiver === receiverFilter;
    const matchFrom = !fromDate || d.dateRaw >= fromDate;
    const matchTo = !toDate || d.dateRaw <= toDate;
    return matchSearch && matchReceiver && matchFrom && matchTo;
  });
  const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
  const currentPage = Math.min(page, totalPages);
  const start = (currentPage - 1) * pageSize;
  const pageRows = filtered.slice(start, start + pageSize);
  const from = filtered.length === 0 ? 0 : start + 1;
  const to = Math.min(start + pageSize, filtered.length);
  const resetPage = () => setPage(1);

  // ---------- Donated by Receiver (Venn + legend) ----------
  const statMap = {};
  donationList.forEach((d) => {
    const r = statMap[d.receiver] || (statMap[d.receiver] = { name: d.receiver, kg: 0, products: new Set() });
    r.kg += d.qty;
    r.products.add(d.product);
  });
  const stats = Object.values(statMap).sort((a, b) => b.kg - a.kg);
  const totalKg = stats.reduce((s, r) => s + r.kg, 0);
  const top3 = stats.slice(0, 3); // Venn-e shudhu shobcheye boro 3 receiver

  const regions = {};
  [...new Set(top3.flatMap((r) => [...r.products]))].forEach((product) => {
    const key = top3.map((r, i) => (r.products.has(product) ? i : '')).join('');
    (regions[key] = regions[key] || []).push(product);
  });

  // ---------- Add Donation ----------
  const openAdd = (batchId) => {
    setForm({
      ...emptyForm,
      batchId: String(batchId ?? eligibleList[0]?.batchId ?? ''),
      receiverId: receivers[0] ? String(receivers[0].receiverId) : '',
    });
    setConfirmed(false);
    setShowAdd(true);
  };
  const selectedBatch = eligibleList.find((b) => String(b.batchId) === form.batchId);
  const maxQty = selectedBatch ? selectedBatch.qty : 0;

  const handleAdd = async (e) => {
    e.preventDefault();
    if (!confirmed) return;
    if (Number(form.quantity) > maxQty) {
      alert(`Quantity cannot be more than ${maxQty} kg`);
      return;
    }
    try {
      await donationApi.create({
        receiverId: Number(form.receiverId),
        batchId: Number(form.batchId),
        quantity: Number(form.quantity),
        conditionNote: form.conditionNote,
        safeConfirmed: true,
        donationDate: new Date().toISOString().split('T')[0],
      });
      setShowAdd(false);
      setConfirmed(false);
      loadData();
    } catch (err) {
      alert(err.message || 'Donation creation failed');
    }
  };

  const handleQuickReceiver = async (e) => {
    e.preventDefault();
    if (!donationApi.createReceiver) {
      alert('donationApi.createReceiver endpoints.js-e nei');
      return;
    }
    try {
      await donationApi.createReceiver(receiverForm);
      setShowReceiver(false);
      setReceiverForm(emptyReceiver);
      loadData();
    } catch (err) {
      alert(err.message || 'Failed to add receiver');
    }
  };

  // ---------- Receipt print & Export ----------
  const printReceipt = (d) => {
    const w = window.open('', '_blank', 'width=480,height=640');
    if (!w) return alert('Please allow pop-ups to print the receipt');
    w.document.write(`<html><head><title>${esc(d.no)}</title>
      <style>body{font-family:sans-serif;padding:32px}h2{margin:0 0 4px}
      table{width:100%;border-collapse:collapse;margin-top:18px}
      td{padding:8px 0;border-bottom:1px solid #ddd}td:first-child{color:#666;width:40%}</style></head>
      <body><h2>SPHWMS</h2><div>Donation Receipt</div>
      <table>
        <tr><td>Donation No</td><td>${esc(d.no)}</td></tr>
        <tr><td>Date</td><td>${esc(d.dateRaw)}</td></tr>
        <tr><td>Product</td><td>${esc(d.product)}</td></tr>
        <tr><td>Quantity</td><td>${esc(d.qty)} kg</td></tr>
        <tr><td>Receiver</td><td>${esc(d.receiver)}</td></tr>
        <tr><td>Phone</td><td>${esc(phoneOf(d))}</td></tr>
      </table></body></html>`);
    w.document.close();
    w.focus();
    w.print();
  };

  const exportXlsx = () => {
    const rows = filtered.map((d) => ({
      'Donation No': d.no,
      Date: d.dateRaw,
      Product: d.product,
      'Qty (kg)': d.qty,
      Receiver: d.receiver,
      Phone: phoneOf(d),
    }));
    const ws = XLSX.utils.json_to_sheet(rows);
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Donations');
    XLSX.writeFile(wb, 'donation-history.xlsx');
  };

  return (
    <div>
      <div className="ar-head">
        <button className="btn btn-primary" onClick={() => openAdd()}>+ Add Donation</button>
      </div>

      {/* Search */}
      <div className="pg-card" style={{ marginBottom: 24 }}>
        <div className="dn-searchbox">
          <Search size={20} />
          <input
            className="dn-search"
            placeholder="Search by product, receiver or donation no"
            value={search}
            onChange={(e) => { setSearch(e.target.value); resetPage(); }}
          />
        </div>
      </div>

      <div className="dn-grid">
        {/* Eligible */}
        <div className="pg-card">
          <h3 className="pg-card-title">Eligible for Donation (1–2 days to expiry)</h3>
          <table className="pg-table">
            <thead>
              <tr>
                <th>Product</th><th>Batch</th><th>Warehouse</th>
                <th>Qty</th><th>Expiry</th><th>Left</th><th></th>
              </tr>
            </thead>
            <tbody>
              {eligibleList.map((b) => (
                <tr key={b.batchId}>
                  <td>{b.product}</td>
                  <td>{b.batch}</td>
                  <td>{b.warehouse}</td>
                  <td>{b.qty} kg</td>
                  <td>{fmtDay(b.expiry)}</td>
                  <td>
                    <span className={`pill ${b.days <= 1 ? 'pill-near' : 'pill-low'}`}>
                      {b.days} {b.days === 1 ? 'day' : 'days'}
                    </span>
                  </td>
                  <td>
                    <button
                      className="dn-icon-btn" title="Donate this batch"
                      aria-label={`Donate ${b.product}`} onClick={() => openAdd(b.batchId)}
                    >
                      <Gift size={18} />
                    </button>
                  </td>
                </tr>
              ))}
              {eligibleList.length === 0 && (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: 30, opacity: 0.6 }}>
                    No batches close to expiry
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Donated by Receiver */}
        <div className="pg-card">
          <h3 className="pg-card-title">Donated by Receiver</h3>
          {top3.length === 0 ? (
            <p style={{ opacity: 0.6 }}>No donations yet</p>
          ) : (
            <>
              <svg className="dn-venn" viewBox="0 0 400 376">
                {top3.map((r, i) => (
                  <circle
                    key={r.name} cx={CIRCLES[i].cx} cy={CIRCLES[i].cy} r="105"
                    fill={CIRCLES[i].color} fillOpacity="0.45"
                    stroke={CIRCLES[i].color} strokeWidth="2"
                  />
                ))}
                {top3.map((r, i) => (
                  <text
                    key={`n-${r.name}`} x={NAME_POS[i][0]} y={NAME_POS[i][1]}
                    textAnchor="middle" fontSize="15" fontWeight="700" fill="#14284b"
                  >
                    {r.name}
                  </text>
                ))}
                {Object.entries(regions).map(([key, names]) => {
                  const [x, y] = REGION_POS[key] || [200, 180];
                  return (
                    <text key={key} x={x} y={y - ((names.length - 1) * 7)} textAnchor="middle"
                      fontSize="12" fontWeight="700" fill="#14284b">
                      {names.map((n, i) => (
                        <tspan key={n} x={x} dy={i === 0 ? 0 : 14}>{n}</tspan>
                      ))}
                    </text>
                  );
                })}
              </svg>
              <ul className="dn-legend">
                {stats.map((r, i) => (
                  <li key={r.name}>
                    <span className="dn-dot" style={{ background: (CIRCLES[i] || { color: '#9aa7ba' }).color }} />
                    <span>
                      {r.name} <strong>{Math.round((r.kg / totalKg) * 100)}%</strong>{' '}
                      <span style={{ opacity: 0.7 }}>({r.kg} kg)</span>
                    </span>
                  </li>
                ))}
              </ul>
            </>
          )}
        </div>
      </div>

      {/* History */}
      <div className="pg-card">
        <h3 className="pg-card-title">Donation History</h3>
        <div className="dn-filters">
          <div>
            <label>Receiver</label>
            <select value={receiverFilter} onChange={(e) => { setReceiverFilter(e.target.value); resetPage(); }}>
              <option value="All">All receivers</option>
              {[...new Set(donationList.map((d) => d.receiver))].sort().map((r) => (
                <option key={r} value={r}>{r}</option>
              ))}
            </select>
          </div>
          <div>
            <label>From</label>
            <input type="date" value={fromDate} onChange={(e) => { setFromDate(e.target.value); resetPage(); }} />
          </div>
          <div>
            <label>To</label>
            <input type="date" value={toDate} onChange={(e) => { setToDate(e.target.value); resetPage(); }} />
          </div>
          <button className="btn btn-outline dn-export" onClick={exportXlsx}>Export XLSX</button>
        </div>

        <table className="pg-table">
          <thead>
            <tr>
              <th>No</th><th>Date</th><th>Product</th><th>Qty</th>
              <th>Receiver</th><th>Phone</th><th>Receipt</th>
            </tr>
          </thead>
          <tbody>
            {pageRows.map((d) => (
              <tr key={d.id ?? d.no}>
                <td>{d.no}</td>
                <td>{fmtDay(d.dateRaw)}</td>
                <td>{d.product}</td>
                <td>{d.qty} kg</td>
                <td>{d.receiver}</td>
                <td>{phoneOf(d)}</td>
                <td>
                  <button className="dn-icon-btn" title="Print receipt"
                    aria-label={`Print receipt ${d.no}`} onClick={() => printReceipt(d)}>
                    <Printer size={18} />
                  </button>
                </td>
              </tr>
            ))}
            {pageRows.length === 0 && (
              <tr>
                <td colSpan={7} style={{ textAlign: 'center', padding: 30, opacity: 0.6 }}>
                  No donations found
                </td>
              </tr>
            )}
          </tbody>
        </table>

        <div className="pager">
          <span className="pager-info">Showing {from}–{to} of {filtered.length}</span>
          <div className="pager-controls">
            <select className="pager-size" value={pageSize}
              onChange={(e) => { setPageSize(Number(e.target.value)); setPage(1); }}>
              {PAGE_SIZES.map((n) => <option key={n} value={n}>Rows: {n}</option>)}
            </select>
            <button className="pager-btn" disabled={currentPage === 1}
              onClick={() => setPage(currentPage - 1)}>‹ Prev</button>
            {Array.from({ length: totalPages }, (_, i) => i + 1).map((n) => (
              <button key={n} className={`pager-btn ${n === currentPage ? 'active' : ''}`}
                onClick={() => setPage(n)}>{n}</button>
            ))}
            <button className="pager-btn" disabled={currentPage === totalPages}
              onClick={() => setPage(currentPage + 1)}>Next ›</button>
          </div>
        </div>
      </div>

      {/* Add Donation modal */}
      {showAdd && !showReceiver && (
        <Modal title="Add Donation" onClose={() => setShowAdd(false)}>
          <form onSubmit={handleAdd}>
            <div className="form-group">
              <label>Item (batch)</label>
              <select required value={form.batchId}
                onChange={(e) => setForm({ ...form, batchId: e.target.value })}>
                {eligibleList.map((b) => (
                  <option key={b.batchId} value={b.batchId}>
                    {b.product} {b.batch} · {b.warehouse} · {b.qty} kg
                  </option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label>Quantity (max {maxQty} kg)</label>
              <input required type="number" min="1" max={maxQty} value={form.quantity}
                onChange={(e) => setForm({ ...form, quantity: e.target.value })} />
            </div>
            <div className="form-group">
              <label>Receiver</label>
              <select required value={form.receiverId}
                onChange={(e) => setForm({ ...form, receiverId: e.target.value })}>
                {receivers.map((r) => (
                  <option key={r.receiverId} value={r.receiverId}>{r.name}</option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label>Food condition note</label>
              <textarea rows={3} value={form.conditionNote}
                onChange={(e) => setForm({ ...form, conditionNote: e.target.value })}
                placeholder="e.g. Safe to eat, packaging ok" />
            </div>
            <label className="ar-check">
              <input type="checkbox" checked={confirmed}
                onChange={(e) => setConfirmed(e.target.checked)} />
              I confirm this food is safe to eat
            </label>
            <div className="ar-qc-actions">
              <button type="submit" className="btn btn-primary" disabled={!confirmed}>
                Confirm Donation
              </button>
              <button type="button" className="btn btn-outline" onClick={() => setShowReceiver(true)}>
                Quick Add Receiver
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Quick Add Receiver modal */}
      {showReceiver && (
        <Modal title="Quick Add Receiver" onClose={() => setShowReceiver(false)}>
          <form onSubmit={handleQuickReceiver}>
            <div className="form-group">
              <label>Name</label>
              <input required value={receiverForm.name}
                onChange={(e) => setReceiverForm({ ...receiverForm, name: e.target.value })} />
            </div>
            <div className="form-group">
              <label>Type</label>
              <select value={receiverForm.type}
                onChange={(e) => setReceiverForm({ ...receiverForm, type: e.target.value })}>
                {RECEIVER_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label>Phone</label>
              <input required value={receiverForm.phone}
                onChange={(e) => setReceiverForm({ ...receiverForm, phone: e.target.value })} />
            </div>
            <div className="ar-qc-actions">
              <button type="submit" className="btn btn-primary">Save Receiver</button>
              <button type="button" className="btn btn-outline" onClick={() => setShowReceiver(false)}>
                Back
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
