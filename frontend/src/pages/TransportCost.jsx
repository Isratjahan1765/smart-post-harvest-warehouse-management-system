import React, { useState, useEffect } from 'react';
import Modal from '../components/Modal';
import { tripCostApi } from '../api/endpoints';

const PAGE_SIZE = 5;
const BAR_COLORS = ['#bddcf7', '#5b9fe0', '#1f5ea8', '#3bb3d6', '#f0b27a', '#bddcf7'];
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

const money = (n) => `Tk ${Number(n || 0).toLocaleString('en-US')}`;

const fmtDate = (d) => {
  if (!d) return '-';
  const dt = new Date(d);
  if (isNaN(dt)) return String(d);
  return `${String(dt.getDate()).padStart(2, '0')} ${MONTHS[dt.getMonth()]}`;
};

const mapRow = (c) => {
  const fuel = Number(c.fuelCost || 0);
  const driverCost = Number(c.driverCost || 0);
  const other = Number(c.otherCost || 0);
  return {
    id: c.tripCostId,
    tripId: c.tripId,
    trip: c.tripNo || '',
    date: c.tripDate,
    driver: c.driverName || '',
    route: `${c.origin || 'Warehouse'} – ${c.destination || 'Destination'}`,
    fuel,
    driverCost,
    other,
    total: Number(c.totalCost ?? fuel + driverCost + other),
    note: c.note || '',
  };
};

// Backend plain array dile array, { content | items | data, totalElements } dile server paging
const readList = (res) => {
  if (Array.isArray(res)) return { list: res, total: res.length, server: false };
  const list = res?.content ?? res?.items ?? res?.data ?? [];
  return { list, total: res?.totalElements ?? res?.total ?? list.length, server: true };
};

// y-axis-er jonno sundor step (1, 2, 5 x 10^k)
const niceStep = (max) => {
  const raw = max / 4;
  const pow = Math.pow(10, Math.floor(Math.log10(raw || 1)));
  const n = raw / pow;
  const f = n <= 1 ? 1 : n <= 2 ? 2 : n <= 5 ? 5 : 10;
  return f * pow;
};

const fmtTick = (v) => (v >= 1000 ? `${v / 1000}K` : String(v));

const emptyForm = { tripId: '', fuelCost: '', driverCost: '', otherCost: '0', note: '' };

