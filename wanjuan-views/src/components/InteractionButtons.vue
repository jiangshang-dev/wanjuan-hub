<script setup>
import { reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { DislikeFilled, DislikeOutlined, LikeFilled, LikeOutlined, StarFilled, StarOutlined } from '@ant-design/icons-vue'
import { interactionApi } from '../api/s3'
import { collectionOf } from '../utils/blog'
import { errorText } from '../utils/format'
import { useAuthStore } from '../stores/auth'

const props = defineProps({
  targetType: { type: String, required: true },
  targetId: { type: [String, Number], required: true },
  likeCount: { type: [String, Number], default: 0 },
  dislikeCount: { type: [String, Number], default: 0 },
  favoriteCount: { type: [String, Number], default: 0 },
  showFavorite: { type: Boolean, default: true },
  checkState: { type: Boolean, default: false },
  compact: { type: Boolean, default: false },
})
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const state = reactive({
  userReaction: 'NONE',
  likes: Number(props.likeCount || 0),
  dislikes: Number(props.dislikeCount || 0),
  favorites: Number(props.favoriteCount || 0),
  favorited: false,
  likeAllowed: true,
  dislikeAllowed: true,
  grayDay: false,
  grayReason: '',
})
const pending = ref('')

watch(() => [props.likeCount, props.dislikeCount, props.favoriteCount], ([likes, dislikes, favorites]) => {
  state.likes = Number(likes || 0)
  state.dislikes = Number(dislikes || 0)
  state.favorites = Number(favorites || 0)
})

function applyReaction(data) {
  if (!data) return
  state.userReaction = data.userReaction || 'NONE'
  state.likes = Number(data.likeCount || 0)
  state.dislikes = Number(data.dislikeCount || 0)
  state.likeAllowed = data.likeAllowed !== false
  state.dislikeAllowed = data.dislikeAllowed !== false
  state.grayDay = Boolean(data.grayDay)
  state.grayReason = data.grayReason || ''
}

async function loadReaction() {
  try {
    applyReaction(await interactionApi.reactionStatus({ targetType: props.targetType, targetId: String(props.targetId) }))
  } catch {}
}

async function loadFavoriteState() {
  state.favorited = false
  if (!props.showFavorite || !props.checkState || !auth.isLoggedIn) return
  try {
    const data = await interactionApi.favorites({ page: 1, size: 100, targetType: props.targetType })
    state.favorited = collectionOf(data).some((item) => String(item.targetId) === String(props.targetId))
  } catch {}
}

function requireLogin() {
  if (auth.isLoggedIn) return true
  router.push({ name: 'login', query: { redirect: route.fullPath } })
  return false
}

function reactionName(type) {
  return type === 'LIKE' ? '点赞' : '点踩'
}

function reactionDisabled(type) {
  if (state.grayDay) return true
  if (state.userReaction === type) return false
  return type === 'LIKE' ? !state.likeAllowed : !state.dislikeAllowed
}

function reactionHint(type) {
  const name = reactionName(type)
  if (state.grayDay) return state.grayReason || `灰日期间暂停${name}与取消${name}`
  if (state.userReaction === type) return `取消${name}`
  if ((type === 'LIKE' && !state.likeAllowed) || (type === 'DISLIKE' && !state.dislikeAllowed)) return `作者已关闭${name}`
  return state.userReaction === 'NONE' ? name : `切换为${name}`
}

function updateLocalReaction(type, wasActive) {
  const previous = state.userReaction
  if (wasActive) {
    state.userReaction = 'NONE'
    if (type === 'LIKE') state.likes = Math.max(0, state.likes - 1)
    else state.dislikes = Math.max(0, state.dislikes - 1)
    return
  }
  if (previous === 'LIKE') state.likes = Math.max(0, state.likes - 1)
  if (previous === 'DISLIKE') state.dislikes = Math.max(0, state.dislikes - 1)
  state.userReaction = type
  if (type === 'LIKE') state.likes += 1
  else state.dislikes += 1
}

async function toggleReaction(type) {
  if (!requireLogin() || pending.value) return
  const name = reactionName(type)
  if (state.grayDay) {
    message.warning(state.grayReason || `灰日期间暂停${name}与取消${name}`)
    return
  }
  const wasActive = state.userReaction === type
  if (!wasActive && reactionDisabled(type)) {
    message.warning(`作者已关闭${name}`)
    return
  }
  pending.value = type
  try {
    const data = { targetType: props.targetType, targetId: String(props.targetId), reactionType: type }
    const result = await interactionApi[wasActive ? 'unreact' : 'react'](data)
    if (result?.userReaction) applyReaction(result)
    else updateLocalReaction(type, wasActive)
    message.success(wasActive ? `已取消${name}` : `已${name}`)
    await loadReaction()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    pending.value = ''
  }
}

async function toggleFavorite() {
  if (!requireLogin() || pending.value) return
  const wasActive = state.favorited
  pending.value = 'favorite'
  try {
    const data = { targetType: props.targetType, targetId: String(props.targetId) }
    await interactionApi[wasActive ? 'unfavorite' : 'favorite'](data)
    state.favorited = !wasActive
    state.favorites = Math.max(0, state.favorites + (wasActive ? -1 : 1))
  } catch (error) {
    message.error(errorText(error))
  } finally {
    pending.value = ''
  }
}

watch(() => [props.targetType, props.targetId, props.checkState], () => {
  state.userReaction = 'NONE'
  loadReaction()
  loadFavoriteState()
}, { immediate: true })
watch(() => auth.user?.id, () => {
  loadReaction()
  loadFavoriteState()
})
</script>

<template>
  <div class="interaction-bar" :class="{ compact }">
    <a-button type="text" size="small" :loading="pending === 'LIKE'" :disabled="reactionDisabled('LIKE')" :class="{ active: state.userReaction === 'LIKE' }" :aria-pressed="state.userReaction === 'LIKE'" :title="reactionHint('LIKE')" @click="toggleReaction('LIKE')">
      <LikeFilled v-if="state.userReaction === 'LIKE'" /><LikeOutlined v-else /><span>{{ state.likes }}</span><span v-if="!compact">点赞</span>
    </a-button>
    <a-button type="text" size="small" :loading="pending === 'DISLIKE'" :disabled="reactionDisabled('DISLIKE')" :class="{ 'dislike-active': state.userReaction === 'DISLIKE' }" :aria-pressed="state.userReaction === 'DISLIKE'" :title="reactionHint('DISLIKE')" @click="toggleReaction('DISLIKE')">
      <DislikeFilled v-if="state.userReaction === 'DISLIKE'" /><DislikeOutlined v-else /><span>{{ state.dislikes }}</span><span v-if="!compact">点踩</span>
    </a-button>
    <a-button v-if="showFavorite" type="text" size="small" :loading="pending === 'favorite'" :class="{ active: state.favorited }" :aria-pressed="state.favorited" :title="state.favorited ? '取消收藏' : '收藏'" @click="toggleFavorite">
      <StarFilled v-if="state.favorited" /><StarOutlined v-else /><span>{{ state.favorites }}</span><span v-if="!compact">收藏</span>
    </a-button>
  </div>
</template>
