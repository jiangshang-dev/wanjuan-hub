export const blogStatuses = {
  DRAFT: { text: '草稿', color: 'default' },
  PENDING: { text: '审核中', color: 'processing' },
  PUBLISHED: { text: '已发布', color: 'success' },
  APPROVED: { text: '已通过', color: 'success' },
  REJECTED: { text: '已驳回', color: 'error' },
  MANUAL_REVIEW: { text: '人工审核', color: 'warning' },
  OFFLINE: { text: '已下线', color: 'default' },
  SUPERSEDED: { text: '历史版本', color: 'default' },
}

export function blogStatus(status) {
  return blogStatuses[status] || { text: status || '未知', color: 'default' }
}

export function collectionOf(data) {
  if (Array.isArray(data)) return data
  return data?.records || data?.list || data?.items || []
}

export function pageOf(data) {
  return { records: collectionOf(data), total: Number(data?.total || 0) }
}

export function blogCover(blog) {
  return blog?.coverUrl || blog?.coverAccessUrl || blog?.cover?.accessUrl || ''
}

export function blogAuthor(blog) {
  return blog?.authorNickname || blog?.authorName || blog?.author?.nickname || blog?.author?.username || '社区作者'
}

export function blogTags(blog) {
  return blog?.tags || blog?.tagList || []
}

export function tagId(tag) {
  return typeof tag === 'object' ? tag.id : tag
}

export function tagName(tag) {
  return typeof tag === 'object' ? tag.name : String(tag)
}
