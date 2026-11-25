import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { getAccessToken } from '../utils/token'

const routeDefinitions = {
  '/dashboard': { name: 'dashboard', title: '工作台', component: () => import('../views/dashboard/DashboardView.vue') },
  '/system/admins': { name: 'system-admins', title: '管理员管理', component: () => import('../views/system/AdminsView.vue') },
  '/system/roles': { name: 'system-roles', title: '角色管理', component: () => import('../views/system/RolesView.vue') },
  '/system/permissions': { name: 'system-permissions', title: '菜单权限', component: () => import('../views/system/PermissionsView.vue') },
  '/system/users': { name: 'system-users', title: '用户管理', component: () => import('../views/system/UsersView.vue') },
  '/system/operation-logs': { name: 'system-operation-logs', title: '操作日志', component: () => import('../views/system/OperationLogsView.vue') },
  '/content/blogs': { name: 'content-blogs', title: '博客管理', component: () => import('../views/content/BlogsView.vue') },
  '/content/blog-categories': { name: 'content-blog-categories', title: '博客分类', component: () => import('../views/content/BlogCategoriesView.vue') },
  '/forum/boards': { name: 'forum-boards', title: '板块与版主', component: () => import('../views/forum/BoardsView.vue') },
  '/forum/topics': { name: 'forum-topics', title: '主题管理', component: () => import('../views/forum/TopicsView.vue') },
  '/forum/posts': { name: 'forum-posts', title: '跟帖管理', component: () => import('../views/forum/PostsView.vue') },
  '/comments': { name: 'comments', title: '评论管理', component: () => import('../views/moderation/CommentsView.vue') },
  '/reports': { name: 'reports', title: '举报管理', component: () => import('../views/moderation/ReportsView.vue') },
  '/audits': { name: 'audit-center', title: '审核中心', component: () => import('../views/audits/AuditCenterView.vue') },
}
const pathAliases = {
  '/': '/dashboard', '/workbench': '/dashboard',
  '/admins': '/system/admins', '/admin': '/system/admins', '/system/admin': '/system/admins',
  '/roles': '/system/roles', '/system/role': '/system/roles',
  '/permissions': '/system/permissions', '/menus': '/system/permissions', '/system/permission': '/system/permissions', '/system/menus': '/system/permissions',
  '/users': '/system/users', '/system/user': '/system/users',
  '/operation-logs': '/system/operation-logs', '/logs': '/system/operation-logs', '/system/logs': '/system/operation-logs', '/system/operation-log': '/system/operation-logs',
  '/blogs': '/content/blogs', '/blog': '/content/blogs', '/blog/blogs': '/content/blogs', '/blog/articles': '/content/blogs', '/content/articles': '/content/blogs', '/admin/blogs': '/content/blogs',
  '/blog-categories': '/content/blog-categories', '/blog/categories': '/content/blog-categories', '/content/categories': '/content/blog-categories',
  '/forum/board': '/forum/boards', '/content/forum/boards': '/forum/boards', '/admin/forum/boards': '/forum/boards',
  '/forum/topic': '/forum/topics', '/content/forum/topics': '/forum/topics', '/admin/forum/topics': '/forum/topics',
  '/forum/post': '/forum/posts', '/content/forum/posts': '/forum/posts', '/admin/forum/posts': '/forum/posts',
  '/content/comments': '/comments', '/moderation/comments': '/comments', '/comment': '/comments', '/admin/comments': '/comments',
  '/content/reports': '/reports', '/moderation/reports': '/reports', '/report': '/reports', '/admin/reports': '/reports',
  '/audit': '/audits', '/audit/tasks': '/audits', '/audit-center': '/audits', '/system/audits': '/audits',
}
const componentAliases = {
  dashboard: '/dashboard', 'dashboard/index': '/dashboard',
  'system/admin/index': '/system/admins', 'system/admins/index': '/system/admins',
  'system/role/index': '/system/roles', 'system/roles/index': '/system/roles',
  'system/permission/index': '/system/permissions', 'system/permissions/index': '/system/permissions', 'system/menu/index': '/system/permissions',
  'system/user/index': '/system/users', 'system/users/index': '/system/users',
  'system/operation-log/index': '/system/operation-logs', 'system/operation-logs/index': '/system/operation-logs', 'system/log/index': '/system/operation-logs',
  'blog/article/index': '/content/blogs', 'blog/blog/index': '/content/blogs', 'content/blogs/index': '/content/blogs',
  'blog/category/index': '/content/blog-categories', 'content/blog-categories/index': '/content/blog-categories',
  'forum/board/index': '/forum/boards', 'forum/boards/index': '/forum/boards', 'forum/moderator/index': '/forum/boards',
  'forum/topic/index': '/forum/topics', 'forum/topics/index': '/forum/topics',
  'forum/post/index': '/forum/posts', 'forum/posts/index': '/forum/posts',
  'comment/index': '/comments', 'comment/comment/index': '/comments', 'moderation/comments/index': '/comments',
  'report/index': '/reports', 'report/report/index': '/reports', 'moderation/reports/index': '/reports',
  'audit/task/index': '/audits', 'audit/index': '/audits', 'audits/index': '/audits',
}

