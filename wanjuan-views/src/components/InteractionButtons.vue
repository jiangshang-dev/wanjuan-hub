<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { LikeFilled, LikeOutlined, StarFilled, StarOutlined } from '@ant-design/icons-vue'
import { interactionApi } from '../api/s3'
import { collectionOf } from '../utils/blog'
import { errorText } from '../utils/format'
import { useAuthStore } from '../stores/auth'

const props = defineProps({
  targetType: { type: String, required: true },
  targetId: { type: [String, Number], required: true },
  likeCount: { type: [String, Number], default: 0 },
  favoriteCount: { type: [String, Number], default: 0 },
  showFavorite: { type: Boolean, default: true },
  checkState: { type: Boolean, default: false },
  compact: { type: Boolean, default: false },
})
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const state = reactive({ liked: false, favorited: false, likes: Number(props.likeCount || 0), favorites: Number(props.favoriteCount || 0) })
const pending = ref('')

watch(() => [props.likeCount, props.favoriteCount], ([likes, favorites]) => {
  state.likes = Number(likes || 0)
  state.favorites = Number(favorites || 0)
})

function requireLogin() {
  if (auth.isLoggedIn) return true
  router.push({ name: 'login', query: { redirect: route.fullPath } })
  return false
}

async function toggle(kind) {
  if (!requireLogin() || pending.value) return
  const activeKey = kind === 'like' ? 'liked' : 'favorited'
  const countKey = kind === 'like' ? 'likes' : 'favorites'
  const wasActive = state[activeKey]
  pending.value = kind
  try {
    const data = { targetType: props.targetType, targetId: props.targetId }
    await interactionApi[wasActive ? `un${kind}` : kind](data)
    state[activeKey] = !wasActive
    state[countKey] = Math.max(0, state[countKey] + (wasActive ? -1 : 1))
  } catch (error) {
    message.error(errorText(error))
  } finally {
    pending.value = ''
  }
}

onMounted(async () => {
  if (!props.checkState || !auth.isLoggedIn) return
  const params = { page: 1, size: 100, targetType: props.targetType }
  try {
    const requests = [interactionApi.likes(params)]
    if (props.showFavorite) requests.push(interactionApi.favorites(params))
    const [likes, favorites] = await Promise.all(requests)
    state.liked = collectionOf(likes).some((item) => String(item.targetId) === String(props.targetId))
    if (favorites) state.favorited = collectionOf(favorites).some((item) => String(item.targetId) === String(props.targetId))
  } catch {}
})
</script>

<template>
  <div class="interaction-bar" :class="{ compact }">
    <a-button type="text" size="small" :loading="pending === 'like'" :class="{ active: state.liked }" :aria-pressed="state.liked" @click="toggle('like')">
      <LikeFilled v-if="state.liked" /><LikeOutlined v-else /><span>{{ state.likes }}</span><span v-if="!compact">点赞</span>
    </a-button>
    <a-button v-if="showFavorite" type="text" size="small" :loading="pending === 'favorite'" :class="{ active: state.favorited }" :aria-pressed="state.favorited" @click="toggle('favorite')">
      <StarFilled v-if="state.favorited" /><StarOutlined v-else /><span>{{ state.favorites }}</span><span v-if="!compact">收藏</span>
    </a-button>
  </div>
</template>
