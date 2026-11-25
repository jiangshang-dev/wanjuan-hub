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

const orderStatuses = {
  PENDING_PAYMENT: { text: '待支付', color: 'processing' },
  PAID: { text: '已支付', color: 'success' },
  CLOSED: { text: '已关闭', color: 'default' },
  REFUNDING: { text: '退款中', color: 'warning' },
  REFUNDED: { text: '已退款', color: 'default' },
}

const refundStatuses = {
  PENDING_REVIEW: { text: '待审核', color: 'processing' },
  APPROVED: { text: '已同意', color: 'processing' },
  PROCESSING: { text: '退款处理中', color: 'processing' },
  SUCCEEDED: { text: '退款成功', color: 'success' },
  REJECTED: { text: '已拒绝', color: 'default' },
  ABNORMAL: { text: '退款异常', color: 'error' },
}

const paymentStatuses = {
  CREATED: { text: '待发起', color: 'default' },
  PAYING: { text: '支付中', color: 'processing' },
  SUCCEEDED: { text: '支付成功', color: 'success' },
  CLOSED: { text: '已关闭', color: 'default' },
  REFUNDED: { text: '已退款', color: 'default' },
}

export function orderStatus(value) {
  return orderStatuses[value] || { text: value || '未知', color: 'default' }
}

export function refundStatus(value) {
  return refundStatuses[value] || { text: value || '未知', color: 'default' }
}

export function paymentStatus(value) {
  return paymentStatuses[value] || { text: value || '未知', color: 'default' }
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
  return { PAGE: '电脑网站支付', WAP: '手机网站支付', NATIVE: 'Native 扫码支付', H5: '微信 H5 支付' }[value] || value || '-'
}

export function money(value, currency = 'CNY') {
  if (value === null || value === undefined || value === '') return '-'
  const amount = Number(value)
  if (!Number.isFinite(amount)) return String(value)
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: currency || 'CNY', minimumFractionDigits: 2 }).format(amount)
}

export function pageRecords(data) {
  return Array.isArray(data?.records) ? data.records : []
}
