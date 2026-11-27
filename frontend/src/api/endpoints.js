import api from './client';

export const warehouseApi = {
  getAll: (search) => api.get('/warehouses', { search }),
  getById: (id) => api.get(`/warehouses/${id}`),
  create: (data) => api.post('/warehouses', data),
  update: (id, data) => api.put(`/warehouses/${id}`, data),
  setStatus: (id, status) => api.patch(`/warehouses/${id}/status`, { status }),
  delete: (id) => api.delete(`/warehouses/${id}`),
};

export const productApi = {
  getAll: (params) => api.get('/products', params),
  getById: (id) => api.get(`/products/${id}`),
  create: (data) => api.post('/products', data),
  update: (id, data) => api.put(`/products/${id}`, data),
  delete: (id) => api.delete(`/products/${id}`),
  getCategories: () => api.get('/products/categories'),
};

export const arrivalApi = {
  getAll: (params) => api.get('/arrivals', params),
  getById: (id) => api.get(`/arrivals/${id}`),
  create: (data) => api.post('/arrivals', data),
  qualityCheck: (id, data) => api.post(`/arrivals/${id}/quality-check`, data),
  confirm: (id) => api.post(`/arrivals/${id}/confirm`),
  reject: (id, reason) => api.post(`/arrivals/${id}/reject`, { reason }),
};

export const buyerApi = {
  getAll: (search) => api.get('/buyers', { search }),
  getById: (id) => api.get(`/buyers/${id}`),
  create: (data) => api.post('/buyers', data),
  update: (id, data) => api.put(`/buyers/${id}`, data),
  delete: (id) => api.delete(`/buyers/${id}`),
  recordPayment: (id, data) => api.post(`/buyers/${id}/payments`, data),
};

export const saleApi = {
  getAll: (params) => api.get('/sales', params),
  getNextNo: () => api.get('/sales/next-no'),
  create: (data) => api.post('/sales', data),
};

export const donationApi = {
  getAll: (receiverId) => api.get('/donations', { receiverId }),
  getEligible: () => api.get('/donations/eligible'),
  getReceivers: () => api.get('/receivers'),
  createReceiver: (data) => api.post('/receivers', data),
  create: (data) => api.post('/donations', data),
  getSummaryByReceiver: () => api.get('/donations/summary/by-receiver'),
};

export const driverApi = {
  getAll: (params) => api.get('/drivers', params),
  getOptions: () => api.get('/drivers/options'),
  getById: (id) => api.get(`/drivers/${id}`),
  create: (data) => api.post('/drivers', data),
  update: (id, data) => api.put(`/drivers/${id}`, data),
  toggleStatus: (id) => api.patch(`/drivers/${id}/status`),
};

export const tripCostApi = {
  getAll: (params) => api.get('/trip-costs', params),
  getMonthly: () => api.get('/trip-costs/monthly'),
  getAvailableTrips: () => api.get('/trip-costs/available-trips'),
  create: (data) => api.post('/trip-costs', data),
  update: (id, data) => api.put(`/trip-costs/${id}`, data),
  delete: (id) => api.delete(`/trip-costs/${id}`),
};

export const alertApi = {
  getAll: (params) => api.get('/alerts', params),
  getSummary: () => api.get('/alerts/summary'),
  getUnreadCount: () => api.get('/alerts/unread-count'),
  markRead: (id) => api.patch(`/alerts/${id}/read`),
  markAllRead: () => api.patch('/alerts/read-all'),
  getSettings: () => api.get('/alerts/settings'),
  updateSettings: (data) => api.put('/alerts/settings', data),
};

export const reportApi = {
  getSummary: (params) => api.get('/reports/summary', params),
  getRevenueByCategory: (params) => api.get('/reports/revenue-by-category', params),
  getTopProducts: (params) => api.get('/reports/top-products', params),
};

export const dashboardApi = {
  getSummary: () => api.get('/dashboard/summary'),
  getInventoryRanking: (limit) => api.get('/dashboard/inventory-ranking', { limit }),
};

import { request as authRequest } from './client';
export const authApi = {
  login: (body) => authRequest('/auth/login', { method: 'POST', body: JSON.stringify(body) }),
  me: () => authRequest('/auth/me'),
};
