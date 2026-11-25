import dayjs from 'dayjs'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

export const idOf = (value) => (value === null || value === undefined ? '' : String(value))

export function displayName(user) {
  if (!user) return '社区用户'
  return user.nickname?.trim() || user.username || user.account || '社区用户'
}

export function formatDate(value, withTime = false) {
  if (!value) return '-'
  const date = dayjs(value)
  return date.isValid() ? date.format(withTime ? 'YYYY-MM-DD HH:mm' : 'YYYY-MM-DD') : '-'
}

export function errorText(error) {
  if (!error?.response && error?.message === 'Network Error') return '网络连接失败，请检查后端服务是否启动'
  if (error?.code === 'ECONNABORTED') return '请求超时，请稍后重试'
  return error?.response?.data?.message || error?.response?.data?.msg || error?.message || '请求失败，请稍后重试'
}

export function renderMarkdown(content) {
  if (!content) return ''
  const html = marked.parse(content, { async: false, breaks: true, gfm: true })
  return DOMPurify.sanitize(html, { USE_PROFILES: { html: true } })
}
