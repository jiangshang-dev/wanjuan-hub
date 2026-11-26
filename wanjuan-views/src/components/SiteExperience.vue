<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { PauseOutlined, SoundOutlined } from '@ant-design/icons-vue'
import { useSiteExperienceStore } from '../stores/siteExperience'

const experience = useSiteExperienceStore()
const audioRef = ref(null)
const playing = ref(false)
const playbackBlocked = ref(false)
const playbackFailed = ref(false)
const musicAvailable = computed(() => experience.music.enabled && Boolean(experience.music.audioUrl))
const playerVisible = computed(() => musicAvailable.value && (experience.music.playerVisible || playbackBlocked.value || playbackFailed.value))
const musicSignature = computed(() => [
  experience.music.enabled,
  experience.music.audioUrl,
  experience.music.playMode,
].join('|'))

async function play(manual = false) {
  const audio = audioRef.value
  if (!audio || !musicAvailable.value) return
  try {
    await audio.play()
  } catch (error) {
    playing.value = false
    if (!manual && error?.name === 'NotAllowedError') playbackBlocked.value = true
    else playbackFailed.value = true
  }
}

function togglePlayback() {
  if (playing.value) audioRef.value?.pause()
  else {
    playbackBlocked.value = false
    playbackFailed.value = false
    play(true)
  }
}

watch(() => experience.gray, (gray) => {
  document.documentElement.classList.toggle('gray-day', gray)
}, { immediate: true })

watch(musicSignature, async () => {
  playbackBlocked.value = false
  playbackFailed.value = false
  playing.value = false
  await nextTick()
  if (!musicAvailable.value) {
    audioRef.value?.pause()
    return
  }
  audioRef.value?.load()
  if (experience.music.playMode === 'AUTO') play()
}, { flush: 'post' })

onMounted(() => experience.load())
onBeforeUnmount(() => {
  audioRef.value?.pause()
  document.documentElement.classList.remove('gray-day')
})
</script>

<template>
  <div v-if="experience.gray" class="gray-day-banner" role="status">
    <strong>全站灰日</strong><span>{{ experience.grayReason || '当前处于全站灰日，社区内容以灰色模式展示。' }}</span>
  </div>
  <audio
    v-if="musicAvailable"
    ref="audioRef"
    :src="experience.music.audioUrl"
    loop
    preload="none"
    @play="playing = true; playbackBlocked = false; playbackFailed = false"
    @pause="playing = false"
    @error="playing = false; playbackFailed = true"
  />
  <div v-if="playerVisible" class="background-music-player" :class="{ 'needs-attention': playbackBlocked || playbackFailed }">
    <SoundOutlined v-if="!playing" />
    <span v-if="playbackBlocked">浏览器已阻止自动播放，请手动播放背景音乐。</span>
    <span v-else-if="playbackFailed">背景音乐未能自动播放，可尝试手动播放。</span>
    <span v-else>背景音乐</span>
    <a-button size="small" :type="playbackBlocked || playbackFailed ? 'primary' : 'default'" @click="togglePlayback">
      <PauseOutlined v-if="playing" /><SoundOutlined v-else />{{ playing ? '暂停' : '播放' }}
    </a-button>
  </div>
</template>

<style scoped>
.gray-day-banner { position: relative; z-index: 40; display: flex; padding: 10px 20px; align-items: center; justify-content: center; flex-wrap: wrap; gap: 8px 14px; color: #30343b; border-bottom: 1px solid #b9bdc5; background: #e2e4e8; font-size: 13px; line-height: 1.6; text-align: center; }
.gray-day-banner strong { flex: 0 0 auto; }
.background-music-player { position: fixed; right: 22px; bottom: 22px; z-index: 1000; display: flex; max-width: min(420px, calc(100vw - 32px)); padding: 10px 12px; align-items: center; gap: 9px; border: 1px solid var(--line); border-radius: 10px; color: var(--ink); background: rgba(255, 255, 255, .97); box-shadow: 0 8px 28px rgba(20, 33, 58, .14); font-size: 12px; }
.background-music-player.needs-attention { padding: 13px 14px; font-size: 13px; }
.background-music-player .ant-btn { flex: 0 0 auto; margin-left: auto; }
@media (max-width: 560px) { .background-music-player { right: 16px; bottom: 16px; left: 16px; } .gray-day-banner { padding-inline: 14px; } }
</style>
