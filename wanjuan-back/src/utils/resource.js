const statuses = {
  DRAFT: { text: '草稿', color: 'default' },
  PENDING: { text: '审核中', color: 'processing' },
  PUBLISHED: { text: '已发布', color: 'success' },
  APPROVED: { text: '审核通过', color: 'success' },
  OFFLINE: { text: '已下线', color: 'warning' },
  REJECTED: { text: '已驳回', color: 'error' },
  MANUAL_REVIEW: { text: '人工审核', color: 'warning' },
  INVALID: { text: '已失效', color: 'error' },
  SUPERSEDED: { text: '已替代', color: 'default' },
  CONFIRMED: { text: '已确认失效', color: 'error' },
}

const accessTypes = {
  GUEST_FREE: '游客免费', LOGIN_FREE: '登录免费', REPLY_REQUIRED: '回复可见', POINTS: '积分兑换', PAID: '付费购买',
}

export const resourceStatus = (value) => statuses[value] || { text: value || '-', color: 'default' }
export const accessTypeText = (value) => accessTypes[value] || value || '-'
export const resourceStatusOptions = ['DRAFT', 'PENDING', 'PUBLISHED', 'OFFLINE', 'REJECTED', 'MANUAL_REVIEW', 'INVALID'].map((value) => ({ value, label: resourceStatus(value).text }))

export function resourcePrice(resource) {
  if (resource?.accessType === 'POINTS') return `${resource.pointsPrice ?? '-'} 积分`
  if (resource?.accessType === 'PAID') return `¥${resource.price ?? '-'}`
  return accessTypeText(resource?.accessType)
}