function normalizedPath(path, parentPath = '') {
  const value = String(path || '').trim()
  if (!value) return ''
  return value.startsWith('/') ? value : `${parentPath || ''}/${value}`.replace(/\/+/g, '/')
}

export function normalizeMenus(menus = [], parentPath = '') {
  return menus.map((menu) => ({
    ...menu,
    path: normalizedPath(menu.path, parentPath),
    children: normalizeMenus(menu.children || [], normalizedPath(menu.path, parentPath)),
  }))
}

export function definitionForMenu(menu) {
  const rawPath = pathAliases[menu.path] || menu.path
  const componentPath = componentAliases[String(menu.component || '').replace(/^\/+|\/+$/g, '')]
  return routeDefinitions[rawPath] || routeDefinitions[componentPath]
}

export function routePathForMenu(menu) {
  return pathAliases[menu.path] || menu.path
}

export function flattenRouteMenus(menus = []) {
  return menus.flatMap((menu) => {
    const current = definitionForMenu(menu) ? [{ ...menu, definition: definitionForMenu(menu) }] : []
    return [...current, ...flattenRouteMenus(menu.children || [])]
  })
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('../views/auth/LoginView.vue'), meta: { title: '管理员登录', public: true } },
    { path: '/', name: 'layout', component: () => import('../layouts/AdminLayout.vue') },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('../views/NotFound.vue'), meta: { title: '页面不存在' } },
  ],
})

let routesReady = false
const dynamicRouteNames = []

export function resetDynamicRoutes() {
  dynamicRouteNames.splice(0).forEach((name) => router.hasRoute(name) && router.removeRoute(name))
  routesReady = false
}

export function buildDynamicRoutes(menus) {
  resetDynamicRoutes()
  const paths = new Set()
  flattenRouteMenus(normalizeMenus(menus)).forEach((menu) => {
    const definition = menu.definition
    const routePath = pathAliases[menu.path] || menu.path
    if (paths.has(routePath)) return
    paths.add(routePath)
    router.addRoute('layout', {
      path: routePath.replace(/^\//, ''),
      name: definition.name,
      component: definition.component,
      meta: { title: menu.name || definition.title, permission: menu.permission, menuPath: menu.path },
    })
    dynamicRouteNames.push(definition.name)
  })
  routesReady = true
  return paths.size
}

export function firstRoutePath(menus) {
  const menu = flattenRouteMenus(normalizeMenus(menus))[0]
  return menu ? (pathAliases[menu.path] || menu.path) : ''
}

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.public) {
    if (!getAccessToken()) return true
    try {
      await auth.loadSession()
      buildDynamicRoutes(auth.menus)
      return firstRoutePath(auth.menus) || true
    } catch {
      return true
    }
  }
  if (!getAccessToken()) return { name: 'login', query: { redirect: to.fullPath } }
  try {
    await auth.loadSession()
    if (!routesReady) {
      buildDynamicRoutes(auth.menus)
      return to.fullPath
    }
    if (to.path === '/') return firstRoutePath(auth.menus) || { name: 'login' }
  } catch {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  document.title = `${to.meta.title || '管理后台'} - 万卷管理后台`
  return true
})

export default router
