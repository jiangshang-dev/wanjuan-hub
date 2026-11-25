<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRightOutlined, CheckCircleOutlined } from '@ant-design/icons-vue'
import { flattenRouteMenus, normalizeMenus, routePathForMenu } from '../../router'
import { useAuthStore } from '../../stores/auth'
import { formatDate } from '../../utils'

const router = useRouter()
const auth = useAuthStore()
const availableMenus = computed(() => flattenRouteMenus(normalizeMenus(auth.menus)).filter((menu) => menu.definition.name !== 'dashboard'))
const displayName = computed(() => auth.user?.nickname || auth.user?.username || auth.user?.account || '管理员')
const roles = computed(() => (auth.user?.roles || auth.user?.roleNames || []).map((role) => typeof role === 'string' ? role : role.name || role.code).filter(Boolean))
const descriptions = {
  'system-admins': '维护管理员账号、状态与角色分配',
  'system-roles': '维护角色并配置角色权限',
  'system-permissions': '维护菜单目录、页面和操作权限',
  'system-users': '查询社区用户并管理账号状态',
  'system-operation-logs': '查询后台关键操作记录',
  'content-blogs': '查询博客详情并管理公开状态',
  'content-blog-categories': '维护博客分类与显示顺序',
  'audit-center': '处理机器审核结果与人工审核任务',
}
</script>

<template>
  <div class="dashboard-page">
    <section class="welcome-panel">
      <div><span class="welcome-label"><CheckCircleOutlined /> 已通过身份验证</span><h2>{{ displayName }}，欢迎回来</h2><p>当前展示的管理入口与权限信息均来自你的账号。</p></div>
      <div class="login-meta"><span>最近登录</span><strong>{{ formatDate(auth.user?.lastLoginTime || auth.user?.lastLoginAt) }}</strong></div>
    </section>
    <div class="account-grid">
      <a-card :bordered="false" class="panel-card" title="账号信息">
        <a-descriptions :column="1" size="small">
          <a-descriptions-item label="账号">{{ auth.user?.username || auth.user?.account || '-' }}</a-descriptions-item>
          <a-descriptions-item label="昵称">{{ auth.user?.nickname || '-' }}</a-descriptions-item>
          <a-descriptions-item label="邮箱">{{ auth.user?.email || '-' }}</a-descriptions-item>
          <a-descriptions-item label="角色"><a-space v-if="roles.length" wrap><a-tag v-for="role in roles" :key="role" color="blue">{{ role }}</a-tag></a-space><span v-else>-</span></a-descriptions-item>
        </a-descriptions>
      </a-card>
      <a-card :bordered="false" class="panel-card" title="我的权限">
        <div class="permission-summary"><strong>{{ auth.permissions.length }}</strong><span>项权限标识</span></div>
        <div v-if="auth.permissions.length" class="permission-list"><a-tag v-for="permission in auth.permissions" :key="permission">{{ permission }}</a-tag></div>
        <a-empty v-else :image="null" description="当前账号未返回权限标识" />
      </a-card>
    </div>
    <section class="workspace-section">
      <div class="section-title"><h3>可用管理功能</h3><span>{{ availableMenus.length }} 个入口</span></div>
      <div class="workspace-grid">
        <button v-for="menu in availableMenus" :key="menu.path" class="workspace-item" type="button" @click="router.push(routePathForMenu(menu))">
          <div><strong>{{ menu.name || menu.definition.title }}</strong><p>{{ descriptions[menu.definition.name] || '进入管理页面' }}</p></div><ArrowRightOutlined />
        </button>
      </div>
      <a-empty v-if="!availableMenus.length" description="暂无可用的其他管理入口" />
    </section>
  </div>
</template>
