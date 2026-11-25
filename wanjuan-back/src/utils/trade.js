export const orderStatusOptions = [
  { value: 'PENDING_PAYMENT', label: '待支付' },
  { value: 'PAID', label: '已支付' },
  { value: 'CLOSED', label: '已关闭' },
  { value: 'REFUNDING', label: '退款中' },
  { value: 'REFUNDED', label: '已退款' },
]

export const refundStatusOptions = [
  { value: 'PENDING_REVIEW', label: '待审核' },
  { value: 'APPROVED', label: '已同意' },
  { value: 'PROCESSING', label: '退款处理中' },
  { value: 'SUCCEEDED', label: '退款成功' },
  { value: 'REJECTED', label: '已拒绝' },
  { value: 'ABNORMAL', label: '退款异常' },
]

export const paymentChannelOptions = [
  { value: 'ALIPAY', label: '支付宝' },
  { value: 'WECHAT', label: '微信支付' },
]

export const paymentStatusOptions = [
  { value: 'CREATED', label: '待发起' },
  { value: 'PAYING', label: '支付中' },
  { value: 'SUCCEEDED', label: '支付成功' },
  { value: 'CLOSED', label: '已关闭' },
  { value: 'REFUNDED', label: '已退款' },
]

export const notificationStatusOptions = [
  { value: 'RECEIVED', label: '已接收' },
  { value: 'PROCESSED', label: '已处理' },
  { value: 'IGNORED', label: '已忽略' },
]

export const reconciliationResultOptions = [
  { value: 'MATCHED', label: '一致' },
  { value: 'REPAIRED', label: '已修复' },
  { value: 'MISMATCH', label: '不一致' },
  { value: 'CHANNEL_PENDING', label: '渠道待支付' },
]

export const entitlementStatusOptions = [
  { value: 'ACTIVE', label: '生效中' },
  { value: 'REVOKED', label: '已撤销' },
]

export function orderStatus(value) {
  return {
    PENDING_PAYMENT: { text: '待支付', color: 'processing' },
    PAID: { text: '已支付', color: 'success' },
    CLOSED: { text: '已关闭', color: 'default' },
    REFUNDING: { text: '退款中', color: 'warning' },
    REFUNDED: { text: '已退款', color: 'default' },
  }[value] || { text: value || '未知', color: 'default' }
}

export function refundStatus(value) {
  return {
    PENDING_REVIEW: { text: '待审核', color: 'processing' },
    APPROVED: { text: '已同意', color: 'processing' },
    PROCESSING: { text: '退款处理中', color: 'processing' },
    SUCCEEDED: { text: '退款成功', color: 'success' },
    REJECTED: { text: '已拒绝', color: 'default' },
    ABNORMAL: { text: '退款异常', color: 'error' },
  }[value] || { text: value || '未知', color: 'default' }
}

export function paymentStatus(value) {
  return {
    CREATED: { text: '待发起', color: 'default' },
    PAYING: { text: '支付中', color: 'processing' },
    SUCCEEDED: { text: '支付成功', color: 'success' },
    CLOSED: { text: '已关闭', color: 'default' },
    REFUNDED: { text: '已退款', color: 'default' },
  }[value] || { text: value || '未知', color: 'default' }
}

export function grantStatus(value) {
  return {
    NONE: { text: '未授权', color: 'default' },
    PENDING: { text: '授权中', color: 'processing' },
    GRANTED: { text: '已授权', color: 'success' },
    REVOKED: { text: '已撤销', color: 'default' },
  }[value] || { text: value || '未知', color: 'default' }
}

export function channelName(value) {
  return { ALIPAY: '支付宝', WECHAT: '微信支付' }[value] || value || '-'
}

export function sceneName(value) {
  return { PAGE: 'PAGE', WAP: 'WAP', NATIVE: 'Native', H5: 'H5' }[value] || value || '-'
}

export function money(value, currency = 'CNY') {
  if (value === null || value === undefined || value === '') return '-'
  const amount = Number(value)
  if (!Number.isFinite(amount)) return String(value)
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: currency || 'CNY', minimumFractionDigits: 2 }).format(amount)
}

export function reconciliationResult(value) {
  return {
    MATCHED: { text: '一致', color: 'success' },
    REPAIRED: { text: '已修复', color: 'success' },
    CHANNEL_PENDING: { text: '渠道待支付', color: 'processing' },
    MISMATCH: { text: '不一致', color: 'error' },
  }[value] || { text: value || '未知', color: 'default' }
}

export function notificationStatus(value) {
  return {
    RECEIVED: { text: '已接收', color: 'processing' },
    PROCESSED: { text: '已处理', color: 'success' },
    IGNORED: { text: '已忽略', color: 'default' },
  }[value] || { text: value || '未知', color: 'default' }
}

export function entitlementStatus(value) {
  return {
    ACTIVE: { text: '生效中', color: 'success' },
    REVOKED: { text: '已撤销', color: 'default' },
  }[value] || { text: value || '未知', color: 'default' }
}

export function entitlementSource(value) {
  return { PAID: '付费订单', POINTS: '积分兑换' }[value] || value || '-'
}
