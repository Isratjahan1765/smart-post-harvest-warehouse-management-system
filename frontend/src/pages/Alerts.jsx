import React, { useState, useEffect, useCallback } from 'react';
import { alertApi } from '../api/endpoints';

const PER_PAGE = 5;

// fixed order, as in the spec: Low Stock, Near Expiry, Due Payment, License Expiry
const TYPES = [
  { key: 'LOW_STOCK',      field: 'lowStock',      label: 'Low Stock',      color: '#bbdaf5', pill: 'al-p1' },
  { key: 'NEAR_EXPIRY',    field: 'nearExpiry',    label: 'Near Expiry',    color: '#f0b27a', pill: 'al-p2' },
  { key: 'DUE_PAYMENT',    field: 'duePayment',    label: 'Due Payment',    color: '#3db4d6', pill: 'al-p3' },
  { key: 'LICENSE_EXPIRY', field: 'licenseExpiry', label: 'License Expiry', color: '#e07a6e', pill: 'al-p4' },
];
const typeInfo = (t) => TYPES.find((x) => x.key === t || x.label === t) || { label: t, pill: 'al-p1' };

// round all percents, then put the error on the largest slice so the sum is 100
function percents(counts) {
  const total = counts.reduce((s, c) => s + c, 0);
  if (total === 0) return counts.map(() => 0);
  const p = counts.map((c) => Math.round((c / total) * 100));
  const diff = 100 - p.reduce((s, v) => s + v, 0);
  if (diff) p[counts.indexOf(Math.max(...counts))] += diff;
  return p;
}

const SearchIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
    <circle cx="11" cy="11" r="7" /><path d="M20 20l-3.5-3.5" />
  </svg>
);
const EyeIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7S1 12 1 12z" /><circle cx="12" cy="12" r="3" />
  </svg>
);
const TickIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="12" cy="12" r="9" /><path d="M8 12.5l2.7 2.7L16 9.8" />
  </svg>
);

