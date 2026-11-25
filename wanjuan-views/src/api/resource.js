import request from './request'

export const resourceApi = {
  list: (params) => request.get('/api/resources', { params }),
  detail: (id) => request.get(`/api/resources/${id}`),
  links: (id) => request.get(`/api/resources/${id}/links`),
  attachment: (id, fileId) => request.get(`/api/resources/${id}/attachments/${fileId}`),
  categories: () => request.get('/api/resource-categories'),
  diskTypes: () => request.get('/api/resource-disk-types'),
}

export const userResourceApi = {
  list: (params) => request.get('/api/user/resources', { params }),
  create: (data) => request.post('/api/user/resources', data),
  detail: (id) => request.get(`/api/user/resources/${id}`),
  update: (id, data) => request.put(`/api/user/resources/${id}`, data),
  remove: (id) => request.delete(`/api/user/resources/${id}`),
  submit: (id) => request.post(`/api/user/resources/${id}/submit`),
  versions: (id, params) => request.get(`/api/user/resources/${id}/versions`, { params }),
  entitlements: (params) => request.get('/api/user/resources/entitlements', { params }),
  redeem: (id) => request.post(`/api/user/resources/${id}/redeem`),
  report: (id, data) => request.post(`/api/user/resources/${id}/invalid-reports`, data),
}
