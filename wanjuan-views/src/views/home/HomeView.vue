<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { displayName } from '../../utils/format'

const router = useRouter()
const auth = useAuthStore()
</script>

<template>
  <div class="home-page">
    <section class="hero-section">
      <div class="hero-copy">
        <span class="eyebrow">WANJUAN COMMUNITY</span>
        <h1>{{ auth.isLoggedIn ? `${displayName(auth.user)}，欢迎回来` : '让每一次学习，都有回响' }}</h1>
        <p>万卷技术社区为学习者提供清晰、安全的账号与个人空间。从这里开始，管理你的社区身份。</p>
        <div class="hero-actions">
          <template v-if="auth.isLoggedIn">
            <a-button type="primary" size="large" @click="router.push('/profile')">进入个人中心</a-button>
            <a-button size="large" @click="router.push('/security')">账号安全</a-button>
          </template>
          <template v-else>
            <a-button type="primary" size="large" @click="router.push('/register')">创建账号</a-button>
            <a-button size="large" @click="router.push('/login')">账号登录</a-button>
          </template>
        </div>
      </div>
      <div class="hero-panel" aria-label="社区账号服务">
        <div class="panel-symbol">卷</div>
        <h2>一个账号，一处空间</h2>
        <p>资料维护、安全验证与登录状态管理，始终围绕你的真实账号数据。</p>
        <div class="panel-lines"><span /><span /><span /></div>
      </div>
    </section>
    <section class="quick-section">
      <div class="section-heading"><span>快速入口</span><h2>管理你的社区账号</h2></div>
      <div class="quick-grid">
        <button class="quick-card" type="button" @click="router.push(auth.isLoggedIn ? '/profile' : '/login')">
          <span class="quick-index">01</span><strong>个人资料</strong><p>查看并更新昵称、头像、签名等公开信息。</p><span class="quick-link">立即前往 →</span>
        </button>
        <button class="quick-card" type="button" @click="router.push(auth.isLoggedIn ? '/security' : '/login')">
          <span class="quick-index">02</span><strong>账号安全</strong><p>修改密码，绑定邮箱和手机号，保护账号安全。</p><span class="quick-link">立即前往 →</span>
        </button>
        <button class="quick-card" type="button" @click="router.push('/blogs')">
          <span class="quick-index">03</span><strong>技术博客</strong><p>按分类浏览社区公开内容，发现真实的学习记录。</p><span class="quick-link">开始阅读 →</span>
        </button>
      </div>
    </section>
  </div>
</template>
