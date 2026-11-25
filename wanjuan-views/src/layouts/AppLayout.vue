<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import { useAuthStore } from '../stores/auth'
import { displayName } from '../utils/format'
import { errorText } from '../utils/format'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loggingOut = ref(false)
const searchKeyword = ref('')
const selectedKeys = computed(() => {
  if (route.name === 'blog-detail') return ['blogs']
  if (route.name === 'resource-detail') return ['resources']
  if (['resource-create', 'resource-edit', 'resource-versions', 'resource-entitlements'].includes(route.name)) return ['my-resources']
  if (['forum-topic', 'forum-topic-create', 'forum-topic-edit'].includes(route.name)) return ['forum']
  if (['blog-create', 'blog-edit', 'blog-versions'].includes(route.name)) return ['my-blogs']
  if (route.name === 'order-detail') return ['orders']
  if (['profile', 'security', 'activity', 'membership', 'messages'].includes(route.name)) return ['user-center']
  return [route.name]
})
const avatarText = computed(() => displayName(auth.user).slice(0, 1).toUpperCase())

function navigate({ key }) {
  if (key && key !== route.name) router.push({ name: key })
}

function submitSearch(value) {
  const keyword = String(value ?? searchKeyword.value).trim()
  router.push({ name: 'search', query: keyword ? { keyword } : {} })
}

async function logout() {
  loggingOut.value = true
  try {
    await auth.logout()
    message.success('已安全退出')
    router.replace({ name: 'home' })
  } catch (error) {
    message.error(errorText(error))
  } finally {
    loggingOut.value = false
  }
}
</script>

<template>
  <a-layout class="app-layout">
    <a-layout-header class="site-header">
      <div class="header-inner">
        <router-link class="brand" to="/" aria-label="万卷技术社区首页">
          <span class="brand-mark">卷</span>
          <span>万卷技术社区</span>
        </router-link>
        <a-menu class="main-nav" mode="horizontal" :selected-keys="selectedKeys" @click="navigate">
          <a-menu-item key="home">首页</a-menu-item>
          <a-menu-item key="forum">技术论坛</a-menu-item>
          <a-menu-item key="blogs">技术博客</a-menu-item>
          <a-menu-item key="resources">资源中心</a-menu-item>
          <a-menu-item v-if="auth.isLoggedIn" key="my-resources">我的发布</a-menu-item>
          <a-menu-item v-if="auth.isLoggedIn" key="my-blogs">我的博客</a-menu-item>
          <a-menu-item v-if="auth.isLoggedIn" key="user-center">个人中心</a-menu-item>
        </a-menu>
        <div class="header-actions">
          <a-input-search v-model:value="searchKeyword" class="header-search" allow-clear placeholder="搜索博客、论坛和资源" @search="submitSearch" />
          <router-link class="header-search-mobile" to="/search" aria-label="全站搜索"><SearchOutlined /></router-link>
          <template v-if="auth.isLoggedIn">
            <a-dropdown placement="bottomRight">
              <button class="user-trigger" type="button">
                <a-avatar :src="auth.user?.avatar" :size="32">{{ avatarText }}</a-avatar>
                <span class="user-name">{{ displayName(auth.user) }}</span>
              </button>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="user-center" @click="router.push('/my')">个人中心</a-menu-item>
                  <a-menu-item key="membership" @click="router.push('/my/membership')">会员与积分</a-menu-item>
                  <a-menu-item key="messages" @click="router.push('/my/messages')">消息中心</a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="my-blogs" @click="router.push('/my/blogs')">我的博客</a-menu-item>
                  <a-menu-item key="my-resources" @click="router.push('/my/resources')">我的发布</a-menu-item>
                  <a-menu-item key="orders" @click="router.push('/my/orders')">我的订单</a-menu-item>
                  <a-menu-item key="resource-entitlements" @click="router.push('/my/resources/entitlements')">我的已购与授权</a-menu-item>
                  <a-menu-item key="activity" @click="router.push('/my/activity')">我的互动</a-menu-item>
                  <a-menu-item key="profile" @click="router.push('/profile')">个人资料</a-menu-item>
                  <a-menu-item key="security" @click="router.push('/security')">账号安全</a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="logout" :disabled="loggingOut" @click="logout">退出登录</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </template>
          <template v-else>
            <a-button @click="router.push('/login')">登录</a-button>
            <a-button type="primary" @click="router.push('/register')">注册</a-button>
          </template>
        </div>
      </div>
    </a-layout-header>
    <a-layout-content class="site-content">
      <router-view />
    </a-layout-content>
    <a-layout-footer class="site-footer">万卷技术社区 · 专注学习与成长</a-layout-footer>
  </a-layout>
</template>
