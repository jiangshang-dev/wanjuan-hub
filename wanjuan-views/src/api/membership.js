import request from './request'

export const paidMembershipApi = {
  status: () => request.get('/api/user/paid-membership'),
  createOrder: (data) => request.post('/api/user/paid-membership/orders', data),
  plans: () => request.get('/api/membership/plans'),
}

export const walletApi = {
  account: () => request.get('/api/user/wallet'),
  ledgers: (params) => request.get('/api/user/wallet/ledgers', { params }),
  withdrawals: (params) => request.get('/api/user/wallet/withdrawals', { params }),
  applyWithdrawal: (data) => request.post('/api/user/wallet/withdrawals', data),
}
