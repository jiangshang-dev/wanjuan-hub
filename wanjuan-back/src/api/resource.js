import request from './request'

export const adminResourceApi = {
  list: (params) => request.get('/api/admin/resources', { params }),
  updateStatus: (id, data) => request.put(`/api/admin/resources/${id}/status`, data),
}

export const adminResourceReportApi = {
  list: (params) => request.get('/api/admin/resource-invalid-reports', { params }),
  handle: (id, data) => request.put(`/api/admin/resource-invalid-reports/${id}/handle`, data),
}

export const adminResourceCategoryApi = {
  publicList: () => request.get('/api/resource-categories'),
  list: (params) => request.get('/api/admin/resource-categories', { params }),
  create: (data) => request.post('/api/admin/resource-categories', data),
  update: (id, data) => request.put(`/api/admin/resource-categories/${id}`, data),
  remove: (id) => request.delete(`/api/admin/resource-categories/${id}`),
}

export const adminResourceTagApi = {
  list: (params) => request.get('/api/admin/resource-tags', { params }),
  create: (data) => request.post('/api/admin/resource-tags', data),
  update: (id, data) => request.put(`/api/admin/resource-tags/${id}`, data),
  remove: (id) => request.delete(`/api/admin/resource-tags/${id}`),
}

export const adminResourceDiskTypeApi = {
  list: (params) => request.get('/api/admin/resource-disk-types', { params }),
  create: (data) => request.post('/api/admin/resource-disk-types', data),
  update: (id, data) => request.put(`/api/admin/resource-disk-types/${id}`, data),
  remove: (id) => request.delete(`/api/admin/resource-disk-types/${id}`),
}
