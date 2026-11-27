import React, { useState, useEffect, useMemo } from 'react';
import Modal from '../components/Modal';
import { buyers as initialBuyers, purchaseHistory as initialHistory } from '../data/mockData';
import { buyerApi, saleApi } from '../api/endpoints';

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

const fmtDate = (value, withYear = false) => {
  if (!value) return '-';
  const d = new Date(value);
  if (isNaN(d.getTime())) return value;
  const base = `${d.getDate()} ${MONTHS[d.getMonth()]}`;
  return withYear ? `${base} ${d.getFullYear()}` : base;
};

const fmtTk = (n) => `Tk ${Number(n || 0).toLocaleString()}`;

const shortTk = (n) => {
  const v = Number(n || 0);
  if (v >= 10000000) return `Tk ${(v / 10000000).toFixed(1).replace(/\.0$/, '')}Cr`;
  if (v >= 100000) return `Tk ${(v / 100000).toFixed(1).replace(/\.0$/, '')}L`;
  if (v >= 1000) return `Tk ${Math.round(v / 1000)}K`;
  return `Tk ${v}`;
};

const mapSale = (s, buyers) => {
  const b = buyers.find(x => x.id === s.buyerId) || {};
  return {
    id: s.saleId,
    saleNo: s.saleNo,
    product: s.productName,
    date: s.saleDate,
    buyer: s.buyerName || b.name,
    phone: s.buyerPhone || b.phone,
    address: s.buyerAddress || b.address,
    quantity: Number(s.quantity),
    rate: Number(s.rate),
    total: Number(s.totalAmount),
    paid: Number(s.paidAmount),
    due: Number(s.dueAmount),
    status: s.paymentStatus
  };
};

