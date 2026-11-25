import { defineStore } from 'pinia'
import { authApi } from '../api'
import { clearTokens, getAccessToken, getRefreshToken, saveTokens } from '../utils/token'

export const useAuthStore = defineStore('admin-auth', {
  state: () => ({ user: null, permissions: [], menus: [], loaded: false, loading: false }),
  getters: {
    isLoggedIn: () => Boolean(getAccessToken()),
  },
  actions: {
    async login(data) {
      const tokens = await authApi.login(data)
      saveTokens(tokens)
      await this.loadSession(true)
    },
    async loadSession(force = false) {
      if (this.loading) return this.user
      if (this.loaded && !force) return this.user
      if (!getAccessToken()) {
        this.loaded = true
        return null
      }
      this.loading = true
      try {
        const data = await authApi.me()
        this.user = data?.admin || data || {}
        this.permissions = Array.isArray(data?.permissions) ? data.permissions : []
        this.menus = Array.isArray(data?.menus) ? data.menus : []
        this.loaded = true
        return this.user
      } finally {
        this.loading = false
      }
    },
    hasPermission(required) {
      if (!required) return true
      const requiredList = Array.isArray(required) ? required : [required]
      return this.permissions.includes('*') || this.permissions.includes('*:*:*') || requiredList.some((item) => this.permissions.includes(item))
    },
    async logout() {
      const refreshToken = getRefreshToken()
      try {
        if (refreshToken) await authApi.logout(refreshToken)
      } finally {
        this.reset()
      }
    },
    reset() {
      clearTokens()
      this.user = null
      this.permissions = []
      this.menus = []
      this.loaded = false
    },
  },
})
