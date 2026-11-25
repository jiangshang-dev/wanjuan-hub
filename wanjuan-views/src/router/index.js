import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { getAccessToken } from '../utils/token'

const routes = [
  {
    path: '/',
    component: () => import('../layouts/AppLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('../views/home/HomeView.vue'), meta: { title: '首页' } },
      { path: 'resources', name: 'resources', component: () => import('../views/resource/ResourceListView.vue'), meta: { title: '资源中心' } },
      { path: 'resources/:id', name: 'resource-detail', component: () => import('../views/resource/ResourceDetailView.vue'), meta: { title: '资源详情' } },
      { path: 'my/resources', name: 'my-resources', component: () => import('../views/resource/MyResourcesView.vue'), meta: { title: '我的发布', requiresAuth: true } },
      { path: 'my/resources/new', name: 'resource-create', component: () => import('../views/resource/ResourceEditorView.vue'), meta: { title: '发布资源', requiresAuth: true } },
      { path: 'my/resources/entitlements', name: 'resource-entitlements', component: () => import('../views/resource/ResourceEntitlementsView.vue'), meta: { title: '我的已购与授权', requiresAuth: true } },
      { path: 'my/resources/:id/edit', name: 'resource-edit', component: () => import('../views/resource/ResourceEditorView.vue'), meta: { title: '编辑资源', requiresAuth: true } },
      { path: 'my/resources/:id/versions', name: 'resource-versions', component: () => import('../views/resource/ResourceVersionsView.vue'), meta: { title: '资源版本', requiresAuth: true } },
      { path: 'blogs', name: 'blogs', component: () => import('../views/blog/BlogListView.vue'), meta: { title: '技术博客' } },
      { path: 'blogs/:id', name: 'blog-detail', component: () => import('../views/blog/BlogDetailView.vue'), meta: { title: '博客详情' } },
      { path: 'forum', name: 'forum', component: () => import('../views/forum/ForumListView.vue'), meta: { title: '技术论坛' } },
      { path: 'forum/topics/:id', name: 'forum-topic', component: () => import('../views/forum/TopicDetailView.vue'), meta: { title: '主题详情' } },
      { path: 'my/forum/topics/new', name: 'forum-topic-create', component: () => import('../views/forum/TopicEditorView.vue'), meta: { title: '发布主题', requiresAuth: true } },
      { path: 'my/forum/topics/:id/edit', name: 'forum-topic-edit', component: () => import('../views/forum/TopicEditorView.vue'), meta: { title: '编辑主题', requiresAuth: true } },
      { path: 'search', name: 'search', component: () => import('../views/search/SearchView.vue'), meta: { title: '全站搜索' } },
      { path: 'my', name: 'user-center', component: () => import('../views/profile/UserCenterView.vue'), meta: { title: '个人中心', requiresAuth: true } },
      { path: 'my/membership', name: 'membership', component: () => import('../views/profile/MembershipView.vue'), meta: { title: '会员中心', requiresAuth: true } },
      { path: 'my/messages', name: 'messages', component: () => import('../views/profile/MessagesView.vue'), meta: { title: '消息中心', requiresAuth: true } },
      { path: 'my/activity', name: 'activity', component: () => import('../views/profile/ActivityCenterView.vue'), meta: { title: '我的互动', requiresAuth: true } },
      { path: 'my/blogs', name: 'my-blogs', component: () => import('../views/blog/MyBlogsView.vue'), meta: { title: '我的博客', requiresAuth: true } },
      { path: 'my/blogs/new', name: 'blog-create', component: () => import('../views/blog/BlogEditorView.vue'), meta: { title: '创作博客', requiresAuth: true } },
      { path: 'my/blogs/:id/edit', name: 'blog-edit', component: () => import('../views/blog/BlogEditorView.vue'), meta: { title: '编辑博客', requiresAuth: true } },
      { path: 'my/blogs/:id/versions', name: 'blog-versions', component: () => import('../views/blog/BlogVersionsView.vue'), meta: { title: '版本记录', requiresAuth: true } },
      { path: 'my/orders', name: 'orders', component: () => import('../views/orders/OrderCenterView.vue'), meta: { title: '订单中心', requiresAuth: true } },
      { path: 'my/orders/:orderNo', name: 'order-detail', component: () => import('../views/orders/OrderDetailView.vue'), meta: { title: '订单详情', requiresAuth: true } },
      { path: 'profile', name: 'profile', component: () => import('../views/profile/ProfileView.vue'), meta: { title: '个人资料', requiresAuth: true } },
      { path: 'security', name: 'security', component: () => import('../views/profile/SecurityView.vue'), meta: { title: '账号安全', requiresAuth: true } },
    ],
  },
  { path: '/login', name: 'login', component: () => import('../views/auth/LoginView.vue'), meta: { title: '登录', guest: true } },
  { path: '/register', name: 'register', component: () => import('../views/auth/RegisterView.vue'), meta: { title: '注册', guest: true } },
  { path: '/reset-password', name: 'reset-password', component: () => import('../views/auth/ResetPasswordView.vue'), meta: { title: '重置密码', guest: true } },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({ history: createWebHistory(), routes, scrollBehavior: () => ({ top: 0 }) })

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.guest && getAccessToken()) return { name: 'home' }
  if (to.meta.requiresAuth) {
    if (!getAccessToken()) return { name: 'login', query: { redirect: to.fullPath } }
    try {
      await auth.loadUser()
    } catch {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
  }
  document.title = `${to.meta.title || '万卷'} - 万卷技术社区`
  return true
})

export default router