export default function Buyers() {
  const [buyerList, setBuyerList] = useState(initialBuyers);
  const [selectedBuyer, setSelectedBuyer] = useState(initialBuyers[0]);
  const [history, setHistory] = useState(initialHistory);
  const [allHistory, setAllHistory] = useState([]);
  const [showAll, setShowAll] = useState(false);
  const [search, setSearch] = useState('');
  const [showAddPurchase, setShowAddPurchase] = useState(false);
  const [showPayment, setShowPayment] = useState(false);
  const [paymentAmount, setPaymentAmount] = useState('');
  const [purchaseForm, setPurchaseForm] = useState({
    productId: '1', warehouseId: '1', quantity: '', rate: '', paidAmount: '', dueDate: ''
  });

  const sb = selectedBuyer || {};

  const loadBuyers = () => {
    buyerApi.getAll()
      .then(data => {
        if (data && data.length > 0) {
          const mapped = data.map(b => ({
            id: b.buyerId,
            name: b.name,
            phone: b.phone,
            address: b.address,
            type: b.buyerType,
            totalPurchase: Number(b.totalSales || 0),
            totalPaid: Number(b.totalPaid || 0),
            totalDue: Number(b.totalDue || 0)
          }));
          setBuyerList(mapped);
          const current = mapped.find(m => selectedBuyer && m.id === selectedBuyer.id);
          setSelectedBuyer(current || mapped[0]);
        }
      })
      .catch(() => {});
  };

  const loadHistory = (buyerId) => {
    if (!buyerId) return;
    saleApi.getAll({ buyerId })
      .then(data => {
        if (data && data.length > 0) {
          setHistory(data.map(s => mapSale(s, buyerList)));
        }
      })
      .catch(() => {});
  };

  const loadAllHistory = () => {
    saleApi.getAll()
      .then(data => {
        if (data) setAllHistory(data.map(s => mapSale(s, buyerList)));
      })
      .catch(() => {});
  };

  useEffect(() => {
    loadBuyers();
  }, []);

  useEffect(() => {
    if (selectedBuyer && selectedBuyer.id) loadHistory(selectedBuyer.id);
  }, [selectedBuyer && selectedBuyer.id]);

  useEffect(() => {
    if (showAll) loadAllHistory();
  }, [showAll, buyerList]);

  const filteredBuyers = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return buyerList;
    return buyerList.filter(b =>
      (b.name || '').toLowerCase().includes(q) || (b.phone || '').toLowerCase().includes(q)
    );
  }, [buyerList, search]);

  const rows = showAll ? allHistory : history;

  const lastPurchase = useMemo(() => {
    const dates = history.map(h => new Date(h.date)).filter(d => !isNaN(d.getTime()));
    if (dates.length === 0) return null;
    return new Date(Math.max(...dates.map(d => d.getTime())));
  }, [history]);

  const handleRecordPayment = async (e) => {
    e.preventDefault();
    if (!sb.id) return;
    try {
      await buyerApi.recordPayment(sb.id, {
        amount: Number(paymentAmount),
        paymentDate: new Date().toISOString().split('T')[0]
      });
      setShowPayment(false);
      setPaymentAmount('');
      loadBuyers();
      loadHistory(sb.id);
      if (showAll) loadAllHistory();
    } catch (err) {
      alert(err.message || 'Payment recording failed');
    }
  };

  const handleCreateSale = async (e) => {
    e.preventDefault();
    if (!sb.id) return;
    try {
      await saleApi.create({
        buyerId: sb.id,
        productId: Number(purchaseForm.productId),
        warehouseId: Number(purchaseForm.warehouseId),
        quantity: Number(purchaseForm.quantity),
        rate: Number(purchaseForm.rate),
        paidAmount: Number(purchaseForm.paidAmount || 0),
        saleDate: new Date().toISOString().split('T')[0],
        dueDate: purchaseForm.dueDate || null
      });
      setShowAddPurchase(false);
      loadBuyers();
      loadHistory(sb.id);
      if (showAll) loadAllHistory();
    } catch (err) {
      alert(err.message || 'Sale creation failed');
    }
  };

  // Export (CSV, opens directly in Excel). Swap with SheetJS if you want a real .xlsx.
  const handleExport = () => {
    const head = ['Sale No', 'Date', 'Product', 'Buyer', 'Phone', 'Address', 'Qty (kg)', 'Total (Tk)', 'Paid (Tk)', 'Due (Tk)', 'Status'];
    const esc = (v) => `"${String(v ?? '').replace(/"/g, '""')}"`;
    const lines = rows.map(r => [
      r.saleNo, r.date, r.product, r.buyer || sb.name, r.phone || sb.phone, r.address || sb.address,
      r.quantity, r.total, r.paid, r.due, r.status
    ].map(esc).join(','));
    const csv = '\uFEFF' + [head.map(esc).join(','), ...lines].join('\n');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8;' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = showAll ? 'purchase-history-all-buyers.csv' : `purchase-history-${sb.name || 'buyer'}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const totalPurchase = Number(sb.totalPurchase || 0);
  const totalPaid = Number(sb.totalPaid || 0);
  const totalDue = Number(sb.totalDue || 0);
  const paidPercent = totalPurchase > 0 ? Math.min(100, Math.round((totalPaid / totalPurchase) * 100)) : 0;
  const duePercent = totalPurchase > 0 ? 100 - paidPercent : 0;

  const donutBg = totalPurchase > 0
    ? `conic-gradient(#5b9fe0 0% ${paidPercent}%, #3bb3d6 ${paidPercent}% 100%)`
    : '#e3e7ee';

  return (
    <div>
      {/* Search + buyer chips */}
      <div className="by-card">
        <div className="dn-searchbox">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="11" cy="11" r="7" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            className="dn-search"
            placeholder="Search buyer by name or phone"
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
        </div>
        <div className="by-chips">
          {filteredBuyers.map(b => (
            <button
              type="button"
              key={b.id || b.name}
              className={`by-chip ${sb.name === b.name ? 'active' : ''}`}
              onClick={() => setSelectedBuyer(b)}
            >
              {b.name}
            </button>
          ))}
          {filteredBuyers.length === 0 && <span className="by-empty">No buyer found</span>}
        </div>
      </div>

      {/* Buyer summary */}
      <div className="by-card">
        <div className="by-summary-head">
          <h2 className="by-title">
            {sb.name}
            <span className="by-info" title={[sb.phone, sb.address].filter(Boolean).join(' • ') || 'Buyer info'}>
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10" /><line x1="12" y1="16" x2="12" y2="12" /><line x1="12" y1="8" x2="12.01" y2="8" />
              </svg>
            </span>
          </h2>
          <button className="btn btn-primary by-receive" onClick={() => setShowPayment(true)}>
            Receive Due Payment
          </button>
        </div>

        <div className="by-summary-body">
          <div className="by-donut" style={{ background: donutBg }}>
            <div className="by-donut-center">
              <span>{shortTk(totalDue)}</span>
              <span>due</span>
            </div>
          </div>

          <div className="by-stats">
            <div className="by-legend">
              <span><i className="by-dot by-dot-paid" />Paid <strong>{paidPercent}%</strong></span>
              <span><i className="by-dot by-dot-due" />Due <strong>{duePercent}%</strong></span>
            </div>
            <div className="by-stat-row">
              <span className="by-tag by-tag-total">Total Purchase</span>
              <strong>{fmtTk(totalPurchase)}</strong>
            </div>
            <div className="by-stat-row">
              <span className="by-tag by-tag-paid">Total Paid</span>
              <strong>{fmtTk(totalPaid)}</strong>
            </div>
            <div className="by-stat-row">
              <span className="by-tag by-tag-due">Total Due</span>
              <strong>{fmtTk(totalDue)}</strong>
            </div>
            <div className="by-stat-row">
              <span className="by-tag by-tag-last">Last Purchase</span>
              <strong>{lastPurchase ? fmtDate(lastPurchase, true) : '-'}</strong>
            </div>
          </div>
        </div>
      </div>

      {/* Purchase history */}
      <div className="by-card">
        <div className="by-history-head">
          <h2 className="by-title">Purchase History</h2>
          <div className="by-history-tools">
            <label className="by-allcheck">
              <input type="checkbox" checked={showAll} onChange={e => setShowAll(e.target.checked)} />
              All buyers
            </label>
            <button className="btn-outline by-export" onClick={handleExport}>Export XLSX</button>
          </div>
        </div>

        <div className="by-table-wrap">
          <table className="pg-table">
            <thead>
              <tr>
                <th>Sale No</th>
                <th>Date</th>
                <th>Product</th>
                <th>Buyer</th>
                <th>Phone</th>
                <th>Address</th>
                <th>Qty</th>
                <th>Total</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row, idx) => {
                const isPaid = String(row.status).toUpperCase() === 'PAID';
                return (
                  <tr key={row.id || idx}>
                    <td>{row.saleNo || `S-${String(idx + 1).padStart(4, '0')}`}</td>
                    <td>{fmtDate(row.date)}</td>
                    <td>{row.product}</td>
                    <td><strong>{row.buyer || sb.name}</strong></td>
                    <td>{row.phone || sb.phone}</td>
                    <td>{row.address || sb.address}</td>
                    <td>{Number(row.quantity).toLocaleString()} kg</td>
                    <td>{fmtTk(row.total)}</td>
                    <td>
                      <span className={`pill ${isPaid ? 'pill-in' : 'pill-due'}`}>
                        {isPaid ? 'Paid' : 'Due'}
                      </span>
                    </td>
                  </tr>
                );
              })}
              {rows.length === 0 && (
                <tr><td colSpan={9} className="by-empty-row">No purchases found</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Record Payment Modal */}
      {showPayment && (
        <Modal title={`Receive Due Payment from ${sb.name}`} onClose={() => setShowPayment(false)}>
          <form onSubmit={handleRecordPayment}>
            <div className="form-group">
              <label>Current Total Due: <strong>{fmtTk(totalDue)}</strong></label>
            </div>
            <div className="form-group">
              <label>Payment Amount (Tk)</label>
              <input required type="number" value={paymentAmount} onChange={e => setPaymentAmount(e.target.value)} placeholder="e.g. 5000" />
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 20 }}>
              <button type="button" className="btn btn-secondary" onClick={() => setShowPayment(false)}>Cancel</button>
              <button type="submit" className="btn btn-primary">Submit Payment</button>
            </div>
          </form>
        </Modal>
      )}

      {/* Add Purchase Modal */}
      {showAddPurchase && (
        <Modal title={`Add New Sale for ${sb.name}`} onClose={() => setShowAddPurchase(false)}>
          <form onSubmit={handleCreateSale}>
            <div className="form-group">
              <label>Product ID</label>
              <input required type="number" value={purchaseForm.productId} onChange={e => setPurchaseForm({ ...purchaseForm, productId: e.target.value })} />
            </div>
            <div className="form-group">
              <label>Warehouse ID</label>
              <input required type="number" value={purchaseForm.warehouseId} onChange={e => setPurchaseForm({ ...purchaseForm, warehouseId: e.target.value })} />
            </div>
            <div className="form-group">
              <label>Quantity (kg)</label>
              <input required type="number" value={purchaseForm.quantity} onChange={e => setPurchaseForm({ ...purchaseForm, quantity: e.target.value })} />
            </div>
            <div className="form-group">
              <label>Rate (Tk/kg)</label>
              <input required type="number" value={purchaseForm.rate} onChange={e => setPurchaseForm({ ...purchaseForm, rate: e.target.value })} />
            </div>
            <div className="form-group">
              <label>Paid Amount (Tk)</label>
              <input type="number" value={purchaseForm.paidAmount} onChange={e => setPurchaseForm({ ...purchaseForm, paidAmount: e.target.value })} />
            </div>
            <div className="form-group">
              <label>Due Date</label>
              <input type="date" value={purchaseForm.dueDate} onChange={e => setPurchaseForm({ ...purchaseForm, dueDate: e.target.value })} />
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 20 }}>
              <button type="button" className="btn btn-secondary" onClick={() => setShowAddPurchase(false)}>Cancel</button>
              <button type="submit" className="btn btn-primary">Create Sale</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