export default function TransportCost() {
  const [monthly, setMonthly] = useState([]);
  const [rows, setRows] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [searchInput, setSearchInput] = useState('');
  const [q, setQ] = useState('');
  const [reloadKey, setReloadKey] = useState(0);
  const [availableTrips, setAvailableTrips] = useState([]);
  const [showAdd, setShowAdd] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [editing, setEditing] = useState(null);
  const [editForm, setEditForm] = useState(emptyForm);

  const reload = () => setReloadKey((k) => k + 1);

  // search debounce
  useEffect(() => {
    const t = setTimeout(() => {
      setQ(searchInput.trim());
      setPage(0);
    }, 300);
    return () => clearTimeout(t);
  }, [searchInput]);

  // Header-er "+ Add Trip Cost" button ei event pathabe
  useEffect(() => {
    const open = () => setShowAdd(true);
    window.addEventListener('open-add-trip-cost', open);
    return () => window.removeEventListener('open-add-trip-cost', open);
  }, []);

  // chart + available trips
  useEffect(() => {
    tripCostApi.getMonthly()
      .then((data) => setMonthly((data || []).map((d) => ({ month: d.month, cost: Number(d.total) }))))
      .catch(() => setMonthly([]));
    tripCostApi.getAvailableTrips()
      .then((data) => {
        setAvailableTrips(data || []);
        if (data && data[0]) setForm((f) => (f.tripId ? f : { ...f, tripId: data[0].tripId }));
      })
      .catch(() => setAvailableTrips([]));
  }, [reloadKey]);

  // table list
  useEffect(() => {
    let cancelled = false;
    tripCostApi.getAll({ q: q || undefined, page, size: PAGE_SIZE })
      .then((res) => {
        if (cancelled) return;
        const { list, total: t, server } = readList(res);
        const mapped = list.map(mapRow);
        if (server) {
          setRows(mapped);
          setTotal(t);
        } else {
          const needle = q.toLowerCase();
          const filtered = needle
            ? mapped.filter((r) => r.trip.toLowerCase().includes(needle) || r.driver.toLowerCase().includes(needle))
            : mapped;
          setTotal(filtered.length);
          setRows(filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE));
        }
      })
      .catch(() => {
        if (!cancelled) { setRows([]); setTotal(0); }
      });
    return () => { cancelled = true; };
  }, [q, page, reloadKey]);

  // ---- actions ----
  const handleAdd = async (e) => {
    e.preventDefault();
    if (!form.tripId) { alert('Please select a trip.'); return; }
    try {
      await tripCostApi.create({
        tripId: Number(form.tripId),
        fuelCost: Number(form.fuelCost),
        driverCost: Number(form.driverCost),
        otherCost: Number(form.otherCost || 0),
        note: form.note,
      });
      setShowAdd(false);
      setForm({ ...emptyForm, tripId: '' });
      setPage(0);
      reload();
    } catch (err) {
      alert(err.message || 'Failed to add trip cost');
    }
  };

  const openEdit = (r) => {
    setEditing(r);
    setEditForm({ tripId: r.tripId, fuelCost: r.fuel, driverCost: r.driverCost, otherCost: r.other, note: r.note });
  };

  const handleEdit = async (e) => {
    e.preventDefault();
    try {
      await tripCostApi.update(editing.id, {
        tripId: editing.tripId,
        fuelCost: Number(editForm.fuelCost),
        driverCost: Number(editForm.driverCost),
        otherCost: Number(editForm.otherCost || 0),
        note: editForm.note,
      });
      setEditing(null);
      reload();
    } catch (err) {
      alert(err.message || 'Failed to update trip cost');
    }
  };

  const handleDelete = async (r) => {
    if (!window.confirm(`Delete cost for trip ${r.trip}?`)) return;
    try {
      await tripCostApi.remove(r.id);
      if (rows.length === 1 && page > 0) setPage(page - 1);
      reload();
    } catch (err) {
      alert(err.message || 'Failed to delete trip cost');
    }
  };

  const handleExport = async () => {
    try {
      const res = await tripCostApi.getAll({ q: q || undefined, page: 0, size: 100000 });
      const all = readList(res).list.map(mapRow);
      const head = ['Trip', 'Date', 'Driver', 'Route', 'Fuel', 'Driver Cost', 'Other', 'Total'];
      const esc = (v) => `"${String(v ?? '').replace(/"/g, '""')}"`;
      const lines = [head.map(esc).join(',')].concat(
        all.map((r) => [r.trip, r.date, r.driver, r.route, r.fuel, r.driverCost, r.other, r.total].map(esc).join(','))
      );
      const blob = new Blob(['\ufeff' + lines.join('\n')], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'trip-costs.csv';
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      alert(err.message || 'Export failed');
    }
  };

  // ---- chart ----
  const maxCost = Math.max(...monthly.map((m) => m.cost), 1);
  const step = niceStep(maxCost);
  const niceMax = Math.ceil(maxCost / step) * step;
  const ticks = [];
  for (let v = 0; v <= niceMax + 0.0001; v += step) ticks.push(v);

  // ---- pagination ----
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));
  const winStart = Math.max(0, Math.min(page - 1, pageCount - 4));
  const pageNums = Array.from({ length: Math.min(4, pageCount) }, (_, i) => winStart + i);
  const from = total === 0 ? 0 : page * PAGE_SIZE + 1;
  const to = Math.min((page + 1) * PAGE_SIZE, total);

  const costFields = (f, setF) => (
    <>
      <div className="form-group">
        <label>Fuel Cost (Tk)</label>
        <input required type="number" value={f.fuelCost} onChange={(e) => setF({ ...f, fuelCost: e.target.value })} placeholder="e.g. 3500" />
      </div>
      <div className="form-group">
        <label>Driver Cost / Allowance (Tk)</label>
        <input required type="number" value={f.driverCost} onChange={(e) => setF({ ...f, driverCost: e.target.value })} placeholder="e.g. 1200" />
      </div>
      <div className="form-group">
        <label>Other / Toll Cost (Tk)</label>
        <input type="number" value={f.otherCost} onChange={(e) => setF({ ...f, otherCost: e.target.value })} placeholder="e.g. 400" />
      </div>
      <div className="form-group">
        <label>Note</label>
        <input value={f.note} onChange={(e) => setF({ ...f, note: e.target.value })} placeholder="Optional trip notes..." />
      </div>
    </>
  );

  return (
    <div className="tc-page">
      {/* Monthly chart */}
      <section className="tc-card">
        <h3 className="tc-card-title">Transport Cost by Month</h3>
        <div className="tc-chart">
          <div className="tc-plot">
            {ticks.map((t) => (
              <div key={t} className="tc-grid" style={{ bottom: `${(t / niceMax) * 100}%` }}>
                <span className="tc-tick">{fmtTick(t)}</span>
              </div>
            ))}
            <div className="tc-cols" style={{ gridTemplateColumns: `repeat(${Math.max(monthly.length, 1)}, 1fr)` }}>
              {monthly.map((m, i) => {
                const pct = (m.cost / niceMax) * 100;
                return (
                  <div className="tc-col" key={m.month}>
                    <span className="tc-val" style={{ bottom: `calc(${pct}% + 8px)` }}>
                      Tk {(m.cost / 1000).toFixed(1)}K
                    </span>
                    <div className="tc-bar" style={{ height: `${pct}%`, background: BAR_COLORS[i % BAR_COLORS.length] }} />
                  </div>
                );
              })}
            </div>
          </div>
          <div className="tc-months" style={{ gridTemplateColumns: `repeat(${Math.max(monthly.length, 1)}, 1fr)` }}>
            {monthly.map((m) => <span key={m.month}>{m.month}</span>)}
          </div>
        </div>
      </section>

      {/* Trip costs table */}
      <section className="tc-card">
        <h3 className="tc-card-title">Trip Costs</h3>
        <div className="tc-toolbar">
          <label className="tc-search">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round">
              <circle cx="11" cy="11" r="7" /><path d="M20 20l-3.5-3.5" />
            </svg>
            <input placeholder="Trip no. or driver" value={searchInput} onChange={(e) => setSearchInput(e.target.value)} />
          </label>
          <button className="tc-export" onClick={handleExport}>Export XLSX</button>
        </div>

        <div className="tc-table-wrap">
          <table className="tc-table">
            <thead>
              <tr>
                <th>Trip</th><th>Date</th><th>Driver</th><th>Route</th>
                <th>Fuel</th><th>Driver Cost</th><th>Other</th><th>Total</th><th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {rows.length === 0 && (
                <tr><td colSpan={9} className="tc-empty">No trip costs found.</td></tr>
              )}
              {rows.map((r) => (
                <tr key={r.id ?? r.trip}>
                  <td>{r.trip}</td>
                  <td>{fmtDate(r.date)}</td>
                  <td>{r.driver}</td>
                  <td>{r.route}</td>
                  <td>{money(r.fuel)}</td>
                  <td>{money(r.driverCost)}</td>
                  <td>{money(r.other)}</td>
                  <td><strong>{money(r.total)}</strong></td>
                  <td>
                    <div className="tc-actions">
                      <button className="tc-icon tc-edit" title="Edit" onClick={() => openEdit(r)}>
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                          <path d="M12 20h9" /><path d="M16.5 3.5a2.1 2.1 0 013 3L7 19l-4 1 1-4 12.5-12.5z" />
                        </svg>
                      </button>
                      <button className="tc-icon tc-del" title="Delete" onClick={() => handleDelete(r)}>
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                          <path d="M3 6h18" /><path d="M8 6V4h8v2" /><path d="M19 6l-1 14H6L5 6" /><path d="M10 11v6M14 11v6" />
                        </svg>
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="tc-footer">
          <span className="tc-showing">Showing {from}–{to} of {total}</span>
          <div className="tc-pager">
            <button className="tc-pg" disabled={page === 0} onClick={() => setPage(page - 1)}>‹</button>
            {pageNums.map((p) => (
              <button key={p} className={`tc-pg${p === page ? ' active' : ''}`} onClick={() => setPage(p)}>{p + 1}</button>
            ))}
            <button className="tc-pg" disabled={page >= pageCount - 1} onClick={() => setPage(page + 1)}>›</button>
          </div>
        </div>
      </section>

      {/* Add modal */}
      {showAdd && (
        <Modal title="Record Trip Cost" onClose={() => setShowAdd(false)}>
          <form onSubmit={handleAdd}>
            <div className="form-group">
              <label>Select Delivered Trip</label>
              {availableTrips.length > 0 ? (
                <select value={form.tripId} onChange={(e) => setForm({ ...form, tripId: e.target.value })}>
                  {availableTrips.map((t) => (
                    <option key={t.tripId} value={t.tripId}>{t.tripNo} - {t.driverName} ({t.origin} → {t.destination})</option>
                  ))}
                </select>
              ) : (
                <input required type="number" value={form.tripId} onChange={(e) => setForm({ ...form, tripId: e.target.value })} placeholder="Enter Trip ID (e.g. 1)" />
              )}
            </div>
            {costFields(form, setForm)}
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 20 }}>
              <button type="button" className="btn btn-secondary" onClick={() => setShowAdd(false)}>Cancel</button>
              <button type="submit" className="btn btn-primary">Save Cost</button>
            </div>
          </form>
        </Modal>
      )}

      {/* Edit modal */}
      {editing && (
        <Modal title={`Edit Trip Cost — ${editing.trip}`} onClose={() => setEditing(null)}>
          <form onSubmit={handleEdit}>
            {costFields(editForm, setEditForm)}
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 20 }}>
              <button type="button" className="btn btn-secondary" onClick={() => setEditing(null)}>Cancel</button>
              <button type="submit" className="btn btn-primary">Save Changes</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}