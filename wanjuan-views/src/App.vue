<script setup>
import { onBeforeUnmount, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'

const router = useRouter()
const auth = useAuthStore()

function onLoginRequired() {
  auth.reset()
  const current = router.currentRoute.value
  if (!current.meta.guest) {
    router.replace({ name: 'login', query: { redirect: current.fullPath } })
  }
}

onMounted(() => {
  window.addEventListener('auth:login', onLoginRequired)
  auth.loadUser().catch(() => {})
})
onBeforeUnmount(() => window.removeEventListener('auth:login', onLoginRequired))
</script>

<template>
  <router-view />
</template>
