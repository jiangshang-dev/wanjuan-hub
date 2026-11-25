<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowRightOutlined, CheckCircleOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { statisticsApi } from '../../api/s6'
import { flattenRouteMenus, normalizeMenus, routePathForMenu } from '../../router'
import { useAuthStore } from '../../stores/auth'
import { errorText, formatDate } from '../../utils'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const refreshing = ref(false)
const days = ref(7)
const refreshDate = ref(dayjs().subtract(1, 'day'))
const dashboard = ref(null)
const availableMenus = computed(() => flattenRouteMenus(normalizeMenus(auth.menus)).filter((menu) => menu.definition.name !== 'dashboard'))
const displayName = computed(() => auth.user?.nickname || auth.user?.username || auth.user?.account || '管理员')
const trends = computed(() => dashboard.value?.trends || [])
const summary = computed(() => dashboard.value?.summary || {})
const metricCards = computed(() => [
  { label: '注册用户', value: summary.value.totalUsers, suffix: '人' },
  { label: '今日新增用户', value: summary.value.newUsersToday, suffix: '人', accent: true },
  { label: '公开内容', value: summary.value.publicContents, suffix: '篇' },
  { label: '待处理审核', value: summary.value.pendingAudits, suffix: '项', warning: true },
  { label: '订单总数', value: summary.value.totalOrders, suffix: '单' },
  { label: '今日新增订单', value: summary.value.newOrdersToday, suffix: '单', accent: true },
  { label: '成功支付总额', value: formatAmount(summary.value.totalPaymentAmount) },
  { label: '今日支付金额', value: formatAmount(summary.value.todayPaymentAmount), accent: true },
])
const seriesDefinitions = [
  { key: 'newUsers', label: '新增用户', color: '#3157d5' },
  { key: 'newContents', label: '新增内容', color: '#19a37a' },
  { key: 'auditCount', label: '完成审核', color: '#e59b28' },
  { key: 'orderCount', label: '新增订单', color: '#a065d3' },
]
const trendMaximum = computed(() => Math.max(1, ...trends.value.flatMap((item) => seriesDefinitions.map((series) => Number(item[series.key] || 0)))))
const lineSeries = computed(() => seriesDefinitions.map((series) => ({
  ...series,
  points: trends.value.map((item, index) => {
    const x = 48 + (index * 704) / Math.max(1, trends.value.length - 1)
    const y = 220 - (Number(item[series.key] || 0) / trendMaximum.value) * 180
    return `${x.toFixed(2)},${y.toFixed(2)}`
  }).join(' '),
})))
const xLabels = computed(() => {
  const step = Math.max(1, Math.ceil(trends.value.length / 7))
  return trends.value.map((item, index) => ({ item, index })).filter(({ index }) => index % step === 0 || index === trends.value.length - 1).map(({ item, index }) => ({
    x: 48 + (index * 704) / Math.max(1, trends.value.length - 1),
    label: dayjs(item.statDate).format('MM-DD'),
  }))
})
const paymentMaximum = computed(() => Math.max(1, ...trends.value.map((item) => Number(item.paymentAmount || 0))))
const paymentBars = computed(() => trends.value.map((item) => ({
  date: dayjs(item.statDate).format('MM-DD'),
  amount: formatAmount(item.paymentAmount),
  height: Math.round((Number(item.paymentAmount || 0) / paymentMaximum.value) * 100),
})))
const categoryRows = computed(() => [...(dashboard.value?.categories || [])].sort((a, b) => Number(b.contentCount || 0) - Number(a.contentCount || 0)).slice(0, 12))
const categoryMaximum = computed(() => Math.max(1, ...categoryRows.value.map((item) => Number(item.contentCount || 0))))
const contentTypeNames = { BLOG: '博客', RESOURCE: '资源', FORUM_TOPIC: '论坛' }
const descriptions = {
  'system-admins': '维护管理员账号、状态与角色分配', 'system-roles': '维护角色并配置角色权限', 'system-permissions': '维护菜单目录、页面和操作权限',
  'system-users': '查询社区用户并管理账号状态', 'system-operation-logs': '查询后台关键操作记录', 'content-blogs': '查询博客详情并管理公开状态',
  'content-blog-categories': '维护博客分类与显示顺序', 'audit-center': '处理机器审核结果与人工审核任务',
  'points-rules': '配置积分与经验奖励规则', 'points-levels': '配置会员成长等级', 'points-adjustments': '查询流水并调整用户积分',
  'messages-system': '发送指定用户或全站系统消息', 'operation-announcements': '管理社区公告与发布时间', 'operation-recommendations': '维护公开内容推荐位',
  'operation-advertisements': '管理广告投放位置与周期', 'operation-friend-links': '维护友情链接', 'operation-hot-searches': '管理热门搜索词', 'operation-configs': '维护网站公开与内部参数',
}

function formatAmount(value) {
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' }).format(Number(value || 0))
}

async function load() {
  loading.value = true
  try {
    dashboard.value = await statisticsApi.dashboard(days.value)
  } catch (error) {
    message.error(errorText(error))
  } finally {
    loading.value = false
  }
}

