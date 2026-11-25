import axios from 'axios'
import { clearTokens, getAccessToken, getRefreshToken, saveTokens } from '../utils/token'

function parseJsonWithLongIds(raw) {
  if (typeof raw !== 'string' || !raw) return raw
  try {
    const normalized = raw.replace(/([:\[,]\s*)(-?\d{15,})(?=\s*[,}\]\r\n])/g, '$1"$2"')
    return JSON.parse(normalized)
  } catch {
    return raw
  }
}

function unwrap(response) {
  const payload = response.data
  if (payload && typeof payload === 'object' && 'code' in payload) {
    if (Number(payload.code) !== 200) {
      const error = new Error(payload.message || payload.msg || '请求失败')
      error.response = response
      error.businessCode = payload.code
      throw error
    }
    return payload.data
  }
  return payload
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 20000,
  headers: { 'Content-Type': 'application/json' },
  transformResponse: [parseJsonWithLongIds],
})

let refreshing = null

function notifyLogin() {
  clearTokens()
  window.dispatchEvent(new CustomEvent('auth:login'))
}

async function refreshTokens() {
  if (!refreshing) {
    refreshing = axios.post(`${import.meta.env.VITE_API_BASE_URL || ''}/api/auth/refresh`, {
      refreshToken: getRefreshToken(),
    }, { transformResponse: [parseJsonWithLongIds] })
      .then((response) => {
        const payload = response.data
        if (payload?.code !== undefined && Number(payload.code) !== 200) {
          throw new Error(payload.message || '登录状态已过期')
        }
        const data = payload?.data ?? payload
        if (!data?.accessToken) throw new Error('刷新令牌响应缺少 accessToken')
        saveTokens(data)
        return data.accessToken
      })
      .finally(() => { refreshing = null })
  }
  return refreshing
}

request.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token && !config.skipAuth) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(unwrap, async (error) => {
  const config = error.config || {}
  if (error.response?.status !== 401 || config.skipAuth || config._retry) {
    if (error.response?.status === 401 && !config.skipAuth) notifyLogin()
    return Promise.reject(error)
  }
  if (!getRefreshToken()) {
    notifyLogin()
    return Promise.reject(error)
  }
  config._retry = true
  try {
    const token = await refreshTokens()
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
    return request(config)
  } catch (refreshError) {
    notifyLogin()
    return Promise.reject(refreshError)
  }
})

export default request
