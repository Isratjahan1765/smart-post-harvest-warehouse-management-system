import React, { useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { useLang, LanguageToggle } from '../i18n/LanguageContext';

export default function Login() {
  const { isAuthenticated, login } = useAuth();
  const { t } = useLang();
  const navigate = useNavigate();
  const location = useLocation();
  const from = location.state?.from?.pathname || '/';

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');
  const [loading, setLoading] = useState(false);

  if (isAuthenticated) return <Navigate to={from} replace />;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setInfo('');
    if (!username.trim() || !password) {
      setError(t('Please enter username and password.'));
      return;
    }
    setError('');
    setLoading(true);
    try {
      await login(username.trim(), password);
      navigate(from, { replace: true });
    } catch (err) {
      setError(err?.message || t('Invalid username or password'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="lg-page">
      {/* background shapes */}
      <div className="lg-c lg-c1" />
      <div className="lg-c lg-c2" />
      <div className="lg-c lg-c3" />
      <div className="lg-c lg-c4" />
      <div className="lg-c lg-c5" />
      <div className="lg-c lg-c6" />
      <svg className="lg-wave" viewBox="0 0 1440 200" preserveAspectRatio="none" aria-hidden="true">
        <path d="M0 90 C 300 20, 700 40, 1000 100 S 1350 150, 1440 80 L1440 200 L0 200 Z" fill="rgba(255,255,255,.22)" />
        <path d="M0 140 C 350 70, 750 90, 1050 140 S 1380 170, 1440 120 L1440 200 L0 200 Z" fill="rgba(255,255,255,.22)" />
      </svg>

      <div className="lg-lang"><LanguageToggle /></div>

      <form className="lg-card" onSubmit={handleSubmit} noValidate>
        <div className="lg-avatar">S</div>
        <h1 className="lg-title">{t('Welcome back')}</h1>
        <p className="lg-sub">{t('Smart Post-Harvest Warehouse Management System')}</p>

        <label className="lg-label" htmlFor="lg-user">{t('Username')}</label>
        <input
          id="lg-user"
          className="lg-input"
          type="text"
          autoComplete="username"
          autoFocus
          placeholder={t('Enter your username')}
          value={username}
          onChange={(e) => setUsername(e.target.value)}
        />

        <label className="lg-label" htmlFor="lg-pass">{t('Password')}</label>
        <input
          id="lg-pass"
          className="lg-input"
          type="password"
          autoComplete="current-password"
          placeholder={t('Enter your password')}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />

        {error && <div className="lg-error" role="alert">{error}</div>}
        {info && <div className="lg-info">{info}</div>}

        <button className="lg-btn" type="submit" disabled={loading}>
          {loading ? t('Logging in...') : t('Log In')}
        </button>

        <button
          type="button"
          className="lg-forgot"
          onClick={() => setInfo(t('Please contact your administrator to reset your password.'))}
        >
          {t('Forgot Password?')}
        </button>
      </form>
    </div>
  );
}