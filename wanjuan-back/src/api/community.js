import request from './request'

export const adminForumApi = {
  boards: (params) => request.get('/api/admin/forum/boards', { params }),
  createBoard: (data) => request.post('/api/admin/forum/boards', data),
  updateBoard: (id, data) => request.put(`/api/admin/forum/boards/${id}`, data),
  deleteBoard: (id) => request.delete(`/api/admin/forum/boards/${id}`),
  moderators: (params) => request.get('/api/admin/forum/moderators', { params }),
  createModerator: (data) => request.post('/api/admin/forum/moderators', data),
  updateModerator: (id, data) => request.put(`/api/admin/forum/moderators/${id}`, data),
  deleteModerator: (id) => request.delete(`/api/admin/forum/moderators/${id}`),
  topics: (params) => request.get('/api/admin/forum/topics', { params }),
  topic: (id) => request.get(`/api/admin/forum/topics/${id}`),
  createTopic: (data) => request.post('/api/admin/forum/topics', data),
  updateTopic: (id, data) => request.put(`/api/admin/forum/topics/${id}`, data),
  deleteTopic: (id) => request.delete(`/api/admin/forum/topics/${id}`),
  topicStatus: (id, data) => request.put(`/api/admin/forum/topics/${id}/status`, data),
  moderateTopic: (id, data) => request.put(`/api/admin/forum/topics/${id}/moderation`, data),
  posts: (params) => request.get('/api/admin/forum/posts', { params }),
  post: (id) => request.get(`/api/admin/forum/posts/${id}`),
  createPost: (data) => request.post('/api/admin/forum/posts', data),
  updatePost: (id, data) => request.put(`/api/admin/forum/posts/${id}`, data),
  deletePost: (id) => request.delete(`/api/admin/forum/posts/${id}`),
  postStatus: (id, data) => request.put(`/api/admin/forum/posts/${id}/status`, data),
}

export const adminCommentApi = {
  list: (params) => request.get('/api/admin/comments', { params }),
  updateStatus: (id, data) => request.put(`/api/admin/comments/${id}/status`, data),
}

export const adminReportApi = {
  list: (params) => request.get('/api/admin/reports', { params }),
  handle: (id, data) => request.put(`/api/admin/reports/${id}/handle`, data),
}
