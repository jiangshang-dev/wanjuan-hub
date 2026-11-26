import request from './request'

export const authApi = {
  captcha: () => request.post('/api/auth/captcha', {}, { skipAuth: true }),
  sendEmailCode: (data) => request.post('/api/auth/code', data, { skipAuth: true }),
  sendSmsCode: (data) => request.post('/api/auth/sms-code', data, { skipAuth: true }),
  login: (data) => request.post('/api/auth/login', data, { skipAuth: true }),
  loginByEmail: (data) => request.post('/api/auth/login/email', data, { skipAuth: true }),
  loginByPhone: (data) => request.post('/api/auth/login/phone', data, { skipAuth: true }),
  register: (data) => request.post('/api/auth/register', data, { skipAuth: true }),
  resetPassword: (data) => request.post('/api/auth/password/reset', data, { skipAuth: true }),
  logout: (refreshToken) => request.post('/api/auth/logout', { refreshToken }, { skipAuth: true }),
  me: () => request.get('/api/auth/me'),
}

export const userApi = {
  profile: () => request.get('/api/user/profile'),
  updateProfile: (data) => request.put('/api/user/profile', data),
  changePassword: (data) => request.post('/api/user/password', data),
  bindEmail: (data) => request.post('/api/user/email/bind', data),
  bindPhone: (data) => request.post('/api/user/phone/bind', data),
  updateInteractionSettings: (data) => request.put('/api/user/interaction-settings', data),
}

export const blogApi = {
  list: (params) => request.get('/api/blogs', { params }),
  detail: (id) => request.get(`/api/blogs/${id}`),
  categories: () => request.get('/api/blog-categories'),
}

export const operationApi = {
  announcements: (limit = 5) => request.get('/api/announcements', { params: { limit } }),
  grayDay: () => request.get('/api/operation/site-experience/gray-day'),
  backgroundMusic: () => request.get('/api/operation/site-experience/background-music'),
}

export const feedbackApi = {
  list: (params) => request.get('/api/user/feedbacks', { params }),
  create: (data) => request.post('/api/user/feedbacks', data),
  detail: (id) => request.get(`/api/user/feedbacks/${encodeURIComponent(String(id))}`),
}

export const userBlogApi = {
  list: (params) => request.get('/api/user/blogs', { params }),
  create: (data) => request.post('/api/user/blogs', data),
  detail: (id) => request.get(`/api/user/blogs/${id}`),
  update: (id, data) => request.put(`/api/user/blogs/${id}`, data),
  remove: (id) => request.delete(`/api/user/blogs/${id}`),
  submit: (id) => request.post(`/api/user/blogs/${id}/submit`),
  versions: (id, params) => request.get(`/api/user/blogs/${id}/versions`, { params }),
}

export const fileApi = {
  uploadImage: (file) => {
    const data = new FormData()
    data.append('file', file)
    return request.post('/api/files/images', data, {
      headers: { 'Content-Type': undefined },
      transformRequest: [(payload, headers) => {
        headers.delete('Content-Type')
        return payload
      }],
    })
  },
}
