import dayjs from 'dayjs'

export const idOf = (value) => (value === null || value === undefined ? '' : String(value))

export function displayName(user) {
  if (!user) return '社区用户'
  if (user.nickname?.trim()) return user.nickname
  const id = idOf(user.id || user.userId)
  return id ? `用户${id.slice(-6)}` : '社区用户'
}

export function formatDate(value, withTime = false) {
  if (!value) return '-'
  const date = dayjs(value)
  return date.isValid() ? date.format(withTime ? 'YYYY-MM-DD HH:mm' : 'YYYY-MM-DD') : '-'
}

export function formatMoney(value) {
  const number = Number(value)
  return Number.isFinite(number) ? number.toFixed(2) : '0.00'
}

export function listResult(result) {
  const data = result?.data ?? result ?? {}
  const records = data.records || data.list || data.rows || data.items || (Array.isArray(data) ? data : [])
  return {
    records,
    total: Number(data.total ?? data.totalCount ?? records.length),
  }
}

export function errorText(error) {
  return error?.response?.data?.message || error?.response?.data?.msg || error?.message || '请求失败，请稍后重试'
}

export function renderMarkdown(marked, DOMPurify, content) {
  if (!content) return ''
  return DOMPurify.sanitize(marked.parse(content, { async: false, breaks: true, gfm: true }))
}
