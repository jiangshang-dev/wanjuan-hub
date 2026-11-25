<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { DownOutlined, LogoutOutlined, UserOutlined } from '@ant-design/icons-vue'
import MenuNode from '../components/MenuNode.vue'
import { definitionForMenu, normalizeMenus, resetDynamicRoutes } from '../router'
import { useAuthStore } from '../stores/auth'
import { errorText } from '../utils'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const collapsed = ref(false)
const loggingOut = ref(false)

function filterMenus(menus) {
  return menus.map((menu) => ({ ...menu, children: filterMenus(menu.children || []) }))
    .filter((menu) => definitionForMenu(menu) || menu.children.length)
}
const visibleMenus = computed(() => filterMenus(normalizeMenus(auth.menus)))
const selectedKeys = computed(() => [route.path])
const openKeys = computed(() => visibleMenus.value.filter((menu) => menu.children.length).map((menu) => menu.path || menu.name))
const displayName = computed(() => auth.user?.nickname || auth.user?.username || auth.user?.account || '管理员')

function navigate({ key }) {
  if (String(key).startsWith('/') && key !== route.path) router.push(key)
}

async function logout() {
  loggingOut.value = true
  try {
    await auth.logout()
    message.success('已安全退出')
  } catch (error) {
    message.error(errorText(error))
  } finally {
    resetDynamicRoutes()
    loggingOut.value = false
    router.replace('/login')
  }
}
</script>

<template>
  <a-layout class="admin-layout">
    <a-layout-sider v-model:collapsed="collapsed" class="admin-sider" :width="232" collapsible breakpoint="lg" :trigger="null">
      <div class="admin-brand"><span class="admin-brand-mark">卷</span><span v-if="!collapsed">万卷管理后台</span></div>
      <a-menu theme="dark" mode="inline" :selected-keys="selectedKeys" :default-open-keys="openKeys" @click="navigate">
        <MenuNode v-for="menu in visibleMenus" :key="menu.id || menu.path || menu.name" :menu="menu" />
      </a-menu>
      <button class="collapse-button" type="button" @click="collapsed = !collapsed">{{ collapsed ? '展开' : '收起菜单' }}</button>
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="admin-header">
        <div><h1>{{ route.meta.title || '管理后台' }}</h1><p>万卷技术社区</p></div>
        <a-dropdown placement="bottomRight">
          <button class="admin-user" type="button"><a-avatar :src="auth.user?.avatar" :size="32"><UserOutlined /></a-avatar><span>{{ displayName }}</span><DownOutlined class="down-icon" /></button>
          <template #overlay><a-menu><a-menu-item key="logout" :disabled="loggingOut" @click="logout"><LogoutOutlined />退出登录</a-menu-item></a-menu></template>
        </a-dropdown>
      </a-layout-header>
      <a-layout-content class="admin-content"><router-view /></a-layout-content>
    </a-layout>
  </a-layout>
</template>
