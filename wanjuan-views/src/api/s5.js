import request from './request'

export const orderApi = {
  create: (data) => request.post('/api/user/orders', data),
  list: (params) => request.get('/api/user/orders', { params }),
  detail: (orderNo) => request.get(`/api/user/orders/${encodeURIComponent(orderNo)}`),
}

export const paymentApi = {
  create: (orderNo, data) => request.post(`/api/user/payments/${encodeURIComponent(orderNo)}`, data),
  detail: (orderNo) => request.get(`/api/user/payments/${encodeURIComponent(orderNo)}`),
  close: (orderNo) => request.post(`/api/user/payments/${encodeURIComponent(orderNo)}/close`),
}

export const refundApi = {
  request: (orderNo, data) => request.post(`/api/user/refunds/${encodeURIComponent(orderNo)}`, data),
  list: (params) => request.get('/api/user/refunds', { params }),
}
