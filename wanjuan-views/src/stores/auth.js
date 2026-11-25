import { defineStore } from 'pinia'
import { authApi } from '../api'
import { clearTokens, getAccessToken, getRefreshToken, saveTokens } from '../utils/token'

export const useAuthStore = defineStore('auth', {
  state: () => ({ user: null, loaded: false, loading: false }),
  getters: {
    isLoggedIn: () => Boolean(getAccessToken()),
  },
  actions: {
    async loadUser(force = false) {
      if (this.loading) return this.user
      if (this.loaded && !force) return this.user
      if (!getAccessToken()) {
        this.user = null
        this.loaded = true
        return null
      }
      this.loading = true
      try {
        this.user = await authApi.me()
        this.loaded = true
        return this.user
      } finally {
        this.loading = false
      }
    },
    async authenticate(apiCall) {
      const data = await apiCall()
      saveTokens(data)
      await this.loadUser(true)
      return data
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
      this.loaded = true
    },
  },
})
