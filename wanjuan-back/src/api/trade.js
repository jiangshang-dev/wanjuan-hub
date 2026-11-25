import request from './request'

export const adminOrderApi = {
  list: (params) => request.get('/api/admin/orders', { params }),
  detail: (orderNo) => request.get(`/api/admin/orders/${encodeURIComponent(orderNo)}`),
}

export const adminPaymentApi = {
  list: (params) => request.get('/api/admin/payments', { params }),
  detail: (paymentNo) => request.get(`/api/admin/payments/${encodeURIComponent(paymentNo)}`),
  reconcile: (orderNo) => request.post(`/api/admin/payments/${encodeURIComponent(orderNo)}/reconcile`),
}

export const adminPaymentNotificationApi = {
  list: (params) => request.get('/api/admin/payment-notifications', { params }),
  detail: (notificationId) => request.get(`/api/admin/payment-notifications/${encodeURIComponent(notificationId)}`),
}

export const adminReconciliationApi = {
  list: (params) => request.get('/api/admin/reconciliations', { params }),
  detail: (reconciliationNo) => request.get(`/api/admin/reconciliations/${encodeURIComponent(reconciliationNo)}`),
}

export const adminEntitlementApi = {
  list: (params) => request.get('/api/admin/resource-entitlements', { params }),
  detail: (id) => request.get(`/api/admin/resource-entitlements/${encodeURIComponent(id)}`),
}

export const adminRefundApi = {
  list: (params) => request.get('/api/admin/refunds', { params }),
  review: (refundId, data) => request.put(`/api/admin/refunds/${encodeURIComponent(refundId)}/review`, data),
  sync: (refundNo) => request.post(`/api/admin/refunds/${encodeURIComponent(refundNo)}/sync`),
}
