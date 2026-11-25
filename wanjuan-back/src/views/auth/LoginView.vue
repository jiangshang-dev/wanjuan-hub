<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { LockOutlined, UserOutlined } from '@ant-design/icons-vue'
import { useAuthStore } from '../../stores/auth'
import { buildDynamicRoutes, firstRoutePath, resetDynamicRoutes } from '../../router'
import { errorText } from '../../utils'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const form = reactive({ account: '', password: '' })
const loading = ref(false)
const errorMessage = ref('')
const rules = { account: [{ required: true, whitespace: true, message: '请输入管理员账号' }], password: [{ required: true, message: '请输入密码' }] }

async function submit() {
  loading.value = true
  errorMessage.value = ''
  try {
    await auth.login({ account: form.account.trim(), password: form.password })
    resetDynamicRoutes()
    buildDynamicRoutes(auth.menus)
    const fallback = firstRoutePath(auth.menus)
    const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/') && !route.query.redirect.startsWith('//') ? route.query.redirect : fallback
    if (!redirect) {
      errorMessage.value = '当前账号没有可访问的菜单，请联系超级管理员'
      return
    }
    router.replace(redirect)
  } catch (error) {
    errorMessage.value = errorText(error)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-intro">
      <div class="login-brand"><span class="admin-brand-mark">卷</span>万卷管理后台</div>
      <div class="intro-copy"><span>WANJUAN ADMIN</span><h1>清晰管理，<br />让社区稳定生长。</h1><p>统一管理组织权限、社区用户与操作记录。</p></div>
      <p class="intro-footer">万卷技术社区 · 管理平台</p>
    </section>
    <section class="login-form-section">
      <div class="login-form-wrap">
        <div class="mobile-login-brand"><span class="admin-brand-mark">卷</span>万卷管理后台</div>
        <h2>管理员登录</h2><p class="login-subtitle">请使用管理员账号进入系统</p>
        <a-alert v-if="errorMessage" class="login-error" type="error" :message="errorMessage" show-icon />
        <a-form :model="form" :rules="rules" layout="vertical" size="large" @finish="submit">
          <a-form-item label="账号" name="account"><a-input v-model:value="form.account" autocomplete="username" placeholder="请输入管理员账号"><template #prefix><UserOutlined /></template></a-input></a-form-item>
          <a-form-item label="密码" name="password"><a-input-password v-model:value="form.password" autocomplete="current-password" placeholder="请输入密码" @press-enter="submit"><template #prefix><LockOutlined /></template></a-input-password></a-form-item>
          <a-button type="primary" html-type="submit" block :loading="loading">登录</a-button>
        </a-form>
        <p class="login-security">登录即表示你同意遵守平台安全管理规范</p>
      </div>
    </section>
  </main>
</template>