async function refreshDaily() {
  if (!refreshDate.value) {
    message.warning('请选择需要重算的日期')
    return
  }
  refreshing.value = true
  try {
    const result = await statisticsApi.refreshDaily(refreshDate.value.format('YYYY-MM-DD'))
    message.success(`${result?.statDate || refreshDate.value.format('YYYY-MM-DD')} 汇总已重算`)
    await load()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    refreshing.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="dashboard-page">
    <section class="welcome-panel statistics-welcome">
      <div><span class="welcome-label"><CheckCircleOutlined /> 实时统计工作台</span><h2>{{ displayName }}，欢迎回来</h2><p>汇总、趋势与分类图表均来自后端真实统计接口。</p></div>
      <div class="login-meta"><span>最近登录</span><strong>{{ formatDate(auth.user?.lastLoginTime || auth.user?.lastLoginAt) }}</strong></div>
    </section>
    <div class="statistics-toolbar">
      <a-radio-group v-model:value="days" button-style="solid" @change="load"><a-radio-button :value="7">近7天</a-radio-button><a-radio-button :value="14">近14天</a-radio-button><a-radio-button :value="30">近30天</a-radio-button></a-radio-group>
      <a-space wrap><a-button :loading="loading" @click="load"><ReloadOutlined />刷新</a-button><a-date-picker v-model:value="refreshDate" :disabled-date="(date) => date.isAfter(dayjs(), 'day')" placeholder="重算日期" /><a-popconfirm title="确定重算该日期的汇总数据吗？" ok-text="确认重算" cancel-text="取消" @confirm="refreshDaily"><a-button v-permission="'statistics:dashboard:refresh'" :loading="refreshing">重算每日汇总</a-button></a-popconfirm></a-space>
    </div>
    <a-spin :spinning="loading">
      <div class="statistics-metric-grid"><a-card v-for="metric in metricCards" :key="metric.label" :bordered="false" class="statistics-metric" :class="{ accent: metric.accent, warning: metric.warning }"><span>{{ metric.label }}</span><strong>{{ metric.value ?? 0 }}<small v-if="metric.suffix">{{ metric.suffix }}</small></strong></a-card></div>
      <div class="statistics-chart-grid">
        <a-card :bordered="false" class="panel-card trend-chart-card" :title="`近${days}天社区趋势`">
          <template #extra><div class="chart-legend"><span v-for="series in seriesDefinitions" :key="series.key"><i :style="{ background: series.color }" />{{ series.label }}</span></div></template>
          <div v-if="trends.length" class="line-chart"><svg viewBox="0 0 800 270" role="img" aria-label="新增用户、内容、审核与订单趋势图"><line v-for="tick in [0, 0.5, 1]" :key="tick" x1="48" x2="752" :y1="220 - tick * 180" :y2="220 - tick * 180" class="chart-grid-line" /><text v-for="tick in [0, 0.5, 1]" :key="`label-${tick}`" x="39" :y="224 - tick * 180" text-anchor="end" class="chart-axis-label">{{ Math.round(trendMaximum * tick) }}</text><polyline v-for="series in lineSeries" :key="series.key" :points="series.points" fill="none" :stroke="series.color" stroke-width="3" stroke-linejoin="round" stroke-linecap="round" /><text v-for="label in xLabels" :key="`${label.x}-${label.label}`" :x="label.x" y="250" text-anchor="middle" class="chart-axis-label">{{ label.label }}</text></svg></div>
          <a-empty v-else description="暂无趋势数据" />
        </a-card>
        <a-card :bordered="false" class="panel-card payment-chart-card" title="每日成功支付金额">
          <div v-if="paymentBars.length" class="payment-bars"><div v-for="bar in paymentBars" :key="bar.date" class="payment-bar-item" :title="`${bar.date} ${bar.amount}`"><span class="payment-value">{{ bar.amount }}</span><div class="payment-bar-track"><i :style="{ height: `${bar.height}%` }" /></div><span>{{ bar.date }}</span></div></div>
          <a-empty v-else description="暂无支付趋势数据" />
        </a-card>
      </div>
      <div class="statistics-bottom-grid">
        <a-card :bordered="false" class="panel-card" title="公开内容分类统计">
          <div v-if="categoryRows.length" class="category-bars"><div v-for="row in categoryRows" :key="`${row.contentType}-${row.categoryId}`"><span class="category-label"><a-tag>{{ contentTypeNames[row.contentType] || row.contentType }}</a-tag>{{ row.categoryName || `分类 ${row.categoryId}` }}</span><div class="category-track"><i :style="{ width: `${Math.round(Number(row.contentCount || 0) / categoryMaximum * 100)}%` }" /></div><strong>{{ row.contentCount }}</strong></div></div>
          <a-empty v-else description="暂无公开内容分类数据" />
        </a-card>
        <a-card :bordered="false" class="panel-card" title="消息与 Outbox 积压">
          <div class="backlog-grid"><div><span>未完成 MQ 消费</span><strong>{{ dashboard?.backlog?.mqConsumeBacklog ?? 0 }}</strong><small>状态非 SUCCESS 的消费记录</small></div><div><span>待处理 Outbox</span><strong>{{ dashboard?.backlog?.outboxBacklog ?? 0 }}</strong><small>待发送或发送失败的记录</small></div></div>
        </a-card>
      </div>
    </a-spin>
    <section class="workspace-section"><div class="section-title"><h3>可用管理功能</h3><span>{{ availableMenus.length }} 个入口</span></div><div class="workspace-grid"><button v-for="menu in availableMenus" :key="menu.path" class="workspace-item" type="button" @click="router.push(routePathForMenu(menu))"><div><strong>{{ menu.name || menu.definition.title }}</strong><p>{{ descriptions[menu.definition.name] || '进入管理页面' }}</p></div><ArrowRightOutlined /></button></div><a-empty v-if="!availableMenus.length" description="暂无可用的其他管理入口" /></section>
  </div>
</template>

<style scoped>
.statistics-welcome { margin-bottom: 18px; }
.statistics-toolbar { display: flex; margin-bottom: 18px; align-items: center; justify-content: space-between; gap: 16px; }
.statistics-metric-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; margin-bottom: 18px; }
.statistics-metric { border: 1px solid var(--line); border-radius: 11px; }
.statistics-metric :deep(.ant-card-body) { padding: 20px 22px; }
.statistics-metric span { color: var(--muted); font-size: 12px; }
.statistics-metric strong { display: block; margin-top: 9px; overflow: hidden; color: var(--ink); font-size: 26px; text-overflow: ellipsis; white-space: nowrap; }
.statistics-metric strong small { margin-left: 5px; color: var(--muted); font-size: 12px; font-weight: 400; }
.statistics-metric.accent strong { color: var(--primary); }
.statistics-metric.warning strong { color: #c88016; }
.statistics-chart-grid { display: grid; grid-template-columns: minmax(0, 1.7fr) minmax(310px, .8fr); gap: 18px; margin-bottom: 18px; }
.chart-legend { display: flex; flex-wrap: wrap; gap: 12px; color: var(--muted); font-size: 11px; font-weight: 400; }
.chart-legend span { display: inline-flex; align-items: center; gap: 5px; }
.chart-legend i { width: 8px; height: 8px; border-radius: 50%; }
.line-chart { width: 100%; overflow-x: auto; }
.line-chart svg { display: block; min-width: 570px; width: 100%; }
.chart-grid-line { stroke: #e8ecf3; stroke-width: 1; }
.chart-axis-label { fill: #8993a6; font-size: 11px; }
.payment-bars { display: flex; height: 270px; padding-top: 30px; align-items: flex-end; gap: 6px; overflow-x: auto; }
.payment-bar-item { display: flex; min-width: 30px; height: 100%; flex: 1; align-items: center; flex-direction: column; justify-content: flex-end; gap: 7px; color: var(--muted); font-size: 10px; }
.payment-value { width: 70px; overflow: hidden; color: #6f7b90; text-align: center; text-overflow: ellipsis; white-space: nowrap; transform: rotate(-45deg) translateY(7px); }
.payment-bar-track { display: flex; width: 16px; height: 155px; align-items: flex-end; overflow: hidden; border-radius: 5px 5px 2px 2px; background: #f0f3f8; }
.payment-bar-track i { display: block; width: 100%; min-height: 2px; border-radius: 5px 5px 2px 2px; background: linear-gradient(#6687e5, #3157d5); }
.statistics-bottom-grid { display: grid; grid-template-columns: minmax(0, 1.5fr) minmax(300px, .7fr); gap: 18px; margin-bottom: 18px; }
.category-bars { display: flex; flex-direction: column; gap: 15px; }
.category-bars > div { display: grid; grid-template-columns: minmax(170px, .8fr) minmax(120px, 1fr) 55px; align-items: center; gap: 12px; }
.category-label { display: flex; min-width: 0; align-items: center; overflow: hidden; color: #536078; text-overflow: ellipsis; white-space: nowrap; }
.category-label .ant-tag { flex: 0 0 auto; margin-inline-end: 7px; }
.category-track { height: 8px; overflow: hidden; border-radius: 8px; background: #edf0f5; }
.category-track i { display: block; height: 100%; min-width: 2px; border-radius: 8px; background: #6c8ce5; }
.category-bars strong { color: var(--ink); text-align: right; }
.backlog-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; }
.backlog-grid > div { padding: 20px; border-radius: 10px; background: #f6f8fc; }
.backlog-grid span, .backlog-grid small { display: block; color: var(--muted); }
.backlog-grid strong { display: block; margin: 12px 0; color: #c36b36; font-size: 32px; }
.backlog-grid small { font-size: 11px; line-height: 1.6; }
@media (max-width: 1200px) { .statistics-metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .statistics-chart-grid, .statistics-bottom-grid { grid-template-columns: 1fr; } }
@media (max-width: 700px) { .statistics-toolbar { align-items: flex-start; flex-direction: column; } .statistics-metric-grid { grid-template-columns: 1fr; } .backlog-grid { grid-template-columns: 1fr; } .category-bars > div { grid-template-columns: minmax(130px, .8fr) 1fr 40px; } }
</style>
