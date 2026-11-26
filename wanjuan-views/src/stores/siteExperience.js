import { defineStore } from 'pinia'
import { operationApi } from '../api'

export const useSiteExperienceStore = defineStore('siteExperience', {
  state: () => ({
    gray: false,
    grayReason: '',
    music: { enabled: false, audioUrl: '', playMode: 'MANUAL', playerVisible: false },
    loaded: false,
    loading: false,
  }),
  actions: {
    async load(force = false) {
      if (this.loading || (this.loaded && !force)) return
      this.loading = true
      try {
        const [grayResult, musicResult] = await Promise.allSettled([
          operationApi.grayDay(),
          operationApi.backgroundMusic(),
        ])
        if (grayResult.status === 'fulfilled') {
          this.gray = Boolean(grayResult.value?.gray)
          this.grayReason = grayResult.value?.reason || ''
        }
        if (musicResult.status === 'fulfilled') {
          this.music = {
            enabled: Boolean(musicResult.value?.enabled),
            audioUrl: musicResult.value?.audioUrl || '',
            playMode: musicResult.value?.playMode === 'AUTO' ? 'AUTO' : 'MANUAL',
            playerVisible: Boolean(musicResult.value?.playerVisible),
          }
        }
        this.loaded = true
      } finally {
        this.loading = false
      }
    },
  },
})
