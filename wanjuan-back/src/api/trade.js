import request from './request'

export const adminOrderApi = {
  list: (params) => request.get('/api/admin/orders', { params }),
  detail: (orderNo) => request.get(`/api/admin/orders/${encodeURIComponent(orderNo)}`),
}

export const adminPaymentApi = {
  reconcile: (orderNo) => request.post(`/api/admin/payments/${encodeURIComponent(orderNo)}/reconcile`),
}

export const adminRefundApi = {
  list: (params) => request.get('/api/admin/refunds', { params }),
  review: (refundId, data) => request.put(`/api/admin/refunds/${encodeURIComponent(refundId)}/review`, data),
  sync: (refundNo) => request.post(`/api/admin/refunds/${encodeURIComponent(refundNo)}/sync`),
}
