const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

function getStoredToken() {
  try { return JSON.parse(localStorage.getItem('sphwms_auth'))?.token || null; } catch { return null; }
}

export async function request(endpoint, options = {}) {
  const url = `${API_BASE}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`;
  const token = getStoredToken();

  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers || {}),
  };

  const response = await fetch(url, { ...options, headers });

  if (!response.ok) {
    if (response.status === 401 && !endpoint.includes('/auth/login')) {
      window.dispatchEvent(new Event('auth-expired'));
    }
    let errorMsg = `Request failed: ${response.status} ${response.statusText}`;
    try {
      const errorData = await response.json();
      errorMsg = errorData.message || errorData.error || errorMsg;
    } catch (_) {}
    throw new Error(errorMsg);
  }

  if (response.status === 204) return null;
  return await response.json();
}

export const api = {
  get: (url, params) => {
    let finalUrl = url;
    if (params) {
      const q = new URLSearchParams();
      Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== null && v !== '') q.append(k, v);
      });
      const qs = q.toString();
      if (qs) finalUrl += `?${qs}`;
    }
    return request(finalUrl, { method: 'GET' });
  },
  post: (url, body) => request(url, { method: 'POST', body: JSON.stringify(body) }),
  put: (url, body) => request(url, { method: 'PUT', body: JSON.stringify(body) }),
  patch: (url, body) => request(url, { method: 'PATCH', body: JSON.stringify(body) }),
  delete: (url) => request(url, { method: 'DELETE' }),
};

export default api;
