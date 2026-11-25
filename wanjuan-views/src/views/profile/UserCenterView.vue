<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { pointsApi, messageApi } from '../../api/s6'
import { useAuthStore } from '../../stores/auth'
import { displayName, errorText } from '../../utils/format'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(true)
const account = ref(null)
const unreadCount = ref(0)
const entries = [
  { path: '/my/membership', title: '会员与积分', description: '每日签到、成长等级、经验进度和积分流水。' },
  { path: '/my/messages', title: '消息中心', description: '分类处理审核、支付、系统和互动通知。' },
  { path: '/my/activity?tab=following', title: '关注与粉丝', description: '查看关注关系，管理你关注的社区用户。' },
  { path: '/my/activity', title: '我的互动', description: '集中查看主题、评论、点赞、收藏和举报。' },
  { path: '/my/blogs', title: '我的博客', description: '管理博客创作、发布状态和历史版本。' },
  { path: '/profile', title: '个人资料', description: '维护昵称、头像、签名和公开资料。' },
  { path: '/security', title: '账号安全', description: '修改密码，绑定邮箱和手机号码。' },
]

onMounted(async () => {
  const results = await Promise.allSettled([pointsApi.account(), messageApi.unreadCount()])
  if (results[0].status === 'fulfilled') account.value = results[0].value
  if (results[1].status === 'fulfilled') unreadCount.value = Number(results[1].value || 0)
  const failed = results.find((result) => result.status === 'rejected')
  if (failed) message.error(errorText(failed.reason))
  loading.value = false
})
</script>

<template>
  <div class="content-page user-center-page">
    <section class="user-center-hero">
      <a-avatar :src="auth.user?.avatar" :size="72">{{ displayName(auth.user).slice(0, 1) }}</a-avatar>
      <div class="user-center-copy"><span class="eyebrow">MY WANJUAN</span><h1>{{ displayName(auth.user) }}的个人中心</h1><p>{{ auth.user?.signature || '管理你的社区身份、内容、互动与成长记录。' }}</p></div>
      <a-button @click="router.push('/profile')">编辑资料</a-button>
    </section>
    <a-spin :spinning="loading">
      <div class="user-center-stats">
        <button type="button" @click="router.push('/my/membership')"><span>可用积分</span><strong>{{ account?.balance ?? '-' }}</strong><small>累计 {{ account?.totalPoints ?? '-' }} · 经验 {{ account?.experience ?? '-' }}</small></button>
        <button type="button" @click="router.push('/my/messages')"><span>未读消息</span><strong>{{ unreadCount }}</strong><small>进入消息中心处理通知</small></button>
        <button type="button" @click="router.push('/my/membership')"><span>今日签到</span><strong class="sign-status">{{ account?.signedToday ? '已签到' : '待签到' }}</strong><small>{{ account?.signedToday ? '明天再来积累成长' : '签到可获取积分与经验' }}</small></button>
      </div>
    </a-spin>
    <section class="user-center-section"><div class="section-title"><h2>我的社区服务</h2><span>所有数据来自你的真实账号</span></div><div class="user-center-grid"><button v-for="(entry, index) in entries" :key="entry.path" type="button" @click="router.push(entry.path)"><span class="entry-index">{{ String(index + 1).padStart(2, '0') }}</span><strong>{{ entry.title }}<a-badge v-if="entry.path === '/my/messages' && unreadCount" :count="unreadCount" /></strong><p>{{ entry.description }}</p><span class="entry-link">立即前往 →</span></button></div></section>
  </div>
</template>

<style scoped>
.user-center-hero { display: flex; padding: 32px 36px; align-items: center; gap: 22px; border: 1px solid var(--line); border-radius: 16px; background: #fff; }
.user-center-copy { min-width: 0; flex: 1; }
.user-center-copy .eyebrow { margin-bottom: 7px; }
.user-center-copy h1 { margin: 0 0 8px; color: var(--ink); font-size: 26px; }
.user-center-copy p { margin: 0; overflow: hidden; color: var(--muted); text-overflow: ellipsis; white-space: nowrap; }
.user-center-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; margin: 20px 0; }
.user-center-stats button { padding: 24px; border: 1px solid var(--line); border-radius: 13px; text-align: left; background: #fff; cursor: pointer; }
.user-center-stats button:hover, .user-center-grid button:hover { border-color: #b4c4ed; box-shadow: 0 10px 25px rgba(32,52,91,.06); }
.user-center-stats span, .user-center-stats small { display: block; color: var(--muted); }
.user-center-stats strong { display: block; margin: 10px 0; color: var(--ink); font-size: 32px; }
.user-center-stats strong.sign-status { color: var(--primary); font-size: 25px; }
.user-center-stats small { font-size: 12px; }
.user-center-section { padding: 28px; border: 1px solid var(--line); border-radius: 15px; background: #fff; }
.section-title { display: flex; margin-bottom: 22px; align-items: center; justify-content: space-between; gap: 16px; }
.section-title h2 { margin: 0; font-size: 19px; }
.section-title > span { color: var(--muted); font-size: 12px; }
.user-center-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.user-center-grid button { position: relative; min-height: 168px; padding: 22px; border: 1px solid var(--line); border-radius: 11px; text-align: left; background: #fff; cursor: pointer; }
.user-center-grid strong { display: flex; align-items: center; gap: 10px; color: var(--ink); font-size: 17px; }
.user-center-grid p { margin: 12px 0 18px; color: var(--muted); font-size: 13px; line-height: 1.7; }
.entry-index { position: absolute; top: 20px; right: 20px; color: #c2cad8; font-size: 12px; }
.entry-link { color: var(--primary); font-size: 12px; }
@media (max-width: 800px) { .user-center-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 600px) { .user-center-hero { padding: 24px 20px; align-items: flex-start; flex-wrap: wrap; } .user-center-copy { flex-basis: calc(100% - 100px); } .user-center-copy p { white-space: normal; } .user-center-stats, .user-center-grid { grid-template-columns: 1fr; } .user-center-section { padding: 20px; } }
</style>
