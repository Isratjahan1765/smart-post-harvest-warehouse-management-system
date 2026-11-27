import React, { useState, useEffect, useMemo } from 'react';
import { dashboardProducts, dashboardSummary } from '../data/mockData';
import { dashboardApi } from '../api/endpoints';

const fmtShort = (val) => {
  const v = Number(val || 0);
  if (v >= 10000000) return `Tk ${(v / 10000000).toFixed(1).replace(/\.0$/, '')}Cr`;
  if (v >= 100000) return `Tk ${(v / 100000).toFixed(1).replace(/\.0$/, '')}L`;
  if (v >= 1000) return `Tk ${(v / 1000).toFixed(0)}K`;
  return `Tk ${Math.round(v)}`;
};

// chart geometry (SVG viewBox units)
const W = 1240, H = 570;
const L = 110, R = 1220, T = 40, B = 400;

export default function Dashboard() {
  const [countInput, setCountInput] = useState('15');
  const [summary, setSummary] = useState(dashboardSummary);
  const [products, setProducts] = useState(dashboardProducts);

  useEffect(() => {
    dashboardApi.getSummary()
      .then(data => {
        if (data) {
          setSummary({
            totalProducts: data.totalProducts,
            arrivalsThisMonth: data.arrivalsThisMonth,
            stockValue: Number(data.stockValue).toLocaleString('en-IN'),
            totalDue: Number(data.totalDue).toLocaleString('en-IN')
          });
        }
      })
      .catch(() => {});

    // fetch a larger list so "Out of total" and the quantity box can work
    dashboardApi.getInventoryRanking(100)
      .then(data => {
        if (data && data.length > 0) {
          setProducts(data.map(p => ({
            name: p.productName,
            category: p.category,
            value: Number(p.totalValue),
            quantity: Number(p.totalQty)
          })));
        }
      })
      .catch(() => {});
  }, []);

  const total = products.length;
  const parsed = parseInt(countInput, 10);
  const displayCount = Math.max(1, Math.min(total || 1, isNaN(parsed) ? 1 : parsed));

  const displayed = useMemo(
    () => [...products].sort((a, b) => b.value - a.value).slice(0, displayCount),
    [products, displayCount]
  );

  const maxVal = Math.max(...displayed.map(p => p.value || 0), 1);
  const half = Math.pow(10, Math.floor(Math.log10(maxVal))) / 2;
  const niceMax = Math.ceil(maxVal / half) * half;
  const ticks = [0, 1, 2, 3, 4].map(i => (niceMax * i) / 4);

  const y = (v) => B - (v / niceMax) * (B - T);
  const slot = (R - L) / Math.max(displayed.length, 1);
  const barW = Math.min(48, slot * 0.66);

  return (
    <div>
      {/* Ranking chart */}
      <div className="db-card">
        <h2 className="db-title">Ranking of products based on inventory value</h2>

        <div className="db-controls">
          <label className="db-field">
            Enter quantity to display:
            <input
              type="number"
              min="1"
              max={total || 1}
              value={countInput}
              onChange={e => setCountInput(e.target.value)}
              onBlur={() => setCountInput(String(displayCount))}
            />
          </label>
          <div className="db-field">
            Out of total: <strong>{total}</strong>
          </div>
        </div>

        <svg className="db-chart" viewBox={`0 0 ${W} ${H}`} role="img" aria-label="Inventory value by product">
          {/* grid + y labels */}
          {ticks.map((t, i) => (
            <g key={i}>
              <line x1={L} x2={R} y1={y(t)} y2={y(t)} className="db-grid" />
              <text x={L - 14} y={y(t) + 5} textAnchor="end" className="db-tick">
                {t === 0 ? 'Tk 0' : fmtShort(t)}
              </text>
            </g>
          ))}

          {/* axis titles */}
          <text transform={`translate(26 ${(T + B) / 2}) rotate(-90)`} textAnchor="middle" className="db-axis-title">
            Inventory value (Tk)
          </text>
          <text x={(L + R) / 2} y={H - 10} textAnchor="middle" className="db-axis-title">Product</text>

          {/* bars */}
          {displayed.map((p, i) => {
            const cx = L + slot * i + slot / 2;
            const top = y(p.value);
            return (
              <g key={`${p.name}-${i}`}>
                <rect
                  x={cx - barW / 2}
                  y={top}
                  width={barW}
                  height={Math.max(B - top, 0)}
                  rx="3"
                  className="db-bar"
                >
                  <title>{`${p.name}: Tk ${Number(p.value).toLocaleString('en-IN')}`}</title>
                </rect>
                <text x={cx} y={top - 8} textAnchor="middle" className="db-bar-value">{fmtShort(p.value)}</text>
                <text
                  x={cx}
                  y={B + 20}
                  textAnchor="end"
                  transform={`rotate(-45 ${cx} ${B + 20})`}
                  className="db-x-label"
                >
                  {p.name}
                </text>
              </g>
            );
          })}
        </svg>
      </div>

      {/* Summary strip */}
      <div className="db-summary">
        <div className="db-stat">
          <span className="db-stat-icon db-ic-blue">
            <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
              <polyline points="3.27 6.96 12 12.01 20.73 6.96" /><line x1="12" y1="22.08" x2="12" y2="12" />
            </svg>
          </span>
          <div>
            <p>Total Products</p>
            <h3>{summary.totalProducts}</h3>
          </div>
        </div>

        <div className="db-stat">
          <span className="db-stat-icon db-ic-teal">
            <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
              <polyline points="7 10 12 15 17 10" /><line x1="12" y1="15" x2="12" y2="3" />
            </svg>
          </span>
          <div>
            <p>Arrivals This Month</p>
            <h3>{summary.arrivalsThisMonth}</h3>
          </div>
        </div>

        <div className="db-stat">
          <span className="db-stat-icon db-ic-orange">
            <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <rect x="2" y="6" width="20" height="12" rx="2" /><circle cx="12" cy="12" r="2" />
              <path d="M6 12h.01M18 12h.01" />
            </svg>
          </span>
          <div>
            <p>Stock Value</p>
            <h3>Tk {summary.stockValue}</h3>
          </div>
        </div>

        <div className="db-stat">
          <span className="db-stat-icon db-ic-red">
            <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" />
            </svg>
          </span>
          <div>
            <p>Total Due</p>
            <h3>Tk {summary.totalDue}</h3>
          </div>
        </div>
      </div>
    </div>
  );
}