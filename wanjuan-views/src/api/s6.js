import request from './request'

export const pointsApi = {
  account: () => request.get('/api/user/points'),
  records: (params) => request.get('/api/user/points/records', { params }),
  membership: () => request.get('/api/user/membership'),
  levels: () => request.get('/api/user/levels'),
  signIn: () => request.post('/api/user/sign-in'),
}

export const messageApi = {
  list: (params) => request.get('/api/user/messages', { params }),
  unreadCount: () => request.get('/api/user/messages/unread-count'),
  markRead: (id) => request.put(`/api/user/messages/${id}/read`),
  markAllRead: () => request.put('/api/user/messages/read-all'),
  preferences: () => request.get('/api/user/message-preferences'),
  updatePreferences: (data) => request.put('/api/user/message-preferences', data),
}

export const searchApi = {
  search: (params) => request.get('/api/search', { params }),
  hotKeywords: (limit = 10) => request.get('/api/search/hot-keywords', { params: { limit } }),
  history: (limit = 20) => request.get('/api/search/history', { params: { limit } }),
}
