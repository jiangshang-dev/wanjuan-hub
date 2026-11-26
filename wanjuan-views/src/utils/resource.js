const statuses = {
  DRAFT: { text: '草稿', color: 'default' },
  PENDING: { text: '审核中', color: 'processing' },
  PUBLISHED: { text: '已发布', color: 'success' },
  APPROVED: { text: '审核通过', color: 'success' },
  OFFLINE: { text: '已下线', color: 'warning' },
  REJECTED: { text: '已驳回', color: 'error' },
  MANUAL_REVIEW: { text: '人工审核', color: 'warning' },
  INVALID: { text: '已失效', color: 'error' },
  SUPERSEDED: { text: '已被新版本替代', color: 'default' },
  ACTIVE: { text: '有效', color: 'success' },
  REVOKED: { text: '已撤销', color: 'default' },
}

const accessTypes = {
  GUEST_FREE: '游客免费',
  LOGIN_FREE: '登录免费',
  REPLY_REQUIRED: '回复可见',
  FREE: '免费',
  POINTS: '积分兑换',
  MEMBER: '会员领取',
  PAID: '会员领取',
}

export const resourceStatus = (value) => statuses[value] || { text: value || '-', color: 'default' }
export const accessTypeText = (value) => accessTypes[value] || value || '-'
export const accessTypeOptions = Object.entries(accessTypes).map(([value, label]) => ({ value, label }))

export function resourcePrice(resource) {
  if (resource?.accessType === 'POINTS') return `${resource.pointsPrice ?? '-'} 积分`
  if (resource?.accessType === 'PAID') return `¥${resource.price ?? '-'}`
  return accessTypeText(resource?.accessType)
}

export function entitlementSource(value) {
  return ({ POINTS: '积分兑换', PAID: '付费购买', ORDER: '订单购买', ADMIN: '管理员授权', AUTHOR: '作者权益' })[value] || value || '-'
}
