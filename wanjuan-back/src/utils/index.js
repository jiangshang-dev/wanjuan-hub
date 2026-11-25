import dayjs from 'dayjs'

export function errorText(error) {
  if (!error?.response && error?.message === 'Network Error') return '网络连接失败，请检查后端服务是否启动'
  if (error?.code === 'ECONNABORTED') return '请求超时，请稍后重试'
  return error?.response?.data?.message || error?.response?.data?.msg || error?.message || '请求失败，请稍后重试'
}

export function formatDate(value) {
  if (!value) return '-'
  const date = dayjs(value)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : '-'
}

export function pageResult(data) {
  return {
    records: Array.isArray(data?.records) ? data.records : [],
    total: Number(data?.total || 0),
    page: Number(data?.page || 1),
    size: Number(data?.size || 10),
  }
}

export const idOf = (value) => value === null || value === undefined ? '' : String(value)

export function statusText(value) {
  if (value === 1 || value === '1' || value === 'ENABLED' || value === 'ACTIVE' || value === true) return '启用'
  if (value === 0 || value === '0' || value === 'DISABLED' || value === 'INACTIVE' || value === false) return '禁用'
  return value ?? '-'
}

export function isEnabled(value) {
  return value === 1 || value === '1' || value === 'ENABLED' || value === 'ACTIVE' || value === true
}
