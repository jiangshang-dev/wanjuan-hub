import request from './request'

export const authApi = {
  captcha: () => request.post('/api/auth/captcha', {}, { skipAuth: true }),
  sendCode: (data) => request.post('/api/auth/code', data, { skipAuth: true }),
  login: (data) => request.post('/api/auth/login', data, { skipAuth: true }),
  register: (data) => request.post('/api/auth/register', data, { skipAuth: true }),
  resetPassword: (data) => request.post('/api/auth/password/reset', data, { skipAuth: true }),
  refresh: (refreshToken) => request.post('/api/auth/refresh', { refreshToken }, { skipAuth: true }),
}

export const userApi = {
  profile: () => request.get('/api/user/profile'),
  updateProfile: (data) => request.put('/api/user/profile', data),
  bindEmail: (data) => request.post('/api/user/email/bind', data),
  bindPhone: (data) => request.post('/api/user/phone/bind', data),
  changePassword: (data) => request.post('/api/user/password', data),
  membership: () => request.get('/api/user/membership'),
  levels: () => request.get('/api/user/levels'),
  signIn: () => request.post('/api/user/sign-in'),
  blogs: (params) => request.get('/api/user/blogs', { params }),
  createBlog: (data) => request.post('/api/user/blogs', data),
  updateBlog: (id, data) => request.put(`/api/user/blogs/${id}`, data),
  topics: (params) => request.get('/api/user/forum/topics', { params }),
  createTopic: (data) => request.post('/api/user/forum/topics', data),
  replyTopic: (id, data) => request.post(`/api/user/forum/topics/${id}/posts`, data),
  comments: (params) => request.get('/api/user/comments', { params }),
  createComment: (data) => request.post('/api/user/comments', data),
}

export const blogApi = {
  list: (params) => request.get('/api/blogs', { params }),
  detail: (id) => request.get(`/api/blogs/${id}`),
}

export const forumApi = {
  boards: () => request.get('/api/forum/boards'),
  topics: (params) => request.get('/api/forum/topics', { params }),
  topic: (id) => request.get(`/api/forum/topics/${id}`),
}

export const commentApi = {
  list: (params) => request.get('/api/comments', { params }),
}

export const resourceApi = {
  list: (params) => request.get('/api/resources', { params }),
  detail: (id) => request.get(`/api/resources/${id}`),
  links: (id) => request.get(`/api/resources/${id}/links`),
}

export const orderApi = {
  create: (data) => request.post('/api/orders', data),
  list: (params) => request.get('/api/orders', { params }),
  pay: (orderNo, data) => request.post(`/api/payments/${orderNo}/pay`, data),
}

export const homeApi = {
  announcements: () => request.get('/api/announcements'),
  stats: () => request.get('/api/stats'),
}
