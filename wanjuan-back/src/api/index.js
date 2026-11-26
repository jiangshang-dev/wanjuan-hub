import request from './request'

export const authApi = {
  captcha: () => request.post('/api/auth/captcha', {}, { skipAuth: true }),
  login: (data) => request.post('/api/admin/auth/login', data, { skipAuth: true }),
  logout: (refreshToken) => request.post('/api/admin/auth/logout', { refreshToken }, { skipAuth: true }),
  me: () => request.get('/api/admin/auth/me'),
}

function resourceApi(resource) {
  const base = `/api/admin/system/${resource}`
  return {
    list: (params) => request.get(base, { params }),
    create: (data) => request.post(base, data),
    update: (id, data) => request.put(`${base}/${id}`, data),
    remove: (id) => request.delete(`${base}/${id}`),
    updateStatus: (id, status) => request.put(`${base}/${id}/status`, { status }),
  }
}

export const adminsApi = {
  ...resourceApi('admins'),
  assignRoles: (id, ids) => request.put(`/api/admin/system/admins/${id}/roles`, { ids }),
}
export const rolesApi = {
  ...resourceApi('roles'),
  assignPermissions: (id, ids) => request.put(`/api/admin/system/roles/${id}/permissions`, { ids }),
}
export const permissionsApi = resourceApi('permissions')
export const usersApi = resourceApi('users')
export const operationLogsApi = resourceApi('operation-logs')

export const adminBlogApi = {
  list: (params) => request.get('/api/admin/blogs', { params }),
  updateStatus: (id, data) => request.put(`/api/admin/blogs/${id}/status`, data),
}

export const adminBlogCategoryApi = {
  list: (params) => request.get('/api/admin/blog-categories', { params }),
  create: (data) => request.post('/api/admin/blog-categories', data),
  update: (id, data) => request.put(`/api/admin/blog-categories/${id}`, data),
  remove: (id) => request.delete(`/api/admin/blog-categories/${id}`),
}

export const auditApi = {
  list: (params) => request.get('/api/admin/audits', { params }),
  detail: (id) => request.get(`/api/admin/audits/${id}`),
  retry: (id) => request.post(`/api/admin/audits/${id}/retry`),
  review: (id, data) => request.post(`/api/admin/audits/${id}/review`, data),
}
