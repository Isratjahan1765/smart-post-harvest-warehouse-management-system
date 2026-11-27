import React from 'react';
import { userProfile } from '../data/mockData';

export default function Profile() {
  return (
    <div className="profile-card card">
      <div className="profile-header">
        <div className="profile-avatar">RU</div>
        <div>
          <h2 style={{ fontSize: 22, fontWeight: 700 }}>{userProfile.name}</h2>
          <span className="badge badge-info" style={{ marginTop: 6 }}>{userProfile.role}</span>
        </div>
        <div style={{ marginLeft: 'auto', display: 'flex', gap: 8 }}>
          <button className="btn-icon edit" title="Edit">✏️</button>
          <button className="btn-icon delete" title="Delete">🗑️</button>
        </div>
      </div>

      <div className="profile-field">
        <span className="field-label">👤 Name</span>
        <span className="field-value">{userProfile.name}</span>
      </div>
      <div className="profile-field">
        <span className="field-label">🏷️ Role</span>
        <span className="field-value">{userProfile.role}</span>
      </div>
      <div className="profile-field">
        <span className="field-label">🆔 Employee ID</span>
        <span className="field-value">{userProfile.id}</span>
      </div>
      <div className="profile-field">
        <span className="field-label">📞 Phone</span>
        <span className="field-value">{userProfile.phone}</span>
      </div>
      <div className="profile-field">
        <span className="field-label">📧 Email</span>
        <span className="field-value">{userProfile.email}</span>
      </div>
      <div className="profile-field">
        <span className="field-label">📍 Address</span>
        <span className="field-value">{userProfile.address}</span>
      </div>
    </div>
  );
}