export default function Alerts() {
  const [rows, setRows] = useState([]);
  const [total, setTotal] = useState(0);          // total rows for the current search
  const [counts, setCounts] = useState({ lowStock: 0, nearExpiry: 0, duePayment: 0, licenseExpiry: 0 });
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(1);
  const [error, setError] = useState('');
  const [detail, setDetail] = useState(null);

  const normalize = (a) => ({
    id: a.id ?? a.alertId,
    type: a.type,
    item: a.item ?? a.itemName ?? '',
    warehouse: a.warehouse ?? a.warehouseName ?? null,
    details: a.details ?? '',
    isRead: a.read ?? a.isRead ?? false,
    time: a.createdAt ? new Date(a.createdAt).toLocaleString() : '',
  });

  const loadSummary = useCallback(() => {
    alertApi.getSummary()
      .then((d) => d && setCounts({
        lowStock: d.lowStock ?? d.LOW_STOCK ?? 0,
        nearExpiry: d.nearExpiry ?? d.NEAR_EXPIRY ?? 0,
        duePayment: d.duePayment ?? d.DUE_PAYMENT ?? 0,
        licenseExpiry: d.licenseExpiry ?? d.LICENSE_EXPIRY ?? 0,
      }))
      .catch(() => setError('Unable to load alerts.'));
  }, []);

  const loadList = useCallback(() => {
    alertApi.getAll({ q: search.trim(), page: page - 1, size: PER_PAGE })
      .then((d) => {
        console.log('alerts list response:', d);   // check this in the browser console
        // find the list whatever the backend wraps it in
        const list = Array.isArray(d) ? d : (d?.items ?? d?.content ?? d?.data ?? []);
        const paged = !Array.isArray(d) && d && (d.totalElements !== undefined);
        if (paged && list.length <= PER_PAGE) {
          // server already paged and searched: use as it is
          setTotal(d.totalElements);
          setRows(list.map(normalize));
        } else {
          // plain array: search and page here in React
          const q = search.trim().toLowerCase();
          const all = list.map(normalize).filter((a) =>
            !q || a.item.toLowerCase().includes(q) || a.details.toLowerCase().includes(q) ||
            typeInfo(a.type).label.toLowerCase().includes(q));
          setTotal(all.length);
          setRows(all.slice((page - 1) * PER_PAGE, page * PER_PAGE));
        }
        setError('');
      })
      .catch(() => setError('Unable to load alerts.'));
  }, [search, page]);

  useEffect(() => { loadSummary(); }, [loadSummary]);
  useEffect(() => { loadList(); }, [loadList]);

  // after any read action refresh cards, donut, list (and tell the bell)
  const refreshAll = () => {
    loadSummary();
    loadList();
    window.dispatchEvent(new Event('alerts-changed'));
  };

  const markRead = async (id) => { try { await alertApi.markRead(id); refreshAll(); } catch (_) { setError('Unable to update alert.'); } };
  const markAll = async () => { try { await alertApi.markAllRead(); refreshAll(); } catch (_) { setError('Unable to update alerts.'); } };

  // donut
  const countList = TYPES.map((t) => counts[t.field]);
  const totalAlerts = countList.reduce((s, c) => s + c, 0);
  const pct = percents(countList);
  let acc = 0;
  const stops = TYPES.map((t, i) => {
    const s = `${t.color} ${acc}% ${acc + pct[i]}%`;
    acc += pct[i];
    return s;
  }).join(', ');

  // paging
  const totalPages = Math.max(1, Math.ceil(total / PER_PAGE));
  const from = total === 0 ? 0 : (page - 1) * PER_PAGE + 1;
  const to = Math.min(page * PER_PAGE, total);

  return (
    <div className="al">
      {error && <div className="al-err">{error}</div>}

      {/* cards */}
      <div className="al-cards">
        {TYPES.map((t) => (
          <div className="al-card" key={t.key}>
            <span className={`al-pill ${t.pill}`}>{t.label}</span>
            <div className="al-num">{counts[t.field]}</div>
          </div>
        ))}
      </div>

      {/* search bar */}
      <div className="al-card al-bar">
        <label className="al-search">
          <SearchIcon />
          <input
            placeholder="Search product, buyer or driver"
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(1); }}
          />
        </label>
        <button className="al-outline" onClick={markAll}>Mark All as Read</button>
      </div>

      <div className="al-grid">
        {/* table */}
        <div className="al-card al-panel">
          <h3>Alerts</h3>
          <table className="al-table">
            <thead>
              <tr><th>Type</th><th>Item</th><th>Warehouse</th><th>Details</th><th>Status</th><th>Actions</th></tr>
            </thead>
            <tbody>
              {rows.length === 0 && (
                <tr><td colSpan="6" className="al-empty">No alerts found.</td></tr>
              )}
              {rows.map((a) => (
                <tr key={a.id}>
                  <td><span className={`al-type ${typeInfo(a.type).pill}`}>{typeInfo(a.type).label}</span></td>
                  <td className="al-item">{a.item}</td>
                  <td className="al-wh">{a.warehouse || '-'}</td>
                  <td className="al-det">{a.details}</td>
                  <td><span className={`al-status ${a.isRead ? 'read' : 'unread'}`}>{a.isRead ? 'Read' : 'Unread'}</span></td>
                  <td>
                    <div className="al-actions">
                      <button className="al-ico" title="View" onClick={() => setDetail(a)}><EyeIcon /></button>
                      <button className="al-ico" title="Mark as read" disabled={a.isRead} onClick={() => markRead(a.id)}><TickIcon /></button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="al-foot">
            <span className="al-showing">Showing {from}–{to} of {total}</span>
            <div className="al-pages">
              <button className="al-pg" disabled={page <= 1} onClick={() => setPage(page - 1)}>‹</button>
              {Array.from({ length: totalPages }, (_, i) => i + 1).map((n) => (
                <button key={n} className={`al-pg ${n === page ? 'on' : ''}`} onClick={() => setPage(n)}>{n}</button>
              ))}
              <button className="al-pg" disabled={page >= totalPages} onClick={() => setPage(page + 1)}>›</button>
            </div>
          </div>
        </div>

        {/* donut */}
        <div className="al-card al-panel al-side">
          <h3>Alerts by Type</h3>
          {totalAlerts === 0 ? (
            <p className="al-empty">No alerts</p>
          ) : (
            <div className="al-donut" style={{ background: `conic-gradient(${stops})` }}>
              <div className="al-hole">{totalAlerts} alerts</div>
            </div>
          )}
          <ul className="al-legend">
            {TYPES.map((t, i) => (
              <li key={t.key}><i style={{ background: t.color }} />{t.label} <b>{pct[i]}%</b></li>
            ))}
          </ul>
        </div>
      </div>

      {/* eye icon: detail pop-up */}
      {detail && (
        <div className="al-modal" onClick={() => setDetail(null)}>
          <div className="al-box" onClick={(e) => e.stopPropagation()}>
            <h3>{typeInfo(detail.type).label}</h3>
            <p><b>Item:</b> {detail.item}</p>
            <p><b>Warehouse:</b> {detail.warehouse || '-'}</p>
            <p><b>Details:</b> {detail.details}</p>
            {detail.time && <p><b>Detected:</b> {detail.time}</p>}
            <p><b>Status:</b> {detail.isRead ? 'Read' : 'Unread'}</p>
            <button className="al-outline" onClick={() => setDetail(null)}>Close</button>
          </div>
        </div>
      )}
    </div>
  );
}