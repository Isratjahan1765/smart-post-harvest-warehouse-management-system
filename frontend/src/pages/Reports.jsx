import React, { useState, useEffect } from 'react';
import { reportKPIs as initialKPIs, revenueByCategory as initialCat, topProducts as initialTop } from '../data/mockData';
import { reportApi } from '../api/endpoints';

export default function Reports() {
  const [kpis, setKpis] = useState(initialKPIs);
  const [categories, setCategories] = useState(initialCat);
  const [products, setProducts] = useState(initialTop);

  useEffect(() => {
    reportApi.getSummary()
      .then(data => {
        if (data) {
          setKpis({
            totalRevenue: Number(data.totalRevenue).toLocaleString(),
            totalExpense: Number(data.totalExpense).toLocaleString(),
            netProfit: Number(data.netProfit).toLocaleString(),
            donatedValue: Number(data.donatedValue).toLocaleString()
          });
        }
      })
      .catch(() => {});

    reportApi.getRevenueByCategory()
      .then(data => {
        if (data && data.length > 0) {
          const totalRev = data.reduce((acc, c) => acc + Number(c.revenue), 0) || 1;
          setCategories(data.map(c => ({
            name: c.category.charAt(0) + c.category.slice(1).toLowerCase(),
            value: Math.round((Number(c.revenue) / totalRev) * 100),
            amount: Number(c.revenue)
          })));
        }
      })
      .catch(() => {});

    reportApi.getTopProducts()
      .then(data => {
        if (data && data.length > 0) {
          setProducts(data.map(p => ({
            name: p.productName,
            value: Number(p.revenue),
            quantity: Number(p.quantity)
          })));
        }
      })
      .catch(() => {});
  }, []);

  const catColors = ['var(--warning)', 'var(--success)', 'var(--purple)', 'var(--danger)', 'var(--info)'];
  const pyramidColors = ['var(--primary)', '#3b82f6', '#60a5fa', '#93c5fd', '#bfdbfe'];
  const maxPyramid = products[0] && products[0].value ? products[0].value : 1;

  let gradientStops = '';
  let cumulative = 0;
  categories.forEach((cat, i) => {
    const color = catColors[i % catColors.length];
    gradientStops += `${color} ${cumulative}% ${cumulative + cat.value}%, `;
    cumulative += cat.value;
  });
  if (gradientStops.endsWith(', ')) gradientStops = gradientStops.slice(0, -2);
  if (!gradientStops) gradientStops = 'var(--primary) 0% 100%';

  return (
    <div>
      {/* KPI Cards */}
      <div className="summary-cards">
        <div className="summary-card">
          <div className="card-icon green">💰</div>
          <div className="card-info"><h3>Tk {kpis.totalRevenue}</h3><p>Total Revenue</p></div>
        </div>
        <div className="summary-card">
          <div className="card-icon red">📉</div>
          <div className="card-info"><h3>Tk {kpis.totalExpense}</h3><p>Total Expense</p></div>
        </div>
        <div className="summary-card">
          <div className="card-icon blue">📊</div>
          <div className="card-info"><h3>Tk {kpis.netProfit}</h3><p>Net Profit</p></div>
        </div>
        <div className="summary-card">
          <div className="card-icon purple">🎁</div>
          <div className="card-info"><h3>Tk {kpis.donatedValue}</h3><p>Donated Value</p></div>
        </div>
      </div>

      <div className="grid-equal">
        {/* Revenue by Category Donut */}
        <div className="card">
          <h3 className="section-title">Revenue by Category</h3>
          <div style={{ display: 'flex', alignItems: 'center', gap: 32, flexWrap: 'wrap' }}>
            <div style={{
              width: 160,
              height: 160,
              borderRadius: '50%',
              background: `conic-gradient(${gradientStops})`,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}>
              <div style={{
                width: 100,
                height: 100,
                borderRadius: '50%',
                background: 'var(--card-bg)',
              }} />
            </div>
            <div className="donut-legend">
              {categories.map((cat, i) => (
                <div className="legend-item" key={cat.name}>
                  <div className="legend-dot" style={{ background: catColors[i % catColors.length] }} />
                  <span className="legend-label">{cat.name}</span>
                  <span className="legend-pct">{cat.value}%</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Top 5 Products by Revenue */}
        <div className="card">
          <h3 className="section-title">Top 5 Products by Revenue</h3>
          <div className="pyramid-container">
            {products.map((p, i) => {
              const width = Math.max(30, Math.round((p.value / maxPyramid) * 100));
              return (
                <div className="pyramid-row" key={p.name}>
                  <div className="pyramid-bar-wrap">
                    <div
                      className="pyramid-bar"
                      style={{
                        width: `${width}%`,
                        background: pyramidColors[i % pyramidColors.length],
                      }}
                    >
                      <span className="pyramid-label">{p.name}</span>
                    </div>
                  </div>
                  <span className="pyramid-value">Tk {(p.value / 1000).toFixed(0)}K</span>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
