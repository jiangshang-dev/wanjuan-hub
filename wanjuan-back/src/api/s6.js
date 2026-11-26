import request from './request'

export const adminPointsApi = {
  rules: (params) => request.get('/api/admin/points/rules', { params }),
  createRule: (data) => request.post('/api/admin/points/rules', data),
  updateRule: (id, data) => request.put(`/api/admin/points/rules/${id}`, data),
  signInRules: () => request.get('/api/admin/points/sign-in-rules'),
  createSignInRule: (data) => request.post('/api/admin/points/sign-in-rules', data),
  updateSignInRule: (id, data) => request.put(`/api/admin/points/sign-in-rules/${id}`, data),
  deleteSignInRule: (id) => request.delete(`/api/admin/points/sign-in-rules/${id}`),
  levels: () => request.get('/api/admin/points/levels'),
  createLevel: (data) => request.post('/api/admin/points/levels', data),
  updateLevel: (id, data) => request.put(`/api/admin/points/levels/${id}`, data),
  adjustments: (params) => request.get('/api/admin/points/adjustments', { params }),
  adjust: (data) => request.post('/api/admin/points/adjustments', data),
}

export const adminMessageApi = {
  sendSystem: (data) => request.post('/api/admin/messages/system', data),
}

const operationEndpoints = {
  announcements: 'announcements',
  recommendations: 'recommendations',
  advertisements: 'advertisements',
  'friend-links': 'friend-links',
  'hot-searches': 'hot-searches',
  configs: 'configs',
}

export const adminOperationApi = {
  list: (type, params) => request.get(`/api/admin/operation/${operationEndpoints[type]}`, { params }),
  create: (type, data) => request.post(`/api/admin/operation/${operationEndpoints[type]}`, data),
  update: (type, id, data) => request.put(`/api/admin/operation/${operationEndpoints[type]}/${id}`, data),
  updateStatus: (type, id, status) => request.put(`/api/admin/operation/${operationEndpoints[type]}/${id}/status`, { status }),
  remove: (type, id) => request.delete(`/api/admin/operation/${operationEndpoints[type]}/${id}`),
}

export const adminSiteExperienceApi = {
  grayDays: (params) => request.get('/api/admin/operation/site-experience/gray-days', { params }),
  createGrayDay: (data) => request.post('/api/admin/operation/site-experience/gray-days', data),
  updateGrayDay: (id, data) => request.put(`/api/admin/operation/site-experience/gray-days/${id}`, data),
  updateGrayDayStatus: (id, status) => request.put(`/api/admin/operation/site-experience/gray-days/${id}/status`, { status }),
  backgroundMusic: () => request.get('/api/admin/operation/site-experience/background-music'),
  saveBackgroundMusic: (data) => request.put('/api/admin/operation/site-experience/background-music', data),
}

export const adminFeedbackApi = {
  list: (params) => request.get('/api/admin/operation/feedbacks', { params }),
  reply: (id, data) => request.put(`/api/admin/operation/feedbacks/${id}/reply`, data),
  close: (id) => request.put(`/api/admin/operation/feedbacks/${id}/close`),
}

export const adminMembershipApi = {
  list: (params) => request.get('/api/admin/membership-plans', { params }),
  create: (data) => request.post('/api/admin/membership-plans', data),
  update: (id, data) => request.put(`/api/admin/membership-plans/${id}`, data),
  commissionConfig: () => request.get('/api/admin/membership/commission-config'),
  updateCommissionConfig: (data) => request.put('/api/admin/membership/commission-config', data),
}

export const adminWithdrawalApi = {
  list: (params) => request.get('/api/admin/wallet/withdrawals', { params }),
  detail: (id) => request.get(`/api/admin/wallet/withdrawals/${id}`),
  handle: (id, data) => request.put(`/api/admin/wallet/withdrawals/${id}/handle`, data),
}

export const statisticsApi = {
  dashboard: (days = 7) => request.get('/api/admin/statistics/dashboard', { params: { days } }),
  refreshDaily: (statDate) => request.post('/api/admin/statistics/daily/refresh', null, { params: { statDate } }),
}
