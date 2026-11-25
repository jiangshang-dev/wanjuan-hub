<script setup>
import { onBeforeUnmount, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { resetDynamicRoutes } from './router'
import { useAuthStore } from './stores/auth'

const router = useRouter()
const auth = useAuthStore()

function onLoginRequired() {
  auth.reset()
  resetDynamicRoutes()
  const current = router.currentRoute.value
  if (!current.meta.public) router.replace({ name: 'login', query: { redirect: current.fullPath } })
}

onMounted(() => window.addEventListener('admin:login', onLoginRequired))
onBeforeUnmount(() => window.removeEventListener('admin:login', onLoginRequired))
</script>

<template><router-view /></template>
