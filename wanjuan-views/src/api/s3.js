import request from './request'

export const forumApi = {
  boards: () => request.get('/api/forum/boards'),
  topics: (params) => request.get('/api/forum/topics', { params }),
  topic: (id) => request.get(`/api/forum/topics/${id}`),
  posts: (topicId, params) => request.get(`/api/forum/topics/${topicId}/posts`, { params }),
}

export const userForumApi = {
  topics: (params) => request.get('/api/user/forum/topics', { params }),
  topic: (id) => request.get(`/api/user/forum/topics/${id}`),
  createTopic: (data) => request.post('/api/user/forum/topics', data),
  updateTopic: (id, data) => request.put(`/api/user/forum/topics/${id}`, data),
  deleteTopic: (id) => request.delete(`/api/user/forum/topics/${id}`),
  posts: (params) => request.get('/api/user/forum/posts', { params }),
  post: (id) => request.get(`/api/user/forum/posts/${id}`),
  createPost: (topicId, data) => request.post(`/api/user/forum/topics/${topicId}/posts`, data),
  updatePost: (id, data) => request.put(`/api/user/forum/posts/${id}`, data),
  deletePost: (id) => request.delete(`/api/user/forum/posts/${id}`),
}

export const commentApi = {
  list: (params) => request.get('/api/comments', { params }),
  replies: (rootId, params) => request.get(`/api/comments/${rootId}/replies`, { params }),
}

export const userCommentApi = {
  list: (params) => request.get('/api/user/comments', { params }),
  create: (data) => request.post('/api/user/comments', data),
  remove: (id) => request.delete(`/api/user/comments/${id}`),
}

export const interactionApi = {
  like: (data) => request.post('/api/user/likes', data),
  unlike: (data) => request.delete('/api/user/likes', { data }),
  likes: (params) => request.get('/api/user/likes', { params }),
  favorite: (data) => request.post('/api/user/favorites', data),
  unfavorite: (data) => request.delete('/api/user/favorites', { data }),
  favorites: (params) => request.get('/api/user/favorites', { params }),
  follow: (userId) => request.post(`/api/user/follows/${userId}`),
  unfollow: (userId) => request.delete(`/api/user/follows/${userId}`),
  following: (params) => request.get('/api/user/follows/following', { params }),
  followers: (params) => request.get('/api/user/follows/followers', { params }),
}

export const userReportApi = {
  list: (params) => request.get('/api/user/reports', { params }),
  create: (data) => request.post('/api/user/reports', data),
}
