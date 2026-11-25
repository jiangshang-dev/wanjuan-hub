<script setup>
import { computed } from 'vue'
import { DashboardOutlined, TeamOutlined, SafetyCertificateOutlined, MenuOutlined, UserOutlined, FileTextOutlined, FolderOutlined } from '@ant-design/icons-vue'
import { definitionForMenu, routePathForMenu } from '../router'

const props = defineProps({ menu: { type: Object, required: true } })
const isRoute = computed(() => Boolean(definitionForMenu(props.menu)))
const iconComponent = computed(() => {
  const value = String(props.menu.icon || '').toLowerCase()
  if (value.includes('dashboard') || value.includes('workbench')) return DashboardOutlined
  if (value.includes('role') || value.includes('safety')) return SafetyCertificateOutlined
  if (value.includes('permission') || value.includes('menu')) return MenuOutlined
  if (value.includes('admin') || value.includes('team')) return TeamOutlined
  if (value.includes('user')) return UserOutlined
  if (value.includes('log')) return FileTextOutlined
  return isRoute.value ? FileTextOutlined : FolderOutlined
})
</script>

<template>
  <a-menu-item v-if="isRoute" :key="routePathForMenu(menu)">
    <component :is="iconComponent" /><span>{{ menu.name || menu.title }}</span>
  </a-menu-item>
  <a-sub-menu v-else-if="menu.children?.length" :key="menu.path || menu.name">
    <template #title><component :is="iconComponent" /><span>{{ menu.name || menu.title }}</span></template>
    <MenuNode v-for="child in menu.children" :key="child.id || child.path || child.name" :menu="child" />
  </a-sub-menu>
</template>
