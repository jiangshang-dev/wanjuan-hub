<script setup>
import { useRouter } from 'vue-router'
import { firstRoutePath } from '../router'
import { useAuthStore } from '../stores/auth'
const router = useRouter()
const auth = useAuthStore()
</script>

<template>
  <main class="not-found"><a-result status="404" title="404" sub-title="页面不存在或当前账号无权访问"><template #extra><a-button type="primary" @click="router.replace(firstRoutePath(auth.menus) || '/login')">返回工作台</a-button></template></a-result></main>
</template>
