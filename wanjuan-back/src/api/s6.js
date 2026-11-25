import request from './request'

export const adminPointsApi = {
  rules: (params) => request.get('/api/admin/points/rules', { params }),
  createRule: (data) => request.post('/api/admin/points/rules', data),
  updateRule: (id, data) => request.put(`/api/admin/points/rules/${id}`, data),
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

export const statisticsApi = {
  dashboard: (days = 7) => request.get('/api/admin/statistics/dashboard', { params: { days } }),
  refreshDaily: (statDate) => request.post('/api/admin/statistics/daily/refresh', null, { params: { statDate } }),
}
