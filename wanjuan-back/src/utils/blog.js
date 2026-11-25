const blogStatuses = {
  DRAFT: ['草稿', 'default'], PENDING: ['审核中', 'processing'], PUBLISHED: ['已公开', 'success'],
  REJECTED: ['已驳回', 'error'], MANUAL_REVIEW: ['人工审核', 'warning'], OFFLINE: ['已下线', 'default'],
}
const auditStatuses = {
  PENDING: ['待审核', 'default'], PROCESSING: ['审核中', 'processing'], APPROVED: ['已通过', 'success'],
  REJECTED: ['已驳回', 'error'], MANUAL_REVIEW: ['待人工审核', 'warning'], FAILED: ['审核失败', 'error'],
}

export function blogStatus(status) {
  const [text, color] = blogStatuses[status] || [status || '未知', 'default']
  return { text, color }
}
export function auditStatus(status) {
  const [text, color] = auditStatuses[status] || [status || '未知', 'default']
  return { text, color }
}
export function blogAuthor(blog) {
  return blog?.authorNickname || blog?.authorName || blog?.author?.nickname || blog?.author?.username || '-'
}
export function blogTags(blog) {
  return blog?.tags || blog?.tagList || []
}
export function tagName(tag) {
  return typeof tag === 'object' ? tag.name : String(tag)
}
